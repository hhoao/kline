package com.hhoa.kline.plugins.jdbc.dbfilemapping.watcher;

import static java.nio.file.StandardWatchEventKinds.*;

import com.hhoa.kline.plugins.jdbc.dbfilemapping.enums.FileStructureMode;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.model.MappingConfiguration;
import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 默认文件监听器实现 Default implementation of FileWatcher using Java NIO WatchService */
public class DefaultFileWatcher implements FileWatcher {

    private static final Logger logger = LoggerFactory.getLogger(DefaultFileWatcher.class);

    /** Java NIO WatchService */
    private WatchService watchService;

    /** 监听线程 */
    private ExecutorService watchThread;

    /** 运行状态标志 */
    private final AtomicBoolean running = new AtomicBoolean(false);

    /** 目录到WatchKey的映射 */
    private final Map<Path, WatchKey> directoryToKeyMap = new ConcurrentHashMap<>();

    /** 目录到配置的映射 */
    private final Map<Path, MappingConfiguration> directoryToConfigMap = new ConcurrentHashMap<>();

    /** 文件变更回调 */
    private FileChangeCallback callback;

    /** 防抖管理器 */
    private DebounceManager debounceManager;

    /** 重启恢复管理器 Requirements: 10.5 */
    private RestartRecoveryManager restartRecoveryManager;

    /** 基础目录（用于重启恢复） */
    private String baseDirectory;

    /** 关闭超时时间（秒） Timeout for graceful shutdown to avoid infinite waiting Requirements: 10.4 */
    private static final int SHUTDOWN_TIMEOUT_SECONDS = 10;

    public DefaultFileWatcher() {}

    /**
     * 构造函数（带基础目录，用于重启恢复）
     *
     * @param baseDirectory 基础目录
     */
    public DefaultFileWatcher(String baseDirectory) {
        this.baseDirectory = baseDirectory;
        if (baseDirectory != null) {
            this.restartRecoveryManager = new RestartRecoveryManager(baseDirectory);
        }
    }

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

