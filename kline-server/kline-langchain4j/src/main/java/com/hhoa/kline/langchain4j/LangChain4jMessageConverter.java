package com.hhoa.kline.langchain4j;

import com.hhoa.kline.core.core.assistant.MessageParam;
import com.hhoa.kline.core.core.assistant.MessageRole;
import com.hhoa.kline.core.core.assistant.TextContentBlock;
import com.hhoa.kline.core.core.assistant.UserContentBlock;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import java.util.ArrayList;
import java.util.List;

final class LangChain4jMessageConverter {
    private LangChain4jMessageConverter() {}

    static List<ChatMessage> convert(String systemPrompt, List<MessageParam> conversationHistory) {
        List<ChatMessage> messages = new ArrayList<>();

        if (!isBlank(systemPrompt)) {
            messages.add(SystemMessage.from(systemPrompt.trim()));
        }

        if (conversationHistory == null) {
            return messages;
        }

        for (MessageParam messageParam : conversationHistory) {
            String text = textContent(messageParam.getContent());
            if (text.isEmpty()) {
                continue;
            }

            if (MessageRole.USER.equals(messageParam.getRole())) {
                messages.add(UserMessage.from(text));
            } else if (MessageRole.ASSISTANT.equals(messageParam.getRole())) {
                messages.add(AiMessage.from(text));
            }
        }

        return messages;
    }

    private static String textContent(List<UserContentBlock> blocks) {
        if (blocks == null || blocks.isEmpty()) {
            return "";
        }

        List<String> textParts = new ArrayList<>();
        for (UserContentBlock block : blocks) {
            if (block instanceof TextContentBlock textBlock && !isBlank(textBlock.getText())) {
                textParts.add(textBlock.getText().trim());
            }
        }

        return String.join("\n", textParts);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
