package com.hhoa.kline.web.service;

import com.hhoa.kline.web.controller.dto.AddRemoteMcpServerRequestDTO;
import com.hhoa.kline.web.controller.dto.ToggleMcpServerRequestDTO;
import com.hhoa.kline.web.controller.dto.ToggleToolAutoApproveRequestDTO;
import com.hhoa.kline.web.controller.dto.UpdateMcpTimeoutRequestDTO;

public interface McpService {

    String toggleMcpServer(ToggleMcpServerRequestDTO request);

    String updateMcpTimeout(UpdateMcpTimeoutRequestDTO request);

    String addRemoteMcpServer(AddRemoteMcpServerRequestDTO request);

    String downloadMcp(String mcpId);

    String restartMcpServer(String serverName);

    String deleteMcpServer(String serverName);

    String toggleToolAutoApprove(ToggleToolAutoApproveRequestDTO request);

    String refreshMcpMarketplace();

    void openMcpSettings();

    String subscribeToMcpMarketplaceCatalog();

    String getLatestMcpServers();
}
