package com.hhoa.kline.core.core.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.hhoa.kline.core.core.controller.testsupport.AbstractLocalTaskIntegrationTest;
import com.hhoa.kline.core.core.controller.testsupport.ScriptedConversationApiHandler;
import com.hhoa.kline.core.core.task.TaskLockUtils;
import com.hhoa.kline.core.core.task.TaskV2;
import com.hhoa.kline.core.enums.ClineDefaultTool;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class LocalTaskConversationIntegrationTest extends AbstractLocalTaskIntegrationTest {

    @Test
    void initTask_runsStreamingAssistantReply_andEndsInTaskComplete() throws InterruptedException {
        String expectedAssistantText = "你好，这是流式模拟回复。";
        ScriptedConversationApiHandler apiHandler =
                ScriptedConversationApiHandler.singleTurnTextReply(expectedAssistantText);

        LocalTaskManagerFactory factory =
                new LocalTaskManagerFactory(apiHandler, systemPromptService, () -> baseDir, null);

        DefaultTaskManager taskManager = (DefaultTaskManager) factory.getOrCreateTaskManager();
        applyIntegrationSettings(taskManager.getStateManager(), false);
        String taskId = taskManager.initTask("请用中文打个招呼", null, null, null, null);
        try {
            TaskV2 task = taskManager.getTask(taskId);
            assertThat(task).isNotNull();

            awaitTaskComplete(task, DEFAULT_TASK_AWAIT_MS);
            flushPendingMergedIncrements();

            assertThat(partialMessages).isNotEmpty();
            assertThat(mergedIncrementSeries).containsExactly(expectedAssistantText);

            assertThat(apiHandler.getStreamInvocationCount()).isEqualTo(1);
            assertThat(statePushCount.get()).isPositive();
            assertFirstUserTaskContentEquals(task, "请用中文打个招呼");
        } finally {
            TaskLockUtils.releaseTaskLock(taskId);
            taskManager.dispose();
        }
    }

    @Test
    void initTask_multiTurn_toolCallThenSummary() throws Exception {
        Path workspaceRoot = baseDir.resolve("workspace");
        Files.createDirectories(workspaceRoot);
        Files.writeString(workspaceRoot.resolve("doc.txt"), "multi-turn-secret");

        String readFileToolXml =
                "<%s>\n<path>doc.txt</path>\n</%s>"
                        .formatted(
                                ClineDefaultTool.FILE_READ.getValue(),
                                ClineDefaultTool.FILE_READ.getValue());

        ScriptedConversationApiHandler apiHandler =
                ScriptedConversationApiHandler.multiTurnRounds(
                        readFileToolXml, "已读取工作区文件，内容为：multi-turn-secret。");

        LocalTaskManagerFactory factory =
                new LocalTaskManagerFactory(apiHandler, systemPromptService, () -> baseDir, null);

        DefaultTaskManager taskManager = (DefaultTaskManager) factory.getOrCreateTaskManager();
        applyIntegrationSettings(taskManager.getStateManager(), true);
        String taskId = taskManager.initTask("请先读取 doc.txt，再用一句话说明读到的内容。", null, null, null, null);
        try {
            TaskV2 task = taskManager.getTask(taskId);
            assertThat(task).isNotNull();

            awaitTaskComplete(task, DEFAULT_TASK_AWAIT_MS);
            flushPendingMergedIncrements();

            assertThat(partialMessages).isNotEmpty();
            String docAbs =
                    workspaceRoot.resolve("doc.txt").toAbsolutePath().normalize().toString();
            String expectedMergedToolJson =
                    """
                        {"path":"%s","tool":"readFile","content":"%s","operationIsLocatedInWorkspace":"true"}"""
                            .formatted(docAbs, docAbs);
            assertThat(mergedIncrementSeries)
                    .containsExactly(expectedMergedToolJson, "已读取工作区文件，内容为：multi-turn-secret。");

            assertThat(apiHandler.getStreamInvocationCount()).isEqualTo(2);
            assertThat(statePushCount.get()).isPositive();
            assertFirstUserTaskContentEquals(task, "请先读取 doc.txt，再用一句话说明读到的内容。");
        } finally {
            TaskLockUtils.releaseTaskLock(taskId);
            taskManager.dispose();
        }
    }
}
