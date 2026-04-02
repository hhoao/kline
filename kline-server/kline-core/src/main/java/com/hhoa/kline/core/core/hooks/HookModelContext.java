package com.hhoa.kline.core.core.hooks;

import lombok.Builder;
import lombok.Data;

/**
 * Hook payload 中的模型上下文解析器。
 *
 * <p>对应 Cline TS 版本的 hook-model-context.ts。 解析当前活动的 provider/model 对，用于 hook 输入。
 */
public final class HookModelContext {

    private HookModelContext() {}

    /** 已解析的 hook 模型上下文。 */
    @Data
    @Builder
    public static class ResolvedContext {
        private final String provider;
        private final String slug;
    }

    /**
     * 解析当前活动的 provider/model 对。
     *
     * @param stateManager 状态管理器
     * @return 已解析的上下文
     */
    /**
     * 解析当前活动的 provider/model 对。 当前从 StateManager 中无法直接获取 apiProvider/apiModelId， 因此提供 fallback
     * 参数方式。未来可扩展从配置中读取。
     *
     * @param provider API 提供商名称（可为 null）
     * @param modelSlug 模型标识（可为 null）
     * @return 已解析的上下文
     */
    public static ResolvedContext resolve(String provider, String modelSlug) {
        return ResolvedContext.builder()
                .provider(provider != null ? provider : "unknown")
                .slug(modelSlug != null ? modelSlug : "unknown")
                .build();
    }

    /**
     * 将 ResolvedContext 转换为 HookInput 中的 HookModelContext。
     *
     * @param resolved 已解析的上下文
     * @return HookInput 可用的模型上下文
     */
    public static HookInput.HookModelContext toHookInputModel(ResolvedContext resolved) {
        return HookInput.HookModelContext.builder()
                .provider(resolved.getProvider())
                .slug(resolved.getSlug())
                .build();
    }
}
