package com.hhoa.kline.core.core.prompts.systemprompt;

/**
 * 系统提示组件接口
 *
 * @author hhoa
 */
public interface SystemPromptComponent {

    /**
     * 应用组件
     *
     * @param variant 提示变体
     * @param context 系统提示上下文
     * @return 组件内容
     */
    String apply(PromptVariant variant, SystemPromptContext context);

    /**
     * 获取组件对应的系统提示部分
     *
     * @return 系统提示部分
     */
    SystemPromptSection getSystemPromptSection();
}
