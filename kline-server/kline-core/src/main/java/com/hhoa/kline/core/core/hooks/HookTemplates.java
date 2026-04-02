package com.hhoa.kline.core.core.hooks;

/**
 * Hook 脚本模板。
 *
 * <p>对应 Cline TS 版本的 templates.ts。 在 Unix 下生成 Bash 模板，在 Windows 下生成 PowerShell 模板。
 */
public final class HookTemplates {

    private HookTemplates() {}

    /** 获取指定 hook 类型的脚本模板。 */
    public static String getHookTemplate(HookName hookName) {
        if (isWindows()) {
            return getWindowsPowerShellTemplate(hookName);
        }
        return switch (hookName) {
            case TASK_START -> getTaskStartTemplate();
            case TASK_RESUME -> getTaskResumeTemplate();
            case TASK_CANCEL -> getTaskCancelTemplate();
            case TASK_COMPLETE -> getTaskCompleteTemplate();
            case PRE_TOOL_USE -> getPreToolUseTemplate();
            case POST_TOOL_USE -> getPostToolUseTemplate();
            case USER_PROMPT_SUBMIT -> getUserPromptSubmitTemplate();
            case NOTIFICATION -> getNotificationTemplate();
            case PRE_COMPACT -> getPreCompactTemplate();
        };
    }

    private static String getWindowsPowerShellTemplate(HookName hookName) {
        return """
                # %s Hook
                # PowerShell template for Windows hook execution.

                try {
                    $rawInput = [Console]::In.ReadToEnd()
                    if ($rawInput) {
                        $null = $rawInput | ConvertFrom-Json
                    }
                } catch {
                    Write-Error "[%s] Invalid JSON input: $($_.Exception.Message)"
                }

                @{
                    cancel = $false
                    contextModification = ""
                    errorMessage = ""
                } | ConvertTo-Json -Compress
                """
                .formatted(hookName.getValue(), hookName.getValue());
    }

    private static String getTaskStartTemplate() {
        return """
                #!/bin/bash
                #
                # TaskStart Hook
                #
                # Executes when a new task begins.
                #
                # Input: { taskId, taskStart: { task: string }, clineVersion, timestamp, ... }
                # Output: { cancel: boolean, contextModification?: string, errorMessage?: string }

                INPUT=$(cat)

                if command -v jq &> /dev/null; then
                  TASK=$(echo "$INPUT" | jq -r '.taskStart.task')
                  TASK_ID=$(echo "$INPUT" | jq -r '.taskId')
                else
                  TASK="<task>"
                  TASK_ID="<taskId>"
                fi

                echo "[TaskStart] Task started: $TASK" >&2

                echo '{"cancel":false,"contextModification":"","errorMessage":""}'
                """;
    }

    private static String getTaskResumeTemplate() {
        return """
                #!/bin/bash
                #
                # TaskResume Hook
                #
                # Executes when a task is resumed after being interrupted.

                INPUT=$(cat)

                if command -v jq &> /dev/null; then
                  TASK=$(echo "$INPUT" | jq -r '.taskResume.task')
                else
                  TASK="<task>"
                fi

                echo "[TaskResume] Resuming task: $TASK" >&2
                echo '{"cancel":false,"contextModification":"","errorMessage":""}'
                """;
    }

    private static String getTaskCancelTemplate() {
        return """
                #!/bin/bash
                #
                # TaskCancel Hook
                #
                # Executes when a task is cancelled by the user.

                INPUT=$(cat)

                if command -v jq &> /dev/null; then
                  TASK=$(echo "$INPUT" | jq -r '.taskCancel.task')
                else
                  TASK="<task>"
                fi

                echo "[TaskCancel] Task cancelled: $TASK" >&2
                echo '{"cancel":false,"contextModification":"","errorMessage":""}'
                """;
    }

