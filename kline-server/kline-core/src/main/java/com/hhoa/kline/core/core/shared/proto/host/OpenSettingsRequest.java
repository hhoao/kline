package com.hhoa.kline.core.core.shared.proto.host;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpenSettingsRequest {
    /**
     * 可选查询以聚焦特定设置部分/键 此值是主机特定的。在 VS Code 中，它直接作为设置搜索查询传递给 "workbench.action.openSettings" 命令 示例（VS
     * Code，参见 - https://code.visualstudio.com/docs/getstarted/settings#settings-editor-filters.）： -
     * "telemetry.telemetryLevel" → 聚焦遥测级别设置 - "@id:telemetry.telemetryLevel" → 通过精确设置 ID 导航 -
     * "@modified", "@ext:publisher.extension" - 纯关键字/类别 如果未提供，主机将打开设置 UI 而不进行特定聚焦
     */
    private String query;
}
