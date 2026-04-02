package com.hhoa.kline.core.core.prompts.commands.deepplanning;

import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptContext;
import java.util.function.Function;
import lombok.Builder;
import lombok.Data;

/**
 * Configuration for a deep-planning prompt variant.
 * 对应 TS deep-planning/types.ts DeepPlanningVariant
 *
 * @author hhoa
 */
@Data
@Builder
public class DeepPlanningVariant {

    /** Unique identifier for this variant (e.g., "anthropic", "gemini", "gpt-5", "generic") */
    private String id;

    /** Human-readable description of this variant */
    private String description;

    /** The model family this variant is designed for */
    private String family;

    /** Version number for this variant */
    private int version;

    /** Matcher function to determine if this variant should be used */
    private Function<SystemPromptContext, Boolean> matcher;

    /** The complete prompt template string */
    private String template;
}
