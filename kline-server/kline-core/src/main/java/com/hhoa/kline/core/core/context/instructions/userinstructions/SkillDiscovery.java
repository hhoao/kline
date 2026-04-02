package com.hhoa.kline.core.core.context.instructions.userinstructions;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import lombok.extern.slf4j.Slf4j;

/**
 * Skill 发现与加载。
 *
 * <p>扫描全局和项目目录中的 SKILL.md 文件，验证 frontmatter，发现可用技能。
 *
 * <ul>
 *   <li>全局 skills：~/.cline/skills/
 *   <li>项目 skills：{workspaceRoot}/.clinerules/skills/
 *   <li>全局 skills 优先于同名项目 skills
 * </ul>
 */
@Slf4j
public class SkillDiscovery {

    /**
     * 发现所有 skills
     *
     * @param workspaceRoot 工作区根目录
     * @param globalSkillsDir 全局 skills 目录（如 ~/.cline/skills）
     * @return 发现的 skill 列表
     */
    public static List<SkillMetadata> discoverSkills(String workspaceRoot, String globalSkillsDir) {
        List<SkillMetadata> skills = new ArrayList<>();

        // 项目 skills
        if (workspaceRoot != null) {
            Path projectSkillsDir = Paths.get(workspaceRoot, ".clinerules", "skills");
            skills.addAll(scanSkillsDirectory(projectSkillsDir, "project"));
        }

        // 全局 skills
        if (globalSkillsDir != null) {
            skills.addAll(scanSkillsDirectory(Paths.get(globalSkillsDir), "global"));
        }

        return skills;
    }

    /**
     * 获取可用 skills（处理全局/项目优先级）
     *
     * <p>全局 skills 优先于同名项目 skills。
     */
    public static List<SkillMetadata> getAvailableSkills(List<SkillMetadata> skills) {
        Set<String> seen = new LinkedHashSet<>();
        List<SkillMetadata> result = new ArrayList<>();

        // 从后向前迭代：全局 skills 最后添加，优先
        for (int i = skills.size() - 1; i >= 0; i--) {
            SkillMetadata skill = skills.get(i);
            if (!seen.contains(skill.getName())) {
                seen.add(skill.getName());
                result.add(0, skill);
            }
        }

        return result;
    }

    /**
     * 获取完整 skill 内容（含指令）
     *
     * @param skillName skill 名称
     * @param availableSkills 可用 skills 列表
     * @return skill 内容，未找到返回 null
     */
    public static SkillContent getSkillContent(
            String skillName, List<SkillMetadata> availableSkills) {
        SkillMetadata skill =
                availableSkills.stream()
                        .filter(s -> s.getName().equals(skillName))
                        .findFirst()
                        .orElse(null);
        if (skill == null) return null;

        try {
            String fileContent = Files.readString(Path.of(skill.getPath()), StandardCharsets.UTF_8);
            FrontmatterParser.FrontmatterParseResult parsed =
                    FrontmatterParser.parseYamlFrontmatter(fileContent);
            return new SkillContent(skill, parsed.getBody().trim());
        } catch (IOException e) {
            log.warn("Failed to load skill content for {}", skillName, e);
            return null;
        }
    }

    /** 扫描目录中的 skill 子目录 */
    private static List<SkillMetadata> scanSkillsDirectory(Path dirPath, String source) {
        List<SkillMetadata> skills = new ArrayList<>();
        if (!Files.isDirectory(dirPath)) {
            return skills;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath)) {
            for (Path entry : stream) {
                if (!Files.isDirectory(entry)) continue;

                String entryName = entry.getFileName().toString();
                SkillMetadata skill = loadSkillMetadata(entry, source, entryName);
                if (skill != null) {
                    skills.add(skill);
                }
            }
        } catch (IOException e) {
            log.warn("Error scanning skills directory: {}", dirPath, e);
        }

        return skills;
    }

    /** 从 skill 目录加载元数据 */
    private static SkillMetadata loadSkillMetadata(Path skillDir, String source, String skillName) {
        Path skillMdPath = skillDir.resolve("SKILL.md");
        if (!Files.isRegularFile(skillMdPath)) return null;

        try {
            String fileContent = Files.readString(skillMdPath, StandardCharsets.UTF_8);
            FrontmatterParser.FrontmatterParseResult parsed =
                    FrontmatterParser.parseYamlFrontmatter(fileContent);
            Map<String, Object> data = parsed.getData();

            // 验证必需字段
            Object nameObj = data.get("name");
            if (!(nameObj instanceof String) || ((String) nameObj).isEmpty()) {
                log.warn("Skill at {} missing required 'name' field", skillDir);
                return null;
            }
            Object descObj = data.get("description");
            if (!(descObj instanceof String) || ((String) descObj).isEmpty()) {
                log.warn("Skill at {} missing required 'description' field", skillDir);
                return null;
            }

            String name = (String) nameObj;
            // 名称必须匹配目录名
            if (!name.equals(skillName)) {
                log.warn("Skill name \"{}\" doesn't match directory \"{}\"", name, skillName);
                return null;
            }

            return SkillMetadata.builder()
                    .name(skillName)
                    .description((String) descObj)
                    .path(skillMdPath.toString())
                    .source(source)
                    .build();
        } catch (IOException e) {
            log.warn("Failed to load skill at {}", skillDir, e);
            return null;
        }
    }
}
