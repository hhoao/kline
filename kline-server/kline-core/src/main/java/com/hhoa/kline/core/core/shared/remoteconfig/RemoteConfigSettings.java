package com.hhoa.kline.core.core.shared.remoteconfig;

import com.hhoa.kline.core.core.shared.TelemetrySetting;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RemoteConfigSettings {
    private TelemetrySetting telemetrySetting;
    private Boolean mcpMarketplaceEnabled;
    private Boolean yoloModeToggled;
    @Builder.Default private List<GlobalInstructionsFile> remoteGlobalRules = new ArrayList<>();
}
