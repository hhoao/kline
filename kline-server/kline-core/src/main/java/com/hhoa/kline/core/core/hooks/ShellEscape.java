package com.hhoa.kline.core.core.hooks;

/**
 * 平台相关的 shell 路径转义工具。
 *
 * <p>对应 Cline TS 版本的 shell-escape.ts。 确保包含空格和特殊字符的路径在 shell（shell: true）执行时正确。
 */
public final class ShellEscape {

    private ShellEscape() {}

    /**
     * 转义文件路径以便在当前平台的 shell 中安全执行。
     *
     * @param path 文件路径
     * @return 转义后的路径
     */
    public static String escapeShellPath(String path) {
        return isWindows() ? escapeWindowsShellPath(path) : escapeUnixShellPath(path);
    }

    /**
     * Windows shell (cmd.exe) 规则：
     *
     * <ul>
     *   <li>用双引号包裹路径
     *   <li>双引号前的反斜杠需要加倍
     *   <li>独立的双引号用双引号 "" 转义
     * </ul>
     */
    static String escapeWindowsShellPath(String path) {
        String escaped = path.replace("\\\"", "\\\\\"");
        escaped = escaped.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    /**
     * Unix shell (sh, bash, zsh) 规则：
     *
     * <ul>
     *   <li>用单引号包裹路径
     *   <li>路径内的单引号转义为 '\''
     * </ul>
     */
    static String escapeUnixShellPath(String path) {
        String escaped = path.replace("'", "'\\''");
        return "'" + escaped + "'";
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }
}
