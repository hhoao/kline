package com.hhoa.kline.core.core.context.management;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class FileMentionUpdate {
    private String baseText;
    private String updatedText;
    private List<String> filePathsUpdated;

    FileMentionUpdate(String baseText, List<String> prevFilesReplaced) {
        this.baseText = baseText;
        this.updatedText = baseText;
        this.filePathsUpdated = new ArrayList<>(prevFilesReplaced);
    }
}
