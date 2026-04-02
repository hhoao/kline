package com.hhoa.kline.core.core.context.tracking;

import com.hhoa.kline.core.core.storage.StateManager;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/** 环境上下文追踪器，记录 OS/环境元数据。 */
@Slf4j
public class EnvironmentContextTracker {

    private final String taskId;
    private final StateManager stateManager;

    public EnvironmentContextTracker(String taskId, StateManager stateManager) {
        this.taskId = taskId;
        this.stateManager = stateManager;
    }

    public void recordEnvironment(String clineVersion) {
        try {
            TaskMetadata metadata = stateManager.getTaskMetadata(taskId);
            if (metadata.getEnvironmentHistory() == null) {
                metadata.setEnvironmentHistory(new ArrayList<>());
            }

            List<EnvironmentMetadataEntry> history = metadata.getEnvironmentHistory();
            EnvironmentMetadataEntry current = collectEnvironmentMetadata(clineVersion);

            if (!history.isEmpty()) {
                EnvironmentMetadataEntry last = history.get(history.size() - 1);
                if (last.isSameEnvironment(current)) {
                    return;
                }
            }

            history.add(current);
            stateManager.saveTaskMetadata(taskId, metadata);
        } catch (Exception e) {
            log.error("Failed to record environment metadata for task {}", taskId, e);
        }
    }

    private EnvironmentMetadataEntry collectEnvironmentMetadata(String clineVersion) {
        String hostName;
        try {
            hostName = InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            hostName = "unknown";
        }

        return EnvironmentMetadataEntry.builder()
                .ts(System.currentTimeMillis())
                .osName(System.getProperty("os.name", "unknown"))
                .osVersion(System.getProperty("os.version", "unknown"))
                .osArch(System.getProperty("os.arch", "unknown"))
                .hostName(hostName)
                .hostVersion(System.getProperty("java.version", "unknown"))
                .clineVersion(clineVersion)
                .build();
    }
}
