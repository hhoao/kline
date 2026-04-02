package com.hhoa.kline.core.core.services.telemetry;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;

/**
 * 默认遥测服务实现 提供基本的遥测数据收集和日志记录功能
 *
 * @author hhoa
 */
@Slf4j
public class DefaultTelemetryService implements TelemetryService {

    private final AtomicBoolean enabled = new AtomicBoolean(true);
    private final Map<String, AtomicLong> counters = new ConcurrentHashMap<>();
    private final Map<String, Double> metrics = new ConcurrentHashMap<>();

    @Override
    public void captureConversationTurnEvent(
            String ulid, String providerId, String model, String user) {}

    @Override
    public void captureSummarizeTask(
            String ulid, String modelId, Integer tokensUsed, Integer maxContextWindow) {}

    @Override
    public void captureToolUsage(
            String taskId, String toolName, String modelId, boolean autoApproved, boolean success) {
        captureToolUsage(taskId, toolName, modelId, autoApproved, success, null);
    }

    @Override
    public void captureToolUsage(
            String taskId,
            String toolName,
            String modelId,
            boolean autoApproved,
            boolean success,
            WorkspaceContext workspaceContext) {
        if (!enabled.get()) {
            return;
        }

        try {
            String counterKey =
                    String.format("tool.%s.%s", toolName, success ? "success" : "failed");
            counters.computeIfAbsent(counterKey, k -> new AtomicLong(0)).incrementAndGet();

            Map<String, Object> properties = new HashMap<>();
            properties.put("ulid", taskId);
            properties.put("tool", toolName);
            properties.put("autoApproved", autoApproved);
            properties.put("success", success);
            properties.put("modelId", modelId);

            if (workspaceContext != null) {
                properties.put("workspace_hint_used", workspaceContext.usedWorkspaceHint);
                properties.put(
                        "workspace_resolved_non_primary", workspaceContext.resolvedToNonPrimary);
                properties.put("workspace_resolution_method", workspaceContext.resolutionMethod);
            }

            log.info(
                    "Tool usage: taskId={}, tool={}, model={}, autoApproved={}, success={}, timestamp={}",
                    taskId,
                    toolName,
                    modelId,
                    autoApproved,
                    success,
                    Instant.now());

            if (workspaceContext != null) {
                log.debug(
                        "Workspace context: usedHint={}, nonPrimary={}, method={}",
                        workspaceContext.usedWorkspaceHint,
                        workspaceContext.resolvedToNonPrimary,
                        workspaceContext.resolutionMethod);
            }

            captureEvent(taskId, "tool_used", properties);
        } catch (Exception e) {
            log.error("Failed to capture tool usage", e);
        }
    }

    @Override
    public void captureError(
            String taskId, String errorType, String errorMessage, Map<String, Object> metadata) {
        if (!enabled.get()) {
            return;
        }

        try {
            String counterKey = String.format("error.%s", errorType);
            counters.computeIfAbsent(counterKey, k -> new AtomicLong(0)).incrementAndGet();

            log.error(
                    "Error captured: taskId={}, type={}, message={}, metadata={}, timestamp={}",
                    taskId,
                    errorType,
                    errorMessage,
                    metadata,
                    Instant.now());
        } catch (Exception e) {
            log.error("Failed to capture error", e);
        }
    }

    @Override
    public void captureMetric(String taskId, String metricName, double value, String unit) {
        if (!enabled.get()) {
            return;
        }

        try {
            String metricKey = String.format("metric.%s", metricName);
            metrics.put(metricKey, value);

            log.info(
                    "Metric captured: taskId={}, metric={}, value={}, unit={}, timestamp={}",
                    taskId,
                    metricName,
                    value,
                    unit,
                    Instant.now());
        } catch (Exception e) {
            log.error("Failed to capture metric", e);
        }
    }

    @Override
    public void captureEvent(String taskId, String eventName, Map<String, Object> properties) {
        if (!enabled.get()) {
            return;
        }

        try {
            String counterKey = String.format("event.%s", eventName);
            counters.computeIfAbsent(counterKey, k -> new AtomicLong(0)).incrementAndGet();

            log.info(
                    "Event captured: taskId={}, event={}, properties={}, timestamp={}",
                    taskId,
                    eventName,
                    properties,
                    Instant.now());
        } catch (Exception e) {
            log.error("Failed to capture event", e);
        }
    }

    @Override
    public void captureTaskCompleted(String taskId) {
        if (!enabled.get()) {
            return;
        }

        try {
            log.info("Task completed: taskId={}, timestamp={}", taskId, Instant.now());

            counters.computeIfAbsent("task.completed", k -> new AtomicLong(0)).incrementAndGet();

            Map<String, Object> properties = new HashMap<>();
            properties.put("ulid", taskId);
            captureEvent(taskId, "task_completed", properties);
        } catch (Exception e) {
            log.error("Failed to capture task completed", e);
        }
    }

