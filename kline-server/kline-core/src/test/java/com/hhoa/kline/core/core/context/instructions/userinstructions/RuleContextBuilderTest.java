package com.hhoa.kline.core.core.context.instructions.userinstructions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hhoa.kline.core.core.shared.ClineAsk;
import com.hhoa.kline.core.core.shared.ClineMessageType;
import com.hhoa.kline.core.core.shared.ClineSay;
import com.hhoa.kline.core.core.task.ClineMessage;
import com.hhoa.kline.core.core.task.MessageStateHandler;
import java.util.List;
import org.junit.jupiter.api.Test;

class RuleContextBuilderTest {

    @Test
    void buildsEvaluationContextFromMessagesAndTabs() {
        MessageStateHandler messageStateHandler =
                new MessageStateHandler("task-1", "ulid-1", null, null, null);

        ClineMessage taskMessage = new ClineMessage();
        taskMessage.setType(ClineMessageType.SAY);
        taskMessage.setSay(ClineSay.TASK);
        taskMessage.setText("Please update src/app/Main.java and docs/guide.md");

        ClineMessage toolResult = new ClineMessage();
        toolResult.setType(ClineMessageType.SAY);
        toolResult.setSay(ClineSay.TOOL);
        toolResult.setText("{\"tool\":\"editedExistingFile\",\"path\":\"src/app/Main.java\"}");

        ClineMessage pendingTool = new ClineMessage();
        pendingTool.setType(ClineMessageType.ASK);
        pendingTool.setAsk(ClineAsk.TOOL);
        pendingTool.setText(
                "{\"tool\":\"apply_patch\",\"input\":\"*** Begin Patch\\n*** Update File: src/app/NewFile.java\\n@@\\n*** End Patch\"}");

        messageStateHandler.setClineMessages(List.of(taskMessage, toolResult, pendingTool));

        RuleConditionals.RuleEvaluationContext context =
                RuleContextBuilder.buildEvaluationContext(
                        RuleContextBuilder.RuleContextBuilderDeps.builder()
                                .cwd("/repo")
                                .messageStateHandler(messageStateHandler)
                                .openTabPaths(List.of("/repo/src/ui/OpenTab.vue"))
                                .visibleTabPaths(List.of("/repo/docs/visible.md"))
                                .build());

        assertEquals(
                List.of(
                        "docs/guide.md",
                        "docs/visible.md",
                        "src/app/Main.java",
                        "src/app/NewFile.java",
                        "src/ui/OpenTab.vue"),
                context.getPaths());
    }

    @Test
    void extractsPathsFromApplyPatchHeaders() {
        List<String> paths =
                RuleContextBuilder.extractPathsFromApplyPatch(
                        """
                        *** Begin Patch
                        *** Update File: src/main/App.java
                        @@
                        *** Add File: docs/new.md
                        +hello
                        *** End Patch
                        """);

        assertEquals(List.of("src/main/App.java", "docs/new.md"), paths);
    }

    @Test
    void ignoresInvalidToolPayloads() {
        MessageStateHandler messageStateHandler =
                new MessageStateHandler("task-2", "ulid-2", null, null, null);
        ClineMessage invalidTool = new ClineMessage();
        invalidTool.setType(ClineMessageType.ASK);
        invalidTool.setAsk(ClineAsk.TOOL);
        invalidTool.setText("not-json");
        messageStateHandler.setClineMessages(List.of(invalidTool));

        RuleConditionals.RuleEvaluationContext context =
                RuleContextBuilder.buildEvaluationContext(
                        RuleContextBuilder.RuleContextBuilderDeps.builder()
                                .cwd("/repo")
                                .messageStateHandler(messageStateHandler)
                                .build());

        assertTrue(context.getPaths().isEmpty());
    }
}
