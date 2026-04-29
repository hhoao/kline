package com.hhoa.kline.core.core.tools;

import com.hhoa.kline.core.core.assistant.ToolUse;
import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptContext;
import com.hhoa.kline.core.core.tools.args.CliSubagentsInput;
import com.hhoa.kline.core.core.tools.args.FocusChainInput;
import com.hhoa.kline.core.core.tools.args.ReplaceInFileInput;
import com.hhoa.kline.core.core.tools.handlers.AccessMcpResourceHandler;
import com.hhoa.kline.core.core.tools.handlers.ActModeRespondToolHandler;
import com.hhoa.kline.core.core.tools.handlers.ApplyPatchHandler;
import com.hhoa.kline.core.core.tools.handlers.AskFollowupQuestionToolHandler;
import com.hhoa.kline.core.core.tools.handlers.AttemptCompletionHandler;
import com.hhoa.kline.core.core.tools.handlers.BrowserToolHandler;
import com.hhoa.kline.core.core.tools.handlers.CondenseHandler;
import com.hhoa.kline.core.core.tools.handlers.ExecuteCommandToolHandler;
import com.hhoa.kline.core.core.tools.handlers.GenerateExplanationToolHandler;
import com.hhoa.kline.core.core.tools.handlers.ListCodeDefinitionNamesToolHandler;
import com.hhoa.kline.core.core.tools.handlers.ListFilesToolHandler;
import com.hhoa.kline.core.core.tools.handlers.LoadMcpDocumentationHandler;
import com.hhoa.kline.core.core.tools.handlers.NewTaskHandler;
import com.hhoa.kline.core.core.tools.handlers.PlanModeRespondHandler;
import com.hhoa.kline.core.core.tools.handlers.ReadFileToolHandler;
import com.hhoa.kline.core.core.tools.handlers.ReportBugHandler;
import com.hhoa.kline.core.core.tools.handlers.SearchFilesToolHandler;
import com.hhoa.kline.core.core.tools.handlers.SubagentToolHandler;
import com.hhoa.kline.core.core.tools.handlers.SummarizeTaskHandler;
import com.hhoa.kline.core.core.tools.handlers.ToolHandler;
import com.hhoa.kline.core.core.tools.handlers.UseMcpToolHandler;
import com.hhoa.kline.core.core.tools.handlers.UseSkillToolHandler;
import com.hhoa.kline.core.core.tools.handlers.WebFetchToolHandler;
import com.hhoa.kline.core.core.tools.handlers.WebSearchToolHandler;
import com.hhoa.kline.core.core.tools.handlers.WriteToFileToolHandler;
import com.hhoa.kline.core.core.tools.specs.AccessMcpResourceTool;
import com.hhoa.kline.core.core.tools.specs.ActModeRespondTool;
import com.hhoa.kline.core.core.tools.specs.ApplyPatchTool;
import com.hhoa.kline.core.core.tools.specs.AskFollowupQuestionTool;
import com.hhoa.kline.core.core.tools.specs.AttemptCompletionTool;
import com.hhoa.kline.core.core.tools.specs.BrowserActionTool;
import com.hhoa.kline.core.core.tools.specs.CliSubagentsTool;
import com.hhoa.kline.core.core.tools.specs.ExecuteCommandToolSpec;
import com.hhoa.kline.core.core.tools.specs.FocusChainTool;
import com.hhoa.kline.core.core.tools.specs.GenerateExplanationTool;
import com.hhoa.kline.core.core.tools.specs.ListCodeDefinitionNamesTool;
import com.hhoa.kline.core.core.tools.specs.ListFilesTool;
import com.hhoa.kline.core.core.tools.specs.LoadMcpDocumentationTool;
import com.hhoa.kline.core.core.tools.specs.NewTaskTool;
import com.hhoa.kline.core.core.tools.specs.PlanModeRespondTool;
import com.hhoa.kline.core.core.tools.specs.ReadFileTool;
import com.hhoa.kline.core.core.tools.specs.ReplaceInFileTool;
import com.hhoa.kline.core.core.tools.specs.SearchFilesTool;
import com.hhoa.kline.core.core.tools.specs.SubagentTool;
import com.hhoa.kline.core.core.tools.specs.UseMcpToolTool;
import com.hhoa.kline.core.core.tools.specs.UseSkillTool;
import com.hhoa.kline.core.core.tools.specs.WebFetchTool;
import com.hhoa.kline.core.core.tools.specs.WebSearchTool;
import com.hhoa.kline.core.core.tools.specs.WriteToFileTool;
import com.hhoa.kline.core.core.tools.subagent.AgentBaseConfig;
import com.hhoa.kline.core.core.tools.subagent.AgentConfigLoader;
import com.hhoa.kline.core.core.tools.types.ToolContext;
import com.hhoa.kline.core.core.tools.types.ToolExecuteResult;
import com.hhoa.kline.core.enums.ClineDefaultTool;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class DefaultToolRegistry implements ToolRegistry {
    /** 与 Cline {@code shared/mcp.ts} 中 {@code CLINE_MCP_TOOL_IDENTIFIER} 一致。 */
    private static final String CLINE_MCP_TOOL_IDENTIFIER = "0mcp0";

    private static final String PLAN_MODE_RESPOND_ID = ClineDefaultTool.PLAN_MODE.getValue();

    private final Map<String, ToolHandler<?>> nameToHandler = new LinkedHashMap<>();
    private final Map<String, ToolSpecBinding<?, ?>> nameToSpecBinding = new LinkedHashMap<>();
    private final Map<String, ToolHandler<?>> dynamicSubagentHandlers = new ConcurrentHashMap<>();
    private final List<ToolSpec> additionalSpecs = new ArrayList<>();
    private final PlanModeRespondHandler planModeRespondHandler = new PlanModeRespondHandler();

    public DefaultToolRegistry() {
        AgentConfigLoader.getInstance();

        WriteToFileToolHandler writeHandler = new WriteToFileToolHandler();
        register(new AccessMcpResourceTool(), new AccessMcpResourceHandler());
        register(new ActModeRespondTool(), new ActModeRespondToolHandler());
        register(new ApplyPatchTool(), new ApplyPatchHandler());
        register(new AskFollowupQuestionTool(), new AskFollowupQuestionToolHandler());
        register(new AttemptCompletionTool(), new AttemptCompletionHandler());
        register(new BrowserActionTool(), new BrowserToolHandler());
        registerSpec(new CliSubagentsTool(), cliSubagentsSpecHandler());
        register(new ExecuteCommandToolSpec(), new ExecuteCommandToolHandler());
        registerSpec(new FocusChainTool(), focusChainSpecHandler());
        register(new GenerateExplanationTool(), new GenerateExplanationToolHandler());
        register(new ListCodeDefinitionNamesTool(), new ListCodeDefinitionNamesToolHandler());
        register(new ListFilesTool(), new ListFilesToolHandler());
        register(new LoadMcpDocumentationTool(), new LoadMcpDocumentationHandler());
        register(new NewTaskTool(), new NewTaskHandler());
        register(new PlanModeRespondTool(), planModeRespondHandler);
        register(new ReadFileTool(), new ReadFileToolHandler());
        register(
                new ReplaceInFileTool(),
                new SharedToolHandler(ClineDefaultTool.FILE_EDIT, writeHandler) {
                    public void handlePartialBlock(
                            ReplaceInFileInput input, ToolContext context, ToolUse block) {
                        writeHandler.handleReplaceInFilePartialBlock(input, context, block);
                    }

                    public ToolExecuteResult execute(
                            ReplaceInFileInput input, ToolContext context, ToolUse block) {
                        return writeHandler.executeReplaceInFile(input, context, block);
                    }
                });
        register(new SearchFilesTool(), new SearchFilesToolHandler());
        register(new SubagentTool(), new SubagentToolHandler());
        register(new UseSkillTool(), new UseSkillToolHandler());
        register(new UseMcpToolTool(), new UseMcpToolHandler());
        register(new WebFetchTool(), new WebFetchToolHandler());
        register(new WebSearchTool(), new WebSearchToolHandler());
        register(new WriteToFileTool(), writeHandler);

        register(
                ClineDefaultTool.NEW_RULE.getValue(),
                new SharedToolHandler(ClineDefaultTool.NEW_RULE, writeHandler));
        register(ClineDefaultTool.CONDENSE.getValue(), new CondenseHandler());
        register(ClineDefaultTool.SUMMARIZE_TASK.getValue(), new SummarizeTaskHandler());
        register(ClineDefaultTool.REPORT_BUG.getValue(), new ReportBugHandler());
    }

    @Override
    public ToolHandler getHandler(String toolName) {
        toolName = normalizeToolName(toolName);

        ToolHandler<?> handler = nameToHandler.get(toolName);
        if (handler != null) {
            return handler;
        }
        if (AgentConfigLoader.getInstance().isDynamicSubagentTool(toolName)) {
            return dynamicSubagentHandlers.computeIfAbsent(
                    toolName,
                    k ->
                            new DelegatingToolHandler(
                                    k,
                                    nameToHandler.get(ClineDefaultTool.USE_SUBAGENTS.getValue()),
                                    executorSpecForDynamicSubagentTool(k)));
        }
        return null;
    }

    @Override
    public ToolSpec getSpec(String toolName, ModelFamily family) {
        toolName = normalizeToolName(toolName);
        if (AgentConfigLoader.getInstance().isDynamicSubagentTool(toolName)) {
            return executorSpecForDynamicSubagentTool(toolName);
        }
        ToolSpecBinding<?, ?> binding = nameToSpecBinding.get(toolName);
        if (binding == null) {
            return null;
        }
        return binding.resolve(family != null ? family : ModelFamily.GENERIC);
    }

    @Override
    public List<ToolSpec> getToolSpecs(ModelFamily variant, SystemPromptContext context) {
        ModelFamily family = variant != null ? variant : ModelFamily.GENERIC;
        List<ToolSpec> list = new ArrayList<>(getTools(family));
        list.removeIf(spec -> PLAN_MODE_RESPOND_ID.equals(spec.getId()));
        ToolSpec planModeSpec = resolvePlanModeSpec(family);
        if (planModeSpec != null) {
            list.add(planModeSpec);
        }
        list.sort(Comparator.comparing(s -> s.getId() != null ? s.getId() : "", String::compareTo));
        return list;
    }

    public ToolSpec register(ToolSpec spec) {
        if (spec != null) {
            additionalSpecs.add(spec);
        }
        return spec;
    }

    public List<ToolSpec> getTools(ModelFamily variant) {
        ModelFamily family = variant != null ? variant : ModelFamily.GENERIC;
        List<ToolSpec> tools = resolveToolsForFamily(family);
        if (tools.isEmpty() && family != ModelFamily.GENERIC) {
            return resolveToolsForFamily(ModelFamily.GENERIC);
        }
        return tools;
    }

    public List<String> getRegisteredModelIds() {
        List<String> ids = new ArrayList<>();
        for (ModelFamily family : ModelFamily.values()) {
            if (!resolveToolsForFamily(family).isEmpty()) {
                ids.add(family.name());
            }
        }
        return ids;
    }

    public ToolSpec getToolByName(String toolName, ModelFamily variant) {
        List<ToolSpec> tools = getTools(variant);
        if (tools.isEmpty()) {
            return null;
        }
        return tools.stream()
                .filter(tool -> toolName.equals(tool.getId()))
                .findFirst()
                .orElse(null);
    }

    public ToolSpec getToolByNameWithFallback(String toolName, ModelFamily variant) {
        ToolSpec exact = getToolByName(toolName, variant);
        if (exact != null) {
            return exact;
        }
        ToolSpec generic = getToolByName(toolName, ModelFamily.GENERIC);
        if (generic != null) {
            return generic;
        }
        for (ModelFamily family : EnumSet.allOf(ModelFamily.class)) {
            if (family != variant && family != ModelFamily.GENERIC) {
                ToolSpec found = getToolByName(toolName, family);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    @Override
    public List<ToolSpec> getToolsForVariantWithFallback(
            ModelFamily variant, List<String> requestedIds, SystemPromptContext context) {
        List<ToolSpec> resolved = new ArrayList<>();
        for (String id : requestedIds) {
            ToolSpec tool;
            if (PLAN_MODE_RESPOND_ID.equals(id) && context != null) {
                tool = resolvePlanModeSpec(variant != null ? variant : ModelFamily.GENERIC);
            } else {
                tool = getToolByNameWithFallback(id, variant);
            }
            if (tool != null && resolved.stream().noneMatch(t -> id.equals(t.getId()))) {
                resolved.add(tool);
            }
        }
        return resolved;
    }

    @Override
    public List<ToolSpec> getEnabledTools(ModelFamily variant, SystemPromptContext context) {
        List<ToolSpec> allTools = getToolSpecs(variant, context);
        return allTools.stream()
                .filter(
                        tool -> {
                            if (tool.getContextRequirements() != null) {
                                try {
                                    return Boolean.TRUE.equals(
                                            tool.getContextRequirements().apply(context));
                                } catch (Exception e) {
                                    return false;
                                }
                            }
                            return true;
                        })
                .toList();
    }

    @Override
    public List<Map<String, Object>> getNativeTools(
            ModelFamily variant,
            SystemPromptContext context,
            Function<ToolSpecConverter.ToolConversionInput, Map<String, Object>> converter) {
        List<ToolSpec> enabledTools = getEnabledTools(variant, context);
        List<Map<String, Object>> result = new ArrayList<>();
        for (ToolSpec tool : enabledTools) {
            try {
                result.add(
                        converter.apply(new ToolSpecConverter.ToolConversionInput(tool, context)));
            } catch (Exception e) {
                // 跳过不满足上下文要求的工具
            }
        }
        return result;
    }

    @Override
    public boolean has(String toolName) {
        String normalized = normalizeToolName(toolName);
        if (nameToHandler.containsKey(normalized)) {
            return true;
        }
        return AgentConfigLoader.getInstance().isDynamicSubagentTool(normalized);
    }

    public DefaultToolRegistry register(String name, ToolHandler<?> handler) {
        if (name != null && handler != null) {
            nameToHandler.put(name, handler);
        }
        return this;
    }

    public <I, H extends ToolHandler<?>> DefaultToolRegistry register(
            ToolSpecProvider<I, H> specProvider, H handler) {
        registerSpec(specProvider, handler);
        register(specProvider.name(), handler);
        return this;
    }

    public <I, H extends ToolHandler<?>> DefaultToolRegistry register(
            H handler, ToolSpecProvider<I, H> specProvider) {
        return register(specProvider, handler);
    }

    public <I, H extends ToolHandler<?>> DefaultToolRegistry registerSpec(
            ToolSpecProvider<I, H> specProvider, H handler) {
        if (handler == null || specProvider == null) {
            return this;
        }
        ToolSpecResolver.resolve(specProvider, ModelFamily.GENERIC, handler);
        nameToSpecBinding.put(specProvider.name(), new ToolSpecBinding<>(specProvider, handler));
        return this;
    }

    private List<ToolSpec> resolveToolsForFamily(ModelFamily family) {
        Set<ToolSpec> specs = new LinkedHashSet<>();
        for (ToolSpecBinding<?, ?> binding : nameToSpecBinding.values()) {
            ToolSpec spec = binding.resolve(family);
            if (spec != null) {
                specs.add(spec);
            }
        }
        for (ToolSpec spec : additionalSpecs) {
            ModelFamily specFamily =
                    spec.getVariant() != null ? spec.getVariant() : ModelFamily.GENERIC;
            if (specFamily == family) {
                specs.add(spec);
            }
        }
        return new ArrayList<>(specs);
    }

    private ToolSpec resolvePlanModeSpec(ModelFamily variant) {
        return ToolSpecResolver.resolve(
                new PlanModeRespondTool(),
                variant != null ? variant : ModelFamily.GENERIC,
                planModeRespondHandler);
    }

    private String normalizeToolName(String toolName) {
        if (toolName != null && toolName.contains(CLINE_MCP_TOOL_IDENTIFIER)) {
            return ClineDefaultTool.MCP_USE.getValue();
        }
        return toolName;
    }

    /** 动态子代理在提示里使用单参数 {@code prompt}，执行前校验必须与之一致（见 Cline 动态工具规格）。 */
    private static ToolSpec executorSpecForDynamicSubagentTool(String toolName) {
        AgentConfigLoader loader = AgentConfigLoader.getInstance();
        String norm = loader.getNormalizedAgentNameForTool(toolName);
        AgentBaseConfig cfg = norm != null ? loader.getAllCachedConfigs().get(norm) : null;
        if (cfg == null) {
            return null;
        }
        Map<String, Object> inputSchema = ToolSchema.objectSchema();
        ToolSchema.putProperty(
                inputSchema,
                "prompt",
                ToolSchema.stringProperty(
                        "Helpful instruction for the task that the subagent will perform."));
        ToolSchema.require(inputSchema, "prompt");
        return ToolSpec.builder()
                .id(ClineDefaultTool.USE_SUBAGENTS.getValue())
                .name(toolName)
                .description(
                        String.format("Use the \"%s\" subagent: %s", cfg.name(), cfg.description()))
                .inputSchema(inputSchema)
                .build();
    }

    private static ToolHandler<CliSubagentsInput> cliSubagentsSpecHandler() {
        return new ToolHandler<>() {
            @Override
            public String getDescription(ToolUse block) {
                return "[cli_subagents]";
            }

            @Override
            public void handlePartialBlock(
                    CliSubagentsInput input, ToolContext context, ToolUse block) {}

            @Override
            public ToolExecuteResult execute(
                    CliSubagentsInput input, ToolContext context, ToolUse block) {
                return new ToolExecuteResult.Immediate(List.of());
            }
        };
    }

    private static ToolHandler<FocusChainInput> focusChainSpecHandler() {
        return new ToolHandler<>() {
            @Override
            public String getDescription(ToolUse block) {
                return "[" + ClineDefaultTool.TODO.getValue() + "]";
            }

            @Override
            public void handlePartialBlock(
                    FocusChainInput input, ToolContext context, ToolUse block) {}

            @Override
            public ToolExecuteResult execute(
                    FocusChainInput input, ToolContext context, ToolUse block) {
                return new ToolExecuteResult.Immediate(List.of());
            }
        };
    }

    private record ToolSpecBinding<I, H extends ToolHandler<?>>(
            ToolSpecProvider<I, H> provider, H handler) {
        private ToolSpec resolve(ModelFamily family) {
            return ToolSpecResolver.resolve(provider, family, handler);
        }
    }
}
