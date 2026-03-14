package com.hhoa.kline.core.core.controller.utils;

import com.hhoa.kline.core.core.shared.Platform;
import java.util.UUID;

public final class ControllerUtils {

    private ControllerUtils() {}

    /**
     * 获取最新公告ID
     *
     * @param version 版本号（例如 "1.2.3"）
     * @return 公告ID字符串（主版本号.次版本号）或空字符串（如果不可用）
     */
    public static String getLatestAnnouncementId(String version) {
        if (version == null || version.isEmpty()) {
            return "";
        }
        String[] parts = version.split("\\.");
        if (parts.length >= 2) {
            return parts[0] + "." + parts[1];
        }
        return version;
    }

    /**
     * 获取唯一标识符
     *
     * @return 唯一标识符
     */
    public static String getDistinctId() {
        // 注意：实际应该从 StateManager 获取，这里提供简化实现
        // 应该从 stateManager.getGlobalStateKey("cline.generatedMachineId") 获取
        return UUID.randomUUID().toString();
    }

    /**
     * 获取平台
     *
     * @return 平台枚举
     */
    public static Platform getPlatform() {
        String osName = System.getProperty("os.name", "").toLowerCase();
        if (osName.contains("win")) {
            return Platform.WIN32;
        } else if (osName.contains("mac") || osName.contains("darwin")) {
            return Platform.DARWIN;
        } else if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) {
            return Platform.LINUX;
        }
        return Platform.LINUX;
    }

    /**
     * 获取环境
     *
     * @return 环境字符串（"production", "development", "local" 等）
     */
    public static String getEnvironment() {
        String env = System.getProperty("cline.environment");
        if (env == null || env.isEmpty()) {
            env = System.getenv("CLINE_ENVIRONMENT");
        }
        if (env == null || env.isEmpty()) {
            return "production";
        }
        return env;
    }

    /**
     * 获取版本号
     *
     * @return 版本号字符串
     */
    public static String getVersion() {
        // 注意：实际应该从配置或常量获取
        // 可以从 application.properties 或 @Value 注解获取
        String version = System.getProperty("cline.version");
        if (version == null || version.isEmpty()) {
            version = System.getenv("CLINE_VERSION");
        }
        if (version == null || version.isEmpty()) {
            return "1.0.0";
        }
        return version;
    }
}
