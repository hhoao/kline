package com.hhoa.kline.core.core.shared.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class OpenAiCompatibleModelInfo extends ModelInfo {
    private Double temperature;
    private Boolean isR1FormatRequired;
}
