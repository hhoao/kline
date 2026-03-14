package com.hhoa.kline.core.core.services.telemetry;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 * 遥测服务接口 用于收集和报告工具使用情况、性能指标等数据
 *
 * @author hhoa
 */
public interface TelemetryService {
    void captureConversationTurnEvent(String ulid, String providerId, String model, String user);

    void captureSummarizeTask(
            @NotNull String ulid, String modelId, Integer tokensUsed, Integer maxContextWindow);

    /** 工作区上下文信息 */
    class WorkspaceContext {
        /** 是否使用了工作区提示 */
        public boolean usedWorkspaceHint;

        /** 是否解析到非主工作区 */
        public boolean resolvedToNonPrimary;

        /** 解析方法：hint 或 primary_fallback */
        public String resolutionMethod;

        public WorkspaceContext() {}

        public WorkspaceContext(
                boolean usedWorkspaceHint, boolean resolvedToNonPrimary, String resolutionMethod) {
            this.usedWorkspaceHint = usedWorkspaceHint;
            this.resolvedToNonPrimary = resolvedToNonPrimary;
            this.resolutionMethod = resolutionMethod;
        }
    }

    /**
     * 捕获工具使用情况
     *
     * @param taskId 任务 ID
     * @param toolName 工具名称
     * @param modelId 模型 ID
     * @param autoApproved 是否自动批准
     * @param success 工具执行是否成功（TypeScript 版本中使用 success 参数）
     */
    void captureToolUsage(
            String taskId, String toolName, String modelId, boolean autoApproved, boolean success);

    /**
     * 捕获工具使用情况（带工作区上下文）
     *
     * @param taskId 任务 ID
     * @param toolName 工具名称
     * @param modelId 模型 ID
     * @param autoApproved 是否自动批准
     * @param success 工具执行是否成功（TypeScript 版本中使用 success 参数）
     * @param workspaceContext 工作区上下文
     */
    void captureToolUsage(
            String taskId,
            String toolName,
            String modelId,
            boolean autoApproved,
            boolean success,
            WorkspaceContext workspaceContext);

    /**
     * 捕获错误事件
     *
     * @param taskId 任务 ID
     * @param errorType 错误类型
     * @param errorMessage 错误消息
     * @param metadata 额外的元数据
     */
    void captureError(
            String taskId, String errorType, String errorMessage, Map<String, Object> metadata);

    /**
     * 捕获性能指标
     *
     * @param taskId 任务 ID
     * @param metricName 指标名称
     * @param value 指标值
     * @param unit 单位（如 ms, bytes）
     */
    void captureMetric(String taskId, String metricName, double value, String unit);

    /**
     * 捕获用户行为事件
     *
     * @param taskId 任务 ID
     * @param eventName 事件名称
     * @param properties 事件属性
     */
    void captureEvent(String taskId, String eventName, Map<String, Object> properties);

    /**
     * 捕获任务完成事件
     *
     * @param taskId 任务 ID
     */
    void captureTaskCompleted(String taskId);

    /**
     * 捕获选项选择事件
     *
     * @param taskId 任务 ID
     * @param optionCount 选项数量
     * @param action 动作类型
     */
    void captureOptionSelected(String taskId, int optionCount, String action);

    /**
     * 捕获选项忽略事件
     *
     * @param taskId 任务 ID
     * @param optionCount 选项数量
     * @param action 动作类型
     */
    void captureOptionsIgnored(String taskId, int optionCount, String action);

    /**
     * 捕获工作区初始化事件
     *
     * @param rootCount 根目录数量
     * @param vcsTypes VCS 类型列表
     * @param initDurationMs 初始化持续时间（毫秒），可选
     * @param featureFlagEnabled 功能标志是否启用，可选
     */
    void captureWorkspaceInitialized(
            int rootCount, List<String> vcsTypes, Long initDurationMs, Boolean featureFlagEnabled);

    /**
     * 捕获工作区初始化错误
     *
     * @param error 错误对象
     * @param fallbackMode 是否回退到单根模式
     * @param workspaceCount 工作区数量（可选）
     */
    void captureWorkspaceInitError(Exception error, boolean fallbackMode, Integer workspaceCount);

