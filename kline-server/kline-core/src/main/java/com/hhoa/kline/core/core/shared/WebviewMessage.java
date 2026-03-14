package com.hhoa.kline.core.core.shared;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebviewMessage {
    private String type;

    private GrpcRequest grpcRequest;
    private GrpcCancel grpcRequestCancel;
}
