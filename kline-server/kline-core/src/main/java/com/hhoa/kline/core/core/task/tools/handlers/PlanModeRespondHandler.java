package com.hhoa.kline.core.core.task.tools.handlers;

import com.hhoa.ai.kline.commons.utils.JsonUtils;
import com.hhoa.kline.core.core.assistant.ToolUse;
import com.hhoa.kline.core.core.assistant.UserContentBlock;
import com.hhoa.kline.core.core.integrations.misc.ExtractText;
import com.hhoa.kline.core.core.prompts.ResponseFormatter;
import com.hhoa.kline.core.core.prompts.systemprompt.ClineToolSpec;
import com.hhoa.kline.core.core.shared.ClineAsk;
import com.hhoa.kline.core.core.shared.ClineMessageFormat;
import com.hhoa.kline.core.core.shared.ClineSay;
import com.hhoa.kline.core.core.shared.storage.types.Mode;
import com.hhoa.kline.core.core.task.AskResult;
import com.hhoa.kline.core.core.task.tools.types.TaskConfig;
import com.hhoa.kline.core.core.task.tools.types.UIHelpers;
import com.hhoa.kline.core.core.utils.PartialJsonUtils;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlanModeRespondHandler implements FullyManagedTool {

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
    public List<UserContentBlock> execute(TaskConfig config, ToolUse block) {
        String response = HandlerUtils.getStringParam(block, "response");
        String optionsRaw = HandlerUtils.getStringParam(block, "options");
        boolean needsMoreExploration =
                "true"
                        .equalsIgnoreCase(
                                HandlerUtils.getStringParam(block, "needs_more_exploration"));

        config.getTaskState().setConsecutiveMistakeCount(0);

        ResponseFormatter formatResponse = new ResponseFormatter();

        if (needsMoreExploration) {
            return HandlerUtils.createTextBlocks(
                    formatResponse.toolResult(
                            "[You have indicated that you need more exploration. Proceed with calling tools to continue the planning process.]",
                            null,
                            null));
        }

        if (config.isYoloModeToggled() && Mode.ACT.equals(config.getMode())) {
            return HandlerUtils.createTextBlocks(
                    formatResponse.toolResult("[Go ahead and execute.]", null, null));
        }

        List<String> options =
                PartialJsonUtils.parseArrayString(optionsRaw != null ? optionsRaw : "[]");
        Map<String, Object> sharedMessageMap = new HashMap<>();
        sharedMessageMap.put("response", response);
        sharedMessageMap.put("options", String.join(",", options));
        String sharedMessage = JsonUtils.toJsonString(sharedMessageMap);

        if (Mode.PLAN.equals(config.getMode())
                && config.isYoloModeToggled()
                && !needsMoreExploration) {
            Boolean switched = config.getCallbacks().switchToActMode();
            if (Boolean.TRUE.equals(switched)) {
                return HandlerUtils.createTextBlocks(
                        new ResponseFormatter()
                                .toolResult(
                                        "[The user has switched to ACT MODE, so you may now proceed with the task.]",
                                        null,
                                        null));
            } else {
                return askAndFormat(config, sharedMessage, options, response, optionsRaw);
            }
        }

        config.getTaskState().setAwaitingPlanResponse(true);

        return askAndFormat(config, sharedMessage, options, response, optionsRaw);
    }

    private static List<UserContentBlock> askAndFormat(
            TaskConfig config,
            String sharedMessage,
            List<String> options,
            String response,
            String optionsRaw) {
        ResponseFormatter formatResponse = new ResponseFormatter();
        AskResult res =
                config.getCallbacks()
                        .ask(
                                ClineAsk.PLAN_MODE_RESPOND,
                                sharedMessage,
                                false,
                                ClineMessageFormat.JSON);
        config.getTaskState().setAwaitingPlanResponse(false);

        String text = res != null ? res.getText() : null;
        String[] images =
                res != null && res.getImages() != null
                        ? res.getImages().toArray(new String[0])
                        : null;
        String[] files =
                res != null && res.getFiles() != null
                        ? res.getFiles().toArray(new String[0])
                        : null;

        String finalText =
                "PLAN_MODE_TOGGLE_RESPONSE".equals(text) ? "" : (text == null ? "" : text);
        String[] finalImages = images;
        String[] finalFiles = files;

        if (optionsRaw != null
                && finalText != null
                && !finalText.isEmpty()
                && PartialJsonUtils.parseArrayString(optionsRaw).contains(finalText)) {
            if (config.getServices() != null
                    && config.getServices().getTelemetryService() != null) {
                config.getServices()
                        .getTelemetryService()
                        .captureOptionSelected(config.getUlid(), options.size(), "plan");
            }

            return HandlerUtils.createTextBlocks(
                    formatResponse.toolResult(
                            "<user_message>\n" + finalText + "\n</user_message>", null, null));
        } else {
            if ((finalText != null && !finalText.isEmpty())
                    || (finalImages != null && finalImages.length > 0)
                    || (finalFiles != null && finalFiles.length > 0)) {
                if (config.getServices() != null
                        && config.getServices().getTelemetryService() != null) {
                    config.getServices()
                            .getTelemetryService()
                            .captureOptionsIgnored(config.getUlid(), options.size(), "plan");
                }

                String fileContentString = "";
                if (finalFiles != null && finalFiles.length > 0) {
                    List<Path> filePaths = new ArrayList<>();
                    for (String file : finalFiles) {
                        filePaths.add(Paths.get(file));
                    }
                    fileContentString = ExtractText.processFilesIntoText(filePaths);
                }

                config.getCallbacks()
                        .say(
                                ClineSay.USER_FEEDBACK,
                                finalText == null ? "" : finalText,
                                finalImages,
                                finalFiles,
                                false,
                                null);

                if (config.getTaskState().isDidRespondToPlanAskBySwitchingMode()) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(
                            "[The user has switched to ACT MODE, so you may now proceed with the task.]");
                    if (finalText != null && !finalText.isEmpty()) {
                        sb.append(
                                        "\n\nThe user also provided the following message when switching to ACT MODE:\n<user_message>\n")
                                .append(finalText)
                                .append("\n</user_message>");
                    }
                    config.getTaskState().setDidRespondToPlanAskBySwitchingMode(false);
                    return HandlerUtils.createTextBlocks(
                            formatResponse.toolResult(
                                    sb.toString(), finalImages, fileContentString));
                } else {
                    return HandlerUtils.createTextBlocks(
                            formatResponse.toolResult(
                                    "<user_message>\n"
                                            + (finalText == null ? "" : finalText)
                                            + "\n</user_message>",
                                    finalImages,
                                    fileContentString));
                }
            } else {
                return HandlerUtils.createTextBlocks(
                        formatResponse.toolResult("<user_message>\n\n</user_message>", null, null));
            }
        }
    }
}
