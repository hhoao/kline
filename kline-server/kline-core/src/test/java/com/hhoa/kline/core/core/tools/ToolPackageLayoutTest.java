package com.hhoa.kline.core.core.tools;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class ToolPackageLayoutTest {
    @Test
    void toolImplementationFilesLiveUnderCentralToolsPackage() {
        Path sourceRoot = Path.of("src/main/java").toAbsolutePath();

        assertFalse(
                Files.exists(
                        sourceRoot.resolve("com/hhoa/kline/core/core/prompts/systemprompt/tools")),
                "Tool specs should live under core/core/tools/specs");
        assertFalse(
                Files.exists(
                        sourceRoot.resolve(
                                "com/hhoa/kline/core/core/prompts/systemprompt/ClineToolSpec.java")),
                "Tool contract should live under core/core/tools");
        assertFalse(
                Files.exists(sourceRoot.resolve("com/hhoa/kline/core/core/task/tools/handlers")),
                "Tool handlers should live under core/core/tools/handlers");
        assertTrue(
                Files.exists(sourceRoot.resolve("com/hhoa/kline/core/core/tools")),
                "Central tools package should exist");
    }

    @Test
    void toolSpecsUseTypedProviderContract() {
        List<Class<?>> toolSpecs =
                List.of(
                        com.hhoa.kline.core.core.tools.specs.AccessMcpResourceTool.class,
                        com.hhoa.kline.core.core.tools.specs.ActModeRespondTool.class,
                        com.hhoa.kline.core.core.tools.specs.AgentTool.class,
                        com.hhoa.kline.core.core.tools.specs.ApplyPatchTool.class,
                        com.hhoa.kline.core.core.tools.specs.AskFollowupQuestionTool.class,
                        com.hhoa.kline.core.core.tools.specs.AttemptCompletionTool.class,
                        com.hhoa.kline.core.core.tools.specs.BrowserActionTool.class,
                        com.hhoa.kline.core.core.tools.specs.ExecuteCommandToolSpec.class,
                        com.hhoa.kline.core.core.tools.specs.TodoWriteTool.class,
                        com.hhoa.kline.core.core.tools.specs.GenerateExplanationTool.class,
                        com.hhoa.kline.core.core.tools.specs.ListCodeDefinitionNamesTool.class,
                        com.hhoa.kline.core.core.tools.specs.ListFilesTool.class,
                        com.hhoa.kline.core.core.tools.specs.LoadMcpDocumentationTool.class,
                        com.hhoa.kline.core.core.tools.specs.NewTaskTool.class,
                        com.hhoa.kline.core.core.tools.specs.PlanModeRespondTool.class,
                        com.hhoa.kline.core.core.tools.specs.ReadFileTool.class,
                        com.hhoa.kline.core.core.tools.specs.ReplaceInFileTool.class,
                        com.hhoa.kline.core.core.tools.specs.SearchFilesTool.class,
                        com.hhoa.kline.core.core.tools.specs.UseMcpToolTool.class,
                        com.hhoa.kline.core.core.tools.specs.UseSkillTool.class,
                        com.hhoa.kline.core.core.tools.specs.WebFetchTool.class,
                        com.hhoa.kline.core.core.tools.specs.WebSearchTool.class,
                        com.hhoa.kline.core.core.tools.specs.WriteToFileTool.class);

        for (Class<?> toolSpec : toolSpecs) {
            assertTrue(
                    Modifier.isFinal(toolSpec.getModifiers()),
                    () -> toolSpec.getSimpleName() + " should be final");
            assertTrue(
                    Arrays.asList(toolSpec.getInterfaces()).contains(ToolSpecProvider.class),
                    () -> toolSpec.getSimpleName() + " should implement ToolSpecProvider");
            for (Method method : toolSpec.getDeclaredMethods()) {
                assertFalse(
                        ToolSpec.class.equals(method.getReturnType()),
                        () ->
                                toolSpec.getSimpleName()
                                        + " should declare metadata, not create ToolSpec via "
                                        + method.getName());
            }
        }
    }

    @Test
    void toolSpecsDoNotBuildToolSpecsDirectly() throws IOException {
        Path specRoot =
                Path.of("src/main/java/com/hhoa/kline/core/core/tools/specs").toAbsolutePath();

        try (var files = Files.list(specRoot)) {
            files.filter(path -> path.getFileName().toString().endsWith(".java"))
                    .forEach(
                            path -> {
                                try {
                                    String source = Files.readString(path);
                                    assertFalse(
                                            source.contains("ToolSpec.builder()"),
                                            () ->
                                                    path.getFileName()
                                                            + " should not build ToolSpec directly");
                                } catch (IOException e) {
                                    throw new AssertionError(e);
                                }
                            });
        }
    }
}
