package com.hhoa.kline.core.core.integrations.editor;

import com.hhoa.kline.core.core.integrations.diagnostics.Diagnostics;
import com.hhoa.kline.core.core.integrations.misc.ExtractText;
import com.hhoa.kline.core.core.integrations.misc.OpenFile;
import com.hhoa.kline.core.core.shared.multiroot.WorkspaceRoot;
import com.hhoa.kline.core.core.workspace.WorkspaceConfig;
import com.hhoa.kline.core.core.workspace.WorkspaceResolver;
import com.hhoa.kline.core.core.workspace.WorkspaceResolver.WorkspacePathResult;
import com.hhoa.kline.core.core.workspace.WorkspaceRootManager;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * 差异视图提供者抽象类 用于管理文件编辑过程中的差异视图显示
 *
 * @author hhoa
 */
@Slf4j
@Data
public abstract class DiffViewProvider {

    protected EditType editType;

    protected boolean isEditing = false;

    protected String originalContent;

    private List<Path> createdDirs = new ArrayList<>();

    protected boolean documentWasOpen = false;

    private List<FileDiagnostics> preDiagnostics = new ArrayList<>();

    protected String relPath;

    protected Path absolutePath;

    protected String fileEncoding = "utf8";

    private List<String> streamedLines = new ArrayList<>();

    private String newContent;

    private WorkspaceRootManager workspaceManager;

    public enum EditType {
        CREATE,
        MODIFY
    }

    public static class FileDiagnostics {
        private String filePath;
        private List<Diagnostic> diagnostics = new ArrayList<>();

        public String getFilePath() {
            return filePath;
        }

        public void setFilePath(String filePath) {
            this.filePath = filePath;
        }

        public List<Diagnostic> getDiagnostics() {
            return diagnostics;
        }

        public void setDiagnostics(List<Diagnostic> diagnostics) {
            this.diagnostics = diagnostics;
        }
    }

    public static class Diagnostic {
        private String message;
        private DiagnosticRange range;
        private DiagnosticSeverity severity;
        private String source;

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public DiagnosticRange getRange() {
            return range;
        }

        public void setRange(DiagnosticRange range) {
            this.range = range;
        }

        public DiagnosticSeverity getSeverity() {
            return severity;
        }

        public void setSeverity(DiagnosticSeverity severity) {
            this.severity = severity;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        // 向后兼容的方法（如果现有代码使用 line/column）
        @Deprecated
        public int getLine() {
            return range != null && range.getStart() != null ? range.getStart().getLine() : 0;
        }

        @Deprecated
        public void setLine(int line) {
            if (range == null) {
                range = new DiagnosticRange();
            }
            if (range.getStart() == null) {
                range.setStart(new DiagnosticPosition());
            }
            range.getStart().setLine(line);
        }

        @Deprecated
        public int getColumn() {
            return range != null && range.getStart() != null ? range.getStart().getCharacter() : 0;
        }

        @Deprecated
        public void setColumn(int column) {
            if (range == null) {
                range = new DiagnosticRange();
            }
            if (range.getStart() == null) {
                range.setStart(new DiagnosticPosition());
            }
            range.getStart().setCharacter(column);
        }
    }

    public static class DiagnosticRange {
        private DiagnosticPosition start;
        private DiagnosticPosition end;

        public DiagnosticPosition getStart() {
            return start;
        }

        public void setStart(DiagnosticPosition start) {
            this.start = start;
        }

        public DiagnosticPosition getEnd() {
            return end;
        }

        public void setEnd(DiagnosticPosition end) {
            this.end = end;
        }
    }

    public static class DiagnosticPosition {
        private int line;
        private int character;

        public int getLine() {
            return line;
        }

        public void setLine(int line) {
            this.line = line;
        }

        public int getCharacter() {
            return character;
        }

        public void setCharacter(int character) {
            this.character = character;
        }
    }

