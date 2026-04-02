package com.hhoa.kline.core.core.shared.remoteconfig;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlobalInstructionsFile {
    private boolean alwaysEnabled;
    private String name;
    private String contents;
}
