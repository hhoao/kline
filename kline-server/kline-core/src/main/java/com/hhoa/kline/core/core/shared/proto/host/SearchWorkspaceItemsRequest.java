package com.hhoa.kline.core.core.shared.proto.host;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchWorkspaceItemsRequest {
    @Builder.Default private String query = "";

    private Integer limit;

    private SearchWorkspaceItemsRequestSearchItemType selectedType;
}
