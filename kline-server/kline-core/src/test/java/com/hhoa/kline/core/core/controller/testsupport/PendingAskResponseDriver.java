package com.hhoa.kline.core.core.controller.testsupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import com.hhoa.kline.core.core.controller.TaskManager;
import com.hhoa.kline.core.core.shared.ClineAsk;
import com.hhoa.kline.core.core.shared.ClineAskResponse;
import com.hhoa.kline.core.core.task.TaskStatus;
import com.hhoa.kline.core.core.task.TaskV2;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** 在任务异步执行时轮询 {@link PendingAskToken}，按 {@link ClineAsk} 类型自动提交 Webview 侧应答。 */
public final class PendingAskResponseDriver {

    private PendingAskResponseDriver() {}

    /**
     * 阻塞直到任务完成：遇到 {@link ClineAsk#FOLLOWUP} 时按序消费 {@code followupTexts}；遇到 {@link
     * ClineAsk#COMPLETION_RESULT} 时点击确认（YES）。
     */
    public static void awaitTaskCompleteRespondingToPendingAsks(
            TaskManager taskManager,
            String taskId,
            TaskV2 task,
            List<String> followupTexts,
            long timeoutMs)
            throws InterruptedException {
        AtomicInteger followupIndex = new AtomicInteger();
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            TaskStatus s = task.getState();
            if (s == TaskStatus.TASK_COMPLETE) {
                return;
            }
            if (s == TaskStatus.ABORT) {
                fail("expected TASK_COMPLETE, got ABORT");
            }
            var pending = task.getTaskState().getToolExecutionState().getPendingAskTokens();
            if (!pending.isEmpty()) {
                var entry = pending.entrySet().iterator().next();
                String pendingId = entry.getKey();
                ClineAsk askType = entry.getValue().getAskType();
                if (askType == ClineAsk.FOLLOWUP) {
                    int i = followupIndex.getAndIncrement();
                    assertThat(i).as("FOLLOWUP 次数超过提供的回复条数").isLessThan(followupTexts.size());
                    taskManager.handleWebviewAskResponse(
                            taskId,
                            pendingId,
                            ClineAskResponse.MESSAGE_RESPONSE,
                            followupTexts.get(i),
                            null,
                            null);
                } else if (askType == ClineAsk.COMPLETION_RESULT) {
                    taskManager.handleWebviewAskResponse(
                            taskId,
                            pendingId,
                            ClineAskResponse.YES_BUTTON_CLICKED,
                            null,
                            null,
                            null);
                } else if (askType == ClineAsk.PROCESS_ASSISTANT_RESPONSE_FAILED) {
                    // 与 UserRespondedTransition 一致：YES 后重试 prepare / API
                    taskManager.handleWebviewAskResponse(
                            taskId,
                            pendingId,
                            ClineAskResponse.YES_BUTTON_CLICKED,
                            null,
                            null,
                            null);
                } else {
                    fail("unexpected pending ask type: " + askType);
                }
            }
            Thread.sleep(20L);
        }
        assertThat(task.getState())
                .as("timeout after %d ms waiting for TASK_COMPLETE", timeoutMs)
                .isEqualTo(TaskStatus.TASK_COMPLETE);
    }
}
