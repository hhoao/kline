package com.hhoa.kline.core.core.workspace;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 处理工作区迁移报告的格式化和呈现
 *
 * <p>从 WorkspaceResolver 中分离出来以遵循单一职责原则。 此类纯粹专注于报告生成和格式化。
 */
public class MigrationReporter {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 从使用统计信息生成综合迁移报告
     *
     * @param usageMap 组件名称到其使用统计信息的映射
     * @param traceEnabled 当前是否启用跟踪
     * @return 格式化的迁移报告字符串
     */
    public String generateReport(Map<String, UsageStats> usageMap, boolean traceEnabled) {
        return generateReport(usageMap, traceEnabled, ReportOptions.builder().build());
    }

    /**
     * 从使用统计信息生成综合迁移报告
     *
     * @param usageMap 组件名称到其使用统计信息的映射
     * @param traceEnabled 当前是否启用跟踪
     * @param options 报告生成的可选配置
     * @return 格式化的迁移报告字符串
     */
    public String generateReport(
            Map<String, UsageStats> usageMap, boolean traceEnabled, ReportOptions options) {
        List<Map.Entry<String, UsageStats>> entries =
                prepareEntries(usageMap, options.isSortByUsage());

        StringBuilder report = new StringBuilder();
        report.append(generateHeader(entries.size(), traceEnabled));
        report.append(generateComponentDetails(entries, options));
        report.append(generateSummary(entries));

        if (options.isIncludeHighUsage()) {
            report.append(generateHighUsageSection(entries, options.getHighUsageThreshold()));
        }

        return report.toString();
    }

    /**
     * 生成简化的摘要报告
     *
     * @param entries 使用条目列表
     * @return 简要摘要字符串
     */
    public String generateSummary(List<Map.Entry<String, UsageStats>> entries) {
        int totalCalls = entries.stream().mapToInt(entry -> entry.getValue().getCount()).sum();

        String summary = "\n=== 摘要 ===\n" + "路径解析调用总数: " + totalCalls + "\n";

        return summary;
    }

    private List<Map.Entry<String, UsageStats>> prepareEntries(
            Map<String, UsageStats> usageMap, boolean sortByUsage) {
        List<Map.Entry<String, UsageStats>> entries = new ArrayList<>(usageMap.entrySet());

        if (sortByUsage) {
            entries.sort(
                    (a, b) -> Integer.compare(b.getValue().getCount(), a.getValue().getCount()));
        }

        return entries;
    }

    private String generateHeader(int componentCount, boolean traceEnabled) {
        String header =
                "=== 多根迁移报告 ===\n"
                        + "使用单根的组件总数: "
                        + componentCount
                        + "\n"
                        + "跟踪已启用: "
                        + traceEnabled
                        + "\n\n";
        return header;
    }

    private String generateComponentDetails(
            List<Map.Entry<String, UsageStats>> entries, ReportOptions options) {
        StringBuilder details = new StringBuilder();

        for (Map.Entry<String, UsageStats> entry : entries) {
            String context = entry.getKey();
            UsageStats stats = entry.getValue();

            details.append(context).append(":\n");
            details.append("  调用次数: ").append(stats.getCount()).append("\n");
            details.append("  最后使用: ")
                    .append(stats.getLastUsed().format(ISO_FORMATTER))
                    .append("\n");

            if (options.isIncludeExamples() && !stats.getExamples().isEmpty()) {
                details.append("  示例路径:\n");
                for (String example : stats.getExamples()) {
                    details.append("    - \"").append(example).append("\"\n");
                }
            }
            details.append("\n");
        }

        return details.toString();
    }

    private String generateHighUsageSection(
            List<Map.Entry<String, UsageStats>> entries, int threshold) {
        List<Map.Entry<String, UsageStats>> highUsageComponents =
                entries.stream()
                        .filter(entry -> entry.getValue().getCount() > threshold)
                        .collect(Collectors.toList());

        if (highUsageComponents.isEmpty()) {
            return "";
        }

        StringBuilder section = new StringBuilder();
        section.append("\n=== 高使用率组件 ===\n");
        section.append("(调用次数 >").append(threshold).append(" 的操作)\n");

        for (Map.Entry<String, UsageStats> entry : highUsageComponents) {
            section.append("  - ")
                    .append(entry.getKey())
                    .append(": ")
                    .append(entry.getValue().getCount())
                    .append(" 次调用\n");
        }

        return section.toString();
    }

    /**
     * 生成使用数据的 JSON 表示
     *
     * @param usageMap 组件名称到其使用统计信息的映射
     * @return JSON 字符串表示
     */
    public String generateJsonReport(Map<String, UsageStats> usageMap) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(usageMap);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    /**
     * 生成使用数据的 CSV 表示
     *
     * @param usageMap 组件名称到其使用统计信息的映射
     * @return CSV 字符串表示
     */
    public String generateCsvReport(Map<String, UsageStats> usageMap) {
        StringBuilder csv = new StringBuilder();
        csv.append("组件,调用次数,最后使用,示例路径\n");

        for (Map.Entry<String, UsageStats> entry : usageMap.entrySet()) {
            String context = entry.getKey();
            UsageStats stats = entry.getValue();
            String examples = String.join("; ", stats.getExamples());

            csv.append("\"")
                    .append(context)
                    .append("\",")
                    .append(stats.getCount())
                    .append(",")
                    .append("\"")
                    .append(stats.getLastUsed().format(ISO_FORMATTER))
                    .append("\",")
                    .append("\"")
                    .append(examples)
                    .append("\"\n");
        }

        return csv.toString();
    }
}
