package com.hhoa.kline.core.core.shared.proto.host;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** https://code.visualstudio.com/api/references/vscode-api#TextDocumentShowOptions */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowTextDocumentOptions {
    private Boolean preview;
    private Boolean preserveFocus;
    private Integer viewColumn;
}
