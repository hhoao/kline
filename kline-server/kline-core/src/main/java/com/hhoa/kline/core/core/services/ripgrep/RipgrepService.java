package com.hhoa.kline.core.core.services.ripgrep;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hhoa.kline.core.core.ignore.ClineIgnoreController;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Ripgrep 服务
 *
 * <p>此文件提供使用 ripgrep 对文件执行正则表达式搜索的功能。 受以下项目启发: https://github.com/DiscreteTom/vscode-ripgrep-utils
 *
 * <p>关键组件:
 *
 * <ul>
 *   <li>execRipgrep: 执行 ripgrep 命令并返回输出。
 *   <li>regexSearchFiles: 执行正则搜索的主要函数。
 *       <ul>
 *         <li>参数:
 *             <ul>
 *               <li>cwd: 当前工作目录（用于相对路径计算）
 *               <li>directoryPath: 要搜索的目录
 *               <li>regex: 要搜索的正则表达式（Rust 正则语法）
 *               <li>filePattern: 可选的文件模式过滤器（默认: '*'）
 *             </ul>
 *         <li>返回: 包含上下文搜索结果的格式化字符串
 *       </ul>
 * </ul>
 *
 * <p>搜索结果包括:
 *
 * <ul>
 *   <li>相对文件路径
 *   <li>每个匹配前后 2 行上下文
 *   <li>使用管道字符格式化以便阅读的匹配
 * </ul>
 *
 * <p>使用示例:
 *
 * <pre>{@code
 * String results = ripgrepService.regexSearchFiles(
 *     "/path/to/cwd",
 *     "/path/to/search",
 *     "TODO:",
 *     "*.ts"
 * );
 * }</pre>
 *
 * <p>输出格式示例:
 *
 * <pre>
 * rel/path/to/app.ts
 * │----
 * │function processData(data: any) {
 * │  // Some processing logic here
 * │  // TODO: Implement error handling
 * │  return processedData;
 * │}
 * │----
 * </pre>
 *
 * @author hhoa
 */
@Slf4j
public class RipgrepService {

    /** 搜索结果数据类 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchResult {
        /** 文件路径 */
        private String filePath;

        /** 行号 */
        private Integer line;

        /** 列号 */
        private Integer column;

        /** 匹配的内容 */
        private String match;

        /** 匹配前的上下文 */
        private List<String> beforeContext;

