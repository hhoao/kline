package com.hhoa.kline.core.core.task.tools;

import com.hhoa.kline.core.core.task.tools.handlers.AccessMcpResourceHandler;
import com.hhoa.kline.core.core.task.tools.handlers.AskFollowupQuestionToolHandler;
import com.hhoa.kline.core.core.task.tools.handlers.AttemptCompletionHandler;
import com.hhoa.kline.core.core.task.tools.handlers.CondenseHandler;
import com.hhoa.kline.core.core.task.tools.handlers.ExecuteCommandToolHandler;
import com.hhoa.kline.core.core.task.tools.handlers.ListCodeDefinitionNamesToolHandler;
import com.hhoa.kline.core.core.task.tools.handlers.ListFilesToolHandler;
import com.hhoa.kline.core.core.task.tools.handlers.LoadMcpDocumentationHandler;
import com.hhoa.kline.core.core.task.tools.handlers.NewTaskHandler;
import com.hhoa.kline.core.core.task.tools.handlers.PlanModeRespondHandler;
import com.hhoa.kline.core.core.task.tools.handlers.ReadFileToolHandler;
import com.hhoa.kline.core.core.task.tools.handlers.ReportBugHandler;
import com.hhoa.kline.core.core.task.tools.handlers.SearchFilesToolHandler;
import com.hhoa.kline.core.core.task.tools.handlers.SummarizeTaskHandler;
import com.hhoa.kline.core.core.task.tools.handlers.ToolHandler;
import com.hhoa.kline.core.core.task.tools.handlers.UseMcpToolHandler;
import com.hhoa.kline.core.core.task.tools.handlers.WebFetchToolHandler;
import com.hhoa.kline.core.core.task.tools.handlers.WriteToFileToolHandler;
import com.hhoa.kline.core.enums.ClineDefaultTool;
import java.util.HashMap;
import java.util.Map;

public class DefaultToolRegistry implements ToolRegistry {
    private final Map<String, ToolHandler> nameToHandler = new HashMap<>();

    public DefaultToolRegistry() {
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
    }

    @Override
    public ToolHandler getHandler(String toolName) {
        return nameToHandler.get(toolName);
    }

    @Override
    public boolean has(String toolName) {
        return nameToHandler.containsKey(toolName);
    }

    public DefaultToolRegistry register(ToolHandler handler) {
        if (handler != null && handler.getName() != null) {
            nameToHandler.put(handler.getName(), handler);
        }
        return this;
    }
}
