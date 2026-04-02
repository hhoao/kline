package com.hhoa.kline.web.core;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hhoa.kline.core.core.assistant.ImageContentBlock;
import com.hhoa.kline.core.core.assistant.MessageParam;
import com.hhoa.kline.core.core.assistant.MessageRole;
import com.hhoa.kline.core.core.assistant.TextContentBlock;
import com.hhoa.kline.core.core.assistant.ToolUseContentBlock;
import com.hhoa.kline.core.core.assistant.UserContentBlock;
import com.hhoa.kline.core.core.assistant.UserMessage;
import com.hhoa.kline.core.core.task.ApiChunk;
import com.hhoa.kline.core.core.task.ApiChunk.ChunkType;
import com.hhoa.kline.core.core.task.ApiHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

/**
 * Spring AI 实现的 ApiHandler
 *
 * <p>使用 Spring AI 的 ChatClient 进行 API 调用，支持流式响应。
 *
 * @author hhoa
 */
public class SpringAIApiHandler implements ApiHandler {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final ChatClient chatClient;
    private final String modelId;
    private final String providerId;
    private final ChatOptions chatOptions;
    private String lastGenerationId;

    /**
     * 构造函数
     *
     * @param chatClient Spring AI ChatClient 实例
     * @param modelId 模型 ID
     * @param providerId 提供者 ID（如 "openai", "anthropic" 等）
     */
    public SpringAIApiHandler(
            ChatClient chatClient, String modelId, String providerId, ChatOptions chatOptions) {
        if (chatClient == null) {
            throw new IllegalArgumentException("chatClient cannot be null");
        }
        if (modelId == null || modelId.isEmpty()) {
            throw new IllegalArgumentException("modelId cannot be null or empty");
        }
        if (providerId == null || providerId.isEmpty()) {
            throw new IllegalArgumentException("providerId cannot be null or empty");
        }
        if (chatOptions == null) {
            throw new IllegalArgumentException("chatOptions cannot be null");
        }

        this.chatClient = chatClient;
        this.modelId = modelId;
        this.providerId = providerId;
        this.chatOptions = chatOptions;
        this.lastGenerationId = null;
    }

    @Override
    public String getLastRequestId() {
        return lastGenerationId;
    }

    @Override
    public Flux<ApiChunk> createMessageStream(
            String systemPrompt, List<MessageParam> conversationHistory) {
        try {
            List<Message> chatMessages =
                    convertConversationHistory(conversationHistory, systemPrompt);

            Prompt prompt = new Prompt(chatMessages, chatOptions);

            Flux<ChatResponse> responseFlux =
                    chatClient.prompt(prompt).advisors().stream().chatResponse();

            return responseFlux.flatMap(
                    response -> {
                        List<ApiChunk> chunks = new ArrayList<>();

                        String text = response.getResult().getOutput().getText();
                        if (text != null && !text.isEmpty()) {
                            ApiChunk textChunk =
                                    new ApiChunkImpl(
                                            ChunkType.TEXT,
                                            text,
                                            null,
                                            null,
                                            null,
                                            null,
                                            null,
                                            null,
                                            null,
                                            null,
                                            null,
                                            null,
                                            null,
                                            null,
                                            null,
                                            null);
                            chunks.add(textChunk);
                        }

                        if (response.hasToolCalls()) {
                            Generation generation = response.getResult();
                            AssistantMessage assistantMessage = generation.getOutput();
                            List<AssistantMessage.ToolCall> toolCalls =
                                    assistantMessage.getToolCalls();
                            for (AssistantMessage.ToolCall toolCall : toolCalls) {
                                String arguments = toolCall.arguments();
                                Map<String, Object> toolParams;
                                try {
                                    toolParams =
                                            objectMapper.readValue(
                                                    arguments,
                                                    new TypeReference<Map<String, Object>>() {});
                                } catch (JsonProcessingException e) {
                                    continue;
                                }

                                ApiChunk toolChunk =
                                        new ApiChunkImpl(
                                                ChunkType.TOOL_USE,
                                                null,
                                                toolCall.name(),
                                                toolParams,
                                                null,
                                                null,
                                                null,
                                                null,
                                                null,
                                                null,
                                                null,
                                                null,
                                                null,
                                                null,
                                                null,
                                                null);
                                chunks.add(toolChunk);
                            }
                        }

                        ChatResponseMetadata metadata = response.getMetadata();
                        if (metadata.getUsage() != null
                                && metadata.getUsage() instanceof DefaultUsage usage) {
                            Integer inputTokens = usage.getPromptTokens();
                            Integer outputTokens = usage.getCompletionTokens();
                            Integer cacheReadTokens = null;
                            Integer cacheWriteTokens = null;
                            int cacheRead = 0;
                            int cacheWrite = 0;
                            int nonCachedInputTokens =
                                    Math.max(
                                            0,
                                            (inputTokens != null ? inputTokens : 0)
                                                    - cacheRead
                                                    - cacheWrite);
                            Integer totalCost = usage.getTotalTokens();

                            ApiChunk usageChunk =
                                    new ApiChunkImpl(
                                            ChunkType.USAGE,
                                            null,
                                            null,
                                            null,
                                            nonCachedInputTokens,
                                            outputTokens,
                                            cacheWriteTokens,
                                            cacheReadTokens,
                                            totalCost.doubleValue(),
                                            null,
                                            null,
                                            null,
                                            null,
                                            null,
                                            null,
                                            null);
                            chunks.add(usageChunk);
                        }

                        return Flux.fromIterable(chunks);
                    });
        } catch (Exception e) {
            return Flux.error(e);
        }
    }

