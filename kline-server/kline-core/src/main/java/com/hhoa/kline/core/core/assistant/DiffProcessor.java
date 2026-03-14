package com.hhoa.kline.core.core.assistant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

/**
 * 差异处理器 处理 SEARCH/REPLACE 格式的代码差异
 *
 * @author hhoa
 */
@Slf4j
public class DiffProcessor {

    private static final String SEARCH_BLOCK_START = "------- SEARCH";
    private static final String SEARCH_BLOCK_END = "=======";
    private static final String REPLACE_BLOCK_END = "+++++++ REPLACE";

    // 搜索块字符（用于检测部分标记）
    private static final String SEARCH_BLOCK_CHAR = "-";
    private static final String REPLACE_BLOCK_CHAR = "+";

    // 正则表达式模式（预编译以提高性能）
    private static final Pattern SEARCH_BLOCK_START_REGEX = Pattern.compile("^[-]{3,} SEARCH>?$");
    private static final Pattern SEARCH_BLOCK_END_REGEX = Pattern.compile("^[=]{3,}$");
    private static final Pattern REPLACE_BLOCK_END_REGEX = Pattern.compile("^[+]{3,} REPLACE>?$");

    private static final Pattern SEARCH_TAG_REGEX = Pattern.compile("^[-]{3,} SEARCH$");
    private static final Pattern REPLACE_BEGIN_TAG_REGEX = Pattern.compile("^[=]{3,}$");
    private static final Pattern REPLACE_END_TAG_REGEX = Pattern.compile("^[+]{3,} REPLACE$");

    /** 构建新文件内容 V2 */
    public String constructNewFileContent(
            String diffContent, String originalContent, boolean isFinal) {
        NewFileContentConstructor constructor =
                new NewFileContentConstructor(originalContent, isFinal);

        String[] lines = diffContent.split("\n");

        // 如果最后一行看起来像部分标记但不是已知标记，则删除它
        if (lines.length > 0) {
            String lastLine = lines[lines.length - 1];
            if (isPartialMarker(lastLine)) {
                lines = Arrays.copyOf(lines, lines.length - 1);
            }
        }

        for (String line : lines) {
            constructor.processLine(line);
        }

        return constructor.getResult();
    }

    private boolean isSearchBlockStart(String line) {
        return SEARCH_BLOCK_START_REGEX.matcher(line).matches();
    }

    private boolean isSearchBlockEnd(String line) {
        return SEARCH_BLOCK_END_REGEX.matcher(line).matches();
    }

    private boolean isReplaceBlockEnd(String line) {
        return REPLACE_BLOCK_END_REGEX.matcher(line).matches();
    }

    private boolean isPartialMarker(String line) {
        return (line.startsWith(SEARCH_BLOCK_CHAR)
                        || line.startsWith("=")
                        || line.startsWith(REPLACE_BLOCK_CHAR))
                && !isSearchBlockStart(line)
                && !isSearchBlockEnd(line)
                && !isReplaceBlockEnd(line);
    }

    /** 行修剪回退匹配 */
    private int[] lineTrimmedFallbackMatch(
            String originalContent, String searchContent, int startIndex) {
        String[] originalLines = originalContent.split("\n");
        String[] searchLines = searchContent.split("\n");

        if (searchLines.length > 0 && searchLines[searchLines.length - 1].isEmpty()) {
            searchLines = Arrays.copyOf(searchLines, searchLines.length - 1);
        }

        int startLineNum = 0;
        int currentIndex = 0;
        while (currentIndex < startIndex && startLineNum < originalLines.length) {
            currentIndex += originalLines[startLineNum].length() + 1;
            startLineNum++;
        }

        for (int i = startLineNum; i <= originalLines.length - searchLines.length; i++) {
            boolean matches = true;

            for (int j = 0; j < searchLines.length; j++) {
                String originalTrimmed = originalLines[i + j].trim();
                String searchTrimmed = searchLines[j].trim();

                if (!originalTrimmed.equals(searchTrimmed)) {
                    matches = false;
                    break;
                }
            }

            if (matches) {
                int matchStartIndex = 0;
                for (int k = 0; k < i; k++) {
                    matchStartIndex += originalLines[k].length() + 1;
                }

                int matchEndIndex = matchStartIndex;
                for (int k = 0; k < searchLines.length; k++) {
                    matchEndIndex += originalLines[i + k].length() + 1;
                }

                return new int[] {matchStartIndex, matchEndIndex};
            }
        }

        return null;
    }

