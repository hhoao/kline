package com.hhoa.kline.core.core.integrations.terminal;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/** 终端注册表，管理所有终端的生命周期 */
@Slf4j
public class TerminalRegistry {
    private static final List<TerminalInfo> terminals = new ArrayList<>();
    private static int nextTerminalId = 1;

    /** 创建新终端 */
    public static TerminalInfo createTerminal(Path cwd, String shellPath) {
        Terminal terminal =
                new ProcessTerminal(
                        cwd != null ? cwd : Paths.get(System.getProperty("user.dir")), shellPath);
        TerminalInfo info = new TerminalInfo();
        info.setTerminal(terminal);
        info.setBusy(false);
        info.setLastCommand("");
        info.setId(nextTerminalId++);
        info.setShellPath(shellPath);
        info.setLastActive(System.currentTimeMillis());
        terminals.add(info);
        return info;
    }

    /** 根据 ID 获取终端 */
    public static TerminalInfo getTerminal(int id) {
        TerminalInfo info =
                terminals.stream().filter(t -> t.getId() == id).findFirst().orElse(null);

        if (info != null && isTerminalClosed(info.getTerminal())) {
            removeTerminal(id);
            return null;
        }
        return info;
    }

    /** 更新终端信息 */
    public static void updateTerminal(int id, TerminalInfo updates) {
        TerminalInfo terminal = getTerminal(id);
        if (terminal != null) {
            if (updates.getLastCommand() != null) {
                terminal.setLastCommand(updates.getLastCommand());
            }
            if (updates.getLastActive() > 0) {
                terminal.setLastActive(updates.getLastActive());
            }
            if (updates.getShellPath() != null) {
                terminal.setShellPath(updates.getShellPath());
            }
        }
    }

    /** 移除终端 */
    public static void removeTerminal(int id) {
        terminals.removeIf(t -> t.getId() == id);
    }

    /** 获取所有终端 */
    public static List<TerminalInfo> getAllTerminals() {
        terminals.removeIf(t -> isTerminalClosed(t.getTerminal()));
        return new ArrayList<>(terminals);
    }

    /** 检查终端是否已关闭 */
    private static boolean isTerminalClosed(Terminal terminal) {
        return terminal.getExitStatus() != null;
    }
}
