package com.hhoa.kline.core.core.integrations.terminal;

import java.util.concurrent.CompletableFuture;
import lombok.Data;

/** 终端信息类，用于跟踪终端状态 */
@Data
public class TerminalInfo {
    private Terminal terminal;
    private boolean busy;
    private String lastCommand;
    private int id;
    private String shellPath;
    private long lastActive;
    private String pendingCwdChange;
    private CwdResolved cwdResolved;

    /** CWD 解析回调 */
    public static class CwdResolved {
        private final CompletableFuture<Void> future;

        public CwdResolved(CompletableFuture<Void> future) {
            this.future = future;
        }

        public void resolve() {
            future.complete(null);
        }

        public void reject(Throwable error) {
            future.completeExceptionally(error);
        }
    }
}
