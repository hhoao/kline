package com.hhoa.kline.core.core.task.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hhoa.kline.core.core.assistant.AssistantMessageContent;
import com.hhoa.kline.core.core.assistant.TextContent;
import com.hhoa.kline.core.core.assistant.parser.ClineTagConfigs;
import com.hhoa.kline.core.core.assistant.parser.DefaultStreamingAssistantMessageParser;
import com.hhoa.kline.core.core.assistant.parser.StreamingAssistantMessageParser;
import com.hhoa.kline.core.core.task.ClineMessage;
import com.hhoa.kline.core.core.task.MessageStateHandler;
import com.hhoa.kline.core.core.task.TaskState;
import com.hhoa.kline.core.subscription.MessageSender;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import org.junit.jupiter.api.Test;

class TaskV2MessagePresenterHandlerTest {

    @Test
    void incrementalChunksAccumulateWithinSingleStreamingResponse() {
        TaskState taskState = new TaskState();
        MessageStateHandler messageStateHandler = new TestMessageStateHandler(taskState);
        TaskV2SayAskHandler sayAskHandler =
                new TaskV2SayAskHandler(
                        new AtomicLong(System.currentTimeMillis()),
                        messageStateHandler,
                        taskState,
                        () -> {},
                        "task",
                        new NoOpMessageSender());
        StreamingAssistantMessageParser parser =
                new DefaultStreamingAssistantMessageParser(ClineTagConfigs.flatFormat());

        TaskV2MessagePresenterHandler handler =
                new TaskV2MessagePresenterHandler(
                        parser,
                        null,
                        null,
                        taskState,
                        new LinkedBlockingQueue<>(),
                        new ReentrantLock(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        () -> null,
                        () -> null,
                        (cmd, timeout) -> null,
                        null,
                        sayAskHandler,
                        messageStateHandler,
                        () -> null,
                        "task",
                        "ulid",
                        "/tmp");

        handler.startAssistantResponseStream();
        handler.updateAssistantMessageContent("Hello ");
        handler.checkAndPresentAssistantMessage(true);
        handler.updateAssistantMessageContent("World");
        handler.checkAndPresentAssistantMessage(true);

        assertEquals(1, messageStateHandler.getClineMessages().size());
        assertEquals("Hello World", messageStateHandler.getClineMessages().getFirst().getText());
        assertTrue(messageStateHandler.getClineMessages().getFirst().getPartial());
    }

    @Test
    void startAssistantResponseStreamResetsStreamingParserState() {
        TaskState taskState = new TaskState();
        MessageStateHandler messageStateHandler = new TestMessageStateHandler(taskState);
        TaskV2SayAskHandler sayAskHandler =
                new TaskV2SayAskHandler(
                        new AtomicLong(System.currentTimeMillis()),
                        messageStateHandler,
                        taskState,
                        () -> {},
                        "task",
                        new NoOpMessageSender());
        StreamingAssistantMessageParser parser =
                new DefaultStreamingAssistantMessageParser(ClineTagConfigs.flatFormat());

        TaskV2MessagePresenterHandler handler =
                new TaskV2MessagePresenterHandler(
                        parser,
                        null,
                        null,
                        taskState,
                        new LinkedBlockingQueue<>(),
                        new ReentrantLock(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        () -> null,
                        () -> null,
                        (cmd, timeout) -> null,
                        null,
                        sayAskHandler,
                        messageStateHandler,
                        () -> null,
                        "task",
                        "ulid",
                        "/tmp");

        handler.startAssistantResponseStream();
        handler.updateAssistantMessageContent("hello <rea");
        handler.checkAndPresentAssistantMessage(true);

        handler.startAssistantResponseStream();
        handler.updateAssistantMessageContent("bye");
        handler.checkAndPresentAssistantMessage(true);

        List<AssistantMessageContent> assistantMessageContent =
                taskState.getAssistantMessageContent();
        assertEquals(1, assistantMessageContent.size());
        assertInstanceOf(TextContent.class, assistantMessageContent.getFirst());
        assertEquals("bye", ((TextContent) assistantMessageContent.getFirst()).getContent());
    }

    private static final class NoOpMessageSender implements MessageSender {

        @Override
        public void send(com.hhoa.kline.core.subscription.SubscriptionMessage message) {}
    }

    private static final class TestMessageStateHandler extends MessageStateHandler {

        private TestMessageStateHandler(TaskState taskState) {
            super("task", "ulid", taskState, null, null);
        }

        @Override
        public void addToClineMessages(ClineMessage message) {
            getClineMessages().add(message);
        }

        @Override
        public void updateClineMessage(int index, ClineMessage updates) {
            getClineMessages().set(index, updates);
        }

        @Override
        public void saveClineMessagesAndUpdateHistory() {}
    }
}
