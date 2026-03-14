package com.hhoa.kline.core.core.context.management;

/** 上下文窗口工具类 用于获取和计算上下文窗口信息 */
public class ContextWindowUtils {

    /**
     * 获取给定 API 处理器的上下文窗口信息
     *
     * @param contextWindow 模型的上下文窗口大小
     * @param modelId 模型ID（用于特殊情况处理）
     * @return 包含原始上下文窗口大小和有效最大允许大小的对象
     */
    public static ContextWindowInfo getContextWindowInfo(int contextWindow, String modelId) {
        if (contextWindow <= 0) {
            contextWindow = 128_000;
        }

        // 处理特殊情况，如 DeepSeek
        // FIXME: 这是一个临时方案，用于让使用 openai 兼容的 deepseek 的用户拥有正确的上下文窗口，
        // 而不是默认的 128k。我们需要一种方法让用户为通过 openai 兼容输入的模型指定上下文窗口
        if (modelId != null && modelId.toLowerCase().contains("deepseek")) {
            contextWindow = 128_000;
        }

        int maxAllowedSize;
        switch (contextWindow) {
            case 64_000:
                maxAllowedSize = contextWindow - 27_000;
                break;
            case 128_000:
                maxAllowedSize = contextWindow - 30_000;
                break;
            case 200_000:
                maxAllowedSize = contextWindow - 40_000;
                break;
            default:
                // 对于 deepseek，64k 的 80% 意味着只有约 10k 的缓冲区，这太小了，
                // 导致用户遇到上下文窗口错误
                maxAllowedSize = Math.max(contextWindow - 40_000, (int) (contextWindow * 0.8));
        }

        return new ContextWindowInfo(contextWindow, maxAllowedSize);
    }

    /**
     * 获取上下文窗口信息（使用默认上下文窗口大小）
     *
     * @param modelId 模型ID
     * @return 上下文窗口信息
     */
    public static ContextWindowInfo getContextWindowInfo(String modelId) {
        return getContextWindowInfo(128_000, modelId);
    }

    /**
     * 上下文窗口信息
     *
     * @param contextWindow 原始上下文窗口大小
     * @param maxAllowedSize 有效的最大允许大小
     */
    public record ContextWindowInfo(int contextWindow, int maxAllowedSize) {}
}
