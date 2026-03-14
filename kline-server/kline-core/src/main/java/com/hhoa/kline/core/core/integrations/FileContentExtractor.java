package com.hhoa.kline.core.core.integrations;

import com.hhoa.kline.core.core.integrations.misc.ExtractImageContent;
import com.hhoa.kline.core.core.integrations.misc.ExtractText;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * 文件内容提取工具 支持文本文件和图片文件的提取 整合了 extract-file-content.ts 的功能
 *
 * @author hhoa
 */
@Slf4j
public class FileContentExtractor {

    public static class FileContentResult {
        public String text;

        public ImageBlock imageBlock;

        public FileContentResult(String text) {
            this.text = text;
        }

        public FileContentResult(ImageBlock imageBlock) {
            this.imageBlock = imageBlock;
            this.text = "Successfully read image";
        }
    }

    public static class ImageBlock {
        public String type = "image";
        public ImageSource source;

        public ImageBlock(ImageSource source) {
            this.source = source;
        }
    }

    public static class ImageSource {
        public String type = "base64";
        public String mediaType;
        public String data;

        public ImageSource(String mediaType, String data) {
            this.mediaType = mediaType;
            this.data = data;
        }
    }

    /**
     * 提取文件内容 处理文本文件和图片文件
     *
     * @param filePath 文件绝对路径
     * @param modelSupportsImages 模型是否支持图片
     * @return 文件内容结果
     */
    public static FileContentResult extractFileContent(Path filePath, boolean modelSupportsImages) {
        try {
            if (!Files.exists(filePath)) {
                throw new IOException("File not found: " + filePath);
            }

            String fileName = filePath.getFileName().toString().toLowerCase();
            String extension = getFileExtension(fileName);
            boolean isImage = isImageExtension(extension);

            if (isImage && modelSupportsImages) {
                ExtractImageContent.ImageExtractionResult imageResult =
                        ExtractImageContent.extractImageContent(filePath);

                if (imageResult.isSuccess()) {
                    ExtractImageContent.ImageBlock imageBlock = imageResult.getImageBlock();
                    ImageSource source =
                            new ImageSource(
                                    imageBlock.getSource().getMediaType(),
                                    imageBlock.getSource().getData());
                    ImageBlock resultImageBlock = new ImageBlock(source);

                    return new FileContentResult(resultImageBlock);
                } else {
                    throw new IOException(imageResult.getError());
                }
            } else if (isImage && !modelSupportsImages) {
                throw new IOException("Current model does not support image input");
            } else {
                try {
                    String textContent = ExtractText.extractTextFromFile(filePath);
                    return new FileContentResult(textContent);
                } catch (Exception e) {
                    String errorMessage = e.getMessage() != null ? e.getMessage() : "Unknown error";
                    throw new IOException("Error reading file: " + errorMessage, e);
                }
            }
        } catch (IOException e) {
            log.error("Failed to extract file content: {}", filePath, e);
            throw new RuntimeException("Failed to extract file content: " + e.getMessage(), e);
        }
    }

    /**
     * 判断是否为图片扩展名
     *
     * @param extension 文件扩展名
     * @return 是否为图片
     */
    private static boolean isImageExtension(String extension) {
        List<String> imageExtensions = List.of("png", "jpg", "jpeg", "webp");
        return imageExtensions.contains(extension.toLowerCase());
    }

    /**
     * 获取文件扩展名
     *
     * @param fileName 文件名
     * @return 扩展名（不含点）
     */
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
}
