package com.hhoa.kline.core.core.task.tools.handlers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hhoa.kline.core.core.assistant.ToolUse;
import com.hhoa.kline.core.core.assistant.UserContentBlock;
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
import com.hhoa.kline.core.core.task.tools.types.TaskConfig;
import com.hhoa.kline.core.core.task.tools.types.UIHelpers;
import com.hhoa.kline.core.core.utils.PartialJsonUtils;
import com.hhoa.kline.core.enums.ClineDefaultTool;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 追问工具处理器 处理向用户提问、选项选择、用户反馈
 *
 * @author hhoa
 */
public class AskFollowupQuestionToolHandler implements FullyManagedTool {

    private final ResponseFormatter formatResponse = new ResponseFormatter();
    private final ObjectMapper objectMapper = new ObjectMapper();

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
    public List<UserContentBlock> execute(TaskConfig config, ToolUse block) {
        String question = HandlerUtils.getStringParam(block, "question");
        String optionsRaw = HandlerUtils.getStringParam(block, "options");

        config.getTaskState().setConsecutiveMistakeCount(0);

        if (config.getAutoApprovalSettings() != null
                && config.getAutoApprovalSettings().isEnabled()
                && config.getAutoApprovalSettings().isEnableNotifications()) {
            String notificationMsg = question.replace("\n", " ");
            if (notificationMsg.length() > 100) {
                notificationMsg = notificationMsg.substring(0, 100) + "...";
            }
            config.getServices()
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

        AskResult res = config.getCallbacks().ask(ClineAsk.FOLLOWUP, sharedMessage, false, null);
        String text = res != null ? res.getText() : null;
        String[] images =
                res != null && res.getImages() != null
                        ? res.getImages().toArray(new String[0])
                        : null;
        String[] followupFiles =
                res != null && res.getFiles() != null
                        ? res.getFiles().toArray(new String[0])
                        : null;

        if (optionsRaw != null && text != null && options.contains(text)) {
            if (config.getServices() != null
                    && config.getServices().getTelemetryService() != null) {
                config.getServices()
                        .getTelemetryService()
                        .captureOptionSelected(config.getUlid(), options.size(), "act");
            }

            List<ClineMessage> clineMessages = config.getMessageState().getClineMessages();
            ClineMessage lastFollowupMessage =
                    MessageUtils.findLast(
                            clineMessages, m -> m != null && ClineAsk.FOLLOWUP.equals(m.getAsk()));
            if (lastFollowupMessage != null) {
                try {
                    Map<String, Object> updatedPayload = new HashMap<>();
                    updatedPayload.put("question", question);
                    updatedPayload.put("options", options);
                    updatedPayload.put("selected", text);

                    String updatedMessage = objectMapper.writeValueAsString(updatedPayload);
                    lastFollowupMessage.setText(updatedMessage);
                    config.getMessageState().saveClineMessagesAndUpdateHistory();
                } catch (JsonProcessingException e) {
                    throw new RuntimeException("Failed to serialize updated followup message", e);
                }
            }

            return formatAnswer(text, images, followupFiles);
        }

        if (config.getServices() != null && config.getServices().getTelemetryService() != null) {
            config.getServices()
                    .getTelemetryService()
                    .captureOptionsIgnored(config.getUlid(), options.size(), "act");
        }

        config.getCallbacks()
                .say(
                        ClineSay.USER_FEEDBACK,
                        text == null ? "" : text,
                        images,
                        followupFiles,
                        false,
                        null);
        return formatAnswer(text, images, followupFiles);
    }

    private List<UserContentBlock> formatAnswer(String text, String[] images, String[] files) {
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

        return HandlerUtils.createTextBlocks(
                formatResponse.toolResult(
                        "<answer>\n" + t + "\n</answer>", images, fileContentString));
    }
}
