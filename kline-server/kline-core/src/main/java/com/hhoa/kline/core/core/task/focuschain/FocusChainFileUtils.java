package com.hhoa.kline.core.core.task.focuschain;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public final class FocusChainFileUtils {

    private FocusChainFileUtils() {}

    private static final String TASKS_BASE_PATH =
            System.getProperty("user.home") + "/.cline/data/tasks";

    public static String getFocusChainFilePath(String taskDir, String taskId) {
        return Paths.get(taskDir, "focus_chain_taskid_" + taskId + ".md").toString();
    }

    public static String createFocusChainMarkdownContent(String taskId, String focusChainList) {
        return "# Focus Chain List for Task "
                + taskId
                + "\n\n"
                + "<!-- Edit this markdown file to update your focus chain list -->\n"
                + "<!-- Use the format: - [ ] for incomplete items and - [x] for completed items -->\n\n"
                + focusChainList
                + "\n\n"
                + "<!-- Save this file and the focus chain list will be updated in the task -->";
    }

    public static List<String> extractFocusChainItemsFromText(String text) {
        String[] lines = text.split("\n");
        List<String> result = new ArrayList<>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (isFocusChainItem(trimmed)) {
                result.add(line);
            }
        }
        return result;
    }

    public static String extractFocusChainListFromText(String text) {
        List<String> lines = extractFocusChainItemsFromText(text);
        return lines.isEmpty() ? null : String.join("\n", lines);
    }

    public static String ensureFocusChainFile(String taskId, String initialFocusChainContent) {
        Path taskDir = ensureTaskDirectoryExists(taskId);
        String focusChainFilePath = getFocusChainFilePath(taskDir.toString(), taskId);

        Path filePath = Paths.get(focusChainFilePath);
        if (!Files.exists(filePath)) {
            String focusChainContent =
                    initialFocusChainContent != null
                            ? initialFocusChainContent
                            : "- [ ] Example checklist item\n"
                                    + "- [ ] Another checklist item\n"
                                    + "- [x] Completed example item";
            String fileContent = createFocusChainMarkdownContent(taskId, focusChainContent);
            try {
                Files.write(filePath, fileContent.getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new RuntimeException("Failed to create focus chain file: " + filePath, e);
            }
        }

        return focusChainFilePath;
    }

    private static Path ensureTaskDirectoryExists(String taskId) {
        Path dir = Paths.get(TASKS_BASE_PATH, taskId);
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create task directory: " + dir, e);
        }
        return dir;
    }

    private static boolean isFocusChainItem(String line) {
        // 匹配 "- [ ] 项" 或 "- [x] 项"（x 大小写均可）
        if (line.length() < 6) return false;
        if (!line.startsWith("- [")) return false;
        if (line.length() < 5 || line.charAt(4) != ']') return false;
        char c = line.charAt(3);
        return c == ' ' || c == 'x' || c == 'X';
    }
}