        /** 匹配后的上下文 */
        private List<String> afterContext;
    }

    /** 最大结果数量 */
    private static final int MAX_RESULTS = 300;

    /** 最大输出大小（MB） */
    private static final double MAX_RIPGREP_MB = 0.25;

    /** 最大字节大小 */
    private static final int MAX_BYTE_SIZE = (int) (MAX_RIPGREP_MB * 1024 * 1024);

    /**
     * 获取二进制文件位置
     *
     * @param name 二进制文件名（如 "rg"）
     * @return 二进制文件的完整路径
     * @throws IOException 如果找不到二进制文件
     */
    private String getBinaryLocation(String name) throws IOException {
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        String binName = isWindows ? name + ".exe" : name;

        // 尝试使用 which/where 命令查找
        String checkCmd = isWindows ? "where" : "which";
        try {
            Process process =
                    new ProcessBuilder(checkCmd, binName).redirectErrorStream(true).start();

            try (BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    process.getInputStream(), StandardCharsets.UTF_8))) {
                String location = reader.readLine();
                if (location != null && !location.trim().isEmpty()) {
                    Path binPath = Paths.get(location.trim());
                    if (Files.exists(binPath)) {
                        return binPath.toAbsolutePath().toString();
                    }
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("Could not find binary " + name + " in PATH");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while searching for binary " + name, e);
        }

        throw new IOException("Could not find binary " + name + " at any known location");
    }

    /**
     * 执行 ripgrep 命令
     *
     * @param args ripgrep 参数
     * @return 命令输出
     * @throws IOException 如果执行失败
     */
    private String execRipgrep(List<String> args) throws IOException {
        String binPath = getBinaryLocation("rg");

        ProcessBuilder pb = new ProcessBuilder(binPath);
        pb.command().addAll(args);
        pb.redirectErrorStream(false);

        Process process = pb.start();

        // 读取标准输出
        StringBuilder output = new StringBuilder();
        int lineCount = 0;
        // 限制 ripgrep 输出行数，因为无法限制结果数量。每个结果最多 5 行。
        int maxLines = MAX_RESULTS * 5;

        try (BufferedReader reader =
                new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (lineCount < maxLines) {
                    output.append(line).append("\n");
                    lineCount++;
                } else {
                    process.destroyForcibly();
                    break;
                }
            }
        }

        // 读取错误输出
        StringBuilder errorOutput = new StringBuilder();
        try (BufferedReader errorReader =
                new BufferedReader(
                        new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = errorReader.readLine()) != null) {
                errorOutput.append(line);
            }
        }

        try {
            int exitCode = process.waitFor();
            if (exitCode != 0 && errorOutput.length() > 0) {
                throw new IOException("ripgrep process error: " + errorOutput);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("ripgrep process interrupted", e);
        }

        return output.toString();
    }

    /**
     * 在文件中执行正则表达式搜索
     *
     * @param cwd 当前工作目录（用于相对路径计算）
     * @param directoryPath 要搜索的目录路径
     * @param regex 正则表达式（Rust 正则语法）
     * @param filePattern 可选的文件模式（如 "*.ts"），默认 "*"
     * @param clineIgnoreController 可选的 ClineIgnore 控制器，用于过滤结果
     * @return 格式化的搜索结果字符串
     * @throws IOException 如果搜索失败
     */
    public String regexSearchFiles(
            String cwd,
            String directoryPath,
            String regex,
            String filePattern,
            ClineIgnoreController clineIgnoreController)
            throws IOException {

        List<String> args = new ArrayList<>();
        args.add("--json");
        args.add("-e");
        args.add(regex);
        args.add("--glob");
        args.add(filePattern != null ? filePattern : "*");
        args.add("--context");
        args.add("1"); // 1 行上下文
        args.add(directoryPath);

        String output;
        try {
            output = execRipgrep(args);
        } catch (IOException e) {
            throw new IOException("Error calling ripgrep: " + e.getMessage(), e);
        }

        List<SearchResult> results = new ArrayList<>();
        SearchResult currentResult = null;

        // 解析 JSON 输出
        for (String line : output.split("\n")) {
            if (line.trim().isEmpty()) {
                continue;
            }

            try {
                // 使用简单的 JSON 解析（生产环境建议使用 Jackson 或 Gson）
                Map<String, Object> parsed = parseJsonLine(line);

                String type = (String) parsed.get("type");
                if ("match".equals(type)) {
                    if (currentResult != null) {
                        results.add(currentResult);
                    }

                    @SuppressWarnings("unchecked")
                    Map<String, Object> data = (Map<String, Object>) parsed.get("data");
                    @SuppressWarnings("unchecked")
                    Map<String, Object> pathData = (Map<String, Object>) data.get("path");
                    String pathText = (String) pathData.get("text");
                    Integer lineNumber = (Integer) data.get("line_number");
                    @SuppressWarnings("unchecked")
                    Map<String, Object> linesData = (Map<String, Object>) data.get("lines");
                    String linesText = (String) linesData.get("text");
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> submatches =
                            (List<Map<String, Object>>) data.get("submatches");
                    Integer column =
                            submatches != null && !submatches.isEmpty()
                                    ? (Integer) submatches.get(0).get("start")
                                    : 0;

                    currentResult =
                            SearchResult.builder()
                                    .filePath(pathText)
                                    .line(lineNumber)
                                    .column(column)
                                    .match(linesText)
                                    .beforeContext(new ArrayList<>())
                                    .afterContext(new ArrayList<>())
                                    .build();
                } else if ("context".equals(type) && currentResult != null) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> data = (Map<String, Object>) parsed.get("data");
                    Integer lineNumber = (Integer) data.get("line_number");
                    @SuppressWarnings("unchecked")
                    Map<String, Object> linesData = (Map<String, Object>) data.get("lines");
                    String linesText = (String) linesData.get("text");

                    if (lineNumber < currentResult.getLine()) {
                        currentResult.getBeforeContext().add(linesText);
                    } else {
                        currentResult.getAfterContext().add(linesText);
                    }
                }
            } catch (Exception e) {
                // 忽略解析错误，继续处理下一行
                log.error("Error parsing ripgrep output: " + e.getMessage());
            }
        }

        if (currentResult != null) {
            results.add(currentResult);
        }

        // 使用 ClineIgnoreController 过滤结果（如果提供）
        List<SearchResult> filteredResults = results;
        if (clineIgnoreController != null) {
            filteredResults =
                    results.stream()
                            .filter(
                                    result ->
                                            clineIgnoreController.validateAccess(
                                                    result.getFilePath()))
                            .collect(Collectors.toList());
        }

        return formatResults(filteredResults, cwd);
    }

    /** 简化版 JSON 解析（仅用于 ripgrep 的 JSON 输出） 生产环境建议使用 Jackson 或 Gson 等成熟的 JSON 库 */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonLine(String line) {
        // 这是一个简化实现，实际应该使用 Jackson ObjectMapper
        // 为了不增加依赖，这里使用简单的字符串解析
        // TODO: 使用 Jackson ObjectMapper 进行正确的 JSON 解析
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(line, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JSON line: " + line, e);
        }
    }

    /**
     * 格式化搜索结果
     *
     * @param results 搜索结果列表
     * @param cwd 当前工作目录
     * @return 格式化的字符串
     */
    private String formatResults(List<SearchResult> results, String cwd) {
        Map<String, List<SearchResult>> groupedResults = new LinkedHashMap<>();

        StringBuilder output = new StringBuilder();
        if (results.size() >= MAX_RESULTS) {
            output.append(
                    String.format(
                            "Showing first %d of %d+ results. Use a more specific search if necessary.\n\n",
                            MAX_RESULTS, MAX_RESULTS));
        } else {
            int count = results.size();
            output.append(
                    String.format(
                            "Found %s.\n\n",
                            count == 1 ? "1 result" : String.format("%d results", count)));
        }

        List<SearchResult> limitedResults =
                results.stream().limit(MAX_RESULTS).collect(Collectors.toList());

        for (SearchResult result : limitedResults) {
            Path resultPath = Paths.get(result.getFilePath());
            Path cwdPath = Paths.get(cwd);
            String relativeFilePath = cwdPath.relativize(resultPath).toString().replace("\\", "/");

            groupedResults.computeIfAbsent(relativeFilePath, k -> new ArrayList<>()).add(result);
        }

        int byteSize = output.toString().getBytes(StandardCharsets.UTF_8).length;
        boolean wasLimitReached = false;

        for (Map.Entry<String, List<SearchResult>> entry : groupedResults.entrySet()) {
            String filePath = entry.getKey();
            List<SearchResult> fileResults = entry.getValue();

            String filePathString = filePath + "\n│----\n";
            int filePathBytes = filePathString.getBytes(StandardCharsets.UTF_8).length;

            if (byteSize + filePathBytes >= MAX_BYTE_SIZE) {
                wasLimitReached = true;
                break;
            }

            output.append(filePathString);
            byteSize += filePathBytes;

            for (int resultIndex = 0; resultIndex < fileResults.size(); resultIndex++) {
                SearchResult result = fileResults.get(resultIndex);
                List<String> allLines = new ArrayList<>(result.getBeforeContext());
                allLines.add(result.getMatch());
                allLines.addAll(result.getAfterContext());

                int resultBytes = 0;
                List<String> resultLines = new ArrayList<>();

                for (String line : allLines) {
                    String trimmedLine = (line != null ? line : "").trim();
                    while (trimmedLine.endsWith(" ") || trimmedLine.endsWith("\t")) {
                        trimmedLine = trimmedLine.substring(0, trimmedLine.length() - 1);
                    }
                    String lineString = "│" + trimmedLine + "\n";
                    int lineBytes = lineString.getBytes(StandardCharsets.UTF_8).length;

                    if (byteSize + resultBytes + lineBytes >= MAX_BYTE_SIZE) {
                        wasLimitReached = true;
                        break;
                    }

                    resultLines.add(lineString);
                    resultBytes += lineBytes;
                }

                if (wasLimitReached) {
                    break;
                }

                for (String line : resultLines) {
                    output.append(line);
                }
                byteSize += resultBytes;

                if (resultIndex < fileResults.size() - 1) {
                    String separatorString = "│----\n";
                    int separatorBytes = separatorString.getBytes(StandardCharsets.UTF_8).length;

                    if (byteSize + separatorBytes >= MAX_BYTE_SIZE) {
                        wasLimitReached = true;
                        break;
                    }

                    output.append(separatorString);
                    byteSize += separatorBytes;
                }

                if (byteSize >= MAX_BYTE_SIZE) {
                    wasLimitReached = true;
                    break;
                }
            }

            if (wasLimitReached) {
                break;
            }

            String closingString = "│----\n\n";
            int closingBytes = closingString.getBytes(StandardCharsets.UTF_8).length;

            if (byteSize + closingBytes >= MAX_BYTE_SIZE) {
                wasLimitReached = true;
                break;
            }

            output.append(closingString);
            byteSize += closingBytes;
        }

        if (wasLimitReached) {
            String truncationMessage =
                    String.format(
                            "\n[Results truncated due to exceeding the %.2fMB size limit. Please use a more specific search pattern.]",
                            MAX_RIPGREP_MB);
            int messageBytes = truncationMessage.getBytes(StandardCharsets.UTF_8).length;
            if (byteSize + messageBytes < MAX_BYTE_SIZE) {
                output.append(truncationMessage);
            }
        }

        return output.toString().trim();
    }
}
