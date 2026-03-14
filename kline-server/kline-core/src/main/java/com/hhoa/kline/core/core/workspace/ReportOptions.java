package com.hhoa.kline.core.core.workspace;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportOptions {

    @Builder.Default private boolean includeExamples = true;

    @Builder.Default private boolean includeHighUsage = true;

    @Builder.Default private int highUsageThreshold = 100;

    @Builder.Default private boolean sortByUsage = true;
}
