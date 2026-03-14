package com.hhoa.kline.core.core.shared.proto.cline;

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
public class FileDiagnostics {
    @Builder.Default private String filePath = "";
    @Builder.Default private List<Diagnostic> diagnostics = new ArrayList<>();
}
