package com.hhoa.kline.core.core.prompts.systemprompt.variants;

import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.prompts.systemprompt.PromptVariant;
import com.hhoa.kline.core.core.prompts.systemprompt.variants.generic.GenericVariantConfig;
import com.hhoa.kline.core.core.prompts.systemprompt.variants.glm.GlmVariantConfig;
import com.hhoa.kline.core.core.prompts.systemprompt.variants.gpt5.Gpt5VariantConfig;
import com.hhoa.kline.core.core.prompts.systemprompt.variants.nextgen.NextGenVariantConfig;
import com.hhoa.kline.core.core.prompts.systemprompt.variants.xs.XsVariantConfig;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

/**
 * Variant Registry - Central hub for all prompt variants
 *
 * <p>This class exports all variant configurations and provides a registry for dynamic loading.
 * Each variant is optimized for specific model families and use cases.
 *
 * @author hhoa
 */
@Slf4j
public class VariantIndex {

    private final Map<ModelFamily, PromptVariant> variantConfigs;

    public VariantIndex() {
        this.variantConfigs = new HashMap<>();
        initializeVariants();
    }

    /** Initialize all available variants */
    private void initializeVariants() {
        try {
            // Generic variant - Fallback for all model types
            // Optimized for broad compatibility and stable performance
            variantConfigs.put(ModelFamily.GENERIC, GenericVariantConfig.getConfig());

            variantConfigs.put(ModelFamily.GLM, GlmVariantConfig.createGlmVariant());

            variantConfigs.put(ModelFamily.NEXT_GEN, NextGenVariantConfig.createNextGenVariant());

            variantConfigs.put(ModelFamily.GPT_5, Gpt5VariantConfig.createGpt5Variant());

            variantConfigs.put(ModelFamily.XS, XsVariantConfig.createXsVariant());

            log.info("Initialized {} prompt variants", variantConfigs.size());
        } catch (Exception e) {
            log.error("Failed to initialize prompt variants", e);
            throw new RuntimeException("Failed to initialize prompt variants", e);
        }
    }

    /** Get variant configuration by model family */
    public Optional<PromptVariant> getVariant(ModelFamily family) {
        return Optional.ofNullable(variantConfigs.get(family));
    }

    /** Get variant configuration by model family, with fallback to generic */
    public PromptVariant getVariantOrDefault(ModelFamily family) {
        return variantConfigs.getOrDefault(family, variantConfigs.get(ModelFamily.GENERIC));
    }

    /** Get all available variant configurations */
    public Map<ModelFamily, PromptVariant> getAllVariants() {
        return new HashMap<>(variantConfigs);
    }

    /** Get all available model families */
    public List<ModelFamily> getAvailableModelFamilies() {
        return List.copyOf(variantConfigs.keySet());
    }

    /** Check if a variant exists for the given model family */
    public boolean hasVariant(ModelFamily family) {
        return variantConfigs.containsKey(family);
    }

    /** Register a new variant */
    public void registerVariant(ModelFamily family, PromptVariant variant) {
        variantConfigs.put(family, variant);
        log.info("Registered variant for model family: {}", family);
    }

    /** Reload all variants */
    public void reload() {
        variantConfigs.clear();
        initializeVariants();
        log.info("Reloaded all prompt variants");
    }

    /** Get available variant IDs */
    public List<String> getAvailableVariants() {
        return variantConfigs.keySet().stream().map(Enum::name).toList();
    }

    /** Check if a variant ID is valid */
    public boolean isValidVariantId(String id) {
        try {
            ModelFamily family = ModelFamily.valueOf(id);
            return variantConfigs.containsKey(family);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /** Load a variant configuration dynamically */
    public PromptVariant loadVariantConfig(String variantId) {
        try {
            ModelFamily family = ModelFamily.valueOf(variantId);
            return variantConfigs.get(family);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid variant ID: " + variantId, e);
        }
    }

    /** Load all variant configurations */
    public Map<ModelFamily, PromptVariant> loadAllVariantConfigs() {
        return new HashMap<>(variantConfigs);
    }
}
