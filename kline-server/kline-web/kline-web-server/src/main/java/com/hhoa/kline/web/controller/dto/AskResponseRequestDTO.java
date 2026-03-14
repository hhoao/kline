package com.hhoa.kline.web.controller.dto;

import com.hhoa.ai.kline.commons.enums.InEnum;
import com.hhoa.kline.core.core.shared.ClineAskResponse;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AskResponseRequestDTO {
    @InEnum(ClineAskResponse.class)
    private String responseType;

    private String text;

    @NonNull private String taskId;

    private List<String> images;

    private List<String> files;
}
