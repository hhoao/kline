package com.hhoa.kline.core.core.context.instructions.userinstructions;

import com.hhoa.kline.core.core.prompts.ResponseFormatter;
import com.hhoa.kline.core.core.storage.GlobalFileNames;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/** 负责获取和刷新 Windsurf 和 Cursor 规则 */
@Slf4j
public class ExternalRules {

    public static class ExternalRuleTogglesResult {
        private final Map<String, Boolean> windsurfLocalToggles;
        private final Map<String, Boolean> cursorLocalToggles;

        public ExternalRuleTogglesResult(
                Map<String, Boolean> windsurfLocalToggles,
                Map<String, Boolean> cursorLocalToggles) {
            this.windsurfLocalToggles = windsurfLocalToggles;
            this.cursorLocalToggles = cursorLocalToggles;
        }

        public Map<String, Boolean> getWindsurfLocalToggles() {
            return windsurfLocalToggles;
        }

        public Map<String, Boolean> getCursorLocalToggles() {
            return cursorLocalToggles;
        }
    }

    /**
     * 刷新 Windsurf 和 Cursor 规则的切换状态 注意：此方法需要 TaskManager 来访问 StateManager
     *
     * @param workingDirectory 工作目录
     * @param localWindsurfRulesToggles 本地 Windsurf 规则切换状态
     * @param localCursorRulesToggles 本地 Cursor 规则切换状态
     * @return 更新后的切换状态
     */
    public static ExternalRuleTogglesResult refreshExternalRulesToggles(
            String workingDirectory,
            Map<String, Boolean> localWindsurfRulesToggles,
            Map<String, Boolean> localCursorRulesToggles) {

        try {
            // 本地 Windsurf 切换状态
            String localWindsurfRulesFilePath =
                    Paths.get(workingDirectory, GlobalFileNames.WINDSURF_RULES).toString();
            Map<String, Boolean> updatedLocalWindsurfToggles =
                    RuleHelpers.synchronizeRuleToggles(
                            localWindsurfRulesFilePath, localWindsurfRulesToggles);
            // 注意：实际使用时需要调用 stateManager.setWorkspaceState("localWindsurfRulesToggles",
            // updatedLocalWindsurfToggles)

            // 本地 Cursor 切换状态
            // Cursor 有两个有效的规则文件位置，因此我们需要检查两者并合并
            // synchronizeRuleToggles 将删除每个给定路径中不存在的规则文件，但合并结果将不会丢失数据
            String localCursorRulesFilePath =
                    Paths.get(workingDirectory, GlobalFileNames.CURSOR_RULES_DIR).toString();
            Map<String, Boolean> updatedLocalCursorToggles1 =
                    RuleHelpers.synchronizeRuleToggles(
                            localCursorRulesFilePath, localCursorRulesToggles, ".mdc", null);

            localCursorRulesFilePath =
                    Paths.get(workingDirectory, GlobalFileNames.CURSOR_RULES_FILE).toString();
            Map<String, Boolean> updatedLocalCursorToggles2 =
                    RuleHelpers.synchronizeRuleToggles(
                            localCursorRulesFilePath, localCursorRulesToggles);

            Map<String, Boolean> updatedLocalCursorToggles =
                    RuleHelpers.combineRuleToggles(
                            updatedLocalCursorToggles1, updatedLocalCursorToggles2);
            // 注意：实际使用时需要调用 stateManager.setWorkspaceState("localCursorRulesToggles",
            // updatedLocalCursorToggles)

            return new ExternalRuleTogglesResult(
                    updatedLocalWindsurfToggles, updatedLocalCursorToggles);
        } catch (Exception e) {
            log.error("Failed to refresh external rules toggles: " + e.getMessage());
            e.printStackTrace();
            return new ExternalRuleTogglesResult(
                    localWindsurfRulesToggles != null ? localWindsurfRulesToggles : new HashMap<>(),
                    localCursorRulesToggles != null ? localCursorRulesToggles : new HashMap<>());
        }
    }

