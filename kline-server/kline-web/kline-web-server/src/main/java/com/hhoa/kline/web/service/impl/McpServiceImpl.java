package com.hhoa.kline.web.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hhoa.kline.core.core.controller.TaskManager;
import com.hhoa.kline.core.core.controller.TaskManagerFactory;
import com.hhoa.kline.core.core.services.mcp.IMcpHub;
import com.hhoa.kline.core.core.services.mcp.McpServerConfig;
import com.hhoa.kline.core.core.shared.storage.Settings;
import com.hhoa.kline.web.controller.dto.AddRemoteMcpServerRequestDTO;
import com.hhoa.kline.web.controller.dto.ToggleMcpServerRequestDTO;
import com.hhoa.kline.web.controller.dto.ToggleToolAutoApproveRequestDTO;
import com.hhoa.kline.web.controller.dto.UpdateMcpTimeoutRequestDTO;
import com.hhoa.kline.web.service.McpService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class McpServiceImpl implements McpService {

    private static final String INTERNAL_SERVER_NAME = "internal";

    private final TaskManagerFactory taskManagerFactory;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String toggleMcpServer(ToggleMcpServerRequestDTO request) {
        IMcpHub hub = getHub();
        try {
            hub.toggleServerDisabled(request.getServerName(), request.getDisabled());
            return toServersJson(hub);
        } catch (Exception e) {
            log.warn("toggleMcpServer failed: {}", e.getMessage());
            return errorJson(e.getMessage());
        }
    }

    @Override
    public String updateMcpTimeout(UpdateMcpTimeoutRequestDTO request) {
        IMcpHub hub = getHub();
        try {
            hub.updateServerTimeout(request.getServerName(), request.getTimeout());
            return toServersJson(hub);
        } catch (Exception e) {
            log.warn("updateMcpTimeout failed: {}", e.getMessage());
            return errorJson(e.getMessage());
        }
    }

    @Override
    public String addRemoteMcpServer(AddRemoteMcpServerRequestDTO request) {
        IMcpHub hub = getHub();
        try {
            hub.addRemoteServer(request.getServerName(), request.getServerUrl());
            return toServersJson(hub);
        } catch (Exception e) {
            log.warn("addRemoteMcpServer failed: {}", e.getMessage());
            return errorJson(e.getMessage());
        }
    }

    @Override
    public String downloadMcp(String mcpId) {
        if (mcpId == null || mcpId.isBlank()) {
            return errorJson("MCP ID 不能为空");
        }
        return "{\"message\":\"后端不支持从市场下载 MCP，请在前端或桌面端操作\"}";
    }

    @Override
    public String restartMcpServer(String serverName) {
        if (serverName == null || serverName.isBlank()) {
            return errorJson("serverName 不能为空");
        }
        IMcpHub hub = getHub();
        try {
            hub.restartServer(serverName.trim());
            return toServersJson(hub);
        } catch (Exception e) {
            log.warn("restartMcpServer failed: {}", e.getMessage());
            return errorJson(e.getMessage());
        }
    }

    @Override
    public String deleteMcpServer(String serverName) {
        if (serverName == null || serverName.isBlank()) {
            return errorJson("serverName 不能为空");
        }
        IMcpHub hub = getHub();
        try {
            hub.deleteServer(serverName.trim());
            return toServersJson(hub);
        } catch (Exception e) {
            log.warn("deleteMcpServer failed: {}", e.getMessage());
            return errorJson(e.getMessage());
        }
    }

    @Override
    public String toggleToolAutoApprove(ToggleToolAutoApproveRequestDTO request) {
        IMcpHub hub = getHub();
        try {
            hub.toggleToolAutoApprove(
                    request.getServerName(),
                    request.getToolNames() != null ? request.getToolNames() : List.of(),
                    Boolean.TRUE.equals(request.getAutoApprove()));
            return toServersJson(hub);
        } catch (Exception e) {
            log.warn("toggleToolAutoApprove failed: {}", e.getMessage());
            return errorJson(e.getMessage());
        }
    }

    @Override
    public String refreshMcpMarketplace() {
        return "{\"message\":\"后端不支持 MCP 市场刷新\"}";
    }

    @Override
    public void openMcpSettings() {}

    @Override
    public String subscribeToMcpMarketplaceCatalog() {
        return "{\"message\":\"后端不支持 MCP 市场目录订阅\"}";
    }

    @Override
    public String getLatestMcpServers() {
        try {
            return toServersJson(getHub());
        } catch (Exception e) {
            log.warn("getLatestMcpServers failed: {}", e.getMessage());
            return errorJson(e.getMessage());
        }
    }

    private IMcpHub getHub() {
        TaskManager taskManager = taskManagerFactory.getOrCreateTaskManager();
        return taskManager.getMcpHub();
    }

    private String toServersJson(IMcpHub hub) {
        List<Map<String, Object>> list = buildServerList(hub);
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            log.warn("serialize mcp servers failed", e);
            return "[]";
        }
    }

    private List<Map<String, Object>> buildServerList(IMcpHub hub) {
        TaskManager taskManager = taskManagerFactory.getOrCreateTaskManager();
        Settings settings = taskManager.getStateManager().getSettings();
        Map<String, McpServerConfig> mcpServers = settings.getMcpServers();
        if (mcpServers == null) {
            return List.of();
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map.Entry<String, McpServerConfig> entry : mcpServers.entrySet()) {
            if (INTERNAL_SERVER_NAME.equals(entry.getKey())) {
                continue;
            }
            McpServerConfig c = entry.getValue();
            Map<String, Object> item = new HashMap<>();
            item.put("name", entry.getKey());
            item.put("disabled", Boolean.TRUE.equals(c.getDisabled()));
            item.put("timeout", c.getTimeout() != null ? c.getTimeout() : 20);
            list.add(item);
        }
        return list;
    }

    private String errorJson(String message) {
        try {
            return objectMapper.writeValueAsString(
                    Map.of("error", message != null ? message : "未知错误"));
        } catch (JsonProcessingException e) {
            return "{\"error\":\"" + (message != null ? message : "未知错误") + "\"}";
        }
    }
}
