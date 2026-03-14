package com.hhoa.kline.core.core.shared;

import lombok.Data;

@Data
public class LanguageModelChatSelector {
    private String vendor;
    private String family;
    private String version;
    private String id;
}
