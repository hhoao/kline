package com.hhoa.kline.core.core.prompts.systemprompt.registry;

import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.prompts.systemprompt.PromptVariant;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptContext;
import com.hhoa.kline.core.core.prompts.systemprompt.variants.VariantRegistry;
import com.hhoa.kline.core.core.prompts.systemprompt.variants.generic.GenericVariantConfig;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 提示注册表
 *
 * @author hhoa
 */
@RequiredArgsConstructor
@Slf4j
public class PromptRegistry {
    private final PromptBuilder promptBuilder;

    private final VariantRegistry variantRegistry;
    private final Map<String, PromptVariant> variants = new HashMap<>();
    private boolean loaded = false;

    public synchronized void load() {
        if (loaded) {
            return;
        }

        log.info("Loading prompt variants and components...");

        loadVariants();

        performHealthCheck();

        loaded = true;
    }

    public String getSystemPrompt(SystemPromptContext context) {
        load();

        ModelFamily modelFamily = detectModelFamily(context);

        PromptVariant variant = findVariant(modelFamily);

        if (variant == null) {
            throw new RuntimeException("No prompt variant found for model family: " + modelFamily);
        }

        log.debug(
                "Using prompt variant: {} for model: {}",
                variant.getId(),
                context.getProviderInfo().getModel().getId());

        return promptBuilder.build(variant, context);
    }

    public String getVersion(
            String modelId,
            int version,
            SystemPromptContext context,
            boolean isNextGenModelFamily) {
        load();

        if (isNextGenModelFamily) {
            PromptVariant nextGen = variants.get(ModelFamily.NEXT_GEN.getValue());
            if (nextGen != null
                    && nextGen.getVersion() != null
                    && nextGen.getVersion() == version) {
                return promptBuilder.build(nextGen, context);
            }
        }

        String versionKey = modelId + "@" + version;
        PromptVariant variant = variants.get(versionKey);

        if (variant == null) {
            for (Map.Entry<String, PromptVariant> entry : variants.entrySet()) {
                String key = entry.getKey();
                PromptVariant v = entry.getValue();
                if (key != null
                        && key.startsWith(modelId)
                        && v.getVersion() != null
                        && v.getVersion() == version) {
                    variant = v;
                    break;
                }
            }
        }

        if (variant == null) {
            throw new RuntimeException(
                    "No prompt variant found for model '" + modelId + "' version " + version);
        }

        return promptBuilder.build(variant, context);
    }

    public String getByTag(
            String modelId,
            String tag,
            String label,
            SystemPromptContext context,
            boolean isNextGenModelFamily) {
        load();

        if (context == null) {
            throw new IllegalArgumentException("Context is required for prompt building");
        }

        PromptVariant matched = null;

        if (isNextGenModelFamily) {
            PromptVariant nextGen = variants.get(ModelFamily.NEXT_GEN.getValue());
            if (nextGen != null) {
                boolean matchesLabel =
                        label != null
                                && nextGen.getLabels() != null
                                && nextGen.getLabels().get(label) != null;
                boolean matchesTag =
                        tag != null && nextGen.getTags() != null && nextGen.getTags().contains(tag);
                if (matchesLabel || matchesTag) {
                    matched = nextGen;
                }
            }
        }

        if (matched == null && label != null) {
            for (PromptVariant v : variants.values()) {
                if (modelId.equals(v.getId())
                        && v.getLabels() != null
                        && v.getLabels().get(label) != null) {
                    matched = v;
                    break;
                }
            }
        }

        if (matched == null && tag != null) {
            for (PromptVariant v : variants.values()) {
                if (modelId.equals(v.getId()) && v.getTags() != null && v.getTags().contains(tag)) {
                    matched = v;
                    break;
                }
            }
        }

        if (matched == null) {
            throw new RuntimeException(
                    "No prompt variant found for model '"
                            + modelId
                            + "' with tag '"
                            + tag
                            + "' or label '"
                            + label
                            + "'");
        }

        return promptBuilder.build(matched, context);
    }

    public void registerVariant(String id, PromptVariant variant) {
        variants.put(id, variant);
        if (variant.getVersion() != null && variant.getVersion() > 1) {
            variants.put(id + "@" + variant.getVersion(), variant);
        }
        log.debug("Registered variant: {}", id);
    }

    public Optional<PromptVariant> getVariant(String id) {
        return Optional.ofNullable(variants.get(id));
    }

    /**
     * 根据上下文获取匹配的变体。
     * 对应 TS PromptRegistry.getVariant(context)
     */
    public PromptVariant getVariant(SystemPromptContext context) {
        load();
        ModelFamily modelFamily = detectModelFamily(context);
        return findVariant(modelFamily);
    }

    public Map<String, PromptVariant> getAllVariants() {
        return new HashMap<>(variants);
    }

    public List<String> getAvailableModels() {
        Set<String> models = new HashSet<>();
        for (PromptVariant v : variants.values()) {
            if (v.getId() != null) {
                models.add(v.getId());
            }
        }
        return new ArrayList<>(models);
    }

    public PromptVariant getVariantMetadata(String modelId) {
        return variants.get(modelId);
    }

    /**
     * 通过遍历所有已注册变体的 matcher 函数来检测模型家族。
     * 对应 TS PromptRegistry.getModelFamily() 的 matcher-based 方法。
     */
    private ModelFamily detectModelFamily(SystemPromptContext context) {
        // 遍历所有变体，找到第一个匹配的（排除 GENERIC，它作为兜底）
        for (Map.Entry<String, PromptVariant> entry : variants.entrySet()) {
            PromptVariant variant = entry.getValue();
            if (variant.getFamily() == ModelFamily.GENERIC) {
                continue;
            }
            if (variant.getMatcher() != null) {
                try {
                    if (Boolean.TRUE.equals(variant.getMatcher().apply(context))) {
                        return variant.getFamily();
                    }
                } catch (Exception e) {
                    log.warn("Matcher failed for variant {}: {}", variant.getId(), e.getMessage());
                }
            }
        }
        return ModelFamily.GENERIC;
    }

    private PromptVariant findVariant(ModelFamily modelFamily) {
        PromptVariant variant = variants.get(modelFamily.getValue());
        if (variant != null) {
            return variant;
        }

        variant = variants.get(ModelFamily.GENERIC.getValue());
        if (variant != null) {
            log.warn("No specific variant found for {}, using generic variant", modelFamily);
            return variant;
        }

        return null;
    }

    private void loadVariants() {
        variants.clear();
        var loaded = variantRegistry.getAllVariants();
        for (var entry : loaded.entrySet()) {
            ModelFamily family = entry.getKey();
            PromptVariant variant = entry.getValue();
            variants.put(family.getValue(), variant);
            if (variant.getVersion() != null && variant.getVersion() > 1) {
                variants.put(family.getValue() + "@" + variant.getVersion(), variant);
            }
        }

        if (!variants.containsKey(ModelFamily.GENERIC.getValue())) {
            log.warn("Generic variant not found, loading generic singleton config");
            variants.put(ModelFamily.GENERIC.getValue(), GenericVariantConfig.getConfig());
        }
        log.debug("Loaded {} variants", variants.size());
    }

    private void performHealthCheck() {
        if (variants.isEmpty()) {
            log.error("Health check failed: No variants loaded");
            throw new RuntimeException("No variants loaded");
        }

        if (!variants.containsKey(ModelFamily.GENERIC.getValue())) {
            log.error("Health check failed: No generic variant found");
            throw new RuntimeException("No generic variant found");
        }

        log.info("Health check passed");
    }
}
