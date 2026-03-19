package com.hhoa.kline.core.core.task.handler;

import com.hhoa.kline.core.core.shared.ClineAsk;
import com.hhoa.kline.core.core.shared.ClineMessageFormat;
import com.hhoa.kline.core.core.shared.ClineMessageType;
import com.hhoa.kline.core.core.shared.ClineSay;
import com.hhoa.kline.core.core.task.AskPending;
import com.hhoa.kline.core.core.task.ClineMessage;
import com.hhoa.kline.core.core.task.MessageStateHandler;
import com.hhoa.kline.core.core.task.TaskState;
import com.hhoa.kline.core.core.utils.PartialJsonUtils;
import com.hhoa.kline.core.subscription.DefaultSubscriptionManager;
import com.hhoa.kline.core.subscription.SubscriptionManager;
import com.hhoa.kline.core.subscription.message.PartialMessage;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class TaskV2SayAskHandler {

    private final AtomicLong tsGenerator;
    private final MessageStateHandler messageStateHandler;
    private final TaskState taskState;
    private final Runnable postStateToWebview;
    private final String taskId;

    public TaskV2SayAskHandler(
            AtomicLong tsGenerator,
            MessageStateHandler messageStateHandler,
            TaskState taskState,
            Runnable postStateToWebview,
            String taskId) {
        this.tsGenerator = tsGenerator;
        this.messageStateHandler = messageStateHandler;
        this.taskState = taskState;
        this.postStateToWebview = postStateToWebview;
        this.taskId = taskId;
    }

    private long nowTs() {
        long base = System.currentTimeMillis();
        long last = tsGenerator.get();
        long next = Math.max(base, last + 1);
        tsGenerator.set(next);
        return next;
    }

    private void sendPartialMessageEvent(
            ClineMessage message, String partialContent, Boolean isUpdatingPreviousPartial) {
        if ((partialContent == null || partialContent.isEmpty())
                && Boolean.TRUE.equals(isUpdatingPreviousPartial)) {
            return;
        }
        SubscriptionManager defaultSubscriptionManager = DefaultSubscriptionManager.getInstance();
        PartialMessage partialMessage = new PartialMessage();
        partialMessage.setTs(message.getTs());
        partialMessage.setTaskId(taskId);
        partialMessage.setClineMessageType(message.getType());
        partialMessage.setPendingId(message.getPendingId());
        partialMessage.setAsk(message.getAsk());
        partialMessage.setSay(message.getSay());
        partialMessage.setIncrementContent(partialContent);
        partialMessage.setReasoning(message.getReasoning());
        partialMessage.setImages(message.getImages());
        partialMessage.setFiles(message.getFiles());
        partialMessage.setCommandCompleted(message.getCommandCompleted());
        partialMessage.setFormat(message.getFormat());
        partialMessage.setIsUpdatingPreviousPartial(Boolean.TRUE.equals(isUpdatingPreviousPartial));
        defaultSubscriptionManager.send(partialMessage);
    }

    private void resetAskResponse() {
        taskState.setAskResponse(null);
        taskState.setAskResponseText(null);
        taskState.setAskResponseImages(null);
        taskState.setAskResponseFiles(null);
    }

    public AskPending ask(ClineAsk type, String text, Boolean partial) {
        return ask(type, text, partial, null);
    }

    public AskPending ask(ClineAsk type, String text, Boolean partial, ClineMessageFormat format) {
        String partialContent = null;
        if (partial != null) {
            List<ClineMessage> clineMessages = messageStateHandler.getClineMessages();
            ClineMessage lastMessage = clineMessages.isEmpty() ? null : clineMessages.getLast();
            boolean isUpdatingPreviousPartial =
                    lastMessage != null && Boolean.TRUE.equals(lastMessage.getPartial());
            String newText = text != null ? text : "";
            if (isUpdatingPreviousPartial && lastMessage != null) {
                String oldText = lastMessage.getText() != null ? lastMessage.getText() : "";
                boolean shouldUseJsonDiff = ClineMessageFormat.JSON.equals(format);
                if (shouldUseJsonDiff) {
                    partialContent = PartialJsonUtils.getJsonPartialContent(text, newText, oldText);
                } else {
                    if (oldText.length() > newText.length()) {
                        log.error(
                                "Old text length larger than new text oldText: {}, newText: {}",
                                oldText,
                                newText);
                    }
                    partialContent = newText.substring(oldText.length());
                }
            } else {
                partialContent = newText;
            }
        }
        return ask(type, text, partialContent, partial, format);
    }

    public AskPending ask(ClineAsk type, String text) {
        return ask(type, text, null);
    }

    public AskPending ask(ClineAsk type, String text, String partialContent, Boolean partial) {
        return ask(type, text, partialContent, partial, null);
    }

    public AskPending ask(
            ClineAsk type,
            String text,
            String partialContent,
            Boolean partial,
            ClineMessageFormat format) {
        if (taskState.isAbort()) {
            throw new IllegalStateException("Cline instance aborted");
        }

        AskPending result = new AskPending();
        final long askTs = nowTs();
        taskState.setLastMessageTs(askTs);
        ClineMessage msg =
                new ClineMessage()
                        .setTs(askTs)
                        .setType(ClineMessageType.ASK)
                        .setAsk(type)
                        .setText(text)
                        .setPartial(partial)
                        .setFormat(format);

        if (partial == null || (!partial)) {
            String pendingId = UUID.randomUUID().toString();
            msg.setPendingId(pendingId);
            result.setPendingId(pendingId);
        }

        boolean isUpdatingPreviousPartial = false;
        if (partial != null) {
            List<ClineMessage> clineMessages = messageStateHandler.getClineMessages();
            ClineMessage lastMessage = clineMessages.isEmpty() ? null : clineMessages.getLast();
            isUpdatingPreviousPartial =
                    lastMessage != null && Boolean.TRUE.equals(lastMessage.getPartial());
        }
        if (isUpdatingPreviousPartial) {
            messageStateHandler.updateClineMessage(
                    messageStateHandler.getClineMessages().size() - 1, msg);
        } else {
            messageStateHandler.addToClineMessages(msg);
        }
        sendPartialMessageEvent(msg, partialContent, isUpdatingPreviousPartial);

        if (partial == null || (!partial)) {
            resetAskResponse();
            postStateToWebview.run();
        }
        return result;
    }

    public synchronized void say(
            ClineSay type, String text, List<String> images, List<String> files, Boolean partial) {
        say(type, text, images, files, partial, null);
    }

    public synchronized void say(
            ClineSay type,
            String text,
            List<String> images,
            List<String> files,
            Boolean partial,
            ClineMessageFormat format) {
        String incrementContent = null;
        if (partial != null) {
            List<ClineMessage> clineMessages = messageStateHandler.getClineMessages();
            ClineMessage lastMessage = clineMessages.isEmpty() ? null : clineMessages.getLast();
            boolean isUpdatingPreviousPartial =
                    lastMessage != null && Boolean.TRUE.equals(lastMessage.getPartial());
            String newText = text != null ? text : "";
            if (isUpdatingPreviousPartial && lastMessage != null) {
                String oldText = lastMessage.getText() != null ? lastMessage.getText() : "";
                boolean shouldUseJsonDiff = ClineMessageFormat.JSON.equals(format);
                if (shouldUseJsonDiff) {
                    incrementContent =
                            PartialJsonUtils.getJsonPartialContent(text, newText, oldText);
                } else {
                    incrementContent = newText.substring(oldText.length());
                }
            } else {
                incrementContent = newText;
            }
        }
        say(type, text, incrementContent, images, files, partial, format);
    }

    public synchronized void say(
            ClineSay type,
            String text,
            String incrementContent,
            List<String> images,
            List<String> files,
            Boolean partial) {
        say(type, text, incrementContent, images, files, partial, null);
    }

    public synchronized void say(
            ClineSay type,
            String text,
            String incrementContent,
            List<String> images,
            List<String> files,
            Boolean partial,
            ClineMessageFormat format) {
        if (taskState.isAbort()) {
            throw new IllegalStateException("Cline instance aborted");
        }
        if ((text == null || text.isEmpty())
                && (incrementContent == null || incrementContent.isEmpty())
                && partial != null
                && partial) {
            return;
        }
        long sayTs = nowTs();
        taskState.setLastMessageTs(sayTs);
        ClineMessage msg =
                new ClineMessage()
                        .setType(ClineMessageType.SAY)
                        .setSay(type)
                        .setText(text)
                        .setImages(images)
                        .setFiles(files)
                        .setPartial(partial)
                        .setFormat(format)
                        .setTs(sayTs);

        boolean isUpdatingPreviousPartial = false;
        if (partial != null) {
            List<ClineMessage> clineMessages = messageStateHandler.getClineMessages();
            ClineMessage lastMessage = clineMessages.isEmpty() ? null : clineMessages.getLast();
            isUpdatingPreviousPartial =
                    lastMessage != null && Boolean.TRUE.equals(lastMessage.getPartial());
        }
        if (isUpdatingPreviousPartial) {
            messageStateHandler.updateClineMessage(
                    messageStateHandler.getClineMessages().size() - 1, msg);
        } else {
            messageStateHandler.addToClineMessages(msg);
        }
        sendPartialMessageEvent(msg, incrementContent, isUpdatingPreviousPartial);
    }

    public void say(ClineSay type, String text) {
        say(type, text, text, null, null, null, null);
    }

    public String sayAndCreateMissingParamError(String toolName, String paramName, String relPath) {
        String message = "Cline tried to use " + toolName;
        if (relPath != null && !relPath.isEmpty()) {
            message += " for '" + relPath + "'";
        }
        message += " without value for required parameter '" + paramName + "'. Retrying...";
        say(ClineSay.ERROR, message);
        return "(missing param: " + paramName + ")";
    }
}
