package com.hhoa.kline.core.core.tools.handlers;

import com.hhoa.kline.core.core.assistant.ToolUse;
import com.hhoa.kline.core.core.prompts.ResponseFormatter;
import com.hhoa.kline.core.core.prompts.systemprompt.ClineToolSpec;
import com.hhoa.kline.core.core.shared.ClineAsk;
import com.hhoa.kline.core.core.shared.ClineSay;
import com.hhoa.kline.core.core.task.AskResult;
import com.hhoa.kline.core.core.tools.types.ToolContext;
import com.hhoa.kline.core.core.tools.types.ToolExecuteResult;
import com.hhoa.kline.core.core.tools.types.ToolState;
import com.hhoa.kline.core.core.tools.types.UIHelpers;
import com.hhoa.kline.core.core.tools.utils.ToolResultUtils;
import com.hhoa.kline.core.enums.ClineDefaultTool;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * apply_patch 工具处理器。
 *
 * <p>对应 Cline TS 版本的 ApplyPatchHandler.ts。 处理统一 diff 格式的补丁应用，支持 ADD / UPDATE / DELETE 三种操作。
 */
@Slf4j
public class ApplyPatchHandler implements StateFullToolHandler {

    private static final String PATCH_BEGIN = "*** Begin Patch";
    private static final String PATCH_END = "*** End Patch";
    private static final String PATCH_ADD = "*** Add File: ";
    private static final String PATCH_UPDATE = "*** Update File: ";
    private static final String PATCH_DELETE = "*** Delete File: ";
    private static final String PATCH_MOVE = "*** Move to: ";
    private static final String PATCH_SECTION = "@@";

    private static final Pattern BASH_WRAPPER_PATTERN =
            Pattern.compile("^(cat <<\\s*'?PATCH'?|cat << 'PATCH'|EOF)$");

    private final ResponseFormatter formatResponse = new ResponseFormatter();

    /** ApplyPatch 的阶段状态 */
    @Getter
    @Setter
    public static class ApplyPatchToolState extends ToolState {
        private String completeMessage;
    }

    @Override
    public String getName() {
        return ClineDefaultTool.APPLY_PATCH.getValue();
    }

    @Override
    public String getDescription(ToolUse block) {
        return "[" + getName() + " for patch application]";
    }

    @Override
    public ClineToolSpec getClineToolSpec() {
        return null;
    }

    @Override
    public void handlePartialBlock(ToolUse block, UIHelpers uiHelpers) {
        String rawInput = HandlerUtils.getStringParam(block, "input");
        if (rawInput == null || rawInput.isEmpty()) {
            return;
        }

        try {
            List<String> allFiles = extractAllFiles(rawInput);
            if (allFiles.isEmpty()) {
                return;
            }
            // 流式预览第一个文件
            uiHelpers.say(
                    ClineSay.TOOL,
                    "{\"tool\":\"editedExistingFile\",\"path\":\""
                            + allFiles.get(0)
                            + "\",\"content\":\""
                            + escapeJson(rawInput)
                            + "\"}",
                    null,
                    null,
                    true,
                    null);
        } catch (Exception e) {
            // 等待更多数据
        }
    }

    @Override
    public ToolExecuteResult execute(ToolContext context, ToolUse block) {
        String rawInput = HandlerUtils.getStringParam(block, "input");

        try {
            List<String> lines = preprocessLines(rawInput);
            List<String> changedFiles = extractAllFiles(rawInput);

            if (changedFiles.isEmpty()) {
                return new ToolExecuteResult.Immediate(
                        HandlerUtils.createTextBlocks(
                                "No files found in patch input. Ensure patch follows the correct format."));
            }

            // 构建完整的工具消息
            String completeMessage =
                    "{\"tool\":\"editedExistingFile\",\"path\":\""
                            + changedFiles.get(0)
                            + "\",\"content\":\""
                            + escapeJson(rawInput)
                            + "\"}";

            ApplyPatchToolState toolState = getToolState(context, ApplyPatchToolState.class);
            toolState.setCompleteMessage(completeMessage);

            // 检查是否需要审批
            Boolean autoApprove =
                    context.getCallbacks()
                            .shouldAutoApproveToolWithPath(block.getName(), changedFiles.get(0));

            if (autoApprove != null && autoApprove) {
                context.getCallbacks().say(ClineSay.TOOL, completeMessage, null, null, false, null);
                context.getTaskState().setDidEditFile(true);

                StringBuilder response = new StringBuilder();
                response.append("Successfully applied patch to the following files:");
                for (String file : changedFiles) {
                    response.append("\n").append(file);
                }
                return new ToolExecuteResult.Immediate(
                        HandlerUtils.createTextBlocks(response.toString()));
            }

            // 需要用户审批 — ask
            var token =
                    ToolResultUtils.askApprovalAndPushFeedbackForToken(
                            ClineAsk.TOOL,
                            completeMessage,
                            context,
                            null,
                            block,
                            getDescription(block));
            return new ToolExecuteResult.PendingAsk(token);

        } catch (Exception e) {
            log.error("Error applying patch", e);
            return new ToolExecuteResult.Immediate(
                    HandlerUtils.createTextBlocks("Error applying patch: " + e.getMessage()));
        }
    }

