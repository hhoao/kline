package com.hhoa.kline.core.core.tools;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
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
}
