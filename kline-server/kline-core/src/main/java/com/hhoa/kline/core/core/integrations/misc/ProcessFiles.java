package com.hhoa.kline.core.core.integrations.misc;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import javax.imageio.ImageIO;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * 文件处理工具类 支持图片和其他文件类型的处理
 *
 * @author hhoa
 */
@Slf4j
public class ProcessFiles {

    private static final List<String> IMAGE_EXTENSIONS =
            Arrays.asList("png", "jpg", "jpeg", "webp");

    private static final List<String> OTHER_FILE_EXTENSIONS =
            Arrays.asList("xml", "json", "txt", "log", "md", "docx", "ipynb", "pdf", "xlsx", "csv");

    private static final int MAX_IMAGE_DIMENSION = 7680;

    private static final long MAX_FILE_SIZE = 20 * 1000 * 1024L;

    @Data
    public static class FileProcessingResult {
        private List<String> images;
        private List<String> files;

        public FileProcessingResult() {
            this.images = new ArrayList<>();
            this.files = new ArrayList<>();
        }
    }

    public static FileProcessingResult selectFiles(List<Path> filePaths, boolean imagesAllowed) {
        FileProcessingResult result = new FileProcessingResult();

        if (filePaths == null || filePaths.isEmpty()) {
            return result;
        }

        for (Path filePath : filePaths) {
            try {
                String fileName = filePath.getFileName().toString().toLowerCase();
                String extension = getFileExtension(fileName);

                boolean isImage = IMAGE_EXTENSIONS.contains(extension);

                if (isImage) {
                    if (!imagesAllowed) {
                        log.warn("Images not allowed, skipping: {}", filePath);
                        continue;
                    }

                    String imageData = processImage(filePath);
                    if (imageData != null) {
                        result.getImages().add(imageData);
                    }
                } else {
                    if (OTHER_FILE_EXTENSIONS.contains(extension) || extension.isEmpty()) {
                        long size = Files.size(filePath);
                        if (size > MAX_FILE_SIZE) {
                            log.warn(
                                    "File too large, skipping: {} (size: {} bytes)",
                                    filePath,
                                    size);
                            continue;
                        }
                        result.getFiles().add(filePath.toString());
                    } else {
                        log.warn("Unsupported file type: {}", filePath);
                    }
                }
            } catch (Exception e) {
                log.error("Error processing file: {}", filePath, e);
            }
        }

        return result;
    }

    private static String processImage(Path filePath) {
        try {
            byte[] buffer = Files.readAllBytes(filePath);

            BufferedImage image = ImageIO.read(Files.newInputStream(filePath));
            if (image == null) {
                log.warn("Could not read image dimensions for: {}", filePath);
                return null;
            }

            int width = image.getWidth();
            int height = image.getHeight();

            if (width > MAX_IMAGE_DIMENSION || height > MAX_IMAGE_DIMENSION) {
                log.warn(
                        "Image dimensions exceed {}px, skipping: {}",
                        MAX_IMAGE_DIMENSION,
                        filePath);
                return null;
            }

            String base64 = Base64.getEncoder().encodeToString(buffer);
            String mimeType = getMimeType(filePath);

            return "data:" + mimeType + ";base64," + base64;

        } catch (IOException e) {
            log.error("Error reading file or getting dimensions for: {}", filePath, e);
            return null;
        }
    }

    private static String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0 && lastDot < fileName.length() - 1) {
            return fileName.substring(lastDot + 1).toLowerCase();
        }
        return "";
    }

    public static String getMimeType(Path filePath) {
        String fileName = filePath.getFileName().toString().toLowerCase();
        if (fileName.endsWith(".png")) {
            return "image/png";
        } else if (fileName.endsWith(".jpeg") || fileName.endsWith(".jpg")) {
            return "image/jpeg";
        } else if (fileName.endsWith(".webp")) {
            return "image/webp";
        } else {
            throw new IllegalArgumentException("Unsupported file type: " + fileName);
        }
    }
}
