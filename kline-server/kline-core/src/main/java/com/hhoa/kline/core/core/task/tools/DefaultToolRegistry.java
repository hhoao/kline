package com.hhoa.kline.core.core.task.tools;

import com.hhoa.kline.core.core.prompts.systemprompt.ClineToolSpec;
import com.hhoa.kline.core.core.task.tools.handlers.AccessMcpResourceHandler;
import com.hhoa.kline.core.core.task.tools.handlers.ActModeRespondToolHandler;
import com.hhoa.kline.core.core.task.tools.handlers.ApplyPatchHandler;
import com.hhoa.kline.core.core.task.tools.handlers.AskFollowupQuestionToolHandler;
import com.hhoa.kline.core.core.task.tools.handlers.AttemptCompletionHandler;
import com.hhoa.kline.core.core.task.tools.handlers.BrowserToolHandler;
import com.hhoa.kline.core.core.task.tools.handlers.CondenseHandler;
import com.hhoa.kline.core.core.task.tools.handlers.ExecuteCommandToolHandler;
import com.hhoa.kline.core.core.task.tools.handlers.GenerateExplanationToolHandler;
import com.hhoa.kline.core.core.task.tools.handlers.ListCodeDefinitionNamesToolHandler;
import com.hhoa.kline.core.core.task.tools.handlers.ListFilesToolHandler;
import com.hhoa.kline.core.core.task.tools.handlers.LoadMcpDocumentationHandler;
import com.hhoa.kline.core.core.task.tools.handlers.NewTaskHandler;
import com.hhoa.kline.core.core.task.tools.handlers.PlanModeRespondHandler;
import com.hhoa.kline.core.core.task.tools.handlers.ReadFileToolHandler;
import com.hhoa.kline.core.core.task.tools.handlers.ReportBugHandler;
import com.hhoa.kline.core.core.task.tools.handlers.SearchFilesToolHandler;
import com.hhoa.kline.core.core.task.tools.handlers.SubagentToolHandler;
import com.hhoa.kline.core.core.task.tools.handlers.SummarizeTaskHandler;
import com.hhoa.kline.core.core.task.tools.handlers.ToolHandler;
import com.hhoa.kline.core.core.task.tools.handlers.UseMcpToolHandler;
import com.hhoa.kline.core.core.task.tools.handlers.UseSkillToolHandler;
import com.hhoa.kline.core.core.task.tools.handlers.WebFetchToolHandler;
import com.hhoa.kline.core.core.task.tools.handlers.WebSearchToolHandler;
import com.hhoa.kline.core.core.task.tools.handlers.WriteToFileToolHandler;
import com.hhoa.kline.core.core.task.tools.subagent.AgentBaseConfig;
import com.hhoa.kline.core.core.task.tools.subagent.AgentConfigLoader;
import com.hhoa.kline.core.enums.ClineDefaultTool;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultToolRegistry implements ToolRegistry {
    /** 与 Cline {@code shared/mcp.ts} 中 {@code CLINE_MCP_TOOL_IDENTIFIER} 一致。 */
    private static final String CLINE_MCP_TOOL_IDENTIFIER = "0mcp0";

    private final Map<String, ToolHandler> nameToHandler = new HashMap<>();
    private final Map<String, ToolHandler> dynamicSubagentHandlers = new ConcurrentHashMap<>();

    public DefaultToolRegistry() {
        AgentConfigLoader.getInstance();
        WriteToFileToolHandler writeHandler = new WriteToFileToolHandler();
        this.register(new ListFilesToolHandler());
        this.register(new ReadFileToolHandler());
        this.register(new AskFollowupQuestionToolHandler());
        this.register(new WebFetchToolHandler());
        this.register(writeHandler);
        this.register(new SharedToolHandler(ClineDefaultTool.FILE_EDIT, writeHandler));
        this.register(new SharedToolHandler(ClineDefaultTool.NEW_RULE, writeHandler));
        this.register(new ListCodeDefinitionNamesToolHandler());
        this.register(new SearchFilesToolHandler());
        this.register(new ExecuteCommandToolHandler());
        this.register(new UseMcpToolHandler());
        this.register(new AccessMcpResourceHandler());
        this.register(new LoadMcpDocumentationHandler());
        this.register(new PlanModeRespondHandler());
        this.register(new NewTaskHandler());
        this.register(new AttemptCompletionHandler());
        this.register(new CondenseHandler());
        this.register(new SummarizeTaskHandler());
        this.register(new ReportBugHandler());
        this.register(new ActModeRespondToolHandler());
        this.register(new UseSkillToolHandler());
        this.register(new WebSearchToolHandler());
        this.register(new SubagentToolHandler());
        this.register(new GenerateExplanationToolHandler());
        this.register(new ApplyPatchHandler());
        this.register(new BrowserToolHandler());
    }

    @Override
    public ToolHandler getHandler(String toolName) {
        // Normalize MCP tool names (e.g. "server0mcp0toolName") to the standard handler
        if (toolName != null && toolName.contains(CLINE_MCP_TOOL_IDENTIFIER)) {
            toolName = ClineDefaultTool.MCP_USE.getValue();
        }

        ToolHandler handler = nameToHandler.get(toolName);
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

    /** 动态子代理在提示里使用单参数 {@code prompt}，执行前校验必须与之一致（见 Cline 动态工具规格）。 */
    private static ClineToolSpec executorSpecForDynamicSubagentTool(String toolName) {
        AgentConfigLoader loader = AgentConfigLoader.getInstance();
        String norm = loader.getNormalizedAgentNameForTool(toolName);
        AgentBaseConfig cfg = norm != null ? loader.getAllCachedConfigs().get(norm) : null;
        if (cfg == null) {
            return null;
        }
        ClineToolSpec.ClineToolSpecParameter promptParam =
                ClineToolSpec.ClineToolSpecParameter.builder()
                        .name("prompt")
                        .required(true)
                        .instruction(
                                "Helpful instruction for the task that the subagent will perform.")
                        .build();
        return ClineToolSpec.builder()
                .id(ClineDefaultTool.USE_SUBAGENTS.getValue())
                .name(toolName)
                .description(
                        String.format("Use the \"%s\" subagent: %s", cfg.name(), cfg.description()))
                .parameters(List.of(promptParam))
                .build();
    }

    @Override
    public boolean has(String toolName) {
        if (nameToHandler.containsKey(toolName)) {
            return true;
        }
        return AgentConfigLoader.getInstance().isDynamicSubagentTool(toolName);
    }

    public DefaultToolRegistry register(ToolHandler handler) {
        if (handler != null && handler.getName() != null) {
            nameToHandler.put(handler.getName(), handler);
        }
        return this;
    }
}
