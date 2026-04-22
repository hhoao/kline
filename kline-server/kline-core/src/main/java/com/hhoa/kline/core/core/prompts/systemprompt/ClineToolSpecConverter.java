package com.hhoa.kline.core.core.prompts.systemprompt;

import java.util.*;
import java.util.function.Function;

/**
 * 将 ClineToolSpec 转换为不同 API 提供商的工具定义格式。 对应 TS 的 spec.ts 中的转换函数。
 *
 * @author hhoa
 */
public class ClineToolSpecConverter {

    /** 多工作区提示 */
    public static final String MULTI_ROOT_HINT =
            " Use @workspace:path syntax (e.g., @frontend:src/index.ts) to specify a workspace.";

    private static final Set<String> RESERVED_KEYS =
            Set.of(
                    "name",
                    "required",
                    "instruction",
                    "usage",
                    "dependencies",
                    "description",
                    "contextRequirements",
                    "type",
                    "items",
                    "properties");

    /** Google Gemini 参数类型映射 */
    private static final Map<String, String> GOOGLE_TOOL_PARAM_MAP =
            Map.of(
                    "string", "STRING",
                    "number", "NUMBER",
                    "integer", "NUMBER",
                    "boolean", "BOOLEAN",
                    "object", "OBJECT",
                    "array", "STRING");

    /** 将 ClineToolSpec 转换为 OpenAI ChatCompletionTool 格式的 Map 结构 */
    public static Map<String, Object> toolSpecFunctionDefinition(
            ClineToolSpec tool, SystemPromptContext context) {
        validateContextRequirements(tool, context);

        Map<String, Object> properties = new LinkedHashMap<>();
        List<String> required = new ArrayList<>();
        buildParameterSchemas(tool, context, properties, required);

        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("type", "object");
        parameters.put("properties", properties);
        parameters.put("required", required);
        parameters.put("additionalProperties", false);

        Map<String, Object> function = new LinkedHashMap<>();
        function.put("name", tool.getName());
        function.put("description", replacer(tool.getDescription(), context));
        function.put("strict", false);
        function.put("parameters", parameters);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", "function");
        result.put("function", function);
        return result;
    }

    /** 将 ClineToolSpec 转换为 Anthropic Tool 格式的 Map 结构 */
    public static Map<String, Object> toolSpecInputSchema(
            ClineToolSpec tool, SystemPromptContext context) {
        validateContextRequirements(tool, context);

        Map<String, Object> properties = new LinkedHashMap<>();
        List<String> required = new ArrayList<>();
        buildParameterSchemas(tool, context, properties, required);

        Map<String, Object> inputSchema = new LinkedHashMap<>();
        inputSchema.put("type", "object");
        inputSchema.put("properties", properties);
        inputSchema.put("required", required);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("name", tool.getName());
        result.put("description", replacer(tool.getDescription(), context));
        result.put("input_schema", inputSchema);
        return result;
    }

