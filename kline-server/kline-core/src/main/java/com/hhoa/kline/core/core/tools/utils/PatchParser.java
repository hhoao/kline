package com.hhoa.kline.core.core.tools.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.Getter;

/**
 * 与 Cline {@code PatchParser.ts} 对齐：解析 Apply Patch 内容（V4A diff 格式）。
 *
 * <p>支持 UPDATE、DELETE、ADD 操作，带模糊匹配和警告。
 */
public class PatchParser {

    // ─── Patch Markers ─────────────────────────────────────────
    public static final String BEGIN = "*** Begin Patch";
    public static final String END = "*** End Patch";
    public static final String UPDATE = "*** Update File: ";
    public static final String DELETE = "*** Delete File: ";
    public static final String ADD = "*** Add File: ";
    public static final String MOVE = "*** Move to: ";
    public static final String END_FILE = "***";

    // ─── Types ─────────────────────────────────────────────────

    public enum PatchActionType {
        UPDATE,
        DELETE,
        ADD
    }

    @Data
    public static class PatchChunk {
        private int origIndex;
        private List<String> delLines;
        private List<String> insLines;

        public PatchChunk(int origIndex, List<String> delLines, List<String> insLines) {
            this.origIndex = origIndex;
            this.delLines = delLines;
            this.insLines = insLines;
        }
    }

    @Data
    public static class PatchAction {
        private PatchActionType type;
        private List<PatchChunk> chunks = new ArrayList<>();
        private String movePath;
        private String newFile;

        public PatchAction(PatchActionType type) {
            this.type = type;
        }
    }

    @Data
    public static class PatchWarning {
        private String path;
        private int chunkIndex;
        private String message;
        private String context;
    }

    @Data
    public static class Patch {
        private Map<String, PatchAction> actions = new LinkedHashMap<>();
        private List<PatchWarning> warnings = new ArrayList<>();
    }

    @Data
    public static class ParseResult {
        private Patch patch;
        private int fuzz;

        public ParseResult(Patch patch, int fuzz) {
            this.patch = patch;
            this.fuzz = fuzz;
        }
    }

    public static class DiffError extends RuntimeException {
        public DiffError(String message) {
            super(message);
        }
    }

    // ─── Parser ────────────────────────────────────────────────

    private final String[] lines;
    private final Map<String, String> currentFiles;
    private Patch patch = new Patch();
    private int index = 0;
    private int fuzz = 0;
    private String currentPath;

    public PatchParser(String[] lines, Map<String, String> currentFiles) {
        this.lines = lines;
        this.currentFiles = currentFiles;
    }

    public ParseResult parse() {
        skipBeginSentinel();

        while (hasMoreLines() && !isEndMarker()) {
            parseNextAction();
        }

        if (patch.getWarnings().isEmpty()) {
            patch.setWarnings(null);
        }

        return new ParseResult(patch, fuzz);
    }

    private void addWarning(PatchWarning warning) {
        if (patch.getWarnings() == null) {
            patch.setWarnings(new ArrayList<>());
        }
        patch.getWarnings().add(warning);
    }

    private void skipBeginSentinel() {
        if (index < lines.length && lines[index] != null && lines[index].startsWith(BEGIN)) {
            index++;
        }
    }

    private boolean hasMoreLines() {
        return index < lines.length;
    }

    private boolean isEndMarker() {
        return lines[index] != null && lines[index].startsWith(END);
    }

    private void parseNextAction() {
        String line = lines[index];

        if (line != null && line.startsWith(UPDATE)) {
            parseUpdate(line.substring(UPDATE.length()).trim());
        } else if (line != null && line.startsWith(DELETE)) {
            parseDelete(line.substring(DELETE.length()).trim());
        } else if (line != null && line.startsWith(ADD)) {
            parseAdd(line.substring(ADD.length()).trim());
        } else {
            throw new DiffError("Unknown line while parsing: " + line);
        }
    }

