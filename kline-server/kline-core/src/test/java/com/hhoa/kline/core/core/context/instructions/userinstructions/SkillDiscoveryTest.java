package com.hhoa.kline.core.core.context.instructions.userinstructions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SkillDiscoveryTest {

    @TempDir Path tempDir;

    @Test
    void globalSkillOverridesProjectSkillWithSameName() throws Exception {
        Path projectSkillDir = tempDir.resolve(".clinerules/skills/my-skill");
        Files.createDirectories(projectSkillDir);
        Files.writeString(
                projectSkillDir.resolve("SKILL.md"),
                """
                ---
                name: my-skill
                description: project description
                ---
                project body
                """);

        Path globalSkillsDir = tempDir.resolve("global-skills");
        Path globalSkillDir = globalSkillsDir.resolve("my-skill");
        Files.createDirectories(globalSkillDir);
        Files.writeString(
                globalSkillDir.resolve("SKILL.md"),
                """
                ---
                name: my-skill
                description: global description
                ---
                global body
                """);

        List<SkillMetadata> discovered =
                SkillDiscovery.discoverSkills(tempDir.toString(), globalSkillsDir.toString());
        List<SkillMetadata> available = SkillDiscovery.getAvailableSkills(discovered);

        assertEquals(1, available.size());
        assertEquals("global", available.get(0).getSource());
        assertEquals("global description", available.get(0).getDescription());
    }

    @Test
    void getSkillContentReturnsBodyWithoutFrontmatter() throws Exception {
        Path globalSkillsDir = tempDir.resolve("global-skills");
        Path skillDir = globalSkillsDir.resolve("formatter");
        Files.createDirectories(skillDir);
        Files.writeString(
                skillDir.resolve("SKILL.md"),
                """
                ---
                name: formatter
                description: Helps formatting
                ---
                Use formatter rules.
                """);

        List<SkillMetadata> available =
                SkillDiscovery.getAvailableSkills(
                        SkillDiscovery.discoverSkills(tempDir.toString(), globalSkillsDir.toString()));

        SkillContent content = SkillDiscovery.getSkillContent("formatter", available);
        assertNotNull(content);
        assertEquals("Use formatter rules.", content.getInstructions());
    }

    @Test
    void invalidSkillMetadataIsIgnored() throws Exception {
        Path globalSkillsDir = tempDir.resolve("global-skills");
        Path skillDir = globalSkillsDir.resolve("broken");
        Files.createDirectories(skillDir);
        Files.writeString(
                skillDir.resolve("SKILL.md"),
                """
                ---
                name: wrong-name
                description: bad
                ---
                ignored
                """);

        List<SkillMetadata> available =
                SkillDiscovery.getAvailableSkills(
                        SkillDiscovery.discoverSkills(tempDir.toString(), globalSkillsDir.toString()));

        assertEquals(0, available.size());
        assertNull(SkillDiscovery.getSkillContent("broken", available));
    }
}
