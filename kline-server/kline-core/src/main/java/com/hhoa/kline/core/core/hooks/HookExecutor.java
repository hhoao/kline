package com.hhoa.kline.core.core.hooks;

import com.hhoa.ai.kline.commons.utils.JsonUtils;
import com.hhoa.kline.core.core.task.HookExecution;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Hook 执行编排器，提供标准化的错误处理、状态跟踪和清理。
 *
 * <p>整合了所有 hook 执行点的通用模式。
 */
@Slf4j
public class HookExecutor {

    /**
     * Hook 执行选项
     *
     * @see #executeHook(HookExecutionOptions)
     */
    @Getter
    @Builder
    public static class HookExecutionOptions {
        private final HookName hookName;
        private final HookInput hookInput;
        private final boolean cancellable;
        private final boolean hooksEnabled;
        private final String taskId;

        /** say 回调：(type, text) → messageTs */
        private final BiConsumer<String, String> say;

        /** 工具名（PreToolUse/PostToolUse 可选） */
        private final String toolName;

        /** hook 目录列表（全局 + 工作区） */
        private final List<String> hooksDirs;

        /** 工作区根目录列表 */
        private final List<String> workspaceRoots;

        /** 可选：hook 状态更新回调 */
        private final Consumer<Map<String, Object>> onStatusUpdate;

        /** 可选：设置当前活跃的 hook 执行（用于取消支持） */
        private final Consumer<HookExecution> setActiveHookExecution;

        /** 可选：清除当前活跃的 hook 执行 */
        private final Runnable clearActiveHookExecution;
    }

    /**
     * 执行 hook 并返回结果
     *
     * @param options 执行选项
     * @return 执行结果
     */
    public static HookExecutionResult executeHook(HookExecutionOptions options) {
        // 如果 hook 未启用，直接返回
        if (!options.isHooksEnabled()) {
            return HookExecutionResult.empty();
        }

        HookFactory factory = new HookFactory(options.getHooksDirs(), options.getWorkspaceRoots());

        // 检查 hook 是否存在
        if (!factory.hasHook(options.getHookName())) {
            return HookExecutionResult.empty();
        }

        List<String> scriptPaths = factory.getHookScriptPaths(options.getHookName());

        try {
            // 显示 hook 执行状态
            Map<String, Object> hookMetadata = new HashMap<>();
            hookMetadata.put("hookName", options.getHookName().getValue());
            if (options.getToolName() != null) {
                hookMetadata.put("toolName", options.getToolName());
            }
            hookMetadata.put("status", "running");
            hookMetadata.put("scriptPaths", scriptPaths);

            Long hookMessageTs = null;
            if (options.getSay() != null) {
                options.getSay().accept("hook_status", JsonUtils.toJsonString(hookMetadata));
                hookMessageTs = System.currentTimeMillis();
            }

            // 注册活跃 hook 执行（用于取消支持）
            if (options.isCancellable() && options.getSetActiveHookExecution() != null) {
                HookExecution hookExecution =
                        HookExecution.builder()
                                .hookName(options.getHookName().getValue())
                                .toolName(options.getToolName())
                                .messageTs(hookMessageTs)
                                .build();
                options.getSetActiveHookExecution().accept(hookExecution);
            }

            // 创建流式回调
            HookStreamCallback streamCallback = null;
            if (options.getSay() != null) {
                BiConsumer<String, String> say = options.getSay();
                streamCallback =
                        (line, stream, source, scriptPath) -> {
                            String prefix = "[" + source + " " + stream + "] ";
                            say.accept("hook_output_stream", prefix + line);
                        };
            }

            // 创建并执行 hook
            HookRunner hook = factory.createWithStreaming(options.getHookName(), streamCallback);
            HookOutput result;
            try {
                result = hook.execute(options.getHookInput());
            } finally {
                // 清除活跃 hook 执行
                if (options.getClearActiveHookExecution() != null) {
                    options.getClearActiveHookExecution().run();
                }
            }

            // NoOp hook 返回默认值
            if (!result.isCancel()
                    && (result.getContextModification() == null
                            || result.getContextModification().isEmpty())
                    && (result.getErrorMessage() == null || result.getErrorMessage().isEmpty())) {
                return HookExecutionResult.empty();
            }

            // 检查是否请求取消
            if (result.isCancel()) {
                updateStatus(options, scriptPaths, "cancelled", 130);
                return fromHookOutput(result);
            }

            // 成功完成
            updateStatus(options, scriptPaths, "completed", 0);
            return fromHookOutput(result);

        } catch (HookExecutionError hookError) {
            // 更新状态为失败
            HookErrorInfo errorInfo = hookError.getErrorInfo();
            Map<String, Object> failedMetadata = new HashMap<>();
            failedMetadata.put("hookName", options.getHookName().getValue());
            failedMetadata.put("status", "failed");
            failedMetadata.put(
                    "exitCode", errorInfo.getExitCode() != null ? errorInfo.getExitCode() : 1);
            failedMetadata.put("scriptPaths", scriptPaths);

            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("type", errorInfo.getType().getValue());
            errorMap.put("message", errorInfo.getMessage());
            if (errorInfo.getDetails() != null) {
                errorMap.put("details", errorInfo.getDetails());
            }
            if (errorInfo.getScriptPath() != null) {
                errorMap.put("scriptPath", errorInfo.getScriptPath());
            }
            failedMetadata.put("error", errorMap);

            if (options.getSay() != null) {
                options.getSay().accept("hook_status", JsonUtils.toJsonString(failedMetadata));
            }

            log.error("{} hook failed:", options.getHookName(), hookError);

            // Fail-open：返回安全默认值
            return HookExecutionResult.builder().cancel(false).wasCancelled(false).build();

        } catch (Exception e) {
            log.error("{} hook unexpected error:", options.getHookName(), e);
            return HookExecutionResult.builder().cancel(false).wasCancelled(false).build();
        }
    }

    private static HookExecutionResult fromHookOutput(HookOutput output) {
        String contextMod =
                output.getContextModification() != null
                                && !output.getContextModification().trim().isEmpty()
                        ? output.getContextModification()
                        : null;
        String errorMsg =
                output.getErrorMessage() != null && !output.getErrorMessage().trim().isEmpty()
                        ? output.getErrorMessage()
                        : null;

        return HookExecutionResult.builder()
                .cancel(output.isCancel())
                .contextModification(contextMod)
                .errorMessage(errorMsg)
                .wasCancelled(false)
                .build();
    }

    private static void updateStatus(
            HookExecutionOptions options, List<String> scriptPaths, String status, int exitCode) {
        if (options.getSay() == null) {
            return;
        }
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("hookName", options.getHookName().getValue());
        if (options.getToolName() != null) {
            metadata.put("toolName", options.getToolName());
        }
        metadata.put("status", status);
        metadata.put("exitCode", exitCode);
        metadata.put("scriptPaths", scriptPaths);

        options.getSay().accept("hook_status", JsonUtils.toJsonString(metadata));
    }
}