    private void checkDuplicate(String path, String operation) {
        if (patch.getActions().containsKey(path)) {
            throw new DiffError("Duplicate " + operation + " for file: " + path);
        }
    }

    private void parseUpdate(String path) {
        checkDuplicate(path, "update");
        currentPath = path;

        index++;
        String movePath = null;
        if (index < lines.length && lines[index] != null && lines[index].startsWith(MOVE)) {
            movePath = lines[index].substring(MOVE.length()).trim();
            index++;
        }

        if (!currentFiles.containsKey(path)) {
            throw new DiffError("Update File Error: Missing File: " + path);
        }

        String text = currentFiles.getOrDefault(path, "");
        PatchAction action = parseUpdateFile(text, path);
        action.setMovePath(movePath);

        patch.getActions().put(path, action);
        currentPath = null;
    }

    private PatchAction parseUpdateFile(String text, String _path) {
        PatchAction action = new PatchAction(PatchActionType.UPDATE);
        String[] fileLines = text.split("\n", -1);
        int fileIndex = 0;

        List<String> stopMarkers = Arrays.asList(END, UPDATE, DELETE, ADD, END_FILE);

        while (index < lines.length
                && stopMarkers.stream()
                        .noneMatch(
                                m -> lines[index] != null && lines[index].startsWith(m.trim()))) {
            String currentLine = lines[index];
            String defStr =
                    currentLine != null && currentLine.startsWith("@@ ")
                            ? currentLine.substring(3)
                            : null;
            boolean isSectionMarker = "@@".equals(currentLine);

            if (defStr != null || isSectionMarker) {
                index++;
            } else if (fileIndex != 0) {
                throw new DiffError("Invalid Line:\n" + lines[index]);
            }

            if (defStr != null && !defStr.trim().isEmpty()) {
                String canonDef = canonicalize(defStr.trim());
                for (int i = fileIndex; i < fileLines.length; i++) {
                    String fileLine = fileLines[i];
                    if (fileLine != null
                            && (canonicalize(fileLine).equals(canonDef)
                                    || canonicalize(fileLine.trim()).equals(canonDef))) {
                        fileIndex = i + 1;
                        if (canonicalize(fileLine.trim()).equals(canonDef)
                                && !canonicalize(fileLine).equals(canonDef)) {
                            fuzz++;
                        }
                        break;
                    }
                }
            }

            PeekResult peekResult = peek(lines, index);
            FindResult findResult =
                    findContext(fileLines, peekResult.context, fileIndex, peekResult.eof);

            if (findResult.index == -1) {
                String ctxText = String.join("\n", peekResult.context);
                PatchWarning warning = new PatchWarning();
                warning.setPath(currentPath != null ? currentPath : _path);
                warning.setChunkIndex(action.getChunks().size());
                warning.setMessage(
                        String.format(
                                "Could not find matching context (similarity: %.2f). Chunk skipped.",
                                findResult.similarity));
                warning.setContext(
                        ctxText.length() > 200 ? ctxText.substring(0, 200) + "..." : ctxText);
                addWarning(warning);
                index = peekResult.endIndex;
            } else {
                fuzz += findResult.fuzz;

                for (PatchChunk chunk : peekResult.chunks) {
                    chunk.setOrigIndex(chunk.getOrigIndex() + findResult.index);
                    action.getChunks().add(chunk);
                }

                fileIndex = findResult.index + peekResult.context.size();
                index = peekResult.endIndex;
            }
        }

        return action;
    }

    private void parseDelete(String path) {
        checkDuplicate(path, "delete");

        if (!currentFiles.containsKey(path)) {
            throw new DiffError("Delete File Error: Missing File: " + path);
        }

        patch.getActions().put(path, new PatchAction(PatchActionType.DELETE));
        index++;
    }

