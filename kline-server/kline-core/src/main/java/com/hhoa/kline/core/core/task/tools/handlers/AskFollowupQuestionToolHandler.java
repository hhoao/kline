package com.hhoa.kline.core.core.task.tools.handlers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hhoa.kline.core.core.assistant.ToolUse;
import com.hhoa.kline.core.core.integrations.misc.ExtractText;
import com.hhoa.kline.core.core.integrations.notifications.NotificationType;
import com.hhoa.kline.core.core.prompts.ResponseFormatter;
import com.hhoa.kline.core.core.prompts.systemprompt.ClineToolSpec;
import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.prompts.systemprompt.tools.AskFollowupQuestionTool;
import com.hhoa.kline.core.core.shared.ClineAsk;
import com.hhoa.kline.core.core.shared.ClineSay;
import com.hhoa.kline.core.core.task.AskResult;
import com.hhoa.kline.core.core.task.ClineMessage;
import com.hhoa.kline.core.core.task.MessageUtils;
import com.hhoa.kline.core.core.task.tools.types.ToolContext;
import com.hhoa.kline.core.core.task.tools.types.ToolExecuteResult;
import com.hhoa.kline.core.core.task.tools.types.ToolState;
import com.hhoa.kline.core.core.task.tools.types.UIHelpers;
import com.hhoa.kline.core.core.task.tools.utils.ToolResultUtils;
import com.hhoa.kline.core.core.utils.PartialJsonUtils;
import com.hhoa.kline.core.enums.ClineDefaultTool;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

public class AskFollowupQuestionToolHandler implements StateFullToolHandler {

    private final ResponseFormatter formatResponse = new ResponseFormatter();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Getter
    @Setter
    public static class AskFollowupToolState extends ToolState {
        private String question;
        private List<String> options;
        private String optionsRaw;
    }

    @Override
    public ToolState createToolState() {
        return new AskFollowupToolState();
    }

    @Override
    public String getName() {
        return ClineDefaultTool.ASK.getValue();
    }

    @Override
    public String getDescription(ToolUse block) {
        String q = HandlerUtils.getStringParam(block, "question");
        return "[" + block.getName() + " for '" + (q == null ? "" : q) + "']";
    }

    @Override
    public ClineToolSpec getClineToolSpec() {
        return AskFollowupQuestionTool.create(ModelFamily.GENERIC);
    }

    @Override
    public void handlePartialBlock(ToolUse block, UIHelpers ui) {
        String question = HandlerUtils.getStringParam(block, "question");
        String optionsRaw = HandlerUtils.getStringParam(block, "options");

        String message;
        try {
            String cleanedQuestion = question == null ? "" : question;
            List<String> options =
                    PartialJsonUtils.parseArrayString(optionsRaw == null ? "[]" : optionsRaw);

            Map<String, Object> payload = new HashMap<>();
            payload.put("question", cleanedQuestion);
            payload.put("options", options);

            message = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize message", e);
        }

        ui.ask(ClineAsk.FOLLOWUP, message, block.isPartial(), null);
    }

    @Override
    public ToolExecuteResult execute(ToolContext context, ToolUse block) {
        String question = HandlerUtils.getStringParam(block, "question");
        String optionsRaw = HandlerUtils.getStringParam(block, "options");

        // In yolo mode, don't wait for user input - instruct AI to use tools instead
        if (context.isYoloModeToggled()) {
            String truncatedQuestion =
                    question.length() > 100 ? question.substring(0, 100) + "..." : question;
            context.getCallbacks()
                    .say(
                            ClineSay.INFO,
                            "[YOLO MODE] Auto-responding to question: \""
                                    + truncatedQuestion
                                    + "\"",
                            null,
                            null,
                            false,
                            null);
            return HandlerUtils.createToolExecuteResult(
                    formatResponse.toolResult(
                            "[YOLO MODE: User input is not available in non-interactive mode. "
                                    + "You must use available tools (read_file, list_files, search_files, etc.) "
                                    + "to gather the information you need instead of asking the user. "
                                    + "Proceed with using tools to find the answer to your question: \""
                                    + question
                                    + "\"]",
                            null,
                            null));
        }

        if (context.getAutoApprovalSettings() != null
                && context.getAutoApprovalSettings().isEnabled()
                && context.getAutoApprovalSettings().isEnableNotifications()) {
            String notificationMsg = question.replace("\n", " ");
            if (notificationMsg.length() > 100) {
                notificationMsg = notificationMsg.substring(0, 100) + "...";
            }
            context.getServices()
                    .getNotificationService()
                    .showNotification(
                            "Cline has a question...", notificationMsg, NotificationType.INFO);
        }

        List<String> options = PartialJsonUtils.parseArrayString(optionsRaw);

        String sharedMessage;
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("question", question);
            payload.put("options", options);

            sharedMessage = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize options", e);
        }

