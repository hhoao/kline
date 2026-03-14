package com.hhoa.kline.web.controller;

import static com.hhoa.kline.web.common.pojo.CommonResult.success;

import com.hhoa.kline.web.common.pojo.CommonResult;
import com.hhoa.kline.web.service.SlashService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(name = "Cline 斜杠命令服务")
@RestController
@RequestMapping("/api/cline/slash")
@RequiredArgsConstructor
public class SlashController {

    private final SlashService slashService;

    @Operation(summary = "压缩")
    @PostMapping("/condense")
    public CommonResult<Void> condense(@Valid @RequestBody String request) {
        slashService.condense(request);
        return success(null);
    }
}