    /**
     * 捕获工作区路径解析事件
     *
     * @param taskId 任务 ID
     * @param context 发生解析的组件/处理器上下文
     * @param resolutionType 解析类型："hint_provided" | "fallback_to_primary" | "cross_workspace_search"
     * @param hintType 工作区提示类型（可选）："workspace_name" | "workspace_path" | "invalid"
     * @param resolutionSuccess 解析是否成功（可选）
     * @param targetWorkspaceIndex 解析到的工作区索引（可选，0=主工作区，1=次工作区等）
     * @param isMultiRootEnabled 是否启用多根模式（可选）
     */
    void captureWorkspacePathResolved(
            String taskId,
            String context,
            String resolutionType,
            String hintType,
            Boolean resolutionSuccess,
            Integer targetWorkspaceIndex,
            Boolean isMultiRootEnabled);

    /**
     * 捕获多工作区搜索模式和性能
     *
     * @param taskId 任务 ID
     * @param searchType 搜索类型："targeted" | "cross_workspace" | "primary_only"
     * @param workspaceCount 搜索的工作区数量
     * @param hintProvided 是否提供了工作区提示
     * @param resultsFound 是否找到搜索结果
     * @param searchDurationMs 搜索持续时间（毫秒，可选）
     */
    void captureWorkspaceSearchPattern(
            String taskId,
            String searchType,
            int workspaceCount,
            boolean hintProvided,
            boolean resultsFound,
            Long searchDurationMs);

    /**
     * 捕获 Focus Chain 列表被写入事件
     *
     * @param taskId 任务 ID
     */
    void captureFocusChainListWritten(String taskId);

    /**
     * 捕获 Focus Chain 首次进度创建事件
     *
     * @param taskId 任务 ID
     * @param totalItems 总项目数
     */
    void captureFocusChainProgressFirst(String taskId, int totalItems);

    /**
     * 捕获 Focus Chain 进度更新事件
     *
     * @param taskId 任务 ID
     * @param totalItems 总项目数
     * @param completedItems 已完成项目数
     */
    void captureFocusChainProgressUpdate(String taskId, int totalItems, int completedItems);

    /**
     * 捕获任务完成时 Focus Chain 未完成项目事件
     *
     * @param taskId 任务 ID
     * @param totalItems 总项目数
     * @param completedItems 已完成项目数
     * @param incompleteItems 未完成项目数
     */
    void captureFocusChainIncompleteOnCompletion(
            String taskId, int totalItems, int completedItems, int incompleteItems);

    /**
     * 捕获 Focus Chain 开关切换事件
     *
     * @param isEnabled 是否启用
     */
    void captureFocusChainToggle(boolean isEnabled);

    /**
     * 捕获 Focus Chain 列表文件打开事件
     *
     * @param taskId 任务 ID
     */
    void captureFocusChainListOpened(String taskId);

    /**
     * 捕获 diff 编辑失败事件（replace_in_file 操作失败）
     *
     * @param taskId 任务 ID
     * @param modelId 模型 ID
     * @param errorType 错误类型（可选，如 "search_not_found", "invalid_format"）
     */
    void captureDiffEditFailure(String taskId, String modelId, String errorType);

    /**
     * 捕获 MCP 工具调用事件
     *
     * @param taskId 任务 ID
     * @param serverName MCP 服务器名称
     * @param toolName 工具名称
     * @param status 状态："started" | "success" | "error"
     * @param errorMessage 错误消息（可选）
     * @param argumentKeys 参数键列表（可选）
     */
    void captureMcpToolCall(
            String taskId,
            String serverName,
            String toolName,
            String status,
            String errorMessage,
            List<String> argumentKeys);

    /**
     * 捕获检查点使用事件（git-based checkpoint system）
     *
     * @param taskId 任务 ID
     * @param action 检查点操作类型："shadow_git_initialized" | "commit_created" | "restored" |
     *     "diff_generated"
     * @param durationMs 操作持续时间（毫秒，可选）
     */
    void captureCheckpointUsage(String taskId, String action, Long durationMs);

    /**
     * 捕获模型选择事件
     *
     * @param model 模型名称
     * @param provider 模型提供者
     * @param taskId 任务 ID（可选）
     */
    void captureModelSelected(String model, String provider, String taskId);

