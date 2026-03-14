package com.hhoa.kline.core.core.prompts;

import com.hhoa.kline.core.core.ignore.ClineIgnoreController;
import com.hhoa.kline.core.core.shared.storage.types.Mode;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * 响应格式化服务 提供各种响应格式和错误处理
 *
 * @author hhoa
 */
@Slf4j
public class ResponseFormatter {

    private static final String LOCK_TEXT_SYMBOL = "\uD83D\uDD12";

    private static final String TOOL_USE_INSTRUCTIONS_REMINDER =
            """
            # Reminder: Instructions for Tool Use

            Tool uses are formatted using XML-style tags. The tool name is enclosed in opening and closing tags, and each parameter is similarly enclosed within its own set of tags. Here's the structure:

            <tool_name>
            <parameter1_name>value1</parameter1_name>
            <parameter2_name>value2</parameter2_name>
            ...
            </tool_name>

            For example:

            <attempt_completion>
            <result>
            I have completed the task...
            </result>
            </attempt_completion>

            Always adhere to this format for all tool uses to ensure proper parsing and execution.
            """;

    public String duplicateFileReadNotice() {
        return "[[NOTE] This file read has been removed to save space in the context window. Refer to the latest file read for the most up to date version of this file.]";
    }

    public String contextTruncationNotice() {
        return "[NOTE] Some previous conversation history with the user has been removed to maintain optimal context window length. The initial user task has been retained for continuity, while intermediate conversation history has been removed. Keep this in mind as you continue assisting the user. Pay special attention to the user's latest messages.";
    }

    public String processFirstUserMessageForTruncation() {
        return "[Continue assisting the user!]";
    }

    public String condense() {
        return """
            The user has accepted the condensed conversation summary you generated. This summary covers important details of the historical conversation with the user which has been truncated.
            <explicit_instructions type="condense_response">It's crucial that you respond by ONLY asking the user what you should work on next. You should NOT take any initiative or make any assumptions about continuing with work. For example you should NOT suggest file changes or attempt to read any files.
            When asking the user what you should work on next, you can reference information in the summary which was just generated. However, you should NOT reference information outside of what's contained in the summary for this response. Keep this response CONCISE.</explicit_instructions>
            """;
    }

    public String toolDenied() {
        return "The user denied this operation.";
    }

    public String toolError(String error) {
        return String.format(
                "The tool execution failed with the following error:\n<error>\n%s\n</error>",
                error != null ? error : "");
    }

    public String clineIgnoreError(String path) {
        return String.format(
                "Access to %s is blocked by the .clineignore file settings. You must try to continue in the task without using this file, or ask the user to update the .clineignore file.",
                path);
    }

    public String noToolsUsed() {
        return MessageFormat.format(
                """
                [ERROR] You did not use a tool in your previous response! Please retry with a tool use.

                {0}\

                # Next Steps

                If you have completed the user''s task, use the attempt_completion tool.\s
                If you require additional information from the user, use the ask_followup_question tool.\s
                Otherwise, if you have not completed the task and do not need additional information, then proceed with the next step of the task.\s
                (This is an automated message, so do not respond to it conversationally.)""",
                TOOL_USE_INSTRUCTIONS_REMINDER);
    }

    public String tooManyMistakes(String feedback) {
        return String.format(
                "You seem to be having trouble proceeding. The user has provided the following feedback to help guide you:\n<feedback>\n%s\n</feedback>",
                feedback != null ? feedback : "");
    }

    public String autoApprovalMaxReached(String feedback) {
        return String.format(
                "Auto-approval limit reached. The user has provided the following feedback to help guide you:\n<feedback>\n%s\n</feedback>",
                feedback != null ? feedback : "");
    }

    public String missingToolParameterError(String paramName) {
        return String.format(
                "Missing value for required parameter '%s'. Please retry with complete response.\n\n%s",
                paramName, TOOL_USE_INSTRUCTIONS_REMINDER);
    }

