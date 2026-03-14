package com.hhoa.kline.core.core.integrations.checkpoints;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CheckpointGitOperations {

    private final String cwd;

    public CheckpointGitOperations(String cwd) {
        this.cwd = cwd;
    }

    public Path initShadowGit(Path gitPath, String cwd, String taskId)
            throws IOException, InterruptedException {
        log.info("Initializing shadow git");

        if (Files.exists(gitPath)) {
            String worktree = getGitConfig(gitPath.getParent().toString(), "core.worktree");
            if (worktree != null && !worktree.equals(cwd)) {
                throw new IOException(
                        "Checkpoints can only be used in the original workspace: " + worktree);
            }
            log.warn("Using existing shadow git at {}", gitPath);

            List<String> lfsPatterns = CheckpointExclusions.getLfsPatterns(this.cwd);
            CheckpointExclusions.writeExcludesFile(gitPath, lfsPatterns);

            return gitPath;
        }

        Path checkpointsDir = gitPath.getParent();
        log.warn("Creating new shadow git in {}", checkpointsDir);

        executeGitCommand(checkpointsDir.toString(), "init");

        setGitConfig(checkpointsDir.toString(), "core.worktree", cwd);
        setGitConfig(checkpointsDir.toString(), "commit.gpgSign", "false");
        setGitConfig(checkpointsDir.toString(), "user.name", "Cline Checkpoint");
        setGitConfig(checkpointsDir.toString(), "user.email", "checkpoint@cline.bot");

        List<String> lfsPatterns = CheckpointExclusions.getLfsPatterns(cwd);
        CheckpointExclusions.writeExcludesFile(gitPath, lfsPatterns);

        CheckpointAddResult addFilesResult = addCheckpointFiles(checkpointsDir.toString());
        if (!addFilesResult.isSuccess()) {
            log.error("Failed to add at least one file(s) to checkpoints shadow git");
            throw new IOException("Failed to add at least one file(s) to checkpoints shadow git");
        }

        executeGitCommand(
                checkpointsDir.toString(), "commit", "-m", "initial commit", "--allow-empty");

        log.warn("Shadow git initialization completed");

        return gitPath;
    }

    public String getShadowGitConfigWorkTree(Path gitPath) {
        try {
            return getGitConfig(gitPath.getParent().toString(), "core.worktree");
        } catch (Exception e) {
            log.error("Failed to get shadow git config worktree: {}", e.getMessage());
            return null;
        }
    }

    public void renameNestedGitRepos(boolean disable) throws IOException {
        List<Path> gitPaths = findNestedGitRepos(disable);

        for (Path gitPath : gitPaths) {
            try {
                Path newPath;
                if (disable) {
                    newPath =
                            Paths.get(
                                    gitPath.toString() + CheckpointExclusions.GIT_DISABLED_SUFFIX);
                } else {
                    String pathStr = gitPath.toString();
                    if (pathStr.endsWith(CheckpointExclusions.GIT_DISABLED_SUFFIX)) {
                        newPath =
                                Paths.get(
                                        pathStr.substring(
                                                0,
                                                pathStr.length()
                                                        - CheckpointExclusions.GIT_DISABLED_SUFFIX
                                                                .length()));
                    } else {
                        newPath = gitPath;
                    }
                }
                Files.move(gitPath, newPath);
                log.info(
                        "CheckpointTracker {} nested git repo {}",
                        disable ? "disabled" : "enabled",
                        gitPath);
            } catch (IOException e) {
                log.error(
                        "CheckpointTracker failed to {} nested git repo {}: {}",
                        disable ? "disable" : "enable",
                        gitPath,
                        e.getMessage());
            }
        }
    }

    private List<Path> findNestedGitRepos(boolean disable) throws IOException {
        List<Path> gitPaths = new ArrayList<>();
        Path rootPath = Paths.get(cwd);
        String suffix = disable ? "" : CheckpointExclusions.GIT_DISABLED_SUFFIX;

        Files.walk(rootPath)
                .filter(
                        path -> {
                            String fileName = path.getFileName().toString();
                            return fileName.equals(".git" + suffix) && Files.isDirectory(path);
                        })
                .filter(path -> !path.equals(rootPath.resolve(".git")))
                .forEach(gitPaths::add);

        return gitPaths;
    }

    public CheckpointAddResult addCheckpointFiles(String gitDir) {
        long startTime = System.currentTimeMillis();
        try {
            renameNestedGitRepos(true);
            log.info("Starting checkpoint add operation...");

            try {
                executeGitCommand(gitDir, "add", ".", "--ignore-errors");
                long durationMs = System.currentTimeMillis() - startTime;
                log.debug("Checkpoint add operation completed in {}ms", durationMs);
                return new CheckpointAddResult(true);
            } catch (Exception e) {
                log.warn("Failed to add files: {}", e.getMessage());
                return new CheckpointAddResult(false);
            }
        } catch (Exception e) {
            log.warn("Failed to add checkpoint files: {}", e.getMessage());
            return new CheckpointAddResult(false);
        } finally {
            try {
                renameNestedGitRepos(false);
            } catch (IOException e) {
                log.error("Failed to re-enable nested git repos: {}", e.getMessage());
            }
        }
    }

    private void executeGitCommand(String gitDir, String... args)
            throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add("git");
        command.addAll(List.of(args));

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(new File(gitDir));
        pb.redirectErrorStream(true);

        Process process = pb.start();
        boolean finished = process.waitFor(30, TimeUnit.SECONDS);

        if (!finished) {
            process.destroyForcibly();
            throw new IOException("Git command timed out: " + String.join(" ", command));
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            try (BufferedReader reader =
                    new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                StringBuilder error = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    error.append(line).append("\n");
                }
                throw new IOException(
                        "Git command failed with exit code " + exitCode + ": " + error);
            }
        }
    }

    private void setGitConfig(String gitDir, String key, String value)
            throws IOException, InterruptedException {
        executeGitCommand(gitDir, "config", key, value);
    }

    private String getGitConfig(String gitDir, String key)
            throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("git", "config", "--get", key);
        pb.directory(new File(gitDir));
        pb.redirectErrorStream(true);

        Process process = pb.start();
        boolean finished = process.waitFor(5, TimeUnit.SECONDS);

        if (!finished) {
            process.destroyForcibly();
            return null;
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            return null;
        }

        try (BufferedReader reader =
                new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line = reader.readLine();
            return line != null ? line.trim() : null;
        }
    }

    public static class CheckpointAddResult {
        private final boolean success;

        public CheckpointAddResult(boolean success) {
            this.success = success;
        }

        public boolean isSuccess() {
            return success;
        }
    }
}
