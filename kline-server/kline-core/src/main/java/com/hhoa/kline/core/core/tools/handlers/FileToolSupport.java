package com.hhoa.kline.core.core.tools.handlers;

import com.hhoa.kline.core.core.ignore.ClineIgnoreController;
import com.hhoa.kline.core.core.integrations.editor.DiffViewProvider;
import com.hhoa.kline.core.core.services.telemetry.TelemetryService;
import com.hhoa.kline.core.core.tools.types.ToolContext;
import com.hhoa.kline.core.core.utils.StringUtils;
import com.hhoa.kline.core.core.workspace.WorkspaceConfig;
import com.hhoa.kline.core.core.workspace.WorkspaceResolver;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;

final class FileToolSupport {

    private FileToolSupport() {}

    static ResolvedFileTarget resolveFileTarget(
            ToolContext context, String rawRelPath, String source) {
        if (context.getWorkspaceManager() == null) {
            throw new IllegalStateException("workspaceManager 未配置，无法操作文件");
        }
        WorkspaceConfig workspaceConfig = new WorkspaceConfig(context.getWorkspaceManager());
        WorkspaceResolver.WorkspacePathResult pathResult =
                WorkspaceResolver.resolveWorkspacePath(workspaceConfig, rawRelPath, source);

        Path absolutePath = Paths.get(pathResult.absolutePath());
        String resolvedPath = pathResult.resolvedPath();

        Path fallbackAbsolutePath = Paths.get(context.getCwd(), rawRelPath).normalize();
        TelemetryService.WorkspaceContext workspaceContext =
                new TelemetryService.WorkspaceContext(
                        true, !absolutePath.equals(fallbackAbsolutePath), "hint");

        return new ResolvedFileTarget(rawRelPath, resolvedPath, absolutePath, workspaceContext);
    }

    static boolean validateClineIgnore(ToolContext context, String resolvedPath) {
        ClineIgnoreController controller = context.getServices().getClineIgnoreController();
        return controller == null || controller.validateAccess(resolvedPath);
    }

    static boolean determineFileExists(DiffViewProvider dvp, Path absolutePath) {
        if (dvp != null && dvp.getEditType() != null) {
            return dvp.getEditType() == DiffViewProvider.EditType.MODIFY;
        }
        boolean exists = Files.exists(absolutePath);
        if (dvp != null) {
            dvp.setEditType(exists ? DiffViewProvider.EditType.MODIFY : DiffViewProvider.EditType.CREATE);
        }
        return exists;
    }

    static String getModelId(ToolContext context) {
        return context.getApi() != null && context.getApi().getModel() != null
                ? context.getApi().getModel().getId()
                : "unknown";
    }

    static String buildToolMessage(String tool, String path, String content) {
        return buildJsonMessage(tool, path, content, "true");
    }

    static String buildUserFeedbackMessage(String tool, String path, String diff) {
        return """
            {"tool":"%s","path":"%s","diff":"%s"}"""
                .formatted(tool, StringUtils.escapeJson(path), StringUtils.escapeJson(diff));
    }

    private static String buildJsonMessage(
            String tool, String path, String content, String operationIsLocatedInWorkspace) {
        return MessageFormat.format(
                """
            '{'"tool":"{0}","path":"{1}","content":"{2}","operationIsLocatedInWorkspace":{3}'}'""",
                tool,
                StringUtils.escapeJson(path),
                StringUtils.escapeJson(content),
                operationIsLocatedInWorkspace != null ? operationIsLocatedInWorkspace : "true");
    }

    record ResolvedFileTarget(
            String relPath,
            String resolvedPath,
            Path absolutePath,
            TelemetryService.WorkspaceContext workspaceContext) {}
}