    public String invalidMcpToolArgumentError(String serverName, String toolName) {
        return String.format(
                "Invalid JSON argument used with %s for %s. Please retry with a properly formatted JSON argument.",
                serverName, toolName);
    }

    public String formatFilesList(
            String absolutePath,
            List<String> files,
            Boolean didHitLimit,
            ClineIgnoreController clineIgnoreController) {
        if (files == null || files.isEmpty()) {
            return "No files found.";
        }

        List<String> sorted =
                files.stream()
                        .map(
                                file -> {
                                    // 将绝对路径转换为相对路径
                                    Path basePath = Paths.get(absolutePath);
                                    Path filePath = Paths.get(file);
                                    String relativePath =
                                            basePath.relativize(filePath)
                                                    .toString()
                                                    .replace("\\", "/");
                                    return file.endsWith("/") ? relativePath + "/" : relativePath;
                                })
                        .sorted(
                                (a, b) -> {
                                    // 排序：文件列在其各自的目录下，使文件列表被截断时也能显示目录
                                    String[] aParts = a.split("/");
                                    String[] bParts = b.split("/");
                                    for (int i = 0;
                                            i < Math.min(aParts.length, bParts.length);
                                            i++) {
                                        if (!aParts[i].equals(bParts[i])) {
                                            if (i + 1 == aParts.length && i + 1 < bParts.length) {
                                                return -1;
                                            }
                                            if (i + 1 == bParts.length && i + 1 < aParts.length) {
                                                return 1;
                                            }
                                            return aParts[i].compareToIgnoreCase(bParts[i]);
                                        }
                                    }
                                    return aParts.length - bParts.length;
                                })
                        .collect(Collectors.toList());

        List<String> clineIgnoreParsed;
        if (clineIgnoreController != null) {
            clineIgnoreParsed =
                    sorted.stream()
                            .map(
                                    filePath -> {
                                        String absoluteFilePath =
                                                Paths.get(absolutePath)
                                                        .resolve(filePath)
                                                        .toString();
                                        boolean isIgnored =
                                                !clineIgnoreController.validateAccess(
                                                        absoluteFilePath);
                                        if (isIgnored) {
                                            return LOCK_TEXT_SYMBOL + " " + filePath;
                                        }
                                        return filePath;
                                    })
                            .collect(Collectors.toList());
        } else {
            clineIgnoreParsed = sorted;
        }

        if (Boolean.TRUE.equals(didHitLimit)) {
            return String.join("\n", clineIgnoreParsed)
                    + "\n\n(File list truncated. Use list_files on specific subdirectories if you need to explore further.)";
        } else if (clineIgnoreParsed.isEmpty()
                || (clineIgnoreParsed.size() == 1 && clineIgnoreParsed.get(0).isEmpty())) {
            return "No files found.";
        } else {
            return String.join("\n", clineIgnoreParsed);
        }
    }

    public String createPrettyPatch(String filename, String oldStr, String newStr) {
        String normalizedFilename = toPosix(filename != null ? filename : "file");
        String oldContent = oldStr != null ? oldStr : "";
        String newContent = newStr != null ? newStr : "";

        String patch = generateUnifiedDiff(normalizedFilename, oldContent, newContent);

        // 去掉前4行（header）：Index, ===, ---, +++
        String[] lines = patch.split("\n");
        if (lines.length > 4) {
            return String.join("\n", Arrays.asList(lines).subList(4, lines.length));
        }

        return patch;
    }

    /**
     * 生成统一差异格式（类似 diff -u）
     *
     * @param filePath 文件路径
     * @param oldContent 旧内容
     * @param newContent 新内容
     * @return 统一差异格式字符串
     */
    private String generateUnifiedDiff(String filePath, String oldContent, String newContent) {
        StringBuilder patch = new StringBuilder();
        patch.append("Index: ").append(filePath).append("\n");
        patch.append("===================================================================\n");
        patch.append("--- ").append(filePath).append("\n");
        patch.append("+++ ").append(filePath).append("\n");

        String diffContent = generateSimpleDiff(oldContent, newContent);
        patch.append(diffContent);

        return patch.toString();
    }

