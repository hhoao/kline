package com.hhoa.kline.core.core.task.tools.handlers;

import com.hhoa.ai.kline.commons.utils.JsonUtils;
import com.hhoa.kline.core.core.assistant.ToolUse;
import com.hhoa.kline.core.core.integrations.misc.ExtractText;
import com.hhoa.kline.core.core.prompts.ResponseFormatter;
import com.hhoa.kline.core.core.prompts.systemprompt.ClineToolSpec;
import com.hhoa.kline.core.core.shared.ClineAsk;
import com.hhoa.kline.core.core.shared.ClineMessageFormat;
import com.hhoa.kline.core.core.shared.ClineSay;
import com.hhoa.kline.core.core.shared.storage.types.Mode;
import com.hhoa.kline.core.core.task.AskResult;
import com.hhoa.kline.core.core.task.tools.types.ToolContext;
import com.hhoa.kline.core.core.task.tools.types.ToolExecuteResult;
import com.hhoa.kline.core.core.task.tools.types.ToolState;
import com.hhoa.kline.core.core.task.tools.types.UIHelpers;
import com.hhoa.kline.core.core.task.tools.utils.ToolResultUtils;
import com.hhoa.kline.core.core.utils.PartialJsonUtils;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

public class PlanModeRespondHandler implements StateFullToolHandler {

    private final ResponseFormatter formatResponse = new ResponseFormatter();

    @Getter
    @Setter
    public static class PlanModeRespondToolState extends ToolState {
        private List<String> options;
        private String optionsRaw;
        private String response;
    }

    @Override
    public ToolState createToolState() {
        return new PlanModeRespondToolState();
    }

    @Override
    public String getName() {
        return ClineAsk.PLAN_MODE_RESPOND.getValue();
    }

    @Override
    public String getDescription(ToolUse block) {
        return "[" + block.getName() + "]";
    }

    @Override
    public ClineToolSpec getClineToolSpec() {
        return ClineToolSpec.builder()
                .name(ClineAsk.PLAN_MODE_RESPOND.getValue())
                .parameters(
                        List.of(
                                ClineToolSpec.ClineToolSpecParameter.builder()
                                        .name("response")
                                        .required(true)
                                        .instruction("")
                                        .usage("")
                                        .build()))
                .build();
    }

    @Override
    public void handlePartialBlock(ToolUse block, UIHelpers ui) {
        String response = HandlerUtils.getStringParam(block, "response");
        String optionsRaw = HandlerUtils.getStringParam(block, "options");
        Map<String, Object> messageMap = new HashMap<>();
        messageMap.put("response", response);
        messageMap.put("options", String.join(",", PartialJsonUtils.parseArrayString(optionsRaw)));
        String message = JsonUtils.toJsonString(messageMap);
        ui.ask(ClineAsk.PLAN_MODE_RESPOND, message, true, ClineMessageFormat.JSON);
    }

    @Override
    public ToolExecuteResult execute(ToolContext context, ToolUse block) {
        String response = HandlerUtils.getStringParam(block, "response");
        String optionsRaw = HandlerUtils.getStringParam(block, "options");
        boolean needsMoreExploration =
                "true"
                        .equalsIgnoreCase(
                                HandlerUtils.getStringParam(block, "needs_more_exploration"));

        // Validate required parameters
        if (response == null || response.trim().isEmpty()) {
            context.getTaskState()
                    .setConsecutiveMistakeCount(
                            context.getTaskState().getConsecutiveMistakeCount() + 1);
            return HandlerUtils.createToolExecuteResult(
                    new ResponseFormatter().missingToolParameterError("response"));
        }
        context.getTaskState().setConsecutiveMistakeCount(0);

        if (needsMoreExploration) {
            return HandlerUtils.createToolExecuteResult(
                    formatResponse.toolResult(
                            "[You have indicated that you need more exploration. Proceed with calling tools to continue the planning process.]",
                            null,
                            null));
        }

        if (context.isYoloModeToggled() && Mode.ACT.equals(context.getMode())) {
            return HandlerUtils.createToolExecuteResult(
                    formatResponse.toolResult("[Go ahead and execute.]", null, null));
        }

        List<String> options =
                PartialJsonUtils.parseArrayString(optionsRaw != null ? optionsRaw : "[]");
        Map<String, Object> sharedMessageMap = new HashMap<>();
        sharedMessageMap.put("response", response);
        sharedMessageMap.put("options", String.join(",", options));
        String sharedMessage = JsonUtils.toJsonString(sharedMessageMap);

        if (Mode.PLAN.equals(context.getMode())
                && context.isYoloModeToggled()
                && !needsMoreExploration) {
            Boolean switched = context.getCallbacks().switchToActMode();
            if (Boolean.TRUE.equals(switched)) {
                return HandlerUtils.createToolExecuteResult(
                        formatResponse.toolResult(
                                "[The user has switched to ACT MODE, so you may now proceed with the task.]",
                                null,
                                null));
            }
        }

        // Set awaiting plan response state
        context.getTaskState().setAwaitingPlanResponse(true);

        PlanModeRespondToolState state = (PlanModeRespondToolState) context.getToolState();
        state.setPhase(1);
        state.setOptions(options);
        state.setOptionsRaw(optionsRaw);
        state.setResponse(response);

        var token =
                ToolResultUtils.askApprovalAndPushFeedbackForToken(
                        ClineAsk.PLAN_MODE_RESPOND,
                        sharedMessage,
                        context,
                        ClineMessageFormat.JSON,
                        block,
                        getDescription(block));
        return new ToolExecuteResult.PendingAsk(token);
    }