        AskFollowupToolState state = (AskFollowupToolState) context.getToolState();
        state.setPhase(1);
        state.setQuestion(question);
        state.setOptions(options);
        state.setOptionsRaw(optionsRaw);

        var token =
                ToolResultUtils.askApprovalAndPushFeedbackForToken(
                        ClineAsk.FOLLOWUP,
                        sharedMessage,
                        context,
                        null,
                        block,
                        getDescription(block));
        return new ToolExecuteResult.PendingAsk(token);
    }

    @Override
    public ToolExecuteResult resume(
            ToolContext context, ToolUse block, ToolState toolState, AskResult askResult) {
        AskFollowupToolState state = (AskFollowupToolState) toolState;

        String text = askResult != null ? askResult.getText() : null;
        String[] images =
                askResult != null && askResult.getImages() != null
                        ? askResult.getImages().toArray(new String[0])
                        : null;
        String[] followupFiles =
                askResult != null && askResult.getFiles() != null
                        ? askResult.getFiles().toArray(new String[0])
                        : null;

        if (state.getOptionsRaw() != null && text != null && state.getOptions().contains(text)) {
            if (context.getServices() != null
                    && context.getServices().getTelemetryService() != null) {
                context.getServices()
                        .getTelemetryService()
                        .captureOptionSelected(context.getUlid(), state.getOptions().size(), "act");
            }

            List<ClineMessage> clineMessages = context.getMessageState().getClineMessages();
            ClineMessage lastFollowupMessage =
                    MessageUtils.findLast(
                            clineMessages, m -> m != null && ClineAsk.FOLLOWUP.equals(m.getAsk()));
            if (lastFollowupMessage != null) {
                try {
                    Map<String, Object> updatedPayload = new HashMap<>();
                    updatedPayload.put("question", state.getQuestion());
                    updatedPayload.put("options", state.getOptions());
                    updatedPayload.put("selected", text);

                    String updatedMessage = objectMapper.writeValueAsString(updatedPayload);
                    lastFollowupMessage.setText(updatedMessage);
                    context.getMessageState().saveClineMessagesAndUpdateHistory();
                } catch (JsonProcessingException e) {
                    throw new RuntimeException("Failed to serialize updated followup message", e);
                }
            }

            return formatAnswer(text, images, followupFiles);
        }

        if (context.getServices() != null && context.getServices().getTelemetryService() != null) {
            context.getServices()
                    .getTelemetryService()
                    .captureOptionsIgnored(context.getUlid(), state.getOptions().size(), "act");
        }

        context.getCallbacks()
                .say(
                        ClineSay.USER_FEEDBACK,
                        text == null ? "" : text,
                        images,
                        followupFiles,
                        false,
                        null);
        return formatAnswer(text, images, followupFiles);
    }

    private ToolExecuteResult formatAnswer(String text, String[] images, String[] files) {
        String t = text == null ? "" : text;

        String fileContentString = "";
        if (files != null && files.length > 0) {
            List<Path> filePaths = new ArrayList<>();
            for (String file : files) {
                if (file != null && !file.isEmpty()) {
                    filePaths.add(Paths.get(file));
                }
            }
            if (!filePaths.isEmpty()) {
                fileContentString = ExtractText.processFilesIntoText(filePaths);
            }
        }

        return HandlerUtils.createToolExecuteResult(
                formatResponse.toolResult(
                        "<answer>\n" + t + "\n</answer>", images, fileContentString));
    }
}
