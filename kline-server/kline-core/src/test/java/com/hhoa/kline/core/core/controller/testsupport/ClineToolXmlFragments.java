package com.hhoa.kline.core.core.controller.testsupport;

import com.hhoa.kline.core.enums.ClineDefaultTool;

/**
 * 集成测试中构造与运行时解析器一致的工具 XML 片段（与 {@link ScriptedConversationApiHandler} 配合）。
 */
public final class ClineToolXmlFragments {

    private ClineToolXmlFragments() {}

    public static String askFollowupQuestion(String question) {
        String name = ClineDefaultTool.ASK.getValue();
        return "<%s>\n<question>%s</question>\n<options>[]</options>\n</%s>\n"
                .formatted(name, escapeXmlText(question), name);
    }

    /** {@code command} 省略，仅 {@code result}，用于触发无 CLI 的完成路径。 */
    public static String attemptCompletionResultOnly(String result) {
        String name = ClineDefaultTool.ATTEMPT.getValue();
        return "<%s>\n<result>%s</result>\n</%s>\n".formatted(name, escapeXmlText(result), name);
    }

    private static String escapeXmlText(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