    /**
     * 获取格式化的 Windsurf 规则
     *
     * @param cwd 当前工作目录
     * @param toggles 切换状态
     * @return 格式化的规则指令，如果没有则返回 null
     */
    public static String getLocalWindsurfRules(String cwd, Map<String, Boolean> toggles) {
        String windsurfRulesFilePath = Paths.get(cwd, GlobalFileNames.WINDSURF_RULES).toString();
        String windsurfRulesFileInstructions = null;

        try {
            Path path = Paths.get(windsurfRulesFilePath);

            if (Files.exists(path)) {
                if (!Files.isDirectory(path)) {
                    try {
                        if (toggles.containsKey(windsurfRulesFilePath)
                                && !Boolean.FALSE.equals(toggles.get(windsurfRulesFilePath))) {
                            String ruleFileContent = Files.readString(path).trim();
                            if (!ruleFileContent.isEmpty()) {
                                windsurfRulesFileInstructions =
                                        ResponseFormatter.windsurfRulesLocalFileInstructions(
                                                cwd, ruleFileContent);
                            }
                        }
                    } catch (IOException e) {
                        log.error("Failed to read .windsurfrules file at " + windsurfRulesFilePath);
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error getting local windsurf rules: " + e.getMessage());
            e.printStackTrace();
        }

        return windsurfRulesFileInstructions;
    }

    /** 可能来自两个来源 */
    public static class CursorRulesResult {
        private final String fileInstructions;
        private final String dirInstructions;

        public CursorRulesResult(String fileInstructions, String dirInstructions) {
            this.fileInstructions = fileInstructions;
            this.dirInstructions = dirInstructions;
        }

        public String getFileInstructions() {
            return fileInstructions;
        }

        public String getDirInstructions() {
            return dirInstructions;
        }
    }

    /**
     * 获取格式化的 Cursor 规则，可以来自两个来源
     *
     * @param cwd 当前工作目录
     * @param toggles 切换状态
     * @return Cursor 规则结果
     */
    public static CursorRulesResult getLocalCursorRules(String cwd, Map<String, Boolean> toggles) {
        String cursorRulesFileInstructions = null;
        String cursorRulesDirInstructions = null;

        // 首先检查 .cursorrules 文件
        String cursorRulesFilePath = Paths.get(cwd, GlobalFileNames.CURSOR_RULES_FILE).toString();

        try {
            Path path = Paths.get(cursorRulesFilePath);

            if (Files.exists(path)) {
                if (!Files.isDirectory(path)) {
                    try {
                        if (toggles.containsKey(cursorRulesFilePath)
                                && !Boolean.FALSE.equals(toggles.get(cursorRulesFilePath))) {
                            String ruleFileContent = Files.readString(path).trim();
                            if (!ruleFileContent.isEmpty()) {
                                cursorRulesFileInstructions =
                                        ResponseFormatter.cursorRulesLocalFileInstructions(
                                                cwd, ruleFileContent);
                            }
                        }
                    } catch (IOException e) {
                        log.error("Failed to read .cursorrules file at " + cursorRulesFilePath);
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error getting cursor rules file: " + e.getMessage());
            e.printStackTrace();
        }

        // 然后检查 .cursor/rules 目录
        String cursorRulesDirPath = Paths.get(cwd, GlobalFileNames.CURSOR_RULES_DIR).toString();

        try {
            Path path = Paths.get(cursorRulesDirPath);

            if (Files.exists(path)) {
                if (Files.isDirectory(path)) {
                    try {
                        List<String> rulesFilePaths =
                                RuleHelpers.readDirectoryRecursive(
                                        cursorRulesDirPath, ".mdc", null);
                        String rulesFilesTotalContent =
                                RuleHelpers.getRuleFilesTotalContent(rulesFilePaths, cwd, toggles);

                        if (rulesFilesTotalContent != null && !rulesFilesTotalContent.isEmpty()) {
                            cursorRulesDirInstructions =
                                    ResponseFormatter.cursorRulesLocalDirectoryInstructions(
                                            cwd, rulesFilesTotalContent);
                        }
                    } catch (Exception e) {
                        log.error(
                                "Failed to read .cursor/rules directory at " + cursorRulesDirPath);
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error getting cursor rules directory: " + e.getMessage());
            e.printStackTrace();
        }

        return new CursorRulesResult(cursorRulesFileInstructions, cursorRulesDirInstructions);
    }
}