    @Override
    public void captureOptionSelected(String taskId, int optionCount, String action) {
        if (!enabled.get()) {
            return;
        }

        try {
            log.info(
                    "Option selected: taskId={}, optionCount={}, action={}, timestamp={}",
                    taskId,
                    optionCount,
                    action,
                    Instant.now());

            counters.computeIfAbsent("option.selected", k -> new AtomicLong(0)).incrementAndGet();

            Map<String, Object> properties = new HashMap<>();
            properties.put("ulid", taskId);
            properties.put("qty", optionCount);
            properties.put("mode", action);
            captureEvent(taskId, "option_selected", properties);
        } catch (Exception e) {
            log.error("Failed to capture option selected", e);
        }
    }

    @Override
    public void captureOptionsIgnored(String taskId, int optionCount, String action) {
        if (!enabled.get()) {
            return;
        }

        try {
            log.info(
                    "Options ignored: taskId={}, optionCount={}, action={}, timestamp={}",
                    taskId,
                    optionCount,
                    action,
                    Instant.now());

            counters.computeIfAbsent("option.ignored", k -> new AtomicLong(0)).incrementAndGet();

            Map<String, Object> properties = new HashMap<>();
            properties.put("ulid", taskId);
            properties.put("qty", optionCount);
            properties.put("mode", action);
            captureEvent(taskId, "options_ignored", properties);
        } catch (Exception e) {
            log.error("Failed to capture options ignored", e);
        }
    }

    @Override
    public void captureWorkspaceInitialized(
            int rootCount, List<String> vcsTypes, Long initDurationMs, Boolean featureFlagEnabled) {
        if (!enabled.get()) {
            return;
        }

        try {
            Map<String, Object> properties = new HashMap<>();
            properties.put("root_count", rootCount);
            properties.put("vcs_types", vcsTypes != null ? vcsTypes : Collections.emptyList());
            properties.put("is_multi_root", rootCount > 1);
            if (vcsTypes != null) {
                properties.put(
                        "has_git",
                        vcsTypes.stream()
                                .anyMatch(
                                        vcs ->
                                                vcs != null
                                                        && (vcs.equals("Git")
                                                                || vcs.equalsIgnoreCase("git")
                                                                || vcs.equalsIgnoreCase("GIT"))));
                properties.put(
                        "has_mercurial",
                        vcsTypes.stream()
                                .anyMatch(
                                        vcs ->
                                                vcs != null
                                                        && (vcs.equals("Mercurial")
                                                                || vcs.equalsIgnoreCase("mercurial")
                                                                || vcs.equalsIgnoreCase(
                                                                        "MERCURIAL"))));
            } else {
                properties.put("has_git", false);
                properties.put("has_mercurial", false);
            }

            if (initDurationMs != null) {
                properties.put("init_duration_ms", initDurationMs);
            }

            if (featureFlagEnabled != null) {
                properties.put("feature_flag_enabled", featureFlagEnabled);
            }

            log.info(
                    "Workspace initialized: rootCount={}, vcsTypes={}, durationMs={}, featureFlagEnabled={}, timestamp={}",
                    rootCount,
                    vcsTypes,
                    initDurationMs,
                    featureFlagEnabled,
                    Instant.now());

            counters.computeIfAbsent("workspace.initialized", k -> new AtomicLong(0))
                    .incrementAndGet();

            captureEvent(null, "workspace_initialized", properties);
        } catch (Exception e) {
            log.error("Failed to capture workspace initialized", e);
        }
    }

    @Override
    public void captureWorkspaceInitError(
            Exception error, boolean fallbackMode, Integer workspaceCount) {
        if (!enabled.get()) {
            return;
        }

        try {
            Map<String, Object> properties = new HashMap<>();
            properties.put(
                    "error_type", error != null ? error.getClass().getSimpleName() : "Unknown");
            properties.put(
                    "error_message",
                    error != null
                            ? (error.getMessage() != null
                                    ? error.getMessage()
                                            .substring(
                                                    0, Math.min(500, error.getMessage().length()))
                                    : "")
                            : "");
            properties.put("fallback_to_single_root", fallbackMode);
            properties.put("workspace_count", workspaceCount != null ? workspaceCount : 0);

            log.error(
                    "Workspace init error: errorType={}, fallbackMode={}, workspaceCount={}, timestamp={}",
                    error != null ? error.getClass().getSimpleName() : "Unknown",
                    fallbackMode,
                    workspaceCount,
                    Instant.now());

            counters.computeIfAbsent("workspace.init_error", k -> new AtomicLong(0))
                    .incrementAndGet();

            captureEvent(null, "workspace_init_error", properties);

            if (error != null) {
                captureError(
                        null,
                        "workspace_init",
                        error.getMessage() != null ? error.getMessage() : "Unknown error",
                        properties);
            }
        } catch (Exception e) {
            log.error("Failed to capture workspace init error", e);
        }
    }

