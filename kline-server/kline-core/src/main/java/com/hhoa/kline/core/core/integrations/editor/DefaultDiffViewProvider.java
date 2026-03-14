package com.hhoa.kline.core.core.integrations.editor;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * 默认的 DiffViewProvider 实现 提供基于内存的简单实现，适用于测试或命令行环境
 *
 * @author hhoa
 */
@Slf4j
public class DefaultDiffViewProvider extends DiffViewProvider {

    private List<String> documentLines = new ArrayList<>();

    private boolean editorOpen = false;

    @Override
    protected void openDiffEditor() {
        if (this.absolutePath == null) {
            log.warn("No file path set, cannot open diff editor");
            return;
        }

        if (this.originalContent != null) {
            String[] lines = this.originalContent.split("\n", -1);
            this.documentLines = new ArrayList<>(List.of(lines));
        } else {
            this.documentLines = new ArrayList<>();
        }

        this.editorOpen = true;
        log.debug("Opened diff editor for file: {}", this.absolutePath);
    }

    @Override
    protected void scrollEditorToLine(int line) {
        if (!this.editorOpen) {
            log.warn("Editor not open, cannot scroll to line {}", line);
            return;
        }
        log.debug("Scrolled to line: {}", line);
    }

    @Override
    protected void scrollAnimation(int startLine, int endLine) {
        if (!this.editorOpen) {
            log.warn("Editor not open, cannot animate scroll");
            return;
        }
        log.debug("Scroll animation from line {} to {}", startLine, endLine);
    }

    @Override
    protected void truncateDocument(int lineNumber) {
        if (!this.editorOpen) {
            log.warn("Editor not open, cannot truncate document");
            return;
        }

        if (lineNumber >= 0 && lineNumber < this.documentLines.size()) {
            this.documentLines =
                    new ArrayList<>(
                            this.documentLines.subList(
                                    0, Math.min(lineNumber, this.documentLines.size())));
            log.debug("Truncated document to line {}", lineNumber);
        } else if (lineNumber >= this.documentLines.size()) {
            log.debug(
                    "Line number {} is beyond document size {}, no truncation needed",
                    lineNumber,
                    this.documentLines.size());
        }
    }

    @Override
    protected String getDocumentText() {
        if (!this.editorOpen) {
            log.warn("Editor not open, cannot get document text");
            return null;
        }

        return String.join("\n", this.documentLines);
    }

    @Override
    protected boolean saveDocument() {
        if (!this.editorOpen) {
            log.warn("Editor not open, cannot save document");
            return false;
        }

        if (this.absolutePath == null) {
            log.warn("No file path set, cannot save document");
            return false;
        }

        try {
            String content = String.join("\n", this.documentLines);

            Charset charset;
            try {
                if (this.fileEncoding != null && !this.fileEncoding.isEmpty()) {
                    charset = Charset.forName(this.fileEncoding);
                } else {
                    charset = StandardCharsets.UTF_8;
                }
            } catch (Exception e) {
                log.warn("Invalid encoding '{}', using UTF-8", this.fileEncoding, e);
                charset = StandardCharsets.UTF_8;
            }

            if (this.absolutePath.getParent() != null) {
                Files.createDirectories(this.absolutePath.getParent());
            }

            Files.writeString(this.absolutePath, content, charset);

            log.info("Document saved to disk: {}", this.absolutePath);
            return true;
        } catch (IOException e) {
            log.error("Failed to save document to disk: {}", this.absolutePath, e);
            return false;
        } catch (Exception e) {
            log.error("Unexpected error while saving document: {}", this.absolutePath, e);
            return false;
        }
    }

    @Override
    protected void closeAllDiffViews() {
        this.editorOpen = false;
        log.debug("Closed all diff views");
    }

    @Override
    protected void resetDiffView() {
        this.documentLines = new ArrayList<>();
        this.editorOpen = false;
        log.debug("Reset diff view");
    }

    @Override
    public void replaceText(String content, TextRange rangeToReplace, Integer currentLine) {
        if (!this.editorOpen) {
            log.warn("Editor not open, cannot replace text");
            return;
        }

        if (rangeToReplace == null) {
            log.warn("Invalid range to replace");
            return;
        }

        int startLine = rangeToReplace.startLine();
        int endLine = rangeToReplace.endLine();

        if (startLine < 0) {
            startLine = 0;
        }
        if (endLine > this.documentLines.size()) {
            endLine = this.documentLines.size();
        }
        if (startLine > endLine) {
            log.warn("Invalid range: startLine {} > endLine {}", startLine, endLine);
            return;
        }

        String[] newLines = content.split("\n", -1);

        List<String> newDocumentLines = new ArrayList<>();

        if (startLine > 0) {
            newDocumentLines.addAll(this.documentLines.subList(0, startLine));
        }

        newDocumentLines.addAll(List.of(newLines));

        if (endLine < this.documentLines.size()) {
            newDocumentLines.addAll(this.documentLines.subList(endLine, this.documentLines.size()));
        }

        this.documentLines = newDocumentLines;

        log.debug(
                "Replaced text from line {} to {} with {} lines",
                startLine,
                endLine,
                newLines.length);
    }
}
