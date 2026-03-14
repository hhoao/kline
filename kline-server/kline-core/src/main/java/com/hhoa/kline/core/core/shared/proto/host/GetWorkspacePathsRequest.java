package com.hhoa.kline.core.core.shared.proto.host;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetWorkspacePathsRequest {
    /** 工作区/项目的唯一 ID 在 vscode 中当前是可选的。在 Cline 在应用程序级别运行的其他环境中是必需的，用户可以打开多个项目 */
    private String id;
}
