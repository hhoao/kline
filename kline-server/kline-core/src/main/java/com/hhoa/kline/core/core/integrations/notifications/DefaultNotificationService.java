package com.hhoa.kline.core.core.integrations.notifications;

import lombok.extern.slf4j.Slf4j;

/**
 * 默认通知服务实现 使用日志输出通知（可以根据需要替换为实际的通知系统）
 *
 * @author hhoa
 */
@Slf4j
public class DefaultNotificationService implements NotificationService {

    @Override
    public void showNotification(String title, String message, NotificationType type) {
        String formattedMessage = String.format("[%s] %s: %s", type, title, message);

        switch (type) {
            case INFO:
                log.info(formattedMessage);
                break;
            case WARNING:
                log.warn(formattedMessage);
                break;
            case ERROR:
                log.error(formattedMessage);
                break;
            case SUCCESS:
                log.info(formattedMessage);
                break;
            default:
                log.info(formattedMessage);
                break;
        }

        // TODO: 在实际实现中，这里可以集成真实的通知系统
        // 例如：
        // - 桌面通知（使用 JNA 或 AWT SystemTray）
        // - WebSocket 推送到前端
        // - 邮件通知
        // - 短信通知
        // - 企业微信/钉钉通知
    }
}
