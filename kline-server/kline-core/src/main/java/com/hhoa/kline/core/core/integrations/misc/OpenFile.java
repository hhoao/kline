package com.hhoa.kline.core.core.integrations.misc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

/**
 * 打开文件工具类
 *
 * @author hhoa
 */
@Slf4j
public class OpenFile {

    private static final Pattern DATA_URI_PATTERN =
            Pattern.compile("^data:image/([a-zA-Z]+);base64,(.+)$");

    /**
     * 打开图片（从 Data URI）
     *
     * @param dataUri 图片的 Data URI
     * @param tempDir 临时目录
     * @return 临时文件路径，如果失败返回 null
     */
    public static Path openImage(String dataUri, Path tempDir) {
        Matcher matcher = DATA_URI_PATTERN.matcher(dataUri);
        if (!matcher.matches()) {
            log.error("Invalid data URI format");
            return null;
        }

        String format = matcher.group(1);
        String base64Data = matcher.group(2);

        try {
            byte[] imageBuffer = Base64.getDecoder().decode(base64Data);
            Path tempFilePath =
                    tempDir.resolve("temp_image_" + System.currentTimeMillis() + "." + format);

            Files.createDirectories(tempFilePath.getParent());
            Files.write(tempFilePath, imageBuffer);

            return tempFilePath;
        } catch (IOException e) {
            log.error("Error opening image", e);
            return null;
        }
    }

    public static void openFile(Path absolutePath, boolean preserveFocus, boolean preview) {
        if (absolutePath == null || !Files.exists(absolutePath)) {
            log.error("Could not open file! File does not exist: {}", absolutePath);
            return;
        }

        log.info(
                "Opening file: {} (preserveFocus: {}, preview: {})",
                absolutePath,
                preserveFocus,
                preview);
    }
}
