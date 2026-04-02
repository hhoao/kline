package com.hhoa.kline.core.core.context.instructions.userinstructions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hhoa.kline.core.core.storage.GlobalFileNames;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ClineRulesTest {

    @TempDir Path tempDir;

    @Test
    void localDirectoryRulesApplyWhenPathsConditionMatches() throws Exception {
        Path rulesDir = tempDir.resolve(".clinerules");
        Files.createDirectories(rulesDir);
        Path ruleFile = rulesDir.resolve("java-rule.md");
        Files.writeString(
                ruleFile,
                """
                ---
                paths:
                  - src/**
                ---
                Prefer Java implementations.
                """);

        Map<String, Boolean> toggles = new HashMap<>();
        toggles.put(ruleFile.toString(), true);

        RuleHelpers.RuleLoadResultWithInstructions result =
                ClineRules.getLocalClineRules(
                        tempDir.toString(),
                        toggles,
                        new RuleConditionals.RuleEvaluationContext(List.of("src/main/App.java")));

        assertNotNull(result.getInstructions());
        assertTrue(result.getInstructions().contains("Prefer Java implementations."));
        assertEquals(1, result.getActivatedConditionalRules().size());
        assertEquals(
                "workspace:java-rule.md",
                result.getActivatedConditionalRules().get(0).getName());
    }

    @Test
    void localDirectoryRulesDoNotApplyWhenPathsConditionDoesNotMatch() throws Exception {
        Path rulesDir = tempDir.resolve(".clinerules");
        Files.createDirectories(rulesDir);
        Path ruleFile = rulesDir.resolve("java-rule.md");
        Files.writeString(
                ruleFile,
                """
                ---
                paths:
                  - src/**
                ---
                Prefer Java implementations.
                """);

        Map<String, Boolean> toggles = new HashMap<>();
        toggles.put(ruleFile.toString(), true);

        RuleHelpers.RuleLoadResultWithInstructions result =
                ClineRules.getLocalClineRules(
                        tempDir.toString(),
                        toggles,
                        new RuleConditionals.RuleEvaluationContext(List.of("docs/readme.md")));

        assertNull(result.getInstructions());
        assertTrue(result.getActivatedConditionalRules().isEmpty());
    }

    @Test
    void singleFileRulesFailOpenWhenFrontmatterIsInvalid() throws Exception {
        Path rulesFile = tempDir.resolve(GlobalFileNames.CLINE_RULES);
        Files.writeString(
                rulesFile,
                """
                ---
                paths: [src/**
                ---
                Keep the raw content.
                """);

        Map<String, Boolean> toggles = new HashMap<>();
        toggles.put(rulesFile.toString(), true);

        RuleHelpers.RuleLoadResultWithInstructions result =
                ClineRules.getLocalClineRules(
                        tempDir.toString(),
                        toggles,
                        new RuleConditionals.RuleEvaluationContext(List.of("docs/readme.md")));

        assertNotNull(result.getInstructions());
        assertTrue(result.getInstructions().contains("paths: [src/**"));
        assertTrue(result.getActivatedConditionalRules().isEmpty());
    }
}