    public enum DiagnosticSeverity {
        ERROR,
        WARNING,
        INFO,
        HINT
    }

    /**
     * 打开差异视图
     *
     * @param relPath 相对路径
     * @param displayPath 显示路径（可选）
     * @throws IOException 如果打开失败
     */
    public void open(String relPath, String displayPath) throws IOException {
        this.isEditing = true;

        WorkspacePathResult absolutePathResolved =
                WorkspaceResolver.resolveWorkspacePath(
                        new WorkspaceConfig(requireWorkspaceManager()),
                        relPath,
                        "DiffViewProvider.open.absolutePath");
        this.absolutePath = Paths.get(absolutePathResolved.absolutePath());
        this.relPath = displayPath != null ? displayPath : relPath;
        boolean fileExists = this.editType == EditType.MODIFY;

        if (fileExists && Files.exists(this.absolutePath)) {
            byte[] fileBuffer = Files.readAllBytes(this.absolutePath);
            this.fileEncoding =
                    ExtractText.detectEncoding(fileBuffer, getFileExtension(absolutePath));
            Charset charset = Charset.forName(this.fileEncoding);
            this.originalContent = new String(fileBuffer, charset);
        } else {
            this.originalContent = "";
            this.fileEncoding = StandardCharsets.UTF_8.name();
        }

        this.createdDirs = createDirectoriesForFile(this.absolutePath);

        if (!fileExists) {
            Files.writeString(this.absolutePath, "");
        }

        this.preDiagnostics = getDiagnostics();

        openDiffEditor();

        scrollEditorToLine(0);

        this.streamedLines = new ArrayList<>();
    }

    public void setWorkspaceManager(WorkspaceRootManager workspaceManager) {
        this.workspaceManager = workspaceManager;
    }

    protected WorkspaceRootManager getWorkspaceManager() {
        return workspaceManager;
    }

    private WorkspaceRootManager requireWorkspaceManager() {
        if (workspaceManager == null) {
            throw new IllegalStateException("workspaceManager 未配置，无法执行 Diff 操作");
        }
        return workspaceManager;
    }

    /**
     * 为文件创建所有必要的目录
     *
     * @param filePath 文件路径
     * @return 创建的目录列表（从最顶层到最底层）
     */
    private List<Path> createDirectoriesForFile(Path filePath) throws IOException {
        List<Path> newDirectories = new ArrayList<>();
        Path normalizedPath = filePath.normalize();
        Path directoryPath = normalizedPath.getParent();

        if (directoryPath == null) {
            return newDirectories;
        }

        // 收集所有需要创建的目录（从最深层到最浅层）
        List<Path> dirsToCreate = new ArrayList<>();
        Path currentPath = directoryPath;

        while (currentPath != null && !Files.exists(currentPath)) {
            dirsToCreate.add(currentPath);
            currentPath = currentPath.getParent();
        }

        for (int i = dirsToCreate.size() - 1; i >= 0; i--) {
            Path dirToCreate = dirsToCreate.get(i);
            Files.createDirectories(dirToCreate);
            newDirectories.add(dirToCreate);
        }

        return newDirectories;
    }

    protected abstract void openDiffEditor();

    /**
     * 滚动编辑器到指定行 子类必须实现此方法
     *
     * @param line 行号（0-based）
     */
    protected abstract void scrollEditorToLine(int line);

    /**
     * 创建滚动动画 子类必须实现此方法以提供滚动动画效果 number): Promise<void>
     *
     * @param startLine 起始行
     * @param endLine 结束行
     */
    protected abstract void scrollAnimation(int startLine, int endLine);

    /**
     * 截断文档 从指定行号开始截断到文档末尾
     *
     * @param lineNumber 行号
     */
    protected abstract void truncateDocument(int lineNumber);

    /**
     * 获取文档文本
     *
     * @return 文档文本，如果文档已关闭返回 null
     */
    protected abstract String getDocumentText();

