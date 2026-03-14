package com.hhoa.kline.core.core.utils;

/**
 * 字符串工具类 用于处理模型输出中的特殊字符和转义
 *
 * @author hhoa
 */
public class StringUtils {

    /**
     * Fixes incorrectly escaped HTML entities in AI model outputs
     *
     * @param text String potentially containing incorrectly escaped HTML entities from AI models
     * @return String with HTML entities converted back to normal characters
     */
    public static String fixModelHtmlEscaping(String text) {
        if (text == null) {
            return null;
        }

        return text.replace("&gt;", ">")
                .replace("&lt;", "<")
                .replace("&quot;", "\"")
                .replace("&amp;", "&")
                .replace("&apos;", "'");
    }

    /**
     * Removes invalid characters (like the replacement character �) from a string
     *
     * @param text String potentially containing invalid characters
     * @return String with invalid characters removed
     */
    public static String removeInvalidChars(String text) {
        if (text == null) {
            return null;
        }

        return text.replace("\uFFFD", "");
    }

    /**
     * 清理 Markdown 代码块标记 某些模型会在内容周围添加 Markdown 代码块标记
     *
     * @param content 原始内容
     * @return 清理后的内容
     */
    public static String cleanMarkdownCodeBlock(String content) {
        if (content == null) {
            return null;
        }

        String cleaned = content.trim();

        if (cleaned.startsWith("```")) {
            String[] lines = cleaned.split("\n", 2);
            if (lines.length > 1) {
                cleaned = lines[1];
            }
        }

        if (cleaned.endsWith("```")) {
            int lastIndex = cleaned.lastIndexOf("```");
            if (lastIndex > 0) {
                cleaned = cleaned.substring(0, lastIndex);
            }
        }

        return cleaned.trim();
    }

    /**
     * 移除尾部空白 移除字符串末尾的空白字符（空格、制表符、换行符）
     *
     * @param text 原始文本
     * @return 移除尾部空白后的文本
     */
    public static String trimEnd(String text) {
        if (text == null) {
            return null;
        }

        int len = text.length();
        int end = len;

        while (end > 0 && Character.isWhitespace(text.charAt(end - 1))) {
            end--;
        }

        return end < len ? text.substring(0, end) : text;
    }

    /**
     * 检查字符串是否为空或仅包含空白字符
     *
     * @param text 要检查的字符串
     * @return 如果为空或仅包含空白字符则返回 true
     */
    public static boolean isBlank(String text) {
        return text == null || text.trim().isEmpty();
    }

    /**
     * 检查字符串是否不为空且不仅包含空白字符
     *
     * @param text 要检查的字符串
     * @return 如果不为空且不仅包含空白字符则返回 true
     */
    public static boolean isNotBlank(String text) {
        return !isBlank(text);
    }

    /**
     * 转义 JSON 字符串
     *
     * @param text 原始文本
     * @return 转义后的文本
     */
    public static String escapeJson(String text) {
        if (text == null) {
            return null;
        }

        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                .replace("\b", "\\b")
                .replace("\f", "\\f");
    }

    /**
     * 截断字符串到指定长度
     *
     * @param text 原始文本
     * @param maxLength 最大长度
     * @param suffix 截断后添加的后缀（如 "..."）
     * @return 截断后的文本
     */
    public static String truncate(String text, int maxLength, String suffix) {
        if (text == null) {
            return null;
        }

        if (text.length() <= maxLength) {
            return text;
        }

        if (suffix == null) {
            suffix = "";
        }

        int truncateAt = maxLength - suffix.length();
        if (truncateAt < 0) {
            truncateAt = 0;
        }

        return text.substring(0, truncateAt) + suffix;
    }

    /**
     * 截断字符串到指定长度（使用默认后缀 "..."）
     *
     * @param text 原始文本
     * @param maxLength 最大长度
     * @return 截断后的文本
     */
    public static String truncate(String text, int maxLength) {
        return truncate(text, maxLength, "...");
    }

    /**
     * 计算两个字符串的相似度（Levenshtein 距离）
     *
     * @param s1 字符串1
     * @param s2 字符串2
     * @return 相似度（0-1之间，1表示完全相同）
     */
    public static double similarity(String s1, String s2) {
        if (s1 == null || s2 == null) {
            return 0.0;
        }

        if (s1.equals(s2)) {
            return 1.0;
        }

        int distance = levenshteinDistance(s1, s2);
        int maxLength = Math.max(s1.length(), s2.length());

        return 1.0 - ((double) distance / maxLength);
    }

    /**
     * 计算 Levenshtein 距离
     *
     * @param s1 字符串1
     * @param s2 字符串2
     * @return 编辑距离
     */
    private static int levenshteinDistance(String s1, String s2) {
        int len1 = s1.length();
        int len2 = s2.length();

        int[][] dp = new int[len1 + 1][len2 + 1];

        for (int i = 0; i <= len1; i++) {
            dp[i][0] = i;
        }

        for (int j = 0; j <= len2; j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                dp[i][j] =
                        Math.min(
                                Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                                dp[i - 1][j - 1] + cost);
            }
        }

        return dp[len1][len2];
    }
}
