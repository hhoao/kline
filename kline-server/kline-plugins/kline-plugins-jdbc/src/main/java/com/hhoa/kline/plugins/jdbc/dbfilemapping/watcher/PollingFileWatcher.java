package com.hhoa.kline.plugins.jdbc.dbfilemapping.watcher;

import com.hhoa.kline.plugins.jdbc.dbfilemapping.model.MappingConfiguration;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 基于轮询的文件监听器 Polling-based file watcher as a fallback when WatchService is unavailable Implements
 * graceful degradation strategy
 *
 * <p>Requirements: 10.1, 10.2
 */
public class PollingFileWatcher implements FileWatcher {

    private static final Logger logger = LoggerFactory.getLogger(PollingFileWatcher.class);

    /** 轮询间隔（毫秒） */
    private static final long POLL_INTERVAL_MILLIS = 2000;

    /** 运行状态标志 */
    private final AtomicBoolean running = new AtomicBoolean(false);

    /** 轮询线程池 */
    private ScheduledExecutorService pollingExecutor;

    /** 目录到配置的映射 */
    private final Map<Path, MappingConfiguration> directoryToConfigMap = new ConcurrentHashMap<>();

    /** 文件最后修改时间缓存 */
    private final Map<Path, Long> fileModificationTimes = new ConcurrentHashMap<>();

    /** 文件变更回调 */
    private FileChangeCallback callback;

    /** 防抖管理器 */
    private DebounceManager debounceManager;

    public PollingFileWatcher() {}

    @Override
    public void registerDirectory(Path directory, MappingConfiguration config)
            throws FileWatcherException {
        if (directory == null) {
            throw new FileWatcherException("Directory cannot be null");
        }

        if (config == null) {
            throw new FileWatcherException("Configuration cannot be null");
        }

        if (!Files.exists(directory)) {
            throw new FileWatcherException("Directory does not exist: " + directory);
        }

        if (!Files.isDirectory(directory)) {
            throw new FileWatcherException("Path is not a directory: " + directory);
        }

        // 初始化DebounceManager（如果尚未初始化）
        if (debounceManager == null) {
            debounceManager = new DebounceManager(config.getDebounceMillis());
        }

        // 保存映射关系
        directoryToConfigMap.put(directory, config);

        // 初始化文件修改时间缓存
        try {
            scanDirectory(directory);
        } catch (IOException e) {
            throw new FileWatcherException("Failed to scan directory: " + directory, e);
        }

        logger.info(
                "Registered directory for polling: {} (table: {})",
                directory,
                config.getTableName());
    }

    @Override
    public void unregisterDirectory(Path directory) {
        if (directory == null) {
            return;
        }

        directoryToConfigMap.remove(directory);

        // 清理该目录下的文件缓存
        fileModificationTimes.keySet().removeIf(path -> path.startsWith(directory));

        logger.info("Unregistered directory from polling: {}", directory);
    }

    @Override
    public void start() throws FileWatcherException {
        if (running.get()) {
            logger.warn("PollingFileWatcher is already running");
            return;
        }

        if (directoryToConfigMap.isEmpty()) {
            throw new FileWatcherException(
                    "No directories registered. Call registerDirectory() first.");
        }

        if (callback == null) {
            throw new FileWatcherException(
                    "FileChangeCallback not set. Call setFileChangeCallback() first.");
        }

        running.set(true);

        // 创建轮询线程池
        pollingExecutor =
                Executors.newScheduledThreadPool(
                        1,
                        r -> {
                            Thread thread = new Thread(r, "PollingFileWatcher-Thread");
                            thread.setDaemon(true);
                            return thread;
                        });

        // 启动轮询任务
        pollingExecutor.scheduleWithFixedDelay(
                this::pollDirectories, 0, POLL_INTERVAL_MILLIS, TimeUnit.MILLISECONDS);

        logger.info("PollingFileWatcher started (polling interval: {}ms)", POLL_INTERVAL_MILLIS);
    }

