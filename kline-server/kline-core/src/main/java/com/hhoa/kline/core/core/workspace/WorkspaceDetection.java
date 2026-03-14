package com.hhoa.kline.core.core.workspace;

import com.hhoa.kline.core.core.shared.multiroot.VcsType;
import com.hhoa.kline.core.core.shared.multiroot.WorkspaceRoot;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * WorkspaceDetection - 工作区和版本控制系统检测工具
 *
 * <p>提供检测工作区根目录和 VCS 类型的功能
 */
@Slf4j
public class WorkspaceDetection {

    /**
     * 检测给定目录路径的 VCS 类型
     *
     * @param dirPath 目录路径
     * @return VCS 类型
     */
    public static VcsType detectVcs(String dirPath) {
        try {
            if (isGitRepository(dirPath)) {
                return VcsType.GIT;
            }
            return VcsType.NONE;
        } catch (Exception e) {
            log.debug("检测 VCS 失败: {}", e.getMessage());
            return VcsType.NONE;
        }
    }

    /**
     * 检查目录是否是 Git 仓库
     *
     * @param dirPath 目录路径
     * @return 如果是 Git 仓库则返回 true
     */
    private static boolean isGitRepository(String dirPath) {
        try {
            ProcessBuilder pb = new ProcessBuilder("git", "rev-parse", "--git-dir");
            pb.directory(new File(dirPath));
            Process process = pb.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 从主机编辑器（VS Code 等）检测工作区根目录 当没有工作区文件夹时回退到当前工作目录
     *
     * <p>注意：此方法需要与具体的主机提供者集成 在实际使用中需要注入工作区路径提供者
     *
     * @param workspacePaths 工作区路径列表（从主机提供者获取）
     * @return 工作区根目录列表
     */
    public static List<WorkspaceRoot> detectWorkspaceRoots(List<String> workspacePaths) {

        List<WorkspaceRoot> roots = new ArrayList<>();
        for (String workspacePath : workspacePaths) {
            VcsType vcs = detectVcs(workspacePath);
            String commitHash = null;
            if (vcs == VcsType.GIT) {
                commitHash = getLatestGitCommitHash(workspacePath);
            }

            roots.add(
                    WorkspaceRoot.builder()
                            .path(workspacePath)
                            .name(Paths.get(workspacePath).getFileName().toString())
                            .vcs(vcs)
                            .commitHash(commitHash)
                            .build());
        }

        return roots;
    }

    /**
     * 获取目录的最新 Git 提交哈希
     *
     * @param dirPath 目录路径
     * @return Git 提交哈希，如果失败则返回 null
     */
    private static String getLatestGitCommitHash(String dirPath) {
        try {
            ProcessBuilder pb = new ProcessBuilder("git", "rev-parse", "HEAD");
            pb.directory(new File(dirPath));
            Process process = pb.start();

            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(process.getInputStream()));
            String hash = reader.readLine();
            int exitCode = process.waitFor();

            if (exitCode == 0 && hash != null && !hash.isEmpty()) {
                return hash.trim();
            }
        } catch (Exception e) {
            log.debug("获取 Git 提交哈希失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 检查当前工作区是否打开了多个根文件夹 这是一个轻量级检查，仅计算工作区文件夹数量， 独立于功能标志或内部多根实现状态
     *
     * <p>当您需要了解实际工作区状态（例如，用于遥测、标题或 UI 显示）时使用此方法， 而不是多根功能是否启用
     *
     * @param workspacePaths 工作区路径列表
     * @return 如果打开了 2 个或更多工作区文件夹则返回 true，否则返回 false
     */
    public static boolean isMultiRootWorkspace(List<String> workspacePaths) {
        try {
            return workspacePaths != null && workspacePaths.size() > 1;
        } catch (Exception e) {
            log.error("检测多根工作区失败", e);
            return false;
        }
    }
}
