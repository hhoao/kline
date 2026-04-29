package com.hhoa.kline.core.core.tools;

import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/** Utilities for reading and customizing tool input JSON Schema. */
public final class ToolSchema {
    public static final String X_USAGE = "x-kline-usage";
    public static final String X_DEPENDENCIES = "x-kline-dependencies";
    public static final String X_CONTEXT_REQUIREMENTS = "x-kline-context-requirements";
    public static final String X_INSTRUCTION_FN = "x-kline-instruction-fn";

    private ToolSchema() {}

    public static Map<String, Object> objectSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", new LinkedHashMap<String, Object>());
        schema.put("required", new ArrayList<String>());
        schema.put("additionalProperties", false);
        return schema;
    }

    public static Map<String, Object> stringProperty(String description) {
        Map<String, Object> property = new LinkedHashMap<>();
        property.put("type", "string");
        if (description != null && !description.isBlank()) {
            property.put("description", description);
        }
        return property;
    }

    public static void putProperty(
            Map<String, Object> schema, String name, Map<String, Object> property) {
        properties(schema).put(name, property);
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Map<String, Object>> properties(Map<String, Object> schema) {
        if (schema == null) {
            return new LinkedHashMap<>();
        }
        Object propertiesValue = schema.get("properties");
        if (!(propertiesValue instanceof Map<?, ?> rawProperties)) {
            Map<String, Map<String, Object>> properties = new LinkedHashMap<>();
            schema.put("properties", properties);
            return properties;
        }
        if (rawProperties instanceof LinkedHashMap<?, ?>) {
            return (Map<String, Map<String, Object>>) rawProperties;
        }
        Map<String, Map<String, Object>> properties = new LinkedHashMap<>();
        rawProperties.forEach(
                (key, value) -> {
                    if (key != null && value instanceof Map<?, ?>) {
                        properties.put(String.valueOf(key), (Map<String, Object>) value);
                    }
                });
        schema.put("properties", properties);
        return properties;
    }

    public static Map<String, Object> property(Map<String, Object> schema, String name) {
        return properties(schema).get(name);
    }

    public static Set<String> required(Map<String, Object> schema) {
        if (schema == null) {
            return Set.of();
        }
        Object requiredValue = schema.get("required");
        if (!(requiredValue instanceof List<?> requiredList)) {
            return Set.of();
        }
        Set<String> required = new LinkedHashSet<>();
        requiredList.forEach(value -> required.add(String.valueOf(value)));
        return required;
    }

    public static void require(Map<String, Object> schema, String name) {
        List<String> required = mutableRequired(schema);
        if (!required.contains(name)) {
            required.add(name);
        }
    }

    public static void optional(Map<String, Object> schema, String name) {
        mutableRequired(schema).removeIf(name::equals);
    }

    @SuppressWarnings("unchecked")
    private static List<String> mutableRequired(Map<String, Object> schema) {
        Object requiredValue = schema.get("required");
        if (requiredValue instanceof List<?> rawRequired) {
            List<String> required = new ArrayList<>();
            rawRequired.forEach(value -> required.add(String.valueOf(value)));
            schema.put("required", required);
            return required;
        }
        List<String> required = new ArrayList<>();
        schema.put("required", required);
        return (List<String>) schema.get("required");
    }

    public static void exclude(Map<String, Object> schema, Set<String> names) {
        if (names == null || names.isEmpty()) {
            return;
        }
        Map<String, Map<String, Object>> properties = properties(schema);
        names.forEach(properties::remove);
        mutableRequired(schema).removeIf(names::contains);
        schema.put("properties", new LinkedHashMap<>(properties));
    }

    public static void description(Map<String, Object> schema, String name, String description) {
        Map<String, Object> property = property(schema, name);
        if (property != null && description != null) {
            property.put("description", description);
        }
    }

    public static void usage(Map<String, Object> schema, String name, String usage) {
        Map<String, Object> property = property(schema, name);
        if (property != null && usage != null) {
            property.put(X_USAGE, usage);
        }
    }

    public static void dependencies(
            Map<String, Object> schema, String name, List<String> dependencies) {
        Map<String, Object> property = property(schema, name);
        if (property != null && dependencies != null && !dependencies.isEmpty()) {
            property.put(X_DEPENDENCIES, List.copyOf(dependencies));
        }
    }

    public static void contextRequirements(
            Map<String, Object> schema,
            String name,
            Function<SystemPromptContext, Boolean> contextRequirements) {
        Map<String, Object> property = property(schema, name);
        if (property != null && contextRequirements != null) {
            property.put(X_CONTEXT_REQUIREMENTS, contextRequirements);
        }
    }

    public static void instructionFn(
            Map<String, Object> schema,
            String name,
            Function<SystemPromptContext, String> instructionFn) {
        Map<String, Object> property = property(schema, name);
        if (property != null && instructionFn != null) {
            property.put(X_INSTRUCTION_FN, instructionFn);
        }
    }

    @SuppressWarnings("unchecked")
    public static boolean parameterEnabled(
            Map<String, Object> property,
            SystemPromptContext context,
            List<String> registryToolIds) {
        Object dependencies = property.get(X_DEPENDENCIES);
        if (dependencies instanceof List<?> list
                && registryToolIds != null
                && !registryToolIds.containsAll(list.stream().map(String::valueOf).toList())) {
            return false;
        }
        Object requirement = property.get(X_CONTEXT_REQUIREMENTS);
        if (requirement instanceof Function<?, ?> function) {
            try {
                return Boolean.TRUE.equals(
                        ((Function<SystemPromptContext, Boolean>) function).apply(context));
            } catch (Exception ignored) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    public static String instruction(Map<String, Object> property, SystemPromptContext context) {
        Object instructionFn = property.get(X_INSTRUCTION_FN);
        if (instructionFn instanceof Function<?, ?> function && context != null) {
            try {
                return ((Function<SystemPromptContext, String>) function).apply(context);
            } catch (Exception ignored) {
            }
        }
        Object description = property.get("description");
        return description != null ? String.valueOf(description) : "";
    }

    public static String usage(Map<String, Object> property) {
        Object usage = property.get(X_USAGE);
        return usage != null ? String.valueOf(usage) : "";
    }

    public static String type(Map<String, Object> property) {
        Object type = property.get("type");
        if (type instanceof String text) {
            return text;
        }
        if (type instanceof List<?> types) {
            return types.stream()
                    .map(String::valueOf)
                    .filter(value -> !"null".equals(value))
                    .findFirst()
                    .orElse("object");
        }
        return "object";
    }

    public static Map<String, Object> externalSchema(Map<String, Object> schema) {
        if (schema == null) {
            return objectSchema();
        }
        Map<String, Object> copy = new LinkedHashMap<>(schema);
        Map<String, Object> properties = new LinkedHashMap<>();
        properties(schema)
                .forEach(
                        (name, property) -> {
                            Map<String, Object> propertyCopy = new LinkedHashMap<>(property);
                            propertyCopy.keySet().removeIf(key -> key.startsWith("x-kline-"));
                            properties.put(name, propertyCopy);
                        });
        copy.put("properties", properties);
        return copy;
    }

    public static List<String> propertyNames(Map<String, Object> schema) {
        return Collections.unmodifiableList(new ArrayList<>(properties(schema).keySet()));
    }
}