    /**
     * 获取新的诊断问题
     *
     * @return 新的诊断问题字符串
     */
    private String getNewDiagnosticProblems() {
        List<FileDiagnostics> postDiagnostics = getDiagnostics();
        List<FileDiagnostics> newProblems =
                Diagnostics.getNewDiagnostics(this.preDiagnostics, postDiagnostics);

        List<DiagnosticSeverity> severities = new ArrayList<>();
        severities.add(DiagnosticSeverity.ERROR);

        String basePath = getPrimaryWorkspacePath();
        return Diagnostics.diagnosticsToProblemsString(newProblems, severities, basePath);
    }

    private String getPrimaryWorkspacePath() {
        WorkspaceRootManager manager = requireWorkspaceManager();
        WorkspaceRoot primary = manager.getPrimaryRoot();
        if (primary == null) {
            List<WorkspaceRoot> roots = manager.getRoots();
            if (roots.isEmpty()) {
                throw new IllegalStateException("未配置任何工作区根目录");
            }
            primary = roots.get(0);
        }
        return primary.getPath();
    }

    /**
     * 获取诊断信息 子类可以实现此方法以获取实际的诊断信息
     *
     * @return 诊断信息列表
     */
    protected List<FileDiagnostics> getDiagnostics() {
        // 默认实现：返回空列表
        // 在实际应用中，这里应该调用相应的诊断服务
        return new ArrayList<>();
    }

    /**
     * 保存文档
     *
     * @return 是否保存成功
     */
    protected abstract boolean saveDocument();

    protected abstract void closeAllDiffViews();

    protected abstract void resetDiffView();

    /**
     * 更新内容
     *
     * @param accumulatedContent 累积的内容
     * @param isFinal 是否为最终更新
     * @param changeLocation 变更位置（可选）
     */
    public void update(String accumulatedContent, boolean isFinal, ChangeLocation changeLocation) {
        updateSync(accumulatedContent, isFinal, changeLocation);
    }

    /**
     * 同步更新内容（内部方法）
     *
     * @param accumulatedContent 累积的内容
     * @param isFinal 是否为最终更新
     * @param changeLocation 变更位置（可选）
     */
    private void updateSync(
            String accumulatedContent, boolean isFinal, ChangeLocation changeLocation) {
        if (!this.isEditing) {
            throw new IllegalStateException("Not editing any file");
        }

        // 移除潜在的 BOM
        if (accumulatedContent.startsWith("\ufeff")) {
            accumulatedContent = accumulatedContent.substring(1);
        }

        this.newContent = accumulatedContent;
        List<String> accumulatedLinesList =
                new ArrayList<>(Arrays.asList(accumulatedContent.split("\n")));

        if (!isFinal && !accumulatedLinesList.isEmpty()) {
            accumulatedLinesList.remove(accumulatedLinesList.size() - 1);
        }

        String[] accumulatedLines = accumulatedLinesList.toArray(new String[0]);

        int diffLinesLength = accumulatedLines.length - this.streamedLines.size();
        int currentLine = this.streamedLines.size() + diffLinesLength - 1;

        if (currentLine >= 0) {
            List<String> linesToReplace = new ArrayList<>();
            for (int i = 0; i <= currentLine && i < accumulatedLines.length; i++) {
                linesToReplace.add(accumulatedLines[i]);
            }
            String contentToReplace = String.join("\n", linesToReplace) + "\n";
            TextRange rangeToReplace = new TextRange(0, currentLine + 1);

            replaceText(contentToReplace, rangeToReplace, currentLine);

            if (changeLocation != null) {
                scrollEditorToLine(changeLocation.startLine);
            } else {
                if (diffLinesLength <= 5) {
                    scrollEditorToLine(currentLine);
                } else {
                    int startLine = this.streamedLines.size();
                    scrollAnimation(startLine, currentLine);
                    scrollEditorToLine(currentLine);
                }
            }
        }

        this.streamedLines = new ArrayList<>(Arrays.asList(accumulatedLines));

        if (isFinal) {
            truncateDocument(this.streamedLines.size());

            if (this.originalContent != null && this.originalContent.endsWith("\n")) {
                if (accumulatedLines.length == 0
                        || !accumulatedLines[accumulatedLines.length - 1].isEmpty()) {
                    accumulatedContent += "\n";
                    this.newContent = accumulatedContent;
                }
            }
        }
    }

