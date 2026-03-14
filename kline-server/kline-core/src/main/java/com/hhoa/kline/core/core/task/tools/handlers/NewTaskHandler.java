package com.hhoa.kline.core.core.task.tools.handlers;

import com.hhoa.kline.core.core.assistant.ToolUse;
import com.hhoa.kline.core.core.assistant.UserContentBlock;
import com.hhoa.kline.core.core.integrations.misc.ExtractText;
import com.hhoa.kline.core.core.prompts.ResponseFormatter;
import com.hhoa.kline.core.core.prompts.systemprompt.ClineToolSpec;
import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.prompts.systemprompt.tools.NewTaskTool;
import com.hhoa.kline.core.core.shared.ClineAsk;
import com.hhoa.kline.core.core.shared.ClineSay;
import com.hhoa.kline.core.core.task.AskResult;
import com.hhoa.kline.core.core.task.tools.types.TaskConfig;
import com.hhoa.kline.core.core.task.tools.types.UIHelpers;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * 新任务工具处理器 处理创建新任务、用户反馈、通知显示
 *
 * @author hhoa
 */
@Slf4j
public class NewTaskHandler implements FullyManagedTool {

    private final ResponseFormatter formatResponse = new ResponseFormatter();

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
    public List<UserContentBlock> execute(TaskConfig config, ToolUse block) {
        String context = HandlerUtils.getStringParam(block, "context");

        config.getTaskState().setConsecutiveMistakeCount(0);

        if (config.getAutoApprovalSettings() != null
                && config.getAutoApprovalSettings().isEnabled()
                && config.getAutoApprovalSettings().isEnableNotifications()) {
            showSystemNotification(
                    "Cline wants to start a new task...",
                    "Cline is suggesting to start a new task with: " + context);
        }

        AskResult res = config.getCallbacks().ask(ClineAsk.NEW_TASK, context, false, null);
        String text = res != null ? res.getText() : null;
        String[] images =
                res != null && res.getImages() != null
                        ? res.getImages().toArray(new String[0])
                        : null;
        String[] files =
                res != null && res.getFiles() != null
                        ? res.getFiles().toArray(new String[0])
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

            config.getCallbacks()
                    .say(
                            ClineSay.USER_FEEDBACK,
                            text == null ? "" : text,
                            images,
                            files,
                            false,
                            null);

            String feedbackText = text == null ? "" : text;
            return HandlerUtils.createTextBlocks(
                    formatResponse.toolResult(
                            "The user provided feedback instead of creating a new task:\n<feedback>\n"
                                    + feedbackText
                                    + "\n</feedback>",
                            images,
                            fileContentString));
        } else {
            return HandlerUtils.createTextBlocks(
                    formatResponse.toolResult(
                            "The user has created a new task with the provided context.",
                            null,
                            null));
        }
    }

    private static void showSystemNotification(String subtitle, String message) {
        // TODO: 实现实际的系统通知功能
        log.info(String.format("[Notification] %s: %s", subtitle, message));
    }
}