    /**
     * 生成简单差异
     *
     * @param oldContent 旧内容
     * @param newContent 新内容
     * @return 差异字符串
     */
    private String generateSimpleDiff(String oldContent, String newContent) {
        String[] oldLines = oldContent.split("\n", -1);
        String[] newLines = newContent.split("\n", -1);

        StringBuilder diff = new StringBuilder();
        int maxLen = Math.max(oldLines.length, newLines.length);

        for (int i = 0; i < maxLen; i++) {
            String oldLine = i < oldLines.length ? oldLines[i] : null;
            String newLine = i < newLines.length ? newLines[i] : null;

            if (oldLine == null) {
                diff.append("+").append(newLine).append("\n");
            } else if (newLine == null) {
                diff.append("-").append(oldLine).append("\n");
            } else if (!oldLine.equals(newLine)) {
                diff.append("-").append(oldLine).append("\n");
                diff.append("+").append(newLine).append("\n");
            }
        }

        return diff.toString();
    }

    public String[] taskResumption(
            Mode mode,
            String agoText,
            String cwd,
            Boolean wasRecent,
            String responseText,
            Boolean hasPendingFileContextWarnings) {
        String cwdPosix = toPosix(cwd);
        String taskResumptionMessage =
                "[TASK RESUMPTION] "
                        + (mode == Mode.PLAN
                                ? String.format(
                                        """
                                        This task was interrupted %s. The conversation may have been incomplete. Be aware that the project state may have changed since then. The current working directory is now '%s'.

                                        Note: If you previously attempted a tool use that the user did not provide a result for, you should assume the tool use was not successful. However you are in PLAN MODE, so rather than continuing the task, you must respond to the user's message.""",
                                        agoText, cwdPosix)
                                : String.format(
                                        """
                                        This task was interrupted %s. It may or may not be complete, so please reassess the task context. Be aware that the project state may have changed since then. The current working directory is now '%s'. If the task has not been completed, retry the last step before interruption and proceed with completing the task.

                                        Note: If you previously attempted a tool use that the user did not provide a result for, you should assume the tool use was not successful and assess whether you should retry. If the last tool was a browser_action, the browser has been closed and you must launch a new browser if needed.""",
                                        agoText, cwdPosix));

        if (Boolean.TRUE.equals(wasRecent) && !Boolean.TRUE.equals(hasPendingFileContextWarnings)) {
            taskResumptionMessage +=
                    "\n\nIMPORTANT: If the last tool use was a replace_in_file or write_to_file that was interrupted, the file was reverted back to its original state before the interrupted edit, and you do NOT need to re-read the file as you already have its up-to-date contents.";
        }

        String userResponseMessage =
                responseText != null
                        ? String.format(
                                """
                                %s:
                                <user_message>
                                %s
                                </user_message>""",
                                mode == Mode.PLAN
                                        ? "New message to respond to with plan_mode_respond tool (be sure to provide your response in the <response> parameter)"
                                        : "New instructions for task continuation",
                                responseText)
                        : mode == Mode.PLAN
                                ? "(The user did not provide a new message. Consider asking them how they'd like you to proceed, or suggest to them to switch to Act mode to continue with the task.)"
                                : "";

        return new String[] {taskResumptionMessage, userResponseMessage};
    }

    public String planModeInstructions() {
        return """
            In this mode you should focus on information gathering, asking questions, and architecting a solution. Once you have a plan, use the plan_mode_respond tool to engage in a conversational back and forth with the user. Do not use the plan_mode_respond tool until you've gathered all the information you need e.g. with read_file or ask_followup_question.
            (Remember: If it seems the user wants you to use tools only available in Act Mode, you should ask the user to "toggle to Act mode" (use those words) - they will have to manually do this themselves with the Plan/Act toggle button below. You do not have the ability to switch to Act Mode yourself, and must wait for the user to do it themselves once they are satisfied with the plan. You also cannot present an option to toggle to Act mode, as this will be something you need to direct the user to do manually themselves.)
            """
                .trim();
    }

