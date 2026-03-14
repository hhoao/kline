package com.hhoa.kline.core.core.shared.proto.host;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecuteCommandInTerminalResponse {
    @Builder.Default private boolean success = false;
}
