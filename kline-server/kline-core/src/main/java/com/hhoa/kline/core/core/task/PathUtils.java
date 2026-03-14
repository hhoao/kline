package com.hhoa.kline.core.core.task;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class PathUtils {

    private PathUtils() {}

    /**
     * @return 桌面目录路径
     */
    public static String getDesktopDir() {
        String os = System.getProperty("os.name").toLowerCase();
        String home = System.getProperty("user.home");

        if (os.contains("win")) {
            // Windows: 尝试从环境变量获取
            String desktop = System.getenv("USERPROFILE");
            if (desktop != null) {
                return Paths.get(desktop, "Desktop").toString();
            }
            return Paths.get(home, "Desktop").toString();
        } else {
            // Linux/Mac: 使用用户主目录下的 Desktop
            return Paths.get(home, "Desktop").toString();
        }
    }

    /**
     * @param baseDir 基础目录（如果为 null，使用系统属性）
     * @return 当前工作目录路径
     */
    public static String getCwd(String baseDir) {
        if (baseDir != null && !baseDir.isEmpty()) {
            return baseDir;
        }
        return System.getProperty("user.dir");
    }

    /**
     * @param dirPath 目录路径
     * @return 目录大小（字节），如果出错返回 0
     */
    public static long getFolderSize(String dirPath) {
        if (dirPath == null || dirPath.isEmpty()) {
            return 0;
        }

        try {
            Path path = Paths.get(dirPath);
            if (!Files.exists(path) || !Files.isDirectory(path)) {
                return 0;
            }

            FolderSizeVisitor visitor = new FolderSizeVisitor();
            Files.walkFileTree(path, visitor);
            return visitor.getTotalSize();
        } catch (Exception e) {
            log.debug("Failed to get folder size for {}: {}", dirPath, e.getMessage());
            return 0;
        }
    }

    /** 文件访问器：用于递归计算目录大小 */
    private static class FolderSizeVisitor extends SimpleFileVisitor<Path> {
        private long totalSize = 0;

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            if (attrs.isRegularFile()) {
                totalSize += attrs.size();
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
            return FileVisitResult.CONTINUE;
        }

        public long getTotalSize() {
            return totalSize;
        }
    }
}
