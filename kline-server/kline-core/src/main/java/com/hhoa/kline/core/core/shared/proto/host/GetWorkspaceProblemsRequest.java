package com.hhoa.kline.core.core.shared.proto.host;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 获取工作区问题的请求
 *
 * <p>供 Mentions 功能通过 Subscription 管道请求 IDE 端返回当前工作区的错误/警告摘要
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetWorkspaceProblemsRequest {
    @Builder.Default private String action = "getWorkspaceProblems";

    private String cwd;
}
