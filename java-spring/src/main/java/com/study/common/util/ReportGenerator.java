package com.study.common.util;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 보고서 생성 유틸리티 클래스
 * HTML, Markdown, Text 형식의 보고서를 생성합니다.
 */
public class ReportGenerator {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 보고서 형식
     */
    public enum Format {
        HTML, MARKDOWN, TEXT
    }

    /**
     * 보고서 빌더
     */
    public static class ReportBuilder {
        private final StringBuilder content;
        private final Format format;
        private String title;
        private String author;
        private LocalDateTime timestamp;
        private final List<Section> sections;

        public ReportBuilder(Format format) {
            this.format = format;
            this.content = new StringBuilder();
            this.sections = new ArrayList<>();
            this.timestamp = LocalDateTime.now();
        }

        public ReportBuilder title(String title) {
            this.title = title;
            return this;
        }

        public ReportBuilder author(String author) {
            this.author = author;
            return this;
        }

        public ReportBuilder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public ReportBuilder addSection(String sectionTitle, String content) {
            sections.add(new Section(sectionTitle, content));
            return this;
        }

        public ReportBuilder addTable(String sectionTitle, String[] headers, List<String[]> rows) {
            String tableContent = buildTable(headers, rows);
            sections.add(new Section(sectionTitle, tableContent));
            return this;
        }

        public ReportBuilder addKeyValue(String sectionTitle, Map<String, String> data) {
            String kvContent = buildKeyValue(data);
            sections.add(new Section(sectionTitle, kvContent));
            return this;
        }

        public ReportBuilder addList(String sectionTitle, List<String> items) {
            String listContent = buildList(items);
            sections.add(new Section(sectionTitle, listContent));
            return this;
        }

        private String buildTable(String[] headers, List<String[]> rows) {
            StringBuilder sb = new StringBuilder();

            switch (format) {
                case HTML -> {
                    sb.append("<table border='1' style='border-collapse: collapse;'>\n");
                    sb.append("  <thead><tr>");
                    for (String header : headers) {
                        sb.append("<th>").append(header).append("</th>");
                    }
                    sb.append("</tr></thead>\n");
                    sb.append("  <tbody>\n");
                    for (String[] row : rows) {
                        sb.append("    <tr>");
                        for (String cell : row) {
                            sb.append("<td>").append(cell).append("</td>");
                        }
                        sb.append("</tr>\n");
                    }
                    sb.append("  </tbody>\n</table>");
                }
                case MARKDOWN -> {
                    sb.append("| ").append(String.join(" | ", headers)).append(" |\n");
                    sb.append("| ").append("--- | ".repeat(headers.length)).append("\n");
                    for (String[] row : rows) {
                        sb.append("| ").append(String.join(" | ", row)).append(" |\n");
                    }
                }
                case TEXT -> {
                    int[] columnWidths = new int[headers.length];
                    for (int i = 0; i < headers.length; i++) {
                        columnWidths[i] = headers[i].length();
                        for (String[] row : rows) {
                            columnWidths[i] = Math.max(columnWidths[i], row[i].length());
                        }
                    }

                    // 헤더
                    for (int i = 0; i < headers.length; i++) {
                        sb.append(String.format("%-" + columnWidths[i] + "s", headers[i]));
                        sb.append(" | ");
                    }
                    sb.append("\n");

                    // 구분선
                    for (int width : columnWidths) {
                        sb.append("-".repeat(width)).append("-+-");
                    }
                    sb.append("\n");

                    // 데이터
                    for (String[] row : rows) {
                        for (int i = 0; i < row.length; i++) {
                            sb.append(String.format("%-" + columnWidths[i] + "s", row[i]));
                            sb.append(" | ");
                        }
                        sb.append("\n");
                    }
                }
            }

            return sb.toString();
        }

        private String buildKeyValue(Map<String, String> data) {
            StringBuilder sb = new StringBuilder();

            switch (format) {
                case HTML -> {
                    sb.append("<dl>\n");
                    data.forEach((key, value) ->
                            sb.append("  <dt><strong>").append(key).append(":</strong></dt>")
                                    .append("<dd>").append(value).append("</dd>\n"));
                    sb.append("</dl>");
                }
                case MARKDOWN -> {
                    data.forEach((key, value) ->
                            sb.append("- **").append(key).append("**: ").append(value).append("\n"));
                }
                case TEXT -> {
                    int maxKeyLength = data.keySet().stream()
                            .mapToInt(String::length)
                            .max().orElse(0);
                    data.forEach((key, value) ->
                            sb.append(String.format("%-" + maxKeyLength + "s : %s\n", key, value)));
                }
            }

            return sb.toString();
        }

