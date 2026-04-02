package com.hhoa.kline.core.core.task;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;

/**
 * Priority-based presentation scheduler that coalesces intermediate flush requests to reduce
 * message-passing overhead.
 *
 * <p>Flush requests come in two priorities:
 *
 * <ul>
 *   <li>{@link PresentationPriority#IMMEDIATE} — flush synchronously (0 ms delay). Used at semantic
 *       boundaries.
 *   <li>{@link PresentationPriority#NORMAL} — flush after the configured cadence delay, coalescing
 *       intermediate chunks.
 * </ul>
 */
@Slf4j
public class TaskPresentationScheduler {

    @FunctionalInterface
    public interface FlushAction {
        void flush() throws Exception;
    }

    @FunctionalInterface
    public interface DelayProvider {
        int getDelayMs(PresentationPriority priority);
    }

    private final FlushAction flush;
    private final DelayProvider getDelayMs;
    private final Consumer<Throwable> onFlushError;
    private final ScheduledExecutorService scheduler;

    private ScheduledFuture<?> scheduledTimer;
    private PresentationPriority scheduledPriority;
    private volatile PresentationPriority pendingPriority;
    private final AtomicBoolean flushInProgress = new AtomicBoolean(false);
    private volatile CompletableFuture<FlushResult> currentFlushCompletion;
    private volatile boolean disposed = false;

    private final Object lock = new Object();

    public TaskPresentationScheduler(
            FlushAction flush, DelayProvider getDelayMs, Consumer<Throwable> onFlushError) {
        this.flush = flush;
        this.getDelayMs = getDelayMs;
        this.onFlushError = onFlushError;
        this.scheduler =
                Executors.newSingleThreadScheduledExecutor(
                        r -> {
                            Thread t = new Thread(r, "presentation-scheduler");
                            t.setDaemon(true);
                            return t;
                        });
    }

    /**
     * Request a flush at the given priority.
     *
     * <p>If a flush is already in progress, the priority is recorded and will be picked up by the
     * post-flush continuation. If an immediate flush is requested, any pending timer is cancelled
     * and the flush cycle starts synchronously.
     */
    public void requestFlush(PresentationPriority priority) {
        if (disposed) {
            return;
        }
        if (priority == null) {
            priority = PresentationPriority.NORMAL;
        }

        synchronized (lock) {
            pendingPriority = PresentationPriority.merge(pendingPriority, priority);

            if (flushInProgress.get()) {
                // pendingPriority is set above; runFlushCycle will pick it up
                return;
            }

            if (pendingPriority == PresentationPriority.IMMEDIATE) {
                cancelScheduledTimer();
                runFlushCycleAsync(false);
                return;
            }

            PresentationPriority nextPriority =
                    pendingPriority != null ? pendingPriority : PresentationPriority.NORMAL;

            if (scheduledTimer != null) {
                if (scheduledPriority == nextPriority) {
                    return;
                }
                cancelScheduledTimer();
            }

            if (pendingPriority == null) {
                return;
            }

            int delayMs = getDelayMs.getDelayMs(nextPriority);
            scheduledPriority = nextPriority;
            scheduledTimer =
                    scheduler.schedule(
                            () -> {
                                synchronized (lock) {
                                    scheduledTimer = null;
                                    scheduledPriority = null;
                                }
                                runFlushCycleAsync(false);
                            },
                            delayMs,
                            TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Flush immediately and block until completion.
     *
     * <p>Guarantees that at least one flush runs at "immediate" priority after this call returns.
     */
    public void flushNow() {
        if (disposed) {
            return;
        }

        synchronized (lock) {
            cancelScheduledTimer();
        }

        // Wait for in-flight flush to complete
        waitForCurrentFlush();

        if (disposed) {
            return;
        }

        synchronized (lock) {
            pendingPriority =
                    PresentationPriority.merge(pendingPriority, PresentationPriority.IMMEDIATE);
        }

        runFlushCycleSync();
    }

    /**
     * Cancel any pending timers and clear queued state without marking the scheduler as disposed.
     * Use between API request retries to prevent stale timers from firing.
     */
    public void reset() {
        if (disposed) {
            return;
        }
        synchronized (lock) {
            cancelScheduledTimer();
            scheduledPriority = null;
            pendingPriority = null;
        }
    }

    /** Dispose the scheduler, cancelling all pending work and awaiting in-flight flushes. */
    public void dispose() {
        disposed = true;
        synchronized (lock) {
            cancelScheduledTimer();
            scheduledPriority = null;
            pendingPriority = null;
        }

        waitForCurrentFlush();
        scheduler.shutdown();
    }

    private void cancelScheduledTimer() {
        if (scheduledTimer != null) {
            scheduledTimer.cancel(false);
            scheduledTimer = null;
            scheduledPriority = null;
        }
    }

    private void waitForCurrentFlush() {
        CompletableFuture<FlushResult> inFlight = currentFlushCompletion;
        if (inFlight != null) {
            try {
                inFlight.join();
            } catch (Exception ignored) {
                // errors handled by onFlushError callback
            }
        }
        // Wait again if another flush started
        while (flushInProgress.get()) {
            inFlight = currentFlushCompletion;
            if (inFlight != null) {
                try {
                    inFlight.join();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private void runFlushCycleAsync(boolean rethrowErrors) {
        scheduler.execute(() -> doFlushCycle(rethrowErrors));
    }

    private void runFlushCycleSync() {
        doFlushCycle(true);
    }

    private void doFlushCycle(boolean rethrowErrors) {
        while (true) {
            if (disposed) {
                return;
            }

            synchronized (lock) {
                if (flushInProgress.get()) {
                    // Wait for in-flight flush and return
                    CompletableFuture<FlushResult> inFlight = currentFlushCompletion;
                    if (inFlight != null) {
                        FlushResult result;
                        try {
                            result = inFlight.join();
                        } catch (Exception e) {
                            if (rethrowErrors) {
                                throw e instanceof RuntimeException
                                        ? (RuntimeException) e
                                        : new RuntimeException(e);
                            }
                            return;
                        }
                        if (rethrowErrors && result.error != null) {
                            throw result.error instanceof RuntimeException
                                    ? (RuntimeException) result.error
                                    : new RuntimeException(result.error);
                        }
                    }
                    if (flushInProgress.get() || disposed || pendingPriority == null) {
                        return;
                    }
                }

                if (pendingPriority == null) {
                    return;
                }

                flushInProgress.set(true);
                pendingPriority = null;
            }

            CompletableFuture<FlushResult> completion = new CompletableFuture<>();
            currentFlushCompletion = completion;

            FlushResult result;
            try {
                flush.flush();
                result = new FlushResult(null);
            } catch (Exception e) {
                if (onFlushError != null) {
                    onFlushError.accept(e);
                }
                result = new FlushResult(e);
            } finally {
                flushInProgress.set(false);
            }

            completion.complete(result);
            currentFlushCompletion = null;

            if (result.error != null && rethrowErrors) {
                throw result.error instanceof RuntimeException
                        ? (RuntimeException) result.error
                        : new RuntimeException(result.error);
            }

            if (disposed) {
                return;
            }

            PresentationPriority priorityToRun;
            synchronized (lock) {
                priorityToRun = pendingPriority;
            }

            if (priorityToRun == null) {
                return;
            }

            if (priorityToRun != PresentationPriority.IMMEDIATE) {
                requestFlush(priorityToRun);
                return;
            }

            // Continue loop for immediate follow-up
        }
    }

    private record FlushResult(Throwable error) {}
}
