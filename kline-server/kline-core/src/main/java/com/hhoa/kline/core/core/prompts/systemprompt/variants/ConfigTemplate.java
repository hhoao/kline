package com.hhoa.kline.core.core.prompts.systemprompt.variants;

import com.hhoa.kline.core.core.prompts.systemprompt.PromptVariant;
import lombok.extern.slf4j.Slf4j;

/**
 * Enhanced Type-Safe Variant Configuration Template
 *
 * <p>This template provides a type-safe way to create new prompt variants with compile-time
 * validation and IntelliSense support.
 *
 * @author hhoa
 */
@Slf4j
public class ConfigTemplate {
    /** Validate a variant configuration */
    public static void validateVariantConfig(PromptVariant variant, String variantId) {
        try {
            VariantValidator.ValidationResult validationResult =
                    VariantValidator.validateVariant(variant, true);

            if (!validationResult.isValid()) {
                log.error(
                        "{} variant configuration validation failed: {}",
                        variantId,
                        validationResult.getErrors());
                throw new RuntimeException(
                        "Invalid "
                                + variantId
                                + " variant configuration: "
                                + String.join(", ", validationResult.getErrors()));
            }

            if (!validationResult.getWarnings().isEmpty()) {
                log.warn(
                        "{} variant configuration warnings: {}",
                        variantId,
                        validationResult.getWarnings());
            }
        } catch (Exception e) {
            log.error("Failed to validate {} variant configuration", variantId, e);
            throw e;
        }
    }
}