    @Override
    public void captureWorkspacePathResolved(
            String taskId,
            String context,
            String resolutionType,
            String hintType,
            Boolean resolutionSuccess,
            Integer targetWorkspaceIndex,
            Boolean isMultiRootEnabled) {
        if (!enabled.get()) {
            return;
        }

        try {
            Map<String, Object> properties = new HashMap<>();
            properties.put("ulid", taskId);
            properties.put("context", context != null ? context : "");
            properties.put("resolution_type", resolutionType != null ? resolutionType : "");

            if (hintType != null) {
                properties.put("hint_type", hintType);
            }

            if (resolutionSuccess != null) {
                properties.put("resolution_success", resolutionSuccess);
            }

            if (targetWorkspaceIndex != null) {
                properties.put("target_workspace_index", targetWorkspaceIndex);
            }

            if (isMultiRootEnabled != null) {
                properties.put("is_multi_root_enabled", isMultiRootEnabled);
            }

            log.debug(
                    "Workspace path resolved: taskId={}, context={}, resolutionType={}, hintType={}, success={}, index={}, multiRoot={}, timestamp={}",
                    taskId,
                    context,
                    resolutionType,
                    hintType,
                    resolutionSuccess,
                    targetWorkspaceIndex,
                    isMultiRootEnabled,
                    Instant.now());

            counters.computeIfAbsent("workspace.path_resolved", k -> new AtomicLong(0))
                    .incrementAndGet();

            captureEvent(taskId, "workspace_path_resolved", properties);
        } catch (Exception e) {
            log.error("Failed to capture workspace path resolved", e);
        }
    }

    @Override
    public void captureWorkspaceSearchPattern(
            String taskId,
            String searchType,
            int workspaceCount,
            boolean hintProvided,
            boolean resultsFound,
            Long searchDurationMs) {
        if (!enabled.get()) {
            return;
        }

        try {
            Map<String, Object> properties = new HashMap<>();
            properties.put("ulid", taskId);
            properties.put("search_type", searchType != null ? searchType : "");
            properties.put("workspace_count", workspaceCount);
            properties.put("hint_provided", hintProvided);
            properties.put("results_found", resultsFound);

            if (searchDurationMs != null) {
                properties.put("search_duration_ms", searchDurationMs);
            }

            log.debug(
                    "Workspace search pattern: taskId={}, searchType={}, workspaceCount={}, hintProvided={}, resultsFound={}, durationMs={}, timestamp={}",
                    taskId,
                    searchType,
                    workspaceCount,
                    hintProvided,
                    resultsFound,
                    searchDurationMs,
                    Instant.now());

            counters.computeIfAbsent("workspace.search_pattern", k -> new AtomicLong(0))
                    .incrementAndGet();

            captureEvent(taskId, "workspace_search_pattern", properties);
        } catch (Exception e) {
            log.error("Failed to capture workspace search pattern", e);
        }
    }

    @Override
    public void flush() {
        if (!enabled.get()) {
            return;
        }

        try {
            log.info("Flushing telemetry data...");

            log.info("=== Telemetry Statistics ===");
            log.info("Counters:");
            counters.forEach((key, value) -> log.info("  {}: {}", key, value.get()));

            log.info("Metrics:");
            metrics.forEach((key, value) -> log.info("  {}: {}", key, value));

            log.info("=== End of Statistics ===");

        } catch (Exception e) {
            log.error("Failed to flush telemetry data", e);
        }
    }

    @Override
    public void enable() {
        enabled.set(true);
        log.info("Telemetry service enabled");
    }

    @Override
    public void disable() {
        enabled.set(false);
        log.info("Telemetry service disabled");
    }

    @Override
    public boolean isEnabled() {
        return enabled.get();
    }

    /**
     * 获取计数器值
     *
     * @param key 计数器键
     * @return 计数值
     */
    public long getCounter(String key) {
        AtomicLong counter = counters.get(key);
        return counter != null ? counter.get() : 0;
    }

    /**
     * 获取指标值
     *
     * @param key 指标键
     * @return 指标值
     */
    public Double getMetric(String key) {
        return metrics.get(key);
    }

    /** 重置所有统计数据 */
    public void reset() {
        counters.clear();
        metrics.clear();
        log.info("Telemetry data reset");
    }

    /**
     * 获取所有计数器
     *
     * @return 计数器映射
     */
    public Map<String, Long> getAllCounters() {
        Map<String, Long> result = new ConcurrentHashMap<>();
        counters.forEach((key, value) -> result.put(key, value.get()));
        return result;
    }

    /**
     * 获取所有指标
     *
     * @return 指标映射
     */
    public Map<String, Double> getAllMetrics() {
        return new ConcurrentHashMap<>(metrics);
    }

    @Override
    public void captureFocusChainListWritten(String taskId) {
        if (!enabled.get()) {
            return;
        }
        try {
            counters.computeIfAbsent("focus_chain_list_written", k -> new AtomicLong(0))
                    .incrementAndGet();
            Map<String, Object> properties = new HashMap<>();
            properties.put("ulid", taskId);
            captureEvent(taskId, "focus_chain_list_written", properties);
        } catch (Exception e) {
            log.error("Failed to capture focus chain list written", e);
        }
    }

    @Override
    public void captureFocusChainProgressFirst(String taskId, int totalItems) {
        if (!enabled.get()) {
            return;
        }
        try {
            counters.computeIfAbsent("focus_chain_progress_first", k -> new AtomicLong(0))
                    .incrementAndGet();
            Map<String, Object> properties = new HashMap<>();
            properties.put("ulid", taskId);
            properties.put("totalItems", totalItems);
            captureEvent(taskId, "focus_chain_progress_first", properties);
        } catch (Exception e) {
            log.error("Failed to capture focus chain progress first", e);
        }
    }

