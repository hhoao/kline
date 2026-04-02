package com.hhoa.kline.core.core.task;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

/**
 * Splits stream handling into two paths:
 *
 * <ol>
 *   <li><b>Usage chunks</b> — processed immediately via {@code onUsageChunk} to keep token/cost
 *       state current while the request is still active.
 *   <li><b>Non-usage chunks</b> (text / reasoning / tool_calls) — queued and consumed by the normal
 *       Task flow, which may await tool execution or ask prompts.
 * </ol>
 *
 * <p>Without this split, usage updates can be delayed behind awaited UI/tool work.
 */
@Slf4j
public class StreamChunkCoordinator {

    /** Sentinel value placed in the queue to signal stream completion. */
    private static final ApiChunk POISON_PILL =
            new ApiChunk() {
                @Override
                public ChunkType type() {
                    return null;
                }

                @Override
                public String text() {
                    return null;
                }

                @Override
                public String toolName() {
                    return null;
                }

                @Override
                public java.util.Map<String, Object> toolParams() {
                    return null;
                }

                @Override
                public Integer inputTokens() {
                    return null;
                }

                @Override
                public Integer outputTokens() {
                    return null;
                }

                @Override
                public Integer cacheWriteTokens() {
                    return null;
                }

                @Override
                public Integer cacheReadTokens() {
                    return null;
                }

                @Override
                public Double totalCost() {
                    return null;
                }

                @Override
                public String reasoning() {
                    return null;
                }

                @Override
                public Object reasoningDetails() {
                    return null;
                }

                @Override
                public String thinking() {
                    return null;
                }

                @Override
                public String signature() {
                    return null;
                }

                @Override
                public String data() {
                    return null;
                }

                @Override
                public String toolId() {
                    return null;
                }

                @Override
                public String callId() {
                    return null;
                }
            };

    private final BlockingQueue<ApiChunk> queue = new LinkedBlockingQueue<>();
    private final AtomicReference<Throwable> readError = new AtomicReference<>();
    private final AtomicBoolean completed = new AtomicBoolean(false);
    private final AtomicBoolean stopRequested = new AtomicBoolean(false);
    private final Consumer<ApiChunk> onUsageChunk;
    private final Disposable subscription;

    public StreamChunkCoordinator(Flux<ApiChunk> stream, Consumer<ApiChunk> onUsageChunk) {
        this.onUsageChunk = onUsageChunk;
        this.subscription = startPump(stream);
    }

    private Disposable startPump(Flux<ApiChunk> stream) {
        return stream.subscribe(
                chunk -> {
                    if (stopRequested.get()) {
                        return;
                    }
                    if (chunk.type() == ApiChunk.ChunkType.USAGE) {
                        onUsageChunk.accept(chunk);
                    } else {
                        queue.offer(chunk);
                    }
                },
                error -> {
                    readError.set(error);
                    completed.set(true);
                    queue.offer(POISON_PILL);
                },
                () -> {
                    completed.set(true);
                    queue.offer(POISON_PILL);
                });
    }

    /**
     * Retrieve the next non-usage chunk, blocking until one is available.
     *
     * @return the next chunk, or {@code null} if the stream is complete
     * @throws RuntimeException wrapping the upstream error if the stream failed
     */
    public ApiChunk nextChunk() throws InterruptedException {
        while (true) {
            Throwable error = readError.get();
            if (error != null) {
                throw error instanceof RuntimeException
                        ? (RuntimeException) error
                        : new RuntimeException(error);
            }

            ApiChunk chunk = queue.take();
            if (chunk == POISON_PILL) {
                // Re-check for error that may have been set before the poison pill
                error = readError.get();
                if (error != null) {
                    throw error instanceof RuntimeException
                            ? (RuntimeException) error
                            : new RuntimeException(error);
                }
                return null; // stream completed normally
            }
            return chunk;
        }
    }

    /** Request the coordinator to stop consuming the upstream stream. */
    public void stop() {
        stopRequested.set(true);
        if (!subscription.isDisposed()) {
            subscription.dispose();
        }
        completed.set(true);
        queue.offer(POISON_PILL);
    }

    /**
     * Wait for the stream to complete (either normally or with an error).
     *
     * @throws RuntimeException if the stream completed with an error
     */
    public void waitForCompletion() throws InterruptedException {
        // Drain the queue until we see the poison pill
        while (!completed.get() || !queue.isEmpty()) {
            ApiChunk chunk = queue.take();
            if (chunk == POISON_PILL) {
                break;
            }
        }
        Throwable error = readError.get();
        if (error != null) {
            throw error instanceof RuntimeException
                    ? (RuntimeException) error
                    : new RuntimeException(error);
        }
    }
}
