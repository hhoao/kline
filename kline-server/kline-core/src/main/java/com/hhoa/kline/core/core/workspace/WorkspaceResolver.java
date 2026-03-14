package com.hhoa.kline.core.core.workspace;

import com.hhoa.kline.core.core.shared.multiroot.WorkspaceRoot;
import com.hhoa.kline.core.core.workspace.utils.ParsedWorkspacePath;
import com.hhoa.kline.core.core.workspace.utils.WorkspacePathParser;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * 带有迁移跟踪的工作区路径解析，用于多工作区支持
 *
 * <p>阶段 0：充当跟踪器以识别所有单根路径操作 阶段 1+：将处理多根路径解析
 */
@Slf4j
public class WorkspaceResolver {

    private static final WorkspaceResolver INSTANCE = new WorkspaceResolver();

    private static final int MAX_EXAMPLE_PATHS = 5;

    private final Map<String, UsageStats> usageMap = new HashMap<>();
    private final boolean traceEnabled;

    public WorkspaceResolver() {
        String multiRootTrace = System.getenv("MULTI_ROOT_TRACE");
        String nodeEnv = System.getenv("NODE_ENV");
        this.traceEnabled = "true".equals(multiRootTrace) || "development".equals(nodeEnv);
    }

    /**
     * 获取单例实例
     *
     * @return WorkspaceResolver 单例实例
     */
    public static WorkspaceResolver getInstance() {
        return INSTANCE;
    }

    /**
     * 增强的工作区路径解析，处理单根和多根工作区
     *
     * @param config 配置对象
     * @param relativePath 要解析的相对路径
     * @param context 用于跟踪使用情况的组件/处理程序名称
     * @return 字符串（向后兼容）或 WorkspacePathResult 对象
     */
    public static WorkspacePathResult resolveWorkspacePath(
            WorkspaceConfig config, String relativePath, String context) {
        if (config == null) {
            throw new IllegalArgumentException("workspaceConfig 不能为空");
        }
        WorkspaceRootManager manager = config.workspaceManager();
        if (manager == null) {
            throw new IllegalStateException("workspaceManager 未配置，无法解析工作区路径");
        }
        ParsedWorkspacePath parsed = WorkspacePathParser.parseWorkspaceInlinePath(relativePath);

        WorkspacePathAdapter adapter = new WorkspacePathAdapter(config);

        String absolutePath = adapter.resolvePath(parsed.getRelPath(), parsed.getWorkspaceHint());

        String displayPath =
                parsed.getWorkspaceHint() != null && !parsed.getWorkspaceHint().isEmpty()
                        ? "@" + parsed.getWorkspaceHint() + ":" + parsed.getRelPath()
                        : parsed.getRelPath();

        WorkspaceRoot matchedRoot = manager.resolvePathToRoot(absolutePath);

        return new WorkspacePathResult(absolutePath, displayPath, parsed.getRelPath(), matchedRoot);
    }

    /**
     * 检查是否处于跟踪模式的辅助方法
     *
     * @return 如果启用了跟踪模式则返回 true
     */
    public static boolean isWorkspaceTraceEnabled() {
        String multiRootTrace = System.getenv("MULTI_ROOT_TRACE");
        String nodeEnv = System.getenv("NODE_ENV");
        return "true".equals(multiRootTrace) || "development".equals(nodeEnv);
    }

    /**
     * 阶段 0：带有跟踪的 path.basename 的便捷函数 这是我们将用来替换现有 path.basename() 调用的函数
     *
     * @param filePath 要获取基本名称的文件路径
     * @param context 用于跟踪使用情况的组件/处理程序名称
     * @return 路径的基本名称
     */
    public static String getWorkspaceBasename(String filePath, String context) {
        return INSTANCE.getBasename(filePath, context);
    }

    /**
     * 获取工作区基本名称（无上下文跟踪）
     *
     * @param filePath 要获取基本名称的文件路径
     * @return 路径的基本名称
     */
    public static String getWorkspaceBasename(String filePath) {
        return INSTANCE.getBasename(filePath, null);
    }

    /**
     * 跟踪给定上下文和路径的使用统计信息
     *
     * @param context 用于跟踪使用情况的组件/处理程序名称
     * @param examplePath 要作为示例跟踪的路径
     */
    private void trackUsage(String context, String examplePath) {
        UsageStats stats =
                usageMap.computeIfAbsent(
                        context,
                        k -> {
                            UsageStats newStats = new UsageStats();
                            newStats.setCount(0);
                            newStats.setExamples(new ArrayList<>());
                            newStats.setLastUsed(LocalDateTime.now());
                            return newStats;
                        });

        stats.setCount(stats.getCount() + 1);
        stats.setLastUsed(LocalDateTime.now());

        if (stats.getExamples().size() < MAX_EXAMPLE_PATHS
                && !stats.getExamples().contains(examplePath)) {
            stats.getExamples().add(examplePath);
        }
    }

    /**
     * 获取用于外部分析的原始使用统计信息
     *
     * @return 组件名称到其使用统计信息的映射
     */
    public Map<String, UsageStats> getUsageStats() {
        return new HashMap<>(usageMap);
    }

    public void clearUsageStats() {
        usageMap.clear();
    }

    public Map<String, UsageStats> exportUsageData() {
        return new HashMap<>(usageMap);
    }

    /**
     * 阶段 0：带跟踪的获取基本名称的实例方法 阶段 1+：将处理多工作区路径的基本名称
     *
     * @param filePath 要从中获取基本名称的文件路径
     * @param context 用于跟踪使用情况的组件/处理程序名称
     * @return 路径的基本名称
     */
    public String getBasename(String filePath, String context) {
        if (filePath == null || filePath.isEmpty()) {
            return "";
        }

        if (context != null && !context.isEmpty()) {
            trackUsage(context, filePath);

            if (traceEnabled) {
                log.debug("[MULTI-ROOT-TRACE] {}: 正在获取 \"{}\" 的基本名称", context, filePath);
            }
        }

        try {
            Path path = Paths.get(filePath);
            Path fileName = path.getFileName();
            return fileName != null ? fileName.toString() : "";
        } catch (Exception e) {
            log.debug("获取路径基本名称失败: {}", filePath, e);
            int lastSeparator = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));
            return lastSeparator >= 0 && lastSeparator < filePath.length() - 1
                    ? filePath.substring(lastSeparator + 1)
                    : filePath;
        }
    }

    public boolean isTraceEnabled() {
        return traceEnabled;
    }

    public record WorkspacePathResult(
            String absolutePath, String displayPath, String resolvedPath, WorkspaceRoot root) {

        @Override
        public String toString() {
            return "WorkspacePathResult{"
                    + "absolutePath='"
                    + absolutePath
                    + '\''
                    + ", displayPath='"
                    + displayPath
                    + '\''
                    + ", resolvedPath='"
                    + resolvedPath
                    + '\''
                    + ", root="
                    + (root != null ? root.getPath() : "null")
                    + '}';
        }
    }
}
