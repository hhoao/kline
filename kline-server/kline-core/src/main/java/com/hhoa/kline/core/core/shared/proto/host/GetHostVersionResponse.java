package com.hhoa.kline.core.core.shared.proto.host;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetHostVersionResponse {
    private String platform;

    private String version;

    /**
     * Cline 主机环境类型，例如 'VSCode Extension'、'Cline for JetBrains'、'CLI' 这与平台不同，因为有许多 JetBrains
     * IDE，但它们都使用相同的插件
     */
    private String clineType;

    private String clineVersion;
}
