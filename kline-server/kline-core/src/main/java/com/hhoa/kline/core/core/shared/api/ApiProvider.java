package com.hhoa.kline.core.core.shared.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;

@Getter
public enum ApiProvider {
    UNKNOWN("unknown", null),
    ANTHROPIC("anthropic", "https://api.anthropic.com"),
    CLAUDE_CODE("claude-code", null),
    OPENROUTER("openrouter", "https://openrouter.ai/api/v1"),
    BEDROCK("bedrock", null),
    VERTEX("vertex", null),
    OPENAI("openai", "https://api.openai.com/v1"),
    OLLAMA("ollama", "http://localhost:11434"),
    LMSTUDIO("lmstudio", "http://localhost:1234/v1"),
    GEMINI("gemini", "https://generativelanguage.googleapis.com"),
    OPENAI_NATIVE("openai-native", "https://api.openai.com/v1"),
    REQUESTY("requesty", null),
    TOGETHER("together", "https://api.together.xyz/v1"),
    DEEPSEEK("deepseek", "https://api.deepseek.com"),
    QWEN("qwen", null),
    QWEN_CODE("qwen-code", null),
    DOUBAO("doubao", "https://ark.cn-beijing.volces.com/api"),
    MISTRAL("mistral", "https://api.mistral.ai/v1"),
    VSCODE_LM("vscode-lm", null),
    CLINE("cline", null),
    LITELLM("litellm", null),
    MOONSHOT("moonshot", null),
    NEBIUS("nebius", null),
    FIREWORKS("fireworks", "https://api.fireworks.ai/inference/v1"),
    ASKSAGE("asksage", null),
    XAI("xai", "https://api.x.ai/v1"),
    SAMBANOVA("sambanova", null),
    CEREBRAS("cerebras", null),
    SAPAICORE("sapaicore", null),
    GROQ("groq", "https://api.groq.com/openai/v1"),
    HUGGINGFACE("huggingface", "https://api-inference.huggingface.co"),
    HUAWEI_CLOUD_MAAS("huawei-cloud-maas", null),
    DIFY("dify", null),
    BASETEN("baseten", null),
    VERCEL_AI_GATEWAY("vercel-ai-gateway", null),
    ZAI("zai", null),
    OCA("oca", null),
    MINIMAX("minimax", null),
    OPENAI_CODEX("openai-codex", null),
    AIHUBMIX("aihubmix", null),
    HICAP("hicap", null),
    WANDB("wandb", null);

    private final String value;
    private final String url;

    ApiProvider(String value, String url) {
        this.value = value;
        this.url = url;
    }

    private static final Map<String, ApiProvider> BY_VALUE =
            Arrays.stream(values())
                    .collect(
                            Collectors.toMap(
                                    provider -> provider.value.toLowerCase(Locale.ROOT),
                                    Function.identity()));

    /**
     * 从字符串值获取 ApiProvider（用于 JSON 反序列化） 支持大小写不敏感
     *
     * @param value 字符串值
     * @return 对应的 ApiProvider，如果不存在则返回 null
     */
    @JsonCreator
    public static ApiProvider fromValue(String value) {
        if (value == null) {
            return null;
        }
        return BY_VALUE.get(value.toLowerCase(Locale.ROOT));
    }

    /**
     * 获取字符串值（用于 JSON 序列化）
     *
     * @return 字符串值
     */
    @JsonValue
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
