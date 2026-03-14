package com.hhoa.kline.core.core.shared.proto.host;

import com.hhoa.kline.core.core.shared.proto.cline.FileDiagnostics;
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
public class GetDiagnosticsResponse {
    @Builder.Default private List<FileDiagnostics> fileDiagnostics = new ArrayList<>();
}