    @Override
    public void captureFocusChainProgressUpdate(String taskId, int totalItems, int completedItems) {
        if (!enabled.get()) {
            return;
        }
        try {
            counters.computeIfAbsent("focus_chain_progress_update", k -> new AtomicLong(0))
                    .incrementAndGet();
            Map<String, Object> properties = new HashMap<>();
            properties.put("ulid", taskId);
            properties.put("totalItems", totalItems);
            properties.put("completedItems", completedItems);
            properties.put(
                    "completionPercentage",
                    totalItems > 0 ? Math.round((completedItems * 100.0) / totalItems) : 0);
            captureEvent(taskId, "focus_chain_progress_update", properties);
        } catch (Exception e) {
            log.error("Failed to capture focus chain progress update", e);
        }
    }

    @Override
    public void captureFocusChainIncompleteOnCompletion(
            String taskId, int totalItems, int completedItems, int incompleteItems) {
        if (!enabled.get()) {
            return;
        }
        try {
            counters.computeIfAbsent("focus_chain_incomplete_on_completion", k -> new AtomicLong(0))
                    .incrementAndGet();
            Map<String, Object> properties = new HashMap<>();
            properties.put("ulid", taskId);
            properties.put("totalItems", totalItems);
            properties.put("completedItems", completedItems);
            properties.put("incompleteItems", incompleteItems);
            properties.put(
                    "completionPercentage",
                    totalItems > 0 ? Math.round((completedItems * 100.0) / totalItems) : 0);
            captureEvent(taskId, "focus_chain_incomplete_on_completion", properties);
        } catch (Exception e) {
            log.error("Failed to capture focus chain incomplete on completion", e);
        }
    }

    @Override
    public void captureFocusChainToggle(boolean isEnabled) {
        if (!enabled.get()) {
            return;
        }
        try {
            counters.computeIfAbsent("focus_chain_toggle", k -> new AtomicLong(0))
                    .incrementAndGet();
            Map<String, Object> properties = new HashMap<>();
            properties.put("isEnabled", isEnabled);
            captureEvent(null, "focus_chain_toggle", properties);
        } catch (Exception e) {
            log.error("Failed to capture focus chain toggle", e);
        }
    }

    @Override
    public void captureFocusChainListOpened(String taskId) {
        if (!enabled.get()) {
            return;
        }
        try {
            counters.computeIfAbsent("focus_chain_list_opened", k -> new AtomicLong(0))
                    .incrementAndGet();
            Map<String, Object> properties = new HashMap<>();
            properties.put("ulid", taskId);
            captureEvent(taskId, "focus_chain_list_opened", properties);
        } catch (Exception e) {
            log.error("Failed to capture focus chain list opened", e);
        }
    }

    @Override
    public void captureDiffEditFailure(String taskId, String modelId, String errorType) {
        if (!enabled.get()) {
            return;
        }
        try {
            Map<String, Object> properties = new HashMap<>();
            properties.put("ulid", taskId);
            properties.put("errorType", errorType != null ? errorType : "");
            properties.put("modelId", modelId != null ? modelId : "unknown");
            log.info(
                    "Diff edit failure: taskId={}, modelId={}, errorType={}, timestamp={}",
                    taskId,
                    modelId,
                    errorType,
                    Instant.now());
            counters.computeIfAbsent("diff_edit_failed", k -> new AtomicLong(0)).incrementAndGet();
            captureEvent(taskId, "diff_edit_failed", properties);
        } catch (Exception e) {
            log.error("Failed to capture diff edit failure", e);
        }
    }

    @Override
    public void captureMcpToolCall(
            String taskId,
            String serverName,
            String toolName,
            String status,
            String errorMessage,
            List<String> argumentKeys) {
        if (!enabled.get()) {
            return;
        }
        try {
            Map<String, Object> properties = new HashMap<>();
            properties.put("ulid", taskId);
            properties.put("serverName", serverName != null ? serverName : "");
            properties.put("toolName", toolName != null ? toolName : "");
            properties.put("status", status != null ? status : "");
            if (errorMessage != null) {
                properties.put("errorMessage", errorMessage);
            }
            if (argumentKeys != null) {
                properties.put("argumentKeys", argumentKeys);
            }
            log.debug(
                    "MCP tool called: taskId={}, server={}, tool={}, status={}, timestamp={}",
                    taskId,
                    serverName,
                    toolName,
                    status,
                    Instant.now());
            counters.computeIfAbsent("mcp_tool_called", k -> new AtomicLong(0)).incrementAndGet();
            captureEvent(taskId, "mcp_tool_called", properties);
        } catch (Exception e) {
            log.error("Failed to capture MCP tool call", e);
        }
    }

    @Override
    public void captureCheckpointUsage(String taskId, String action, Long durationMs) {
        if (!enabled.get()) {
            return;
        }
        try {
            Map<String, Object> properties = new HashMap<>();
            properties.put("ulid", taskId);
            properties.put("action", action != null ? action : "");
            if (durationMs != null) {
                properties.put("durationMs", durationMs);
            }
            log.info(
                    "Checkpoint usage: taskId={}, action={}, durationMs={}, timestamp={}",
                    taskId,
                    action,
                    durationMs,
                    Instant.now());
            counters.computeIfAbsent("checkpoint_used", k -> new AtomicLong(0)).incrementAndGet();
            captureEvent(taskId, "checkpoint_used", properties);
        } catch (Exception e) {
            log.error("Failed to capture checkpoint usage", e);
        }
    }

