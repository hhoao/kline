package com.hhoa.kline.core.core.shared;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FocusChainSettings {
    @Builder.Default private boolean enabled = true;

    @Builder.Default private int remindClineInterval = 6;
}
