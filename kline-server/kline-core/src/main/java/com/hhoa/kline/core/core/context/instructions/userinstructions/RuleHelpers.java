package com.hhoa.kline.core.core.context.instructions.userinstructions;

import com.hhoa.kline.core.core.shared.remoteconfig.GlobalInstructionsFile;
import com.hhoa.kline.core.core.storage.GlobalFileNames;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RuleHelpers {

    @Getter
    @AllArgsConstructor
    public static class ActivatedConditionalRule {
        private final String name;
        private final Map<String, List<String>> matchedConditions;
    }

    @Getter
    @AllArgsConstructor
    public static class RuleLoadResult {
        private final String content;
        private final List<ActivatedConditionalRule> activatedConditionalRules;
    }

    @Getter
    @AllArgsConstructor
    public static class RuleLoadResultWithInstructions {
        private final String instructions;
        private final List<ActivatedConditionalRule> activatedConditionalRules;
    }

    @Getter
    @AllArgsConstructor
    public static class RuleLoadOptions {
        private final RuleConditionals.RuleEvaluationContext evaluationContext;
        private final RuleSourcePrefix ruleNamePrefix;
    }

    public enum RuleSourcePrefix {
        WORKSPACE("workspace"),
        GLOBAL("global"),
        REMOTE("remote");

        private final String value;

        RuleSourcePrefix(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public static List<String> readDirectoryRecursive(
            String directoryPath, String allowedFileExtension, List<List<String>> excludedPaths) {
        try {
            List<String> entries = readDirectory(directoryPath, excludedPaths);
            List<String> results = new ArrayList<>();

            for (String entry : entries) {
                if (allowedFileExtension != null && !allowedFileExtension.isEmpty()) {
                    String fileExtension = getFileExtension(entry);
                    if (!fileExtension.equals(allowedFileExtension)) {
                        continue;
                    }
                }
                results.add(entry);
            }
            return results;
        } catch (Exception e) {
            log.error("Error reading directory {}: {}", directoryPath, e.getMessage());
            return new ArrayList<>();
        }
    }

    private static List<String> readDirectory(
            String directoryPath, List<List<String>> excludedPaths) throws IOException {
        Path dir = Paths.get(directoryPath);
        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            return new ArrayList<>();
        }

        Set<String> excludedSet = new HashSet<>();
        if (excludedPaths != null) {
            for (List<String> pathParts : excludedPaths) {
                excludedSet.add(String.join(File.separator, pathParts));
            }
        }

        try (Stream<Path> paths = Files.walk(dir)) {
            return paths.filter(Files::isRegularFile)
                    .map(p -> dir.relativize(p).toString())
                    .filter(p -> !isExcluded(p, excludedSet))
                    .collect(Collectors.toList());
        }
    }

    private static boolean isExcluded(String path, Set<String> excludedSet) {
        for (String excluded : excludedSet) {
            if (path.startsWith(excluded)) {
                return true;
            }
        }
        return false;
    }

    private static String getFileExtension(String filePath) {
        int lastDot = filePath.lastIndexOf('.');
        if (lastDot > 0 && lastDot < filePath.length() - 1) {
            return filePath.substring(lastDot);
        }
        return "";
    }

    public static Map<String, Boolean> synchronizeRuleToggles(
            String rulesDirectoryPath,
            Map<String, Boolean> currentToggles,
            String allowedFileExtension,
            List<List<String>> excludedPaths) {

        Map<String, Boolean> updatedToggles =
                new HashMap<>(currentToggles != null ? currentToggles : new HashMap<>());

        try {
            Path path = Paths.get(rulesDirectoryPath);
            boolean pathExists = Files.exists(path);

            if (pathExists) {
                boolean isDir = Files.isDirectory(path);

                if (isDir) {
                    List<String> filePaths =
                            readDirectoryRecursive(
                                    rulesDirectoryPath,
                                    allowedFileExtension != null ? allowedFileExtension : "",
                                    excludedPaths);
                    Set<String> existingRulePaths = new HashSet<>();

                    for (String filePath : filePaths) {
                        String ruleFilePath = Paths.get(rulesDirectoryPath, filePath).toString();
                        existingRulePaths.add(ruleFilePath);

                        if (!updatedToggles.containsKey(ruleFilePath)) {
                            updatedToggles.put(ruleFilePath, true);
                        }
                    }

                    List<String> keysToRemove = new ArrayList<>();
                    for (String togglePath : updatedToggles.keySet()) {
                        if (!existingRulePaths.contains(togglePath)) {
                            keysToRemove.add(togglePath);
                        }
                    }
                    keysToRemove.forEach(updatedToggles::remove);
                } else {
                    if (!updatedToggles.containsKey(rulesDirectoryPath)) {
                        updatedToggles.put(rulesDirectoryPath, true);
                    }

                    List<String> keysToRemove = new ArrayList<>();
                    for (String togglePath : updatedToggles.keySet()) {
                        if (!togglePath.equals(rulesDirectoryPath)) {
                            keysToRemove.add(togglePath);
                        }
                    }
                    keysToRemove.forEach(updatedToggles::remove);
                }
            } else {
                updatedToggles.clear();
            }
        } catch (Exception e) {
            log.error("Failed to synchronize rule toggles for path: {}", rulesDirectoryPath, e);
        }

        return updatedToggles;
    }

    public static Map<String, Boolean> synchronizeRuleToggles(
            String rulesDirectoryPath, Map<String, Boolean> currentToggles) {
        return synchronizeRuleToggles(rulesDirectoryPath, currentToggles, "", null);
    }

    public static Map<String, Boolean> synchronizeRemoteRuleToggles(
            List<GlobalInstructionsFile> remoteRules, Map<String, Boolean> currentToggles) {
        Map<String, Boolean> updatedToggles =
                new HashMap<>(currentToggles != null ? currentToggles : new HashMap<>());
        Set<String> existingRuleNames =
                remoteRules == null
                        ? Collections.emptySet()
                        : remoteRules.stream()
                                .map(GlobalInstructionsFile::getName)
                                .collect(Collectors.toSet());

        updatedToggles.entrySet().removeIf(entry -> !existingRuleNames.contains(entry.getKey()));

        if (remoteRules != null) {
            for (GlobalInstructionsFile rule : remoteRules) {
                updatedToggles.putIfAbsent(rule.getName(), true);
            }
        }

        return updatedToggles;
    }

    public static Map<String, Boolean> combineRuleToggles(
            Map<String, Boolean> toggles1, Map<String, Boolean> toggles2) {
        Map<String, Boolean> combined = new HashMap<>(toggles1 != null ? toggles1 : Map.of());
        if (toggles2 != null) {
            combined.putAll(toggles2);
        }
        return combined;
    }

    public static String getRuleFilesTotalContent(
            List<String> rulesFilePaths, String basePath, Map<String, Boolean> toggles) {
        return getRuleFilesTotalContentWithMetadata(rulesFilePaths, basePath, toggles, null)
                .getContent();
    }

    public static RuleLoadResult getRuleFilesTotalContentWithMetadata(
            List<String> rulesFilePaths,
            String basePath,
            Map<String, Boolean> toggles,
            RuleLoadOptions options) {
        List<String> contents = new ArrayList<>();
        List<ActivatedConditionalRule> activatedConditionalRules = new ArrayList<>();
        RuleConditionals.RuleEvaluationContext evaluationContext =
                options != null && options.getEvaluationContext() != null
                        ? options.getEvaluationContext()
                        : new RuleConditionals.RuleEvaluationContext(Collections.emptyList());
        RuleSourcePrefix prefix =
                options != null && options.getRuleNamePrefix() != null
                        ? options.getRuleNamePrefix()
                        : RuleSourcePrefix.GLOBAL;

        for (String filePath : rulesFilePaths) {
            try {
                Path ruleFilePath = Paths.get(basePath, filePath);
                String ruleFilePathStr = ruleFilePath.toString();
                Path ruleFilePathRelative = Paths.get(basePath).relativize(ruleFilePath);

                if (toggles != null
                        && toggles.containsKey(ruleFilePathStr)
                        && Boolean.FALSE.equals(toggles.get(ruleFilePathStr))) {
                    continue;
                }

                String raw = Files.readString(ruleFilePath).trim();
                if (raw.isEmpty()) {
                    continue;
                }

                FrontmatterParser.FrontmatterParseResult parsed =
                        FrontmatterParser.parseYamlFrontmatter(raw);
                if (parsed.isHadFrontmatter() && parsed.getParseError() != null) {
                    contents.add(ruleFilePathRelative + "\n" + raw);
                    continue;
                }

                RuleConditionals.ConditionalResult conditionalResult =
                        RuleConditionals.evaluateRuleConditionals(
                                parsed.getData(), evaluationContext);
                if (!conditionalResult.isPassed()) {
                    continue;
                }

                if (parsed.isHadFrontmatter()
                        && conditionalResult.getMatchedConditions() != null
                        && !conditionalResult.getMatchedConditions().isEmpty()) {
                    activatedConditionalRules.add(
                            new ActivatedConditionalRule(
                                    prefix.getValue()
                                            + ":"
                                            + ruleFilePathRelative.toString().replace('\\', '/'),
                                    new LinkedHashMap<>(conditionalResult.getMatchedConditions())));
                }

                String body = parsed.getBody() != null ? parsed.getBody().trim() : "";
                if (!body.isEmpty()) {
                    contents.add(ruleFilePathRelative + "\n" + body);
                }
            } catch (IOException e) {
                log.error("Failed to read rule file: {}", filePath, e);
            }
        }

        return new RuleLoadResult(String.join("\n\n", contents), activatedConditionalRules);
    }

    public static RuleLoadResult getRemoteRulesTotalContentWithMetadata(
            List<GlobalInstructionsFile> remoteRules,
            Map<String, Boolean> remoteToggles,
            RuleConditionals.RuleEvaluationContext evaluationContext) {
        List<ActivatedConditionalRule> activatedConditionalRules = new ArrayList<>();
        StringBuilder combinedContent = new StringBuilder();
        RuleConditionals.RuleEvaluationContext effectiveContext =
                evaluationContext != null
                        ? evaluationContext
                        : new RuleConditionals.RuleEvaluationContext(Collections.emptyList());

        if (remoteRules == null) {
            return new RuleLoadResult("", activatedConditionalRules);
        }

        for (GlobalInstructionsFile rule : remoteRules) {
            if (rule == null || rule.getName() == null) {
                continue;
            }
            boolean isEnabled =
                    rule.isAlwaysEnabled()
                            || remoteToggles == null
                            || !Boolean.FALSE.equals(remoteToggles.get(rule.getName()));
            if (!isEnabled) {
                continue;
            }

            String raw = rule.getContents() != null ? rule.getContents().trim() : "";
            if (raw.isEmpty()) {
                continue;
            }

            FrontmatterParser.FrontmatterParseResult parsed =
                    FrontmatterParser.parseYamlFrontmatter(raw);
            if (parsed.isHadFrontmatter() && parsed.getParseError() != null) {
                appendContent(combinedContent, rule.getName(), raw);
                continue;
            }

            RuleConditionals.ConditionalResult conditionalResult =
                    RuleConditionals.evaluateRuleConditionals(parsed.getData(), effectiveContext);
            if (!conditionalResult.isPassed()) {
                continue;
            }

            if (parsed.isHadFrontmatter()
                    && conditionalResult.getMatchedConditions() != null
                    && !conditionalResult.getMatchedConditions().isEmpty()) {
                activatedConditionalRules.add(
                        new ActivatedConditionalRule(
                                RuleSourcePrefix.REMOTE.getValue() + ":" + rule.getName(),
                                new LinkedHashMap<>(conditionalResult.getMatchedConditions())));
            }

            appendContent(combinedContent, rule.getName(), parsed.getBody().trim());
        }

        return new RuleLoadResult(combinedContent.toString(), activatedConditionalRules);
    }

    private static void appendContent(StringBuilder builder, String name, String body) {
        if (body == null || body.isBlank()) {
            return;
        }
        if (builder.length() > 0) {
            builder.append("\n\n");
        }
        builder.append(name).append("\n").append(body);
    }

    public static class CreateRuleFileResult {
        private final String filePath;
        private final boolean fileExists;

        public CreateRuleFileResult(String filePath, boolean fileExists) {
            this.filePath = filePath;
            this.fileExists = fileExists;
        }

        public String getFilePath() {
            return filePath;
        }

        public boolean isFileExists() {
            return fileExists;
        }
    }

    public static boolean ensureLocalClineDirExists(
            String clinerulePath, String defaultRuleFilename) {
        try {
            Path path = Paths.get(clinerulePath);
            boolean exists = Files.exists(path);

            if (exists && !Files.isDirectory(path)) {
                String content = Files.readString(path);
                Path tempPath = Paths.get(clinerulePath + ".bak");
                Files.move(path, tempPath);

                try {
                    Files.createDirectories(path);
                    Files.writeString(path.resolve(defaultRuleFilename), content);
                    Files.deleteIfExists(tempPath);
                    return false;
                } catch (Exception conversionError) {
                    try {
                        if (Files.exists(path)) {
                            deleteDirectory(path);
                        }
                        Files.move(tempPath, path);
                    } catch (Exception restoreError) {
                        log.debug("Failed to restore backup for {}", clinerulePath, restoreError);
                    }
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return true;
        }
    }

    private static void deleteDirectory(Path directory) throws IOException {
        if (Files.exists(directory)) {
            try (Stream<Path> paths = Files.walk(directory)) {
                paths.sorted(Comparator.reverseOrder())
                        .forEach(
                                path -> {
                                    try {
                                        Files.delete(path);
                                    } catch (IOException ignored) {
                                    }
                                });
            }
        }
    }

    public static CreateRuleFileResult createRuleFile(
            boolean isGlobal,
            String filename,
            String cwd,
            String type,
            String globalClineWorkflowFilePath,
            String globalClineRulesFilePath) {
        try {
            String filePath;
            if (isGlobal) {
                if ("workflow".equals(type)) {
                    filePath = Paths.get(globalClineWorkflowFilePath, filename).toString();
                } else {
                    filePath = Paths.get(globalClineRulesFilePath, filename).toString();
                }
            } else {
                String localClineRulesFilePath =
                        Paths.get(cwd, GlobalFileNames.CLINE_RULES).toString();

                boolean hasError =
                        ensureLocalClineDirExists(localClineRulesFilePath, "default-rules.md");
                if (hasError) {
                    return new CreateRuleFileResult(null, false);
                }

                Files.createDirectories(Paths.get(localClineRulesFilePath));

                if ("workflow".equals(type)) {
                    String localWorkflowsFilePath =
                            Paths.get(cwd, GlobalFileNames.WORKFLOWS).toString();

                    hasError =
                            ensureLocalClineDirExists(
                                    localWorkflowsFilePath, "default-workflows.md");
                    if (hasError) {
                        return new CreateRuleFileResult(null, false);
                    }

                    Files.createDirectories(Paths.get(localWorkflowsFilePath));
                    filePath = Paths.get(localWorkflowsFilePath, filename).toString();
                } else {
                    filePath = Paths.get(localClineRulesFilePath, filename).toString();
                }
            }

            Path filePathObj = Paths.get(filePath);
            boolean fileExists = Files.exists(filePathObj);

            if (fileExists) {
                return new CreateRuleFileResult(filePath, true);
            }

            Files.writeString(filePathObj, "");
            return new CreateRuleFileResult(filePath, false);
        } catch (Exception e) {
            log.error("Failed to create rule file: {}", e.getMessage(), e);
            return new CreateRuleFileResult(null, false);
        }
    }

    public static class DeleteRuleFileResult {
        private final boolean success;
        private final String message;

        public DeleteRuleFileResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }

    public static DeleteRuleFileResult deleteRuleFile(
            String rulePath, boolean isGlobal, String type) {
        try {
            Path path = Paths.get(rulePath);
            if (!Files.exists(path)) {
                return new DeleteRuleFileResult(false, "File does not exist: " + rulePath);
            }

            Files.delete(path);
            String fileName = path.getFileName().toString();
            return new DeleteRuleFileResult(true, "File \"" + fileName + "\" deleted successfully");
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            log.error("Error deleting file: {}", errorMessage, e);
            return new DeleteRuleFileResult(false, "Failed to delete file.");
        }
    }
}
