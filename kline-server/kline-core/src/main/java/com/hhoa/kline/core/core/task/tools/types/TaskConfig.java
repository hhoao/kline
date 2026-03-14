package com.hhoa.kline.core.core.task.tools.types;

import com.hhoa.kline.core.core.context.management.ContextManager;
import com.hhoa.kline.core.core.context.tracking.FileContextTracker;
import com.hhoa.kline.core.core.ignore.ClineIgnoreController;
import com.hhoa.kline.core.core.integrations.editor.DiffViewProvider;
import com.hhoa.kline.core.core.integrations.notifications.NotificationService;
import com.hhoa.kline.core.core.services.mcp.IMcpHub;
import com.hhoa.kline.core.core.services.telemetry.TelemetryService;
import com.hhoa.kline.core.core.shared.AutoApprovalSettings;
import com.hhoa.kline.core.core.shared.ClineAsk;
import com.hhoa.kline.core.core.shared.ClineMessageFormat;
import com.hhoa.kline.core.core.shared.ClineSay;
import com.hhoa.kline.core.core.shared.api.ApiProvider;
import com.hhoa.kline.core.core.shared.storage.types.Mode;
import com.hhoa.kline.core.core.storage.StateManager;
import com.hhoa.kline.core.core.task.AskResult;
import com.hhoa.kline.core.core.task.MessageStateHandler;
import com.hhoa.kline.core.core.task.TaskState;
import com.hhoa.kline.core.core.task.tools.AutoApprove;
import com.hhoa.kline.core.core.task.tools.ToolExecutorCoordinator;
import com.hhoa.kline.core.core.workspace.WorkspaceRootManager;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskConfig {
    @NotNull private String taskId;

    @NotNull private String ulid;

    @NotNull private String cwd;

    @NotNull private Mode mode;

    private WorkspaceRootManager workspaceManager;

    @Builder.Default private boolean yoloModeToggled = false;

    @NotNull private TaskState taskState;

    @NotNull private MessageStateHandler messageState;

    private Api api;

    @NotNull private Services services;

    @NotNull private AutoApprovalSettings autoApprovalSettings;

    @NotNull private Callbacks callbacks;

    @NotNull private ToolExecutorCoordinator coordinator;

    private AutoApprove autoApprover;

    public interface Api {
        Model getModel();
    }

    public interface Model {
        String getId();
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static final class Services {
        @NotNull private ContextManager contextManager;

        @NotNull private StateManager stateManager;

        private IMcpHub mcpHub;

        private DiffViewProvider diffViewProvider;

        private ClineIgnoreController clineIgnoreController;

        private FileContextTracker fileContextTracker;

        private TelemetryService telemetryService;

        private NotificationService notificationService;
    }

    public static final class ResourceResult {
        public List<ContentItem> contents;

        public ResourceResult() {
            this.contents = new ArrayList<>();
        }

        public ResourceResult(List<ContentItem> contents) {
            this.contents = contents != null ? contents : new ArrayList<>();
        }
    }

    public static final class ContentItem {
        public String text;
        public String type;

        public ContentItem() {}

        public ContentItem(String text) {
            this.text = text;
            this.type = "text";
        }
    }

    public interface ApiConfiguration {
        ApiProvider getPlanModeApiProvider();

        ApiProvider getActModeApiProvider();
    }

    public static final class OpenOptions {
        public String displayPath;
    }

    public interface Callbacks {
        void say(
                ClineSay type,
                String text,
                String[] images,
                String[] files,
                Boolean partial,
                ClineMessageFormat format);

        AskResult ask(ClineAsk type, String text, Boolean partial, ClineMessageFormat format);

        void saveCheckpoint(Boolean isAttemptCompletionMessage, Long completionMessageTs);

        Boolean shouldAutoApproveToolWithPath(String toolName, String path);

        Boolean shouldAutoApproveTool(String toolName);

        String sayAndCreateMissingParamError(String toolName, String paramName);

        ExecuteResult executeCommandTool(String command, Integer timeoutSeconds);

        void sayUserFeedback(String text, String[] images, String[] files);

        Boolean switchToActMode();

        Boolean updateFCListFromToolResponse(String text);

        Boolean doesLatestTaskCompletionHaveNewChanges();
    }

    public static final class ExecuteResult {
        public boolean userRejected;
        public String result;
    }
}
