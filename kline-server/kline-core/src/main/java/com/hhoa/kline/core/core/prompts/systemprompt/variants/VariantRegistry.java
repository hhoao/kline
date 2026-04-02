package com.hhoa.kline.core.core.prompts.systemprompt.variants;

import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.prompts.systemprompt.PromptVariant;
import com.hhoa.kline.core.core.prompts.systemprompt.variants.devstral.DevstralVariantConfig;
import com.hhoa.kline.core.core.prompts.systemprompt.variants.gemini3.Gemini3VariantConfig;
import com.hhoa.kline.core.core.prompts.systemprompt.variants.generic.GenericVariantConfig;
import com.hhoa.kline.core.core.prompts.systemprompt.variants.glm.GlmVariantConfig;
import com.hhoa.kline.core.core.prompts.systemprompt.variants.gpt5.Gpt5VariantConfig;
import com.hhoa.kline.core.core.prompts.systemprompt.variants.hermes.HermesVariantConfig;
import com.hhoa.kline.core.core.prompts.systemprompt.variants.nativegpt5.NativeGpt5VariantConfig;
import com.hhoa.kline.core.core.prompts.systemprompt.variants.nativegpt51.NativeGpt51VariantConfig;
import com.hhoa.kline.core.core.prompts.systemprompt.variants.nativenextgen.NativeNextGenVariantConfig;
import com.hhoa.kline.core.core.prompts.systemprompt.variants.nextgen.NextGenVariantConfig;
import com.hhoa.kline.core.core.prompts.systemprompt.variants.trinity.TrinityVariantConfig;
import com.hhoa.kline.core.core.prompts.systemprompt.variants.xs.XsVariantConfig;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

/**
 * Variant Registry - Central hub for all prompt variants
 *
 * <p>This class provides a registry for dynamic loading of variant configurations. Each variant is
 * optimized for specific model families and use cases.
 *
 * @author hhoa
 */
@Slf4j
public class VariantRegistry {

    private final Map<ModelFamily, PromptVariant> variantConfigs;

    public VariantRegistry() {
        this.variantConfigs = new HashMap<>();
        initializeVariants();
    }

    private void initializeVariants() {
        try {
            variantConfigs.put(ModelFamily.GENERIC, GenericVariantConfig.getConfig());
            variantConfigs.put(ModelFamily.GLM, GlmVariantConfig.getConfig());
            variantConfigs.put(ModelFamily.NEXT_GEN, NextGenVariantConfig.getConfig());
            variantConfigs.put(ModelFamily.GPT_5, Gpt5VariantConfig.getConfig());
            variantConfigs.put(ModelFamily.XS, XsVariantConfig.getConfig());
            variantConfigs.put(ModelFamily.NATIVE_GPT_5, NativeGpt5VariantConfig.getConfig());
            variantConfigs.put(ModelFamily.NATIVE_GPT_5_1, NativeGpt51VariantConfig.getConfig());
            variantConfigs.put(ModelFamily.NATIVE_NEXT_GEN, NativeNextGenVariantConfig.getConfig());
            variantConfigs.put(ModelFamily.DEVSTRAL, DevstralVariantConfig.getConfig());
            variantConfigs.put(ModelFamily.GEMINI_3, Gemini3VariantConfig.getConfig());
            variantConfigs.put(ModelFamily.HERMES, HermesVariantConfig.getConfig());
            variantConfigs.put(ModelFamily.TRINITY, TrinityVariantConfig.getConfig());

            log.info("Initialized {} prompt variants", variantConfigs.size());
        } catch (Exception e) {
            log.error("Failed to initialize prompt variants", e);
            throw new RuntimeException("Failed to initialize prompt variants", e);
        }
    }

    public Optional<PromptVariant> getVariant(ModelFamily family) {
        return Optional.ofNullable(variantConfigs.get(family));
    }

    public PromptVariant getVariantOrDefault(ModelFamily family) {
        return variantConfigs.getOrDefault(family, variantConfigs.get(ModelFamily.GENERIC));
    }

    public Map<ModelFamily, PromptVariant> getAllVariants() {
        return new HashMap<>(variantConfigs);
    }

    public List<ModelFamily> getAvailableModelFamilies() {
        return List.copyOf(variantConfigs.keySet());
    }

    public boolean hasVariant(ModelFamily family) {
        return variantConfigs.containsKey(family);
    }

    public void registerVariant(ModelFamily family, PromptVariant variant) {
        variantConfigs.put(family, variant);
        log.info("Registered variant for model family: {}", family);
    }

    public void reload() {
        variantConfigs.clear();
        initializeVariants();
        log.info("Reloaded all prompt variants");
    }
}
