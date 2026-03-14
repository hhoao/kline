package com.hhoa.kline.core.core.context.tracking;

import com.hhoa.kline.core.core.shared.ClineSay;
import com.hhoa.kline.core.core.storage.StateManager;
import com.hhoa.kline.core.core.task.ClineMessage;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * 文件上下文跟踪器
 *
 * <p>此类负责跟踪可能导致上下文过期的文件操作。 如果用户在 Cline 外部修改文件，上下文可能会过期并需要更新。 我们不希望 Cline 在每次文件修改时都重新加载上下文，因此我们使用此类仅
 * 通知 Cline 发生了更改，并告诉 Cline 在对文件进行任何更改之前重新加载文件。 这修复了差异编辑的问题，即 Cline 无法完成差异编辑，因为文件在 Cline 上次读取后被修改。
 *
 * <p>如果文件的完整内容通过工具、提及或编辑传递给 Cline，则该文件被标记为活跃。 如果文件在 Cline 外部被修改，我们检测并跟踪此更改以防止上下文过期。 这在恢复任务（非 git
 * "检查点"恢复）和任务中期使用。
 */
@Slf4j
public class FileContextTracker {
    @Getter private final String taskId;

    @Setter @Getter private String cwd;

    private final StateManager stateManager;

    private final Map<String, WatchService> fileWatchers = new ConcurrentHashMap<>();

    private final Set<String> recentlyModifiedFiles = ConcurrentHashMap.newKeySet();

    private final Set<String> recentlyEditedByCline = ConcurrentHashMap.newKeySet();

    public FileContextTracker(String taskId, String cwd, StateManager stateManager) {
        this.taskId = taskId;
        this.cwd = cwd;
        this.stateManager = stateManager;
    }

    /**
     * 为每个在任务元数据中跟踪的文件设置文件监视器
     *
     * @param filePath 文件路径
     */
    public void setupFileWatcher(String filePath) {
        if (fileWatchers.containsKey(filePath)) {
            return;
        }

        if (cwd == null || cwd.isEmpty()) {
            log.info("No workspace folder available - cannot determine current working directory");
            return;
        }

        try {
            Path resolvedFilePath = Paths.get(cwd, filePath);
            Path parentDir = resolvedFilePath.getParent();

            if (parentDir == null || !Files.exists(parentDir)) {
                return;
            }

            WatchService watchService = FileSystems.getDefault().newWatchService();
            parentDir.register(
                    watchService,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_CREATE);

            Thread watchThread =
                    new Thread(
                            () -> {
                                try {
                                    WatchKey key;
                                    while ((key = watchService.take()) != null) {
                                        for (WatchEvent<?> event : key.pollEvents()) {
                                            Path changed = (Path) event.context();
                                            if (changed != null
                                                    && resolvedFilePath.endsWith(changed)) {
                                                handleFileChange(filePath);
                                            }
                                        }
                                        key.reset();
                                    }
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                } catch (Exception e) {
                                    log.error("Error in file watcher: " + e.getMessage());
                                }
                            });
            watchThread.setDaemon(true);
            watchThread.start();

            fileWatchers.put(filePath, watchService);
        } catch (IOException e) {
            log.error("Failed to setup file watcher for " + filePath + ": " + e.getMessage());
        }
    }

    /**
     * 处理文件变更事件
     *
     * @param filePath 文件路径
     */
    private void handleFileChange(String filePath) {
        if (recentlyEditedByCline.contains(filePath)) {
            recentlyEditedByCline.remove(filePath);
        } else {
            recentlyModifiedFiles.add(filePath);
            trackFileContext(filePath, "user_edited");
        }
    }

