package com.hhoa.kline.core.core.shared.storage;

import com.hhoa.kline.core.core.controller.HistoryItem;
import com.hhoa.kline.core.core.shared.McpDisplayMode;
import com.hhoa.kline.core.core.shared.TerminalExecutionMode;
import com.hhoa.kline.core.core.shared.multiroot.WorkspaceRoot;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class GlobalState {
    private String lastShownAnnouncementId;

    private List<HistoryItem> taskHistory;

    private List<String> favoritedModelIds = new ArrayList<>();

    private boolean mcpMarketplaceEnabled = false;

    private boolean mcpResponsesCollapsed = false;

    private boolean terminalReuseEnabled = false;

    private TerminalExecutionMode terminalExecutionMode = TerminalExecutionMode.VSCODE_TERMINAL;

    private Boolean welcomeViewCompleted;

    private McpDisplayMode mcpDisplayMode;

    private List<WorkspaceRoot> workspaceRoots;

    private int primaryRootIndex = 0;

    private boolean hooksEnabled = false;

    private int lastDismissedInfoBannerVersion = 0;

    private int lastDismissedModelBannerVersion = 0;

    private int lastDismissedCliBannerVersion = 0;

    private Boolean multiRootEnabled;
}