    /**
     * 捕获浏览器工具启动事件
     *
     * @param taskId 任务 ID
     * @param viewport 视口设置
     * @param isRemote 是否使用远程浏览器
     * @param remoteBrowserHost 远程浏览器主机（可选）
     */
    void captureBrowserToolStart(
            String taskId, String viewport, boolean isRemote, String remoteBrowserHost);

    /**
     * 捕获浏览器工具完成事件
     *
     * @param taskId 任务 ID
     * @param actionCount 操作数量
     * @param duration 持续时间（毫秒）
     * @param actions 操作列表（可选）
     */
    void captureBrowserToolEnd(String taskId, int actionCount, long duration, List<String> actions);

    /**
     * 捕获浏览器错误事件
     *
     * @param taskId 任务 ID
     * @param errorType 错误类型（如 "launch_error", "connection_error", "navigation_error"）
     * @param errorMessage 错误消息
     * @param context 上下文信息（可选，包含 action, url, isRemote, remoteBrowserHost, endpoint）
     */
    void captureBrowserError(
            String taskId, String errorType, String errorMessage, Map<String, Object> context);

    /**
     * 捕获 Gemini API 性能指标
     *
     * @param taskId 任务 ID
     * @param modelId Gemini 模型 ID
     * @param ttftSec 首次标记时间（秒，可选）
     * @param totalDurationSec 总持续时间（秒，可选）
     * @param promptTokens 提示令牌数
     * @param outputTokens 输出令牌数
     * @param cacheReadTokens 缓存读取令牌数
     * @param cacheHit 是否缓存命中
     * @param cacheHitPercentage 缓存命中率（可选）
     * @param apiSuccess API 是否成功
     * @param apiError API 错误消息（可选）
     * @param throughputTokensPerSec 吞吐量（令牌/秒，可选）
     */
    void captureGeminiApiPerformance(
            String taskId,
            String modelId,
            Double ttftSec,
            Double totalDurationSec,
            int promptTokens,
            int outputTokens,
            int cacheReadTokens,
            boolean cacheHit,
            Double cacheHitPercentage,
            boolean apiSuccess,
            String apiError,
            Double throughputTokensPerSec);

    /**
     * 捕获模型收藏使用事件
     *
     * @param model 模型名称
     * @param isFavorited 是否被收藏（true=收藏，false=取消收藏）
     */
    void captureModelFavoritesUsage(String model, boolean isFavorited);

    /**
     * 捕获按钮点击事件
     *
     * @param button 按钮名称
     * @param taskId 任务 ID（可选）
     */
    void captureButtonClick(String button, String taskId);

    /**
     * 捕获 API 提供者错误事件
     *
     * @param taskId 任务 ID
     * @param model 模型标识符
     * @param errorMessage 详细错误消息
     * @param provider 提供者（可选）
     * @param errorStatus HTTP 错误状态码（可选）
     * @param requestId 请求 ID（可选）
     */
    void captureProviderApiError(
            String taskId,
            String model,
            String errorMessage,
            String provider,
            Integer errorStatus,
            String requestId);

    /**
     * 捕获斜杠命令使用事件
     *
     * @param taskId 任务 ID
     * @param commandName 命令名称（如 "newtask", "reportbug" 或自定义工作流名称）
     * @param commandType 命令类型："builtin" | "workflow"
     */
    void captureSlashCommandUsed(String taskId, String commandName, String commandType);

    /**
     * 捕获 Cline 规则切换事件
     *
     * @param taskId 任务 ID（用于在任务上下文中跟踪规则更改）
     * @param ruleFileName 规则文件名（已清理，排除完整路径）
     * @param enabled 是否启用（true=启用，false=禁用）
     * @param isGlobal 是否为全局规则（true=全局，false=工作区特定）
     */
    void captureClineRuleToggled(
            String taskId, String ruleFileName, boolean enabled, boolean isGlobal);

    /**
     * 捕获自动压缩切换事件
     *
     * @param taskId 任务 ID
     * @param enabled 是否启用（true=启用，false=禁用）
     * @param modelId 切换时使用的模型 ID
     */
    void captureAutoCondenseToggle(String taskId, boolean enabled, String modelId);