    /**
     * 转换对话历史为 Spring AI Message
     *
     * @param conversationHistory 对话历史（List<MessageParam>）
     * @param systemPrompt 系统提示词
     * @return Spring AI Message 列表
     */
    private List<Message> convertConversationHistory(
            List<MessageParam> conversationHistory, String systemPrompt) {
        List<Message> chatMessages = new ArrayList<>();

        if (StrUtil.isNotBlank(systemPrompt)) {
            chatMessages.add(new SystemMessage(systemPrompt));
        }

        if (conversationHistory != null) {
            for (MessageParam messageParam : conversationHistory) {
                MessageRole role = messageParam.getRole();
                if (MessageRole.USER.equals(role)
                        && messageParam instanceof UserMessage userMessage) {
                    List<Message> convertedMessages = convertUserMessage(userMessage);
                    chatMessages.addAll(convertedMessages);
                } else if (MessageRole.ASSISTANT.equals(role)
                        && messageParam
                                instanceof
                                com.hhoa.kline.core.core.assistant.AssistantMessage
                                        assistantMessage) {
                    chatMessages.add(convertAssistantMessage(assistantMessage));
                }
            }
        }

        return chatMessages;
    }

    /** 转换用户消息内容为 Spring AI UserMessage */
    private List<Message> convertUserMessage(UserMessage userMessage) {
        List<Message> messages = new ArrayList<>();
        List<UserContentBlock> content = userMessage.getContent();

        if (content == null || content.isEmpty()) {
            messages.add(new org.springframework.ai.chat.messages.UserMessage(""));
            return messages;
        }

        List<UserContentBlock> nonToolMessages = new ArrayList<>();
        for (UserContentBlock block : content) {
            if (block instanceof TextContentBlock || block instanceof ImageContentBlock) {
                nonToolMessages.add(block);
            }
        }

        if (!nonToolMessages.isEmpty()) {
            StringBuilder textBuilder = new StringBuilder();
            boolean hasImages = false;
            List<String> imageDataList = new ArrayList<>();

            for (UserContentBlock block : nonToolMessages) {
                if (block instanceof TextContentBlock textBlock) {
                    String text = textBlock.getText();
                    if (text != null && !text.isEmpty()) {
                        if (!textBuilder.isEmpty()) {
                            textBuilder.append("\n");
                        }
                        textBuilder.append(text);
                    }
                } else if (block instanceof ImageContentBlock imageBlock) {
                    hasImages = true;
                    String mediaType =
                            imageBlock.getMediaType() != null
                                    ? imageBlock.getMediaType()
                                    : "image/png";
                    String source = imageBlock.getSource();
                    if (source != null) {
                        String imageUrl = "data:" + mediaType + ";base64," + source;
                        imageDataList.add(imageUrl);
                    }
                }
            }

            if (hasImages && !imageDataList.isEmpty()) {
                if (!textBuilder.isEmpty()) {
                    textBuilder.append("\n");
                }
                textBuilder.append("[Images: ").append(imageDataList.size()).append(" image(s)]");
            }

            String messageContent = textBuilder.toString();
            if (messageContent.isEmpty()) {
                messageContent = "";
            }
            messages.add(new org.springframework.ai.chat.messages.UserMessage(messageContent));
        }

        return messages;
    }

