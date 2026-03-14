package com.hhoa.kline.core.core.integrations.checkpoints;

import com.hhoa.kline.core.core.services.telemetry.DefaultTelemetryService;
import com.hhoa.kline.core.core.services.telemetry.TelemetryService;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CheckpointTracker {

    private final String taskId;
    private final String cwd;
    private final String cwdHash;
    private String lastRetrievedShadowGitConfigWorkTree;
    private final CheckpointGitOperations gitOperations;
    private final TelemetryService telemetryService;

    private CheckpointTracker(String taskId, String cwd, String cwdHash) {
        this.taskId = taskId;
        this.cwd = cwd;
        this.cwdHash = cwdHash;
        this.gitOperations = new CheckpointGitOperations(cwd);
        this.telemetryService = new DefaultTelemetryService();
    }

    public static CompletableFuture<CheckpointTracker> create(
            String taskId, boolean enableCheckpointsSetting, String workspacePath) {
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        log.info("Creating new CheckpointTracker for task {}", taskId);

                        if (!enableCheckpointsSetting) {
                            log.info("Checkpoints disabled by setting for task {}", taskId);
                            return null;
                        }

                        if (!isGitInstalled()) {
                            throw new RuntimeException("Git must be installed to use checkpoints.");
                        }

                        CheckpointUtils.validateWorkspacePath(workspacePath);
                        String cwdHash = CheckpointUtils.hashWorkingDir(workspacePath);
                        log.debug("Repository ID (cwdHash): {}", cwdHash);

                        CheckpointTracker tracker =
                                new CheckpointTracker(taskId, workspacePath, cwdHash);
                        long initStartTime = System.currentTimeMillis();
                        Path gitPath = CheckpointUtils.getShadowGitPath(cwdHash);
                        tracker.gitOperations.initShadowGit(gitPath, workspacePath, taskId);
                        long initDurationMs = System.currentTimeMillis() - initStartTime;
                        tracker.telemetryService.captureCheckpointUsage(
                                taskId, "shadow_git_initialized", initDurationMs);

                        log.info("CheckpointTracker created");

                        return tracker;
                    } catch (Exception e) {
                        log.error("Failed to create CheckpointTracker: {}", e.getMessage(), e);
                        throw new RuntimeException(
                                "Failed to create CheckpointTracker: " + e.getMessage(), e);
                    }
                });
    }

    private static boolean isGitInstalled() {
        try {
            ProcessBuilder pb = new ProcessBuilder("git", "--version");
            Process process = pb.start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return false;
            }
            return process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    public CompletableFuture<String> commit() {
        return CompletableFuture.supplyAsync(
                () -> {
                    boolean lockAcquired = false;
                    try {
                        log.info("Creating new checkpoint commit for task {}", taskId);
                        long startTime = System.currentTimeMillis();

                        CheckpointLockUtils.FolderLockWithRetryResult lockResult =
                                CheckpointLockUtils.tryAcquireCheckpointLockWithRetry(
                                        cwdHash, taskId);

                        if (!lockResult.acquired() && !lockResult.skipped()) {
                            throw new RuntimeException(
                                    "Failed to acquire checkpoint folder lock - another Cline instance may be performing checkpoint operations");
                        }

                        if (!lockResult.acquired() && lockResult.skipped()) {
                            log.info("Skipping Checkpoints lock - VS Code");
                        }

                        if (lockResult.acquired()) {
                            lockAcquired = true;
                        }

                        Path gitPath = CheckpointUtils.getShadowGitPath(cwdHash);
                        String gitDir = gitPath.getParent().toString();

                        log.info("Using shadow git at: {}", gitPath);

                        CheckpointGitOperations.CheckpointAddResult addFilesResult =
                                gitOperations.addCheckpointFiles(gitDir);
                        if (!addFilesResult.isSuccess()) {
                            log.error(
                                    "Failed to add at least one file(s) to checkpoints shadow git");
                        }

                        String commitMessage = "checkpoint-" + cwdHash + "-" + taskId;
                        log.info("Creating checkpoint commit with message: {}", commitMessage);

                        String commitHash = executeGitCommit(gitDir, commitMessage);
                        log.info("Checkpoint commit created: {}", commitHash);

                        long durationMs = System.currentTimeMillis() - startTime;
                        telemetryService.captureCheckpointUsage(
                                taskId, "commit_created", durationMs);

                        return commitHash;
                    } catch (Exception e) {
                        log.error("Failed to create checkpoint: {}", e.getMessage(), e);
                        throw new RuntimeException(
                                "Failed to create checkpoint: " + e.getMessage(), e);
                    } finally {
                        if (lockAcquired) {
                            log.info("Releasing checkpoint folder lock");
                            CheckpointLockUtils.releaseCheckpointLock(cwdHash, taskId);
                        }
                    }
                });
    }

    private String executeGitCommit(String gitDir, String message)
            throws IOException, InterruptedException {
        ProcessBuilder pb =
                new ProcessBuilder("git", "commit", "-m", message, "--allow-empty", "--no-verify");
        pb.directory(new File(gitDir));
        pb.redirectErrorStream(true);

        Process process = pb.start();
        boolean finished = process.waitFor(30, TimeUnit.SECONDS);

        if (!finished) {
            process.destroyForcibly();
            throw new IOException("Git commit timed out");
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            throw new IOException("Git commit failed with exit code " + exitCode);
        }

        return getCurrentCommitHash(gitDir);
    }

    private String getCurrentCommitHash(String gitDir) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("git", "rev-parse", "HEAD");
        pb.directory(new File(gitDir));
        pb.redirectErrorStream(true);

        Process process = pb.start();
        boolean finished = process.waitFor(5, TimeUnit.SECONDS);

        if (!finished) {
            process.destroyForcibly();
            throw new IOException("Git rev-parse timed out");
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            throw new IOException("Git rev-parse failed with exit code " + exitCode);
        }

        try (BufferedReader reader =
                new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String hash = reader.readLine();
            return hash != null ? hash.trim() : "";
        }
    }

    public CompletableFuture<String> getShadowGitConfigWorkTree() {
        if (lastRetrievedShadowGitConfigWorkTree != null) {
            return CompletableFuture.completedFuture(lastRetrievedShadowGitConfigWorkTree);
        }
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        Path gitPath = CheckpointUtils.getShadowGitPath(cwdHash);
                        lastRetrievedShadowGitConfigWorkTree =
                                gitOperations.getShadowGitConfigWorkTree(gitPath);
                        return lastRetrievedShadowGitConfigWorkTree;
                    } catch (Exception e) {
                        log.error(
                                "Failed to get shadow git config worktree: {}", e.getMessage(), e);
                        return null;
                    }
                });
    }

    public CompletableFuture<Void> resetHead(String commitHash) {
        return CompletableFuture.runAsync(
                () -> {
                    boolean lockAcquired = false;
                    try {
                        log.info("Resetting to checkpoint: {}", commitHash);
                        long startTime = System.currentTimeMillis();

                        CheckpointLockUtils.FolderLockWithRetryResult lockResult =
                                CheckpointLockUtils.tryAcquireCheckpointLockWithRetry(
                                        cwdHash, taskId);

                        if (!lockResult.acquired() && !lockResult.skipped()) {
                            throw new RuntimeException(
                                    "Failed to acquire checkpoint folder lock - another Cline instance may be performing checkpoint operations");
                        }

                        if (!lockResult.acquired() && lockResult.skipped()) {
                            log.info("Skipping Checkpoints lock - VS Code");
                        }

                        if (lockResult.acquired()) {
                            lockAcquired = true;
                        }

                        Path gitPath = CheckpointUtils.getShadowGitPath(cwdHash);
                        String gitDir = gitPath.getParent().toString();
                        log.debug("Using shadow git at: {}", gitPath);

                        executeGitReset(gitDir, cleanCommitHash(commitHash));
                        log.debug("Successfully reset to checkpoint: {}", commitHash);

                        long durationMs = System.currentTimeMillis() - startTime;
                        telemetryService.captureCheckpointUsage(taskId, "restored", durationMs);
                    } catch (Exception e) {
                        log.error("Failed to reset to checkpoint: {}", e.getMessage(), e);
                        throw new RuntimeException(
                                "Failed to reset to checkpoint: " + e.getMessage(), e);
                    } finally {
                        if (lockAcquired) {
                            CheckpointLockUtils.releaseCheckpointLock(cwdHash, taskId);
                        }
                    }
                });
    }

    private void executeGitReset(String gitDir, String commitHash)
            throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("git", "reset", "--hard", commitHash);
        pb.directory(new File(gitDir));
        pb.redirectErrorStream(true);

        Process process = pb.start();
        boolean finished = process.waitFor(30, TimeUnit.SECONDS);

        if (!finished) {
            process.destroyForcibly();
            throw new IOException("Git reset timed out");
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            throw new IOException("Git reset failed with exit code " + exitCode);
        }
    }

    private String cleanCommitHash(String hash) {
        return hash.startsWith("HEAD ") ? hash.substring(5) : hash;
    }

    public CompletableFuture<List<FileDiff>> getDiffSet(String lhsHash, String rhsHash) {
        return CompletableFuture.supplyAsync(
                () -> {
                    long startTime = System.currentTimeMillis();
                    try {
                        Path gitPath = CheckpointUtils.getShadowGitPath(cwdHash);
                        String gitDir = gitPath.getParent().toString();

                        log.info(
                                "Getting diff between commits: {} -> {}",
                                lhsHash,
                                rhsHash != null ? rhsHash : "working directory");

                        gitOperations.addCheckpointFiles(gitDir);

                        String cleanRhs = rhsHash != null ? cleanCommitHash(rhsHash) : null;
                        String diffRange =
                                cleanRhs != null
                                        ? cleanCommitHash(lhsHash) + ".." + cleanRhs
                                        : cleanCommitHash(lhsHash);

                        log.info("Diff range: {}", diffRange);

                        List<String> changedFiles = getChangedFiles(gitDir, diffRange);
                        List<FileDiff> result = new ArrayList<>();

                        for (String filePath : changedFiles) {
                            Path absolutePath = Paths.get(cwd, filePath);

                            String beforeContent = "";
                            try {
                                beforeContent =
                                        getFileContentAtCommit(
                                                gitDir, cleanCommitHash(lhsHash), filePath);
                            } catch (Exception e) {
                                // file didn't exist in older commit => remains empty
                            }

                            String afterContent = "";
                            if (rhsHash != null) {
                                try {
                                    afterContent =
                                            getFileContentAtCommit(
                                                    gitDir, cleanCommitHash(rhsHash), filePath);
                                } catch (Exception e) {
                                    // file didn't exist in newer commit => remains empty
                                }
                            } else {
                                try {
                                    if (Files.exists(absolutePath)) {
                                        afterContent = Files.readString(absolutePath);
                                    }
                                } catch (Exception e) {
                                    // file might be deleted => remains empty
                                }
                            }

                            result.add(
                                    new FileDiff(
                                            filePath,
                                            absolutePath.toString(),
                                            beforeContent,
                                            afterContent));
                        }

                        long durationMs = System.currentTimeMillis() - startTime;
                        telemetryService.captureCheckpointUsage(
                                taskId, "diff_generated", durationMs);

                        return result;
                    } catch (Exception e) {
                        log.error("Failed to get diff set: {}", e.getMessage(), e);
                        throw new RuntimeException("Failed to get diff set: " + e.getMessage(), e);
                    }
                });
    }

    private List<String> getChangedFiles(String gitDir, String diffRange)
            throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("git", "diff", "--name-only", diffRange);
        pb.directory(new File(gitDir));
        pb.redirectErrorStream(true);

        Process process = pb.start();
        boolean finished = process.waitFor(30, TimeUnit.SECONDS);

        if (!finished) {
            process.destroyForcibly();
            throw new IOException("Git diff timed out");
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            throw new IOException("Git diff failed with exit code " + exitCode);
        }

        List<String> files = new ArrayList<>();
        try (BufferedReader reader =
                new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    files.add(line.trim());
                }
            }
        }
        return files;
    }

    private String getFileContentAtCommit(String gitDir, String commitHash, String filePath)
            throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("git", "show", commitHash + ":" + filePath);
        pb.directory(new File(gitDir));
        pb.redirectErrorStream(true);

        Process process = pb.start();
        boolean finished = process.waitFor(10, TimeUnit.SECONDS);

        if (!finished) {
            process.destroyForcibly();
            throw new IOException("Git show timed out");
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            throw new IOException("Git show failed with exit code " + exitCode);
        }

        StringBuilder content = new StringBuilder();
        try (BufferedReader reader =
                new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }

    public CompletableFuture<Integer> getDiffCount(String lhsHash, String rhsHash) {
        return CompletableFuture.supplyAsync(
                () -> {
                    long startTime = System.currentTimeMillis();
                    try {
                        Path gitPath = CheckpointUtils.getShadowGitPath(cwdHash);
                        String gitDir = gitPath.getParent().toString();

                        log.info(
                                "Getting diff count between commits: {} -> {}",
                                lhsHash,
                                rhsHash != null ? rhsHash : "working directory");

                        gitOperations.addCheckpointFiles(gitDir);

                        String cleanRhs = rhsHash != null ? cleanCommitHash(rhsHash) : null;
                        String diffRange =
                                cleanRhs != null
                                        ? cleanCommitHash(lhsHash) + ".." + cleanRhs
                                        : cleanCommitHash(lhsHash);

                        List<String> changedFiles = getChangedFiles(gitDir, diffRange);

                        long durationMs = System.currentTimeMillis() - startTime;
                        telemetryService.captureCheckpointUsage(
                                taskId, "diff_generated", durationMs);

                        return changedFiles.size();
                    } catch (Exception e) {
                        log.error("Failed to get diff count: {}", e.getMessage(), e);
                        return 0;
                    }
                });
    }

    public static class FileDiff {
        private final String relativePath;
        private final String absolutePath;
        private final String before;
        private final String after;

        public FileDiff(String relativePath, String absolutePath, String before, String after) {
            this.relativePath = relativePath;
            this.absolutePath = absolutePath;
            this.before = before;
            this.after = after;
        }

        public String getRelativePath() {
            return relativePath;
        }

        public String getAbsolutePath() {
            return absolutePath;
        }

        public String getBefore() {
            return before;
        }

        public String getAfter() {
            return after;
        }
    }
}
