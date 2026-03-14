package com.hhoa.kline.core.core.context.instructions.userinstructions;

import com.hhoa.kline.core.core.prompts.ResponseFormatter;
import com.hhoa.kline.core.core.storage.GlobalFileNames;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/** 负责获取和刷新全局和本地的 Cline 规则 */
@Slf4j
public class ClineRules {

    @Getter
    public static class RuleTogglesResult {
        private final Map<String, Boolean> globalToggles;
        private final Map<String, Boolean> localToggles;

        public RuleTogglesResult(
                Map<String, Boolean> globalToggles, Map<String, Boolean> localToggles) {
            this.globalToggles = globalToggles;
            this.localToggles = localToggles;
        }
    }

    /**
     * 获取全局 Cline 规则
     *
     * @param globalClineRulesFilePath 全局 Cline 规则文件路径
     * @param toggles 切换状态
     * @return 格式化的规则指令，如果没有则返回 null
     */
    public static String getGlobalClineRules(
            String globalClineRulesFilePath, Map<String, Boolean> toggles) {
        try {
            Path path = Paths.get(globalClineRulesFilePath);

            if (Files.exists(path)) {
                if (Files.isDirectory(path)) {
                    try {
                        List<String> rulesFilePaths =
                                RuleHelpers.readDirectoryRecursive(
                                        globalClineRulesFilePath, "", null);
                        String rulesFilesTotalContent =
                                RuleHelpers.getRuleFilesTotalContent(
                                        rulesFilePaths, globalClineRulesFilePath, toggles);

                        if (rulesFilesTotalContent != null && !rulesFilesTotalContent.isEmpty()) {
                            return ResponseFormatter.clineRulesGlobalDirectoryInstructions(
                                    globalClineRulesFilePath, rulesFilesTotalContent);
                        }
                    } catch (Exception e) {
                        log.error(
                                "Failed to read .clinerules directory at "
                                        + globalClineRulesFilePath);
                        e.printStackTrace();
                    }
                } else {
                    log.error(globalClineRulesFilePath + " is not a directory");
                    return null;
                }
            }
        } catch (Exception e) {
            log.error("Error getting global cline rules: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    /**
     * 获取本地 Cline 规则
     *
     * @param cwd 当前工作目录
     * @param toggles 切换状态
     * @return 格式化的规则指令，如果没有则返回 null
     */
    public static String getLocalClineRules(String cwd, Map<String, Boolean> toggles) {
        String clineRulesFilePath = Paths.get(cwd, GlobalFileNames.CLINE_RULES).toString();
        String clineRulesFileInstructions = null;

        try {
            Path path = Paths.get(clineRulesFilePath);

            if (Files.exists(path)) {
                if (Files.isDirectory(path)) {
                    try {
                        // 排除 .clinerules/workflows 目录
                        List<List<String>> excludedPaths = new ArrayList<>();
                        excludedPaths.add(Arrays.asList(".clinerules", "workflows"));

                        List<String> rulesFilePaths =
                                RuleHelpers.readDirectoryRecursive(
                                        clineRulesFilePath, "", excludedPaths);

                        String rulesFilesTotalContent =
                                RuleHelpers.getRuleFilesTotalContent(rulesFilePaths, cwd, toggles);

                        if (rulesFilesTotalContent != null && !rulesFilesTotalContent.isEmpty()) {
                            clineRulesFileInstructions =
                                    ResponseFormatter.clineRulesLocalDirectoryInstructions(
                                            cwd, rulesFilesTotalContent);
                        }
                    } catch (Exception e) {
                        log.error("Failed to read .clinerules directory at " + clineRulesFilePath);
                        e.printStackTrace();
                    }
                } else {
                    try {
                        if (toggles.containsKey(clineRulesFilePath)
                                && !Boolean.FALSE.equals(toggles.get(clineRulesFilePath))) {
                            String ruleFileContent = Files.readString(path).trim();
                            if (!ruleFileContent.isEmpty()) {
                                clineRulesFileInstructions =
                                        ResponseFormatter.clineRulesLocalFileInstructions(
                                                cwd, ruleFileContent);
                            }
                        }
                    } catch (IOException e) {
                        log.error("Failed to read .clinerules file at " + clineRulesFilePath);
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error getting local cline rules: " + e.getMessage());
            e.printStackTrace();
        }

        return clineRulesFileInstructions;
    }

    /**
     * 刷新 Cline 规则切换状态 注意：此方法需要 TaskManager 来访问 StateManager 实际使用时需要传入 StateManager 或 TaskManager
     *
     * @param workingDirectory 工作目录
     * @param globalClineRulesToggles 全局 Cline 规则切换状态
     * @param localClineRulesToggles 本地 Cline 规则切换状态
     * @return 更新后的切换状态
     */
    public static RuleTogglesResult refreshClineRulesToggles(
            String globalClineRulesFilePath,
            String workingDirectory,
            Map<String, Boolean> globalClineRulesToggles,
            Map<String, Boolean> localClineRulesToggles) {
        try {
            Map<String, Boolean> updatedGlobalToggles =
                    RuleHelpers.synchronizeRuleToggles(
                            globalClineRulesFilePath, globalClineRulesToggles);
            // 注意：实际使用时需要调用 stateManager.setGlobalState("globalMap<String, Boolean>",
            // updatedGlobalToggles)

            // 本地切换状态
            String localClineRulesFilePath =
                    Paths.get(workingDirectory, GlobalFileNames.CLINE_RULES).toString();
            List<List<String>> excludedPaths = new ArrayList<>();
            excludedPaths.add(Arrays.asList(".clinerules", "workflows"));

            Map<String, Boolean> updatedLocalToggles =
                    RuleHelpers.synchronizeRuleToggles(
                            localClineRulesFilePath, localClineRulesToggles, "", excludedPaths);
            // 注意：实际使用时需要调用 stateManager.setWorkspaceState("localClineRulesToggles",
            // updatedLocalToggles)

            return new RuleTogglesResult(updatedGlobalToggles, updatedLocalToggles);
        } catch (Exception e) {
            log.error("Failed to refresh cline rules toggles: " + e.getMessage());
            e.printStackTrace();
            return new RuleTogglesResult(
                    globalClineRulesToggles != null ? globalClineRulesToggles : new HashMap<>(),
                    localClineRulesToggles != null ? localClineRulesToggles : new HashMap<>());
        }
    }
}
