package com.hhoa.kline.core.core.prompts.systemprompt;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Data;

/**
 * 提示变体
 *
 * @author hhoa
 */
@Data
@Builder
public class PromptVariant {

    private String id;

    private Integer version;

    private List<String> tags;

    private Map<String, Integer> labels;

    private ModelFamily family;

    private String description;

    /** 变体匹配函数，用于根据上下文判断是否应使用此变体。 对应 TS PromptVariant.matcher */
    private Function<SystemPromptContext, Boolean> matcher;

    private PromptConfig config;

    private String baseTemplate;

    private List<SystemPromptSection> componentOrder;

    private Map<SystemPromptSection, ConfigOverride> componentOverrides;

    private Map<String, String> placeholders;

    private List<String> tools;

    private Map<String, ConfigOverride> toolOverrides;

    public static String generateTemplateFromComponents(List<SystemPromptSection> components) {
        if (components == null || components.isEmpty()) {
            throw new IllegalArgumentException(
                    "Cannot generate template from empty component order");
        }

        return components.stream()
                .map(component -> "{{" + component.name() + "}}")
                .collect(Collectors.joining("\n\n====\n\n"));
    }

    public static PromptVariant buildAndValidate(PromptVariant.PromptVariantBuilder builder) {
        PromptVariant variant = builder.build();

        if (variant.getComponentOrder() == null || variant.getComponentOrder().isEmpty()) {
            throw new IllegalArgumentException("Component order is required");
        }
        if (variant.getDescription() == null || variant.getDescription().trim().isEmpty()) {
            throw new IllegalArgumentException("Description is required");
        }

        String template = variant.getBaseTemplate();
        if (template == null || template.trim().isEmpty()) {
            template = generateTemplateFromComponents(variant.getComponentOrder());
            return PromptVariant.builder()
                    .id(variant.getId())
                    .version(variant.getVersion())
                    .tags(variant.getTags())
                    .labels(variant.getLabels())
                    .family(variant.getFamily())
                    .description(variant.getDescription())
                    .matcher(variant.getMatcher())
                    .config(variant.getConfig())
                    .baseTemplate(template)
                    .componentOrder(variant.getComponentOrder())
                    .componentOverrides(variant.getComponentOverrides())
                    .placeholders(variant.getPlaceholders())
                    .tools(variant.getTools())
                    .toolOverrides(variant.getToolOverrides())
                    .build();
        }

        return variant;
    }
}
