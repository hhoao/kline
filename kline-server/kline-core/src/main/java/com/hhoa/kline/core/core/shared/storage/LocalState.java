package com.hhoa.kline.core.core.shared.storage;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocalState {
    private Map<String, Boolean> localClineRulesToggles;
    private Map<String, Boolean> localCursorRulesToggles;
    private Map<String, Boolean> localWindsurfRulesToggles;
    private Map<String, Boolean> workflowToggles;
}
