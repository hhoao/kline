package com.hhoa.kline.core.core.context.tracking;

import com.hhoa.kline.core.core.shared.storage.types.Mode;
import com.hhoa.kline.core.core.storage.StateManager;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ModelContextTracker {
    private final String taskId;

    private final StateManager stateManager;

    public ModelContextTracker(String taskId, StateManager stateManager) {
        this.taskId = taskId;
        this.stateManager = stateManager;
    }

    public void recordModelUsage(String apiProviderId, String modelId, Mode mode) {
        try {
            TaskMetadata metadata = stateManager.getTaskMetadata(taskId);

            if (metadata.getModelUsage() == null) {
                metadata.setModelUsage(new ArrayList<>());
            }

            List<ModelMetadataEntry> modelUsage = metadata.getModelUsage();
            if (!modelUsage.isEmpty()) {
                ModelMetadataEntry lastEntry = modelUsage.get(modelUsage.size() - 1);
                if (lastEntry.getModelId().equals(modelId)
                        && lastEntry.getModelProviderId().equals(apiProviderId)
                        && lastEntry.getMode().equals(mode.getValue())) {
                    return;
                }
            }

            ModelMetadataEntry newEntry =
                    new ModelMetadataEntry(
                            System.currentTimeMillis(), modelId, apiProviderId, mode.getValue());
            modelUsage.add(newEntry);

            stateManager.saveTaskMetadata(taskId, metadata);
        } catch (Exception e) {
            log.error("Failed to record model usage: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public String getTaskId() {
        return taskId;
    }
}
