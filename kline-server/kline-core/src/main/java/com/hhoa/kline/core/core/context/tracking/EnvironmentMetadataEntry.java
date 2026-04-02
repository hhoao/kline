package com.hhoa.kline.core.core.context.tracking;

import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 环境元数据条目，记录 OS/环境信息及时间戳 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnvironmentMetadataEntry {
    private long ts;
    private String osName;
    private String osVersion;
    private String osArch;
    private String hostName;
    private String hostVersion;
    private String clineVersion;

    /** 比较两个环境条目是否相同（忽略时间戳） */
    public boolean isSameEnvironment(EnvironmentMetadataEntry other) {
        if (other == null) return false;
        return Objects.equals(osName, other.osName)
                && Objects.equals(osVersion, other.osVersion)
                && Objects.equals(osArch, other.osArch)
                && Objects.equals(hostName, other.hostName)
                && Objects.equals(hostVersion, other.hostVersion)
                && Objects.equals(clineVersion, other.clineVersion);
    }
}
