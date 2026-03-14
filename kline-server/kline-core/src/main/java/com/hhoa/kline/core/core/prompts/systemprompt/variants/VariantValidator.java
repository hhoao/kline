package com.hhoa.kline.core.core.prompts.systemprompt.variants;

import com.hhoa.kline.core.core.prompts.systemprompt.ConfigOverride;
import com.hhoa.kline.core.core.prompts.systemprompt.PromptVariant;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptSection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Data;

/**
 * Validator for prompt variants Provides validation logic for variant configurations
 *
 * @author hhoa
 */
public class VariantValidator {

    @Data
    public static class ValidationResult {
        private boolean isValid;
        private List<String> errors;
        private List<String> warnings;

        public ValidationResult() {
            this.isValid = true;
            this.errors = new ArrayList<>();
            this.warnings = new ArrayList<>();
        }

        public void addError(String error) {
            this.errors.add(error);
            this.isValid = false;
        }

        public void addWarning(String warning) {
            this.warnings.add(warning);
        }
    }

    /** Validate a variant configuration */
    public static ValidationResult validateVariant(PromptVariant variant, boolean strict) {
        ValidationResult result = new ValidationResult();

        // Required fields validation
        if (variant.getDescription() == null || variant.getDescription().trim().isEmpty()) {
            result.addError("Description is required");
        }

        if (variant.getComponentOrder() == null || variant.getComponentOrder().isEmpty()) {
            result.addError("Component order is required");
        }

        // Duplicate components
        if (variant.getComponentOrder() != null) {
            Set<SystemPromptSection> seen = new HashSet<>();
            Set<SystemPromptSection> dups = new HashSet<>();
            for (SystemPromptSection s : variant.getComponentOrder()) {
                if (!seen.add(s)) {
                    dups.add(s);
                }
            }
            if (!dups.isEmpty()) {
                result.addError("Duplicate components in order: " + dups);
            }
        }

        if (variant.getFamily() == null) {
            result.addError("Model family is required");
        }

        if (variant.getVersion() <= 0) {
            result.addError("Version must be positive");
        }

        // Template validation
        if (variant.getBaseTemplate() == null || variant.getBaseTemplate().trim().isEmpty()) {
            result.addError("Base template is required");
        } else if (variant.getComponentOrder() != null) {
            // Check components used in template
            List<SystemPromptSection> missingInTemplate = new ArrayList<>();
            for (SystemPromptSection section : variant.getComponentOrder()) {
                String placeholder = "{{" + section.name() + "}}";
                if (!variant.getBaseTemplate().contains(placeholder)) {
                    missingInTemplate.add(section);
                }
            }
            if (!missingInTemplate.isEmpty()) {
                result.addWarning(
                        "Components defined but not used in template: " + missingInTemplate);
            }
        }

        // Strict validation
        if (strict) {
            if (variant.getTags() == null || variant.getTags().isEmpty()) {
                result.addWarning("No tags specified");
            }

            if (variant.getLabels() == null || variant.getLabels().isEmpty()) {
                result.addWarning("No labels specified");
            }
        }

        // Component overrides reference check
        Map<SystemPromptSection, ConfigOverride> compOverrides = variant.getComponentOverrides();
        if (compOverrides != null && variant.getComponentOrder() != null) {
            List<SystemPromptSection> invalid = new ArrayList<>();
            for (SystemPromptSection key : compOverrides.keySet()) {
                if (!variant.getComponentOrder().contains(key)) {
                    invalid.add(key);
                }
            }
            if (!invalid.isEmpty()) {
                result.addWarning("Component overrides for unused components: " + invalid);
            }
        }

        // Tools validation (duplicates and overrides referencing)
        if (variant.getTools() != null) {
            Set<String> seenTools = new HashSet<>();
            Set<String> dupTools = new HashSet<>();
            for (String t : variant.getTools()) {
                if (!seenTools.add(t)) {
                    dupTools.add(t);
                }
            }
            if (!dupTools.isEmpty()) {
                result.addError("Duplicate tools: " + dupTools);
            }
            Map<String, ConfigOverride> toolOverrides = variant.getToolOverrides();
            if (toolOverrides != null) {
                List<String> invalidToolOverrides = new ArrayList<>();
                for (String k : toolOverrides.keySet()) {
                    if (!seenTools.contains(k)) {
                        invalidToolOverrides.add(k);
                    }
                }
                if (!invalidToolOverrides.isEmpty()) {
                    result.addWarning("Tool overrides for unused tools: " + invalidToolOverrides);
                }
            }
        }

        return result;
    }
}
