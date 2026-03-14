package com.hhoa.kline.web.enums;

import com.hhoa.ai.kline.commons.core.ArrayValuable;
import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * AI 模型平台
 *
 * @author hhoa
 */
@Getter
@AllArgsConstructor
public enum AiPlatformEnum implements ArrayValuable<String> {

    // ========== 国内平台 ==========

    TONG_YI("TongYi", "通义千问", null, null, null, null), // 阿里
    YI_YAN("YiYan", "文心一言", null, null, null, null), // 百度
    DEEP_SEEK(
            "DeepSeek",
            "DeepSeek",
            "https://api.deepseek.com",
            "deepseek-chat",
            null,
            null), // DeepSeek
    ZHI_PU("ZhiPu", "智谱", null, null, null, null), // 智谱 AI
    XING_HUO("XingHuo", "星火", "https://spark-api-open.xf-yun.com", "generalv3.5", null, null), // 讯飞
    DOU_BAO(
            "DouBao",
            "豆包",
            "https://ark.cn-beijing.volces.com/api",
            "doubao-1-5-lite-32k-250115",
            null,
            null), // 字节
    HUN_YUAN(
            "HunYuan",
            "混元",
            "https://api.hunyuan.cloud.tencent.com",
            "hunyuan-turbo",
            null,
            null), // 腾讯
    SILICON_FLOW(
            "SiliconFlow",
            "硅基流动",
            "https://api.siliconflow.cn",
            "deepseek-ai/DeepSeek-R1-Distill-Qwen-7B",
            "deepseek-ai/DeepSeek-R1-Distill-Qwen-7B",
            "Kwai-Kolors/Kolors"), // 硅基流动
    MINI_MAX("MiniMax", "MiniMax", null, null, null, null), // 稀宇科技
    MOONSHOT("Moonshot", "月之暗灭", null, null, null, null), // KIMI
    BAI_CHUAN(
            "BaiChuan",
            "百川智能",
            "https://api.baichuan-ai.com",
            "Baichuan4-Turbo",
            null,
            null), // 百川智能

    // ========== 国外平台 ==========

    OPENAI("OpenAI", "OpenAI", null, null, null, null), // OpenAI 官方
    AZURE_OPENAI("AzureOpenAI", "AzureOpenAI", null, null, null, null), // OpenAI 微软
    OLLAMA("Ollama", "Ollama", null, null, null, null), // Ollama

    STABLE_DIFFUSION("StableDiffusion", "StableDiffusion", null, null, null, null), // Stability AI
    MIDJOURNEY("Midjourney", "Midjourney", null, null, null, null), // Midjourney
    SUNO("Suno", "Suno", null, null, null, null), // Suno AI
    ;

    /** 平台 */
    private final String platform;

    /** 平台名 */
    private final String name;

    private final String url;

    private final String defaultChatModel;

    private final String defaultEmbedModel;

    private final String defaultImageModel;

    public static final String[] ARRAYS =
            Arrays.stream(values()).map(AiPlatformEnum::getPlatform).toArray(String[]::new);

    public static AiPlatformEnum validatePlatform(String platform) {
        for (AiPlatformEnum platformEnum : AiPlatformEnum.values()) {
            if (platformEnum.getPlatform().equals(platform)) {
                return platformEnum;
            }
        }
        throw new IllegalArgumentException("非法平台： " + platform);
    }

    @Override
    public String[] array() {
        return ARRAYS;
    }
}
