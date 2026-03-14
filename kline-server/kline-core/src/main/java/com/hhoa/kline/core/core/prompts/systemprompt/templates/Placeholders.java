package com.hhoa.kline.core.core.prompts.systemprompt.templates;

import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptSection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Standard placeholder definitions used across prompt templates
 *
 * @author hhoa
 */
public class Placeholders {

    public static final Map<String, String> STANDARD_PLACEHOLDERS;

    static {
        Map<String, String> placeholders = new HashMap<>();

        // System Information
        placeholders.put("OS", "OS");
        placeholders.put("SHELL", "SHELL");
        placeholders.put("HOME_DIR", "HOME_DIR");
        placeholders.put("WORKING_DIR", "WORKING_DIR");

        // MCP Servers
        placeholders.put("MCP_SERVERS_LIST", "MCP_SERVERS_LIST");

        // Context Variables
        placeholders.put("CWD", "CWD");
        placeholders.put("SUPPORTS_BROWSER", "SUPPORTS_BROWSER");
        placeholders.put("MODEL_FAMILY", "MODEL_FAMILY");

        // Dynamic Content
        placeholders.put("CURRENT_DATE", "CURRENT_DATE");

        // System Prompt Sections (automatically include all enum values, matching TS version)
        for (SystemPromptSection section : SystemPromptSection.values()) {
            placeholders.put(section.getValue(), section.getValue());
        }

        STANDARD_PLACEHOLDERS = Collections.unmodifiableMap(placeholders);
    }

    /** Required placeholders that must be provided for basic prompt functionality */
    public static final List<String> REQUIRED_PLACEHOLDERS =
            List.of(
                    SystemPromptSection.AGENT_ROLE.getValue(),
                    SystemPromptSection.SYSTEM_INFO.getValue());

    /** Optional placeholders that enhance prompt functionality when available */
    public static final List<String> OPTIONAL_PLACEHOLDERS =
            List.of(
                    SystemPromptSection.FEEDBACK.getValue(),
                    SystemPromptSection.USER_INSTRUCTIONS.getValue(),
                    SystemPromptSection.TODO.getValue());

    /** Validates that all required placeholders are present in the provided values */
    public static List<String> validateRequiredPlaceholders(Map<String, Object> placeholders) {
        List<String> missing = new ArrayList<>();

        for (String required : REQUIRED_PLACEHOLDERS) {
            if (!placeholders.containsKey(required) || placeholders.get(required) == null) {
                missing.add(required);
            }
        }

        return missing;
    }
}
