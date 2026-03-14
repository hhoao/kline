package com.hhoa.kline.core.core.task.focuschain;

/** Focus Chain 工具类：提供解析和统计功能。 */
public final class FocusChainUtils {

    private FocusChainUtils() {}

    /** Focus Chain 列表计数结果 */
    public record TodoListCounts(int totalItems, int completedItems) {}

    /**
     * 解析 Focus Chain 列表字符串并返回总项数和已完成项数
     *
     * @param todoList Focus Chain 列表字符串
     * @return 包含 totalItems 和 completedItems 的对象
     */
    public static TodoListCounts parseFocusChainListCounts(String todoList) {
        if (todoList == null || todoList.isBlank()) {
            return new TodoListCounts(0, 0);
        }

        String[] lines = todoList.split("\n");
        int totalItems = 0;
        int completedItems = 0;

        for (String line : lines) {
            String trimmed = line.trim();
            if (isFocusChainItem(trimmed)) {
                totalItems++;
                if (isCompletedFocusChainItem(trimmed)) {
                    completedItems++;
                }
            }
        }

        return new TodoListCounts(totalItems, completedItems);
    }

    /**
     * 检查一行是否是 Focus Chain 项
     *
     * @param line 修剪后的行
     * @return 如果是 Focus Chain 项返回 true
     */
    public static boolean isFocusChainItem(String line) {
        return line.startsWith("- [ ]") || line.startsWith("- [x]") || line.startsWith("- [X]");
    }

    /**
     * 检查一行是否是已完成的 Focus Chain 项
     *
     * @param line 修剪后的行
     * @return 如果是已完成的 Focus Chain 项返回 true
     */
    public static boolean isCompletedFocusChainItem(String line) {
        return line.startsWith("- [x]") || line.startsWith("- [X]");
    }
}
