package com.hhoa.kline.core.core.hooks;

import com.hhoa.kline.core.core.task.ClineMessage;
import com.hhoa.kline.core.core.task.MessageStateHandler;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * PreCompact hook 执行器。
 *
 * <p>对应 Cline TS 版本的 precompact-executor.ts。 提供在上下文压缩前执行 hook 的标准化流程。
 */
@Slf4j
public final class PreCompactExecutor {

    private PreCompactExecutor() {}

    /** Token 使用信息。 */
    @Data
    @Builder
    public static class TokenUsage {
        private int tokensIn;
        private int tokensOut;
        private int tokensInCache;
        private int tokensOutCache;
    }

    /** PreCompact hook 参数。 */
    @Data
    @Builder
    public static class PreCompactHookParams {
        private String taskId;
        private String ulid;
        private HookModelContext.ResolvedContext modelContext;
        private int[] conversationHistoryDeletedRange;
        private List<ClineMessage> clineMessages;
        private MessageStateHandler messageStateHandler;
        private String compactionStrategy;
        private int[] deletedRange;
        private BiConsumer<String, String> say;
        private Runnable postStateToWebview;
        private Consumer<HookExecutionResult> onResult;
        private boolean hooksEnabled;
        private List<String> hooksDirs;
        private List<String> workspaceRoots;
    }

    /** PreCompact hook 结果。 */
    @Data
    @Builder
    public static class PreCompactHookResult {
        private String contextModification;
    }

    /** Hook 取消错误。 */
    public static class HookCancellationError extends RuntimeException {
        private final boolean wasCancelled;

        public HookCancellationError(boolean wasCancelled) {
            super("Hook cancelled the operation");
            this.wasCancelled = wasCancelled;
        }

        public boolean wasCancelled() {
            return wasCancelled;
        }
    }

    /**
     * 从 API 请求消息中提取 token 使用信息。
     *
     * @param message API 请求消息
     * @return token 使用信息
     */
    public static TokenUsage extractTokenUsageFromMessage(ClineMessage message) {
        if (message == null || message.getText() == null || message.getText().isEmpty()) {
            return TokenUsage.builder().build();
        }
        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            var node = mapper.readTree(message.getText());
            return TokenUsage.builder()
                    .tokensIn(node.has("tokensIn") ? node.get("tokensIn").asInt() : 0)
                    .tokensOut(node.has("tokensOut") ? node.get("tokensOut").asInt() : 0)
                    .tokensInCache(node.has("cacheWrites") ? node.get("cacheWrites").asInt() : 0)
                    .tokensOutCache(node.has("cacheReads") ? node.get("cacheReads").asInt() : 0)
                    .build();
        } catch (Exception e) {
            log.error("[PreCompact] Failed to parse API request token usage:", e);
            return TokenUsage.builder().build();
        }
    }

    /**
     * 执行 PreCompact hook 并返回结果。
     *
     * @param params hook 参数
     * @return hook 结果
     * @throws HookCancellationError 如果 hook 取消了操作
     */
    public static PreCompactHookResult executePreCompactHookWithCleanup(
            PreCompactHookParams params) {
        if (!params.isHooksEnabled()) {
            return PreCompactHookResult.builder().build();
        }

        try {
            // 找到最近的 API 请求消息以提取 token 使用信息
            int previousApiReqIndex = -1;
            for (int i = params.getClineMessages().size() - 1; i >= 0; i--) {
                ClineMessage m = params.getClineMessages().get(i);
                if (m.getSay() != null && "api_req_started".equals(m.getSay().getValue())) {
                    previousApiReqIndex = i;
                    break;
                }
            }

            ClineMessage previousRequest =
                    previousApiReqIndex >= 0
                            ? params.getClineMessages().get(previousApiReqIndex)
                            : null;
            TokenUsage usage = extractTokenUsageFromMessage(previousRequest);

            // 提取删除范围
            int deletedRangeStart = 0;
            int deletedRangeEnd = 0;
            if (params.getDeletedRange() != null && params.getDeletedRange().length >= 2) {
                deletedRangeStart = params.getDeletedRange()[0];
                deletedRangeEnd = params.getDeletedRange()[1];
            } else if (params.getConversationHistoryDeletedRange() != null
                    && params.getConversationHistoryDeletedRange().length >= 2) {
                deletedRangeStart = params.getConversationHistoryDeletedRange()[0];
                deletedRangeEnd = params.getConversationHistoryDeletedRange()[1];
            }

            // 构建 hook 输入
            HookInput hookInput =
                    HookInput.builder()
                            .taskId(params.getTaskId())
                            .hookName(HookName.PRE_COMPACT.getValue())
                            .preCompact(
                                    HookData.PreCompactData.builder()
                                            .compactionStrategy(params.getCompactionStrategy())
                                            .build())
                            .build();

            // 执行 hook
            HookExecutionResult result =
                    HookExecutor.executeHook(
                            HookExecutor.HookExecutionOptions.builder()
                                    .hookName(HookName.PRE_COMPACT)
                                    .hookInput(hookInput)
                                    .cancellable(true)
                                    .hooksEnabled(params.isHooksEnabled())
                                    .taskId(params.getTaskId())
                                    .say(params.getSay())
                                    .hooksDirs(params.getHooksDirs())
                                    .workspaceRoots(params.getWorkspaceRoots())
                                    .build());

            if (Boolean.TRUE.equals(result.getCancel())) {
                log.info(
                        "[PreCompact] Context compaction cancelled for task {}",
                        params.getTaskId());
                throw new HookCancellationError(result.isWasCancelled());
            }

            if (result.getContextModification() != null) {
                log.info(
                        "[PreCompact] Hook provided context modification for task {}",
                        params.getTaskId());
            }

            return PreCompactHookResult.builder()
                    .contextModification(result.getContextModification())
                    .build();

        } catch (HookCancellationError e) {
            throw e;
        } catch (Exception e) {
            log.error("[PreCompact] Error executing PreCompact hook:", e);
            return PreCompactHookResult.builder().build();
        }
    }
}
