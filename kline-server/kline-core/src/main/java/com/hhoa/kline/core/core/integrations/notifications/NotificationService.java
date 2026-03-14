package com.hhoa.kline.core.core.integrations.notifications;

/**
 * 通知服务接口 用于显示系统通知
 *
 * @author hhoa
 */
public interface NotificationService {
    /**
     * 显示通知
     *
     * @param title 标题
     * @param message 消息
     * @param type 通知类型
     */
    void showNotification(String title, String message, NotificationType type);

    /**
     * 显示信息通知
     *
     * @param title 标题
     * @param message 消息
     */
    default void showInfo(String title, String message) {
        showNotification(title, message, NotificationType.INFO);
    }

    /**
     * 显示警告通知
     *
     * @param title 标题
     * @param message 消息
     */
    default void showWarning(String title, String message) {
        showNotification(title, message, NotificationType.WARNING);
    }

    /**
     * 显示错误通知
     *
     * @param title 标题
     * @param message 消息
     */
    default void showError(String title, String message) {
        showNotification(title, message, NotificationType.ERROR);
    }

    /**
     * 显示成功通知
     *
     * @param title 标题
     * @param message 消息
     */
    default void showSuccess(String title, String message) {
        showNotification(title, message, NotificationType.SUCCESS);
    }

    /**
     * 显示审批通知（如果启用了自动批准）
     *
     * @param message 消息
     * @param autoApprovalEnabled 是否启用自动批准
     * @param notificationsEnabled 是否启用通知
     */
    default void showNotificationForApprovalIfAutoApprovalEnabled(
            String message, boolean autoApprovalEnabled, boolean notificationsEnabled) {
        if (autoApprovalEnabled && notificationsEnabled) {
            showInfo("Auto-Approval", message);
        }
    }
}
