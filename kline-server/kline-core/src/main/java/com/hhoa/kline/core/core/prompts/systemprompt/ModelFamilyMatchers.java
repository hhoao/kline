package com.hhoa.kline.core.core.prompts.systemprompt;

import java.util.Set;

/**
 * 模型家族匹配工具类，对应 TS model-utils.ts 中的检测函数。
 *
 * @author hhoa
 */
public final class ModelFamilyMatchers {

    private static final Set<String> NEXT_GEN_PROVIDERS = Set.of(
            "cline", "anthropic", "bedrock", "gemini", "vertex",
            "openrouter", "openai", "minimax", "openai-native",
            "openai-compatible", "openai-codex", "baseten",
            "vercel-ai-gateway", "deepseek", "oca");

    private static final Set<String> LOCAL_PROVIDERS = Set.of("lmstudio", "ollama");

    private ModelFamilyMatchers() {}

    public static boolean isNextGenModelProvider(SystemPromptContext.ApiProviderInfo providerInfo) {
        if (providerInfo == null || providerInfo.getProviderId() == null) {
            return false;
        }
        return NEXT_GEN_PROVIDERS.contains(normalize(providerInfo.getProviderId()));
    }

    public static boolean isLocalModel(SystemPromptContext.ApiProviderInfo providerInfo) {
        if (providerInfo == null || providerInfo.getProviderId() == null) {
            return false;
        }
        return LOCAL_PROVIDERS.contains(normalize(providerInfo.getProviderId()));
    }

    public static boolean isGPT5ModelFamily(String id) {
        if (id == null) return false;
        String modelId = normalize(id);
        return modelId.contains("gpt-5") || modelId.contains("gpt5");
    }

    public static boolean isGptOssModelFamily(String id) {
        if (id == null) return false;
        String modelId = normalize(id);
        return modelId.contains("gpt-oss") || modelId.contains("gpt_oss");
    }

    /**
     * 对应 TS utils/model-utils.ts isNativeToolCallingConfig().
     *
     * @param providerInfo 提供商与模型信息
     * @param enableNativeToolCalls 是否已启用 native tool calling
     * @return 当前 provider/model 组合是否会实际使用 native tool calling
     */
    public static boolean isNativeToolCallingConfig(
            SystemPromptContext.ApiProviderInfo providerInfo, boolean enableNativeToolCalls) {
        if (!enableNativeToolCalls || providerInfo == null || providerInfo.getModel() == null) {
            return false;
        }
        if (!isNextGenModelProvider(providerInfo)) {
            return false;
        }
        return isNextGenModelFamily(providerInfo.getModel().getId());
    }

    /**
     * 对应 TS utils/model-utils.ts isParallelToolCallingEnabled()。
     *
     * @param enableParallelSetting 是否显式启用并行工具调用
     * @param providerInfo 提供商与模型信息
     * @return 是否应开启并行工具调用
     */
    public static boolean isParallelToolCallingEnabled(
            boolean enableParallelSetting, SystemPromptContext.ApiProviderInfo providerInfo) {
        if (enableParallelSetting) {
            return true;
        }
        if (providerInfo == null || providerInfo.getProviderId() == null) {
            return false;
        }
        return isNativeToolCallingConfig(providerInfo, true)
                || (providerInfo.getModel() != null
                        && isGPT5ModelFamily(providerInfo.getModel().getId()));
    }

    public static boolean isGPT51Model(String id) {
        if (id == null) return false;
        String modelId = normalize(id);
        return modelId.contains("gpt-5.1") || modelId.contains("gpt-5-1");
    }

    public static boolean isGPT52Model(String id) {
        if (id == null) return false;
        String modelId = normalize(id);
        return modelId.contains("gpt-5.2") || modelId.contains("gpt-5-2");
    }

    public static boolean isGLMModelFamily(String id) {
        if (id == null) return false;
        String modelId = normalize(id);
        return modelId.contains("glm-5")
                || modelId.contains("glm-4.7")
                || modelId.contains("glm-4.6")
                || modelId.contains("glm-4.5")
                || modelId.contains("glm 4.")
                || modelId.contains("z-ai/glm")
                || modelId.contains("zai-org/glm");
    }

