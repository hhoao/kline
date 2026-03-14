package com.hhoa.kline.web.controller;

import static com.hhoa.kline.web.common.pojo.CommonResult.*;

import com.hhoa.kline.web.common.pojo.CommonResult;
import com.hhoa.kline.web.controller.dto.CheckpointRestoreRequestDTO;
import com.hhoa.kline.web.service.CheckpointsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(name = "Cline 检查点服务")
@RestController
@RequestMapping("/api/cline/checkpoints")
@RequiredArgsConstructor
public class CheckpointsController {

    private final CheckpointsService checkpointsService;

    @Operation(summary = "检查点差异")
    @PostMapping("/diff")
    public CommonResult<Void> checkpointDiff(@Valid @RequestBody Long request) {
        checkpointsService.checkpointDiff(request);
        return success(null);
    }

    @Operation(summary = "恢复检查点")
    @PostMapping("/restore")
    public CommonResult<Void> checkpointRestore(
            @Valid @RequestBody CheckpointRestoreRequestDTO request) {
        checkpointsService.checkpointRestore(request);
        return success(null);
    }

    @Operation(summary = "获取当前工作目录哈希")
    @PostMapping("/cwd-hash")
    public CommonResult<String> getCwdHash(@Valid @RequestBody List<String> request) {
        String response = checkpointsService.getCwdHash(request);
        return success(response);
    }
}
