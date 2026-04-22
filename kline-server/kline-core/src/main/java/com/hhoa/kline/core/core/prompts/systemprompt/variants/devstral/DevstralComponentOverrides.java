package com.hhoa.kline.core.core.prompts.systemprompt.variants.devstral;

import com.hhoa.kline.core.core.prompts.systemprompt.ConfigOverride;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptSection;
import java.util.Map;

/**
 * Devstral-specific component override templates. Provides optimized prompt text for Devstral model
 * family.
 *
 * @author hhoa
 */
public final class DevstralComponentOverrides {

    private DevstralComponentOverrides() {}

    private static final String DEVSTRAL_AGENT_ROLE_TEMPLATE =
            """
            You are Cline, a highly skilled software engineer with extensive knowledge in many programming languages, frameworks, design patterns, and best practices.
            """;

    public static Map<SystemPromptSection, ConfigOverride> getOverrides() {
        return Map.of(
                SystemPromptSection.AGENT_ROLE,
                ConfigOverride.create().template(DEVSTRAL_AGENT_ROLE_TEMPLATE));
    }
}
