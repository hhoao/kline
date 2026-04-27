package com.hhoa.kline.core.core.prompts.systemprompt;

import com.hhoa.kline.core.core.tools.ToolSpec;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

/**
 * 提示配置
 *
 * @author hhoa
 */
@Data
@Builder
public class PromptConfig {

    private String modelName;

    private Double temperature;

    private Integer maxTokens;

    private List<ToolSpec> tools;

    private Map<String, Object> additionalConfig;
}
