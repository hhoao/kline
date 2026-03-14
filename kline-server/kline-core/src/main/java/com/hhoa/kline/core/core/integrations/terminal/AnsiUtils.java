package com.hhoa.kline.core.core.integrations.terminal;

import java.util.regex.Pattern;

/** ANSI 转义序列处理工具 */
public class AnsiUtils {
    // ANSI escape sequence pattern
    // In Java character classes, ] must be escaped or placed at the start
    // Using [\\[\\]()#;?]* to match [, ], (, ), #, ;, ?
    private static final Pattern ANSI_PATTERN =
            Pattern.compile(
                    "[\\u001B\\u009B][\\[\\]()#;?]*(?:(?:(?:(?:;[-a-zA-Z\\d\\/#&.:=?%@~_]+)*|[a-zA-Z\\d]+(?:;[-a-zA-Z\\d\\/#&.:=?%@~_]*)*)?(?:\\u0007|\\u001B\\u005C|\\u009C))|(?:(?:\\d{1,4}(?:;\\d{0,4})*)?[\\dA-PR-TZcf-nq-uy=><~]))",
                    Pattern.MULTILINE);

    /** 移除字符串中的 ANSI 转义序列 */
    public static String stripAnsi(String input) {
        if (input == null) {
            return "";
        }
        return ANSI_PATTERN.matcher(input).replaceAll("");
    }
}