    /** 块锚点回退匹配 */
    private int[] blockAnchorFallbackMatch(
            String originalContent, String searchContent, int startIndex) {
        String[] originalLines = originalContent.split("\n");
        String[] searchLines = searchContent.split("\n");

        // 仅对 3+ 行的块使用此方法
        if (searchLines.length < 3) {
            return null;
        }

        if (searchLines.length > 0 && searchLines[searchLines.length - 1].isEmpty()) {
            searchLines = Arrays.copyOf(searchLines, searchLines.length - 1);
        }

        String firstLineSearch = searchLines[0].trim();
        String lastLineSearch = searchLines[searchLines.length - 1].trim();
        int searchBlockSize = searchLines.length;

        int startLineNum = 0;
        int currentIndex = 0;
        while (currentIndex < startIndex && startLineNum < originalLines.length) {
            currentIndex += originalLines[startLineNum].length() + 1;
            startLineNum++;
        }

        for (int i = startLineNum; i <= originalLines.length - searchBlockSize; i++) {
            if (!originalLines[i].trim().equals(firstLineSearch)) {
                continue;
            }

            if (!originalLines[i + searchBlockSize - 1].trim().equals(lastLineSearch)) {
                continue;
            }

            int matchStartIndex = 0;
            for (int k = 0; k < i; k++) {
                matchStartIndex += originalLines[k].length() + 1;
            }

            int matchEndIndex = matchStartIndex;
            for (int k = 0; k < searchBlockSize; k++) {
                matchEndIndex += originalLines[i + k].length() + 1;
            }

            return new int[] {matchStartIndex, matchEndIndex};
        }

        return null;
    }

    /** 处理状态枚举 */
    private enum ProcessingState {
        IDLE(0),
        STATE_SEARCH(1 << 0),
        STATE_REPLACE(1 << 1);

        private final int value;

