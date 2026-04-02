package com.hhoa.kline.core.core.shared;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FocusChainUtils {
    /**
     * 灵活的正则表达式模式，用于匹配焦点链项，支持间距变化 匹配模式如 "- [x] text", "- [X] text", "- [ ] text", "- [ ] text" 等 对应
     */
    public static final Pattern FOCUS_CHAIN_ITEM_REGEX =
            Pattern.compile("^-\\s*\\[([ xX])\\]\\s*(.+)$");

    /**
     * 检查修剪后的行是否匹配焦点链项模式
     *
     * @param line 要检查的修剪后的行
     * @return 如果该行是焦点链项（- [ ]、- [x] 或 - [X]），则返回 true
     */
    public static boolean isFocusChainItem(String line) {
        if (line == null) {
            return false;
        }
        return line.startsWith("- [ ]") || line.startsWith("- [x]") || line.startsWith("- [X]");
    }

    /**
     * 检查修剪后的行是否是已完成的焦点链项
     *
     * @param line 要检查的修剪后的行
     * @return 如果该行是已完成的焦点链项（- [x] 或 - [X]），则返回 true
     */
    public static boolean isCompletedFocusChainItem(String line) {
        if (line == null) {
            return false;
        }
        return line.startsWith("- [x]") || line.startsWith("- [X]");
    }

    /**
     * 使用灵活的正则表达式解析焦点链项（允许间距变化）
     *
     * @param line 要解析的修剪后的行
     * @return 包含 checked 状态和 text 的对象，如果不是焦点链项则返回 null
     */
    public static FocusChainItem parseFocusChainItem(String line) {
        if (line == null) {
            return null;
        }
        Matcher match = FOCUS_CHAIN_ITEM_REGEX.matcher(line);
        if (match.matches()) {
            boolean checked = "x".equals(match.group(1)) || "X".equals(match.group(1));
            String text = match.group(2).trim();
            return new FocusChainItem(checked, text);
        }
        return null;
    }

    public record FocusChainItem(boolean checked, String text) {}
}
