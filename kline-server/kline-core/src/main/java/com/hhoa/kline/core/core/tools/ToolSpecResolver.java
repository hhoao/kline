package com.hhoa.kline.core.core.tools;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.github.victools.jsonschema.module.jackson.JacksonModule;
import com.github.victools.jsonschema.module.jackson.JacksonOption;
import com.github.victools.jsonschema.module.swagger2.Swagger2Module;
import com.hhoa.ai.kline.commons.utils.JsonUtils;
import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.tools.handlers.ToolHandler;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;

/** Resolves a {@link ToolSpec} from typed metadata and argument schema. */
public final class ToolSpecResolver {
    private static final SchemaGenerator SCHEMA_GENERATOR = createSchemaGenerator();

    private ToolSpecResolver() {}

    public static ToolSpec resolve(ToolSpecProvider<?> provider, ModelFamily family) {
        ToolHandler<?> handler = provider.handler(family);
        validateHandlerBinding(provider, handler, family);

        Class<?> inputType = requireProviderInputType(provider, family);
        Map<String, Object> inputSchema = generateInputSchema(inputType);
        return ToolSpec.builder()
                .variant(family)
                .name(provider.name())
                .description(provider.description(family))
                .prompt(provider.prompt(family))
                .contextRequirements(provider.contextRequirements(family))
                .inputType(inputType)
                .inputSchema(inputSchema)
                .build();
    }

    public static ToolSpec resolve(
            String id,
            String name,
            String description,
            String prompt,
            Class<?> inputType,
            ModelFamily family) {
        Map<String, Object> inputSchema = generateInputSchema(inputType);
        return ToolSpec.builder()
                .variant(family)
                .name(name)
                .description(description)
                .prompt(prompt)
                .inputType(inputType)
                .inputSchema(inputSchema)
                .build();
    }

    public static ToolSpec resolve(
            String id,
            String name,
            String description,
            String prompt,
            ModelFamily family,
            ToolHandler<?> handler) {
        Class<?> inputType = requireArgumentType(handler.getClass());
        Map<String, Object> inputSchema = generateInputSchema(inputType);
        return ToolSpec.builder()
                .variant(family != null ? family : ModelFamily.GENERIC)
                .name(name)
                .description(description)
                .prompt(prompt)
                .inputType(inputType)
                .inputSchema(inputSchema)
                .build();
    }

    public static void validateHandlerBinding(
            ToolSpecProvider<?> provider, ToolHandler<?> handler, ModelFamily family) {
        if (handler == null) {
            throw new IllegalArgumentException("Tool handler cannot be null.");
        }

        Class<?> handlerArg = requireArgumentType(handler.getClass());
        Class<?> schemaArg = requireProviderInputType(provider, family);
        if (!schemaArg.equals(handlerArg)) {
            throw new IllegalArgumentException(
                    ("Tool spec provider %s declares input type %s but handler expects %s.")
                            .formatted(
                                    provider.getClass().getName(),
                                    schemaArg.getName(),
                                    handlerArg.getName()));
        }
    }

    private static Class<?> requireProviderInputType(
            ToolSpecProvider<?> provider, ModelFamily family) {
        Class<?> inputType = provider.inputType(family);
        if (inputType == null || Object.class.equals(inputType)) {
            throw new IllegalArgumentException(
                    "Tool spec provider %s must declare a concrete public input type."
                            .formatted(provider.getClass().getName()));
        }
        return inputType;
    }

    public static Class<?> resolveArgumentType(Class<?> handlerType) {
        Class<?> resolved = resolveArgumentTypeFrom(handlerType);
        return resolved != null && !Object.class.equals(resolved) ? resolved : null;
    }

    public static Class<?> requireArgumentType(Class<?> handlerType) {
        Class<?> inputType = resolveArgumentType(handlerType);
        if (inputType == null) {
            throw new IllegalArgumentException(
                    "Tool handler %s must declare a concrete ToolHandler input type."
                            .formatted(handlerType.getName()));
        }
        return inputType;
    }

    private static SchemaGenerator createSchemaGenerator() {
        SchemaGeneratorConfigBuilder builder =
                new SchemaGeneratorConfigBuilder(
                                SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON)
                        .with(new JacksonModule(JacksonOption.RESPECT_JSONPROPERTY_REQUIRED))
                        .with(new Swagger2Module())
                        .with(Option.EXTRA_OPEN_API_FORMAT_VALUES)
                        .with(Option.PLAIN_DEFINITION_KEYS);
        SchemaGeneratorConfig config = builder.build();
        return new SchemaGenerator(config);
    }

    public static Map<String, Object> generateInputSchema(Class<?> argumentType) {
        ObjectNode schema = SCHEMA_GENERATOR.generateSchema(argumentType);
        @SuppressWarnings("unchecked")
        Map<String, Object> schemaMap =
                JsonUtils.readValue(
                        schema.toString(), new TypeReference<LinkedHashMap<String, Object>>() {});
        return schemaMap;
    }
    private static Class<?> resolveArgumentTypeFrom(Type type) {
        return resolveGenericArgumentTypeFrom(type, ToolHandler.class, 0);
    }

    private static Class<?> resolveGenericArgumentTypeFrom(
            Type type, Class<?> targetRawType, int argumentIndex) {
        if (type instanceof ParameterizedType parameterizedType) {
            Class<?> rawType = rawClass(parameterizedType.getRawType());
            if (rawType != null && targetRawType.isAssignableFrom(rawType)) {
                return rawClass(parameterizedType.getActualTypeArguments()[argumentIndex]);
            }
            if (rawType != null) {
                Class<?> nested =
                        resolveGenericArgumentTypeFrom(rawType, targetRawType, argumentIndex);
                if (nested != null) {
                    return nested;
                }
            }
        }
        if (type instanceof Class<?> clazz) {
            for (Type genericInterface : clazz.getGenericInterfaces()) {
                Class<?> resolved =
                        resolveGenericArgumentTypeFrom(
                                genericInterface, targetRawType, argumentIndex);
                if (resolved != null) {
                    return resolved;
                }
            }
            Type genericSuperclass = clazz.getGenericSuperclass();
            if (genericSuperclass != null) {
                return resolveGenericArgumentTypeFrom(
                        genericSuperclass, targetRawType, argumentIndex);
            }
        }
        return null;
    }

    private static Class<?> rawClass(Type type) {
        if (type instanceof Class<?> clazz) {
            return clazz;
        }
        if (type instanceof ParameterizedType parameterizedType) {
            return rawClass(parameterizedType.getRawType());
        }
        return null;
    }
}
