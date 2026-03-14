package com.hhoa.kline.core.core.integrations.subagents;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SubagentCommandUtils {
    private static final Pattern CLINE_COMMAND_PATTERN =
            Pattern.compile("^cline\\s+(['\"])(.+?)\\1(\\s+.*)?$");

    public static boolean isSubagentCommand(String command) {
        if (command == null || command.trim().isEmpty()) {
            return false;
        }
        return CLINE_COMMAND_PATTERN.matcher(command.trim()).matches();
    }

    public static String transformClineCommand(String command) {
        if (!isSubagentCommand(command)) {
            return command;
        }
        return injectSubagentSettings(command);
    }

    private static String injectSubagentSettings(String command) {
        String[] postPromptFlags = {
            "-s",
            "yolo_mode_toggled=true",
            "-s",
            "max_consecutive_mistakes=6",
            "-F",
            "plain",
            "-y",
            "--oneshot"
        };

        Matcher matcher = CLINE_COMMAND_PATTERN.matcher(command);
        if (matcher.matches()) {
            String quote = matcher.group(1);
            String prompt = matcher.group(2);
            String additionalFlags = matcher.group(3) != null ? matcher.group(3) : "";
            String flagsString = String.join(" ", postPromptFlags);
            return String.format(
                    "cline %s%s%s %s%s", quote, prompt, quote, flagsString, additionalFlags);
        }

        String[] parts = command.split("\\s+");
        int promptEndIndex = -1;
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].endsWith("\"") || parts[i].endsWith("'")) {
                promptEndIndex = i;
                break;
            }
        }
        if (promptEndIndex != -1 && promptEndIndex + 1 < parts.length) {
            StringBuilder result = new StringBuilder();
            for (int i = 0; i <= promptEndIndex; i++) {
                if (i > 0) result.append(" ");
                result.append(parts[i]);
            }
            for (String flag : postPromptFlags) {
                result.append(" ").append(flag);
            }
            for (int i = promptEndIndex + 1; i < parts.length; i++) {
                result.append(" ").append(parts[i]);
            }
            return result.toString();
        }

        return command;
    }
}
