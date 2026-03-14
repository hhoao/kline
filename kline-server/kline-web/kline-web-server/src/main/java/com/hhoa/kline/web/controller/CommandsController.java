package com.hhoa.kline.web.controller;

import static com.hhoa.kline.web.common.pojo.CommonResult.success;

import com.hhoa.kline.web.common.pojo.CommonResult;
import com.hhoa.kline.web.controller.dto.CommandContextRequestDTO;
import com.hhoa.kline.web.service.CommandsService;
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
@Tag(name = "Cline 命令服务")
@RestController
@RequestMapping("/api/cline/commands")
@RequiredArgsConstructor
public class CommandsController {

    private final CommandsService commandsService;

    @Operation(summary = "添加到 Cline")
    @PostMapping("/add-to-cline")
    public CommonResult<Void> addToCline(@Valid @RequestBody CommandContextRequestDTO request) {
        commandsService.addToCline(request);
        return success(null);
    }

    @Operation(summary = "使用 Cline 修复")
    @PostMapping("/fix-with-cline")
    public CommonResult<Void> fixWithCline(@Valid @RequestBody CommandContextRequestDTO request) {
        commandsService.fixWithCline(request);
        return success(null);
    }

    @Operation(summary = "使用 Cline 解释")
    @PostMapping("/explain-with-cline")
    public CommonResult<Void> explainWithCline(
            @Valid @RequestBody CommandContextRequestDTO request) {
        commandsService.explainWithCline(request);
        return success(null);
    }

    @Operation(summary = "使用 Cline 改进")
    @PostMapping("/improve-with-cline")
    public CommonResult<Void> improveWithCline(
            @Valid @RequestBody CommandContextRequestDTO request) {
        commandsService.improveWithCline(request);
        return success(null);
    }
}