    /**
     * 在元数据中跟踪文件操作并为文件设置监视器 这是 FileContextTracker 的主要入口点，当文件通过工具、提及或编辑传递给 Cline 时调用。
     *
     * @param filePath 文件路径
     * @param operation
     *     操作类型：read_tool（读取工具）、user_edited（用户编辑）、cline_edited（Cline编辑）、file_mentioned（文件提及）
     */
    public CompletableFuture<Void> trackFileContext(String filePath, String operation) {
        try {
            if (cwd == null || cwd.isEmpty()) {
                log.info(
                        "No workspace folder available - cannot determine current working directory");
                return CompletableFuture.completedFuture(null);
            }

            addFileToFileContextTracker(taskId, filePath, operation);

            setupFileWatcher(filePath);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("Failed to track file operation: " + e.getMessage());
            e.printStackTrace();
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * 将文件添加到元数据跟踪器 这处理确定文件是新的、过期的还是活跃的业务逻辑。 它还使用最新的读取/编辑日期更新元数据。
     *
     * @param taskId 任务ID
     * @param filePath 文件路径
     * @param source 记录来源
     */
    public void addFileToFileContextTracker(String taskId, String filePath, String source) {
        try {
            TaskMetadata metadata = stateManager.getTaskMetadata(taskId);
            long now = System.currentTimeMillis();

            for (FileMetadataEntry entry : metadata.getFilesInContext()) {
                if (entry.getPath().equals(filePath) && "active".equals(entry.getRecordState())) {
                    entry.setRecordState("stale");
                }
            }

            Long latestClineReadDate = getLatestDateForField(metadata, filePath, "clineReadDate");
            Long latestClineEditDate = getLatestDateForField(metadata, filePath, "clineEditDate");
            Long latestUserEditDate = getLatestDateForField(metadata, filePath, "userEditDate");

            FileMetadataEntry newEntry =
                    new FileMetadataEntry(
                            filePath,
                            "active",
                            source,
                            latestClineReadDate,
                            latestClineEditDate,
                            latestUserEditDate);

            switch (source) {
                case "user_edited":
                    newEntry.setUserEditDate(now);
                    recentlyModifiedFiles.add(filePath);
                    break;

                case "cline_edited":
                    newEntry.setClineReadDate(now);
                    newEntry.setClineEditDate(now);
                    break;

                case "read_tool":
                case "file_mentioned":
                    newEntry.setClineReadDate(now);
                    break;
            }

            metadata.getFilesInContext().add(newEntry);
            stateManager.saveTaskMetadata(taskId, metadata);
        } catch (Exception e) {
            log.error("Failed to add file to metadata: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 获取特定字段和文件的最新日期
     *
     * @param metadata 任务元数据
     * @param path 文件路径
     * @param field 字段名称
     * @return 最新日期
     */
    private Long getLatestDateForField(TaskMetadata metadata, String path, String field) {
        return metadata.getFilesInContext().stream()
                .filter(entry -> entry.getPath().equals(path))
                .map(
                        entry ->
                                switch (field) {
                                    case "clineReadDate" -> entry.getClineReadDate();
                                    case "clineEditDate" -> entry.getClineEditDate();
                                    case "userEditDate" -> entry.getUserEditDate();
                                    default -> null;
                                })
                .filter(Objects::nonNull)
                .max(Long::compareTo)
                .orElse(null);
    }

    /**
     * 返回（然后清除）最近修改的文件集合
     *
     * @return 最近修改的文件列表
     */
    public List<String> getAndClearRecentlyModifiedFiles() {
        List<String> files = new ArrayList<>(recentlyModifiedFiles);
        recentlyModifiedFiles.clear();
        return files;
    }

    /**
     * 将文件标记为由 Cline 编辑，以防止文件监视器中的误报
     *
     * @param filePath 文件路径
     */
    public void markFileAsEditedByCline(String filePath) {
        recentlyEditedByCline.add(filePath);
    }

    public void dispose() {
        for (WatchService watchService : fileWatchers.values()) {
            try {
                watchService.close();
            } catch (IOException e) {
                log.error("Error closing watch service: " + e.getMessage());
            }
        }
        fileWatchers.clear();
    }

    /**
     * 检测在特定消息时间戳之后由 Cline 或用户编辑的文件 这在恢复检查点时用于警告潜在的文件内容不匹配
     *
     * @param messageTs 消息时间戳
     * @param deletedMessages 已删除的消息列表
     * @return 编辑的文件列表
     */
    public List<String> detectFilesEditedAfterMessage(
            long messageTs, List<ClineMessage> deletedMessages) {
        Set<String> editedFiles = new HashSet<>();

        try {
            TaskMetadata taskMetadata = stateManager.getTaskMetadata(taskId);

            if (taskMetadata != null && taskMetadata.getFilesInContext() != null) {
                for (FileMetadataEntry fileEntry : taskMetadata.getFilesInContext()) {
                    boolean clineEditedAfter =
                            fileEntry.getClineEditDate() != null
                                    && fileEntry.getClineEditDate() > messageTs;
                    boolean userEditedAfter =
                            fileEntry.getUserEditDate() != null
                                    && fileEntry.getUserEditDate() > messageTs;

                    if (clineEditedAfter || userEditedAfter) {
                        editedFiles.add(fileEntry.getPath());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error checking file context metadata: " + e.getMessage());
        }

        if (deletedMessages != null) {
            for (ClineMessage message : deletedMessages) {
                if (ClineSay.TOOL.equals(message.getSay()) && message.getText() != null) {
                    try {
                        // 这里需要解析 JSON 来获取工具数据
                        // 简化处理：检查文本中是否包含文件路径
                        if (message.getText().contains("editedExistingFile")
                                || message.getText().contains("newFileCreated")) {
                            // 实际实现中需要正确解析 JSON
                            // 这里仅作为示例
                        }
                    } catch (Exception e) {
                        log.error("Error checking task messages: " + e.getMessage());
                    }
                }
            }
        }

        return new ArrayList<>(editedFiles);
    }

    /**
     * 在工作区状态中存储待处理的文件上下文警告，以便在任务重新初始化时保持
     *
     * @param files 文件列表
     */
    public void storePendingFileContextWarning(List<String> files) {
        try {
            // 注意：这里需要与 StateManager 集成
            // 实际实现中需要调用 StateManager 的方法
            // 例如：stateManager.setWorkspaceState("pendingFileContextWarning_" + taskId, files);
            log.info("Storing pending file context warning for task " + taskId + ": " + files);
        } catch (Exception e) {
            log.error("Error storing pending file context warning: " + e.getMessage());
        }
    }

    /**
     * 从工作区状态检索待处理的文件上下文警告（不清除）
     *
     * @return 文件列表
     */
    public List<String> retrievePendingFileContextWarning() {
        try {
            // 注意：这里需要与 StateManager 集成
            // 实际实现中需要调用 StateManager 的方法
            // 例如：return stateManager.getWorkspaceStateKey("pendingFileContextWarning_" + taskId);
            return null;
        } catch (Exception e) {
            log.error("Error retrieving pending file context warning: " + e.getMessage());
            return null;
        }
    }

    /**
     * 从工作区状态检索并清除待处理的文件上下文警告
     *
     * @return 文件列表
     */
    public List<String> retrieveAndClearPendingFileContextWarning() {
        try {
            List<String> files = retrievePendingFileContextWarning();
            if (files != null) {
                // 注意：这里需要与 StateManager 集成
                // 实际实现中需要调用 StateManager 的方法来清除
                // 例如：stateManager.setWorkspaceState("pendingFileContextWarning_" + taskId, null);
                log.info("Clearing pending file context warning for task " + taskId);
                return files;
            }
        } catch (Exception e) {
            log.error("Error retrieving pending file context warning: " + e.getMessage());
        }
        return null;
    }
}
