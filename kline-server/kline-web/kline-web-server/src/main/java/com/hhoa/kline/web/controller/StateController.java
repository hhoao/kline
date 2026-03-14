package com.hhoa.kline.web.controller;

import static com.hhoa.kline.web.common.pojo.CommonResult.success;

import com.hhoa.kline.core.core.shared.storage.GlobalState;
import com.hhoa.kline.core.core.shared.storage.Secrets;
import com.hhoa.kline.core.core.shared.storage.Settings;
import com.hhoa.kline.web.common.pojo.CommonResult;
import com.hhoa.kline.web.service.StateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(name = "Cline 状态服务")
@RestController
@RequestMapping("/api/cline/state")
@RequiredArgsConstructor
public class StateController {

    private final StateService stateService;

    @Operation(summary = "获取最新状态")
    @GetMapping("/latest")
    public CommonResult<String> getLatestState() {
        String response = stateService.getLatestState();
        return success(response);
    }

    @Operation(summary = "切换收藏模型")
    @PostMapping("/toggle-favorite-model")
    public CommonResult<Void> toggleFavoriteModel(@Valid @RequestBody String request) {
        stateService.toggleFavoriteModel(request);
        return success(null);
    }

    @Operation(summary = "重置状态")
    @PostMapping("/reset")
    public CommonResult<Void> resetState() {
        stateService.resetState();
        return success(null);
    }

    @Operation(summary = "获取全局状态")
    @GetMapping("/global-state")
    public CommonResult<GlobalState> getGlobalState() {
        GlobalState globalState = stateService.getGlobalState();
        return success(globalState);
    }

    @Operation(summary = "更新全局状态")
    @PostMapping("/global-state")
    public CommonResult<Void> updateGlobalState(@Valid @RequestBody GlobalState globalState) {
        stateService.updateGlobalState(globalState);
        return success(null);
    }

    @Operation(summary = "获取密钥配置")
    @GetMapping("/secrets")
    public CommonResult<Secrets> getSecrets() {
        Secrets secrets = stateService.getSecrets();
        return success(secrets);
    }

    @Operation(summary = "更新密钥配置")
    @PostMapping("/secrets")
    public CommonResult<Void> updateSecrets(@Valid @RequestBody Secrets secrets) {
        stateService.updateSecrets(secrets);
        return success(null);
    }

    @Operation(summary = "获取设置配置")
    @GetMapping("/settings")
    public CommonResult<Settings> getSettings() {
        Settings settings = stateService.getSettings();
        return success(settings);
    }

    @Operation(summary = "更新设置配置")
    @PostMapping("/settings")
    public CommonResult<Void> updateSettings(@Valid @RequestBody Settings settings) {
        stateService.updateSettings(settings);
        return success(null);
    }
}
