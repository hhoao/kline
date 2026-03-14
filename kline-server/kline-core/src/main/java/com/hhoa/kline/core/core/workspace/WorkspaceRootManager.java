package com.hhoa.kline.core.core.workspace;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hhoa.kline.core.core.shared.multiroot.VcsType;
import com.hhoa.kline.core.core.shared.multiroot.WorkspaceRoot;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WorkspaceRootManager {

    private final List<WorkspaceRoot> roots;

    @Getter private int primaryIndex;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public WorkspaceRootManager() {
        this(new ArrayList<>(), 0);
    }

    public WorkspaceRootManager(List<WorkspaceRoot> roots, int primaryIndex) {
        this.roots = new ArrayList<>(roots);
        this.primaryIndex = Math.min(primaryIndex, Math.max(0, roots.size() - 1));
    }

    public String getCwd() throws Exception {
        return getPrimaryRoot().getPath();
    }

    private static VcsType detectVcs(String dirPath) {
        try {
            ProcessBuilder pb = new ProcessBuilder("git", "rev-parse", "--git-dir");
            pb.directory(new File(dirPath));
            Process process = pb.start();
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                return VcsType.GIT;
            }
        } catch (Exception e) {
        }

        return VcsType.NONE;
    }

    private static String getLatestGitCommitHash(String dirPath) {
        try {
            ProcessBuilder pb = new ProcessBuilder("git", "rev-parse", "HEAD");
            pb.directory(new File(dirPath));
            Process process = pb.start();

            try (BufferedReader reader =
                    new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String hash = reader.readLine();
                int exitCode = process.waitFor();
                if (exitCode == 0 && hash != null && !hash.isEmpty()) {
                    return hash.trim();
                }
            }
        } catch (Exception e) {
            log.debug("获取 {} 的 git 提交哈希失败: {}", dirPath, e.getMessage());
        }
        return null;
    }

    public List<WorkspaceRoot> getRoots() {
        return new ArrayList<>(roots);
    }

    public WorkspaceRoot getPrimaryRoot() {
        if (primaryIndex >= 0 && primaryIndex < roots.size()) {
            return roots.get(primaryIndex);
        }
        return roots.getFirst();
    }

    public void setPrimaryIndex(int index) {
        if (index >= 0 && index < roots.size()) {
            this.primaryIndex = index;
        }
    }

    public WorkspaceRoot resolvePathToRoot(String absolutePath) {
        if (absolutePath == null || absolutePath.isEmpty() || roots == null || roots.isEmpty()) {
            return null;
        }

        Path normalizedAbsolutePath = Paths.get(absolutePath).normalize().toAbsolutePath();

        List<WorkspaceRoot> sortedRoots = new ArrayList<>(roots);
        sortedRoots.sort(
                (a, b) -> {
                    if (a == null || b == null || a.getPath() == null || b.getPath() == null) {
                        return 0;
                    }
                    return Integer.compare(b.getPath().length(), a.getPath().length());
                });

        for (WorkspaceRoot root : sortedRoots) {
            if (root == null || root.getPath() == null) {
                continue;
            }

            try {
                Path normalizedRootPath = Paths.get(root.getPath()).normalize().toAbsolutePath();
                if (normalizedAbsolutePath.startsWith(normalizedRootPath)) {
                    return root;
                }
            } catch (Exception e) {
                log.debug("比较路径时出错: {} vs {}", absolutePath, root.getPath(), e);
                if (absolutePath.startsWith(root.getPath())) {
                    return root;
                }
            }
        }

        return null;
    }

    public WorkspaceRoot getRootByName(String name) {
        return roots.stream().filter(r -> name.equals(r.getName())).findFirst().orElse(null);
    }

    public WorkspaceRoot getRootByIndex(int index) {
        if (index >= 0 && index < roots.size()) {
            return roots.get(index);
        }
        return null;
    }

    public boolean isPathInWorkspace(String absolutePath) {
        return resolvePathToRoot(absolutePath) != null;
    }

    public String getRelativePathFromRoot(String absolutePath) {
        return getRelativePathFromRoot(absolutePath, null);
    }

    public String getRelativePathFromRoot(String absolutePath, WorkspaceRoot root) {
        WorkspaceRoot targetRoot = root != null ? root : resolvePathToRoot(absolutePath);
        if (targetRoot == null) {
            return null;
        }

        Path rootPath = Paths.get(targetRoot.getPath());
        Path absPath = Paths.get(absolutePath);
        return rootPath.relativize(absPath).toString();
    }

    public WorkspaceContext createContext() {
        return createContext(null);
    }

    public WorkspaceContext createContext(WorkspaceRoot currentRoot) {
        WorkspaceRoot primary = getPrimaryRoot();
        if (primary == null && !roots.isEmpty()) {
            primary = roots.get(0);
        }
        WorkspaceRoot current = currentRoot != null ? currentRoot : primary;

        return WorkspaceContext.builder()
                .workspaceRoots(getRoots())
                .primaryRoot(primary)
                .currentRoot(current)
                .build();
    }

    public Map<String, Object> toJSON() {
        Map<String, Object> data = new HashMap<>();
        data.put("roots", roots);
        data.put("primaryIndex", primaryIndex);
        return data;
    }

    @SuppressWarnings("unchecked")
    public static WorkspaceRootManager fromJSON(Map<String, Object> data) {
        List<WorkspaceRoot> roots = (List<WorkspaceRoot>) data.get("roots");
        Integer primaryIndex = (Integer) data.get("primaryIndex");
        return new WorkspaceRootManager(roots, primaryIndex != null ? primaryIndex : 0);
    }

    public String getSummary() {
        if (roots.isEmpty()) {
            return "未配置工作区根目录";
        }

        if (roots.size() == 1) {
            WorkspaceRoot root = roots.get(0);
            return "单工作区: " + (root.getName() != null ? root.getName() : root.getPath());
        }

        WorkspaceRoot primary = getPrimaryRoot();
        String primaryName =
                primary != null
                        ? (primary.getName() != null ? primary.getName() : primary.getPath())
                        : "未知";

        String additional =
                roots.stream()
                        .filter(r -> roots.indexOf(r) != primaryIndex)
                        .map(
                                r ->
                                        r.getName() != null
                                                ? r.getName()
                                                : Paths.get(r.getPath()).getFileName().toString())
                        .collect(Collectors.joining(", "));

        return String.format(
                "多工作区 (%d 个根目录)\n主工作区: %s\n附加工作区: %s", roots.size(), primaryName, additional);
    }

    public boolean isSingleRoot() {
        return roots.size() == 1;
    }

    public WorkspaceRoot getSingleRoot() {
        if (roots.size() != 1) {
            throw new IllegalStateException("期望单个根目录，但找到了 " + roots.size() + " 个根目录");
        }
        return roots.getFirst();
    }

    public void updateCommitHashes() {
        if (roots == null || roots.isEmpty()) {
            return;
        }
        for (WorkspaceRoot root : roots) {
            if (root != null && root.getVcs() == VcsType.GIT) {
                String gitHash = getLatestGitCommitHash(root.getPath());
                root.setCommitHash(gitHash);
            }
        }
    }

    public String buildWorkspacesJson() {
        if (roots == null || roots.isEmpty()) {
            return null;
        }

        Map<String, Map<String, Object>> workspaces = new HashMap<>();

        for (WorkspaceRoot root : roots) {
            if (root == null || root.getPath() == null) {
                continue;
            }

            try {
                String hint =
                        root.getName() != null && !root.getName().isEmpty()
                                ? root.getName()
                                : Paths.get(root.getPath()).getFileName().toString();
                List<String> gitRemotes = getGitRemoteUrls(root.getPath());
                String gitCommitHash = getLatestGitCommitHash(root.getPath());

                Map<String, Object> workspaceInfo = new HashMap<>();
                workspaceInfo.put("hint", hint);

                if (gitRemotes != null && !gitRemotes.isEmpty()) {
                    workspaceInfo.put("associatedRemoteUrls", gitRemotes);
                }

                if (gitCommitHash != null && !gitCommitHash.isEmpty()) {
                    workspaceInfo.put("latestGitCommitHash", gitCommitHash);
                }

                workspaces.put(root.getPath(), workspaceInfo);
            } catch (Exception e) {
                log.debug("构建工作区 JSON 时跳过根目录 {}: {}", root.getPath(), e.getMessage());
            }
        }

        if (workspaces.isEmpty()) {
            return null;
        }

        try {
            Map<String, Object> result = new HashMap<>();
            result.put("workspaces", workspaces);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
        } catch (JsonProcessingException e) {
            log.error("序列化工作区 JSON 失败", e);
            return null;
        }
    }

    private static List<String> getGitRemoteUrls(String dirPath) {
        List<String> remotes = new ArrayList<>();
        try {
            ProcessBuilder pb = new ProcessBuilder("git", "remote", "-v");
            pb.directory(new File(dirPath));
            Process process = pb.start();

            try (BufferedReader reader =
                    new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("(fetch)")) {
                        Pattern pattern = Pattern.compile("^(\\S+)\\s+(\\S+)\\s+\\(fetch\\)$");
                        Matcher matcher = pattern.matcher(line);
                        if (matcher.matches()) {
                            String remoteName = matcher.group(1);
                            String remoteUrl = matcher.group(2);
                            String formatted = remoteName + ": " + remoteUrl;
                            if (!remotes.contains(formatted)) {
                                remotes.add(formatted);
                            }
                        }
                    }
                }
            }
            process.waitFor();
        } catch (Exception e) {
            log.debug("获取 {} 的 git 远程地址失败: {}", dirPath, e.getMessage());
        }
        return remotes;
    }
}
