package com.hhoa.kline.core.core.prompts.systemprompt;

import com.hhoa.kline.core.core.prompts.systemprompt.registry.PromptRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 系统提示服务
 *
 * @author hhoa
 */
@RequiredArgsConstructor
@Slf4j
public class SystemPromptService {

    private final PromptRegistry promptRegistry;

    /**
     * 获取系统提示
     *
     * @param context 系统提示上下文
     * @return 系统提示
     */
    public String getSystemPrompt(SystemPromptContext context) {
        log.debug(
                "Getting system prompt for model: {}",
                context.getProviderInfo() != null
                        ? context.getProviderInfo().getModel().getId()
                        : "unknown");

        return promptRegistry.getSystemPrompt(context);
    }
}
