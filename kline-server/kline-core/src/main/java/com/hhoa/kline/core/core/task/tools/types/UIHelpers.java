package com.hhoa.kline.core.core.task.tools.types;

import com.hhoa.kline.core.core.shared.ClineAsk;
import com.hhoa.kline.core.core.shared.ClineMessageFormat;
import com.hhoa.kline.core.core.shared.ClineSay;
import com.hhoa.kline.core.core.task.AskPending;
import com.hhoa.kline.core.core.task.TaskUtils;

/** 面向工具处理器的强类型 UI 助手接口与工厂方法。 */
public interface UIHelpers {

    void say(
            ClineSay type,
            String text,
            String[] images,
            String[] files,
            Boolean partial,
            ClineMessageFormat format);

    AskPending ask(ClineAsk type, String text, Boolean partial, ClineMessageFormat format);

    Boolean shouldAutoApproveTool(String toolName);

    Boolean shouldAutoApproveToolWithPath(String toolName, String path);

    void captureTelemetry(
            String toolName, boolean autoApproved, boolean approved, boolean isNativeToolCall);

    void showNotificationIfEnabled(String message);

    ToolContext getContext();

    /** 工厂：由 ToolContext 创建强类型 UI 助手。 */
    static UIHelpers create(ToolContext context) {
        return new UIHelpers() {
            @Override
            public void say(
                    ClineSay type,
                    String text,
                    String[] images,
                    String[] files,
                    Boolean partial,
                    ClineMessageFormat format) {
                context.getCallbacks().say(type, text, images, files, partial, format);
            }

            @Override
            public AskPending ask(
                    ClineAsk type, String text, Boolean partial, ClineMessageFormat format) {
                return context.getCallbacks().ask(type, text, partial, format);
            }

            @Override
            public Boolean shouldAutoApproveTool(String toolName) {
                return context.getCallbacks().shouldAutoApproveTool(toolName);
            }

            @Override
            public Boolean shouldAutoApproveToolWithPath(String toolName, String path) {
                return context.getCallbacks().shouldAutoApproveToolWithPath(toolName, path);
            }

            @Override
            public void captureTelemetry(
                    String toolName,
                    boolean autoApproved,
                    boolean approved,
                    boolean isNativeToolCall) {
                // 若需要可在此对接实际遥测服务；当前不做操作以保持最小实现
            }

            @Override
            public void showNotificationIfEnabled(String message) {
                TaskUtils.showNotificationForApproval(
                        message,
                        context.getAutoApprovalSettings() != null
                                && context.getAutoApprovalSettings().isEnableNotifications(),
                        (subtitle, msg) -> {});
            }

            @Override
            public ToolContext getContext() {
                return context;
            }
        };
    }
}
