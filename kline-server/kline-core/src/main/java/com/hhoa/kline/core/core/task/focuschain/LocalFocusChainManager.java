package com.hhoa.kline.core.core.task.focuschain;

import com.hhoa.kline.core.core.services.telemetry.TelemetryService;
import com.hhoa.kline.core.core.storage.StateManager;
import com.hhoa.kline.core.core.task.TaskState;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LocalFocusChainManager extends AbstractFocusChainManager {

    private final Runnable postStateToWebview;
    private final String taskDirectory;

    private final AtomicBoolean disposed = new AtomicBoolean(false);
    private ExecutorService watcherExecutor;
    private WatchService watchService;
    private ScheduledExecutorService debounceExecutor;
    private ScheduledFuture<?> fileUpdateDebounceTimer;

    public LocalFocusChainManager(
            String taskId,
            TaskState taskState,
            String taskDirectory,
            StateManager stateManager,
            Runnable postStateToWebview,
            SayCallback say,
            TelemetryService telemetryService) {
        super(taskId, taskState, stateManager, say, telemetryService);
        this.taskDirectory = taskDirectory;
        this.postStateToWebview = postStateToWebview;
        this.debounceExecutor =
                Executors.newSingleThreadScheduledExecutor(
                        r -> {
                            Thread t = new Thread(r, "focus-chain-debounce-" + taskId);
                            t.setDaemon(true);
                            return t;
                        });
    }

    @Override
    public String getFocusChain() {
        return stateManager.getFocusChain(taskId);
    }

    @Override
    public void saveFocusChain(String todoList) throws IOException {
        stateManager.saveFocusChain(taskId, todoList);
    }

    @Override
    public void setupFocusChain() {
        if (watcherExecutor != null) {
            return;
        }

        watcherExecutor =
                Executors.newSingleThreadExecutor(
                        r -> {
                            Thread t = new Thread(r, "focus-chain-watcher-" + taskId);
                            t.setDaemon(true);
                            return t;
                        });

        watcherExecutor.submit(
                () -> {
                    try {
                        String focusChainFilePath =
                                FocusChainFileUtils.getFocusChainFilePath(taskDirectory, taskId);
                        Path focusFile = Paths.get(focusChainFilePath);
                        Path dir = focusFile.getParent();

                        if (dir == null || !Files.exists(dir)) {
                            return;
                        }

                        watchService = FileSystems.getDefault().newWatchService();
                        dir.register(
                                watchService,
                                StandardWatchEventKinds.ENTRY_MODIFY,
                                StandardWatchEventKinds.ENTRY_CREATE,
                                StandardWatchEventKinds.ENTRY_DELETE);

                        log.info("[Task {}] Todo file watcher initialized", taskId);

                        while (!disposed.get()) {
                            try {
                                WatchKey key = watchService.take();
                                for (WatchEvent<?> event : key.pollEvents()) {
                                    WatchEvent.Kind<?> kind = event.kind();
                                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                                        continue;
                                    }

                                    Path changed = dir.resolve((Path) event.context());
                                    if (changed.getFileName()
                                            .toString()
                                            .equals(focusFile.getFileName().toString())) {
                                        if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                                            taskState.setCurrentFocusChainChecklist(null);
                                            if (postStateToWebview != null) {
                                                postStateToWebview.run();
                                            }
                                        } else {
                                            updateFCListFromMarkdownFileAndNotifyUI();
                                        }
                                    }
                                }
                                if (!key.reset()) {
                                    break;
                                }
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                break;
                            } catch (Exception watchError) {
                                log.error(
                                        "[Task {}] Failed to watch focus chain file",
                                        taskId,
                                        watchError);
                            }
                        }
                    } catch (Exception error) {
                        log.error("[Task {}] Failed to setup todo file watcher", taskId, error);
                    }
                });
    }

    /** 从 markdown 文件读取当前的 Focus Chain 列表并更新 UI。 使用防抖（300ms）防止过度更新，仅在内容实际更改时通知 webview。 */
    private void updateFCListFromMarkdownFileAndNotifyUI() {
        if (fileUpdateDebounceTimer != null) {
            fileUpdateDebounceTimer.cancel(false);
        }

        fileUpdateDebounceTimer =
                debounceExecutor.schedule(
                        () -> {
                            try {
                                String markdownTodoList = getFocusChain();
                                if (markdownTodoList != null) {
                                    String previousList = taskState.getCurrentFocusChainChecklist();

                                    // 仅在内容实际更改时更新
                                    if (!markdownTodoList.equals(previousList)) {
                                        taskState.setCurrentFocusChainChecklist(markdownTodoList);
                                        taskState.setTodoListWasUpdatedByUser(true);

                                        if (postStateToWebview != null) {
                                            postStateToWebview.run();
                                        }
                                        if (telemetryService != null) {
                                            telemetryService.captureFocusChainListWritten(
                                                    this.taskId);
                                        }
                                    } else {
                                        log.debug(
                                                "[Task {}] Focus Chain List: File watcher triggered but content unchanged, skipping update",
                                                taskId);
                                    }
                                }
                            } catch (Exception error) {
                                log.error(
                                        "[Task {}] Error updating focus chain list from markdown file",
                                        taskId,
                                        error);
                            }
                        },
                        300,
                        TimeUnit.MILLISECONDS);
    }

    @Override
    public void dispose() {
        disposed.set(true);

        if (fileUpdateDebounceTimer != null) {
            fileUpdateDebounceTimer.cancel(false);
            fileUpdateDebounceTimer = null;
        }

        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException e) {
                log.warn("[Task {}] Failed to close watch service", taskId, e);
            }
            watchService = null;
        }

        if (watcherExecutor != null) {
            watcherExecutor.shutdownNow();
            watcherExecutor = null;
        }

        if (debounceExecutor != null) {
            debounceExecutor.shutdownNow();
            debounceExecutor = null;
        }
    }
}