    private static String getTaskCompleteTemplate() {
        return """
                #!/bin/bash
                #
                # TaskComplete Hook
                #
                # Executes when a task completes successfully.

                INPUT=$(cat)

                if command -v jq &> /dev/null; then
                  TASK=$(echo "$INPUT" | jq -r '.taskComplete.task')
                else
                  TASK="<task>"
                fi

                echo "[TaskComplete] Task completed: $TASK" >&2
                echo '{"cancel":false,"contextModification":"","errorMessage":""}'
                """;
    }

    private static String getPreToolUseTemplate() {
        return """
                #!/bin/bash
                #
                # PreToolUse Hook
                #
                # Executes before any tool is used.

                INPUT=$(cat)

                if command -v jq &> /dev/null; then
                  TOOL=$(echo "$INPUT" | jq -r '.preToolUse.tool')
                  COMMAND=$(echo "$INPUT" | jq -r '.preToolUse.parameters.command // empty')
                else
                  TOOL="<tool>"
                  COMMAND=""
                fi

                if [[ "$TOOL" == "execute_command" ]] && [[ "$COMMAND" == *"rm -rf /"* ]]; then
                  echo '{"cancel":true,"errorMessage":"Dangerous command blocked by PreToolUse hook"}'
                  exit 0
                fi

                echo "[PreToolUse] Tool about to execute: $TOOL" >&2
                echo '{"cancel":false,"contextModification":"","errorMessage":""}'
                """;
    }

    private static String getPostToolUseTemplate() {
        return """
                #!/bin/bash
                #
                # PostToolUse Hook
                #
                # Executes after any tool is used.

                INPUT=$(cat)

                if command -v jq &> /dev/null; then
                  TOOL=$(echo "$INPUT" | jq -r '.postToolUse.tool')
                  SUCCESS=$(echo "$INPUT" | jq -r '.postToolUse.success')
                else
                  TOOL="<tool>"
                  SUCCESS="true"
                fi

                echo "[PostToolUse] Tool completed: $TOOL ($SUCCESS)" >&2
                echo '{"cancel":false,"contextModification":"","errorMessage":""}'
                """;
    }

    private static String getUserPromptSubmitTemplate() {
        return """
                #!/bin/bash
                #
                # UserPromptSubmit Hook
                #
                # Executes when the user submits a prompt.

                INPUT=$(cat)

                echo "[UserPromptSubmit] User submitted prompt" >&2
                echo '{"cancel":false,"contextModification":"","errorMessage":""}'
                """;
    }

    private static String getNotificationTemplate() {
        return """
                #!/bin/bash
                #
                # Notification Hook
                #
                # Executes when Kline emits lifecycle notifications.

                INPUT=$(cat)

                if command -v jq &> /dev/null; then
                  EVENT=$(echo "$INPUT" | jq -r '.notification.event // "unknown"')
                  SOURCE=$(echo "$INPUT" | jq -r '.notification.source // "unknown"')
                else
                  EVENT="unknown"
                  SOURCE="unknown"
                fi

                echo "[Notification] event=$EVENT source=$SOURCE" >&2
                echo '{"cancel":false,"contextModification":"","errorMessage":""}'
                """;
    }

    private static String getPreCompactTemplate() {
        return """
                #!/bin/bash
                #
                # PreCompact Hook
                #
                # Executes before conversation context is compacted.

                INPUT=$(cat)

                if command -v jq &> /dev/null; then
                  CONV_LENGTH=$(echo "$INPUT" | jq -r '.preCompact.conversationLength')
                  EST_TOKENS=$(echo "$INPUT" | jq -r '.preCompact.estimatedTokens')
                else
                  CONV_LENGTH="<length>"
                  EST_TOKENS="<tokens>"
                fi

                echo "[PreCompact] About to compact (messages: $CONV_LENGTH, tokens: $EST_TOKENS)" >&2
                echo '{"cancel":false,"contextModification":"","errorMessage":""}'
                """;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }
}