    @Override
    public void captureModelSelected(String model, String provider, String taskId) {
        if (!enabled.get()) {
            return;
        }
        try {
            Map<String, Object> properties = new HashMap<>();
            properties.put("model", model != null ? model : "");
            properties.put("provider", provider != null ? provider : "");
            if (taskId != null) {
                properties.put("ulid", taskId);
            }
            log.info(
                    "Model selected: model={}, provider={}, taskId={}, timestamp={}",
                    model,
                    provider,
                    taskId,
                    Instant.now());
            counters.computeIfAbsent("model_selected", k -> new AtomicLong(0)).incrementAndGet();
            captureEvent(taskId, "model_selected", properties);
        } catch (Exception e) {
            log.error("Failed to capture model selected", e);
        }
    }

    @Override
    public void captureBrowserToolStart(
            String taskId, String viewport, boolean isRemote, String remoteBrowserHost) {
        if (!enabled.get()) {
            return;
        }
        try {
            Map<String, Object> properties = new HashMap<>();
            properties.put("ulid", taskId);
            properties.put("viewport", viewport != null ? viewport : "");
            properties.put("isRemote", isRemote);
            if (remoteBrowserHost != null) {
                properties.put("remoteBrowserHost", remoteBrowserHost);
            }
            properties.put("timestamp", Instant.now().toString());
            log.info(
                    "Browser tool start: taskId={}, viewport={}, isRemote={}, timestamp={}",
                    taskId,
                    viewport,
                    isRemote,
                    Instant.now());
            counters.computeIfAbsent("browser_tool_start", k -> new AtomicLong(0))
                    .incrementAndGet();
            captureEvent(taskId, "browser_tool_start", properties);
        } catch (Exception e) {
            log.error("Failed to capture browser tool start", e);
        }
    }

    @Override
    public void captureBrowserToolEnd(
            String taskId, int actionCount, long duration, List<String> actions) {
        if (!enabled.get()) {
            return;
        }
        try {
            Map<String, Object> properties = new HashMap<>();
            properties.put("ulid", taskId);
            properties.put("actionCount", actionCount);
            properties.put("duration", duration);
            if (actions != null) {
                properties.put("actions", actions);
            }
            properties.put("timestamp", Instant.now().toString());
            log.info(
                    "Browser tool end: taskId={}, actionCount={}, duration={}, timestamp={}",
                    taskId,
                    actionCount,
                    duration,
                    Instant.now());
            counters.computeIfAbsent("browser_tool_end", k -> new AtomicLong(0)).incrementAndGet();
            captureEvent(taskId, "browser_tool_end", properties);
        } catch (Exception e) {
            log.error("Failed to capture browser tool end", e);
        }
    }

    @Override
    public void captureBrowserError(
            String taskId, String errorType, String errorMessage, Map<String, Object> context) {
        if (!enabled.get()) {
            return;
        }
        try {
            Map<String, Object> properties = new HashMap<>();
            properties.put("ulid", taskId);
            properties.put("errorType", errorType != null ? errorType : "");
            properties.put("errorMessage", errorMessage != null ? errorMessage : "");
            if (context != null) {
                properties.put("context", context);
            }
            properties.put("timestamp", Instant.now().toString());
            log.error(
                    "Browser error: taskId={}, errorType={}, errorMessage={}, timestamp={}",
                    taskId,
                    errorType,
                    errorMessage,
                    Instant.now());
            counters.computeIfAbsent("browser_error", k -> new AtomicLong(0)).incrementAndGet();
            captureEvent(taskId, "browser_error", properties);
        } catch (Exception e) {
            log.error("Failed to capture browser error", e);
        }
    }

    @Override
    public void captureGeminiApiPerformance(
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
            Double throughputTokensPerSec) {
        if (!enabled.get()) {
            return;
        }
        try {
            Map<String, Object> properties = new HashMap<>();
            properties.put("ulid", taskId);
            properties.put("modelId", modelId != null ? modelId : "");
            if (ttftSec != null) {
                properties.put("ttftSec", ttftSec);
            }
            if (totalDurationSec != null) {
                properties.put("totalDurationSec", totalDurationSec);
            }
            properties.put("promptTokens", promptTokens);
            properties.put("outputTokens", outputTokens);
            properties.put("cacheReadTokens", cacheReadTokens);
            properties.put("cacheHit", cacheHit);
            if (cacheHitPercentage != null) {
                properties.put("cacheHitPercentage", cacheHitPercentage);
            }
            properties.put("apiSuccess", apiSuccess);
            if (apiError != null) {
                properties.put("apiError", apiError);
            }
            if (throughputTokensPerSec != null) {
                properties.put("throughputTokensPerSec", throughputTokensPerSec);
            }
            log.info(
                    "Gemini API performance: taskId={}, modelId={}, apiSuccess={}, timestamp={}",
                    taskId,
                    modelId,
                    apiSuccess,
                    Instant.now());
            counters.computeIfAbsent("gemini_api_performance", k -> new AtomicLong(0))
                    .incrementAndGet();
            captureEvent(taskId, "gemini_api_performance", properties);
        } catch (Exception e) {
            log.error("Failed to capture Gemini API performance", e);
        }
    }

