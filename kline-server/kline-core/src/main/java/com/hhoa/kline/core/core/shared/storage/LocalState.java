package com.hhoa.kline.core.core.shared.storage;

import java.util.HashMap;
import java.util.List;
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
    @Builder.Default private Map<String, Boolean> localClineRulesToggles = new HashMap<>();
    @Builder.Default private Map<String, Boolean> localCursorRulesToggles = new HashMap<>();
    @Builder.Default private Map<String, Boolean> localWindsurfRulesToggles = new HashMap<>();
    @Builder.Default private Map<String, Boolean> localAgentsRulesToggles = new HashMap<>();
    @Builder.Default private Map<String, Boolean> localSkillsToggles = new HashMap<>();
    @Builder.Default private Map<String, Boolean> workflowToggles = new HashMap<>();
    @Builder.Default private Map<String, List<String>> pendingFileContextWarnings = new HashMap<>();
}
