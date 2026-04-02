package com.hhoa.kline.core.core.integrations.terminal;

import java.util.concurrent.CompletableFuture;

public class TerminalProcessResultPromise {
    private final TerminalProcess process;
    private final CompletableFuture<Void> promise;

    public TerminalProcessResultPromise(TerminalProcess process, CompletableFuture<Void> promise) {
        this.process = process;
        this.promise = promise;
    }

    public TerminalProcess getProcess() {
        return process;
    }

    public CompletableFuture<Void> getPromise() {
        return promise;
    }

    public CompletableFuture<Void> thenRun(Runnable action) {
        return promise.thenRun(action);
    }

    public <U> CompletableFuture<U> thenApply(
            java.util.function.Function<? super Void, ? extends U> fn) {
        return promise.thenApply(fn);
    }

    public CompletableFuture<Void> thenCompose(
            java.util.function.Function<? super Void, ? extends CompletableFuture<Void>> fn) {
        return promise.thenCompose(fn);
    }

    public CompletableFuture<Void> exceptionally(
            java.util.function.Function<Throwable, ? extends Void> fn) {
        return promise.exceptionally(fn);
    }

    public void onLine(TerminalProcess.LineListener listener) {
        process.onLine(listener);
    }

    public void onceContinue(Runnable listener) {
        process.onceContinue(listener);
    }

    public void onceCompleted(Runnable listener) {
        process.onceCompleted(listener);
    }

    public void onceError(TerminalProcess.ErrorListener listener) {
        process.onceError(listener);
    }

    public void onceNoShellIntegration(Runnable listener) {
        process.onceNoShellIntegration(listener);
    }

    public void continueExecution() {
        process.continueExecution();
    }

    public String getUnretrievedOutput() {
        return process.getUnretrievedOutput();
    }

    public boolean isHot() {
        return process.isHot();
    }

    /** 等待完成（类似 CompletableFuture.join()） */
    public Void join() {
        return promise.join();
    }

    /** 获取结果（带超时） */
    public Void get(long timeout, java.util.concurrent.TimeUnit unit)
            throws java.util.concurrent.TimeoutException,
                    java.util.concurrent.ExecutionException,
                    InterruptedException {
        return promise.get(timeout, unit);
    }
}
