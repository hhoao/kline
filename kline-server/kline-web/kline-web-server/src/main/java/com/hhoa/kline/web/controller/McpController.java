package com.hhoa.kline.web.controller;

import static com.hhoa.kline.web.common.pojo.CommonResult.success;

import com.hhoa.kline.web.common.pojo.CommonResult;
import com.hhoa.kline.web.controller.dto.AddRemoteMcpServerRequestDTO;
import com.hhoa.kline.web.controller.dto.ToggleMcpServerRequestDTO;
import com.hhoa.kline.web.controller.dto.ToggleToolAutoApproveRequestDTO;
import com.hhoa.kline.web.controller.dto.UpdateMcpTimeoutRequestDTO;
import com.hhoa.kline.web.service.impl.McpServiceImpl;
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
@Tag(name = "Cline MCP 服务")
@RestController
@RequestMapping("/api/cline/mcp")
@RequiredArgsConstructor
public class McpController {

    private final McpServiceImpl mcpService;

    @Operation(summary = "切换 MCP 服务器")
    @PostMapping("/toggle-server")
    public CommonResult<String> toggleMcpServer(
            @Valid @RequestBody ToggleMcpServerRequestDTO request) {
        String response = mcpService.toggleMcpServer(request);
        return success(response);
    }

    @Operation(summary = "更新 MCP 超时")
    @PostMapping("/update-timeout")
    public CommonResult<String> updateMcpTimeout(
            @Valid @RequestBody UpdateMcpTimeoutRequestDTO request) {
        String response = mcpService.updateMcpTimeout(request);
        return success(response);
    }

    @Operation(summary = "添加远程 MCP 服务器")
    @PostMapping("/add-remote-server")
    public CommonResult<String> addRemoteMcpServer(
            @Valid @RequestBody AddRemoteMcpServerRequestDTO request) {
        String response = mcpService.addRemoteMcpServer(request);
        return success(response);
    }

    @Operation(summary = "下载 MCP")
    @PostMapping("/download")
    public CommonResult<String> downloadMcp(@Valid @RequestBody String mcpId) {
        String response = mcpService.downloadMcp(mcpId);
        return success(response);
    }

    @Operation(summary = "重启 MCP 服务器")
    @PostMapping("/restart-server")
    public CommonResult<String> restartMcpServer(@Valid @RequestBody String serverName) {
        String response = mcpService.restartMcpServer(serverName);
        return success(response);
    }

    @Operation(summary = "删除 MCP 服务器")
    @PostMapping("/delete-server")
    public CommonResult<String> deleteMcpServer(@Valid @RequestBody String serverName) {
        String response = mcpService.deleteMcpServer(serverName);
        return success(response);
    }

    @Operation(summary = "切换工具自动批准")
    @PostMapping("/toggle-tool-auto-approve")
    public CommonResult<String> toggleToolAutoApprove(
            @Valid @RequestBody ToggleToolAutoApproveRequestDTO request) {
        String response = mcpService.toggleToolAutoApprove(request);
        return success(response);
    }

    @Operation(summary = "刷新 MCP 市场")
    @PostMapping("/refresh-marketplace")
    public CommonResult<String> refreshMcpMarketplace() {
        String response = mcpService.refreshMcpMarketplace();
        return success(response);
    }

    @Operation(summary = "打开 MCP 设置")
    @PostMapping("/open-settings")
    public CommonResult<Void> openMcpSettings() {
        mcpService.openMcpSettings();
        return success(null);
    }

    @Operation(summary = "订阅 MCP 市场目录更新（流式）")
    @PostMapping("/subscribe-to-marketplace-catalog")
    public CommonResult<String> subscribeToMcpMarketplaceCatalog() {
        String response = mcpService.subscribeToMcpMarketplaceCatalog();
        return success(response);
    }

    @Operation(summary = "获取最新 MCP 服务器")
    @GetMapping("/latest-servers")
    public CommonResult<String> getLatestMcpServers() {
        String response = mcpService.getLatestMcpServers();
        return success(response);
    }
}
