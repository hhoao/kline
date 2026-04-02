package com.hhoa.kline.core.core.hooks;

import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/** Hook 脚本的输入数据，包含通用元数据和 hook 特定数据 */
@Getter
@Setter
@Builder
public class HookInput {
    private String clineVersion;
    private String hookName;
    private String timestamp;
    private String taskId;
    private List<String> workspaceRoots;
    private String userId;

    /** Hook 特定数据（根据 hookName 类型只有一个非 null） */
    private HookData.PreToolUseData preToolUse;

    private HookData.PostToolUseData postToolUse;
    private HookData.UserPromptSubmitData userPromptSubmit;
    private HookData.TaskStartData taskStart;
    private HookData.TaskResumeData taskResume;
    private HookData.TaskCancelData taskCancel;
    private HookData.TaskCompleteData taskComplete;
    private HookData.PreCompactData preCompact;
    private HookData.NotificationData notification;

    /** 模型上下文（可选） */
    private HookModelContext model;

    @Getter
    @Builder
    public static class HookModelContext {
        private final String provider;
        private final String slug;
    }
}