    public static boolean isHermesModelFamily(String id) {
        if (id == null) return false;
        String modelId = normalize(id);
        return modelId.contains("hermes-4")
                || modelId.contains("hermes4")
                || modelId.contains("nous/hermes-4")
                || modelId.contains("nous/hermes4")
                || modelId.contains("nous-hermes-4")
                || modelId.contains("nousresearch/hermes-4")
                || modelId.contains("nousresearch/hermes4");
    }

    public static boolean isDevstralModelFamily(String id) {
        if (id == null) return false;
        return normalize(id).contains("devstral");
    }

    public static boolean isTrinityModelFamily(String id) {
        if (id == null) return false;
        String modelId = normalize(id);
        return modelId.contains("arcee-ai/trinity") || modelId.contains("trinity");
    }

    public static boolean isGemini3ModelFamily(String id) {
        if (id == null) return false;
        String modelId = normalize(id);
        return modelId.contains("gemini3") || modelId.contains("gemini-3");
    }

    public static boolean isGemini2dot5ModelFamily(String id) {
        if (id == null) return false;
        return normalize(id).contains("gemini-2.5");
    }

    public static boolean isGrok4ModelFamily(String id) {
        if (id == null) return false;
        return normalize(id).contains("grok-4");
    }

    public static boolean isMinimaxModelFamily(String id) {
        if (id == null) return false;
        return normalize(id).contains("minimax");
    }

    public static boolean isClaude4PlusModelFamily(String id) {
        if (id == null) return false;
        String modelId = normalize(id);
        if ("sonnet".equals(modelId) || "opus".equals(modelId)) {
            return true;
        }
        // Check if it contains Claude model markers
        boolean isClaude = modelId.contains("sonnet")
                || modelId.contains("opus")
                || modelId.contains("haiku");
        if (!isClaude) {
            return false;
        }
        // Extract version number
        java.util.regex.Matcher matcher =
                java.util.regex.Pattern.compile("[-_ ]([\\d](?:\\.[05])?)[-_ ]?").matcher(modelId);
        if (!matcher.find()) {
            return false;
        }
        double version = Double.parseDouble(matcher.group(1));
        return version >= 4;
    }

    public static boolean isNextGenOpenSourceModelFamily(String id) {
        if (id == null) return false;
        return normalize(id).contains("kimi-k2");
    }

    public static boolean isDeepSeekNativeModelFamily(String id) {
        if (id == null) return false;
        String modelId = normalize(id);
        return modelId.contains("deepseek-chat") || modelId.contains("deepseek-reasoner");
    }

    public static boolean isNextGenModelFamily(String id) {
        if (id == null) return false;
        String modelId = normalize(id);
        return isClaude4PlusModelFamily(modelId)
                || isGemini2dot5ModelFamily(modelId)
                || isGrok4ModelFamily(modelId)
                || isGPT5ModelFamily(modelId)
                || isGptOssModelFamily(modelId)
                || isMinimaxModelFamily(modelId)
                || isGemini3ModelFamily(modelId)
                || isNextGenOpenSourceModelFamily(modelId)
                || isDeepSeekNativeModelFamily(modelId);
    }

    public static boolean isAnthropicModelId(String id) {
        if (id == null) return false;
        String modelId = normalize(id);
        return modelId.contains("sonnet")
                || modelId.contains("opus")
                || modelId.contains("haiku");
    }

    /** 获取模型 ID（安全处理 null） */
    public static String getModelId(SystemPromptContext context) {
        if (context == null
                || context.getProviderInfo() == null
                || context.getProviderInfo().getModel() == null
                || context.getProviderInfo().getModel().getId() == null) {
            return "";
        }
        return context.getProviderInfo().getModel().getId();
    }

    private static String normalize(String text) {
        return text == null ? "" : text.trim().toLowerCase();
    }
}