    @Override
    public ToolExecuteResult resume(
            ToolContext context, ToolUse block, ToolState toolState, AskResult askResult) {
        PlanModeRespondToolState state = (PlanModeRespondToolState) toolState;

        context.getTaskState().setAwaitingPlanResponse(false);

        String text = askResult != null ? askResult.getText() : null;
        String[] images =
                askResult != null && askResult.getImages() != null
                        ? askResult.getImages().toArray(new String[0])
                        : null;
        String[] files =
                askResult != null && askResult.getFiles() != null
                        ? askResult.getFiles().toArray(new String[0])
                        : null;

        String finalText =
                "PLAN_MODE_TOGGLE_RESPONSE".equals(text) ? "" : (text == null ? "" : text);

        if (state.getOptionsRaw() != null
                && finalText != null
                && !finalText.isEmpty()
                && PartialJsonUtils.parseArrayString(state.getOptionsRaw()).contains(finalText)) {
            if (context.getServices() != null
                    && context.getServices().getTelemetryService() != null) {
                context.getServices()
                        .getTelemetryService()
                        .captureOptionSelected(
                                context.getUlid(), state.getOptions().size(), "plan");
            }

            return HandlerUtils.createToolExecuteResult(
                    formatResponse.toolResult(
                            "<user_message>\n" + finalText + "\n</user_message>", null, null));
        }

        boolean hasFeedback =
                (finalText != null && !finalText.isEmpty())
                        || (images != null && images.length > 0)
                        || (files != null && files.length > 0);
        if (hasFeedback) {
            if (context.getServices() != null
                    && context.getServices().getTelemetryService() != null) {
                context.getServices()
                        .getTelemetryService()
                        .captureOptionsIgnored(
                                context.getUlid(), state.getOptions().size(), "plan");
            }

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
                            finalText == null ? "" : finalText,
                            images,
                            files,
                            false,
                            null);

            if (context.getTaskState().isDidRespondToPlanAskBySwitchingMode()) {
                StringBuilder sb = new StringBuilder();
                sb.append(
                        "[The user has switched to ACT MODE, so you may now proceed with the task.]");
                if (finalText != null && !finalText.isEmpty()) {
                    sb.append(
                                    "\n\nThe user also provided the following message when switching to ACT MODE:\n<user_message>\n")
                            .append(finalText)
                            .append("\n</user_message>");
                }
                context.getTaskState().setDidRespondToPlanAskBySwitchingMode(false);
                return HandlerUtils.createToolExecuteResult(
                        formatResponse.toolResult(sb.toString(), images, fileContentString));
            } else {
                return HandlerUtils.createToolExecuteResult(
                        formatResponse.toolResult(
                                "<user_message>\n"
                                        + (finalText == null ? "" : finalText)
                                        + "\n</user_message>",
                                images,
                                fileContentString));
            }
        }

        return HandlerUtils.createToolExecuteResult(
                formatResponse.toolResult("<user_message>\n\n</user_message>", null, null));
    }
}
