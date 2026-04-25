package com.hhoa.kline.langchain4j;

import static org.assertj.core.api.Assertions.assertThat;

import com.hhoa.kline.core.core.assistant.AssistantMessage;
import com.hhoa.kline.core.core.assistant.MessageParam;
import com.hhoa.kline.core.core.assistant.TextContentBlock;
import com.hhoa.kline.core.core.assistant.UserMessage;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import java.util.List;
import org.junit.jupiter.api.Test;

class LangChain4jMessageConverterTest {
    @Test
    void convertsSystemUserAndAssistantText() {
        List<MessageParam> history =
                List.of(
                        UserMessage.builder()
                                .content(List.of(new TextContentBlock("hello")))
                                .build(),
                        AssistantMessage.builder()
                                .content(List.of(new TextContentBlock("hi")))
                                .build());

        List<ChatMessage> messages = LangChain4jMessageConverter.convert("system prompt", history);

        assertThat(messages).hasSize(3);
        assertThat((SystemMessage) messages.get(0))
                .extracting(SystemMessage::text)
                .isEqualTo("system prompt");
        assertThat((dev.langchain4j.data.message.UserMessage) messages.get(1))
                .extracting(dev.langchain4j.data.message.UserMessage::singleText)
                .isEqualTo("hello");
        assertThat((AiMessage) messages.get(2)).extracting(AiMessage::text).isEqualTo("hi");
    }

    @Test
    void joinsMultipleTextBlocksWithNewlines() {
        List<MessageParam> history =
                List.of(
                        UserMessage.builder()
                                .content(
                                        List.of(
                                                new TextContentBlock("line one"),
                                                new TextContentBlock("line two")))
                                .build());

        List<ChatMessage> messages = LangChain4jMessageConverter.convert("", history);

        assertThat(messages).hasSize(1);
        assertThat((dev.langchain4j.data.message.UserMessage) messages.get(0))
                .extracting(dev.langchain4j.data.message.UserMessage::singleText)
                .isEqualTo("line one\nline two");
    }
}
