package com.hhoa.kline.web.controller;

import com.hhoa.kline.core.subscription.SubscriptionMessage;
import com.hhoa.kline.web.common.pojo.CommonResult;
import com.hhoa.kline.web.service.UiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@Slf4j
@Tag(name = "Cline UI 服务")
@RestController
@RequestMapping("/api/cline/ui")
@RequiredArgsConstructor
public class UiController {

    private final UiService uiService;

    @Operation(summary = "订阅所有消息事件（流式SSE）")
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<CommonResult<SubscriptionMessage>> subscribe() {
        return uiService.subscribe().cast(SubscriptionMessage.class).map(CommonResult::success);
    }
}
