package com.hhoa.kline.core.core.shared;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClineSayTool {
    private String
            tool; // "editedExistingFile" | "newFileCreated" | "readFile" | "listFilesTopLevel" |
    // "listFilesRecursive" | "listCodeDefinitionNames" | "searchFiles" | "webFetch" |
    // "summarizeTask"
    private String path;
    private String diff;
    private String content;
    private String regex;
    private String filePattern;
    private Boolean operationIsLocatedInWorkspace;
}
