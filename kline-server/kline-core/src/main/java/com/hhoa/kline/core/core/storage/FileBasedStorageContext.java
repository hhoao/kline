package com.hhoa.kline.core.core.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/** 基于文件系统的存储上下文实现 */
@Slf4j
public class FileBasedStorageContext implements StorageContext {
    final String globalStoragePath;
    final List<String> workspaceRoots;

    public FileBasedStorageContext(String globalStoragePath, List<String> workspaceRoots) {
        this.globalStoragePath = globalStoragePath;
        this.workspaceRoots =
                workspaceRoots != null ? new ArrayList<>(workspaceRoots) : new ArrayList<>();
        try {
            Files.createDirectories(Paths.get(globalStoragePath));

            for (String workspaceRoot : workspaceRoots) {
                Files.createDirectories(Paths.get(workspaceRoot));
            }
        } catch (IOException e) {
            throw new RuntimeException("无法创建存储目录: " + globalStoragePath, e);
        }
    }

    @Override
    public String getGlobalStoragePath() {
        return globalStoragePath;
    }

    @Override
    public List<String> getWorkspaceRoots() {
        return new ArrayList<>(workspaceRoots);
    }
}