    /** 转换助手消息内容为 Spring AI AssistantMessage */
    private AssistantMessage convertAssistantMessage(
            com.hhoa.kline.core.core.assistant.AssistantMessage assistantMessage) {
        List<UserContentBlock> content = assistantMessage.getContent();

        if (content == null || content.isEmpty()) {
            return new AssistantMessage("");
        }

        List<UserContentBlock> nonToolMessages = new ArrayList<>();
        List<ToolUseContentBlock> toolMessages = new ArrayList<>();

        for (UserContentBlock block : content) {
            if (block instanceof ToolUseContentBlock toolBlock) {
                toolMessages.add(toolBlock);
            } else if (block instanceof TextContentBlock || block instanceof ImageContentBlock) {
                nonToolMessages.add(block);
            }
        }

        String messageContent = null;
        List<Object> reasoningDetails = new ArrayList<>();

        if (!nonToolMessages.isEmpty()) {
            StringBuilder textBuilder = new StringBuilder();
            for (UserContentBlock block : nonToolMessages) {
                if (block instanceof TextContentBlock textBlock) {
                    List<Object> blockReasoningDetails = textBlock.getReasoningDetails();
                    if (blockReasoningDetails != null && !blockReasoningDetails.isEmpty()) {
                        reasoningDetails.addAll(blockReasoningDetails);
                    }

                    String text = textBlock.getText();
                    if (text != null && !text.isEmpty()) {
                        if (!textBuilder.isEmpty()) {
                            textBuilder.append("\n");
                        }
                        textBuilder.append(text);
                    }
                }
            }
            String contentText = textBuilder.toString();
            messageContent = contentText.isEmpty() ? null : contentText;
        }

        List<AssistantMessage.ToolCall> toolCalls = new ArrayList<>();
        for (ToolUseContentBlock toolBlock : toolMessages) {
            try {
                String arguments = objectMapper.writeValueAsString(toolBlock.getInput());

                AssistantMessage.ToolCall toolCall =
                        new AssistantMessage.ToolCall(
                                toolBlock.getId() != null
                                        ? toolBlock.getId()
                                        : "tool_" + System.currentTimeMillis(),
                                "function",
                                toolBlock.getName(),
                                arguments);
                toolCalls.add(toolCall);
            } catch (JsonProcessingException e) {
            }
        }

        if (messageContent == null && toolCalls.isEmpty()) {
            return new AssistantMessage("");
        }

        List<AssistantMessage.ToolCall> finalToolCalls =
                toolCalls.isEmpty() ? new ArrayList<>() : toolCalls;

        return AssistantMessage.builder()
                .content(messageContent != null ? messageContent : "")
                .toolCalls(finalToolCalls)
                .build();
    }

    @Override
    public String getModelId() {
        return modelId;
    }

    @Override
    public String getProviderId() {
        return providerId;
    }

    private record ApiChunkImpl(
            ChunkType type,
            String text,
            String toolName,
            Map<String, Object> toolParams,
            Integer inputTokens,
            Integer outputTokens,
            Integer cacheWriteTokens,
            Integer cacheReadTokens,
            Double totalCost,
            String reasoning,
            Object reasoningDetails,
            String thinking,
            String signature,
            String data,
            String toolId,
            String callId)
            implements ApiChunk {}
}
