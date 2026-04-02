package com.hhoa.kline.core.core.context.instructions.userinstructions;

import com.hhoa.kline.core.core.prompts.ResponseFormatter;
import com.hhoa.kline.core.core.shared.remoteconfig.RemoteConfigSettings;
import com.hhoa.kline.core.core.storage.GlobalFileNames;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/** 负责获取和刷新全局和本地的 Cline 规则。 */
@Slf4j
public class ClineRules {

    @Getter
    public static class RuleTogglesResult {
        private final Map<String, Boolean> globalToggles;
        private final Map<String, Boolean> localToggles;
        private final Map<String, Boolean> remoteToggles;

        public RuleTogglesResult(
                Map<String, Boolean> globalToggles,
                Map<String, Boolean> localToggles,
                Map<String, Boolean> remoteToggles) {
            this.globalToggles = globalToggles;
            this.localToggles = localToggles;
            this.remoteToggles = remoteToggles;
        }
    }

    public static String getGlobalClineRules(
            String globalClineRulesFilePath, Map<String, Boolean> toggles) {
        return getGlobalClineRules(globalClineRulesFilePath, toggles, null, null, null)
                .getInstructions();
    }

    public static RuleHelpers.RuleLoadResultWithInstructions getGlobalClineRules(
            String globalClineRulesFilePath,
            Map<String, Boolean> toggles,
            RuleConditionals.RuleEvaluationContext evaluationContext) {
        return getGlobalClineRules(
                globalClineRulesFilePath, toggles, evaluationContext, null, null);
    }

    public static RuleHelpers.RuleLoadResultWithInstructions getGlobalClineRules(
            String globalClineRulesFilePath,
            Map<String, Boolean> toggles,
            RuleConditionals.RuleEvaluationContext evaluationContext,
            RemoteConfigSettings remoteConfigSettings,
            Map<String, Boolean> remoteToggles) {
        String combinedContent = "";
        List<RuleHelpers.ActivatedConditionalRule> activatedConditionalRules = new ArrayList<>();

        try {
            Path path = Paths.get(globalClineRulesFilePath);
            if (Files.exists(path)) {
                if (Files.isDirectory(path)) {
                    try {
                        List<String> rulesFilePaths =
                                RuleHelpers.readDirectoryRecursive(
                                        globalClineRulesFilePath, "", null);
                        RuleHelpers.RuleLoadResult result =
                                RuleHelpers.getRuleFilesTotalContentWithMetadata(
                                        rulesFilePaths,
                                        globalClineRulesFilePath,
                                        toggles,
                                        new RuleHelpers.RuleLoadOptions(
                                                evaluationContext,
                                                RuleHelpers.RuleSourcePrefix.GLOBAL));
                        if (result.getContent() != null && !result.getContent().isEmpty()) {
                            combinedContent = result.getContent();
                            activatedConditionalRules.addAll(
                                    result.getActivatedConditionalRules());
                        }
                    } catch (Exception e) {
                        log.error(
                                "Failed to read .clinerules directory at {}",
                                globalClineRulesFilePath,
                                e);
                    }
                } else {
                    log.error("{} is not a directory", globalClineRulesFilePath);
                }
            }

            if (remoteConfigSettings != null
                    && remoteConfigSettings.getRemoteGlobalRules() != null
                    && !remoteConfigSettings.getRemoteGlobalRules().isEmpty()) {
                RuleHelpers.RuleLoadResult remoteResult =
                        RuleHelpers.getRemoteRulesTotalContentWithMetadata(
                                remoteConfigSettings.getRemoteGlobalRules(),
                                remoteToggles,
                                evaluationContext);
                if (remoteResult.getContent() != null && !remoteResult.getContent().isEmpty()) {
                    if (!combinedContent.isEmpty()) {
                        combinedContent += "\n\n";
                    }
                    combinedContent += remoteResult.getContent();
                    activatedConditionalRules.addAll(remoteResult.getActivatedConditionalRules());
                }
            }
        } catch (Exception e) {
            log.error("Error getting global cline rules: {}", e.getMessage(), e);
        }

        if (combinedContent.isEmpty()) {
            return new RuleHelpers.RuleLoadResultWithInstructions(null, List.of());
        }

        return new RuleHelpers.RuleLoadResultWithInstructions(
                ResponseFormatter.clineRulesGlobalDirectoryInstructions(
                        globalClineRulesFilePath, combinedContent),
                activatedConditionalRules);
    }

    public static String getLocalClineRules(String cwd, Map<String, Boolean> toggles) {
        return getLocalClineRules(cwd, toggles, null).getInstructions();
    }