    @Override
    public ToolState createToolState() {
        return new ApplyPatchToolState();
    }

    @Override
    public ToolExecuteResult resume(
            ToolContext context, ToolUse block, ToolState state, AskResult askResult) {
        boolean approved = ToolResultUtils.processAskResult(askResult, context);
        if (!approved) {
            return new ToolExecuteResult.Immediate(
                    HandlerUtils.createTextBlocks(formatResponse.toolDenied()));
        }

        // 用户批准
        context.getTaskState().setDidEditFile(true);

        return new ToolExecuteResult.Immediate(
                HandlerUtils.createTextBlocks("Successfully applied patch. Changes saved."));
    }

    // ── Patch 解析辅助方法 ──

    private List<String> preprocessLines(String text) {
        String[] rawLines = text.split("\n");
        List<String> lines = new ArrayList<>();
        for (String line : rawLines) {
            lines.add(line.replaceAll("\\r$", ""));
        }
        lines = stripBashWrapper(lines);

        boolean hasBegin = !lines.isEmpty() && lines.get(0).startsWith(PATCH_BEGIN);
        boolean hasEnd = !lines.isEmpty() && lines.get(lines.size() - 1).equals(PATCH_END);

        if (!hasBegin && !hasEnd) {
            List<String> wrapped = new ArrayList<>();
            wrapped.add(PATCH_BEGIN);
            wrapped.addAll(lines);
            wrapped.add(PATCH_END);
            return wrapped;
        }
        if (hasBegin && hasEnd) {
            return lines;
        }
        throw new IllegalArgumentException(
                "Invalid patch text - incomplete sentinels. Try breaking it into smaller patches.");
    }

    private List<String> stripBashWrapper(List<String> lines) {
        List<String> result = new ArrayList<>();
        boolean insidePatch = false;
        boolean foundBegin = false;
        boolean foundContent = false;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (!insidePatch && BASH_WRAPPER_PATTERN.matcher(line).matches()) {
                continue;
            }
            if (line.startsWith(PATCH_BEGIN)) {
                insidePatch = true;
                foundBegin = true;
                result.add(line);
                continue;
            }
            if (line.equals(PATCH_END)) {
                insidePatch = false;
                result.add(line);
                continue;
            }

            boolean isPatchContent = isPatchLine(line);
            if (isPatchContent && i != lines.size() - 1) {
                foundContent = true;
            }

            if (insidePatch
                    || (!foundBegin && isPatchContent)
                    || (line.isEmpty() && foundContent)) {
                result.add(line);
            }
        }

        while (!result.isEmpty() && result.get(result.size() - 1).isEmpty()) {
            result.remove(result.size() - 1);
        }

        return !foundBegin && !foundContent ? lines : result;
    }

    private boolean isPatchLine(String line) {
        return line.startsWith(PATCH_ADD)
                || line.startsWith(PATCH_UPDATE)
                || line.startsWith(PATCH_DELETE)
                || line.startsWith(PATCH_MOVE)
                || line.startsWith(PATCH_SECTION)
                || line.startsWith("+")
                || line.startsWith("-")
                || line.startsWith(" ")
                || line.equals("***");
    }

    private List<String> extractFilesForOperations(String text, String... markers) {
        List<String> lines = stripBashWrapper(List.of(text.split("\n")));
        List<String> files = new ArrayList<>();
        for (String line : lines) {
            for (String marker : markers) {
                if (line.startsWith(marker)) {
                    String file = line.substring(marker.length()).trim();
                    if (!text.trim().endsWith(file)) {
                        files.add(file);
                    }
                    break;
                }
            }
        }
        return files;
    }

    private List<String> extractAllFiles(String text) {
        return extractFilesForOperations(text, PATCH_ADD, PATCH_UPDATE, PATCH_DELETE);
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    @SuppressWarnings("unchecked")
    private <T extends ToolState> T getToolState(ToolContext context, Class<T> clazz) {
        ToolState state = context.getToolState();
        if (state != null && clazz.isInstance(state)) {
            return (T) state;
        }
        try {
            T newState = clazz.getDeclaredConstructor().newInstance();
            return newState;
        } catch (Exception e) {
            throw new RuntimeException("Cannot create tool state", e);
        }
    }
}
