package com.hhoa.kline.core.core.tools.subagent;

/** 与 Cline {@code SubagentToolName.ts} 一致的动态子代理工具名生成。 */
public final class SubagentToolName {

    private static final String SUBAGENT_TOOL_NAME_PREFIX = "use_subagent_";
    private static final int SUBAGENT_TOOL_NAME_MAX_LENGTH = 64;

    private SubagentToolName() {}

    public static String buildSubagentToolName(String agentName) {
        String sanitized = sanitizeAgentName(agentName);
        if (sanitized.isEmpty()) {
            sanitized = "agent";
        }
        String hashFull = hashString(agentName);
        String hashSuffix = hashFull.substring(0, Math.min(6, hashFull.length()));
        String base = SUBAGENT_TOOL_NAME_PREFIX + sanitized;

        if (base.length() <= SUBAGENT_TOOL_NAME_MAX_LENGTH) {
            return base;
        }

        int maxBodyLength =
                Math.max(
                        1,
                        SUBAGENT_TOOL_NAME_MAX_LENGTH
                                - SUBAGENT_TOOL_NAME_PREFIX.length()
                                - hashSuffix.length()
                                - 1);
        String body = sanitized.substring(0, Math.min(sanitized.length(), maxBodyLength));
        String candidate = SUBAGENT_TOOL_NAME_PREFIX + body + "_" + hashSuffix;
        return trimToolNameToMax(candidate);
    }

    public static boolean isSubagentToolName(String toolName) {
        return toolName != null && toolName.startsWith(SUBAGENT_TOOL_NAME_PREFIX);
    }

    private static String sanitizeAgentName(String name) {
        if (name == null) {
            return "";
        }
        String trimmed = name.trim().toLowerCase();
        String replaced = trimmed.replaceAll("[^a-z0-9_]+", "_");
        replaced = replaced.replaceAll("_+", "_");
        replaced = replaced.replaceAll("^_+|_+$", "");
        return replaced;
    }

    /** FNV-1a 与 TS {@code hashString} 一致，输出无符号 36 进制字符串。 */
    static String hashString(String value) {
        int hash = (int) 2166136261L;
        for (int i = 0; i < value.length(); i++) {
            hash ^= value.charAt(i);
            hash *= 16777619;
        }
        long unsigned = hash & 0xffffffffL;
        return Long.toString(unsigned, 36);
    }

    private static String trimToolNameToMax(String value) {
        if (value.length() <= SUBAGENT_TOOL_NAME_MAX_LENGTH) {
            return value;
        }
        return value.substring(0, SUBAGENT_TOOL_NAME_MAX_LENGTH);
    }
}
