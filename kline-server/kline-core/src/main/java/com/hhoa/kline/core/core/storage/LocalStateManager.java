package com.hhoa.kline.core.core.storage;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hhoa.kline.core.common.utils.DirtyTrackingProxy;
import com.hhoa.kline.core.core.assistant.MessageParam;
import com.hhoa.kline.core.core.context.instructions.userinstructions.RuleHelpers;
import com.hhoa.kline.core.core.context.tracking.TaskMetadata;
import com.hhoa.kline.core.core.controller.HistoryItem;
import com.hhoa.kline.core.core.shared.storage.GlobalState;
import com.hhoa.kline.core.core.shared.storage.LocalState;
import com.hhoa.kline.core.core.shared.storage.Secrets;
import com.hhoa.kline.core.core.shared.storage.Settings;
import com.hhoa.kline.core.core.task.ClineMessage;
import com.hhoa.kline.core.core.task.PathUtils;
import com.hhoa.kline.core.core.task.focuschain.FocusChainFileUtils;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class LocalStateManager implements StateManager {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final long PERSISTENCE_DELAY_MS = 500;

    private static final long PERSISTENCE_INTERVAL_SECONDS = 30;

    private final StorageContext context;

    private final AtomicBoolean globalStateCacheLoaded = new AtomicBoolean(false);

    private final AtomicBoolean globalStateDirty = new AtomicBoolean(false);

    private final AtomicBoolean secretsCacheLoaded = new AtomicBoolean(false);

    private final AtomicBoolean secretsDirty = new AtomicBoolean(false);

    private final AtomicBoolean settingsCacheLoaded = new AtomicBoolean(false);

    private final AtomicBoolean settingsDirty = new AtomicBoolean(false);

    private final AtomicBoolean localStateCacheLoaded = new AtomicBoolean(false);

    private final AtomicBoolean localStateDirty = new AtomicBoolean(false);

    private final ScheduledExecutorService persistenceExecutor =
            Executors.newSingleThreadScheduledExecutor(
                    r -> {
                        Thread t = new Thread(r, "StateManager-Persistence");
                        t.setDaemon(true);
                        return t;
                    });

    private volatile GlobalState globalState;
    private volatile Secrets secrets;
    private volatile Settings settings;
    private volatile LocalState localState;

    private ScheduledFuture<?> persistenceTask;

    private ScheduledFuture<?> delayedPersistenceTask;

    private File globalStateFile;
    private File secretsFile;
    private File settingsFile;
    private File localStateFile;

    private final Path clineWorkflowsDirectory;
    private final Path clineRulesDirectory;
    private final String tasksDirectory;
    private final String stateDirectory;
    private final String settingsDirectory;
    private final String cacheDirectory;

    public LocalStateManager(
            StorageContext context,
            String tasksDirectory,
            String stateDirectory,
            String clineWorkflowsDirectory,
            String clineRulesDirectory,
            String settingsDirectory,
            String cacheDirectory) {
        this.tasksDirectory = tasksDirectory;
        this.stateDirectory = stateDirectory;
        this.settingsDirectory = settingsDirectory;
        this.cacheDirectory = cacheDirectory;
        this.clineWorkflowsDirectory = Paths.get(clineWorkflowsDirectory);
        this.clineRulesDirectory = Paths.get(clineRulesDirectory);

        this.context = context;
        initializeDirectoriesAndFiles();
        startPersistenceTask();
        loadAllStateFromDisk();
    }

    private <T> T readJsonFromFile(File file, Class<T> clazz, T defaultValue) {
        if (!file.exists()) {
            return defaultValue;
        }
        try {
            return objectMapper.readValue(file, clazz);
        } catch (Exception e) {
            log.error("读取JSON文件失败: {} - {}", file.getPath(), e.getMessage(), e);
            return defaultValue;
        }
    }

    private <T> T readJsonFromFile(File file, TypeReference<T> typeRef, T defaultValue) {
        if (!file.exists()) {
            return defaultValue;
        }
        try {
            return objectMapper.readValue(file, typeRef);
        } catch (Exception e) {
            log.error("读取JSON文件失败: {} - {}", file.getPath(), e.getMessage(), e);
            return defaultValue;
        }
    }

    private boolean writeJsonToFile(File file, Object value) {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, value);
            return true;
        } catch (Exception e) {
            log.error("写入JSON文件失败: {} - {}", file.getPath(), e.getMessage(), e);
            return false;
        }
    }

    private void createDirectories(Path path) {
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            throw new RuntimeException("创建目录失败: " + path, e);
        }
    }

    private void initializeDirectoriesAndFiles() {
        globalStateFile =
                createFileIfNotExists(
                        Paths.get(stateDirectory, "globalState.json").toString(), "{}");
        secretsFile =
                createFileIfNotExists(Paths.get(stateDirectory, "secrets.json").toString(), "{}");

        settingsFile =
                createFileIfNotExists(
                        Paths.get(settingsDirectory, "settings.json").toString(), "{}");
        localStateFile =
                createFileIfNotExists(
                        Paths.get(stateDirectory, "localState.json").toString(), "{}");
    }

    private File createFileIfNotExists(String filePath, String defaultContent) {
        File file = new File(filePath);
        if (!file.exists()) {
            try {
                File parentDir = file.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    createDirectories(parentDir.toPath());
                }
                Files.write(file.toPath(), defaultContent.getBytes());
            } catch (IOException e) {
                log.error("创建文件失败: {} - {}", filePath, e.getMessage(), e);
                throw new RuntimeException("创建文件失败: " + filePath, e);
            }
        }
        return file;
    }

    private void startPersistenceTask() {
        persistenceTask =
                persistenceExecutor.scheduleWithFixedDelay(
                        this::persistDirtyData,
                        PERSISTENCE_INTERVAL_SECONDS,
                        PERSISTENCE_INTERVAL_SECONDS,
                        TimeUnit.SECONDS);
    }

    private void persistDirtyData() {
        try {
            if (globalStateDirty.getAndSet(false)) {
                persistGlobalState();
            }
            if (secretsDirty.getAndSet(false)) {
                persistSecrets();
            }
            if (settingsDirty.getAndSet(false)) {
                persistSettings();
            }
            if (localStateDirty.getAndSet(false)) {
                persistLocalState();
            }
        } catch (Exception e) {
            log.error("定时持久化数据失败: {}", e.getMessage(), e);
        }
    }

    private synchronized void loadAllStateFromDisk() {
        loadGlobalStateFromDisk();
        loadSecretsFromDisk();
        loadSettingsFromDisk();
        loadLocalStateFromDisk();
    }

    private synchronized void loadGlobalStateFromDisk() {
        if (globalStateCacheLoaded.get()) {
            return;
        }
        GlobalState loadedState =
                readJsonFromFile(globalStateFile, GlobalState.class, new GlobalState());
        globalState = DirtyTrackingProxy.createProxy(loadedState, this::markGlobalStateDirty);
        globalStateCacheLoaded.set(true);
    }

    private synchronized void persistGlobalState() {
        if (globalState == null) {
            return;
        }
        boolean success = writeJsonToFile(globalStateFile, globalState);
        if (!success) {
            globalStateDirty.set(true);
        }
    }

    private synchronized void loadSecretsFromDisk() {
        if (secretsCacheLoaded.get()) {
            return;
        }
        Secrets loadedSecrets = readJsonFromFile(secretsFile, Secrets.class, new Secrets());
        secrets = DirtyTrackingProxy.createProxy(loadedSecrets, this::markSecretsDirty);
        secretsCacheLoaded.set(true);
    }

    private synchronized void persistSecrets() {
        if (secrets == null) {
            return;
        }
        boolean success = writeJsonToFile(secretsFile, secrets);
        if (!success) {
            secretsDirty.set(true);
        }
    }

    private synchronized void loadSettingsFromDisk() {
        if (settingsCacheLoaded.get()) {
            return;
        }
        Settings loadedSettings = readJsonFromFile(settingsFile, Settings.class, new Settings());
        settings = DirtyTrackingProxy.createProxy(loadedSettings, this::markSettingsDirty);
        settingsCacheLoaded.set(true);
    }

    private synchronized void persistSettings() {
        if (settings == null) {
            return;
        }
        boolean success = writeJsonToFile(settingsFile, settings);
        if (!success) {
            settingsDirty.set(true);
        }
    }

    private synchronized void loadLocalStateFromDisk() {
        if (localStateCacheLoaded.get()) {
            return;
        }
        LocalState loadedLocalState =
                readJsonFromFile(localStateFile, LocalState.class, LocalState.builder().build());
        localState = DirtyTrackingProxy.createProxy(loadedLocalState, this::markLocalStateDirty);
        localStateCacheLoaded.set(true);
    }

    private synchronized void persistLocalState() {
        if (localState == null) {
            return;
        }
        boolean success = writeJsonToFile(localStateFile, localState);
        if (!success) {
            localStateDirty.set(true);
        }
    }

    public synchronized GlobalState getGlobalState() {
        if (!globalStateCacheLoaded.get()) {
            loadGlobalStateFromDisk();
        }
        return globalState;
    }

    public synchronized Secrets getSecrets() {
        if (!secretsCacheLoaded.get()) {
            loadSecretsFromDisk();
        }
        return secrets;
    }

    public synchronized Settings getSettings() {
        if (!settingsCacheLoaded.get()) {
            loadSettingsFromDisk();
        }
        return settings;
    }

    public synchronized LocalState getLocalState() {
        if (!localStateCacheLoaded.get()) {
            loadLocalStateFromDisk();
        }
        return localState;
    }

    public synchronized void updateGlobalState(GlobalState globalState) {
        this.globalState = globalState;
        markGlobalStateDirty();
    }

    public synchronized void updateSecrets(Secrets secrets) {
        this.secrets = secrets;
        markSecretsDirty();
    }

    public synchronized void updateSettings(Settings settings) {
        this.settings = settings;
        markSettingsDirty();
    }

    public synchronized void updateLocalState(LocalState localState) {
        this.localState = localState;
        markLocalStateDirty();
    }

    private synchronized void schedulePersistence(Runnable persistenceTask) {
        if (delayedPersistenceTask != null && !delayedPersistenceTask.isDone()) {
            delayedPersistenceTask.cancel(false);
        }
        delayedPersistenceTask =
                persistenceExecutor.schedule(
                        persistenceTask, PERSISTENCE_DELAY_MS, TimeUnit.MILLISECONDS);
    }

    private void markGlobalStateDirty() {
        globalStateDirty.set(true);
        schedulePersistence(
                () -> {
                    if (globalStateDirty.get()) {
                        persistGlobalState();
                    }
                });
    }

    private void markSecretsDirty() {
        secretsDirty.set(true);
        schedulePersistence(
                () -> {
                    if (secretsDirty.get()) {
                        persistSecrets();
                    }
                });
    }

    private void markSettingsDirty() {
        settingsDirty.set(true);
        schedulePersistence(
                () -> {
                    if (settingsDirty.get()) {
                        persistSettings();
                    }
                });
    }

    private void markLocalStateDirty() {
        localStateDirty.set(true);
        schedulePersistence(
                () -> {
                    if (localStateDirty.get()) {
                        persistLocalState();
                    }
                });
    }

    private void flush() {
        persistDirtyData();
    }

    public void shutdown() {
        if (persistenceTask != null) {
            persistenceTask.cancel(false);
        }
        if (delayedPersistenceTask != null && !delayedPersistenceTask.isDone()) {
            delayedPersistenceTask.cancel(false);
        }
        flush();
        persistenceExecutor.shutdown();
        try {
            if (!persistenceExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                persistenceExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            persistenceExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public String getOrCreateTaskDirectoryExists(String taskId) {
        Path taskDir = Paths.get(tasksDirectory, taskId);
        try {
            if (!Files.exists(taskDir)) {
                Files.createDirectories(taskDir);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return taskDir.toString();
    }

    public long getTaskDirectorySize(String taskId) {
        String taskDir = getOrCreateTaskDirectoryExists(taskId);
        return PathUtils.getFolderSize(taskDir);
    }

    public List<MessageParam> getSavedApiConversationHistory(String taskId) {
        String filePath =
                Paths.get(
                                getOrCreateTaskDirectoryExists(taskId),
                                GlobalFileNames.API_CONVERSATION_HISTORY)
                        .toString();
        return readJsonFromFile(
                new File(filePath), new TypeReference<List<MessageParam>>() {}, new ArrayList<>());
    }

    public void saveApiConversationHistory(
            String taskId, List<MessageParam> apiConversationHistory) {
        String filePath =
                Paths.get(
                                getOrCreateTaskDirectoryExists(taskId),
                                GlobalFileNames.API_CONVERSATION_HISTORY)
                        .toString();
        writeJsonToFile(new File(filePath), apiConversationHistory);
    }

    public List<ClineMessage> getSavedClineMessages(String taskId) {
        String filePath =
                Paths.get(getOrCreateTaskDirectoryExists(taskId), GlobalFileNames.UI_MESSAGES)
                        .toString();
        return readJsonFromFile(
                new File(filePath), new TypeReference<List<ClineMessage>>() {}, new ArrayList<>());
    }

    public void saveClineMessages(String taskId, List<ClineMessage> uiMessages) {
        String taskDir = getOrCreateTaskDirectoryExists(taskId);
        String filePath = Paths.get(taskDir, GlobalFileNames.UI_MESSAGES).toString();
        writeJsonToFile(new File(filePath), uiMessages);
    }

    public TaskMetadata getTaskMetadata(String taskId) {
        String filePath =
                Paths.get(getOrCreateTaskDirectoryExists(taskId), GlobalFileNames.TASK_METADATA)
                        .toString();
        return readJsonFromFile(new File(filePath), TaskMetadata.class, new TaskMetadata());
    }

    public void saveTaskMetadata(String taskId, TaskMetadata metadata) {
        String taskDir = getOrCreateTaskDirectoryExists(taskId);
        String filePath = Paths.get(taskDir, GlobalFileNames.TASK_METADATA).toString();
        writeJsonToFile(new File(filePath), metadata);
    }

    public void deleteTask(String taskId) {
        List<HistoryItem> history = globalState.getTaskHistory();
        if (history == null) {
            history = new ArrayList<>();
        }
        history.removeIf(task -> task.getId().equals(taskId));
        markGlobalStateDirty();

        String orCreateTaskDirectoryExists = getOrCreateTaskDirectoryExists(taskId);

        List<String> filePaths = new ArrayList<>();
        filePaths.add(orCreateTaskDirectoryExists);

        for (String filePath : filePaths) {
            try {
                Files.deleteIfExists(Paths.get(filePath));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public List<HistoryItem> getTaskHistory(String taskId) {
        return globalState.getTaskHistory().stream()
                .filter(item -> item.getId().equals(taskId))
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getWorkspaceRoots() {
        return context.getWorkspaceRoots();
    }

    @Override
    public long getTotalTasksSize() {
        String tasksDir = Paths.get(context.getGlobalStoragePath(), "tasks").toString();
        String checkpointsDir = Paths.get(context.getGlobalStoragePath(), "checkpoints").toString();

        long tasksSize = PathUtils.getFolderSize(tasksDir);
        long checkpointsSize = PathUtils.getFolderSize(checkpointsDir);

        return tasksSize + checkpointsSize;
    }

    @Override
    public String getGlobalClineRulesDirectory() {
        return clineRulesDirectory != null ? clineRulesDirectory.toString() : null;
    }

    @Override
    public String getFocusChain(String taskId) {
        String taskDir = getOrCreateTaskDirectoryExists(taskId);
        String todoFilePath = FocusChainFileUtils.getFocusChainFilePath(taskDir, taskId);
        String markdownContent;
        if (new File(todoFilePath).exists()) {
            try {
                markdownContent = Files.readString(Paths.get(todoFilePath), StandardCharsets.UTF_8);
            } catch (IOException e) {
                log.error("Read FocusChain error", e);
                return null;
            }
            String todoList = FocusChainFileUtils.extractFocusChainListFromText(markdownContent);

            if (todoList != null) {
                FocusChainFileUtils.extractFocusChainItemsFromText(markdownContent);
                return todoList;
            }
        }
        return null;
    }

    @Override
    public void saveFocusChain(String taskId, String todoList) throws IOException {
        try {
            String taskDir = getOrCreateTaskDirectoryExists(taskId);
            String todoFilePath = FocusChainFileUtils.getFocusChainFilePath(taskDir, taskId);
            String fileContent =
                    FocusChainFileUtils.createFocusChainMarkdownContent(taskId, todoList);
            Files.write(Paths.get(todoFilePath), fileContent.getBytes(StandardCharsets.UTF_8));
        } catch (IOException error) {
            log.error(
                    "[Task {}] focus chain list: FILE WRITE FAILED - Error: {}",
                    taskId,
                    error.getMessage());
            throw error;
        }
    }

    @Data
    private static class TaskInfo {
        private String ulid;
        private Long timestamp;
        private String taskDescription;
        private Integer tokensIn;
        private Integer tokensOut;
        private Integer cacheWrites;
        private Integer cacheReads;
        private Double totalCost;
        private Long size;
        private Boolean isFavorited;
        private int[] conversationHistoryDeletedRange;
    }

    public RuleHelpers.CreateRuleFileResult createRuleFile(
            boolean isGlobal, String filename, String cwd, String type) {
        return RuleHelpers.createRuleFile(
                isGlobal,
                filename,
                cwd,
                type,
                clineWorkflowsDirectory.toString(),
                clineRulesDirectory.toString());
    }
}