    public String fileEditWithUserChanges(
            String relPath,
            String userEdits,
            String autoFormattingEdits,
            String finalContent,
            String newProblemsMessage) {
        String relPathPosix = toPosix(relPath);
        StringBuilder result = new StringBuilder();
        result.append(
                        """
                The user made the following updates to your content:

                """)
                .append(userEdits)
                .append("\n\n");

        if (autoFormattingEdits != null && !autoFormattingEdits.isEmpty()) {
            result.append(
                            """
                    The user's editor also applied the following auto-formatting to your content:

                    """)
                    .append(autoFormattingEdits)
                    .append(
                            """

                    (Note: Pay close attention to changes such as single quotes being converted to double quotes, semicolons being removed or added, long lines being broken into multiple lines, adjusting indentation style, adding/removing trailing commas, etc. This will help you ensure future SEARCH/REPLACE operations to this file are accurate.)

                    """);
        }

        result.append(
                        String.format(
                                """
                The updated content, which includes both your original modifications and the additional edits, has been successfully saved to %s. Here is the full, updated content of the file that was saved:

                <final_file_content path="%s">
                %s
                </final_file_content>

                Please note:
                1. You do not need to re-write the file with these changes, as they have already been applied.
                2. Proceed with the task using this updated file content as the new baseline.
                3. If the user's edits have addressed part of the task or changed the requirements, adjust your approach accordingly.
                4. IMPORTANT: For any future changes to this file, use the final_file_content shown above as your reference. This content reflects the current state of the file, including both user edits and any auto-formatting (e.g., if you used single quotes but the formatter converted them to double quotes). Always base your SEARCH/REPLACE operations on this final version to ensure accuracy.
                """,
                                relPathPosix,
                                relPathPosix,
                                finalContent != null ? finalContent : ""))
                .append(newProblemsMessage != null ? newProblemsMessage : "");

        return result.toString();
    }

    public String fileEditWithoutUserChanges(
            String relPath,
            String autoFormattingEdits,
            String finalContent,
            String newProblemsMessage) {
        String relPathPosix = toPosix(relPath);
        StringBuilder result = new StringBuilder();
        result.append(
                String.format(
                        """
                The content was successfully saved to %s.

                """,
                        relPathPosix));

        if (autoFormattingEdits != null && !autoFormattingEdits.isEmpty()) {
            result.append(
                            """
                    Along with your edits, the user's editor applied the following auto-formatting to your content:

                    """)
                    .append(autoFormattingEdits)
                    .append(
                            """

                    (Note: Pay close attention to changes such as single quotes being converted to double quotes, semicolons being removed or added, long lines being broken into multiple lines, adjusting indentation style, adding/removing trailing commas, etc. This will help you ensure future SEARCH/REPLACE operations to this file are accurate.)

                    """);
        }

        result.append(
                        String.format(
                                """
                Here is the full, updated content of the file that was saved:

                <final_file_content path="%s">
                %s
                </final_file_content>

                IMPORTANT: For any future changes to this file, use the final_file_content shown above as your reference. This content reflects the current state of the file, including any auto-formatting (e.g., if you used single quotes but the formatter converted them to double quotes). Always base your SEARCH/REPLACE operations on this final version to ensure accuracy.

                """,
                                relPathPosix, finalContent != null ? finalContent : ""))
                .append(newProblemsMessage != null ? newProblemsMessage : "");

        return result.toString();
    }