        ProcessingState(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    /** 新文件内容构造器 V2 */
    private class NewFileContentConstructor {
        private final String originalContent;
        private final boolean isFinal;
        private final List<String> pendingNonStandardLines = new ArrayList<>();
        private final StringBuilder result = new StringBuilder();
        private final StringBuilder currentSearchContentBuilder = new StringBuilder();
        private int state = ProcessingState.IDLE.getValue();
        private int lastProcessedIndex = 0;
        private int searchMatchIndex = -1;
        private int searchEndIndex = -1;

        public NewFileContentConstructor(String originalContent, boolean isFinal) {
            this.originalContent = originalContent;
            this.isFinal = isFinal;
        }

        public void processLine(String line) {
            internalProcessLine(line, true, pendingNonStandardLines.size());
        }

        public String getResult() {
            if (isFinal && lastProcessedIndex < originalContent.length()) {
                result.append(originalContent.substring(lastProcessedIndex));
            }
            if (isFinal && state != ProcessingState.IDLE.getValue()) {
                throw new RuntimeException("文件处理不完整 - SEARCH/REPLACE 操作在最终化期间仍然活跃");
            }
            return result.toString();
        }

        private void resetForNextBlock() {
            state = ProcessingState.IDLE.getValue();
            currentSearchContentBuilder.setLength(0);
            searchMatchIndex = -1;
            searchEndIndex = -1;
        }

        private int findLastMatchingLineIndex(Pattern regex, int lineLimit) {
            for (int i = lineLimit; i > 0; ) {
                i--;
                if (regex.matcher(pendingNonStandardLines.get(i)).matches()) {
                    return i;
                }
            }
            return -1;
        }

        private void updateProcessingState(ProcessingState newState) {
            boolean isValidTransition =
                    (state == ProcessingState.IDLE.getValue()
                                    && newState == ProcessingState.STATE_SEARCH)
                            || (state == ProcessingState.STATE_SEARCH.getValue()
                                    && newState == ProcessingState.STATE_REPLACE);

            if (!isValidTransition) {
                throw new RuntimeException(
                        "无效状态转换。\n"
                                + "有效转换是:\n"
                                + "- Idle → StateSearch\n"
                                + "- StateSearch → StateReplace");
            }

            state |= newState.getValue();
        }

        private boolean isStateActive(ProcessingState state) {
            return (this.state & state.getValue()) == state.getValue();
        }

        private void activateReplaceState() {
            updateProcessingState(ProcessingState.STATE_REPLACE);
        }

        private void activateSearchState() {
            updateProcessingState(ProcessingState.STATE_SEARCH);
            currentSearchContentBuilder.setLength(0);
        }

        private boolean isSearchingActive() {
            return isStateActive(ProcessingState.STATE_SEARCH);
        }

        private boolean isReplacingActive() {
            return isStateActive(ProcessingState.STATE_REPLACE);
        }

        private boolean hasPendingNonStandardLines(int pendingNonStandardLineLimit) {
            return pendingNonStandardLineLimit < pendingNonStandardLines.size();
        }

        private int internalProcessLine(
                String line,
                boolean canWritePendingNonStandardLines,
                int pendingNonStandardLineLimit) {
            int removeLineCount = 0;
            if (isSearchBlockStart(line)) {
                removeLineCount =
                        trimPendingNonStandardTrailingEmptyLines(pendingNonStandardLineLimit);
                if (removeLineCount > 0) {
                    pendingNonStandardLineLimit = pendingNonStandardLineLimit - removeLineCount;
                }
                if (hasPendingNonStandardLines(pendingNonStandardLineLimit)) {
                    tryFixSearchReplaceBlock(pendingNonStandardLineLimit);
                    if (canWritePendingNonStandardLines) {
                        pendingNonStandardLines.clear();
                    }
                }
                activateSearchState();
            } else if (isSearchBlockEnd(line)) {
                if (!isSearchingActive()) {
                    tryFixSearchBlock(pendingNonStandardLineLimit);
                    if (canWritePendingNonStandardLines) {
                        pendingNonStandardLines.clear();
                    }
                }
                activateReplaceState();
                beforeReplace();
            } else if (isReplaceBlockEnd(line)) {
                if (!isReplacingActive()) {
                    tryFixReplaceBlock(pendingNonStandardLineLimit);
                    if (canWritePendingNonStandardLines) {
                        pendingNonStandardLines.clear();
                    }
                }
                lastProcessedIndex = searchEndIndex;
                resetForNextBlock();
            } else {
                if (isReplacingActive()) {
                    if (searchMatchIndex != -1) {
                        result.append(line).append("\n");
                    }
                } else if (isSearchingActive()) {
                    currentSearchContentBuilder.append(line).append("\n");
                } else {
                    if (canWritePendingNonStandardLines) {
                        pendingNonStandardLines.add(line);
                    }
                }
            }
            return removeLineCount;
        }

        private void beforeReplace() {
            String currentSearchContent = currentSearchContentBuilder.toString();

            if (currentSearchContent.isEmpty()) {
                if (originalContent.isEmpty()) {
                    searchMatchIndex = 0;
                    searchEndIndex = 0;
                } else {
                    searchMatchIndex = 0;
                    searchEndIndex = originalContent.length();
                }
            } else {
                int exactIndex = originalContent.indexOf(currentSearchContent, lastProcessedIndex);
                if (exactIndex != -1) {
                    searchMatchIndex = exactIndex;
                    searchEndIndex = exactIndex + currentSearchContent.length();
                } else {
                    int[] lineMatch =
                            lineTrimmedFallbackMatch(
                                    originalContent, currentSearchContent, lastProcessedIndex);
                    if (lineMatch != null) {
                        searchMatchIndex = lineMatch[0];
                        searchEndIndex = lineMatch[1];
                    } else {
                        int[] blockMatch =
                                blockAnchorFallbackMatch(
                                        originalContent, currentSearchContent, lastProcessedIndex);
                        if (blockMatch != null) {
                            searchMatchIndex = blockMatch[0];
                            searchEndIndex = blockMatch[1];
                        } else {
                            throw new RuntimeException(
                                    "SEARCH 块不匹配文件中的任何内容: " + currentSearchContent.trim());
                        }
                    }
                }
            }

            if (searchMatchIndex < lastProcessedIndex) {
                throw new RuntimeException("SEARCH 块匹配了文件中不正确的内容: " + currentSearchContent.trim());
            }

            result.append(originalContent, lastProcessedIndex, searchMatchIndex);
        }

        private int tryFixSearchBlock(int lineLimit) {
            int removeLineCount = 0;
            if (lineLimit < 0) {
                lineLimit = pendingNonStandardLines.size();
            }
            if (lineLimit == 0) {
                throw new RuntimeException("无效的 SEARCH/REPLACE 块结构 - 没有可处理的行");
            }
            int searchTagIndex = findLastMatchingLineIndex(SEARCH_TAG_REGEX, lineLimit);
            if (searchTagIndex != -1) {
                List<String> fixLines =
                        new ArrayList<>(pendingNonStandardLines.subList(searchTagIndex, lineLimit));
                fixLines.set(0, SEARCH_BLOCK_START);
                for (String line : fixLines) {
                    removeLineCount += internalProcessLine(line, false, searchTagIndex);
                }
            } else {
                throw new RuntimeException(
                        "检测到无效的 REPLACE 标记 - 无法找到匹配的 SEARCH 块，从第 " + (searchTagIndex + 1) + " 行开始");
            }
            return removeLineCount;
        }

        private int tryFixReplaceBlock(int lineLimit) {
            int removeLineCount = 0;
            if (lineLimit < 0) {
                lineLimit = pendingNonStandardLines.size();
            }
            if (lineLimit == 0) {
                throw new RuntimeException("无效的 SEARCH/REPLACE 块结构 - 没有可处理的行");
            }
            int replaceBeginTagIndex =
                    findLastMatchingLineIndex(REPLACE_BEGIN_TAG_REGEX, lineLimit);
            if (replaceBeginTagIndex != -1) {
                List<String> fixLines =
                        new ArrayList<>(
                                pendingNonStandardLines.subList(
                                        replaceBeginTagIndex - removeLineCount,
                                        lineLimit - removeLineCount));
                fixLines.set(0, SEARCH_BLOCK_END);
                for (String line : fixLines) {
                    removeLineCount +=
                            internalProcessLine(
                                    line, false, replaceBeginTagIndex - removeLineCount);
                }
            } else {
                throw new RuntimeException(
                        "格式错误的 REPLACE 块 - 第 " + (replaceBeginTagIndex + 1) + " 行后缺少有效的分隔符");
            }
            return removeLineCount;
        }

        private int tryFixSearchReplaceBlock(int lineLimit) {
            int removeLineCount = 0;
            if (lineLimit < 0) {
                lineLimit = pendingNonStandardLines.size();
            }
            if (lineLimit == 0) {
                throw new RuntimeException("无效的 SEARCH/REPLACE 块结构 - 没有可处理的行");
            }

            int replaceEndTagIndex = findLastMatchingLineIndex(REPLACE_END_TAG_REGEX, lineLimit);
            boolean likeReplaceEndTag = replaceEndTagIndex == lineLimit - 1;
            if (likeReplaceEndTag) {
                List<String> fixLines =
                        new ArrayList<>(
                                pendingNonStandardLines.subList(
                                        replaceEndTagIndex - removeLineCount,
                                        lineLimit - removeLineCount));
                fixLines.set(fixLines.size() - 1, REPLACE_BLOCK_END);
                for (String line : fixLines) {
                    removeLineCount +=
                            internalProcessLine(line, false, replaceEndTagIndex - removeLineCount);
                }
            } else {
                throw new RuntimeException("格式错误的 SEARCH/REPLACE 块结构: 缺少有效的结束 REPLACE 标记");
            }
            return removeLineCount;
        }

        private int trimPendingNonStandardTrailingEmptyLines(int lineLimit) {
            int removedCount = 0;
            int i = Math.min(lineLimit, pendingNonStandardLines.size()) - 1;

            while (i >= 0 && pendingNonStandardLines.get(i).trim().isEmpty()) {
                pendingNonStandardLines.remove(pendingNonStandardLines.size() - 1);
                removedCount++;
                i--;
            }

            return removedCount;
        }

        private boolean isSearchBlockStart(String line) {
            return SEARCH_BLOCK_START_REGEX.matcher(line).matches();
        }

        private boolean isSearchBlockEnd(String line) {
            return SEARCH_BLOCK_END_REGEX.matcher(line).matches();
        }

        private boolean isReplaceBlockEnd(String line) {
            return REPLACE_BLOCK_END_REGEX.matcher(line).matches();
        }
    }
}
