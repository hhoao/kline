package com.hhoa.kline.core.core.shared.proto.host;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowOpenDialogueRequest {
    private Boolean canSelectMany;
    private String openLabel;
    private ShowOpenDialogueFilterOption filters;
}
