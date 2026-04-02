package com.hhoa.kline.core.core.task;

import lombok.extern.slf4j.Slf4j;

/**
 * Presentation cadence configuration.
 *
 * <p>Determines flush delay based on whether the workspace is remote and the flush priority.
 * Cadence overrides can be set via system properties (read once at class load).
 */
@Slf4j
public final class PresentationLatency {

    private static final int DEFAULT_LOCAL_CADENCE_MS = 40;
    private static final int DEFAULT_REMOTE_CADENCE_MS = 90;

    private static final Integer LOCAL_CADENCE_OVERRIDE;
    private static final Integer REMOTE_CADENCE_OVERRIDE;
    private static final boolean SCHEDULING_DISABLED;

    static {
        LOCAL_CADENCE_OVERRIDE = readCadenceOverride("KLINE_PRESENTATION_CADENCE_MS");
        REMOTE_CADENCE_OVERRIDE = readCadenceOverride("KLINE_REMOTE_PRESENTATION_CADENCE_MS");
        SCHEDULING_DISABLED = readBooleanEnv("KLINE_DISABLE_PRESENTATION_SCHEDULER");
    }

    private PresentationLatency() {}

    /**
     * Whether the host is connected to a remote workspace. The primary signal is {@code remoteName}
     * which is populated from the IDE's remote environment info (e.g. "ssh-remote",
     * "dev-container"). When present the host is definitively remote.
     */
    public static boolean isRemoteWorkspaceEnvironment(String remoteName) {
        return remoteName != null && !remoteName.isEmpty();
    }

    /** Whether the presentation scheduler is disabled via environment variable. */
    public static boolean isPresentationSchedulingDisabled() {
        return SCHEDULING_DISABLED;
    }

    /**
     * Get the flush delay in milliseconds for the given priority and workspace type.
     *
     * @param isRemoteWorkspace whether the workspace is remote
     * @param priority the flush priority
     * @return delay in milliseconds (0 for immediate)
     */
    public static int getPresentationCadenceMs(
            boolean isRemoteWorkspace, PresentationPriority priority) {
        if (priority == PresentationPriority.IMMEDIATE) {
            return 0;
        }

        Integer override = isRemoteWorkspace ? REMOTE_CADENCE_OVERRIDE : LOCAL_CADENCE_OVERRIDE;
        if (override != null) {
            return override;
        }

        return isRemoteWorkspace ? DEFAULT_REMOTE_CADENCE_MS : DEFAULT_LOCAL_CADENCE_MS;
    }

    private static Integer readCadenceOverride(String envVarName) {
        String rawValue = System.getenv(envVarName);
        if (rawValue == null || rawValue.isEmpty()) {
            return null;
        }
        try {
            int parsed = Integer.parseInt(rawValue);
            if (parsed < 0) {
                log.warn(
                        "[latency] Ignoring invalid cadence override {}=\"{}\" (must be non-negative)",
                        envVarName,
                        rawValue);
                return null;
            }
            return parsed;
        } catch (NumberFormatException e) {
            log.warn(
                    "[latency] Ignoring invalid cadence override {}=\"{}\" (must be an integer)",
                    envVarName,
                    rawValue);
            return null;
        }
    }

    private static boolean readBooleanEnv(String envVarName) {
        String rawValue = System.getenv(envVarName);
        if (rawValue == null) {
            return false;
        }
        String lower = rawValue.toLowerCase();
        return "1".equals(lower) || "true".equals(lower) || "yes".equals(lower);
    }
}
