package com.hhoa.kline.core.core.task;

import com.hhoa.kline.core.core.api.TaskContext;
import com.hhoa.kline.core.core.context.management.ContextManager;
import com.hhoa.kline.core.core.controller.HistoryItem;
import com.hhoa.kline.core.core.integrations.notifications.NotificationService;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptService;
import com.hhoa.kline.core.core.services.mcp.IMcpHub;
import com.hhoa.kline.core.core.storage.StateManager;
import com.hhoa.kline.core.core.task.focuschain.FocusChainManagerFactory;
import com.hhoa.kline.core.core.task.tools.ToolExecutor;
import com.hhoa.kline.core.core.workspace.WorkspaceRootManager;
import com.hhoa.kline.core.subscription.MessageSender;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TaskParams {
    private TaskContext taskContext;

    private String taskId;

    private String cwd;

    private IMcpHub mcpHub;

    private Runnable postStateToWebview;

    private Consumer<String> reinitExistingTaskFromId;

    private Runnable cancelTask;

    private BiConsumer<Boolean, String> updateBackgroundCommandState;

    private Supplier<Boolean> shouldShowBackgroundTerminalSuggestion;

    @Builder.Default private int shellIntegrationTimeout = 180000;

    @Builder.Default private int askResponseTimeout = 120000;

    @Builder.Default private boolean terminalReuseEnabled = true;

    @Builder.Default private int terminalOutputLineLimit = 500;

    private String defaultTerminalProfile;

    private StateManager stateManager;

    private WorkspaceRootManager workspaceManager;

    private String initialTaskText;

    private List<String> images;

    private List<String> files;

    private HistoryItem historyItem;

    @Builder.Default private boolean taskLockAcquired = false;

    private ToolExecutor toolExecutor;

    private FocusChainManagerFactory focusChainManagerFactory;

    private String ulid;

    private ApiHandler apiHandler;

    private SystemPromptService systemPromptService;

    private ContextManager contextManager;

    private NotificationService notificationService;

    @Builder.Default private String mode = "act";

    private ContextFactory contextFactory;

    private MessageSender messageSender;

    /**
     * 便捷构建器方法，用于创建基本的 TaskParams。
     *
     * @param systemPromptService System Prompt Service
     * @param cwd 当前工作目录
     * @param contextManager Context Manager
     * @param apiHandler API Handler
     * @param toolExecutor Tool Executor
     * @return TaskParamsBuilder
     */
    public static TaskParamsBuilder builder(
            SystemPromptService systemPromptService,
            String cwd,
            ContextManager contextManager,
            ApiHandler apiHandler,
            ToolExecutor toolExecutor,
            TaskContext taskContext,
            ContextFactory contextFactory) {
        return new TaskParamsBuilder()
                .systemPromptService(systemPromptService)
                .cwd(cwd)
                .contextManager(contextManager)
                .apiHandler(apiHandler)
                .toolExecutor(toolExecutor)
                .contextFactory(contextFactory)
                .taskContext(taskContext);
    }
}
