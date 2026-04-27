package com.hhoa.kline.core.core.task.handler;

import static com.hhoa.kline.core.core.shared.LanguageKey.getLanguageKey;

import com.hhoa.ai.kline.commons.utils.JsonUtils;
import com.hhoa.kline.core.core.assistant.ImageContentBlock;
import com.hhoa.kline.core.core.assistant.MessageParam;
import com.hhoa.kline.core.core.assistant.TextContentBlock;
import com.hhoa.kline.core.core.assistant.UserContentBlock;
import com.hhoa.kline.core.core.assistant.UserMessage;
import com.hhoa.kline.core.core.context.instructions.userinstructions.ClineRules;
import com.hhoa.kline.core.core.context.instructions.userinstructions.ExternalRules;
import com.hhoa.kline.core.core.context.instructions.userinstructions.RuleConditionals;
import com.hhoa.kline.core.core.context.instructions.userinstructions.RuleContextBuilder;
import com.hhoa.kline.core.core.context.instructions.userinstructions.RuleHelpers;
import com.hhoa.kline.core.core.context.instructions.userinstructions.SkillDiscovery;
import com.hhoa.kline.core.core.context.instructions.userinstructions.SkillMetadata;
import com.hhoa.kline.core.core.context.management.ContextManager;
import com.hhoa.kline.core.core.context.management.ContextWindowUtils;
import com.hhoa.kline.core.core.context.tracking.FileContextTracker;
import com.hhoa.kline.core.core.context.tracking.ModelContextTracker;
import com.hhoa.kline.core.core.ignore.ClineIgnoreController;
import com.hhoa.kline.core.core.integrations.checkpoints.CheckpointInitializer;
import com.hhoa.kline.core.core.integrations.checkpoints.ICheckpointManager;
import com.hhoa.kline.core.core.mentions.Mentions;
import com.hhoa.kline.core.core.prompts.ContextManagement;
import com.hhoa.kline.core.core.prompts.ResponseFormatter;
import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamilyMatchers;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptContext;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptService;
import com.hhoa.kline.core.core.services.mcp.IMcpHub;
import com.hhoa.kline.core.core.services.telemetry.TelemetryService;
import com.hhoa.kline.core.core.shared.ClineApiReqInfo;
import com.hhoa.kline.core.core.shared.ClineMessageType;
import com.hhoa.kline.core.core.shared.ClineSay;
import com.hhoa.kline.core.core.shared.FocusChainSettings;
import com.hhoa.kline.core.core.shared.LanguageKey;
import com.hhoa.kline.core.core.shared.api.ModelInfo;
import com.hhoa.kline.core.core.shared.proto.host.ShowMessageRequest;
import com.hhoa.kline.core.core.shared.proto.host.ShowMessageType;
import com.hhoa.kline.core.core.shared.remoteconfig.RemoteConfigSettings;
import com.hhoa.kline.core.core.shared.storage.GlobalState;
import com.hhoa.kline.core.core.shared.storage.LocalState;
import com.hhoa.kline.core.core.shared.storage.types.Mode;
import com.hhoa.kline.core.core.slashcommands.SlashCommandParser;
import com.hhoa.kline.core.core.storage.GlobalFileNames;
import com.hhoa.kline.core.core.storage.StateManager;
import com.hhoa.kline.core.core.task.ClineMessage;
import com.hhoa.kline.core.core.task.ContextFactory;
import com.hhoa.kline.core.core.task.LoadContextResult;
import com.hhoa.kline.core.core.task.MessageStateHandler;
import com.hhoa.kline.core.core.task.MessageUtils;
import com.hhoa.kline.core.core.task.ProviderInfo;
import com.hhoa.kline.core.core.task.TaskState;
import com.hhoa.kline.core.core.task.deps.TaskDispatch;
import com.hhoa.kline.core.core.task.focuschain.FocusChainManager;
import com.hhoa.kline.core.core.workspace.WorkspaceRootManager;
import com.hhoa.kline.core.subscription.MessageSender;
import com.hhoa.kline.core.subscription.message.WindowShowMessageRequestMessage;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class TaskV2ContextPrepareHandler {

    private final StateManager stateManager;
    private final ModelContextTracker modelContextTracker;
    private final ContextManager contextManager;
    private final TelemetryService telemetryService;
    private final FileContextTracker fileContextTracker;
    private final FocusChainManager focusChainManager;
    private final ClineIgnoreController clineIgnoreController;
    private final SystemPromptService systemPromptService;
    private final ContextFactory contextFactory;
    private final ResponseFormatter responseFormatter;
    private final WorkspaceRootManager workspaceManager;
    private final IMcpHub mcpHub;
    private final TaskState taskState;
    private final MessageStateHandler messageStateHandler;
    private final Supplier<ProviderInfo> getCurrentProviderInfo;
    private final TaskV2SayAskHandler sayAskHandler;
    private final TaskDispatch taskDispatch;
    private final Supplier<ICheckpointManager> getCheckpointManager;
    private final String taskId;
    private final String ulid;
    private final String cwd;
    private final long taskInitializationStartTime;
    private final Runnable postStateToWebview;
    private final MessageSender messageSender;

    public TaskV2ContextPrepareHandler(
            StateManager stateManager,
            ModelContextTracker modelContextTracker,
            ContextManager contextManager,
            TelemetryService telemetryService,
            FileContextTracker fileContextTracker,
            FocusChainManager focusChainManager,
            ClineIgnoreController clineIgnoreController,
            SystemPromptService systemPromptService,
            ContextFactory contextFactory,
            ResponseFormatter responseFormatter,
            WorkspaceRootManager workspaceManager,
            IMcpHub mcpHub,
            TaskState taskState,
            MessageStateHandler messageStateHandler,
            Supplier<ProviderInfo> getCurrentProviderInfo,
            TaskV2SayAskHandler sayAskHandler,
            TaskDispatch taskDispatch,
            Supplier<ICheckpointManager> getCheckpointManager,
            String taskId,
            String ulid,
            String cwd,
            long taskInitializationStartTime,
            Runnable postStateToWebview,
            MessageSender messageSender) {
        this.stateManager = stateManager;
        this.modelContextTracker = modelContextTracker;
        this.contextManager = contextManager;
        this.telemetryService = telemetryService;
        this.fileContextTracker = fileContextTracker;
        this.focusChainManager = focusChainManager;
        this.clineIgnoreController = clineIgnoreController;
        this.systemPromptService = systemPromptService;
        this.contextFactory = contextFactory;
        this.responseFormatter = responseFormatter;
        this.workspaceManager = workspaceManager;
        this.mcpHub = mcpHub;
        this.taskState = taskState;
        this.messageStateHandler = messageStateHandler;
        this.getCurrentProviderInfo = getCurrentProviderInfo;
        this.sayAskHandler = sayAskHandler;
        this.taskDispatch = taskDispatch;
        this.getCheckpointManager = getCheckpointManager;
        this.taskId = taskId;
        this.ulid = ulid;
        this.cwd = cwd;
        this.taskInitializationStartTime = taskInitializationStartTime;
        this.postStateToWebview = postStateToWebview;
        this.messageSender = messageSender;
    }

    public PrepareContextResult doPrepareContext() {
        if (taskState.isAbort()) {
            return new PrepareContextResult.Abort();
        }

        int maxConsecutiveMistakes = stateManager.getSettings().getMaxConsecutiveMistakes();
        if (taskState.getApiTurnState().getConsecutiveMistakeCount() >= maxConsecutiveMistakes) {
            // In yolo mode, don't wait for user input - fail the task
            if (stateManager.getSettings().isYoloModeToggled()) {
                String errorMessage =
                        "[YOLO MODE] Task failed: Too many consecutive mistakes ("
                                + taskState.getApiTurnState().getConsecutiveMistakeCount()
                                + "). "
                                + "The model may not be capable enough for this task. Consider using a more capable model.";
                sayAskHandler.say(ClineSay.ERROR, errorMessage);
                return new PrepareContextResult.EndLoop();
            }

            String mistakeMessage;
            ProviderInfo providerInfo = getCurrentProviderInfo.get();
            if (providerInfo.getModel() != null && providerInfo.getModel().contains("claude")) {
                mistakeMessage =
                        "This may indicate a failure in Cline's thought process or inability to use a tool properly, which can be mitigated with some user guidance (e.g. \"Try breaking down the task into smaller steps\").";
            } else {
                mistakeMessage =
                        "Cline uses complex prompts and iterative task execution that may be challenging for less capable models. For best results, it's recommended to use Claude 4.5 Sonnet for its advanced agentic coding capabilities.";
            }
            return new PrepareContextResult.MaxMistakeLimitReached(mistakeMessage);
        }

        List<UserContentBlock> userContent = taskState.getApiTurnState().getCurrentUserContent();
        boolean includeFileDetails = taskState.getApiTurnState().isCurrentIncludeFileDetails();
        if (userContent == null) {
            userContent = new ArrayList<>();
        }

        taskState.setApiRequestCount(taskState.getApiRequestCount() + 1);
        taskState.setApiRequestsSinceLastTodoUpdate(
                taskState.getApiRequestsSinceLastTodoUpdate() + 1);

        ProviderInfo providerInfo = getCurrentProviderInfo.get();
        if (providerInfo.getProviderId() != null && providerInfo.getModel() != null) {
            modelContextTracker.recordModelUsage(
                    providerInfo.getProviderId(),
                    providerInfo.getModel(),
                    stateManager.getSettings().getMode());
        }

        int previousApiReqIndex = -1;
        List<ClineMessage> clineMessagesForIndex = messageStateHandler.getClineMessages();
        for (int i = clineMessagesForIndex.size() - 1; i >= 0; i--) {
            ClineMessage msg = clineMessagesForIndex.get(i);
            if (ClineMessageType.SAY.equals(msg.getType())
                    && ClineSay.API_REQ_STARTED.equals(msg.getSay())) {
                previousApiReqIndex = i;
                break;
            }
        }
        taskState.getApiTurnState().setCurrentPreviousApiReqIndex(previousApiReqIndex);

        final boolean isFirstRequest =
                clineMessagesForIndex.stream()
                        .noneMatch(
                                m ->
                                        ClineMessageType.SAY.equals(m.getType())
                                                && ClineSay.API_REQ_STARTED.equals(m.getSay()));

        String requestText =
                userContent.stream()
                        .map(
                                block -> {
                                    if (block instanceof TextContentBlock) {
                                        return ((TextContentBlock) block).getText();
                                    } else if (block instanceof ImageContentBlock) {
                                        return "[IMAGE]";
                                    }
                                    return "";
                                })
                        .filter(s -> s != null && !s.isEmpty())
                        .reduce((a, b) -> a + "\n\n" + b)
                        .orElse("");

        ClineApiReqInfo clineApiReqInfo = new ClineApiReqInfo();
        clineApiReqInfo.setRequest(requestText);
        sayAskHandler.say(
                ClineSay.API_REQ_STARTED,
                JsonUtils.toJsonString(clineApiReqInfo),
                null,
                null,
                null);

        ICheckpointManager checkpointManager = getCheckpointManager.get();
        if (isFirstRequest
                && stateManager.getSettings().isEnableCheckpointsSetting()
                && checkpointManager != null
                && taskState.getCheckpointManagerErrorMessage() == null) {
            try {
                CheckpointInitializer.ensureCheckpointInitialized(
                                checkpointManager,
                                15_000L,
                                "Checkpoints taking too long to initialize. Consider re-opening Cline in a project that uses git, or disabling checkpoints.")
                        .get();
            } catch (Exception error) {
                String errorMessage =
                        error.getMessage() != null ? error.getMessage() : "Unknown error";
                log.error("Failed to initialize checkpoint manager: {}", errorMessage);
                taskState.setCheckpointManagerErrorMessage(errorMessage);
                ShowMessageRequest request =
                        ShowMessageRequest.builder()
                                .type(ShowMessageType.ERROR)
                                .message("Checkpoint initialization timed out: " + errorMessage)
                                .build();
                messageSender.send(new WindowShowMessageRequestMessage(request));
            }
        }

        if (isFirstRequest
                && stateManager.getSettings().isEnableCheckpointsSetting()
                && checkpointManager != null) {
            sayAskHandler.say(ClineSay.CHECKPOINT_CREATED, null, null, null, null);
            int lastCheckpointMessageIndex =
                    MessageUtils.findLastIndex(
                            messageStateHandler.getClineMessages(),
                            m ->
                                    ClineMessageType.SAY.equals(m.getType())
                                            && ClineSay.CHECKPOINT_CREATED.equals(m.getSay()));
            if (lastCheckpointMessageIndex != -1) {
                final int finalIndex = lastCheckpointMessageIndex;
                checkpointManager
                        .commit()
                        .thenAccept(
                                commitHash -> {
                                    if (commitHash != null) {
                                        try {
                                            ClineMessage checkpointMessage =
                                                    messageStateHandler
                                                            .getClineMessages()
                                                            .get(finalIndex);
                                            checkpointMessage.setLastCheckpointHash(commitHash);
                                            messageStateHandler.updateClineMessage(
                                                    finalIndex, checkpointMessage);
                                        } catch (Exception e) {
                                            log.error(
                                                    "Failed to update checkpoint message: {}",
                                                    e.getMessage(),
                                                    e);
                                        }
                                    }
                                })
                        .exceptionally(
                                error -> {
                                    log.error(
                                            "Failed to create checkpoint commit for task {}: {}",
                                            taskId,
                                            error.getMessage());
                                    return null;
                                });
            }
        }

        boolean useAutoCondense = stateManager.getSettings().isUseAutoCondense();
        boolean shouldCompact = false;

        if (useAutoCondense) {
            if (taskState.isCurrentlySummarizing()) {
                taskState.setCurrentlySummarizing(false);
                int[] deletedRange = taskState.getConversationHistoryDeletedRange();
                if (deletedRange != null && deletedRange.length == 2) {
                    List<MessageParam> apiHistory = messageStateHandler.getApiConversationHistory();
                    int start = deletedRange[0];
                    int end = deletedRange[1];
                    int safeEnd = Math.min(end + 2, apiHistory.size() - 1);
                    if (end + 2 <= safeEnd) {
                        taskState.setConversationHistoryDeletedRange(new int[] {start, end + 2});
                    }
                    messageStateHandler.saveClineMessagesAndUpdateHistory();
                }
            } else {
                ContextWindowUtils.ContextWindowInfo contextWindowInfo =
                        ContextWindowUtils.getContextWindowInfo(200000, providerInfo.getModel());
                int contextWindow = contextWindowInfo.contextWindow();
                int maxAllowedSize = contextWindowInfo.maxAllowedSize();
                shouldCompact =
                        contextManager.shouldCompactContextWindow(
                                messageStateHandler.getClineMessages(),
                                contextWindow,
                                maxAllowedSize,
                                previousApiReqIndex);
                if (shouldCompact && taskState.getConversationHistoryDeletedRange() != null) {
                    List<MessageParam> apiHistory = messageStateHandler.getApiConversationHistory();
                    int[] deletedRange = taskState.getConversationHistoryDeletedRange();
                    int activeMessageCount = apiHistory.size() - deletedRange[1] - 1;
                    if (activeMessageCount <= 2) {
                        shouldCompact = false;
                    }
                }
            }
        }

        final boolean finalShouldCompact = shouldCompact;

        LoadContextResult loadResult;
        if (useAutoCondense) {
            if (finalShouldCompact) {
                loadResult = new LoadContextResult(new ArrayList<>(userContent), "", false);
                taskState.setLastAutoCompactTriggerIndex(previousApiReqIndex);
            } else {
                loadResult = loadContext(userContent, includeFileDetails, false);
            }
        } else {
            boolean useCompactPrompt = "compact".equals(providerInfo.getCustomPrompt());
            loadResult = loadContext(userContent, includeFileDetails, useCompactPrompt);
        }

        if (loadResult.clinerulesError()) {
            sayAskHandler.say(
                    ClineSay.ERROR,
                    "Issue with processing the /newrule command. Double check that, if '.clinerules' already exists, it's a directory and not a file. Otherwise there was an issue referencing this file/directory.",
                    null,
                    null,
                    null);
        }

        List<UserContentBlock> processedContent =
                new ArrayList<>(loadResult.processedUserContent());

        if (!finalShouldCompact
                && loadResult.environmentDetails() != null
                && !loadResult.environmentDetails().isEmpty()) {
            TextContentBlock envBlock = new TextContentBlock(loadResult.environmentDetails());
            processedContent.add(envBlock);
        }

        if (finalShouldCompact) {
            String summarizePrompt =
                    ContextManagement.summarizeTask(
                            stateManager.getSettings().getFocusChainSettings().isEnabled(),
                            cwd,
                            false);
            TextContentBlock summarizeBlock = new TextContentBlock(summarizePrompt);
            processedContent.add(summarizeBlock);
        }

        messageStateHandler.addToApiConversationHistory(buildApiUserMessage(processedContent));

        telemetryService.captureConversationTurnEvent(
                ulid, providerInfo.getProviderId(), providerInfo.getModel(), "user");

        if (isFirstRequest) {
            long durationMs = System.currentTimeMillis() - taskInitializationStartTime;
            telemetryService.captureTaskInitialization(
                    ulid,
                    taskId,
                    durationMs,
                    stateManager.getSettings().isEnableCheckpointsSetting());
        }

        String finalRequestText =
                processedContent.stream()
                        .map(
                                block -> {
                                    if (block instanceof TextContentBlock) {
                                        return ((TextContentBlock) block).getText();
                                    } else if (block instanceof ImageContentBlock) {
                                        return "[IMAGE]";
                                    }
                                    return "";
                                })
                        .filter(s -> s != null && !s.isEmpty())
                        .reduce((a, b) -> a + "\n\n" + b)
                        .orElse("");

        List<ClineMessage> messages = messageStateHandler.getClineMessages();
        for (int i = messages.size() - 1; i >= 0; i--) {
            ClineMessage msg = messages.get(i);
            if (ClineMessageType.SAY.equals(msg.getType())
                    && ClineSay.API_REQ_STARTED.equals(msg.getSay())) {
                ClineApiReqInfo apiReqInfo = new ClineApiReqInfo();
                apiReqInfo.setRequest(finalRequestText);
                msg.setText(JsonUtils.toJsonString(apiReqInfo));
                messageStateHandler.updateClineMessage(i, msg);
                break;
            }
        }

        if (postStateToWebview != null) {
            try {
                postStateToWebview.run();
            } catch (Exception e) {
                log.error("Error in postStateToWebview callback: {}", e.getMessage(), e);
            }
        }
        taskState.getPresentationState().setNextUserMessageContent(processedContent);

        return new PrepareContextResult.Success();
    }

    public LoadContextResult loadContext(
            List<UserContentBlock> userContent,
            boolean includeFileDetails,
            boolean useCompactPrompt) {
        List<UserContentBlock> processed = new ArrayList<>();
        boolean needsClinerulesFileCheck = false;

        for (UserContentBlock block : userContent) {
            if (block instanceof TextContentBlock textBlock) {
                String text = textBlock.getText();
                if (text == null) {
                    continue;
                }
                if (text.contains("<feedback>")
                        || text.contains("<answer>")
                        || text.contains("<task>")
                        || text.contains("<user_message>")) {
                    text =
                            Mentions.parseMentions(
                                            text,
                                            cwd,
                                            null,
                                            fileContextTracker,
                                            workspaceManager,
                                            null,
                                            null,
                                            telemetryService,
                                            messageSender)
                                    .join();
                    SystemPromptContext.ApiProviderInfo slashCommandProviderInfo =
                            toApiProviderInfo(getCurrentProviderInfo.get());
                    SlashCommandParser.SlashCommandParseResult slashCommandParseResult =
                            SlashCommandParser.parseSlashCommands(
                                    text,
                                    null,
                                    null,
                                    ulid,
                                    stateManager.getSettings().getFocusChainSettings(),
                                    slashCommandProviderInfo,
                                    resolveNativeToolCallingEnabled(slashCommandProviderInfo),
                                    telemetryService);
                    text = slashCommandParseResult.getProcessedText();
                    if (slashCommandParseResult.isNeedsClinerulesFileCheck()) {
                        needsClinerulesFileCheck = true;
                    }
                }
                TextContentBlock processedBlock = new TextContentBlock(text);
                processed.add(processedBlock);
            } else {
                processed.add(block);
            }
        }

        if (!useCompactPrompt
                && focusChainManager != null
                && focusChainManager.shouldIncludeFocusChainInstructions()) {
            String focusChainInstructions = focusChainManager.generateFocusChainInstructions();
            if (focusChainInstructions != null && !focusChainInstructions.isBlank()) {
                TextContentBlock fcBlock = new TextContentBlock(focusChainInstructions);
                processed.add(fcBlock);
                taskState.setApiRequestsSinceLastTodoUpdate(0);
                taskState.setTodoListWasUpdatedByUser(false);
            }
        }

        String environmentDetails = getEnvironmentDetails(includeFileDetails);

        boolean clinerulesError = false;
        if (needsClinerulesFileCheck) {
            try {
                Path clinerulesPath = Paths.get(cwd, ".clinerules");
                if (java.nio.file.Files.exists(clinerulesPath)
                        && !java.nio.file.Files.isDirectory(clinerulesPath)) {
                    clinerulesError = true;
                }
            } catch (Exception e) {
                clinerulesError = true;
            }
        }

        return new LoadContextResult(processed, environmentDetails, clinerulesError);
    }

    public String getEnvironmentDetails(boolean includeFileDetails) {
        StringBuilder details = new StringBuilder();
        details.append("<environment_details>\n");
        details.append("# Current Working Directory\n");
        details.append(cwd).append("\n");

        ZonedDateTime now = ZonedDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss a");
        details.append("\n# Current Time\n");
        details.append(now.format(formatter))
                .append(" (")
                .append(now.getZone())
                .append(", UTC")
                .append(now.getOffset().getTotalSeconds() / 3600 >= 0 ? "+" : "")
                .append(now.getOffset().getTotalSeconds() / 3600)
                .append(":00)\n");

        details.append("\n# Current Mode\n");
        Mode mode = stateManager.getSettings().getMode();
        if (mode == Mode.PLAN) {
            details.append("PLAN MODE\n");
            details.append(responseFormatter.planModeInstructions()).append("\n");
        } else {
            details.append("ACT MODE\n");
        }

        if (includeFileDetails) {
            details.append("\n# Current Working Directory Files\n");
            try {
                Path cwdPath = Paths.get(cwd);
                if (java.nio.file.Files.exists(cwdPath)
                        && java.nio.file.Files.isDirectory(cwdPath)) {
                    List<String> fileList;
                    boolean didHitLimit = false;
                    try (Stream<Path> stream = java.nio.file.Files.walk(cwdPath, 2)) {
                        fileList =
                                stream.filter(
                                                path -> {
                                                    String fileName =
                                                            path.getFileName() != null
                                                                    ? path.getFileName().toString()
                                                                    : "";
                                                    return !fileName.startsWith(".")
                                                            && !fileName.equals("node_modules")
                                                            && !fileName.equals("target")
                                                            && !fileName.equals("build");
                                                })
                                        .map(Path::toString)
                                        .limit(201)
                                        .collect(Collectors.toList());
                    }
                    if (!fileList.isEmpty()) {
                        String formattedList =
                                responseFormatter.formatFilesList(
                                        cwd, fileList, didHitLimit, clineIgnoreController);
                        details.append(formattedList).append("\n");
                    } else {
                        details.append("(No files found)\n");
                    }
                } else {
                    details.append("(Directory not accessible)\n");
                }
            } catch (Exception e) {
                details.append("(Error listing files: ").append(e.getMessage()).append(")\n");
            }
        }

        details.append("\n# Context Window Usage\n");
        ProviderInfo providerInfo = getCurrentProviderInfo.get();
        ContextWindowUtils.ContextWindowInfo contextWindowInfo =
                ContextWindowUtils.getContextWindowInfo(200000, providerInfo.getModel());
        int contextWindow = contextWindowInfo.contextWindow();
        int lastApiReqTotalTokens =
                MessageUtils.getApiReqInfo(messageStateHandler.getClineMessages())
                        .f1
                        .getTotalTokens();
        int usagePercentage =
                lastApiReqTotalTokens > 0
                        ? (int) Math.round((lastApiReqTotalTokens / (double) contextWindow) * 100)
                        : 0;
        details.append(
                String.format(
                        "%s / %sK tokens used (%d%%)\n",
                        formatNumber(lastApiReqTotalTokens),
                        formatNumber(contextWindow / 1000),
                        usagePercentage));

        details.append("</environment_details>");
        return details.toString();
    }

    public static String formatNumber(int number) {
        return String.format("%,d", number);
    }

    public UserMessage buildApiUserMessage(List<UserContentBlock> userContent) {
        List<UserContentBlock> content = new ArrayList<>();
        for (UserContentBlock block : userContent) {
            if (block instanceof TextContentBlock) {
                TextContentBlock textBlock =
                        new TextContentBlock(((TextContentBlock) block).getText());
                content.add(textBlock);
            } else if (block instanceof ImageContentBlock imageBlock) {
                ImageContentBlock newImageBlock =
                        new ImageContentBlock(
                                imageBlock.getSource(),
                                imageBlock.getSourceType() != null
                                        ? imageBlock.getSourceType()
                                        : "base64",
                                imageBlock.getMediaType() != null
                                        ? imageBlock.getMediaType()
                                        : "image/png");
                content.add(newImageBlock);
            }
        }
        return UserMessage.builder().content(content).build();
    }

    public SystemPromptContext buildSystemPromptContext(boolean hasTruncatedConversationHistory) {
        SystemPromptContext context = new SystemPromptContext();
        context.setCwd(cwd);

        ProviderInfo providerInfo = getCurrentProviderInfo.get();
        SystemPromptContext.ApiProviderInfo apiProviderInfo = toApiProviderInfo(providerInfo);
        context.setProviderInfo(apiProviderInfo);
        context.setSupportsBrowserUse(
                stateManager.getSettings().getBrowserSettings().getRemoteBrowserEnabled());
        context.setMcpHub(mcpHub);

        FocusChainSettings focusChainSettings = stateManager.getSettings().getFocusChainSettings();
        focusChainSettings.setEnabled(focusChainSettings.isEnabled());
        context.setFocusChainSettings(focusChainSettings);

        GlobalState globalState = stateManager.getGlobalState();
        LocalState localState = stateManager.getLocalState();
        RemoteConfigSettings remoteConfigSettings =
                globalState != null ? globalState.getRemoteConfigSettings() : null;

        RuleConditionals.RuleEvaluationContext evaluationContext =
                RuleContextBuilder.buildEvaluationContext(
                        RuleContextBuilder.RuleContextBuilderDeps.builder()
                                .cwd(cwd)
                                .messageStateHandler(messageStateHandler)
                                .workspaceManager(workspaceManager)
                                .build());

        Map<String, Boolean> globalClineToggles =
                new HashMap<>(stateManager.getSettings().getGlobalClineRulesToggles());
        Map<String, Boolean> localClineToggles =
                new HashMap<>(localState.getLocalClineRulesToggles());
        Map<String, Boolean> remoteRulesToggles =
                new HashMap<>(stateManager.getSettings().getRemoteRulesToggles());
        ClineRules.RuleTogglesResult clineRuleToggles =
                ClineRules.refreshClineRulesToggles(
                        stateManager.getGlobalClineRulesDirectory(),
                        cwd,
                        globalClineToggles,
                        localClineToggles,
                        remoteConfigSettings,
                        remoteRulesToggles);
        stateManager.getSettings().setGlobalClineRulesToggles(clineRuleToggles.getGlobalToggles());
        stateManager.getSettings().setRemoteRulesToggles(clineRuleToggles.getRemoteToggles());
        localState.setLocalClineRulesToggles(clineRuleToggles.getLocalToggles());
        stateManager.updateSettings(stateManager.getSettings());
        stateManager.updateLocalState(localState);

        String globalClineRulesDir = stateManager.getGlobalClineRulesDirectory();
        RuleHelpers.RuleLoadResultWithInstructions globalRules =
                ClineRules.getGlobalClineRules(
                        globalClineRulesDir,
                        clineRuleToggles.getGlobalToggles(),
                        evaluationContext,
                        remoteConfigSettings,
                        clineRuleToggles.getRemoteToggles());
        context.setGlobalClineRulesFileInstructions(globalRules.getInstructions());

        RuleHelpers.RuleLoadResultWithInstructions localRules =
                ClineRules.getLocalClineRules(
                        cwd, clineRuleToggles.getLocalToggles(), evaluationContext);
        context.setLocalClineRulesFileInstructions(localRules.getInstructions());

        ExternalRules.ExternalRuleTogglesResult externalToggles =
                ExternalRules.refreshExternalRulesToggles(
                        cwd,
                        new HashMap<>(localState.getLocalWindsurfRulesToggles()),
                        new HashMap<>(localState.getLocalCursorRulesToggles()),
                        new HashMap<>(localState.getLocalAgentsRulesToggles()));
        localState.setLocalWindsurfRulesToggles(externalToggles.getWindsurfLocalToggles());
        localState.setLocalCursorRulesToggles(externalToggles.getCursorLocalToggles());
        localState.setLocalAgentsRulesToggles(externalToggles.getAgentsLocalToggles());
        stateManager.updateLocalState(localState);

        ExternalRules.CursorRulesResult cursorResult =
                ExternalRules.getLocalCursorRules(cwd, externalToggles.getCursorLocalToggles());
        context.setLocalCursorRulesFileInstructions(cursorResult.getFileInstructions());
        context.setLocalCursorRulesDirInstructions(cursorResult.getDirInstructions());
        context.setLocalWindsurfRulesFileInstructions(
                ExternalRules.getLocalWindsurfRules(
                        cwd, externalToggles.getWindsurfLocalToggles()));
        context.setLocalAgentsRulesFileInstructions(
                ExternalRules.getLocalAgentsRules(cwd, externalToggles.getAgentsLocalToggles()));

        List<RuleHelpers.ActivatedConditionalRule> activatedConditionalRules = new ArrayList<>();
        activatedConditionalRules.addAll(globalRules.getActivatedConditionalRules());
        activatedConditionalRules.addAll(localRules.getActivatedConditionalRules());
        if (!activatedConditionalRules.isEmpty()) {
            sayAskHandler.say(
                    ClineSay.CONDITIONAL_RULES_APPLIED,
                    JsonUtils.toJsonString(Map.of("rules", activatedConditionalRules)),
                    null,
                    null,
                    null);
        }

        context.setClineIgnoreInstructions(null);

        String preferredLanguageRaw = stateManager.getSettings().getPreferredLanguage();
        LanguageKey preferredLanguage = getLanguageKey(preferredLanguageRaw);
        String preferredLanguageInstructions =
                preferredLanguage != null
                                && !preferredLanguage.equals(LanguageKey.DEFAULT_LANGUAGE_SETTINGS)
                        ? "# Preferred Language\n\nSpeak in " + preferredLanguage + "."
                        : "";
        context.setPreferredLanguageInstructions(preferredLanguageInstructions);
        context.setBrowserSettings(stateManager.getSettings().getBrowserSettings());
        context.setYoloModeToggled(stateManager.getSettings().isYoloModeToggled());
        context.setWorkspaceRoots(workspaceManager.getRoots());
        context.setSubagentsEnabled(stateManager.getSettings().isSubagentsEnabled());
        context.setIsSubagentRun(false);
        context.setIsCliEnvironment(false);
        context.setHasTruncatedConversationHistory(hasTruncatedConversationHistory);

        List<SkillMetadata> discoveredSkills =
                SkillDiscovery.discoverSkills(cwd, GlobalFileNames.GLOBAL_SKILLS_DIR);
        List<SkillMetadata> availableSkills = SkillDiscovery.getAvailableSkills(discoveredSkills);
        Map<String, Boolean> globalSkillsToggles =
                stateManager.getSettings().getGlobalSkillsToggles() != null
                        ? stateManager.getSettings().getGlobalSkillsToggles()
                        : new HashMap<>();
        Map<String, Boolean> localSkillsToggles =
                localState.getLocalSkillsToggles() != null
                        ? localState.getLocalSkillsToggles()
                        : new HashMap<>();
        context.setSkills(
                availableSkills.stream()
                        .filter(
                                skill -> {
                                    Map<String, Boolean> toggles =
                                            "global".equals(skill.getSource())
                                                    ? globalSkillsToggles
                                                    : localSkillsToggles;
                                    return !Boolean.FALSE.equals(toggles.get(skill.getPath()));
                                })
                        .map(
                                skill ->
                                        new SystemPromptContext.SkillInfo(
                                                skill.getName(), skill.getDescription()))
                        .toList());

        context.setEditorTabs(new SystemPromptContext.EditorTabs(List.of(), List.of()));

        boolean nativeToolCallsEnabled = resolveNativeToolCallingEnabled(apiProviderInfo);
        context.setEnableNativeToolCalls(nativeToolCallsEnabled);
        boolean parallelToolCallingEnabled =
                ModelFamilyMatchers.isParallelToolCallingEnabled(false, apiProviderInfo);
        context.setEnableParallelToolCalling(parallelToolCallingEnabled);
        taskState.getToolExecutionState().setEnableParallelToolCalling(parallelToolCallingEnabled);

        if (globalState != null && globalState.getTerminalExecutionMode() != null) {
            context.setTerminalExecutionMode(
                    globalState.getTerminalExecutionMode().name().toLowerCase());
        } else {
            context.setTerminalExecutionMode("vscodeTerminal");
        }

        context.setIsMultiRootEnabled(
                globalState != null && Boolean.TRUE.equals(globalState.getMultiRootEnabled()));

        return context;
    }

    private SystemPromptContext.ApiProviderInfo toApiProviderInfo(ProviderInfo providerInfo) {
        SystemPromptContext.ApiProviderInfo apiProviderInfo =
                new SystemPromptContext.ApiProviderInfo();
        ModelInfo modelInfo = new ModelInfo();
        modelInfo.setMaxTokens(8000);

        SystemPromptContext.ApiHandlerModel apiHandlerModel =
                new SystemPromptContext.ApiHandlerModel();
        if (providerInfo != null) {
            apiHandlerModel.setId(providerInfo.getModel());
            apiProviderInfo.setCustomPrompt(providerInfo.getCustomPrompt());
            apiProviderInfo.setProviderId(providerInfo.getProviderId());
        }
        apiHandlerModel.setModelInfo(modelInfo);
        apiProviderInfo.setModel(apiHandlerModel);
        return apiProviderInfo;
    }

    private boolean resolveNativeToolCallingEnabled(
            SystemPromptContext.ApiProviderInfo providerInfo) {
        return ModelFamilyMatchers.isNativeToolCallingConfig(providerInfo, true);
    }
}