    private void parseAdd(String path) {
        checkDuplicate(path, "add");

        if (currentFiles.containsKey(path)) {
            throw new DiffError("Add File Error: File already exists: " + path);
        }

        index++;
        List<String> addedLines = new ArrayList<>();
        List<String> addStopMarkers = Arrays.asList(END, UPDATE, DELETE, ADD);

        while (hasMoreLines()
                && addStopMarkers.stream()
                        .noneMatch(
                                m -> lines[index] != null && lines[index].startsWith(m.trim()))) {
            String line = lines[index++];
            if (line == null) {
                break;
            }
            if (!line.startsWith("+")) {
                throw new DiffError("Invalid Add File line (missing '+'): " + line);
            }
            addedLines.add(line.substring(1));
        }

        PatchAction action = new PatchAction(PatchActionType.ADD);
        action.setNewFile(String.join("\n", addedLines));
        patch.getActions().put(path, action);
    }

    // ─── Static helpers ────────────────────────────────────────

    static String canonicalize(String s) {
        if (s == null) {
            return "";
        }
        return s.replaceAll("\\s+", " ").trim().toLowerCase();
    }

    @Getter
    static class PeekResult {
        final List<String> context;
        final List<PatchChunk> chunks;
        final int endIndex;
        final boolean eof;

        PeekResult(List<String> context, List<PatchChunk> chunks, int endIndex, boolean eof) {
            this.context = context;
            this.chunks = chunks;
            this.endIndex = endIndex;
            this.eof = eof;
        }
    }

    static PeekResult peek(String[] lines, int initialIndex) {
        int idx = initialIndex;
        List<String> old = new ArrayList<>();
        List<String> delLines = new ArrayList<>();
        List<String> insLines = new ArrayList<>();
        List<PatchChunk> chunks = new ArrayList<>();
        String mode = "keep";

        List<String> stopMarkers = Arrays.asList("@@", END, UPDATE, DELETE, ADD, END_FILE);

        while (idx < lines.length) {
            String s = lines[idx];
            if (s == null || stopMarkers.stream().anyMatch(m -> s.startsWith(m.trim()))) {
                break;
            }
            if ("***".equals(s)) {
                break;
            }
            if (s.startsWith("***")) {
                throw new DiffError("Invalid line: " + s);
            }

            idx++;
            String lastMode = mode;
            String line = s;

            if (line.startsWith("+")) {
                mode = "add";
            } else if (line.startsWith("-")) {
                mode = "delete";
            } else if (line.startsWith(" ")) {
                mode = "keep";
            } else {
                mode = "keep";
                line = " " + line;
            }

            line = line.substring(1);

            if ("keep".equals(mode) && !mode.equals(lastMode)) {
                if (!insLines.isEmpty() || !delLines.isEmpty()) {
                    chunks.add(
                            new PatchChunk(
                                    old.size() - delLines.size(),
                                    new ArrayList<>(delLines),
                                    new ArrayList<>(insLines)));
                }
                delLines = new ArrayList<>();
                insLines = new ArrayList<>();
            }

            if ("delete".equals(mode)) {
                delLines.add(line);
                old.add(line);
            } else if ("add".equals(mode)) {
                insLines.add(line);
            } else {
                old.add(line);
            }
        }

        if (!insLines.isEmpty() || !delLines.isEmpty()) {
            chunks.add(
                    new PatchChunk(
                            old.size() - delLines.size(),
                            new ArrayList<>(delLines),
                            new ArrayList<>(insLines)));
        }

        if (idx < lines.length && END_FILE.equals(lines[idx])) {
            idx++;
            return new PeekResult(old, chunks, idx, true);
        }

        return new PeekResult(old, chunks, idx, false);
    }

    @Getter
    static class FindResult {
        final int index;
        final int fuzz;
        final double similarity;

        FindResult(int index, int fuzz, double similarity) {
            this.index = index;
            this.fuzz = fuzz;
            this.similarity = similarity;
        }
    }

