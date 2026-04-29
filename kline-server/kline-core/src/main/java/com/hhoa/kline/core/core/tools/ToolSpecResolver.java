package com.hhoa.kline.core.core.tools;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
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
import com.hhoa.kline.core.core.tools.types.ToolContext;
import com.hhoa.kline.core.core.tools.types.UIHelpers;
import com.hhoa.kline.core.enums.ClineDefaultTool;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Resolves a {@link ToolSpec} from typed metadata and argument schema. */
public final class ToolSpecResolver {
    private static final SchemaGenerator SCHEMA_GENERATOR = createSchemaGenerator();

    private ToolSpecResolver() {}

    public static <I, H extends ToolHandler> ToolSpec resolve(
            ToolSpecProvider<I, H> provider, ModelFamily family, H handler) {
        validateHandlerBinding(provider, handler);
        if (!provider.enabled(family)) {
            return null;
        }
        MethodInput methodInput = resolveMethodInput(handler.getClass(), "execute");
        Class<?> inputType = methodInput.primaryInputType();
        Map<String, Object> inputSchema = generateInputSchema(methodInput);
        customizeInputSchema(provider, family, inputSchema);
        return ToolSpec.builder()
                .variant(family)
                .id(provider.id())
                .name(provider.name())
                .description(provider.description(family))
                .instruction(provider.instruction(family))
                .contextRequirements(provider.contextRequirements(family))
                .inputType(inputType)
                .inputSchema(inputSchema)
                .build();
    }

    public static ToolSpec resolve(
            String id,
            String name,
            String description,
            String instruction,
            Class<?> inputType,
            ModelFamily family) {
        Map<String, Object> inputSchema = generateInputSchema(inputType);
        applyDefaultSchemaExtensions(inputSchema);
        return ToolSpec.builder()
                .variant(family)
                .id(id)
                .name(name)
                .description(description)
                .instruction(instruction)
                .inputType(inputType)
                .inputSchema(inputSchema)
                .build();
    }

    public static ToolSpec resolve(
            String id,
            String name,
            String description,
            String instruction,
            ModelFamily family,
            ToolHandler handler) {
        MethodInput methodInput = resolveMethodInput(handler.getClass(), "execute");
        Map<String, Object> inputSchema = generateInputSchema(methodInput);
        applyDefaultSchemaExtensions(inputSchema);
        return ToolSpec.builder()
                .variant(family != null ? family : ModelFamily.GENERIC)
                .id(id)
                .name(name)
                .description(description)
                .instruction(instruction)
                .inputType(methodInput.primaryInputType())
                .inputSchema(inputSchema)
                .build();
    }

    private static void customizeInputSchema(
            ToolSpecProvider<?, ?> provider, ModelFamily family, Map<String, Object> inputSchema) {
        applyDefaultSchemaExtensions(inputSchema);
        ToolSchema.exclude(inputSchema, provider.excludedParameters(family));
        provider.customizeInputSchema(family, inputSchema);
    }

    private static void applyDefaultSchemaExtensions(Map<String, Object> inputSchema) {
        if (ToolSchema.property(inputSchema, "task_progress") != null) {
            ToolSchema.dependencies(
                    inputSchema, "task_progress", List.of(ClineDefaultTool.TODO.getValue()));
        }
    }

    public static <I, H extends ToolHandler> void validateHandlerBinding(
            ToolSpecProvider<I, H> provider, H handler) {
        if (handler == null) {
            throw new IllegalArgumentException("Tool handler cannot be null.");
        }

        Class<?> actualInputType =
                resolveMethodInput(handler.getClass(), "execute").primaryInputType();
        Class<?> expectedInputType = resolveProviderInputType(provider.getClass());
        if (expectedInputType != null && !expectedInputType.equals(actualInputType)) {
            throw new IllegalArgumentException(
                    "Tool spec provider '%s' expects input type %s but handler %s uses %s."
                            .formatted(
                                    provider.getClass().getName(),
                                    expectedInputType.getName(),
                                    handler.getClass().getName(),
                                    actualInputType != null
                                            ? actualInputType.getName()
                                            : "<none>"));
        }
    }

    public static Class<?> resolveArgumentType(Class<?> handlerType) {
        return resolveMethodInput(handlerType, "execute").primaryInputType();
    }

    public static Class<?> resolveProviderInputType(Class<?> providerType) {
        Class<?> resolved = resolveGenericArgumentTypeFrom(providerType, ToolSpecProvider.class, 0);
        return resolved != null && !Object.class.equals(resolved) ? resolved : null;
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
        if (argumentType == null
                || Void.class.equals(argumentType)
                || Void.TYPE.equals(argumentType)) {
            return emptyObjectSchema();
        }
        ObjectNode schema = SCHEMA_GENERATOR.generateSchema(argumentType);
        schema.put("additionalProperties", false);
        @SuppressWarnings("unchecked")
        Map<String, Object> schemaMap =
                JsonUtils.readValue(
                        schema.toString(), new TypeReference<LinkedHashMap<String, Object>>() {});
        return schemaMap;
    }