        private String buildList(List<String> items) {
            StringBuilder sb = new StringBuilder();

            switch (format) {
                case HTML -> {
                    sb.append("<ul>\n");
                    items.forEach(item -> sb.append("  <li>").append(item).append("</li>\n"));
                    sb.append("</ul>");
                }
                case MARKDOWN, TEXT -> {
                    items.forEach(item -> sb.append("- ").append(item).append("\n"));
                }
            }

            return sb.toString();
        }

        public String build() {
            StringBuilder report = new StringBuilder();

            switch (format) {
                case HTML -> buildHTML(report);
                case MARKDOWN -> buildMarkdown(report);
                case TEXT -> buildText(report);
            }

            return report.toString();
        }

        private void buildHTML(StringBuilder report) {
            report.append("<!DOCTYPE html>\n<html>\n<head>\n");
            report.append("  <meta charset='UTF-8'>\n");
            report.append("  <title>").append(title != null ? title : "Report").append("</title>\n");
            report.append("  <style>\n");
            report.append("    body { font-family: Arial, sans-serif; margin: 40px; }\n");
            report.append("    h1 { color: #333; }\n");
            report.append("    h2 { color: #666; border-bottom: 2px solid #ddd; padding-bottom: 5px; }\n");
            report.append("    table { margin: 20px 0; }\n");
            report.append("    th { background-color: #f0f0f0; padding: 8px; text-align: left; }\n");
            report.append("    td { padding: 8px; }\n");
            report.append("    .meta { color: #999; font-size: 0.9em; }\n");
            report.append("  </style>\n");
            report.append("</head>\n<body>\n");

            if (title != null) {
                report.append("  <h1>").append(title).append("</h1>\n");
            }

            report.append("  <div class='meta'>\n");
            if (author != null) {
                report.append("    <p>Author: ").append(author).append("</p>\n");
            }
            report.append("    <p>Generated: ").append(timestamp.format(DATE_FORMATTER)).append("</p>\n");
            report.append("  </div>\n");

            for (Section section : sections) {
                report.append("  <h2>").append(section.title).append("</h2>\n");
                report.append("  <div>\n").append(section.content).append("\n  </div>\n");
            }

            report.append("</body>\n</html>");
        }

        private void buildMarkdown(StringBuilder report) {
            if (title != null) {
                report.append("# ").append(title).append("\n\n");
            }

            if (author != null) {
                report.append("**Author:** ").append(author).append("  \n");
            }
            report.append("**Generated:** ").append(timestamp.format(DATE_FORMATTER)).append("\n\n");
            report.append("---\n\n");

            for (Section section : sections) {
                report.append("## ").append(section.title).append("\n\n");
                report.append(section.content).append("\n\n");
            }
        }

        private void buildText(StringBuilder report) {
            if (title != null) {
                report.append("=".repeat(title.length())).append("\n");
                report.append(title).append("\n");
                report.append("=".repeat(title.length())).append("\n\n");
            }

            if (author != null) {
                report.append("Author: ").append(author).append("\n");
            }
            report.append("Generated: ").append(timestamp.format(DATE_FORMATTER)).append("\n\n");
            report.append("-".repeat(60)).append("\n\n");

            for (Section section : sections) {
                report.append(section.title).append("\n");
                report.append("-".repeat(section.title.length())).append("\n\n");
                report.append(section.content).append("\n\n");
            }
        }

        public void saveToFile(String filePath) throws IOException {
            FileUtils.writeFile(filePath, build());
        }
    }

    /**
     * 섹션 클래스
     */
    private static class Section {
        final String title;
        final String content;

        Section(String title, String content) {
            this.title = title;
            this.content = content;
        }
    }

    /**
     * HTML 보고서 생성
     */
    public static ReportBuilder html(String title) {
        return new ReportBuilder(Format.HTML).title(title);
    }

    /**
     * Markdown 보고서 생성
     */
    public static ReportBuilder markdown(String title) {
        return new ReportBuilder(Format.MARKDOWN).title(title);
    }

    /**
     * Text 보고서 생성
     */
    public static ReportBuilder text(String title) {
        return new ReportBuilder(Format.TEXT).title(title);
    }

    /**
     * 성능 보고서 생성 (벤치마크 결과용)
     */
    public static String generatePerformanceReport(
            String title,
            Map<String, Long> metrics,
            Format format) {

        ReportBuilder builder = new ReportBuilder(format)
                .title(title)
                .author("Process-Thread Study");

        // 메트릭을 문자열로 변환
        Map<String, String> metricsStr = new LinkedHashMap<>();
        metrics.forEach((key, value) ->
                metricsStr.put(key, TimeUtils.formatNanos(value)));

        builder.addKeyValue("Performance Metrics", metricsStr);

        return builder.build();
    }

    /**
     * 비교 보고서 생성
     */
    public static String generateComparisonReport(
            String title,
            String[] headers,
            List<String[]> data,
            Format format) {

        return new ReportBuilder(format)
                .title(title)
                .author("Process-Thread Study")
                .addTable("Comparison Results", headers, data)
                .build();
    }

