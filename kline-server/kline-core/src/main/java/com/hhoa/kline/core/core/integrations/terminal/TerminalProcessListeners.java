package com.hhoa.kline.core.core.integrations.terminal;

import lombok.Getter;

/** 终端进程监听器配置 用于在 runCommand 时一次性注册所有监听器，避免时序问题 */
@Getter
public class TerminalProcessListeners {
    private TerminalProcess.LineListener onLine;
    private Runnable onCompleted;
    private Runnable onError;
    private Runnable onNoShellIntegration;

    public TerminalProcessListeners() {}

    public TerminalProcessListeners onLine(TerminalProcess.LineListener listener) {
        this.onLine = listener;
        return this;
    }

    public TerminalProcessListeners onCompleted(Runnable listener) {
        this.onCompleted = listener;
        return this;
    }

    public TerminalProcessListeners onError(Runnable listener) {
        this.onError = listener;
        return this;
    }

    public TerminalProcessListeners onNoShellIntegration(Runnable listener) {
        this.onNoShellIntegration = listener;
        return this;
    }
}
