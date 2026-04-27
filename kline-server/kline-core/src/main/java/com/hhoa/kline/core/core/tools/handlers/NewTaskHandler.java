package com.hhoa.kline.core.core.tools.handlers;

import com.hhoa.kline.core.core.assistant.ToolUse;
import com.hhoa.kline.core.core.integrations.misc.ExtractText;
import com.hhoa.kline.core.core.prompts.ResponseFormatter;
import com.hhoa.kline.core.core.prompts.systemprompt.ClineToolSpec;
import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.shared.ClineAsk;
import com.hhoa.kline.core.core.shared.ClineSay;
import com.hhoa.kline.core.core.task.AskResult;
import com.hhoa.kline.core.core.tools.specs.NewTaskTool;
import com.hhoa.kline.core.core.tools.types.ToolContext;
import com.hhoa.kline.core.core.tools.types.ToolExecuteResult;
import com.hhoa.kline.core.core.tools.types.ToolState;
import com.hhoa.kline.core.core.tools.types.UIHelpers;
import com.hhoa.kline.core.core.tools.utils.ToolResultUtils;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NewTaskHandler implements StateFullToolHandler {

    private final ResponseFormatter formatResponse = new ResponseFormatter();

    @Getter
    @Setter
    public static class NewTaskToolState extends ToolState {
        private String context;
    }

    @Override
    public ToolState createToolState() {
        return new NewTaskToolState();
    }

    @Override
    public String getName() {
        return ClineAsk.NEW_TASK.getValue();
    }

    @Override
    public String getDescription(ToolUse block) {
        return "[" + block.getName() + " for creating a new task]";
    }

    @Override
    public ClineToolSpec getClineToolSpec() {
        return NewTaskTool.create(ModelFamily.GENERIC);
    }

    @Override
    public void handlePartialBlock(ToolUse block, UIHelpers ui) {
        String context = HandlerUtils.getStringParam(block, "context");
        ui.ask(ClineAsk.NEW_TASK, context, true, null);
    }

    @Override
    public ToolExecuteResult execute(ToolContext toolContext, ToolUse block) {
        String context = HandlerUtils.getStringParam(block, "context");

        if (toolContext.getAutoApprovalSettings() != null
                && toolContext.getAutoApprovalSettings().isEnabled()
                && toolContext.getAutoApprovalSettings().isEnableNotifications()) {
            showSystemNotification(
                    "Cline wants to start a new task...",
                    "Cline is suggesting to start a new task with: " + context);
        }

        NewTaskToolState state = (NewTaskToolState) toolContext.getToolState();
        state.setPhase(1);
        state.setContext(context);

        var token =
                ToolResultUtils.askApprovalAndPushFeedbackForToken(
                        ClineAsk.NEW_TASK,
                        context,
                        toolContext,
                        null,
                        block,
                        getDescription(block));
        return new ToolExecuteResult.PendingAsk(token);
    }

    @Override
    public ToolExecuteResult resume(
            ToolContext context, ToolUse block, ToolState toolState, AskResult askResult) {

        String text = askResult != null ? askResult.getText() : null;
        String[] images =
                askResult != null && askResult.getImages() != null
                        ? askResult.getImages().toArray(new String[0])
                        : null;
        String[] files =
                askResult != null && askResult.getFiles() != null
                        ? askResult.getFiles().toArray(new String[0])
                        : null;

        boolean hasFeedback =
                (text != null && !text.isEmpty())
                        || (images != null && images.length > 0)
                        || (files != null && files.length > 0);

        if (hasFeedback) {
            String fileContentString = "";
            if (files != null && files.length > 0) {
                List<Path> filePaths = new ArrayList<>();
                for (String file : files) {
                    filePaths.add(Paths.get(file));
                }
                fileContentString = ExtractText.processFilesIntoText(filePaths);
            }

            context.getCallbacks()
                    .say(
                            ClineSay.USER_FEEDBACK,
                            text == null ? "" : text,
                            images,
                            files,
                            false,
                            null);

            String feedbackText = text == null ? "" : text;
            return HandlerUtils.createToolExecuteResult(
                    formatResponse.toolResult(
                            "The user provided feedback instead of creating a new task:\n<feedback>\n"
                                    + feedbackText
                                    + "\n</feedback>",
                            images,
                            fileContentString));
        }

        return HandlerUtils.createToolExecuteResult(
                formatResponse.toolResult(
                        "The user has created a new task with the provided context.", null, null));
    }

    private static void showSystemNotification(String subtitle, String message) {
        // TODO: 实现实际的系统通知功能
        log.info(String.format("[Notification] %s: %s", subtitle, message));
    }
}
