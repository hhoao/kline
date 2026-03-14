package com.hhoa.kline.web.controller.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 对应 proto 的 CommandContext */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommandContextRequestDTO {
    private String filePath;

    private String selectedText;

    private String language;

    /** 注意：Diagnostic 类需要从 proto 生成，暂时使用 Object */
    @Builder.Default private List<Object> diagnostics = new ArrayList<>();
}
