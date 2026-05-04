package com.hhoa.kline.core.core.tools.agent;

import com.hhoa.kline.core.core.context.instructions.userinstructions.FrontmatterParser;
import com.hhoa.kline.core.core.context.instructions.userinstructions.FrontmatterParser.FrontmatterParseResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class AgentCatalog {
    private AgentCatalog() {}

    public static List<AgentDefinition> load(String cwd) {
        Map<String, AgentDefinition> agents = new LinkedHashMap<>();
        for (AgentDefinition builtIn : BuiltInAgents.all()) {
            agents.put(builtIn.agentType(), builtIn);
        }
        for (AgentDefinition custom : loadCustomAgents(cwd)) {
            agents.put(custom.agentType(), custom);
        }
        return List.copyOf(agents.values());
    }

    public static AgentDefinition find(String agentType, String cwd) {
        String effectiveType =
                agentType == null || agentType.isBlank()
                        ? AgentConstants.GENERAL_PURPOSE_AGENT_TYPE
                        : agentType.trim();
        return load(cwd).stream()
                .filter(agent -> agent.agentType().equals(effectiveType))
                .findFirst()
                .orElse(null);
    }

    private static List<AgentDefinition> loadCustomAgents(String cwd) {
        List<AgentDefinition> agents = new ArrayList<>();
        List<Path> roots = new ArrayList<>();
        if (cwd != null && !cwd.isBlank()) {
            roots.add(Path.of(cwd).resolve(".claude").resolve("agents"));
        }
        roots.add(Path.of(System.getProperty("user.home")).resolve(".claude").resolve("agents"));

        for (Path root : roots) {
            agents.addAll(loadAgentsFromDirectory(root));
        }
        return agents;
    }

    private static List<AgentDefinition> loadAgentsFromDirectory(Path directory) {
        if (directory == null || !Files.isDirectory(directory)) {
            return List.of();
        }
        List<AgentDefinition> agents = new ArrayList<>();
        try (var stream = Files.list(directory)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".md"))
                    .sorted()
                    .forEach(path -> parseAgent(path).ifPresent(agents::add));
        } catch (IOException e) {
            log.debug("Failed to load agents from {}", directory, e);
        }
        return agents;
    }

    private static java.util.Optional<AgentDefinition> parseAgent(Path path) {
        try {
            String content = Files.readString(path);
            FrontmatterParseResult parsed = FrontmatterParser.parseYamlFrontmatter(content);
            Map<String, Object> data = parsed.getData() != null ? parsed.getData() : Map.of();
            String name = stringValue(data.get("name"));
            if (name == null || name.isBlank()) {
                name = stripMarkdownExtension(path.getFileName().toString());
            }
            String description = stringValue(data.get("description"));
            if (description == null || description.isBlank()) {
                description = "Custom project agent.";
            }
            String prompt = parsed.getBody() != null ? parsed.getBody().trim() : "";
            if (prompt.isBlank()) {
                return java.util.Optional.empty();
            }
            return java.util.Optional.of(
                    new AgentDefinition(
                            name.trim(),
                            description.trim(),
                            prompt,
                            parseStringList(data.get("tools")),
                            parseStringList(data.get("disallowedTools")),
                            stringValue(data.get("model"))));
        } catch (Exception e) {
            log.debug("Failed to parse agent {}", path, e);
            return java.util.Optional.empty();
        }
    }

    private static List<String> parseStringList(Object value) {
        if (value == null) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        if (value instanceof List<?> list) {
            for (Object item : list) {
                addListValue(result, item);
            }
        } else {
            String text = String.valueOf(value);
            for (String item : text.split(",")) {
                addListValue(result, item);
            }
        }
        return List.copyOf(result);
    }

    private static void addListValue(List<String> result, Object item) {
        if (item == null) {
            return;
        }
        String text = String.valueOf(item).trim();
        if (!text.isBlank()) {
            result.add(text);
        }
    }

    private static String stringValue(Object value) {
        return value != null ? String.valueOf(value).trim() : null;
    }

    private static String stripMarkdownExtension(String filename) {
        return filename.endsWith(".md") ? filename.substring(0, filename.length() - 3) : filename;
    }
}