    /**
     * 捕获 YOLO 模式切换事件
     *
     * @param taskId 任务 ID
     * @param enabled 是否启用（true=启用，false=禁用）
     */
    void captureYoloModeToggle(String taskId, boolean enabled);

    /**
     * 捕获任务初始化事件
     *
     * @param taskId 任务 ID（时间戳，任务创建时的毫秒数）
     * @param ulid 唯一标识符
     * @param durationMs 初始化持续时间（毫秒）
     * @param hasCheckpoints 此任务是否启用了检查点
     */
    void captureTaskInitialization(
            String ulid, String taskId, long durationMs, boolean hasCheckpoints);

    /** 捕获规则菜单打开事件 */
    void captureRulesMenuOpened();

    /**
     * 捕获终端命令执行结果
     *
     * @param success 是否成功捕获命令输出
     * @param method 捕获方法："shell_integration" | "clipboard" | "none"
     */
    void captureTerminalExecution(boolean success, String method);

    /**
     * 捕获终端输出捕获失败事件
     *
     * @param reason 失败原因（如 "timeout", "no_shell_integration", "clipboard_failed"）
     */
    void captureTerminalOutputFailure(String reason);

    /**
     * 捕获终端用户干预事件
     *
     * @param action 用户操作（如 "process_while_running", "manual_paste", "cancelled"）
     */
    void captureTerminalUserIntervention(String action);

    /**
     * 捕获终端挂起事件
     *
     * @param stage 挂起发生的阶段（如 "waiting_for_completion", "buffer_stuck", "stream_timeout"）
     */
    void captureTerminalHang(String stage);

    /**
     * 捕获多根检查点操作事件
     *
     * @param taskId 任务标识符
     * @param action 检查点操作类型："initialized" | "committed" | "restored"
     * @param rootCount 正在检查点的根数
     * @param successCount 成功检查点数量
     * @param failureCount 失败检查点数量
     * @param durationMs 总操作持续时间（毫秒，可选）
     */
    void captureMultiRootCheckpoint(
            String taskId,
            String action,
            int rootCount,
            int successCount,
            int failureCount,
            Long durationMs);

    /**
     * 捕获 Mention 使用事件（成功使用并检索内容）
     *
     * @param mentionType Mention 类型："file" | "folder" | "url" | "problems" | "terminal" |
     *     "git-changes" | "commit"
     * @param contentLength 检索的内容长度（可选，用于大小跟踪）
     */
    void captureMentionUsed(String mentionType, Integer contentLength);

    /**
     * 捕获 Mention 失败事件（无法检索内容）
     *
     * @param mentionType 失败的 Mention 类型
     * @param errorType 错误类别："not_found" | "permission_denied" | "network_error" | "parse_error" |
     *     "unknown"
     * @param errorMessage 错误消息（可选，将被截断）
     */
    void captureMentionFailed(String mentionType, String errorType, String errorMessage);

    /**
     * 捕获 Mention 搜索结果事件（用户在 Mention 下拉菜单中搜索文件/文件夹时）
     *
     * @param queryLength 搜索查询长度
     * @param resultCount 返回的结果数量
     * @param searchType 搜索类型："file" | "folder" | "all"
     * @param isEmpty 搜索是否返回无结果
     */
    void captureMentionSearchResults(
            int queryLength, int resultCount, String searchType, boolean isEmpty);

    /**
     * 捕获 CLI Subagent 切换事件
     *
     * @param enabled 是否启用（true=启用，false=禁用）
     */
    void captureSubagentToggle(boolean enabled);

    /**
     * 捕获 CLI Subagent 执行事件
     *
     * @param taskId 任务 ID
     * @param durationMs Subagent 执行持续时间（毫秒）
     * @param outputLines Subagent 产生的输出行数
     * @param success Subagent 执行是否成功
     */
    void captureSubagentExecution(String taskId, long durationMs, int outputLines, boolean success);

    /** 刷新遥测数据（发送到服务器） */
    void flush();

    /** 启用遥测 */
    void enable();

    /** 禁用遥测 */
    void disable();

    /**
     * 检查遥测是否启用
     *
     * @return 是否启用
     */
    boolean isEnabled();
}