    /** 将 ClineToolSpec 转换为 Google Gemini FunctionDeclaration 格式的 Map 结构 */
    public static Map<String, Object> toolSpecFunctionDeclarations(
            ClineToolSpec tool, SystemPromptContext context) {
        validateContextRequirements(tool, context);

        Map<String, Object> properties = new LinkedHashMap<>();
        List<String> required = new ArrayList<>();

        if (tool.getParameters() != null) {
            for (ClineToolSpec.ClineToolSpecParameter param : tool.getParameters()) {
                if (param.getContextRequirements() != null
                        && !param.getContextRequirements().apply(context)) {
                    continue;
                }
                if (param.getName() == null) {
                    continue;
                }
                if (param.isRequired()) {
                    required.add(param.getName());
                }

                String paramType = param.getType() != null ? param.getType() : "string";
                Map<String, Object> paramSchema = new LinkedHashMap<>();
                paramSchema.put("type", GOOGLE_TOOL_PARAM_MAP.getOrDefault(paramType, "OBJECT"));

                String instruction = resolveInstruction(param, context);
                if (instruction != null) {
                    String desc = replacer(instruction, context);
                    if (!desc.isEmpty()) {
                        paramSchema.put("description", desc);
                    }
                }

                if (param.getProperties() != null) {
                    Map<String, Object> nestedProps = new LinkedHashMap<>();
                    for (Map.Entry<String, Object> entry : param.getProperties().entrySet()) {
                        if ("$schema".equals(entry.getKey())) {
                            continue;
                        }
                        @SuppressWarnings("unchecked")
                        Map<String, Object> prop = (Map<String, Object>) entry.getValue();
                        Map<String, Object> nestedProp = new LinkedHashMap<>();
                        String propType =
                                prop.containsKey("type") ? prop.get("type").toString() : "string";
                        nestedProp.put(
                                "type", GOOGLE_TOOL_PARAM_MAP.getOrDefault(propType, "OBJECT"));
                        nestedProp.put("description", replacer(instruction, context));
                        if (prop.containsKey("enum")) {
                            nestedProp.put("enum", prop.get("enum"));
                        }
                        nestedProps.put(entry.getKey(), nestedProp);
                    }
                    paramSchema.put("properties", nestedProps);
                }

                properties.put(param.getName(), paramSchema);
            }
        }

        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("type", "OBJECT");
        parameters.put("properties", properties);
        parameters.put("required", required);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("name", tool.getName());
        result.put("description", replacer(tool.getDescription(), context));
        result.put("parameters", parameters);
        return result;
    }

    /** 将 OpenAI 格式的工具定义转换为 Anthropic 格式 */
    public static Map<String, Object> openAIToolToAnthropic(Map<String, Object> openAITool) {
        if ("function".equals(openAITool.get("type"))) {
            @SuppressWarnings("unchecked")
            Map<String, Object> func = (Map<String, Object>) openAITool.get("function");
            @SuppressWarnings("unchecked")
            Map<String, Object> params =
                    (Map<String, Object>) func.getOrDefault("parameters", Collections.emptyMap());

            Map<String, Object> inputSchema = new LinkedHashMap<>();
            inputSchema.put("type", "object");
            inputSchema.put(
                    "properties", params.getOrDefault("properties", Collections.emptyMap()));
            inputSchema.put("required", params.getOrDefault("required", Collections.emptyList()));

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("name", func.get("name"));
            result.put("description", func.getOrDefault("description", ""));
            result.put("input_schema", inputSchema);
            return result;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> custom = (Map<String, Object>) openAITool.get("custom");
        Map<String, Object> inputSchema = new LinkedHashMap<>();
        inputSchema.put("type", "object");
        inputSchema.put("required", List.of("text"));
        inputSchema.put("properties", Map.of("text", Map.of("type", "string")));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("name", custom.get("name"));
        result.put("description", custom.getOrDefault("description", ""));
        result.put("input_schema", inputSchema);
        return result;
    }

    /** 将 OpenAI 工具列表转换为 Response API 格式 */
    public static List<Map<String, Object>> toOpenAIResponseTools(
            List<Map<String, Object>> openAITools) {
        if (openAITools == null) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> tool : openAITools) {
            if ("function".equals(tool.get("type"))) {
                result.add(toOpenAIResponsesAPITool(tool));
            }
        }
        return result;
    }

    /** 将单个 OpenAI 工具转换为 Response API 格式 */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> toOpenAIResponsesAPITool(Map<String, Object> openAITool) {
        if ("function".equals(openAITool.get("type"))) {
            Map<String, Object> fn = (Map<String, Object>) openAITool.get("function");
            Map<String, Object> params =
                    (Map<String, Object>) fn.getOrDefault("parameters", Collections.emptyMap());

            Map<String, Object> parameters = new LinkedHashMap<>();
            parameters.put("type", "object");
            parameters.put("properties", params.getOrDefault("properties", Collections.emptyMap()));
            parameters.put("required", params.getOrDefault("required", Collections.emptyList()));

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("type", "function");
            result.put("name", fn.get("name"));
            result.put("description", fn.getOrDefault("description", ""));
            result.put("strict", fn.getOrDefault("strict", false));
            result.put("parameters", parameters);
            return result;
        }

        Map<String, Object> custom = (Map<String, Object>) openAITool.get("custom");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", "function");
        result.put("name", custom.get("name"));
        result.put("description", custom.getOrDefault("description", ""));
        result.put("strict", false);
        result.put(
                "parameters",
                Map.of(
                        "type", "object",
                        "properties", Map.of("text", Map.of("type", "string")),
                        "required", List.of("text")));
        return result;
    }