    @Override
    public void captureModelFavoritesUsage(String model, boolean isFavorited) {
        if (!enabled.get()) {
            return;
        }
        try {
            Map<String, Object> properties = new HashMap<>();
            properties.put("model", model != null ? model : "");
            properties.put("isFavorited", isFavorited);
            log.info(
                    "Model favorite toggled: model={}, isFavorited={}, timestamp={}",
                    model,
                    isFavorited,
                    Instant.now());
            counters.computeIfAbsent("model_favorite_toggled", k -> new AtomicLong(0))
                    .incrementAndGet();
            captureEvent(null, "model_favorite_toggled", properties);
        } catch (Exception e) {
            log.error("Failed to capture model favorites usage", e);
        }
    }

    @Override
    public void captureButtonClick(String button, String taskId) {
        if (!enabled.get()) {
            return;
        }
        try {
            Map<String, Object> properties = new HashMap<>();
            properties.put("button", button != null ? button : "");
            if (taskId != null) {
                properties.put("ulid", taskId);
            }
            log.debug(
                    "Button clicked: button={}, taskId={}, timestamp={}",
                    button,
                    taskId,
                    Instant.now());
            counters.computeIfAbsent("button_clicked", k -> new AtomicLong(0)).incrementAndGet();
            captureEvent(taskId, "button_clicked", properties);
        } catch (Exception e) {
            log.error("Failed to capture button click", e);
        }
    }

    @Override
    public void captureProviderApiError(
            String taskId,
            String model,
            String errorMessage,
            String provider,
            Integer errorStatus,
            String requestId) {
        if (!enabled.get()) {
            return;
        }
        try {
            Map<String, Object> properties = new HashMap<>();
            properties.put("ulid", taskId);
            properties.put("model", model != null ? model : "");
            String truncatedError =
                    errorMessage != null
                            ? errorMessage.substring(0, Math.min(500, errorMessage.length()))
                            : "";
            properties.put("errorMessage", truncatedError);
            if (provider != null) {
                properties.put("provider", provider);
            }
            if (errorStatus != null) {
                properties.put("errorStatus", errorStatus);
            }
            if (requestId != null) {
                properties.put("requestId", requestId);
            }
            properties.put("timestamp", Instant.now().toString());
            log.error(
                    "Provider API error: taskId={}, model={}, errorMessage={}, timestamp={}",
                    taskId,
                    model,
                    truncatedError,
                    Instant.now());
            counters.computeIfAbsent("provider_api_error", k -> new AtomicLong(0))
                    .incrementAndGet();
            captureEvent(taskId, "provider_api_error", properties);
        } catch (Exception e) {
            log.error("Failed to capture provider API error", e);
        }
    }

    @Override
    public void captureSlashCommandUsed(String taskId, String commandName, String commandType) {
        if (!enabled.get()) {
            return;
        }
        try {
            Map<String, Object> properties = new HashMap<>();
            properties.put("ulid", taskId);
            properties.put("commandName", commandName != null ? commandName : "");
            properties.put("commandType", commandType != null ? commandType : "");
            log.info(
                    "Slash command used: taskId={}, command={}, type={}, timestamp={}",
                    taskId,
                    commandName,
                    commandType,
                    Instant.now());
            counters.computeIfAbsent("slash_command_used", k -> new AtomicLong(0))
                    .incrementAndGet();
            captureEvent(taskId, "slash_command_used", properties);
        } catch (Exception e) {
            log.error("Failed to capture slash command used", e);
        }
    }

    @Override
    public void captureClineRuleToggled(
            String taskId, String ruleFileName, boolean enabled, boolean isGlobal) {
        if (!this.enabled.get()) {
            return;
        }
        try {
            String sanitizedFileName = ruleFileName;
            if (ruleFileName != null) {
                int lastSlash =
                        Math.max(ruleFileName.lastIndexOf('/'), ruleFileName.lastIndexOf('\\'));
                if (lastSlash >= 0 && lastSlash < ruleFileName.length() - 1) {
                    sanitizedFileName = ruleFileName.substring(lastSlash + 1);
                }
            }
            Map<String, Object> properties = new HashMap<>();
            properties.put("ulid", taskId);
            properties.put("ruleFileName", sanitizedFileName != null ? sanitizedFileName : "");
            properties.put("enabled", enabled);
            properties.put("isGlobal", isGlobal);
            log.info(
                    "Cline rule toggled: taskId={}, rule={}, enabled={}, isGlobal={}, timestamp={}",
                    taskId,
                    sanitizedFileName,
                    enabled,
                    isGlobal,
                    Instant.now());
            counters.computeIfAbsent("rule_toggled", k -> new AtomicLong(0)).incrementAndGet();
            captureEvent(taskId, "rule_toggled", properties);
        } catch (Exception e) {
            log.error("Failed to capture cline rule toggled", e);
        }
    }

