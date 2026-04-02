package com.hhoa.kline.core.core.task.tools.utils;

import java.util.Set;

/** 与 Cline {@code ModelContentProcessor.ts} 对齐：修复非 Claude 模型的内容 quirk。 */
public final class ModelContentProcessor {

    private static final Set<String> ESCAPED_CHARACTER_EXTENSIONS = Set.of(".xml");

    private ModelContentProcessor() {}

    /**
     * 对模型输出内容应用修复：处理转义字符和无效字符。 使用转义字符作为语法的文件（如 XML）将被豁免。
     *
     * @param text 要处理的内容
     * @param modelId 模型 ID（可选 — 若不提供则应用修复）
     * @param filePath 文件路径（可选 — 用于判断是否使用转义字符）
     * @return 处理后的内容
     */
    public static String applyModelContentFixes(String text, String modelId, String filePath) {
        if (text == null) {
            return text;
        }
        if (modelId != null && modelId.contains("claude")) {
            return text;
        }

        boolean usesEscapedCharacters =
                filePath != null
                        && ESCAPED_CHARACTER_EXTENSIONS.stream()
                                .anyMatch(ext -> filePath.toLowerCase().endsWith(ext));

        String processed = text;

        if (!usesEscapedCharacters) {
            processed = fixModelHtmlEscaping(processed);
        }

        processed = removeInvalidChars(processed);

        return processed;
    }

    /** 修复模型 HTML 转义字符 — 将 &lt; &gt; &amp; 等替换回原字符。 */
    static String fixModelHtmlEscaping(String text) {
        if (text == null) {
            return null;
        }
        return text.replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&#39;", "'");
    }

    /** 移除无效字符（零宽字符等）。 */
    static String removeInvalidChars(String text) {
        if (text == null) {
            return null;
        }
        // 移除零宽字符和其他不可见控制字符（保留换行、回车、制表符）
        return text.replaceAll("[\\u200B\\u200C\\u200D\\uFEFF]", "");
    }
}