        try {
            // 初始化WatchService（如果尚未初始化）
            if (watchService == null) {
                watchService = FileSystems.getDefault().newWatchService();
            }

            // 初始化DebounceManager（如果尚未初始化）
            // 使用配置中的防抖延迟，如果有多个配置，使用第一个配置的值
            if (debounceManager == null) {
                debounceManager = new DebounceManager(config.getDebounceMillis());
            }

            // 根据文件结构模式决定监听策略
            switch (config.getFileStructureMode()) {
                case SINGLE_JSON:
                    // SINGLE_JSON 模式：只监听表目录
                    registerSingleDirectory(directory, config);
                    break;

                case FIELD_FILES:
                    // FIELD_FILES 模式：递归监听表目录及其所有子目录（主键目录）
                    registerDirectoryRecursively(directory, config);
                    break;
            }

            logger.info(
                    "Registered directory for watching: {} (table: {}, mode: {})",
                    directory,
                    config.getTableName(),
                    config.getFileStructureMode());

        } catch (IOException e) {
            throw new FileWatcherException("Failed to register directory: " + directory, e);
        }
    }

    /** 注册单个目录 */
    private void registerSingleDirectory(Path directory, MappingConfiguration config)
            throws IOException {
        WatchKey key = directory.register(watchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);

        directoryToKeyMap.put(directory, key);
        directoryToConfigMap.put(directory, config);

        logger.debug("Registered single directory: {}", directory);
    }

    /** 递归注册目录及其所有子目录 */
    private void registerDirectoryRecursively(Path directory, MappingConfiguration config)
            throws IOException {
        // 注册当前目录
        registerSingleDirectory(directory, config);

        // 递归注册所有子目录
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
            for (Path path : stream) {
                if (Files.isDirectory(path)) {
                    registerSingleDirectory(path, config);
                    logger.debug("Registered subdirectory: {}", path);
                }
            }
        }
    }

    @Override
    public void unregisterDirectory(Path directory) {
        if (directory == null) {
            return;
        }

        WatchKey key = directoryToKeyMap.remove(directory);
        if (key != null) {
            key.cancel();
            logger.info("Unregistered directory from watching: {}", directory);
        }

        directoryToConfigMap.remove(directory);
    }

    @Override
    public void start() throws FileWatcherException {
        if (running.get()) {
            logger.warn("FileWatcher is already running");
            return;
        }

        if (watchService == null) {
            throw new FileWatcherException(
                    "No directories registered. Call registerDirectory() first.");
        }

        if (callback == null) {
            throw new FileWatcherException(
                    "FileChangeCallback not set. Call setFileChangeCallback() first.");
        }

        // 执行重启恢复（扫描目录并同步停机期间的变更）
        // Requirements: 10.5
        performRestartRecovery();

        running.set(true);
        watchThread =
                Executors.newSingleThreadExecutor(
                        r -> {
                            Thread thread = new Thread(r, "FileWatcher-Thread");
                            thread.setDaemon(true);
                            return thread;
                        });

        watchThread.submit(this::watchLoop);

        logger.info("FileWatcher started successfully");
    }

    /**
     * 执行重启恢复 Scan directories and sync changes that occurred during downtime
     *
     * <p>Requirements: 10.5
     */
    private void performRestartRecovery() {
        if (restartRecoveryManager == null) {
            logger.debug("RestartRecoveryManager not initialized, skipping restart recovery");
            return;
        }

        logger.info(
                "Performing restart recovery for {} registered directories",
                directoryToConfigMap.size());

        int totalChanges = 0;

        for (Map.Entry<Path, MappingConfiguration> entry : directoryToConfigMap.entrySet()) {
            Path directory = entry.getKey();
            MappingConfiguration config = entry.getValue();

            try {
                logger.debug("Scanning directory for changes: {}", directory);
                int changes = restartRecoveryManager.performRestartRecovery(config, callback);
                totalChanges += changes;

            } catch (Exception e) {
                logger.error("Failed to perform restart recovery for directory: {}", directory, e);
            }
        }

        if (totalChanges > 0) {
            logger.info(
                    "Restart recovery completed: {} total changes detected and synced",
                    totalChanges);
        } else {
            logger.info("Restart recovery completed: no changes detected");
        }
    }

    @Override
    public void stop() {
        if (!running.get()) {
            logger.warn("FileWatcher is not running");
            return;
        }

        logger.info("Stopping FileWatcher - initiating graceful shutdown...");
        running.set(false);

        try {
            // Step 1: 关闭DebounceManager（等待防抖任务完成）
            // Shutdown debounce manager and wait for pending tasks
            if (debounceManager != null) {
                logger.debug("Shutting down DebounceManager...");
                debounceManager.shutdown();
                logger.debug("DebounceManager shutdown complete");
            }

            // Step 2: 取消所有WatchKey（停止接收新事件）
            // Cancel all watch keys to stop receiving new events
            logger.debug("Cancelling {} watch keys...", directoryToKeyMap.size());
            for (Map.Entry<Path, WatchKey> entry : directoryToKeyMap.entrySet()) {
                try {
                    entry.getValue().cancel();
                    logger.debug("Cancelled watch key for directory: {}", entry.getKey());
                } catch (Exception e) {
                    logger.warn("Error cancelling watch key for directory: {}", entry.getKey(), e);
                }
            }

            // Step 3: 关闭WatchService
            // Close WatchService to interrupt the watch loop
            if (watchService != null) {
                try {
                    logger.debug("Closing WatchService...");
                    watchService.close();
                    logger.debug("WatchService closed");
                } catch (IOException e) {
                    logger.error("Error closing WatchService", e);
                }
            }

            // Step 4: 优雅关闭监听线程（等待进行中的任务完成）
            // Gracefully shutdown watch thread with timeout
            if (watchThread != null) {
                logger.debug("Shutting down watch thread...");
                watchThread.shutdown();

                try {
                    // 等待线程完成，最多等待配置的超时时间
                    // Wait for thread to complete, up to configured timeout
                    boolean terminated =
                            watchThread.awaitTermination(
                                    SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS);

                    if (!terminated) {
                        // 超时后强制关闭
                        // Force shutdown after timeout
                        logger.warn(
                                "FileWatcher thread did not terminate within {} seconds, forcing shutdown",
                                SHUTDOWN_TIMEOUT_SECONDS);
                        watchThread.shutdownNow();

                        // 再等待一小段时间确认强制关闭
                        // Wait a bit more to confirm forced shutdown
                        if (!watchThread.awaitTermination(2, TimeUnit.SECONDS)) {
                            logger.error(
                                    "FileWatcher thread did not terminate even after forced shutdown");
                        } else {
                            logger.info("FileWatcher thread terminated after forced shutdown");
                        }
                    } else {
                        logger.debug("Watch thread terminated gracefully");
                    }

                } catch (InterruptedException e) {
                    // 如果等待被中断，强制关闭
                    // If waiting is interrupted, force shutdown
                    logger.warn(
                            "Interrupted while waiting for watch thread termination, forcing shutdown");
                    watchThread.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }

            // Step 5: 清理资源映射
            // Clear resource mappings
            logger.debug("Clearing resource mappings...");
            directoryToKeyMap.clear();
            directoryToConfigMap.clear();

            logger.info("FileWatcher stopped successfully - all resources cleaned up");

        } catch (Exception e) {
            logger.error(
                    "Error during FileWatcher shutdown, some resources may not be cleaned up properly",
                    e);

            // 尽力清理剩余资源
            // Best effort cleanup of remaining resources
            try {
                if (watchThread != null && !watchThread.isShutdown()) {
                    watchThread.shutdownNow();
                }
                directoryToKeyMap.clear();
                directoryToConfigMap.clear();
            } catch (Exception cleanupEx) {
                logger.error("Error during cleanup after shutdown failure", cleanupEx);
            }
        }
    }

    @Override
    public void setFileChangeCallback(FileChangeCallback callback) {
        this.callback = callback;
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    /** 监听循环 Main watch loop that processes file system events */
    private void watchLoop() {
        logger.info("FileWatcher loop started");

        while (running.get()) {
            try {
                // 等待事件（阻塞调用）
                WatchKey key = watchService.take();

                // 获取对应的目录
                Path directory = getDirectoryForKey(key);
                if (directory == null) {
                    logger.warn("Received event for unknown directory");
                    key.reset();
                    continue;
                }

                // 处理所有事件
                for (WatchEvent<?> event : key.pollEvents()) {
                    processEvent(directory, event);
                }

                // 重置key以继续接收事件
                boolean valid = key.reset();
                if (!valid) {
                    // 目录不再可访问
                    logger.warn("Directory is no longer accessible: {}", directory);
                    handleDirectoryInvalid(directory);
                }

            } catch (InterruptedException e) {
                logger.info("FileWatcher loop interrupted");
                Thread.currentThread().interrupt();
                break;
            } catch (ClosedWatchServiceException e) {
                logger.info("WatchService closed, exiting loop");
                break;
            } catch (Exception e) {
                logger.error("Error in watch loop", e);
                // 继续运行，不因单个错误而停止
                handleWatchLoopException(e);
            }
        }

        logger.info("FileWatcher loop ended");
    }

    /** 处理单个文件系统事件 */
    private void processEvent(Path directory, WatchEvent<?> event) {
        WatchEvent.Kind<?> kind = event.kind();

        // 忽略OVERFLOW事件
        if (kind == StandardWatchEventKinds.OVERFLOW) {
            logger.warn("Event overflow occurred for directory: {}", directory);
            return;
        }

        // 获取文件路径
        @SuppressWarnings("unchecked")
        WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
        Path fileName = pathEvent.context();
        Path filePath = directory.resolve(fileName);

        // 获取该目录的配置
        MappingConfiguration config = directoryToConfigMap.get(directory);
        if (config == null) {
            logger.warn("No configuration found for directory: {}", directory);
            return;
        }

        // 在 FIELD_FILES 模式下，如果创建了新的子目录（主键目录），需要注册监听
        if (config.getFileStructureMode() == FileStructureMode.FIELD_FILES
                && kind == ENTRY_CREATE
                && Files.isDirectory(filePath)) {
            try {
                registerSingleDirectory(filePath, config);
                logger.info("Auto-registered new subdirectory: {}", filePath);
            } catch (IOException e) {
                logger.error("Failed to register new subdirectory: {}", filePath, e);
            }
        }

        // 根据文件结构模式判断是否需要处理该文件
        if (!shouldProcessFile(filePath, config)) {
            return;
        }

        try {
            // 根据事件类型调用相应的回调，使用防抖机制
            if (kind == ENTRY_CREATE) {
                debounceFileEvent(filePath, () -> callback.onFileCreated(filePath));
            } else if (kind == ENTRY_MODIFY) {
                debounceFileEvent(filePath, () -> callback.onFileModified(filePath));
            } else if (kind == ENTRY_DELETE) {
                // 删除事件不需要防抖，立即处理
                callback.onFileDeleted(filePath);
            }
        } catch (Exception e) {
            logger.error("Error processing file event: {} - {}", kind.name(), filePath, e);
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

    /**
     * 使用防抖机制处理文件事件 Process file event with debouncing
     *
     * @param filePath 文件路径
     * @param action 要执行的操作
     */
    private void debounceFileEvent(Path filePath, Runnable action) {
        if (debounceManager != null) {
            debounceManager.debounce(filePath, action);
        } else {
            // 如果防抖管理器未初始化，直接执行
            action.run();
        }
    }

    /** 根据WatchKey获取对应的目录 */
    private Path getDirectoryForKey(WatchKey key) {
        for (Map.Entry<Path, WatchKey> entry : directoryToKeyMap.entrySet()) {
            if (entry.getValue().equals(key)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * 处理目录失效的情况 Implements automatic recovery for deleted/recreated directories
     *
     * <p>Requirements: 10.2
     */
    private void handleDirectoryInvalid(Path directory) {
        logger.warn("Handling invalid directory: {}", directory);

        // 移除映射
        directoryToKeyMap.remove(directory);
        MappingConfiguration config = directoryToConfigMap.remove(directory);

        if (config == null) {
            return;
        }

        // 尝试恢复（如果目录重新出现）
        int maxAttempts = 3;
        int attempt = 0;
        boolean recovered = false;

        while (attempt < maxAttempts && !recovered) {
            attempt++;

            try {
                // 等待目录重新出现
                Thread.sleep(1000 * attempt); // 递增等待时间

                if (Files.exists(directory)) {
                    logger.info("Directory reappeared, attempting to re-register: {}", directory);
                    registerDirectory(directory, config);
                    recovered = true;
                    logger.info(
                            "Successfully recovered directory watch after {} attempts: {}",
                            attempt,
                            directory);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Directory recovery interrupted for: {}", directory);
                break;
            } catch (FileWatcherException e) {
                logger.warn(
                        "Failed to re-register directory (attempt {}/{}): {}",
                        attempt,
                        maxAttempts,
                        directory,
                        e);
            }
        }

        if (!recovered) {
            logger.error(
                    "Failed to recover directory watch after {} attempts: {}",
                    maxAttempts,
                    directory);
        }
    }

    /**
     * 处理监听循环中的异常 Implements exception recovery to keep watcher running
     *
     * <p>Requirements: 10.1
     */
    private void handleWatchLoopException(Exception e) {
        logger.error("Exception in watch loop, attempting to recover", e);

        // 检查是否为严重错误
        if (isFatalException(e)) {
            logger.error("Fatal exception detected, stopping file watcher", e);
            running.set(false);
            return;
        }

        // 尝试恢复
        try {
            // 短暂休眠后继续
            Thread.sleep(1000);

            // 检查WatchService是否仍然有效
            if (watchService == null) {
                logger.warn("WatchService is null, attempting to reinitialize");
                reinitializeWatchService();
            }

        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            logger.warn("Recovery interrupted");
        } catch (Exception ex) {
            logger.error("Failed to recover from exception", ex);
        }
    }

    /** 检查是否为致命异常 */
    private boolean isFatalException(Exception e) {
        // 检查cause是否为OutOfMemoryError等严重错误
        Throwable cause = e.getCause();
        while (cause != null) {
            if (cause instanceof OutOfMemoryError) {
                return true;
            }
            cause = cause.getCause();
        }

        return false;
    }

    /** 重新初始化WatchService */
    private void reinitializeWatchService() {
        try {
            // 关闭旧的WatchService
            if (watchService != null) {
                try {
                    watchService.close();
                } catch (Exception e) {
                    logger.warn("Error closing old WatchService", e);
                }
            }

            // 创建新的WatchService
            watchService = FileSystems.getDefault().newWatchService();

            // 重新注册所有目录
            Map<Path, MappingConfiguration> configsCopy =
                    new ConcurrentHashMap<>(directoryToConfigMap);
            directoryToKeyMap.clear();
            directoryToConfigMap.clear();

            for (Map.Entry<Path, MappingConfiguration> entry : configsCopy.entrySet()) {
                try {
                    registerDirectory(entry.getKey(), entry.getValue());
                    logger.info("Re-registered directory: {}", entry.getKey());
                } catch (FileWatcherException e) {
                    logger.error("Failed to re-register directory: {}", entry.getKey(), e);
                }
            }

            logger.info("WatchService reinitialized successfully");

        } catch (IOException e) {
            logger.error("Failed to reinitialize WatchService", e);
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
