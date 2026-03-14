package com.hhoa.kline.core.core.prompts.systemprompt;

import java.util.function.Function;
import lombok.Data;

/**
 * 配置覆盖
 *
 * @author hhoa
 */
@Data
public class ConfigOverride {
    private String template;

    private Function<SystemPromptContext, String> templateFunction;

    private Boolean enabled;

    private Integer order;

    public static ConfigOverride create() {
        return new ConfigOverride();
    }

    public ConfigOverride template(String template) {
        this.template = template;
        return this;
    }

    public ConfigOverride templateFunction(Function<SystemPromptContext, String> templateFunction) {
        this.templateFunction = templateFunction;
        return this;
    }

    public ConfigOverride enabled(Boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public ConfigOverride order(Integer order) {
        this.order = order;
        return this;
    }
}
