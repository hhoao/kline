package com.hhoa.kline.core.core.workspace;

import com.hhoa.kline.core.core.shared.multiroot.WorkspaceRoot;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceContext {

    private List<WorkspaceRoot> workspaceRoots;

    private WorkspaceRoot primaryRoot;

    private WorkspaceRoot currentRoot;
}
