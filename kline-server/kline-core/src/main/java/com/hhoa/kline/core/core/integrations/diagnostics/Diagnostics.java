package com.hhoa.kline.core.core.integrations.diagnostics;

import com.hhoa.kline.core.core.integrations.editor.DiffViewProvider;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * 诊断工具类
 *
 * @author hhoa
 */
@Slf4j
public class Diagnostics {

    /**
     * 获取新的诊断信息
     *
     * @param oldDiagnostics 旧的诊断信息
     * @param newDiagnostics 新的诊断信息
     * @return 新增的诊断信息列表
     */
    public static List<DiffViewProvider.FileDiagnostics> getNewDiagnostics(
            List<DiffViewProvider.FileDiagnostics> oldDiagnostics,
            List<DiffViewProvider.FileDiagnostics> newDiagnostics) {
        Map<String, List<DiffViewProvider.Diagnostic>> oldMap = new HashMap<>();
        for (DiffViewProvider.FileDiagnostics diag : oldDiagnostics) {
            oldMap.put(diag.getFilePath(), new ArrayList<>(diag.getDiagnostics()));
        }

        List<DiffViewProvider.FileDiagnostics> newProblems = new ArrayList<>();
        for (DiffViewProvider.FileDiagnostics newDiags : newDiagnostics) {
            List<DiffViewProvider.Diagnostic> oldDiags =
                    oldMap.getOrDefault(newDiags.getFilePath(), new ArrayList<>());
            List<DiffViewProvider.Diagnostic> newProblemsForFile = new ArrayList<>();

            for (DiffViewProvider.Diagnostic newDiag : newDiags.getDiagnostics()) {
                boolean isNew = true;
                for (DiffViewProvider.Diagnostic oldDiag : oldDiags) {
                    if (diagnosticsEqual(oldDiag, newDiag)) {
                        isNew = false;
                        break;
                    }
                }
                if (isNew) {
                    newProblemsForFile.add(newDiag);
                }
            }

            if (!newProblemsForFile.isEmpty()) {
                DiffViewProvider.FileDiagnostics newFileDiagnostics =
                        new DiffViewProvider.FileDiagnostics();
                newFileDiagnostics.setFilePath(newDiags.getFilePath());
                newFileDiagnostics.setDiagnostics(newProblemsForFile);
                newProblems.add(newFileDiagnostics);
            }
        }

        return newProblems;
    }

    private static boolean diagnosticsEqual(
            DiffViewProvider.Diagnostic d1, DiffViewProvider.Diagnostic d2) {
        if (d1 == d2) {
            return true;
        }
        if (d1 == null || d2 == null) {
            return false;
        }

        if (d1.getSeverity() != d2.getSeverity()) {
            return false;
        }
        if (!java.util.Objects.equals(d1.getMessage(), d2.getMessage())) {
            return false;
        }
        if (!java.util.Objects.equals(d1.getSource(), d2.getSource())) {
            return false;
        }

        DiffViewProvider.DiagnosticRange range1 = d1.getRange();
        DiffViewProvider.DiagnosticRange range2 = d2.getRange();
        if (range1 == null && range2 == null) {
            return true;
        }
        if (range1 == null || range2 == null) {
            return false;
        }

        DiffViewProvider.DiagnosticPosition start1 = range1.getStart();
        DiffViewProvider.DiagnosticPosition start2 = range2.getStart();
        if (!positionsEqual(start1, start2)) {
            return false;
        }

        DiffViewProvider.DiagnosticPosition end1 = range1.getEnd();
        DiffViewProvider.DiagnosticPosition end2 = range2.getEnd();
        return positionsEqual(end1, end2);
    }

    private static boolean positionsEqual(
            DiffViewProvider.DiagnosticPosition p1, DiffViewProvider.DiagnosticPosition p2) {
        if (p1 == p2) {
            return true;
        }
        if (p1 == null || p2 == null) {
            return false;
        }
        return p1.getLine() == p2.getLine() && p1.getCharacter() == p2.getCharacter();
    }

    /**
     * 将诊断信息转换为问题字符串
     *
     * @param diagnostics 诊断信息列表
     * @param severities 要包含的严重程度列表（如果为 null，则包含所有）
     * @param cwd 当前工作目录
     * @return 格式化的问题字符串，如果没有问题则返回空字符串
     */
    public static String diagnosticsToProblemsString(
            List<DiffViewProvider.FileDiagnostics> diagnostics,
            List<DiffViewProvider.DiagnosticSeverity> severities,
            String cwd) {
        List<String> results = new ArrayList<>();
        for (DiffViewProvider.FileDiagnostics fileDiagnostics : diagnostics) {
            List<DiffViewProvider.Diagnostic> problems = new ArrayList<>();
            for (DiffViewProvider.Diagnostic d : fileDiagnostics.getDiagnostics()) {
                if (severities == null || severities.contains(d.getSeverity())) {
                    problems.add(d);
                }
            }

            String problemString =
                    singleFileDiagnosticsToProblemsString(
                            fileDiagnostics.getFilePath(), problems, cwd);
            if (problemString != null && !problemString.isEmpty()) {
                results.add(problemString);
            }
        }
        return String.join("\n\n", results);
    }

    /**
     * 将单个文件的诊断信息转换为问题字符串
     *
     * @param filePath 文件路径
     * @param diagnostics 诊断信息列表
     * @param cwd 当前工作目录
     * @return 格式化的问题字符串，如果没有问题则返回空字符串
     */
    public static String singleFileDiagnosticsToProblemsString(
            String filePath, List<DiffViewProvider.Diagnostic> diagnostics, String cwd) {
        if (diagnostics == null || diagnostics.isEmpty()) {
            return "";
        }

        String relPath = getRelativePath(cwd, filePath);

        StringBuilder result = new StringBuilder(relPath);

        for (DiffViewProvider.Diagnostic diagnostic : diagnostics) {
            String label = severityToString(diagnostic.getSeverity());

            String line = "";
            if (diagnostic.getRange() != null && diagnostic.getRange().getStart() != null) {
                line = String.valueOf(diagnostic.getRange().getStart().getLine() + 1);
            }

            String source = diagnostic.getSource() != null ? diagnostic.getSource() + " " : "";

            result.append(
                    String.format(
                            "\n- [%s%s] Line %s: %s",
                            source, label, line, diagnostic.getMessage()));
        }

        return result.toString();
    }

    private static String severityToString(DiffViewProvider.DiagnosticSeverity severity) {
        if (severity == null) {
            return "Diagnostic";
        }

        return switch (severity) {
            case ERROR -> "Error";
            case WARNING -> "Warning";
            case INFO -> "Information";
            case HINT -> "Hint";
            default -> {
                log.error("Unhandled diagnostic severity level: " + severity);
                yield "Diagnostic";
            }
        };
    }

    private static String getRelativePath(String cwd, String filePath) {
        try {
            Path cwdPath = Paths.get(cwd).toAbsolutePath().normalize();
            Path filePathObj = Paths.get(filePath).toAbsolutePath().normalize();

            Path relativePath = cwdPath.relativize(filePathObj);
            return relativePath.toString().replace('\\', '/');
        } catch (Exception e) {
            return filePath.replace('\\', '/');
        }
    }
}
