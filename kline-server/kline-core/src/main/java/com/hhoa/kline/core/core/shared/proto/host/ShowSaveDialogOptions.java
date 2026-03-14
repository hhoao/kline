package com.hhoa.kline.core.core.shared.proto.host;

import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowSaveDialogOptions {
    private String defaultPath;

    /** 文件类型到扩展名的映射，例如 "Text Files": { "extensions": ["txt", "md"] } */
    @Builder.Default private Map<String, FileExtensionList> filters = new HashMap<>();
}