    /**
     * 替换文本
     *
     * @param content 新内容
     * @param rangeToReplace 要替换的范围
     * @param currentLine 当前行号
     */
    public abstract void replaceText(String content, TextRange rangeToReplace, Integer currentLine);

    /**
     * 保存变更
     *
     * @return 保存结果
     */
    public SaveResult saveChanges() {
        String preSaveContent = getDocumentText();
        if (this.relPath == null
                || this.absolutePath == null
                || this.newContent == null
                || preSaveContent == null) {
            return new SaveResult();
        }

        boolean saved = saveDocument();
        if (!saved) {
            return new SaveResult();
        }

        String postSaveContentParam = getDocumentText();
        final String postSaveContent = postSaveContentParam != null ? postSaveContentParam : "";
        final String preSaveContentFinal = preSaveContent;

        // openFile
        try {
            OpenFile.openFile(this.absolutePath, true, false);
        } catch (Exception e) {
            log.warn("Failed to open file after save", e);
        }

        closeAllDiffViews();

        String newProblems = getNewDiagnosticProblems();
        String newProblemsMessage =
                newProblems.isEmpty()
                        ? ""
                        : "\n\nNew problems detected after saving the file:\n" + newProblems;

        // 规范化换行符
        String newContentEOL = this.newContent.contains("\r\n") ? "\r\n" : "\n";
        String normalizedPreSaveContent =
                preSaveContentFinal.replaceAll("\r\n|\n", newContentEOL).replaceAll("\\s+$", "")
                        + newContentEOL;
        String normalizedPostSaveContent =
                postSaveContent.replaceAll("\r\n|\n", newContentEOL).replaceAll("\\s+$", "")
                        + newContentEOL;
        String normalizedNewContent =
                this.newContent.replaceAll("\r\n|\n", newContentEOL).replaceAll("\\s+$", "")
                        + newContentEOL;

        SaveResult result = new SaveResult();
        result.newProblemsMessage = newProblemsMessage;

        if (!normalizedPreSaveContent.equals(normalizedNewContent)) {
            String posixPath = toPosix(this.relPath);
            result.userEdits =
                    createPrettyPatch(posixPath, normalizedNewContent, normalizedPreSaveContent);
        }

        if (!normalizedPreSaveContent.equals(normalizedPostSaveContent)) {
            String posixPath = toPosix(this.relPath);
            result.autoFormattingEdits =
                    createPrettyPatch(
                            posixPath, normalizedPreSaveContent, normalizedPostSaveContent);
        }

        result.finalContent = normalizedPostSaveContent;

        return result;
    }

    /**
     * 创建漂亮的补丁 然后去掉前4行（header）
     *
     * @param filePath 文件路径
     * @param oldContent 旧内容
     * @param newContent 新内容
     * @return 补丁字符串（去掉前4行 header）
     */
    private String createPrettyPatch(String filePath, String oldContent, String newContent) {
        String patch =
                generateUnifiedDiff(
                        filePath,
                        oldContent != null ? oldContent : "",
                        newContent != null ? newContent : "");

        String[] lines = patch.split("\n");
        if (lines.length > 4) {
            StringBuilder result = new StringBuilder();
            for (int i = 4; i < lines.length; i++) {
                if (i > 4) {
                    result.append("\n");
                }
                result.append(lines[i]);
            }
            return result.toString();
        }

        return patch;
    }