    @Override
    public void stop() {
        if (!running.get()) {
            logger.warn("PollingFileWatcher is not running");
            return;
        }

        logger.info("Stopping PollingFileWatcher...");
        running.set(false);

        // 关闭DebounceManager
        if (debounceManager != null) {
            debounceManager.shutdown();
        }

        // 关闭线程池
        if (pollingExecutor != null) {
            pollingExecutor.shutdown();
            try {
                if (!pollingExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                    pollingExecutor.shutdownNow();
                    logger.warn("PollingFileWatcher thread did not terminate gracefully");
                }
            } catch (InterruptedException e) {
                pollingExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        // 清理缓存
        directoryToConfigMap.clear();
        fileModificationTimes.clear();

        logger.info("PollingFileWatcher stopped");
    }

    @Override
    public void setFileChangeCallback(FileChangeCallback callback) {
        this.callback = callback;
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    /** 轮询所有注册的目录 */
    private void pollDirectories() {
        for (Map.Entry<Path, MappingConfiguration> entry : directoryToConfigMap.entrySet()) {
            Path directory = entry.getKey();

            try {
                // 检查目录是否仍然存在
                if (!Files.exists(directory)) {
                    logger.warn("Directory no longer exists: {}", directory);
                    continue;
                }

                // 扫描目录并检测变更
                checkDirectoryForChanges(directory);

            } catch (Exception e) {
                logger.error("Error polling directory: {}", directory, e);
            }
        }
    }

    /** 检查目录中的文件变更 */
    private void checkDirectoryForChanges(Path directory) throws IOException {
        // 获取该目录的配置
        MappingConfiguration config = directoryToConfigMap.get(directory);
        if (config == null) {
            logger.warn("No configuration found for directory: {}", directory);
            return;
        }

        // 当前扫描的文件集合
        Map<Path, Long> currentFiles = new ConcurrentHashMap<>();

        // 扫描目录
        Files.walkFileTree(
                directory,
                new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        if (shouldProcessFile(file, config)) {
                            currentFiles.put(file, attrs.lastModifiedTime().toMillis());
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) {
                        logger.warn("Failed to visit file: {}", file, exc);
                        return FileVisitResult.CONTINUE;
                    }
                });

        // 检测新建和修改的文件
        for (Map.Entry<Path, Long> entry : currentFiles.entrySet()) {
            Path file = entry.getKey();
            Long currentModTime = entry.getValue();
            Long cachedModTime = fileModificationTimes.get(file);

            if (cachedModTime == null) {
                // 新文件
                logger.debug("Detected new file: {}", file);
                debounceFileEvent(file, () -> callback.onFileCreated(file));
                fileModificationTimes.put(file, currentModTime);

            } else if (!currentModTime.equals(cachedModTime)) {
                // 修改的文件
                logger.debug("Detected modified file: {}", file);
                debounceFileEvent(file, () -> callback.onFileModified(file));
                fileModificationTimes.put(file, currentModTime);
            }
        }

        // 检测删除的文件
        for (Path cachedFile : fileModificationTimes.keySet()) {
            if (cachedFile.startsWith(directory) && !currentFiles.containsKey(cachedFile)) {
                logger.debug("Detected deleted file: {}", cachedFile);
                callback.onFileDeleted(cachedFile);
                fileModificationTimes.remove(cachedFile);
            }
        }
    }

    /** 扫描目录并初始化文件修改时间缓存 */
    private void scanDirectory(Path directory) throws IOException {
        MappingConfiguration config = directoryToConfigMap.get(directory);
        if (config == null) {
            return;
        }

        Files.walkFileTree(
                directory,
                new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        if (shouldProcessFile(file, config)) {
                            fileModificationTimes.put(file, attrs.lastModifiedTime().toMillis());
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) {
                        logger.warn("Failed to visit file during scan: {}", file, exc);
                        return FileVisitResult.CONTINUE;
                    }
                });
    }

    /** 使用防抖机制处理文件事件 */
    private void debounceFileEvent(Path filePath, Runnable action) {
        if (debounceManager != null) {
            debounceManager.debounce(filePath, action);
        } else {
            action.run();
        }
    }

    /** 判断是否应该处理该文件 根据文件结构模式判断 */
    private boolean shouldProcessFile(Path filePath, MappingConfiguration config) {
        if (filePath == null || config == null) {
            return false;
        }

        switch (config.getFileStructureMode()) {
            case SINGLE_JSON:
                // SINGLE_JSON 模式：只处理 JSON 文件
                return isJsonFile(filePath);

            case FIELD_FILES:
                // FIELD_FILES 模式：处理所有常规文件（排除隐藏文件和特殊文件）
                String fileName = filePath.getFileName().toString();
                // 排除隐藏文件、临时文件等
                return !fileName.startsWith(".")
                        && !fileName.endsWith("~")
                        && !fileName.endsWith(".tmp")
                        && Files.isRegularFile(filePath);

            default:
                return false;
        }
    }

    /** 检查是否为JSON文件 */
    private boolean isJsonFile(Path path) {
        if (path == null) {
            return false;
        }
        String fileName = path.getFileName().toString().toLowerCase();
        return fileName.endsWith(".json");
    }
}
