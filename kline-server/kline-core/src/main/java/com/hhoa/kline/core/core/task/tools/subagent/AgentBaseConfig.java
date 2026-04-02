package com.hhoa.kline.core.core.task.tools.subagent;

import com.hhoa.kline.core.enums.ClineDefaultTool;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 与 Cline {@code AgentConfigLoader} 中 {@code AgentBaseConfig} 对应的代理配置。 */
public record AgentBaseConfig(
        String name,
        String description,
        List<ClineDefaultTool> tools,
        List<String> skills,
        String modelId,
        String systemPrompt) {

    @SuppressWarnings("unchecked")
    static AgentBaseConfig parseFromFrontmatter(Map<String, Object> data, String body) {
        if (data == null) {
            throw new IllegalArgumentException("Missing YAML frontmatter data.");
        }
        Object nameObj = data.get("name");
        Object descObj = data.get("description");
        if (nameObj == null || String.valueOf(nameObj).trim().isEmpty()) {
            throw new IllegalArgumentException("Agent config requires non-empty 'name'.");
        }
        if (descObj == null || String.valueOf(descObj).trim().isEmpty()) {
            throw new IllegalArgumentException("Agent config requires non-empty 'description'.");
        }
        String name = String.valueOf(nameObj).trim();
        String description = String.valueOf(descObj).trim();
        String modelId =
                data.get("modelId") != null ? String.valueOf(data.get("modelId")).trim() : null;
        if (modelId != null && modelId.isEmpty()) {
            modelId = null;
        }

        List<ClineDefaultTool> tools = parseToolsField(data.get("tools"));
        List<String> skills = parseSkillsField(data.get("skills"));

        String systemPrompt = body != null ? body.trim() : "";
        if (systemPrompt.isEmpty()) {
            throw new IllegalArgumentException("Missing system prompt body in agent config file.");
        }

        return new AgentBaseConfig(name, description, tools, skills, modelId, systemPrompt);
    }

    private static List<ClineDefaultTool> parseToolsField(Object toolsField) {
        if (toolsField == null) {
            return List.of();
        }
        List<String> raw;
        if (toolsField instanceof List<?> list) {
            raw = new ArrayList<>();
            for (Object o : list) {
                if (o != null) {
                    raw.add(String.valueOf(o).trim());
                }
            }
        } else {
            String s = String.valueOf(toolsField).trim();
            if (s.isEmpty()) {
                return List.of();
            }
            raw = List.of(s.split(","));
        }
        Set<ClineDefaultTool> set = new LinkedHashSet<>();
        for (String t : raw) {
            String trimmed = t.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            set.add(normalizeToolName(trimmed));
        }
        return List.copyOf(set);
    }

    private static List<String> parseSkillsField(Object skillsField) {
        if (skillsField == null) {
            return null;
        }
        if (skillsField instanceof List<?> list) {
            Set<String> out = new LinkedHashSet<>();
            for (Object o : list) {
                if (o != null) {
                    String n = String.valueOf(o).trim();
                    if (!n.isEmpty()) {
                        out.add(n);
                    }
                }
            }
            return out.isEmpty() ? List.of() : List.copyOf(out);
        }
        String s = String.valueOf(skillsField).trim();
        if (s.isEmpty()) {
            return List.of();
        }
        Set<String> out = new LinkedHashSet<>();
        for (String part : s.split(",")) {
            String n = part.trim();
            if (!n.isEmpty()) {
                out.add(n);
            }
        }
        return List.copyOf(out);
    }

    private static ClineDefaultTool normalizeToolName(String trimmed) {
        for (ClineDefaultTool t : ClineDefaultTool.values()) {
            if (t.getValue().equals(trimmed)) {
                return t;
            }
        }
        throw new IllegalArgumentException(
                "Unknown tool '"
                        + trimmed
                        + "'. Expected a ClineDefaultTool value (for example: read_file, list_files).");
    }
}
