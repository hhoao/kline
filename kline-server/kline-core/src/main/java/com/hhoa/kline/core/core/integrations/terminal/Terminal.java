package com.hhoa.kline.core.core.integrations.terminal;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/** 终端接口，抽象终端操作 */
public interface Terminal {
    /** 获取终端的工作目录 */
    Path getCwd();

    /** 获取 Shell 集成信息 */
    ShellIntegration getShellIntegration();

    /** 发送文本到终端 */
    void sendText(String text, boolean addNewLine);

    /** 显示终端 */
    void show();

    /** 释放终端资源 */
    void dispose();

    /** 获取终端退出状态 */
    Integer getExitStatus();

    /** Shell 集成接口 */
    interface ShellIntegration {
        /** 获取当前工作目录 */
        Path getCwd();

        /** 执行命令并返回输出流 */
        CommandExecution executeCommand(String command);
    }

    /** 命令执行接口 */
    interface CommandExecution {
        /**
         * 读取命令输出流（流式处理）
         *
         * @param onData 数据回调，每次接收到数据时调用
         * @return CompletableFuture，当流结束时完成
         */
        CompletableFuture<Void> read(java.util.function.Consumer<String> onData);
    }
}
