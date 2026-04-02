package com.hhoa.kline.core.core.controller;

import com.hhoa.kline.core.core.controller.testsupport.AbstractLocalTaskIntegrationTest;
import com.hhoa.kline.core.core.controller.testsupport.AskResponseLoopScript;
import com.hhoa.kline.core.core.controller.testsupport.PendingAskResponseDriver;
import com.hhoa.kline.core.core.controller.testsupport.ScriptedConversationApiHandler;
import com.hhoa.kline.core.core.task.TaskLockUtils;
import com.hhoa.kline.core.core.task.TaskV2;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 多轮 ask/response：数据见 {@link AskResponseLoopScript}，挂起应答见 {@link PendingAskResponseDriver}。
 */
class MultiTurnAskResponseIntegrationTest extends AbstractLocalTaskIntegrationTest {

    @Test
    void sixRoundsAskFollowup_userRepliesEachRound_thenCompletion() throws Exception {
        ScriptedConversationApiHandler apiHandler =
                ScriptedConversationApiHandler.multiTurnRounds(
                        AskResponseLoopScript.assistantApiRounds().toArray(String[]::new));

        LocalTaskManagerFactory factory = new LocalTaskManagerFactory(
                apiHandler, systemPromptService, () -> baseDir, null);

        DefaultTaskManager taskManager = (DefaultTaskManager) factory.getOrCreateTaskManager();
        applyIntegrationSettings(taskManager.getStateManager(), false);

        String taskId =
                taskManager.initTask(AskResponseLoopScript.INITIAL_TASK_TEXT, null, null, null, null);
        try {
            TaskV2 task = taskManager.getTask(taskId);
            assertThat(task).isNotNull();

            PendingAskResponseDriver.awaitTaskCompleteRespondingToPendingAsks(
                    taskManager,
                    taskId,
                    task,
                    AskResponseLoopScript.userRepliesAfterEachFollowup(),
                    DEFAULT_TASK_AWAIT_MS);

            flushPendingMergedIncrements();

            assertThat(partialMessages).isNotEmpty();
            assertThat(apiHandler.getStreamInvocationCount())
                    .isEqualTo(AskResponseLoopScript.assistantApiRounds().size());
            assertThat(statePushCount.get()).isPositive();
            assertFirstUserTaskContentEquals(task, AskResponseLoopScript.INITIAL_TASK_TEXT);
        } finally {
            TaskLockUtils.releaseTaskLock(taskId);
            taskManager.dispose();
        }
    }
}