    public String diffError(String relPath, String originalContent) {
        String relPathPosix = toPosix(relPath);
        return MessageFormat.format(
                """
            This is likely because the SEARCH block content doesn''t match exactly with what''s in the file, or if you used multiple SEARCH/REPLACE blocks they may not have been in the order they appear in the file. (Please also ensure that when using the replace_in_file tool, Do NOT add extra characters to the markers (e.g., ------- SEARCH> is INVALID). Do NOT forget to use the closing +++++++ REPLACE marker. Do NOT modify the marker format in any way. Malformed XML will cause complete tool failure and break the entire editing process.)

            The file was reverted to its original state:

            {0}Now that you have the latest state of the file, try the operation again with fewer, more precise SEARCH blocks. For large files especially, it may be prudent to try to limit yourself to <5 SEARCH/REPLACE blocks at a time, then wait for the user to respond with the result of the operation before following up with another replace_in_file call to make additional edits.
            (If you run into this error 3 times in a row, you may use the write_to_file tool as a fallback.)
            """,
                String.format(
                        "<file_content path=\"%s\">\n%s\n</file_content>\n\n",
                        relPathPosix, originalContent != null ? originalContent : ""));
    }

    public String toolAlreadyUsed(String toolName) {
        return String.format(
                "Tool [%s] was not executed because a tool has already been used in this message. Only one tool may be used per message. You must assess the first tool's result before proceeding to use the next tool.",
                toolName);
    }

    public String clineIgnoreInstructions(String content) {
        return String.format(
                """
                # .clineignore

                (The following is provided by a root-level .clineignore file where the user has specified files and directories that should not be accessed. When using list_files, you'll notice a %s next to files that are blocked. Attempting to access the file's contents e.g. through read_file will result in an error.)

                %s
                .clineignore""",
                LOCK_TEXT_SYMBOL, content);
    }

    /**
     * Cline 规则全局目录指令
     *
     * @param globalClineRulesFilePath 全局 Cline 规则文件路径
     * @param content 规则内容
     * @return 格式化的指令
     */
    public static String clineRulesGlobalDirectoryInstructions(
            String globalClineRulesFilePath, String content) {
        return String.format(
                """
                # .clinerules/

                The following is provided by a global .clinerules/ directory, located at %s, where the user has specified instructions for all working directories:

                %s""",
                toPosix(globalClineRulesFilePath), content);
    }

    /**
     * 将路径转换为 POSIX 格式（将反斜杠转换为正斜杠）
     *
     * @param path 路径
     * @return POSIX 格式的路径
     */
    private static String toPosix(String path) {
        if (path == null) {
            return null;
        }
        return path.replace('\\', '/');
    }

    /**
     * Cline 规则本地目录指令
     *
     * @param cwd 当前工作目录
     * @param content 规则内容
     * @return 格式化的指令
     */
    public static String clineRulesLocalDirectoryInstructions(String cwd, String content) {
        return String.format(
                """
                # .clinerules/

                The following is provided by a root-level .clinerules/ directory where the user has specified instructions for this working directory (%s)

                %s""",
                toPosix(cwd), content);
    }

    /**
     * Cline 规则本地文件指令
     *
     * @param cwd 当前工作目录
     * @param content 规则文件内容
     * @return 格式化的指令
     */
    public static String clineRulesLocalFileInstructions(String cwd, String content) {
        return String.format(
                """
                # .clinerules

                The following is provided by a root-level .clinerules file where the user has specified instructions for this working directory (%s)

                %s""",
                toPosix(cwd), content);
    }

    /**
     * Windsurf 规则本地文件指令
     *
     * @param cwd 当前工作目录
     * @param content 规则文件内容
     * @return 格式化的指令
     */
    public static String windsurfRulesLocalFileInstructions(String cwd, String content) {
        return String.format(
                """
                # .windsurfrules

                The following is provided by a root-level .windsurfrules file where the user has specified instructions for this working directory (%s)

                %s""",
                toPosix(cwd), content);
    }