    /**
     * 生成统一差异格式（类似 diff -u）
     *
     * @param filePath 文件路径
     * @param oldContent 旧内容
     * @param newContent 新内容
     * @return 统一差异格式字符串
     */
    private String generateUnifiedDiff(String filePath, String oldContent, String newContent) {
        StringBuilder patch = new StringBuilder();
        patch.append("Index: ").append(filePath).append("\n");
        patch.append("===================================================================\n");
        patch.append("--- ").append(filePath).append("\n");
        patch.append("+++ ").append(filePath).append("\n");

        String diffContent = generateSimpleDiff(oldContent, newContent);
        patch.append(diffContent);

        return patch.toString();
    }

    /**
     * 生成简单差异
     *
     * @param oldContent 旧内容
     * @param newContent 新内容
     * @return 差异字符串
     */
    private String generateSimpleDiff(String oldContent, String newContent) {
        String[] oldLines = oldContent.split("\n");
        String[] newLines = newContent.split("\n");

        StringBuilder diff = new StringBuilder();
        int maxLen = Math.max(oldLines.length, newLines.length);

        for (int i = 0; i < maxLen; i++) {
            String oldLine = i < oldLines.length ? oldLines[i] : null;
            String newLine = i < newLines.length ? newLines[i] : null;

            if (oldLine == null) {
                diff.append("+").append(newLine).append("\n");
            } else if (newLine == null) {
                diff.append("-").append(oldLine).append("\n");
            } else if (!oldLine.equals(newLine)) {
                diff.append("-").append(oldLine).append("\n");
                diff.append("+").append(newLine).append("\n");
            }
        }

        return diff.toString();
    }

    /**
     * 恢复变更
     *
     * @throws IOException 如果恢复失败
     */
    public void revertChanges() throws IOException {
        revertChangesSync();
    }

    private void revertChangesSync() throws IOException {
        if (this.absolutePath == null || !this.isEditing) {
            return;
        }

        boolean fileExists = this.editType == EditType.MODIFY;

        if (!fileExists) {
            saveDocument();
            closeAllDiffViews();
            Files.deleteIfExists(this.absolutePath);

            for (int i = this.createdDirs.size() - 1; i >= 0; i--) {
                try {
                    Path dir = this.createdDirs.get(i);
                    if (Files.exists(dir)) {
                        // 递归删除目录（如果目录不为空）
                        try (Stream<Path> paths = Files.walk(dir)) {
                            paths.sorted(Comparator.reverseOrder())
                                    .forEach(
                                            path -> {
                                                try {
                                                    Files.delete(path);
                                                } catch (IOException e) {
                                                    log.debug("Could not delete path: {}", path, e);
                                                }
                                            });
                        }
                        log.debug("Directory {} has been deleted.", dir);
                    }
                } catch (IOException e) {
                    log.warn("Could not delete directory: {}", this.createdDirs.get(i), e);
                }
            }
        } else {
            String contents = getDocumentText();
            if (contents == null) {
                contents = "";
            }

            int newlineCount = 0;
            for (int i = 0; i < contents.length(); i++) {
                if (contents.charAt(i) == '\n') {
                    newlineCount++;
                }
            }
            int lineCount = newlineCount + 1;
            TextRange range = new TextRange(0, lineCount);
            replaceText(this.originalContent != null ? this.originalContent : "", range, null);

            saveDocument();

            if (this.documentWasOpen) {
                OpenFile.openFile(this.absolutePath, true, false);
            }

            closeAllDiffViews();
        }

        reset();
    }

    public void scrollToFirstDiff() {
        if (!this.isEditing) {
            return;
        }

        String currentContent = getDocumentText();
        if (currentContent == null) {
            currentContent = "";
        }

        String originalContent = this.originalContent != null ? this.originalContent : "";
        List<DiffPart> diffs = diffLines(originalContent, currentContent);

        int lineCount = 0;
        for (DiffPart part : diffs) {
            if (part.added || part.removed) {
                scrollEditorToLine(lineCount);
                return;
            }
            if (!part.removed) {
                lineCount += part.count;
            }
        }
    }

