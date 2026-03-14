package com.hhoa.kline.core.core.shared.proto.cline;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Diagnostic {
    @Builder.Default private String message = "";
    private DiagnosticRange range;
    @Builder.Default private DiagnosticSeverity severity = DiagnosticSeverity.DIAGNOSTIC_ERROR;
    private String source;
}
