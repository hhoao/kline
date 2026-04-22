package com.hhoa.kline.core.core.prompts.commands.deepplanning;

import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptContext;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * Singleton registry for managing deep-planning prompt variants. Selects appropriate variant based
 * on model family detection. 对应 TS deep-planning/registry.ts
 *
 * @author hhoa
 */
@Slf4j
public class DeepPlanningRegistry {

    private static volatile DeepPlanningRegistry instance;

    private final Map<String, DeepPlanningVariant> variants = new LinkedHashMap<>();
    private final DeepPlanningVariant genericVariant;

    private DeepPlanningRegistry() {
        registerVariant(AnthropicDeepPlanningVariant.create());
        registerVariant(GeminiDeepPlanningVariant.create());
        registerVariant(Gemini3DeepPlanningVariant.create());
        registerVariant(Gpt51DeepPlanningVariant.create());

        DeepPlanningVariant generic = GenericDeepPlanningVariant.create();
        registerVariant(generic);
        this.genericVariant = generic;
    }

    public static DeepPlanningRegistry getInstance() {
        if (instance == null) {
            synchronized (DeepPlanningRegistry.class) {
                if (instance == null) {
                    instance = new DeepPlanningRegistry();
                }
            }
        }
        return instance;
    }

    public void register(DeepPlanningVariant variant) {
        registerVariant(variant);
    }

    private void registerVariant(DeepPlanningVariant variant) {
        variants.put(variant.getId(), variant);
    }

    /**
     * Get the appropriate variant based on the system prompt context. Uses matcher functions to
     * determine which variant to use. Falls back to generic variant if no match or on error.
     */
    public DeepPlanningVariant get(SystemPromptContext context) {
        try {
            for (DeepPlanningVariant variant : variants.values()) {
                if ("generic".equals(variant.getId())) {
                    continue;
                }
                if (variant.getMatcher() != null
                        && Boolean.TRUE.equals(variant.getMatcher().apply(context))) {
                    return variant;
                }
            }
            return genericVariant;
        } catch (Exception e) {
            log.warn(
                    "Error selecting deep-planning variant, falling back to generic: {}",
                    e.getMessage());
            return genericVariant;
        }
    }

    public List<DeepPlanningVariant> getAll() {
        return new ArrayList<>(variants.values());
    }
}