    private List<DiffPart> diffLines(String oldText, String newText) {
        List<DiffPart> result = new ArrayList<>();
        String[] oldLines = oldText.split("\n", -1);
        String[] newLines = newText.split("\n", -1);

        int oldIndex = 0;
        int newIndex = 0;

        while (oldIndex < oldLines.length || newIndex < newLines.length) {
            if (oldIndex >= oldLines.length) {
                DiffPart part = new DiffPart();
                part.added = true;
                part.removed = false;
                part.count = newLines.length - newIndex;
                result.add(part);
                break;
            } else if (newIndex >= newLines.length) {
                DiffPart part = new DiffPart();
                part.added = false;
                part.removed = true;
                part.count = oldLines.length - oldIndex;
                result.add(part);
                break;
            } else if (oldLines[oldIndex].equals(newLines[newIndex])) {
                int sameCount = 0;
                while (oldIndex < oldLines.length
                        && newIndex < newLines.length
                        && oldLines[oldIndex].equals(newLines[newIndex])) {
                    sameCount++;
                    oldIndex++;
                    newIndex++;
                }
                DiffPart part = new DiffPart();
                part.added = false;
                part.removed = false;
                part.count = sameCount;
                result.add(part);
            } else {
                int oldMatch = -1;
                int newMatch = -1;

                for (int i = oldIndex + 1; i < oldLines.length; i++) {
                    if (oldLines[i].equals(newLines[newIndex])) {
                        oldMatch = i;
                        break;
                    }
                }

                for (int i = newIndex + 1; i < newLines.length; i++) {
                    if (newLines[i].equals(oldLines[oldIndex])) {
                        newMatch = i;
                        break;
                    }
                }

                if (oldMatch >= 0 && (newMatch < 0 || oldMatch - oldIndex <= newMatch - newIndex)) {
                    DiffPart part = new DiffPart();
                    part.added = false;
                    part.removed = true;
                    part.count = oldMatch - oldIndex;
                    result.add(part);
                    oldIndex = oldMatch;
                } else if (newMatch >= 0) {
                    DiffPart part = new DiffPart();
                    part.added = true;
                    part.removed = false;
                    part.count = newMatch - newIndex;
                    result.add(part);
                    newIndex = newMatch;
                } else {
                    DiffPart removedPart = new DiffPart();
                    removedPart.added = false;
                    removedPart.removed = true;
                    removedPart.count = 1;
                    result.add(removedPart);

                    DiffPart addedPart = new DiffPart();
                    addedPart.added = true;
                    addedPart.removed = false;
                    addedPart.count = 1;
                    result.add(addedPart);

                    oldIndex++;
                    newIndex++;
                }
            }
        }

        return result;
    }

    private static class DiffPart {
        boolean added;
        boolean removed;
        int count;
    }

    public void reset() {
        this.isEditing = false;
        this.editType = null;
        this.absolutePath = null;
        this.relPath = null;
        this.preDiagnostics = new ArrayList<>();
        this.originalContent = null;
        this.fileEncoding = StandardCharsets.UTF_8.name();
        this.documentWasOpen = false;
        this.streamedLines = new ArrayList<>();
        this.createdDirs = new ArrayList<>();
        this.newContent = null;

        resetDiffView();
    }

    private String getFileExtension(Path path) {
        String fileName = path.getFileName().toString();
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0 && lastDot < fileName.length() - 1) {
            return fileName.substring(lastDot + 1);
        }
        return "";
    }

    private String toPosix(String path) {
        if (path == null) {
            return null;
        }
        return path.replace('\\', '/');
    }

    public record TextRange(int startLine, int endLine) {}

    public record ChangeLocation(int startLine, int endLine, int startChar, int endChar) {}

    public static class SaveResult {
        public String newProblemsMessage;
        public String userEdits;
        public String autoFormattingEdits;
        public String finalContent;
    }
}
