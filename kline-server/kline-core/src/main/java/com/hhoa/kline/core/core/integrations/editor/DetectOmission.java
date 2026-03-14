package com.hhoa.kline.core.core.integrations.editor;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

/**
 * 检测代码遗漏工具类 用于检测 AI 生成的代码中可能存在的遗漏（当达到最大输出限制时）
 *
 * @author hhoa
 */
@Slf4j
public class DetectOmission {

    private static final List<String> OMISSION_KEYWORDS =
            Arrays.asList("remain", "remains", "unchanged", "rest", "previous", "existing", "...");

    private static final List<Pattern> COMMENT_PATTERNS =
            Arrays.asList(
                    Pattern.compile("^\\s*//"),
                    Pattern.compile("^\\s*#"),
                    Pattern.compile("^\\s*/\\*"),
                    Pattern.compile("^\\s*\\{\\s*/\\*"),
                    Pattern.compile("^\\s*<!--"));

    /**
     * 检测代码遗漏
     *
     * @param originalFileContent 原始文件内容
     * @param newFileContent 新文件内容
     * @return 是否检测到潜在的遗漏
     */
    public static boolean detectCodeOmission(String originalFileContent, String newFileContent) {
        if (originalFileContent == null || newFileContent == null) {
            return false;
        }

        String[] originalLines = originalFileContent.split("\n");
        String[] newLines = newFileContent.split("\n");

        for (String line : newLines) {
            boolean isComment =
                    COMMENT_PATTERNS.stream().anyMatch(pattern -> pattern.matcher(line).find());

            if (isComment) {
                String[] words = line.toLowerCase().split("\\s+");
                boolean containsKeyword =
                        OMISSION_KEYWORDS.stream()
                                .anyMatch(
                                        keyword -> {
                                            for (String word : words) {
                                                if (word.equals(keyword)) {
                                                    return true;
                                                }
                                            }
                                            return false;
                                        });

                if (containsKeyword) {
                    boolean existsInOriginal = false;
                    for (String originalLine : originalLines) {
                        if (originalLine.equals(line)) {
                            existsInOriginal = true;
                            break;
                        }
                    }

                    if (!existsInOriginal) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * 显示遗漏警告 注意：在实际应用中，这个方法应该调用相应的 UI 框架来显示警告
     *
     * @param originalFileContent 原始文件内容
     * @param newFileContent 新文件内容
     * @return 是否检测到遗漏
     */
    public static boolean showOmissionWarning(String originalFileContent, String newFileContent) {
        if (detectCodeOmission(originalFileContent, newFileContent)) {
            String warningMessage =
                    "Potential code truncation detected. This happens when the AI reaches its max output limit.";
            log.warn(warningMessage);
            // 在实际应用中，这里应该调用 UI 框架显示警告
            // 例如：HostProvider.window.showMessage(...)
            return true;
        }
        return false;
    }
}
