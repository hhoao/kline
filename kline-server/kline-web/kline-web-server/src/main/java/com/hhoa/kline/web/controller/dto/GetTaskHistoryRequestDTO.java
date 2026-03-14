package com.hhoa.kline.web.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetTaskHistoryRequestDTO {
    @Builder.Default private Boolean favoritesOnly = false;

    @Builder.Default private String searchQuery = "";

    @Builder.Default private String sortBy = "";

    @Builder.Default private Boolean currentWorkspaceOnly = false;
}
