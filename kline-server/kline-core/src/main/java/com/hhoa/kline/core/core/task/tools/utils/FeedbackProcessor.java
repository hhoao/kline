package com.hhoa.kline.core.core.task.tools.utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * 用户反馈处理工具。 用于处理文本、图片和文件反馈。
 *
 * @author hhoa
 */
@Slf4j
public final class FeedbackProcessor {

    private FeedbackProcessor() {}

    @Data
    @lombok.AllArgsConstructor
    public static class FileInfo {
        private String path;
        private String name;
        private String type;
        private long size;
    }

    @Data
    @lombok.AllArgsConstructor
    public static class ImageInfo {
        private String path;
        private String base64Data;
        private String mimeType;
        private int width;
        private int height;
    }

    public static String processFilesIntoText(List<FileInfo> files) {
        if (files == null || files.isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        result.append("\n\n=== Attached Files ===\n\n");

        for (FileInfo file : files) {
            try {
                result.append("File: ").append(file.name).append("\n");
                result.append("Path: ").append(file.path).append("\n");
                result.append("Type: ").append(file.type).append("\n");
                result.append("Size: ").append(formatFileSize(file.size)).append("\n");
                result.append("Content:\n");
                result.append("```\n");

                String content = readFileContent(file.path);

                if (content.length() > 10000) {
                    content = content.substring(0, 10000) + "\n... [Content truncated]";
                }

                result.append(content);
                result.append("\n```\n\n");

            } catch (Exception e) {
                log.error("Failed to process file: {}", file.path, e);
                result.append("Error reading file: ").append(e.getMessage()).append("\n\n");
            }
        }

        result.append("=== End of Attached Files ===\n");
        return result.toString();
    }

    public static String processImagesIntoText(List<ImageInfo> images) {
        if (images == null || images.isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        result.append("\n\n=== Attached Images ===\n\n");

        for (int i = 0; i < images.size(); i++) {
            ImageInfo image = images.get(i);
            result.append("Image ").append(i + 1).append(":\n");
            result.append("  Path: ").append(image.path).append("\n");
            result.append("  Type: ").append(image.mimeType).append("\n");
            result.append("  Size: ")
                    .append(image.width)
                    .append("x")
                    .append(image.height)
                    .append("\n");
            result.append("  [Image data available for AI processing]\n\n");
        }

        result.append("=== End of Attached Images ===\n");
        return result.toString();
    }

    public static String combineFeedback(
            String text, List<ImageInfo> images, List<FileInfo> files) {
        StringBuilder result = new StringBuilder();

        if (text != null && !text.isEmpty()) {
            result.append(text);
        }

        if (images != null && !images.isEmpty()) {
            result.append(processImagesIntoText(images));
        }

        if (files != null && !files.isEmpty()) {
            result.append(processFilesIntoText(files));
        }

        return result.toString();
    }

    private static String readFileContent(String filePath) throws IOException {
        Path path = Paths.get(filePath);

        if (!Files.exists(path)) {
            return "[File not found]";
        }

        long size = Files.size(path);
        if (size > 1024 * 1024) { // 1MB
            return "[File too large to display]";
        }

        String mimeType = Files.probeContentType(path);
        if (mimeType != null && !mimeType.startsWith("text/")) {
            return "[Binary file - content not displayed]";
        }

        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private static String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.2f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", size / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", size / (1024.0 * 1024.0 * 1024.0));
        }
    }

    public static boolean isPathSafe(String filePath, String baseDir) {
        try {
            Path path = Paths.get(filePath).normalize().toAbsolutePath();
            Path base = Paths.get(baseDir).normalize().toAbsolutePath();
            return path.startsWith(base);
        } catch (Exception e) {
            log.error("Failed to validate path: {}", filePath, e);
            return false;
        }
    }

    public static String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }

        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0 && lastDot < fileName.length() - 1) {
            return fileName.substring(lastDot + 1).toLowerCase();
        }

        return "";
    }

    public static boolean isTextFile(String fileName) {
        String ext = getFileExtension(fileName);
        List<String> textExtensions =
                List.of(
                        "txt", "md", "java", "py", "js", "ts", "jsx", "tsx", "html", "css", "scss",
                        "json", "xml", "yaml", "yml", "sh", "bat", "cmd", "ps1", "sql", "log",
                        "csv", "c", "cpp", "h", "hpp", "go", "rs", "kt", "swift");
        return textExtensions.contains(ext);
    }

    public static boolean isImageFile(String fileName) {
        String ext = getFileExtension(fileName);
        List<String> imageExtensions =
                List.of("jpg", "jpeg", "png", "gif", "bmp", "webp", "svg", "ico");
        return imageExtensions.contains(ext);
    }
}
