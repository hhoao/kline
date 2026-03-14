package com.hhoa.kline.core.core.integrations.checkpoints;

import com.hhoa.kline.core.core.storage.GlobalFileNames;
import com.hhoa.kline.core.core.task.PathUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CheckpointUtils {

    public static Path getShadowGitPath(String cwdHash) throws IOException {
        Path checkpointsDir = Paths.get(GlobalFileNames.BASE_DIR, "checkpoints", cwdHash);
        Files.createDirectories(checkpointsDir);
        return checkpointsDir.resolve(".git");
    }

    public static void validateWorkspacePath(String workspacePath) throws IOException {
        Path path = Paths.get(workspacePath);
        if (!Files.exists(path) || !Files.isReadable(path)) {
            throw new IOException(
                    "Cannot access workspace directory. Please ensure VS Code has permission to access your workspace.");
        }

        String homedir = System.getProperty("user.home");
        String desktopPath = PathUtils.getDesktopDir();
        String documentsPath = Paths.get(homedir, "Documents").toString();
        String downloadsPath = Paths.get(homedir, "Downloads").toString();

        String normalizedPath = path.normalize().toString();
        if (normalizedPath.equals(homedir)) {
            throw new IOException("Cannot use checkpoints in home directory");
        }
        if (normalizedPath.equals(desktopPath)) {
            throw new IOException("Cannot use checkpoints in Desktop directory");
        }
        if (normalizedPath.equals(documentsPath)) {
            throw new IOException("Cannot use checkpoints in Documents directory");
        }
        if (normalizedPath.equals(downloadsPath)) {
            throw new IOException("Cannot use checkpoints in Downloads directory");
        }
    }

    public static String getWorkingDirectory(String baseDir) throws IOException {
        validateWorkspacePath(baseDir);
        return baseDir;
    }

    public static String hashWorkingDir(String workingDir) {
        if (workingDir == null || workingDir.isEmpty()) {
            throw new IllegalArgumentException("Working directory path cannot be empty");
        }
        int hash = 0;
        for (int i = 0; i < workingDir.length(); i++) {
            hash = (hash * 31 + workingDir.charAt(i));
        }
        long unsignedHash = Integer.toUnsignedLong(hash);
        String numericHash = String.valueOf(unsignedHash);
        return numericHash.length() > 13 ? numericHash.substring(0, 13) : numericHash;
    }
}