    /**
     * 시스템 정보 보고서 생성
     */
    public static String generateSystemReport(Format format) {
        ReportBuilder builder = new ReportBuilder(format)
                .title("System Information Report")
                .author("Process-Thread Study");

        // OS 정보
        Map<String, String> osInfo = SystemInfo.getOSInfo();
        builder.addKeyValue("Operating System", osInfo);

        // JVM 정보
        Map<String, String> jvmInfo = SystemInfo.getJVMInfo();
        builder.addKeyValue("JVM Information", jvmInfo);

        // 하드웨어 정보
        Map<String, Object> hwInfo = SystemInfo.getHardwareInfo();
        Map<String, String> hwInfoStr = new LinkedHashMap<>();
        hwInfo.forEach((key, value) -> {
            if (value instanceof Long) {
                hwInfoStr.put(key, FileUtils.formatFileSize((Long) value));
            } else {
                hwInfoStr.put(key, String.valueOf(value));
            }
        });
        builder.addKeyValue("Hardware", hwInfoStr);

        // 메모리 정보
        Map<String, String> memInfo = new LinkedHashMap<>();
        memInfo.put("Used Memory", FileUtils.formatFileSize(SystemInfo.getUsedMemory()));
        memInfo.put("Free Memory", FileUtils.formatFileSize(SystemInfo.getFreeMemory()));
        memInfo.put("Total Memory", FileUtils.formatFileSize(SystemInfo.getTotalMemory()));
        memInfo.put("Max Memory", FileUtils.formatFileSize(SystemInfo.getMaxMemory()));
        memInfo.put("Usage", String.format("%.2f%%", SystemInfo.getMemoryUsagePercentage()));
        builder.addKeyValue("Memory", memInfo);

        return builder.build();
    }

    /**
     * 테스트 메인 메서드
     */
    public static void main(String[] args) {
        System.out.println("=== ReportGenerator 테스트 ===\n");

        try {
            // 1. HTML 보고서
            System.out.println("1. HTML 보고서 생성:");
            String htmlReport = html("Performance Test Report")
                    .author("Test User")
                    .addSection("Introduction", "This is a test report for demonstrating ReportGenerator.")
                    .addKeyValue("Test Parameters", Map.of(
                            "Iterations", "1000",
                            "Threads", "4",
                            "Duration", "10 seconds"
                    ))
                    .addTable("Results",
                            new String[]{"Metric", "Value", "Unit"},
                            List.of(
                                    new String[]{"Throughput", "1500", "ops/sec"},
                                    new String[]{"Latency", "2.5", "ms"},
                                    new String[]{"Memory", "256", "MB"}
                            ))
                    .addList("Conclusions", List.of(
                            "Performance is acceptable",
                            "Memory usage is within limits",
                            "No bottlenecks detected"
                    ))
                    .build();

            FileUtils.writeFile("report.html", htmlReport);
            System.out.println("HTML 보고서 저장: report.html");

            // 2. Markdown 보고서
            System.out.println("\n2. Markdown 보고서 생성:");
            String mdReport = markdown("Benchmark Results")
                    .author("Benchmark System")
                    .addTable("Comparison",
                            new String[]{"Method", "Average", "Min", "Max"},
                            List.of(
                                    new String[]{"Process", "150 ms", "100 ms", "200 ms"},
                                    new String[]{"Thread", "50 ms", "40 ms", "60 ms"},
                                    new String[]{"Virtual Thread", "30 ms", "25 ms", "35 ms"}
                            ))
                    .build();

            FileUtils.writeFile("report.md", mdReport);
            System.out.println("Markdown 보고서 저장: report.md");
            System.out.println("\nMarkdown 미리보기:");
            System.out.println(mdReport);

            // 3. Text 보고서
            System.out.println("\n3. Text 보고서 생성:");
            String textReport = text("System Status Report")
                    .addKeyValue("Current Status", Map.of(
                            "CPU Usage", "45%",
                            "Memory Usage", "60%",
                            "Active Threads", "12"
                    ))
                    .build();

            System.out.println(textReport);

            // 4. 성능 보고서
            System.out.println("\n4. 성능 보고서:");
            Map<String, Long> metrics = new LinkedHashMap<>();
            metrics.put("Total Time", 1_500_000_000L);
            metrics.put("Average Time", 1_500_000L);
            metrics.put("Min Time", 500_000L);
            metrics.put("Max Time", 5_000_000L);

            String perfReport = generatePerformanceReport(
                    "Process vs Thread Performance",
                    metrics,
                    Format.TEXT
            );
            System.out.println(perfReport);

            // 5. 시스템 정보 보고서
            System.out.println("\n5. 시스템 정보 보고서:");
            String sysReport = generateSystemReport(Format.TEXT);
            System.out.println(sysReport);

            System.out.println("\n모든 보고서 생성 완료!");

        } catch (IOException e) {
            System.err.println("오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }
}