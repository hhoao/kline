package com.hhoa.kline.core.core.context.instructions.userinstructions;

import com.hhoa.kline.core.core.storage.GlobalFileNames;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RuleHelpers {
    /**
     * 递归遍历目录并查找所有文件，包括检查可选的白名单文件扩展名
     *
     * @param directoryPath 目录路径
     * @param allowedFileExtension 允许的文件扩展名（空字符串表示所有文件）
     * @param excludedPaths 排除的路径列表
     * @return 文件路径列表
     */
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
            log.error("Error reading directory " + directoryPath + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 读取目录中的所有文件（递归）
     *
     * @param directoryPath 目录路径
     * @param excludedPaths 排除的路径列表
     * @return 文件路径列表（相对于目录路径）
     */
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

    /**
     * 获取最新的切换状态
     *
     * @param rulesDirectoryPath 规则目录路径
     * @param currentToggles 当前切换状态
     * @param allowedFileExtension 允许的文件扩展名
     * @param excludedPaths 排除的路径列表
     * @return 更新后的切换状态
     */
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

                        boolean pathHasToggle = updatedToggles.containsKey(ruleFilePath);
                        if (!pathHasToggle) {
                            updatedToggles.put(ruleFilePath, true);
                        }
                    }

                    // 清理不存在文件的切换状态
                    List<String> keysToRemove = new ArrayList<>();
                    for (String togglePath : updatedToggles.keySet()) {
                        if (!existingRulePaths.contains(togglePath)) {
                            keysToRemove.add(togglePath);
                        }
                    }
                    keysToRemove.forEach(updatedToggles::remove);
                } else {
                    boolean pathHasToggle = updatedToggles.containsKey(rulesDirectoryPath);
                    if (!pathHasToggle) {
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
                // 清除所有切换状态，因为路径不存在
                updatedToggles.clear();
            }
        } catch (Exception e) {
            log.error("Failed to synchronize rule toggles for path: " + rulesDirectoryPath);
            e.printStackTrace();
        }

        return updatedToggles;
    }

    public static Map<String, Boolean> synchronizeRuleToggles(
            String rulesDirectoryPath, Map<String, Boolean> currentToggles) {
        return synchronizeRuleToggles(rulesDirectoryPath, currentToggles, "", null);
    }

    /**
     * 某些项目规则有多个允许存储规则的位置
     *
     * @param toggles1 第一个切换映射
     * @param toggles2 第二个切换映射
     * @return 合并后的切换映射
     */
    public static Map<String, Boolean> combineRuleToggles(
            Map<String, Boolean> toggles1, Map<String, Boolean> toggles2) {
        Map<String, Boolean> combined = new HashMap<>(toggles1);
        combined.putAll(toggles2);
        return combined;
    }

    /**
     * 读取规则文件的内容
     *
     * @param rulesFilePaths 规则文件路径列表
     * @param basePath 基础路径
     * @param toggles 切换状态
     * @return 规则文件的总内容
     */
    public static String getRuleFilesTotalContent(
            List<String> rulesFilePaths, String basePath, Map<String, Boolean> toggles) {

        List<String> contents = new ArrayList<>();

        for (String filePath : rulesFilePaths) {
            try {
                Path ruleFilePath = Paths.get(basePath, filePath);
                String ruleFilePathStr = ruleFilePath.toString();
                Path ruleFilePathRelative = Paths.get(basePath).relativize(ruleFilePath);

                if (toggles.containsKey(ruleFilePathStr)
                        && Boolean.FALSE.equals(toggles.get(ruleFilePathStr))) {
                    continue;
                }

                String content = Files.readString(ruleFilePath).trim();
                if (!content.isEmpty()) {
                    contents.add(ruleFilePathRelative + "\n" + content);
                }
            } catch (IOException e) {
                log.error("Failed to read rule file: " + filePath);
                e.printStackTrace();
            }
        }

        return String.join("\n\n", contents);
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

    /**
     * 处理将任何目录转换为文件（专门用于 .clinerules 和 .clinerules/workflows） 旧的 .clinerules 文件或
     * .clinerules/workflows 文件将被重命名为默认文件名 如果目录已存在或不存在，则不执行任何操作
     *
     * @param clinerulePath Cline 规则路径
     * @param defaultRuleFilename 默认规则文件名
     * @return 是否有任何未捕获的错误
     */
    public static boolean ensureLocalClineDirExists(
            String clinerulePath, String defaultRuleFilename) {
        try {
            Path path = Paths.get(clinerulePath);
            boolean exists = Files.exists(path);

            if (exists && !Files.isDirectory(path)) {
                // 将 .clinerules 文件转换为目录，并将规则文件重命名为 {defaultRuleFilename}
                String content = Files.readString(path);
                Path tempPath = Paths.get(clinerulePath + ".bak");
                Files.move(path, tempPath);

                try {
                    Files.createDirectories(path);
                    Files.writeString(path.resolve(defaultRuleFilename), content);
                    Files.deleteIfExists(tempPath);

                    return false;
                } catch (Exception conversionError) {
                    // 转换失败时尝试恢复备份
                    try {
                        if (Files.exists(path)) {
                            deleteDirectory(path);
                        }
                        Files.move(tempPath, path);
                    } catch (Exception restoreError) {
                        // 忽略恢复错误
                    }
                    return true;
                }
            }
            // 存在且是目录或不存在，这两种情况我们都不需要在这里处理
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
                                    } catch (IOException e) {
                                    }
                                });
            }
        }
    }

    /**
     * 创建规则文件或工作流文件
     *
     * @param isGlobal 是否为全局规则
     * @param filename 文件名
     * @param cwd 当前工作目录
     * @param type 类型（"workflow" 或其他）
     * @return 创建结果
     */
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
            log.error("Failed to create rule file: " + e.getMessage());
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

    /**
     * 删除规则文件或工作流文件 注意：此方法需要 TaskManager 来更新切换状态，实际使用时需要传入 StateManager
     *
     * @param rulePath 规则路径
     * @param isGlobal 是否为全局规则
     * @param type 类型
     * @return 删除结果
     */
    public static DeleteRuleFileResult deleteRuleFile(
            String rulePath, boolean isGlobal, String type) {
        try {
            Path path = Paths.get(rulePath);
            if (!Files.exists(path)) {
                return new DeleteRuleFileResult(false, "File does not exist: " + rulePath);
            }

            Files.delete(path);

            String fileName = path.getFileName().toString();

            // 注意：更新切换状态需要 StateManager 集成
            // 实际使用时需要调用 StateManager 的方法来更新切换状态
            // 这里仅返回成功消息

            return new DeleteRuleFileResult(true, "File \"" + fileName + "\" deleted successfully");
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            log.error("Error deleting file: " + errorMessage);
            return new DeleteRuleFileResult(false, "Failed to delete file.");
        }
    }
}
