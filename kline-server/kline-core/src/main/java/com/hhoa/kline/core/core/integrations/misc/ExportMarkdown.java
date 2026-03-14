package com.hhoa.kline.core.core.integrations.misc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * 导出 Markdown 工具类 用于将对话历史导出为 Markdown 文件
 *
 * @author hhoa
 */
@Slf4j
public class ExportMarkdown {

    public interface ContentBlock {
        String formatToMarkdown();
    }

    public static class TextBlock implements ContentBlock {
        private final String text;

        public TextBlock(String text) {
            this.text = text;
        }

        @Override
        public String formatToMarkdown() {
            return text;
        }
    }

    public static class ImageBlock implements ContentBlock {
        @Override
        public String formatToMarkdown() {
            return "[Image]";
        }
    }

    public static class DocumentBlock implements ContentBlock {
        @Override
        public String formatToMarkdown() {
            return "[Document]";
        }
    }

    public static class ToolUseBlock implements ContentBlock {
        private final String name;
        private final Object input;

        public ToolUseBlock(String name, Object input) {
            this.name = name;
            this.input = input;
        }

        @Override
        public String formatToMarkdown() {
            StringBuilder sb = new StringBuilder();
            sb.append("[Tool Use: ").append(name).append("]\n");

            if (input != null) {
                if (input instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> map = (Map<String, Object>) input;
                    for (Map.Entry<String, Object> entry : map.entrySet()) {
                        String key = entry.getKey();
                        String capitalizedKey =
                                key.substring(0, 1).toUpperCase() + key.substring(1);
                        sb.append(capitalizedKey)
                                .append(": ")
                                .append(entry.getValue())
                                .append("\n");
                    }
                } else {
                    sb.append(input);
                }
            }

            return sb.toString();
        }
    }

    public static class ToolResultBlock implements ContentBlock {
        private final Object content;
        private final boolean isError;

        public ToolResultBlock(Object content, boolean isError) {
            this.content = content;
            this.isError = isError;
        }

        @Override
        public String formatToMarkdown() {
            StringBuilder sb = new StringBuilder();
            sb.append("[Tool");
            if (isError) {
                sb.append(" (Error)");
            }
            sb.append("]\n");

            if (content != null) {
                if (content instanceof String) {
                    sb.append(content);
                } else if (content instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<ContentBlock> blocks = (List<ContentBlock>) content;
                    for (ContentBlock block : blocks) {
                        sb.append(block.formatToMarkdown()).append("\n");
                    }
                }
            }

            return sb.toString();
        }
    }

    public record MessageParam(String role, Object content) {}

    /**
     * 下载任务（导出对话历史为 Markdown）
     *
     * @param dateTs 时间戳（毫秒）
     * @param conversationHistory 对话历史
     * @param savePath 保存路径
     * @throws IOException 如果保存失败
     */
    public static void downloadTask(
            long dateTs, List<MessageParam> conversationHistory, Path savePath) throws IOException {
        String fileName = generateFileName(dateTs);

        if (Files.isDirectory(savePath)) {
            savePath = savePath.resolve(fileName);
        } else if (savePath.getFileName() == null
                || !savePath.getFileName().toString().endsWith(".md")) {
            savePath = savePath.resolveSibling(fileName);
        }

        String markdownContent = generateMarkdown(conversationHistory);

        Files.writeString(savePath, markdownContent);
        log.info("Markdown file saved to: {}", savePath);
    }

    private static String generateFileName(long dateTs) {
        LocalDateTime date =
                LocalDateTime.ofInstant(Instant.ofEpochMilli(dateTs), ZoneId.systemDefault());

        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH);
        String month = date.format(monthFormatter).toLowerCase();
        int day = date.getDayOfMonth();
        int year = date.getYear();

        int hours = date.getHour();
        int minutes = date.getMinute();
        int seconds = date.getSecond();

        String ampm = hours >= 12 ? "pm" : "am";
        hours = hours % 12;
        hours = hours == 0 ? 12 : hours;

        return String.format(
                "cline_task_%s-%d-%d_%d-%02d-%02d-%s.md",
                month, day, year, hours, minutes, seconds, ampm);
    }

    private static String generateMarkdown(List<MessageParam> conversationHistory) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < conversationHistory.size(); i++) {
            MessageParam message = conversationHistory.get(i);
            String role = "user".equals(message.role()) ? "**User:**" : "**Assistant:**";

            String content;
            if (message.content() instanceof List) {
                @SuppressWarnings("unchecked")
                List<ContentBlock> blocks = (List<ContentBlock>) message.content();
                content =
                        blocks.stream()
                                .map(ContentBlock::formatToMarkdown)
                                .collect(Collectors.joining("\n"));
            } else {
                content = message.content().toString();
            }

            sb.append(role).append("\n\n").append(content).append("\n\n");

            if (i < conversationHistory.size() - 1) {
                sb.append("---\n\n");
            }
        }

        return sb.toString();
    }

    /**
     * 格式化内容块为 Markdown
     *
     * @param block 内容块
     * @return Markdown 字符串
     */
    public static String formatContentBlockToMarkdown(ContentBlock block) {
        if (block == null) {
            return "[Unexpected content type]";
        }

        return block.formatToMarkdown();
    }
}
