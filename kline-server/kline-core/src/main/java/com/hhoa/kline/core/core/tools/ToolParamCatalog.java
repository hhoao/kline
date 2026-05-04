package com.hhoa.kline.core.core.tools;

import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Collects tool parameter names from registered tool schemas. */
public final class ToolParamCatalog {
    private final ToolRegistry toolRegistry;

    public ToolParamCatalog(ToolRegistry toolRegistry) {
        if (toolRegistry == null) {
            throw new IllegalArgumentException("toolRegistry == null");
        }
        this.toolRegistry = toolRegistry;
    }

    public Set<String> all() {
        Set<String> names = new LinkedHashSet<>();

        for (ToolSpec toolSpec : toolRegistry.getToolSpecs(ModelFamily.GENERIC, null, null)) {
            Map<String, Object> schema = toolSpec.getInputSchema();
            if (schema == null) {
                continue;
            }
            Object properties = schema.get("properties");
            if (!(properties instanceof Map<?, ?> propertyMap)) {
                continue;
            }
            for (Object key : propertyMap.keySet()) {
                if (key instanceof String keyName && !keyName.isBlank()) {
                    names.add(keyName);
                }
            }
        }

        return Collections.unmodifiableSet(names);
    }
}
