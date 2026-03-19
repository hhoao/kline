package com.hhoa.kline.core.core.task.handler;

import static com.hhoa.kline.core.core.integrations.misc.ExtractText.processFilesIntoText;

import com.hhoa.kline.core.core.assistant.ImageContentBlock;
import com.hhoa.kline.core.core.assistant.TextContentBlock;
import com.hhoa.kline.core.core.assistant.UserContentBlock;
import com.hhoa.kline.core.core.shared.ClineSay;
import com.hhoa.kline.core.core.task.MessageStateHandler;
import com.hhoa.kline.core.core.task.TaskState;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class TaskV2StartTaskHandler {

    private final MessageStateHandler messageStateHandler;
    private final Runnable postStateToWebview;
    private final TaskV2SayAskHandler sayAskHandler;
    private final TaskState taskState;

    public TaskV2StartTaskHandler(
            MessageStateHandler messageStateHandler,
            Runnable postStateToWebview,
            TaskV2SayAskHandler sayAskHandler,
            TaskState taskState) {
        this.messageStateHandler = messageStateHandler;
        this.postStateToWebview = postStateToWebview;
        this.sayAskHandler = sayAskHandler;
        this.taskState = taskState;
    }

    public void startTask(String taskText, List<String> images, List<String> files) {
        messageStateHandler.setClineMessages(new ArrayList<>());
        messageStateHandler.getApiConversationHistory().clear();
        if (postStateToWebview != null) {
            postStateToWebview.run();
        }
        sayAskHandler.say(ClineSay.TEXT, taskText, images, files, null);
        taskState.setInitialized(true);

        List<UserContentBlock> userContent = new ArrayList<>();

        if (taskText != null && !taskText.isEmpty()) {
            TextContentBlock textBlock =
                    new TextContentBlock(String.format("<task>\n%s\n</task>", taskText));
            userContent.add(textBlock);
        }

        if (images != null && !images.isEmpty()) {
            for (String image : images) {
                ImageContentBlock imageBlock = new ImageContentBlock(image, "base64", "image/png");
                userContent.add(imageBlock);
            }
        }

        if (files != null && !files.isEmpty()) {
            String fileContentString =
                    processFilesIntoText(files.stream().map(Path::of).collect(Collectors.toList()));
            if (!fileContentString.isEmpty()) {
                TextContentBlock fileBlock = new TextContentBlock(fileContentString);
                userContent.add(fileBlock);
            }
        }

        taskState.setCurrentUserContent(userContent);
        taskState.setCurrentIncludeFileDetails(true);
    }
}