    public static RuleHelpers.RuleLoadResultWithInstructions getLocalClineRules(
            String cwd,
            Map<String, Boolean> toggles,
            RuleConditionals.RuleEvaluationContext evaluationContext) {
        String clineRulesFilePath = Paths.get(cwd, GlobalFileNames.CLINE_RULES).toString();
        String instructions = null;
        List<RuleHelpers.ActivatedConditionalRule> activatedConditionalRules = new ArrayList<>();

        try {
            Path path = Paths.get(clineRulesFilePath);

            if (Files.exists(path)) {
                if (Files.isDirectory(path)) {
                    try {
                        List<List<String>> excludedPaths = new ArrayList<>();
                        excludedPaths.add(List.of("workflows"));
                        excludedPaths.add(List.of("hooks"));
                        excludedPaths.add(List.of("skills"));

                        List<String> rulesFilePaths =
                                RuleHelpers.readDirectoryRecursive(
                                        clineRulesFilePath, "", excludedPaths);

                        RuleHelpers.RuleLoadResult result =
                                RuleHelpers.getRuleFilesTotalContentWithMetadata(
                                        rulesFilePaths,
                                        clineRulesFilePath,
                                        toggles,
                                        new RuleHelpers.RuleLoadOptions(
                                                evaluationContext,
                                                RuleHelpers.RuleSourcePrefix.WORKSPACE));

                        if (result.getContent() != null && !result.getContent().isEmpty()) {
                            instructions =
                                    ResponseFormatter.clineRulesLocalDirectoryInstructions(
                                            cwd, result.getContent());
                            activatedConditionalRules.addAll(result.getActivatedConditionalRules());
                        }
                    } catch (Exception e) {
                        log.error("Failed to read .clinerules directory at {}", clineRulesFilePath, e);
                    }
                } else {
                    try {
                        if (toggles != null
                                && toggles.containsKey(clineRulesFilePath)
                                && !Boolean.FALSE.equals(toggles.get(clineRulesFilePath))) {
                            String raw = Files.readString(path).trim();
                            if (!raw.isEmpty()) {
                                FrontmatterParser.FrontmatterParseResult parsed =
                                        FrontmatterParser.parseYamlFrontmatter(raw);
                                if (parsed.isHadFrontmatter() && parsed.getParseError() != null) {
                                    instructions =
                                            ResponseFormatter.clineRulesLocalFileInstructions(
                                                    cwd, raw);
                                } else {
                                    RuleConditionals.ConditionalResult conditionalResult =
                                            RuleConditionals.evaluateRuleConditionals(
                                                    parsed.getData(),
                                                    evaluationContext != null
                                                            ? evaluationContext
                                                            : new RuleConditionals.RuleEvaluationContext(
                                                                    List.of()));
                                    if (conditionalResult.isPassed()) {
                                        instructions =
                                                ResponseFormatter.clineRulesLocalFileInstructions(
                                                        cwd, parsed.getBody().trim());
                                        if (parsed.isHadFrontmatter()
                                                && conditionalResult.getMatchedConditions() != null
                                                && !conditionalResult
                                                        .getMatchedConditions()
                                                        .isEmpty()) {
                                            activatedConditionalRules.add(
                                                    new RuleHelpers.ActivatedConditionalRule(
                                                            "workspace:"
                                                                    + GlobalFileNames.CLINE_RULES,
                                                            conditionalResult
                                                                    .getMatchedConditions()));
                                        }
                                    }
                                }
                            }
                        }
                    } catch (IOException e) {
                        log.error("Failed to read .clinerules file at {}", clineRulesFilePath, e);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error getting local cline rules: {}", e.getMessage(), e);
        }

        return new RuleHelpers.RuleLoadResultWithInstructions(
                instructions, activatedConditionalRules);
    }

    public static RuleTogglesResult refreshClineRulesToggles(
            String globalClineRulesFilePath,
            String workingDirectory,
            Map<String, Boolean> globalClineRulesToggles,
            Map<String, Boolean> localClineRulesToggles,
            RemoteConfigSettings remoteConfigSettings,
            Map<String, Boolean> remoteRulesToggles) {
        try {
            Map<String, Boolean> updatedGlobalToggles =
                    RuleHelpers.synchronizeRuleToggles(
                            globalClineRulesFilePath, globalClineRulesToggles);

            String localClineRulesFilePath =
                    Paths.get(workingDirectory, GlobalFileNames.CLINE_RULES).toString();
            List<List<String>> excludedPaths = new ArrayList<>();
            excludedPaths.add(List.of("workflows"));
            excludedPaths.add(List.of("hooks"));
            excludedPaths.add(List.of("skills"));

            Map<String, Boolean> updatedLocalToggles =
                    RuleHelpers.synchronizeRuleToggles(
                            localClineRulesFilePath, localClineRulesToggles, "", excludedPaths);
            Map<String, Boolean> updatedRemoteToggles =
                    RuleHelpers.synchronizeRemoteRuleToggles(
                            remoteConfigSettings != null
                                    ? remoteConfigSettings.getRemoteGlobalRules()
                                    : List.of(),
                            remoteRulesToggles);

            return new RuleTogglesResult(
                    updatedGlobalToggles, updatedLocalToggles, updatedRemoteToggles);
        } catch (Exception e) {
            log.error("Failed to refresh cline rules toggles: {}", e.getMessage(), e);
            return new RuleTogglesResult(
                    globalClineRulesToggles != null ? globalClineRulesToggles : new HashMap<>(),
                    localClineRulesToggles != null ? localClineRulesToggles : new HashMap<>(),
                    remoteRulesToggles != null ? remoteRulesToggles : new HashMap<>());
        }
    }
}
