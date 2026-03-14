package com.hhoa.kline.core.core.task.tools.types;

import com.hhoa.kline.core.core.shared.ClineAsk;
import com.hhoa.kline.core.core.shared.ClineAskResponse;
import com.hhoa.kline.core.core.shared.ClineMessageFormat;
import com.hhoa.kline.core.core.shared.ClineSay;
import com.hhoa.kline.core.core.task.AskResult;
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

    AskResult ask(ClineAsk type, String text, Boolean partial, ClineMessageFormat format);

    Boolean shouldAutoApproveTool(String toolName);

    Boolean shouldAutoApproveToolWithPath(String toolName, String path);

    Boolean askApproval(ClineAsk messageType, String message);

    void captureTelemetry(String toolName, boolean autoApproved, boolean approved);

    void showNotificationIfEnabled(String message);

    TaskConfig getConfig();

    /** 工厂：由 TaskConfig 创建强类型 UI 助手。 */
    static UIHelpers create(TaskConfig config) {
        return new UIHelpers() {
            @Override
            public void say(
                    ClineSay type,
                    String text,
                    String[] images,
                    String[] files,
                    Boolean partial,
                    ClineMessageFormat format) {
                config.getCallbacks().say(type, text, images, files, partial, format);
            }

            @Override
            public AskResult ask(
                    ClineAsk type, String text, Boolean partial, ClineMessageFormat format) {
                return config.getCallbacks().ask(type, text, partial, format);
            }

            @Override
            public Boolean shouldAutoApproveTool(String toolName) {
                return config.getCallbacks().shouldAutoApproveTool(toolName);
            }

            @Override
            public Boolean shouldAutoApproveToolWithPath(String toolName, String path) {
                return config.getCallbacks().shouldAutoApproveToolWithPath(toolName, path);
            }

            @Override
            public Boolean askApproval(ClineAsk messageType, String message) {
                AskResult res = config.getCallbacks().ask(messageType, message, false, null);
                return res != null && ClineAskResponse.YES_BUTTON_CLICKED.equals(res.getResponse());
            }

            @Override
            public void captureTelemetry(String toolName, boolean autoApproved, boolean approved) {
                // 若需要可在此对接实际遥测服务；当前不做操作以保持最小实现
            }

            @Override
            public void showNotificationIfEnabled(String message) {
                TaskUtils.showNotificationForApprovalIfAutoApprovalEnabled(
                        message,
                        config.getAutoApprovalSettings() != null
                                && config.getAutoApprovalSettings().isEnabled(),
                        config.getAutoApprovalSettings() != null
                                && config.getAutoApprovalSettings().isEnableNotifications(),
                        (subtitle, msg) -> {});
            }

            @Override
            public TaskConfig getConfig() {
                return config;
            }
        };
    }
}