    public static Map<String, Object> generateInputSchema(MethodInput input) {
        if (input == null || input.inputParameters().isEmpty()) {
            return emptyObjectSchema();
        }
        if (input.inputParameters().size() == 1
                && isAggregateInputType(input.inputParameters().getFirst().getType())) {
            return generateInputSchema(input.inputParameters().getFirst().getType());
        }

        Map<String, Object> schema = new LinkedHashMap<>();
        Map<String, Object> properties = new LinkedHashMap<>();
        List<String> required = new ArrayList<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", required);
        schema.put("additionalProperties", false);

        for (Parameter parameter : input.inputParameters()) {
            String name = parameterName(parameter);
            Map<String, Object> propertySchema = generateInputSchema(parameter.getType());
            propertySchema.remove("additionalProperties");
            JsonPropertyDescription description =
                    parameter.getAnnotation(JsonPropertyDescription.class);
            if (description != null && !description.value().isBlank()) {
                propertySchema.put("description", description.value());
            }
            properties.put(name, propertySchema);
            JsonProperty jsonProperty = parameter.getAnnotation(JsonProperty.class);
            if (jsonProperty == null || jsonProperty.required()) {
                required.add(name);
            }
        }
        return schema;
    }

    private static Map<String, Object> emptyObjectSchema() {
        return ToolSchema.objectSchema();
    }

    public record MethodInput(Method method, List<Parameter> inputParameters) {
        Class<?> primaryInputType() {
            if (inputParameters == null || inputParameters.isEmpty()) {
                return null;
            }
            if (inputParameters.size() == 1) {
                return inputParameters.getFirst().getType();
            }
            return null;
        }
    }

    public static MethodInput resolveMethodInput(Class<?> handlerType, String methodName) {
        Method best = null;
        List<Parameter> bestInputParameters = List.of();
        for (Method method : methodsOf(handlerType)) {
            if (!methodName.equals(method.getName())) {
                continue;
            }
            List<Parameter> inputParameters = inputParametersBeforeContext(method);
            if (inputParameters.isEmpty()) {
                continue;
            }
            if (best == null || isDeclaredCloserTo(handlerType, method, best)) {
                best = method;
                bestInputParameters = inputParameters;
            }
        }
        return new MethodInput(best, bestInputParameters);
    }

    private static List<Method> methodsOf(Class<?> type) {
        List<Method> methods = new ArrayList<>();
        Class<?> current = type;
        while (current != null && !Object.class.equals(current)) {
            Collections.addAll(methods, current.getDeclaredMethods());
            current = current.getSuperclass();
        }
        for (Class<?> iface : type.getInterfaces()) {
            Collections.addAll(methods, iface.getMethods());
        }
        return methods;
    }

    private static boolean isDeclaredCloserTo(Class<?> handlerType, Method candidate, Method best) {
        if (candidate.getDeclaringClass().equals(best.getDeclaringClass())) {
            return candidate.getParameterCount() > best.getParameterCount();
        }
        return candidate.getDeclaringClass().equals(handlerType);
    }

    private static List<Parameter> inputParametersBeforeContext(Method method) {
        List<Parameter> inputParameters = new ArrayList<>();
        for (Parameter parameter : method.getParameters()) {
            if (isContextParameter(parameter.getType())) {
                break;
            }
            inputParameters.add(parameter);
        }
        return inputParameters;
    }

    public static boolean isContextParameter(Class<?> type) {
        return ToolContext.class.isAssignableFrom(type)
                || UIHelpers.class.isAssignableFrom(type)
                || com.hhoa.kline.core.core.assistant.ToolUse.class.isAssignableFrom(type);
    }

    private static boolean isAggregateInputType(Class<?> type) {
        return !(type.isPrimitive()
                || String.class.equals(type)
                || Number.class.isAssignableFrom(type)
                || Boolean.class.equals(type)
                || Character.class.equals(type)
                || Map.class.isAssignableFrom(type)
                || List.class.isAssignableFrom(type));
    }

    public static String parameterName(Parameter parameter) {
        JsonProperty jsonProperty = parameter.getAnnotation(JsonProperty.class);
        if (jsonProperty != null && !jsonProperty.value().isBlank()) {
            return jsonProperty.value();
        }
        if (parameter.isNamePresent()) {
            return parameter.getName();
        }
        throw new IllegalArgumentException(
                "Tool method parameter '%s' on %s needs @JsonProperty or javac -parameters."
                        .formatted(parameter, parameter.getDeclaringExecutable()));
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
