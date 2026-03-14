package com.hhoa.kline.core.core.shared.multiroot;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceRoot {
    private String path;

    private String name;

    private VcsType vcs;

    private String commitHash;
}
