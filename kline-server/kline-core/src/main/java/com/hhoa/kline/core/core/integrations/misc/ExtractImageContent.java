package com.hhoa.kline.core.core.integrations.misc;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import javax.imageio.ImageIO;
import lombok.extern.slf4j.Slf4j;

/**
 * 提取图片内容工具类
 *
 * @author hhoa
 */
@Slf4j
public class ExtractImageContent {

    private static final int MAX_IMAGE_DIMENSION = 7500;

    public static class ImageExtractionResult {
        private final boolean success;
        private final ImageBlock imageBlock;
        private final String error;

        private ImageExtractionResult(boolean success, ImageBlock imageBlock, String error) {
            this.success = success;
            this.imageBlock = imageBlock;
            this.error = error;
        }

        public static ImageExtractionResult success(ImageBlock imageBlock) {
            return new ImageExtractionResult(true, imageBlock, null);
        }

        public static ImageExtractionResult failure(String error) {
            return new ImageExtractionResult(false, null, error);
        }

        public boolean isSuccess() {
            return success;
        }

        public ImageBlock getImageBlock() {
            return imageBlock;
        }

        public String getError() {
            return error;
        }
    }

    public static class ImageBlock {
        private final String type = "image";
        private final ImageSource source;

        public ImageBlock(ImageSource source) {
            this.source = source;
        }

        public String getType() {
            return type;
        }

        public ImageSource getSource() {
            return source;
        }
    }

    public static class ImageSource {
        private final String type = "base64";
        private final String mediaType;
        private final String data;

        public ImageSource(String mediaType, String data) {
            this.mediaType = mediaType;
            this.data = data;
        }

        public String getType() {
            return type;
        }

        public String getMediaType() {
            return mediaType;
        }

        public String getData() {
            return data;
        }
    }

    /**
     * 提取图片内容
     *
     * @param filePath 文件路径
     * @return 图片提取结果
     */
    public static ImageExtractionResult extractImageContent(Path filePath) {
        try {
            if (!Files.exists(filePath)) {
                return ImageExtractionResult.failure("File not found: " + filePath);
            }

            byte[] imageBytes = Files.readAllBytes(filePath);

            BufferedImage image = ImageIO.read(Files.newInputStream(filePath));
            if (image == null) {
                return ImageExtractionResult.failure(
                        "Could not determine image dimensions, so image could not be read");
            }

            int width = image.getWidth();
            int height = image.getHeight();

            if (width > MAX_IMAGE_DIMENSION || height > MAX_IMAGE_DIMENSION) {
                return ImageExtractionResult.failure(
                        "Image dimensions exceed "
                                + MAX_IMAGE_DIMENSION
                                + "px by "
                                + MAX_IMAGE_DIMENSION
                                + "px, so image could not be read");
            }

            String base64 = Base64.getEncoder().encodeToString(imageBytes);
            String mimeType = getMimeType(filePath);

            ImageSource source = new ImageSource(mimeType, base64);
            ImageBlock imageBlock = new ImageBlock(source);

            return ImageExtractionResult.success(imageBlock);

        } catch (IOException e) {
            log.error("Error reading image: {}", filePath, e);
            return ImageExtractionResult.failure("Error reading image: " + e.getMessage());
        }
    }

    /**
     * 根据文件路径获取 MIME 类型
     *
     * @param filePath 文件路径
     * @return MIME 类型
     */
    private static String getMimeType(Path filePath) {
        String fileName = filePath.getFileName().toString().toLowerCase();
        if (fileName.endsWith(".png")) {
            return "image/png";
        } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (fileName.endsWith(".webp")) {
            return "image/webp";
        } else {
            throw new IllegalArgumentException("Unsupported image type: " + fileName);
        }
    }
}
