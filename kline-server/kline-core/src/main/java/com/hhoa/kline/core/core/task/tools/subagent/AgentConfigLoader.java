package com.hhoa.kline.core.core.task.tools.subagent;

import com.hhoa.kline.core.core.context.instructions.userinstructions.FrontmatterParser;
import com.hhoa.kline.core.core.context.instructions.userinstructions.FrontmatterParser.FrontmatterParseResult;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

/**
 * 从 {@code ~/Documents/Cline/Agents} 加载子代理 YAML 配置，与 Cline {@code AgentConfigLoader.ts}
 * 行为对齐：动态工具名映射、热重载。
 */
@Slf4j
public final class AgentConfigLoader {

    public static final String AGENTS_CONFIG_DIRECTORY_NAME = "Agents";

    private static final Pattern YAML_FILE =
            Pattern.compile(".*\\.(yaml|yml)$", Pattern.CASE_INSENSITIVE);

    private static volatile AgentConfigLoader instance;

    private final Path homeDir;
    private final Path directoryPath;
    private final Map<String, AgentBaseConfig> cachedConfigs = new LinkedHashMap<>();
    private final Map<String, String> cachedAgentToolNames = new ConcurrentHashMap<>();
    private final Map<String, String> cachedToolNameToAgentName = new ConcurrentHashMap<>();

    private WatchService watchService;
    private Thread watchThread;
    private volatile boolean disposed;

    private AgentConfigLoader(Path homeDir) {
        this.homeDir = homeDir;
        this.directoryPath = getAgentsConfigPath(homeDir);
        try {
            Files.createDirectories(directoryPath);
        } catch (IOException e) {
            log.debug("Could not create agents config directory: {}", directoryPath, e);
        }
        load();
        startWatch();
    }

    public static AgentConfigLoader getInstance() {
        if (instance == null) {
            synchronized (AgentConfigLoader.class) {
                if (instance == null) {
                    instance = new AgentConfigLoader(Path.of(System.getProperty("user.home")));
                }
            }
        }
        return instance;
    }

    /** 仅测试用：重置单例。 */
    public static synchronized void resetInstanceForTests() {
        if (instance != null) {
            instance.dispose();
            instance = null;
        }
    }

    public static Path getAgentsConfigPath(Path homeDir) {
        return homeDir.resolve("Documents").resolve("Cline").resolve(AGENTS_CONFIG_DIRECTORY_NAME);
    }

    public Path getConfigPath() {
        return directoryPath;
    }

    public AgentBaseConfig getCachedConfig(String subagentName) {
        if (subagentName == null || subagentName.trim().isEmpty()) {
            return null;
        }
        return cachedConfigs.get(normalizeAgentName(subagentName));
    }

    public Map<String, AgentBaseConfig> getAllCachedConfigs() {
        return Map.copyOf(cachedConfigs);
    }

    public List<AgentConfigWithToolName> getAllCachedConfigsWithToolNames() {
        List<AgentConfigWithToolName> result = new ArrayList<>();
        for (Map.Entry<String, AgentBaseConfig> e : cachedConfigs.entrySet()) {
            String toolName = cachedAgentToolNames.get(e.getKey());
            if (toolName != null) {
                result.add(new AgentConfigWithToolName(toolName, e.getValue()));
            }
        }
        return result;
    }

    public String resolveSubagentNameForTool(String toolName) {
        if (toolName == null || toolName.trim().isEmpty()) {
            return null;
        }
        String normalized = cachedToolNameToAgentName.get(toolName.trim());
        if (normalized == null) {
            return null;
        }
        AgentBaseConfig cfg = cachedConfigs.get(normalized);
        return cfg != null ? cfg.name() : null;
    }

    public boolean isDynamicSubagentTool(String toolName) {
        if (toolName == null || toolName.trim().isEmpty()) {
            return false;
        }
        return cachedToolNameToAgentName.containsKey(toolName.trim());
    }

    /** 动态工具名 → 配置 map 中的归一化 agent 键（小写）。 */
    public String getNormalizedAgentNameForTool(String toolName) {
        if (toolName == null || toolName.trim().isEmpty()) {
            return null;
        }
        return cachedToolNameToAgentName.get(toolName.trim());
    }

    public synchronized void load() {
        try {
            Map<String, AgentBaseConfig> fromDisk = readAgentConfigsFromDisk(homeDir);
            cachedConfigs.clear();
            cachedConfigs.putAll(fromDisk);
            rebuildDynamicToolMappings();
            log.debug(
                    "[AgentConfigLoader] Loaded {} agent config(s) from disk.",
                    cachedConfigs.size());
        } catch (IOException e) {
            log.error("[AgentConfigLoader] Failed to load agent configs", e);
        }
    }

