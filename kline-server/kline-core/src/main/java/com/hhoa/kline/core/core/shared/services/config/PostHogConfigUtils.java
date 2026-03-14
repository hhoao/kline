package com.hhoa.kline.core.core.shared.services.config;

/** 注意：确保开发环境不用于生产。 CI 在 CI 环境中始终为 true，在测试和发布步骤期间都是如此， 因此它不是环境的可靠指示器。 */
public class PostHogConfigUtils {
    private static boolean isTestEnv() {
        String e2eTest = System.getenv("E2E_TEST");
        String isTest = System.getenv("IS_TEST");
        return "true".equals(e2eTest) || "true".equals(isTest);
    }

    private static boolean useDevEnv() {
        String isDev = System.getenv("IS_DEV");
        String clineEnvironment = System.getenv("CLINE_ENVIRONMENT");
        return "true".equals(isDev) || "local".equals(clineEnvironment);
    }

    /**
     * 生产环境： 注意：生产环境变量将在 CI/CD 流水线中在构建时注入。 重要：密钥必须添加到 GitHub Secrets 并与
     * .github/workflows/publish.yml 工作流中定义的环境变量名称匹配。 注意：开发环境变量应从 1password 共享保险库中检索。
     */
    public static PostHogClientConfig getPostHogConfig() {
        boolean useDev = useDevEnv();
        return PostHogClientConfig.builder()
                .apiKey(System.getenv("TELEMETRY_SERVICE_API_KEY"))
                .errorTrackingApiKey(System.getenv("ERROR_SERVICE_API_KEY"))
                .host("https://data.cline.bot")
                .uiHost(useDev ? "https://us.i.posthog.com" : "https://us.posthog.com")
                .build();
    }

    public static boolean isPostHogConfigValid(PostHogClientConfig config) {
        if (isTestEnv()) {
            return false;
        }
        return config.getApiKey() != null
                && config.getErrorTrackingApiKey() != null
                && config.getHost() != null
                && config.getUiHost() != null;
    }
}
