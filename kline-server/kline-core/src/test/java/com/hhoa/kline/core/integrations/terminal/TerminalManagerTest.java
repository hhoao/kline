package com.hhoa.kline.core.integrations.terminal;

import com.hhoa.kline.core.core.integrations.terminal.TerminalInfo;
import com.hhoa.kline.core.core.integrations.terminal.TerminalManager;
import com.hhoa.kline.core.core.integrations.terminal.TerminalProcessListeners;
import com.hhoa.kline.core.core.integrations.terminal.TerminalProcessResultPromise;
import com.hhoa.kline.core.core.integrations.terminal.TerminalRegistry;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** TerminalManager 测试类 */
class TerminalManagerTest {

    private TerminalManager terminalManager;

    @BeforeEach
    void setUp() {
        terminalManager = new TerminalManager();
        // 清理 TerminalRegistry 中的终端
        TerminalRegistry.getAllTerminals()
                .forEach(terminal -> TerminalRegistry.removeTerminal(terminal.getId()));
    }

    @AfterEach
    void tearDown() {
        if (terminalManager != null) {
            terminalManager.disposeAll();
        }
        // 清理 TerminalRegistry 中的终端
        TerminalRegistry.getAllTerminals()
                .forEach(terminal -> TerminalRegistry.removeTerminal(terminal.getId()));
    }

    @Test
    void testGetOrCreateTerminal() throws Exception {
        // 测试获取或创建终端
        String cwd = System.getProperty("user.dir");

        CompletableFuture<TerminalInfo> future = terminalManager.getOrCreateTerminal(cwd);
        TerminalInfo terminalInfo = future.get();
        TerminalProcessListeners terminalProcessListeners = new TerminalProcessListeners();
        terminalProcessListeners.onLine(
                (line) -> {
                    System.out.println(line);
                });
        terminalProcessListeners.onError(
                () -> {
                    System.out.println("error");
                });
        terminalProcessListeners.onCompleted(
                () -> {
                    System.out.println("completed");
                });
        TerminalProcessResultPromise cwd1 =
                terminalManager.runCommand(terminalInfo, "pwd", terminalProcessListeners);
        cwd1.join();
    }
}