    /** 替换描述中的模板占位符 */
    public static String replacer(String description, SystemPromptContext context) {
        if (description == null) {
            return "";
        }

        int width = 900;
        int height = 600;
        if (context.getBrowserSettings() != null
                && context.getBrowserSettings().getViewport() != null) {
            width = context.getBrowserSettings().getViewport().getWidth();
            height = context.getBrowserSettings().getViewport().getHeight();
        }

        String cwd = context.getCwd() != null ? context.getCwd() : System.getProperty("user.dir");
        String multiRootHint =
                Boolean.TRUE.equals(context.getIsMultiRootEnabled()) ? MULTI_ROOT_HINT : "";

        return description
                .replace("{{BROWSER_VIEWPORT_WIDTH}}", String.valueOf(width))
                .replace("{{BROWSER_VIEWPORT_HEIGHT}}", String.valueOf(height))
                .replace("{{CWD}}", cwd)
                .replace("{{MULTI_ROOT_HINT}}", multiRootHint);
    }

    /** 解析参数的 instruction（可能是字符串或函数） */
    public static String resolveInstruction(
            ClineToolSpec.ClineToolSpecParameter param, SystemPromptContext context) {
        if (param.getInstructionFn() != null) {
            return param.getInstructionFn().apply(context);
        }
        return param.getInstruction();
    }

    /** 根据提供商 ID 获取对应的原生工具转换器 */
    public static Function<ToolConversionInput, Map<String, Object>> getNativeConverter(
            String providerId) {
        if (providerId == null) {
            return input -> toolSpecFunctionDefinition(input.tool(), input.context());
        }
        return switch (providerId.toLowerCase()) {
            case "anthropic" -> input -> toolSpecInputSchema(input.tool(), input.context());
            case "google", "gemini" ->
                    input -> toolSpecFunctionDeclarations(input.tool(), input.context());
            default -> input -> toolSpecFunctionDefinition(input.tool(), input.context());
        };
    }

    /** 工具转换输入 */
    public record ToolConversionInput(ClineToolSpec tool, SystemPromptContext context) {}

    // ========== 私有辅助方法 ==========

    private static void validateContextRequirements(
            ClineToolSpec tool, SystemPromptContext context) {
        if (tool.getContextRequirements() != null
                && !tool.getContextRequirements().apply(context)) {
            throw new IllegalStateException(
                    "Tool %s does not meet context requirements".formatted(tool.getName()));
        }
    }

    private static void buildParameterSchemas(
            ClineToolSpec tool,
            SystemPromptContext context,
            Map<String, Object> properties,
            List<String> required) {
        if (tool.getParameters() == null) {
            return;
        }

        for (ClineToolSpec.ClineToolSpecParameter param : tool.getParameters()) {
            if (param.getContextRequirements() != null
                    && !param.getContextRequirements().apply(context)) {
                continue;
            }
            if (param.isRequired()) {
                required.add(param.getName());
            }

            String paramType = param.getType() != null ? param.getType() : "string";

            Map<String, Object> paramSchema = new LinkedHashMap<>();
            paramSchema.put("type", paramType);
            paramSchema.put("description", replacer(resolveInstruction(param, context), context));

            if ("array".equals(paramType) && param.getItems() != null) {
                paramSchema.put("items", param.getItems());
            }
            if ("object".equals(paramType) && param.getProperties() != null) {
                paramSchema.put("properties", param.getProperties());
            }

            properties.put(param.getName(), paramSchema);
        }
    }
}
