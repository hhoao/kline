package com.hhoa.kline.core.core.prompts.systemprompt.registry;

import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptComponent;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptSection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

/**
 * 组件注册表
 *
 * @author hhoa
 */
@Slf4j
public class ComponentRegistry {
    private final Map<SystemPromptSection, SystemPromptComponent> components = new HashMap<>();

    public ComponentRegistry() {}

    /**
     * 注册组件
     *
     * @param section 系统提示部分
     * @param component 组件
     */
    public void register(SystemPromptSection section, SystemPromptComponent component) {
        components.put(section, component);
        log.debug("Registered component for section: {}", section);
    }

    /**
     * 获取组件
     *
     * @param section 系统提示部分
     * @return 组件
     */
    public Optional<SystemPromptComponent> get(SystemPromptSection section) {
        return Optional.ofNullable(components.get(section));
    }

    /**
     * 获取组件数量
     *
     * @return 组件数量
     */
    public int size() {
        return components.size();
    }
}