    @Override
    public void captureAutoCondenseToggle(String taskId, boolean enabled, String modelId) {
        if (!this.enabled.get()) {
            return;
        }
        try {
            Map<String, Object> properties = new HashMap<>();
            properties.put("ulid", taskId);
            properties.put("enabled", enabled);
            properties.put("modelId", modelId != null ? modelId : "");
            log.info(
                    "Auto condense toggled: taskId={}, enabled={}, modelId={}, timestamp={}",
                    taskId,
                    enabled,
                    modelId,
                    Instant.now());
            counters.computeIfAbsent("auto_condense_toggled", k -> new AtomicLong(0))
                    .incrementAndGet();
            captureEvent(taskId, "auto_condense_toggled", properties);
        } catch (Exception e) {
            log.error("Failed to capture auto condense toggle", e);
        }
    }

    @Override
    public void captureYoloModeToggle(String taskId, boolean enabled) {
        if (!this.enabled.get()) {
            return;
        }
        try {
            Map<String, Object> properties = new HashMap<>();
            properties.put("ulid", taskId);
            properties.put("enabled", enabled);
            log.info(
                    "Yolo mode toggled: taskId={}, enabled={}, timestamp={}",
                    taskId,
                    enabled,
                    Instant.now());
            counters.computeIfAbsent("yolo_mode_toggled", k -> new AtomicLong(0)).incrementAndGet();
            captureEvent(taskId, "yolo_mode_toggled", properties);
        } catch (Exception e) {
            log.error("Failed to capture yolo mode toggle", e);
        }
    }

    @Override
    public void captureTaskInitialization(
            String ulid, String taskId, long durationMs, boolean hasCheckpoints) {
        if (!enabled.get()) {
            return;
        }
        try {
            Map<String, Object> properties = new HashMap<>();
            properties.put("ulid", ulid);
            properties.put("taskId", taskId);
            properties.put("durationMs", durationMs);
            properties.put("hasCheckpoints", hasCheckpoints);
            log.info(
                    "Task initialization: ulid={}, taskId={}, durationMs={}, hasCheckpoints={}, timestamp={}",
                    ulid,
                    taskId,
                    durationMs,
                    hasCheckpoints,
                    Instant.now());
            counters.computeIfAbsent("task_initialization", k -> new AtomicLong(0))
                    .incrementAndGet();
            captureEvent(ulid, "task_initialization", properties);
        } catch (Exception e) {
            log.error("Failed to capture task initialization", e);
        }
    }

    @Override
    public void captureRulesMenuOpened() {
        if (!enabled.get()) {
            return;
        }
        try {
            log.info("Rules menu opened: timestamp={}", Instant.now());
            counters.computeIfAbsent("rules_menu_opened", k -> new AtomicLong(0)).incrementAndGet();
            captureEvent(null, "rules_menu_opened", new HashMap<>());
        } catch (Exception e) {
            log.error("Failed to capture rules menu opened", e);
        }
    }

    @Override
    public void captureTerminalExecution(boolean success, String method) {
        if (!enabled.get()) {
            return;
        }
        try {
            Map<String, Object> properties = new HashMap<>();
            properties.put("success", success);
            properties.put("method", method != null ? method : "");
            log.info(
                    "Terminal execution: success={}, method={}, timestamp={}",
                    success,
                    method,
                    Instant.now());
            counters.computeIfAbsent("terminal_execution", k -> new AtomicLong(0))
                    .incrementAndGet();
            captureEvent(null, "terminal_execution", properties);
        } catch (Exception e) {
            log.error("Failed to capture terminal execution", e);
        }
    }

    @Override
    public void captureTerminalOutputFailure(String reason) {
        if (!enabled.get()) {
            return;
        }
        try {
            Map<String, Object> properties = new HashMap<>();
            properties.put("reason", reason != null ? reason : "");
            log.warn("Terminal output failure: reason={}, timestamp={}", reason, Instant.now());
            counters.computeIfAbsent("terminal_output_failure", k -> new AtomicLong(0))
                    .incrementAndGet();
            captureEvent(null, "terminal_output_failure", properties);
        } catch (Exception e) {
            log.error("Failed to capture terminal output failure", e);
        }
    }

    @Override
    public void captureTerminalUserIntervention(String action) {
        if (!enabled.get()) {
            return;
        }
        try {
            Map<String, Object> properties = new HashMap<>();
            properties.put("action", action != null ? action : "");
            log.info("Terminal user intervention: action={}, timestamp={}", action, Instant.now());
            counters.computeIfAbsent("terminal_user_intervention", k -> new AtomicLong(0))
                    .incrementAndGet();
            captureEvent(null, "terminal_user_intervention", properties);
        } catch (Exception e) {
            log.error("Failed to capture terminal user intervention", e);
        }
    }

    @Override
    public void captureTerminalHang(String stage) {
        if (!enabled.get()) {
            return;
        }
        try {
            Map<String, Object> properties = new HashMap<>();
            properties.put("stage", stage != null ? stage : "");
            log.warn("Terminal hang: stage={}, timestamp={}", stage, Instant.now());
            counters.computeIfAbsent("terminal_hang", k -> new AtomicLong(0)).incrementAndGet();
            captureEvent(null, "terminal_hang", properties);
        } catch (Exception e) {
            log.error("Failed to capture terminal hang", e);
        }
    }