    private void startWatch() {
        try {
            if (!Files.isDirectory(directoryPath)) {
                return;
            }
            watchService = FileSystems.getDefault().newWatchService();
            directoryPath.register(
                    watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE,
                    StandardWatchEventKinds.ENTRY_MODIFY);
            watchThread = new Thread(this::watchLoop, "agent-config-loader-watch");
            watchThread.setDaemon(true);
            watchThread.start();
        } catch (IOException e) {
            log.debug("[AgentConfigLoader] Watch not started", e);
        }
    }

    private void watchLoop() {
        while (!disposed && watchService != null) {
            try {
                WatchKey key = watchService.take();
                if (disposed) {
                    break;
                }
                for (WatchEvent<?> event : key.pollEvents()) {
                    if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }
                    Object ctx = event.context();
                    if (ctx instanceof Path p) {
                        String name = p.getFileName().toString();
                        if (YAML_FILE.matcher(name).matches()) {
                            load();
                            break;
                        }
                    }
                }
                key.reset();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.debug("[AgentConfigLoader] watch error", e);
            }
        }
    }

    public void dispose() {
        disposed = true;
        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException ignored) {
            }
            watchService = null;
        }
    }

    private static String normalizeAgentName(String name) {
        return name.trim().toLowerCase();
    }

    private static boolean isYamlFile(String fileName) {
        return YAML_FILE.matcher(fileName).matches();
    }

    private Map<String, AgentBaseConfig> readAgentConfigsFromDisk(Path homeDir) throws IOException {
        Path agentsDir = getAgentsConfigPath(homeDir);
        Map<String, AgentBaseConfig> configs = new LinkedHashMap<>();
        if (!Files.isDirectory(agentsDir)) {
            return configs;
        }
        List<Path> yamlFiles = new ArrayList<>();
        try (var stream = Files.list(agentsDir)) {
            stream.filter(Files::isRegularFile)
                    .filter(p -> isYamlFile(p.getFileName().toString()))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .forEach(yamlFiles::add);
        }
        log.debug("[AgentConfigLoader] Found {} YAML file(s).", yamlFiles.size());
        for (Path filePath : yamlFiles) {
            try {
                String content = Files.readString(filePath);
                FrontmatterParseResult parsed = FrontmatterParser.parseYamlFrontmatter(content);
                if (!parsed.isHadFrontmatter()) {
                    log.warn(
                            "[AgentConfigLoader] Skipping '{}': missing YAML frontmatter.",
                            filePath.getFileName());
                    continue;
                }
                if (parsed.getParseError() != null) {
                    log.error(
                            "[AgentConfigLoader] Failed to parse '{}': {}",
                            filePath.getFileName(),
                            parsed.getParseError());
                    continue;
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> data =
                        parsed.getData() != null
                                ? (Map<String, Object>) (Map<?, ?>) parsed.getData()
                                : Map.of();
                AgentBaseConfig cfg = AgentBaseConfig.parseFromFrontmatter(data, parsed.getBody());
                configs.put(normalizeAgentName(cfg.name()), cfg);
                log.debug("[AgentConfigLoader] Loaded agent config '{}'", filePath.getFileName());
            } catch (Exception e) {
                log.error(
                        "[AgentConfigLoader] Failed to parse agent config '{}'",
                        filePath.getFileName(),
                        e);
            }
        }
        return configs;
    }

    private void rebuildDynamicToolMappings() {
        List<Map.Entry<String, AgentBaseConfig>> sorted = new ArrayList<>(cachedConfigs.entrySet());
        sorted.sort(Comparator.comparing(Map.Entry::getKey));

        Set<String> usedToolNames = ConcurrentHashMap.newKeySet();
        Map<String, String> agentToolNames = new LinkedHashMap<>();
        Map<String, String> toolNameToAgentName = new LinkedHashMap<>();

        for (Map.Entry<String, AgentBaseConfig> e : sorted) {
            String normalizedName = e.getKey();
            AgentBaseConfig config = e.getValue();
            String baseName = SubagentToolName.buildSubagentToolName(config.name());
            String candidate = baseName;
            int suffix = 2;
            while (usedToolNames.contains(candidate)) {
                String suffixText = "_" + suffix++;
                int maxBaseLength = Math.max(1, 64 - suffixText.length());
                candidate =
                        baseName.substring(0, Math.min(baseName.length(), maxBaseLength))
                                + suffixText;
            }
            usedToolNames.add(candidate);
            agentToolNames.put(normalizedName, candidate);
            toolNameToAgentName.put(candidate, normalizedName);
        }

        cachedAgentToolNames.clear();
        cachedAgentToolNames.putAll(agentToolNames);
        cachedToolNameToAgentName.clear();
        cachedToolNameToAgentName.putAll(toolNameToAgentName);
    }

    public record AgentConfigWithToolName(String toolName, AgentBaseConfig config) {}
}