    /**
     * Cursor 规则本地文件指令
     *
     * @param cwd 当前工作目录
     * @param content 规则文件内容
     * @return 格式化的指令
     */
    public static String cursorRulesLocalFileInstructions(String cwd, String content) {
        return String.format(
                """
                # .cursorrules

                The following is provided by a root-level .cursorrules file where the user has specified instructions for this working directory (%s)

                %s""",
                toPosix(cwd), content);
    }

    /**
     * Cursor 规则本地目录指令
     *
     * @param cwd 当前工作目录
     * @param content 规则内容
     * @return 格式化的指令
     */
    public static String cursorRulesLocalDirectoryInstructions(String cwd, String content) {
        return String.format(
                """
                # .cursor/rules

                The following is provided by a root-level .cursor/rules directory where the user has specified instructions for this working directory (%s)

                %s""",
                toPosix(cwd), content);
    }

    public String completeTruncatedContent(String truncatedContent) {
        return String.format(
                """
        请继续补全因maxTokens限制被截断的工具调用内容（仅输出截断后的剩余部分，需严格匹配对应工具的闭合格式）:
        ```
        %s
        ```
        ### 补全规则
        1. 标签闭合优先：先检查并补全缺失的闭合标签（如`</content>`、`</write_to_file>`、`</read_file>`等），确保工具调用结构完整。
        2. 内容片段补全：基于上下文逻辑补全缺失的内容（如截断的代码、未写完的路径），补全内容需与前文语义连贯、格式一致。
        3. 无额外创作：仅补全被截断的部分，不新增与前文无关的内容或修改已输出的完整内容。
        ### 输入输出规范
        - 输入：包含被截断的工具调用内容的上下文文本。
        - 输出：仅输出补全后的工具调用剩余部分（无需重复已输出的完整内容），确保与前文拼接后形成完整、合法的工具调用。
        """,
                truncatedContent);
    }

    /**
     * 工具结果格式化
     *
     * @param text 文本内容
     * @param images 图片数组（可选）
     * @param fileString 文件内容（可选）
     * @return 格式化的工具结果
     */
    public String toolResult(String text, String[] images, String fileString) {
        if ((images == null || images.length == 0)
                && (fileString == null || fileString.isEmpty())) {
            return text != null ? text : "";
        }

        StringBuilder result = new StringBuilder();
        if (text != null) {
            result.append(text);
        }

        if (fileString != null && !fileString.isEmpty()) {
            if (result.length() > 0) {
                result.append("\n\n");
            }
            result.append(fileString);
        }

        return result.toString();
    }

    /**
     * 文件上下文警告
     *
     * @param editedFiles 被编辑的文件列表
     * @return 格式化的警告消息
     */
    public String fileContextWarning(List<String> editedFiles) {
        if (editedFiles == null || editedFiles.isEmpty()) {
            return "";
        }

        int fileCount = editedFiles.size();
        String fileVerb = fileCount == 1 ? "file has" : "files have";
        String fileDemonstrativePronoun = fileCount == 1 ? "this file" : "these files";
        String filePersonalPronoun = fileCount == 1 ? "it" : "they";

        String fileList =
                editedFiles.stream()
                        .map(file -> " " + toPosix(Paths.get(file).toString()))
                        .collect(Collectors.joining("\n"));

        return MessageFormat.format(
                """
                <explicit_instructions>
                CRITICAL FILE STATE ALERT: {0} {1} been externally modified since your last interaction. Your cached understanding of {2} is now stale and unreliable. Before making ANY modifications to {3}, you must execute read_file to obtain the current state, as {4} may contain completely different content than what you expect:
                {5}
                Failure to re-read before editing will result in replace_in_file edit errors, requiring subsequent attempts and wasting tokens. You DO NOT need to re-read these files after subsequent edits, unless instructed to do so.
                </explicit_instructions>
                """,
                fileCount,
                fileVerb,
                fileDemonstrativePronoun,
                fileDemonstrativePronoun,
                filePersonalPronoun,
                fileList);
    }
}