    @Override
    public void captureMultiRootCheckpoint(
            String taskId,
            String action,
            int rootCount,
            int successCount,
            int failureCount,
            Long durationMs) {
        if (!enabled.get()) {
            return;
        }
        try {
            Map<String, Object> properties = new HashMap<>();
            properties.put("ulid", taskId);
            properties.put("action", action != null ? action : "");
            properties.put("root_count", rootCount);
            properties.put("success_count", successCount);
            properties.put("failure_count", failureCount);
            properties.put("success_rate", rootCount > 0 ? (double) successCount / rootCount : 0.0);
            if (durationMs != null) {
                properties.put("duration_ms", durationMs);
            }
            log.info(
                    "Multi-root checkpoint: taskId={}, action={}, rootCount={}, successCount={}, failureCount={}, timestamp={}",
                    taskId,
                    action,
                    rootCount,
                    successCount,
                    failureCount,
                    Instant.now());
            counters.computeIfAbsent("multi_root_checkpoint", k -> new AtomicLong(0))
                    .incrementAndGet();
            captureEvent(taskId, "multi_root_checkpoint", properties);
        } catch (Exception e) {
            log.error("Failed to capture multi-root checkpoint", e);
        }
    }

    @Override
    public void captureMentionUsed(String mentionType, Integer contentLength) {
        if (!enabled.get()) {
            return;
        }
        try {
            Map<String, Object> properties = new HashMap<>();
            properties.put("mentionType", mentionType != null ? mentionType : "");
            if (contentLength != null) {
                properties.put("contentLength", contentLength);
            }
            properties.put("timestamp", Instant.now().toString());
            log.info(
                    "Mention used: type={}, contentLength={}, timestamp={}",
                    mentionType,
                    contentLength,
                    Instant.now());
            counters.computeIfAbsent("mention_used", k -> new AtomicLong(0)).incrementAndGet();
            captureEvent(null, "mention_used", properties);
        } catch (Exception e) {
            log.error("Failed to capture mention used", e);
        }
    }

    @Override
    public void captureMentionFailed(String mentionType, String errorType, String errorMessage) {
        if (!enabled.get()) {
            return;
        }
        try {
            Map<String, Object> properties = new HashMap<>();
            properties.put("mentionType", mentionType != null ? mentionType : "");
            properties.put("errorType", errorType != null ? errorType : "");
            if (errorMessage != null) {
                properties.put(
                        "errorMessage",
                        errorMessage.substring(0, Math.min(500, errorMessage.length())));
            }
            properties.put("timestamp", Instant.now().toString());
            log.warn(
                    "Mention failed: type={}, errorType={}, timestamp={}",
                    mentionType,
                    errorType,
                    Instant.now());
            counters.computeIfAbsent("mention_failed", k -> new AtomicLong(0)).incrementAndGet();
            captureEvent(null, "mention_failed", properties);
        } catch (Exception e) {
            log.error("Failed to capture mention failed", e);
        }
    }

    @Override
    public void captureMentionSearchResults(
            int queryLength, int resultCount, String searchType, boolean isEmpty) {
        if (!enabled.get()) {
            return;
        }
        try {
            Map<String, Object> properties = new HashMap<>();
            properties.put("queryLength", queryLength);
            properties.put("resultCount", resultCount);
            properties.put("searchType", searchType != null ? searchType : "");
            properties.put("isEmpty", isEmpty);
            properties.put("timestamp", Instant.now().toString());
            log.debug(
                    "Mention search results: queryLength={}, resultCount={}, searchType={}, isEmpty={}, timestamp={}",
                    queryLength,
                    resultCount,
                    searchType,
                    isEmpty,
                    Instant.now());
            counters.computeIfAbsent("mention_search_results", k -> new AtomicLong(0))
                    .incrementAndGet();
            captureEvent(null, "mention_search_results", properties);
        } catch (Exception e) {
            log.error("Failed to capture mention search results", e);
        }
    }

    @Override
    public void captureSubagentToggle(boolean enabled) {
        if (!this.enabled.get()) {
            return;
        }
        try {
            Map<String, Object> properties = new HashMap<>();
            properties.put("enabled", enabled);
            properties.put("timestamp", Instant.now().toString());
            log.info("Subagent toggle: enabled={}, timestamp={}", enabled, Instant.now());
            counters.computeIfAbsent("subagent_toggle", k -> new AtomicLong(0)).incrementAndGet();
            captureEvent(null, enabled ? "subagent_enabled" : "subagent_disabled", properties);
        } catch (Exception e) {
            log.error("Failed to capture subagent toggle", e);
        }
    }

    @Override
    public void captureSubagentExecution(
            String taskId, long durationMs, int outputLines, boolean success) {
        if (!enabled.get()) {
            return;
        }
        try {
            Map<String, Object> properties = new HashMap<>();
            properties.put("ulid", taskId);
            properties.put("durationMs", durationMs);
            properties.put("outputLines", outputLines);
            properties.put("success", success);
            properties.put("timestamp", Instant.now().toString());
            log.info(
                    "Subagent execution: taskId={}, durationMs={}, outputLines={}, success={}, timestamp={}",
                    taskId,
                    durationMs,
                    outputLines,
                    success,
                    Instant.now());
            String eventKey = success ? "subagent_completed" : "subagent_started";
            counters.computeIfAbsent(eventKey, k -> new AtomicLong(0)).incrementAndGet();
            captureEvent(taskId, eventKey, properties);
        } catch (Exception e) {
            log.error("Failed to capture subagent execution", e);
        }
    }
}
