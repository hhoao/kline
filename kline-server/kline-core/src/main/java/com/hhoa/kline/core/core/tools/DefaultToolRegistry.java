package com.hhoa.kline.core.core.tools;

import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptContext;
import com.hhoa.kline.core.core.tools.handlers.ToolHandler;
import com.hhoa.kline.core.core.tools.specs.AccessMcpResourceTool;
import com.hhoa.kline.core.core.tools.specs.ActModeRespondTool;
import com.hhoa.kline.core.core.tools.specs.AgentTool;
import com.hhoa.kline.core.core.tools.specs.ApplyPatchTool;
import com.hhoa.kline.core.core.tools.specs.AskFollowupQuestionTool;
import com.hhoa.kline.core.core.tools.specs.AttemptCompletionTool;
import com.hhoa.kline.core.core.tools.specs.BrowserActionTool;
import com.hhoa.kline.core.core.tools.specs.CondenseTool;
import com.hhoa.kline.core.core.tools.specs.ExecuteCommandToolSpec;
import com.hhoa.kline.core.core.tools.specs.GenerateExplanationTool;
import com.hhoa.kline.core.core.tools.specs.ListCodeDefinitionNamesTool;
import com.hhoa.kline.core.core.tools.specs.ListFilesTool;
import com.hhoa.kline.core.core.tools.specs.LoadMcpDocumentationTool;
import com.hhoa.kline.core.core.tools.specs.NewRuleTool;
import com.hhoa.kline.core.core.tools.specs.NewTaskTool;
import com.hhoa.kline.core.core.tools.specs.PlanModeRespondTool;
import com.hhoa.kline.core.core.tools.specs.ReadFileTool;
import com.hhoa.kline.core.core.tools.specs.ReplaceInFileTool;
import com.hhoa.kline.core.core.tools.specs.ReportBugTool;
import com.hhoa.kline.core.core.tools.specs.SearchFilesTool;
import com.hhoa.kline.core.core.tools.specs.SummarizeTaskTool;
import com.hhoa.kline.core.core.tools.specs.TodoWriteTool;
import com.hhoa.kline.core.core.tools.specs.UseMcpToolTool;
import com.hhoa.kline.core.core.tools.specs.UseSkillTool;
import com.hhoa.kline.core.core.tools.specs.WebFetchTool;
import com.hhoa.kline.core.core.tools.specs.WebSearchTool;
import com.hhoa.kline.core.core.tools.specs.WriteToFileTool;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class DefaultToolRegistry implements ToolRegistry {
    private final Map<String, ToolHandler<?>> nameToHandler = new LinkedHashMap<>();
    private final Map<String, ToolSpecProvider<?>> nameToSpecProvider = new LinkedHashMap<>();

    public DefaultToolRegistry() {
        register(new AccessMcpResourceTool());
        register(new ActModeRespondTool());
        register(new AgentTool());
        register(new ApplyPatchTool());
        register(new AskFollowupQuestionTool());
        register(new AttemptCompletionTool());
        register(new BrowserActionTool());
        register(new ExecuteCommandToolSpec());
        register(new TodoWriteTool());
        register(new GenerateExplanationTool());
        register(new ListCodeDefinitionNamesTool());
        register(new ListFilesTool());
        register(new LoadMcpDocumentationTool());
        register(new NewTaskTool());
        register(new PlanModeRespondTool());
        register(new ReadFileTool());
        register(new ReplaceInFileTool());
        register(new SearchFilesTool());
        register(new UseSkillTool());
        register(new UseMcpToolTool());
        register(new WebFetchTool());
        register(new WebSearchTool());
        register(new WriteToFileTool());

        register(new NewRuleTool());
        register(new CondenseTool());
        register(new SummarizeTaskTool());
        register(new ReportBugTool());
    }

    @Override
    public ToolHandler<?> getToolHandler(String toolName) {
        return nameToHandler.get(toolName);
    }

    @Override
    public ToolSpec getToolSpec(String toolName, ModelFamily family) {
        ToolSpecProvider<?> provider = nameToSpecProvider.get(toolName);
        if (provider == null) {
            return null;
        }
        return ToolSpecResolver.resolve(provider, family != null ? family : ModelFamily.GENERIC);
    }


    @Override
    public List<ToolSpec> getToolSpecs(ModelFamily variant, SystemPromptContext context, Boolean enabled) {
        ModelFamily family = variant != null ? variant : ModelFamily.GENERIC;

        List<ToolSpec> specs = new ArrayList<>();
        for (ToolSpecProvider<?> provider : nameToSpecProvider.values()) {
            ToolSpec spec = ToolSpecResolver.resolve(provider, family);
            if (spec != null && (enabled == null || (enabled.equals(spec.getEnabled().apply(context))))) {
                specs.add(spec);
            }
        }
        return specs;
    }

    @Override
    public List<ToolSpec> getToolSpecs(
        ModelFamily variant, SystemPromptContext context, List<String> names, Boolean enabled) {
            List<ToolSpec> tools = getToolSpecs(variant, context, enabled);
        if (names.isEmpty()) {
            return tools;
        }
        return tools.stream()
            .filter(tool -> names.contains(tool.getName()))
            .toList();
    }

    @Override
    public <T> DefaultToolRegistry register(ToolSpecProvider<T> specProvider) {
        if (specProvider == null) {
            return this;
        }
        ToolSpecResolver.resolve(specProvider, ModelFamily.GENERIC);
        nameToSpecProvider.put(specProvider.name(), specProvider);
        nameToHandler.put(specProvider.name(), specProvider.handler(ModelFamily.GENERIC));

        return this;
    }

    @Override
    public boolean has(String toolName) {
        return nameToHandler.containsKey(toolName);
    }
}
