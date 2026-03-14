package com.hhoa.kline.core.core.shared;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class AutoApprovalSettings {
    private int version = 1;

    private boolean enabled = true;

    private AutoApprovalActions actions;

    private int maxRequests = 20;

    private boolean enableNotifications = false;

    private List<String> favorites = new ArrayList<>();

    @Data
    public static class AutoApprovalActions {
        private boolean readFiles = true;

        private Boolean readFilesExternally;

        private boolean editFiles = false;

        private Boolean editFilesExternally;

        private boolean executeSafeCommands = true;

        private boolean executeAllCommands = false;

        private boolean useBrowser = false;

        private boolean useMcp = false;
    }
}
