package com.hhoa.kline.core.core.shared.proto.host;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpenMultiFileDiffRequest {
    private String title;
    @Builder.Default private List<ContentDiff> diffs = new ArrayList<>();
}
