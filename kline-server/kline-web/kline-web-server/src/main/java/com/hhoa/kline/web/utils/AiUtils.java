package com.hhoa.kline.web.utils;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.hhoa.kline.web.enums.AiPlatformEnum;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.ai.azure.openai.AzureOpenAiChatOptions;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.minimax.MiniMaxChatOptions;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.zhipuai.ZhiPuAiChatOptions;

/** Spring AI 工具类 */
public class AiUtils {
    public static final String TOOL_CONTEXT_LOGIN_USER_NAME = "LOGIN_USER_NAME";
    public static final String TOOL_CONTEXT_LOGIN_USER_ID = "LOGIN_USER_ID";
    public static final String TOOL_CONTEXT_DB_WORKSPACE_DIR = "dbWorkspaceDir";

    public static ChatOptions buildChatOptions(
            AiPlatformEnum platform, String model, Double temperature, Integer maxTokens) {
        return buildChatOptions(platform, model, temperature, maxTokens, null, null, null);
    }

    public static ChatOptions buildChatOptions(
            AiPlatformEnum platform,
            String model,
            Double temperature,
            Integer maxTokens,
            Set<String> toolNames,
            List<ToolCallback> toolCallbacks,
            Map<String, Object> toolContext) {
        toolNames = ObjUtil.defaultIfNull(toolNames, Collections.emptySet());
        // noinspection EnhancedSwitchMigration
        ChatOptions chatOptions;
        chatOptions =
                switch (platform) {
                    case AiPlatformEnum.ZHI_PU ->
                            ZhiPuAiChatOptions.builder()
                                    .model(model)
                                    .temperature(temperature)
                                    .maxTokens(maxTokens)
                                    .toolNames(toolNames)
                                    .toolCallbacks(toolCallbacks)
                                    .toolContext(toolContext)
                                    .build();
                    case AiPlatformEnum.MINI_MAX ->
                            MiniMaxChatOptions.builder()
                                    .model(model)
                                    .temperature(temperature)
                                    .maxTokens(maxTokens)
                                    .toolNames(toolNames)
                                    .toolCallbacks(toolCallbacks)
                                    .toolContext(toolContext)
                                    .build();
                    case AiPlatformEnum.OPENAI,
                            AiPlatformEnum.DEEP_SEEK, // 复用 OpenAI 客户端
                            AiPlatformEnum.YI_YAN,
                            AiPlatformEnum.TONG_YI,
                            AiPlatformEnum.DOU_BAO, // 复用 OpenAI 客户端
                            AiPlatformEnum.HUN_YUAN, // 复用 OpenAI 客户端
                            AiPlatformEnum.XING_HUO, // 复用 OpenAI 客户端
                            AiPlatformEnum.SILICON_FLOW, // 复用 OpenAI 客户端
                            AiPlatformEnum.BAI_CHUAN -> // 复用 OpenAI 客户端
                            OpenAiChatOptions.builder()
                                    .model(model)
                                    .temperature(temperature)
                                    .maxTokens(maxTokens)
                                    .toolNames(toolNames)
                                    .toolCallbacks(toolCallbacks)
                                    .toolContext(toolContext)
                                    .build();
                    case AiPlatformEnum.AZURE_OPENAI ->
                            AzureOpenAiChatOptions.builder()
                                    .deploymentName(model)
                                    .temperature(temperature)
                                    .maxTokens(maxTokens)
                                    .toolNames(toolNames)
                                    .toolCallbacks(toolCallbacks)
                                    .toolContext(toolContext)
                                    .build();
                    default ->
                            throw new IllegalArgumentException(
                                    StrUtil.format("未知平台({})", platform));
                };

        if (chatOptions instanceof ToolCallingChatOptions toolCallingChatOptions) {
            toolCallingChatOptions.setInternalToolExecutionEnabled(false);
        }

        return chatOptions;
    }

    public static Message buildMessage(String type, String content) {
        if (MessageType.USER.getValue().equals(type)) {
            return new UserMessage(content);
        }
        if (MessageType.ASSISTANT.getValue().equals(type)) {
            return new AssistantMessage(content);
        }
        if (MessageType.SYSTEM.getValue().equals(type)) {
            return new SystemMessage(content);
        }
        if (MessageType.TOOL.getValue().equals(type)) {
            throw new UnsupportedOperationException("暂不支持 tool 消息：" + content);
        }
        throw new IllegalArgumentException(StrUtil.format("未知消息类型({})", type));
    }

    public static Map<String, Object> buildCommonToolContext() {
        Map<String, Object> context = new HashMap<>();
        Long loginIdDefaultNull = LoginUserUtil.getLoginIdDefaultNull();
        String loginUsernameDefaultNull = LoginUserUtil.getLoginUsernameDefaultNull();
        if (ObjUtil.isNotNull(loginIdDefaultNull)) {
            context.put(TOOL_CONTEXT_LOGIN_USER_ID, loginIdDefaultNull);
        }
        if (StrUtil.isNotBlank(loginUsernameDefaultNull)) {
            context.put(TOOL_CONTEXT_LOGIN_USER_NAME, loginUsernameDefaultNull);
        }
        return context;
    }
}
