package com.hhoa.kline.core.core.task.tools.utils;

/** 与 Cline {@code lineDiffStats.ts} 对齐：计算两段文本之间的行级差异统计。 */
public final class LineDiffStats {

    private LineDiffStats() {}

    public record DiffStats(int linesAdded, int linesRemoved, int linesChanged) {}

    /**
     * 计算 before/after 内容之间的行级差异统计。
     *
     * @param beforeContent 变更前内容
     * @param afterContent 变更后内容
     * @return 差异统计
     */
    public static DiffStats computeLineDiffStats(String beforeContent, String afterContent) {
        if (beforeContent == null) {
            beforeContent = "";
        }
        if (afterContent == null) {
            afterContent = "";
        }

        String[] beforeLines = beforeContent.split("\n", -1);
        String[] afterLines = afterContent.split("\n", -1);

        int beforeLen = beforeLines.length;
        int afterLen = afterLines.length;

        // 简单的逐行比较（非 LCS）
        int commonPrefix = 0;
        int minLen = Math.min(beforeLen, afterLen);
        while (commonPrefix < minLen
                && beforeLines[commonPrefix].equals(afterLines[commonPrefix])) {
            commonPrefix++;
        }

        int commonSuffix = 0;
        while (commonSuffix < (minLen - commonPrefix)
                && beforeLines[beforeLen - 1 - commonSuffix].equals(
                        afterLines[afterLen - 1 - commonSuffix])) {
            commonSuffix++;
        }

        int beforeChanged = beforeLen - commonPrefix - commonSuffix;
        int afterChanged = afterLen - commonPrefix - commonSuffix;

        int linesChanged = Math.min(beforeChanged, afterChanged);
        int linesRemoved = Math.max(0, beforeChanged - linesChanged);
        int linesAdded = Math.max(0, afterChanged - linesChanged);

        return new DiffStats(linesAdded, linesRemoved, linesChanged);
    }
}
