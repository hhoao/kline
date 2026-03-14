package com.hhoa.kline.core.core.shared;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrpcResponse {
    private Object message;

    private String requestId;

    private String error;

    private Boolean isStreaming;

    private Integer sequenceNumber;
}
