package com.hhoa.kline.core.core.workspace;

import com.hhoa.kline.core.core.shared.multiroot.WorkspaceRoot;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * WorkspacePathAdapter - 多工作区路径解析工具
 *
 * <p>所有路径解析都基于 {@link WorkspaceRootManager}，默认启用多根模式。
 */
@Slf4j
public class WorkspacePathAdapter {

    private final WorkspaceConfig config;

    public WorkspacePathAdapter(WorkspaceConfig config) {
        this.config = config;
    }

    /**
     * 使用单根或多根逻辑解析路径
     *
     * @param relativePath 要解析的路径（可以是相对路径或绝对路径）
     * @param workspaceHint 要使用的工作区的可选提示（名称或路径）
     * @return 解析后的绝对路径
     */
    public String resolvePath(String relativePath, String workspaceHint) {
        WorkspaceRootManager manager = requireManager();
        String candidate = normalizeRelativePath(relativePath);

        if (isAbsolute(candidate)) {
            WorkspaceRoot root = manager.resolvePathToRoot(candidate);
            return candidate;
        }

        WorkspaceRoot root = selectWorkspaceRoot(manager, workspaceHint);
        if (root == null) {
            throw new IllegalStateException("未找到可用的工作区根目录");
        }

        if (candidate.isEmpty()) {
            return root.getPath();
        }
        return Paths.get(root.getPath()).resolve(candidate).toString();
    }

    /**
     * 使用单根或多根逻辑解析路径（无工作区提示）
     *
     * @param relativePath 要解析的路径（可以是相对路径或绝对路径）
     * @return 解析后的绝对路径
     */
    public String resolvePath(String relativePath) {
        return resolvePath(relativePath, null);
    }

    /**
     * 获取跨所有工作区的相对路径的所有可能路径 适用于搜索操作或检查文件是否存在于任何工作区中
     *
     * @param relativePath 要解析的相对路径
     * @return 绝对路径数组，每个工作区一个
     */
    public List<String> getAllPossiblePaths(String relativePath) {
        WorkspaceRootManager manager = requireManager();
        String candidate = normalizeRelativePath(relativePath);

        if (isAbsolute(candidate)) {
            return List.of(candidate);
        }

        return manager.getRoots().stream()
                .map(
                        root ->
                                candidate.isEmpty()
                                        ? root.getPath()
                                        : Paths.get(root.getPath()).resolve(candidate).toString())
                .collect(Collectors.toList());
    }

    /**
     * 确定给定的绝对路径属于哪个工作区
     *
     * @param absolutePath 要检查的绝对路径
     * @return 包含此路径的工作区根目录，如果不在任何工作区中则返回 null
     */
    public WorkspaceInfo getWorkspaceForPath(String absolutePath) {
        WorkspaceRootManager manager = requireManager();
        WorkspaceRoot root = manager.resolvePathToRoot(absolutePath);
        if (root != null) {
            return new WorkspaceInfo(
                    root.getName() != null
                            ? root.getName()
                            : Paths.get(root.getPath()).getFileName().toString(),
                    root.getPath());
        }

        return null;
    }

    /**
     * 从适当的工作区根目录获取相对路径
     *
     * @param absolutePath 要转换为相对路径的绝对路径
     * @return 从其工作区根目录开始的相对路径，如果不在工作区中则返回原始路径
     */
    public String getRelativePath(String absolutePath) {
        WorkspaceRootManager manager = requireManager();
        String relativePath = manager.getRelativePathFromRoot(absolutePath);
        return relativePath != null ? relativePath : absolutePath;
    }

    /**
     * 检查是否启用了多根模式
     *
     * @return 如果启用并配置了多根模式则返回 true
     */
    public boolean isMultiRootEnabled() {
        WorkspaceRootManager manager = requireManager();
        return manager.getRoots().size() > 1;
    }

    /**
     * 获取所有工作区根目录
     *
     * @return 工作区根目录信息数组
     */
    public List<WorkspaceInfo> getWorkspaceRoots() {
        WorkspaceRootManager manager = requireManager();
        return manager.getRoots().stream()
                .map(
                        root ->
                                new WorkspaceInfo(
                                        root.getName() != null
                                                ? root.getName()
                                                : Paths.get(root.getPath())
                                                        .getFileName()
                                                        .toString(),
                                        root.getPath()))
                .collect(Collectors.toList());
    }

    /**
     * 获取主工作区根目录
     *
     * @return 主工作区根目录信息
     */
    public WorkspaceInfo getPrimaryWorkspace() {
        WorkspaceRootManager manager = requireManager();
        WorkspaceRoot primaryRoot = manager.getPrimaryRoot();
        if (primaryRoot == null) {
            List<WorkspaceRoot> roots = manager.getRoots();
            primaryRoot = roots.isEmpty() ? null : roots.getFirst();
        }

        if (primaryRoot == null) {
            throw new IllegalStateException("未配置任何工作区根目录");
        }

        return new WorkspaceInfo(
                primaryRoot.getName() != null
                        ? primaryRoot.getName()
                        : Paths.get(primaryRoot.getPath()).getFileName().toString(),
                primaryRoot.getPath());
    }

    public record WorkspaceInfo(String name, String path) {}

    /**
     * 创建 WorkspacePathAdapter 的工厂方法
     *
     * @param config 适配器配置
     * @return 新的 WorkspacePathAdapter 实例
     */
    public static WorkspacePathAdapter createWorkspacePathAdapter(WorkspaceConfig config) {
        return new WorkspacePathAdapter(config);
    }

    private WorkspaceRootManager requireManager() {
        WorkspaceRootManager manager = config.workspaceManager();
        if (manager == null) {
            throw new IllegalStateException("workspaceManager 未配置，无法执行路径解析");
        }
        return manager;
    }

    private static String normalizeRelativePath(String relativePath) {
        return relativePath == null ? "" : relativePath;
    }

    private static boolean isAbsolute(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        try {
            return Paths.get(path).isAbsolute();
        } catch (Exception e) {
            return false;
        }
    }

    private WorkspaceRoot selectWorkspaceRoot(WorkspaceRootManager manager, String workspaceHint) {
        if (workspaceHint != null && !workspaceHint.isEmpty()) {
            WorkspaceRoot root = manager.getRootByName(workspaceHint);
            if (root != null) {
                return root;
            }
            return manager.getRoots().stream()
                    .filter(
                            r ->
                                    r.getPath().equals(workspaceHint)
                                            || r.getPath().contains(workspaceHint))
                    .findFirst()
                    .orElse(null);
        }
        return manager.getPrimaryRoot();
    }
}
