package com.hhoa.kline.core.core.shared.proto.cline;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetTaskHistoryRequest {
    private Object metadata;
    @Builder.Default private boolean favoritesOnly = false;
    @Builder.Default private String searchQuery = "";
    @Builder.Default private String sortBy = "";
    @Builder.Default private boolean currentWorkspaceOnly = false;
}
