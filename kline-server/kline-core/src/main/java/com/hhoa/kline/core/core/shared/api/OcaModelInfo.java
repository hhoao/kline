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
public class OcaModelInfo extends OpenAiCompatibleModelInfo {
    private String modelName;
    private String surveyId;
    private String banner;
    private String surveyContent;
}