    static FindResult findContext(
            String[] fileLines, List<String> context, int start, boolean eof) {
        if (context.isEmpty()) {
            return new FindResult(start, 0, 1.0);
        }

        double bestSimilarity = 0;
        String canonicalContext = canonicalize(String.join("\n", context));

        // Pass 1: exact equality after canonicalization
        for (int i = start; i < fileLines.length; i++) {
            int end = Math.min(i + context.size(), fileLines.length);
            List<String> slice = Arrays.asList(Arrays.copyOfRange(fileLines, i, end));
            String segment = canonicalize(String.join("\n", slice));
            if (segment.equals(canonicalContext)) {
                return new FindResult(i, 0, 1.0);
            }
            double sim = calculateSimilarity(segment, canonicalContext);
            if (sim > bestSimilarity) {
                bestSimilarity = sim;
            }
        }

        // Pass 2: ignore trailing whitespace
        for (int i = start; i < fileLines.length; i++) {
            int end = Math.min(i + context.size(), fileLines.length);
            List<String> slice = new ArrayList<>();
            for (int j = i; j < end; j++) {
                slice.add(fileLines[j].stripTrailing());
            }
            List<String> ctxTrimmed = new ArrayList<>();
            for (String s : context) {
                ctxTrimmed.add(s.stripTrailing());
            }
            if (canonicalize(String.join("\n", slice))
                    .equals(canonicalize(String.join("\n", ctxTrimmed)))) {
                return new FindResult(i, 1, 1.0);
            }
        }

        // Pass 3: ignore all surrounding whitespace
        for (int i = start; i < fileLines.length; i++) {
            int end = Math.min(i + context.size(), fileLines.length);
            List<String> slice = new ArrayList<>();
            for (int j = i; j < end; j++) {
                slice.add(fileLines[j].trim());
            }
            List<String> ctxTrimmed = new ArrayList<>();
            for (String s : context) {
                ctxTrimmed.add(s.trim());
            }
            if (canonicalize(String.join("\n", slice))
                    .equals(canonicalize(String.join("\n", ctxTrimmed)))) {
                return new FindResult(i, 100, 1.0);
            }
        }

        // Pass 4: partial matching with similarity threshold
        double SIMILARITY_THRESHOLD = 0.66;
        for (int i = start; i < fileLines.length; i++) {
            int end = Math.min(i + context.size(), fileLines.length);
            List<String> slice = Arrays.asList(Arrays.copyOfRange(fileLines, i, end));
            String segment = canonicalize(String.join("\n", slice));
            double similarity = calculateSimilarity(segment, canonicalContext);
            if (similarity >= SIMILARITY_THRESHOLD) {
                return new FindResult(i, 1000, similarity);
            }
            if (similarity > bestSimilarity) {
                bestSimilarity = similarity;
            }
        }

        return new FindResult(-1, 0, bestSimilarity);
    }

    static double calculateSimilarity(String str1, String str2) {
        String longer = str1.length() > str2.length() ? str1 : str2;
        String shorter = str1.length() > str2.length() ? str2 : str1;
        if (longer.isEmpty()) {
            return 1.0;
        }
        int editDistance = levenshteinDistance(shorter, longer);
        return (double) (longer.length() - editDistance) / longer.length();
    }

    static int levenshteinDistance(String str1, String str2) {
        int rows = str2.length() + 1;
        int cols = str1.length() + 1;
        int[] matrix = new int[rows * cols];

        for (int i = 0; i <= str2.length(); i++) {
            matrix[i * cols] = i;
        }
        for (int j = 0; j <= str1.length(); j++) {
            matrix[j] = j;
        }

        for (int i = 1; i <= str2.length(); i++) {
            for (int j = 1; j <= str1.length(); j++) {
                if (str2.charAt(i - 1) == str1.charAt(j - 1)) {
                    matrix[i * cols + j] = matrix[(i - 1) * cols + (j - 1)];
                } else {
                    matrix[i * cols + j] =
                            1
                                    + Math.min(
                                            matrix[(i - 1) * cols + (j - 1)],
                                            Math.min(
                                                    matrix[i * cols + (j - 1)],
                                                    matrix[(i - 1) * cols + j]));
                }
            }
        }

        return matrix[str2.length() * cols + str1.length()];
    }
}
