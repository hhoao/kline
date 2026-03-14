package com.hhoa.kline.core.core.controller;

import com.hhoa.kline.core.core.api.TaskContext;
import com.hhoa.kline.core.core.api.TaskContextHolder;
import com.hhoa.kline.core.core.context.management.ContextManager;
import com.hhoa.kline.core.core.context.management.LocalContextManager;
import com.hhoa.kline.core.core.prompts.ResponseFormatter;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptService;
import com.hhoa.kline.core.core.services.mcp.DefaultMcpHub;
import com.hhoa.kline.core.core.services.mcp.IMcpHub;
import com.hhoa.kline.core.core.services.mcp.IMcpHubInitializer;
import com.hhoa.kline.core.core.storage.FileBasedStorageContext;
import com.hhoa.kline.core.core.storage.LocalStateManager;
import com.hhoa.kline.core.core.storage.StateManager;
import com.hhoa.kline.core.core.task.ApiHandler;
import com.hhoa.kline.core.core.task.ContextFactory;
import com.hhoa.kline.core.core.task.focuschain.FocusChainManagerFactory;
import com.hhoa.kline.core.core.task.focuschain.LocalFocusChainManagerFactory;
import com.hhoa.kline.core.core.task.tools.ToolExecutor;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import reactor.util.context.Context;
import reactor.util.context.ContextView;

/**
 * TaskManagerFactory
 *
 * @author xianxing
 * @since 2025/11/25
 */
public class LocalTaskManagerFactory implements TaskManagerFactory {
    private final ApiHandler apiHandler;
    private final SystemPromptService systemPromptService;
    private final Map<Long, TaskManager> taskManagers = new HashMap<>();
    private final Supplier<Path> basePathSupplier;
    private final IMcpHubInitializer mcpHubInitializer;

    public LocalTaskManagerFactory(
            ApiHandler apiHandler,
            SystemPromptService systemPromptService,
            Supplier<Path> basePathSupplier,
            IMcpHubInitializer mcpHubInitializer) {
        this.apiHandler = apiHandler;
        this.systemPromptService = systemPromptService;
        this.basePathSupplier = basePathSupplier;
        this.mcpHubInitializer = mcpHubInitializer;
    }

    public TaskManager getOrCreateTaskManager() {
        try {
            TaskContext taskContext = TaskContextHolder.get();
            if (taskContext == null) {
                throw new IllegalStateException(
                        "TaskContext not set; ensure web layer sets TaskContextHolder before creating TaskManager");
            }
            Long taskManagerId = taskContext.getTaskManagerId();
            Path basePath = basePathSupplier.get();

            if (!taskManagers.containsKey(taskManagerId)) {
                List<String> workspaceRoots =
                        List.of(Path.of(basePath.toString(), "workspace").toString());
                String globalStoragePath = Path.of(basePath.toString(), "global").toString();

                FileBasedStorageContext fileBasedStorageContext =
                        new FileBasedStorageContext(globalStoragePath, workspaceRoots);

                String stateDirectory = getGlobalStorageDir(globalStoragePath, "state");
                String settingsDirectory = getGlobalStorageDir(globalStoragePath, "settings");
                String cacheDirectory = getGlobalStorageDir(globalStoragePath, "cache");
                String tasksDirectory = getGlobalStorageDir(globalStoragePath, "tasks");

                Path clineWorkflowsDir = Paths.get(settingsDirectory, "Cline", "Workflows");
                Files.createDirectories(clineWorkflowsDir);
                String clineWorkflowsDirectory = clineWorkflowsDir.toString();

                Path clineRulesDir = Paths.get(settingsDirectory, "Cline", "Rules");
                Files.createDirectories(clineRulesDir);
                String clineRulesDirectory = clineRulesDir.toString();

                ContextManager contextManager = new LocalContextManager(tasksDirectory);
                StateManager stateManager =
                        new LocalStateManager(
                                fileBasedStorageContext,
                                tasksDirectory,
                                stateDirectory,
                                clineWorkflowsDirectory,
                                clineRulesDirectory,
                                settingsDirectory,
                                cacheDirectory);
                FocusChainManagerFactory focusChainManagerFactory =
                        new LocalFocusChainManagerFactory(stateManager, tasksDirectory);
                ToolExecutor toolExecutor = new ToolExecutor(null, new ResponseFormatter());
                IMcpHub mcpHub = new DefaultMcpHub(stateManager);
                if (mcpHubInitializer != null) {
                    mcpHubInitializer.initialize(mcpHub);
                }
                ContextFactory contextFactory =
                        new ContextFactory() {
                            @Override
                            public Context modifyContext(Context context) {
                                return context.put("customContext", taskContext);
                            }

                            @Override
                            public void runWithContext(ContextView context, Runnable runnable) {
                                TaskContext taskContext = context.get("customContext");
                                taskContext.run(runnable);
                            }
                        };
                taskManagers.put(
                        taskManagerId,
                        new TaskManager(
                                mcpHub,
                                stateManager,
                                systemPromptService,
                                contextManager,
                                toolExecutor,
                                apiHandler,
                                focusChainManagerFactory,
                                contextFactory));
            }
            return taskManagers.get(taskManagerId);
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to get or create task manager: " + e.getMessage(), e);
        }
    }

    /**
     * 获取全局存储目录
     *
     * @param subdirs 子目录
     * @return 全局存储目录路径
     */
    private String getGlobalStorageDir(String globalStoragePath, String... subdirs)
            throws IOException {
        Path fullPath = Paths.get(globalStoragePath, subdirs);
        Files.createDirectories(fullPath);
        return fullPath.toString();
    }
}
