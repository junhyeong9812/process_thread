package com.study.process.monitoring;

import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * 프로세스 메트릭 수집 및 분석
 * 다양한 메트릭을 수집하고 분석하여 리포트 생성
 */
public class ProcessMetrics {

    private final Map<Long, ProcessMetricData> metricsData;
    private final ScheduledExecutorService scheduler;
    private final List<MetricCollector> collectors;
    private final long collectionInterval;
    private final TimeUnit collectionUnit;
    private volatile boolean collecting;

    /**
     * 프로세스 메트릭 데이터
     */
    public static class ProcessMetricData {
        private final long pid;
        private final String processName;
        private final Map<String, TimeSeries> metrics;
        private final Instant startTime;
        private final Map<String, Object> metadata;

        public ProcessMetricData(long pid, String processName) {
            this.pid = pid;
            this.processName = processName;
            this.metrics = new ConcurrentHashMap<>();
            this.startTime = Instant.now();
            this.metadata = new ConcurrentHashMap<>();
        }

        public void addMetric(String name, double value) {
            metrics.computeIfAbsent(name, k -> new TimeSeries(name))
                    .addValue(value);
        }

        public void addMetric(String name, double value, Instant timestamp) {
            metrics.computeIfAbsent(name, k -> new TimeSeries(name))
                    .addValue(value, timestamp);
        }

        public TimeSeries getMetric(String name) {
            return metrics.get(name);
        }

        public Map<String, TimeSeries> getAllMetrics() {
            return new HashMap<>(metrics);
        }

        public MetricSummary getSummary(String metricName) {
            TimeSeries series = metrics.get(metricName);
            return series != null ? series.getSummary() : null;
        }

        public void setMetadata(String key, Object value) {
            metadata.put(key, value);
        }

        public Object getMetadata(String key) {
            return metadata.get(key);
        }

        // Getters
        public long getPid() { return pid; }
        public String getProcessName() { return processName; }
        public Instant getStartTime() { return startTime; }
    }

    /**
     * 시계열 데이터
     */
    public static class TimeSeries {
        private final String name;
        private final Deque<DataPoint> dataPoints;
        private final int maxPoints;

        public TimeSeries(String name) {
            this(name, 1000);
        }

        public TimeSeries(String name, int maxPoints) {
            this.name = name;
            this.dataPoints = new LinkedList<>();
            this.maxPoints = maxPoints;
        }

        public void addValue(double value) {
            addValue(value, Instant.now());
        }

        public void addValue(double value, Instant timestamp) {
            dataPoints.offer(new DataPoint(timestamp, value));
            if (dataPoints.size() > maxPoints) {
                dataPoints.poll();
            }
        }

        public List<DataPoint> getDataPoints() {
            return new ArrayList<>(dataPoints);
        }

        public List<DataPoint> getDataPoints(Instant from, Instant to) {
            return dataPoints.stream()
                    .filter(dp -> !dp.timestamp.isBefore(from) && !dp.timestamp.isAfter(to))
                    .collect(Collectors.toList());
        }

        public DataPoint getLatest() {
            return dataPoints.peekLast();
        }

        public MetricSummary getSummary() {
            if (dataPoints.isEmpty()) {
                return new MetricSummary(name, 0, 0, 0, 0, 0, 0, 0);
            }

            DoubleSummaryStatistics stats = dataPoints.stream()
                    .mapToDouble(DataPoint::getValue)
                    .summaryStatistics();

            List<Double> sorted = dataPoints.stream()
                    .map(DataPoint::getValue)
                    .sorted()
                    .collect(Collectors.toList());

            double median = getPercentile(sorted, 50);
            double p95 = getPercentile(sorted, 95);
            double p99 = getPercentile(sorted, 99);
            double stdDev = calculateStandardDeviation(sorted, stats.getAverage());

            return new MetricSummary(
                    name,
                    stats.getMin(),
                    stats.getMax(),
                    stats.getAverage(),
                    median,
                    p95,
                    p99,
                    stdDev
            );
        }

        private double getPercentile(List<Double> sorted, int percentile) {
            if (sorted.isEmpty()) return 0;
            int index = (int) Math.ceil(percentile / 100.0 * sorted.size()) - 1;
            return sorted.get(Math.max(0, Math.min(index, sorted.size() - 1)));
        }

        private double calculateStandardDeviation(List<Double> values, double mean) {
            double variance = values.stream()
                    .mapToDouble(v -> Math.pow(v - mean, 2))
                    .average()
                    .orElse(0.0);
            return Math.sqrt(variance);
        }

        public String getName() { return name; }
        public int size() { return dataPoints.size(); }
        public boolean isEmpty() { return dataPoints.isEmpty(); }
    }

    /**
     * 데이터 포인트
     */
    public static class DataPoint {
        private final Instant timestamp;
        private final double value;

        public DataPoint(Instant timestamp, double value) {
            this.timestamp = timestamp;
            this.value = value;
        }

        public Instant getTimestamp() { return timestamp; }
        public double getValue() { return value; }

        @Override
        public String toString() {
            return String.format("[%s] %.2f", timestamp, value);
        }
    }

    /**
     * 메트릭 요약
     */
    public static class MetricSummary {
        private final String name;
        private final double min;
        private final double max;
        private final double average;
        private final double median;
        private final double p95;
        private final double p99;
        private final double standardDeviation;

        public MetricSummary(String name, double min, double max, double average,
                             double median, double p95, double p99, double standardDeviation) {
            this.name = name;
            this.min = min;
            this.max = max;
            this.average = average;
            this.median = median;
            this.p95 = p95;
            this.p99 = p99;
            this.standardDeviation = standardDeviation;
        }

        @Override
        public String toString() {
            return String.format("%s: min=%.2f, max=%.2f, avg=%.2f, median=%.2f, " +
                            "p95=%.2f, p99=%.2f, stddev=%.2f",
                    name, min, max, average, median, p95, p99, standardDeviation);
        }

        // Getters
        public String getName() { return name; }
        public double getMin() { return min; }
        public double getMax() { return max; }
        public double getAverage() { return average; }
        public double getMedian() { return median; }
        public double getP95() { return p95; }
        public double getP99() { return p99; }
        public double getStandardDeviation() { return standardDeviation; }
    }

    /**
     * 메트릭 수집기 인터페이스
     */
    public interface MetricCollector {
        Map<String, Double> collect(ProcessHandle handle);
        String getName();
    }

    /**
     * 생성자
     */
    public ProcessMetrics(long collectionInterval, TimeUnit unit) {
        this.metricsData = new ConcurrentHashMap<>();
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.collectors = new ArrayList<>();
        this.collectionInterval = collectionInterval;
        this.collectionUnit = unit;
        this.collecting = false;

        // 기본 수집기 등록
        registerDefaultCollectors();
    }

    /**
     * 기본 메트릭 수집기 등록
     */
    private void registerDefaultCollectors() {
        // CPU 메트릭 수집기
        collectors.add(new MetricCollector() {
            @Override
            public Map<String, Double> collect(ProcessHandle handle) {
                Map<String, Double> metrics = new HashMap<>();

                ProcessHandle.Info info = handle.info();
                info.totalCpuDuration().ifPresent(duration -> {
                    metrics.put("cpu_time", duration.toMillis() / 1000.0);
                });

                // CPU 사용률 계산
                info.startInstant().ifPresent(start -> {
                    Duration uptime = Duration.between(start, Instant.now());
                    if (!uptime.isZero()) {
                        info.totalCpuDuration().ifPresent(cpu -> {
                            double usage = (cpu.toMillis() * 100.0) / uptime.toMillis();
                            metrics.put("cpu_usage_percent", Math.min(usage, 100.0));
                        });
                    }
                });

                return metrics;
            }

            @Override
            public String getName() {
                return "CPU Collector";
            }
        });

        // 메모리 메트릭 수집기 (JVM 프로세스인 경우)
        collectors.add(new MetricCollector() {
            @Override
            public Map<String, Double> collect(ProcessHandle handle) {
                Map<String, Double> metrics = new HashMap<>();

                if (handle.pid() == ProcessHandle.current().pid()) {
                    Runtime runtime = Runtime.getRuntime();
                    metrics.put("heap_used_mb",
                            (runtime.totalMemory() - runtime.freeMemory()) / 1024.0 / 1024.0);
                    metrics.put("heap_total_mb",
                            runtime.totalMemory() / 1024.0 / 1024.0);
                    metrics.put("heap_max_mb",
                            runtime.maxMemory() / 1024.0 / 1024.0);
                }

                return metrics;
            }

            @Override
            public String getName() {
                return "Memory Collector";
            }
        });

        // 스레드 수 수집기
        collectors.add(new MetricCollector() {
            @Override
            public Map<String, Double> collect(ProcessHandle handle) {
                Map<String, Double> metrics = new HashMap<>();

                long threadCount = handle.descendants().count() + 1;
                metrics.put("thread_count", (double) threadCount);

                return metrics;
            }

            @Override
            public String getName() {
                return "Thread Collector";
            }
        });
    }

    /**
     * 수집 시작
     */
    public void startCollection() {
        if (collecting) {
            return;
        }

        collecting = true;
        scheduler.scheduleAtFixedRate(
                this::collectMetrics,
                0, collectionInterval, collectionUnit
        );

        System.out.println("ProcessMetrics collection started");
    }

    /**
     * 수집 중지
     */
    public void stopCollection() {
        collecting = false;
        scheduler.shutdown();

        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }

        System.out.println("ProcessMetrics collection stopped");
    }

    /**
     * 프로세스 추적 시작
     */
    public void trackProcess(long pid, String name) {
        metricsData.computeIfAbsent(pid, k -> new ProcessMetricData(k, name));
        System.out.printf("Tracking metrics for: [%d] %s\n", pid, name);
    }

    /**
     * 프로세스 추적 중지
     */
    public void untrackProcess(long pid) {
        ProcessMetricData removed = metricsData.remove(pid);
        if (removed != null) {
            System.out.printf("Stopped tracking metrics: [%d] %s\n",
                    pid, removed.getProcessName());
        }
    }

    /**
     * 메트릭 수집
     */
    private void collectMetrics() {
        for (Map.Entry<Long, ProcessMetricData> entry : metricsData.entrySet()) {
            long pid = entry.getKey();
            ProcessMetricData data = entry.getValue();

            ProcessHandle.of(pid).ifPresent(handle -> {
                if (handle.isAlive()) {
                    for (MetricCollector collector : collectors) {
                        Map<String, Double> metrics = collector.collect(handle);
                        metrics.forEach(data::addMetric);
                    }
                }
            });
        }
    }

    /**
     * 커스텀 수집기 추가
     */
    public void addCollector(MetricCollector collector) {
        collectors.add(collector);
        System.out.println("Added metric collector: " + collector.getName());
    }

    /**
     * 메트릭 조회
     */
    public ProcessMetricData getMetrics(long pid) {
        return metricsData.get(pid);
    }

    public Map<Long, ProcessMetricData> getAllMetrics() {
        return new HashMap<>(metricsData);
    }

    /**
     * CSV 내보내기
     */
    public void exportToCSV(String filename) throws IOException {
        Path path = Paths.get(filename);

        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(path))) {
            writer.println("PID,Process,Metric,Timestamp,Value");

            for (ProcessMetricData data : metricsData.values()) {
                for (Map.Entry<String, TimeSeries> entry : data.getAllMetrics().entrySet()) {
                    String metricName = entry.getKey();
                    TimeSeries series = entry.getValue();

                    for (DataPoint point : series.getDataPoints()) {
                        writer.printf("%d,%s,%s,%s,%.2f%n",
                                data.getPid(),
                                data.getProcessName(),
                                metricName,
                                point.getTimestamp(),
                                point.getValue());
                    }
                }
            }
        }

        System.out.println("Metrics exported to: " + filename);
    }

    /**
     * JSON 내보내기
     */
    public String exportToJSON() {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"timestamp\": \"").append(Instant.now()).append("\",\n");
        json.append("  \"processes\": [\n");

        List<ProcessMetricData> dataList = new ArrayList<>(metricsData.values());
        for (int i = 0; i < dataList.size(); i++) {
            ProcessMetricData data = dataList.get(i);
            json.append("    {\n");
            json.append("      \"pid\": ").append(data.getPid()).append(",\n");
            json.append("      \"name\": \"").append(data.getProcessName()).append("\",\n");
            json.append("      \"metrics\": {\n");

            Map<String, TimeSeries> metrics = data.getAllMetrics();
            List<String> metricNames = new ArrayList<>(metrics.keySet());

            for (int j = 0; j < metricNames.size(); j++) {
                String metricName = metricNames.get(j);
                TimeSeries series = metrics.get(metricName);
                DataPoint latest = series.getLatest();

                if (latest != null) {
                    json.append("        \"").append(metricName).append("\": ")
                            .append(latest.getValue());

                    if (j < metricNames.size() - 1) {
                        json.append(",");
                    }
                    json.append("\n");
                }
            }

            json.append("      }\n");
            json.append("    }");

            if (i < dataList.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }

        json.append("  ]\n");
        json.append("}");

        return json.toString();
    }

    /**
     * 리포트 생성
     */
    public String generateReport() {
        StringBuilder report = new StringBuilder();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        report.append("=".repeat(60)).append("\n");
        report.append("   PROCESS METRICS REPORT\n");
        report.append("=".repeat(60)).append("\n");
        report.append("Generated: ").append(LocalDateTime.now().format(formatter)).append("\n\n");

        for (ProcessMetricData data : metricsData.values()) {
            report.append("Process: ").append(data.getProcessName())
                    .append(" [PID: ").append(data.getPid()).append("]\n");
            report.append("-".repeat(50)).append("\n");

            for (Map.Entry<String, TimeSeries> entry : data.getAllMetrics().entrySet()) {
                MetricSummary summary = entry.getValue().getSummary();
                report.append("  ").append(summary).append("\n");
            }
            report.append("\n");
        }

        // 전체 통계
        report.append("=== Overall Statistics ===\n");
        MetricsStatistics stats = getStatistics();
        stats.appendTo(report);

        return report.toString();
    }

    /**
     * 통계 정보
     */
    public MetricsStatistics getStatistics() {
        return new MetricsStatistics(metricsData.values());
    }

    public static class MetricsStatistics {
        private final int totalProcesses;
        private final int totalMetrics;
        private final long totalDataPoints;
        private final Map<String, Double> averageValues;

        public MetricsStatistics(Collection<ProcessMetricData> data) {
            this.totalProcesses = data.size();

            Set<String> metricNames = new HashSet<>();
            long points = 0;

            for (ProcessMetricData pd : data) {
                for (Map.Entry<String, TimeSeries> entry : pd.getAllMetrics().entrySet()) {
                    metricNames.add(entry.getKey());
                    points += entry.getValue().size();
                }
            }

            this.totalMetrics = metricNames.size();
            this.totalDataPoints = points;

            // 각 메트릭의 평균 계산
            this.averageValues = new HashMap<>();
            for (String metricName : metricNames) {
                double sum = 0;
                int count = 0;

                for (ProcessMetricData pd : data) {
                    TimeSeries series = pd.getMetric(metricName);
                    if (series != null && !series.isEmpty()) {
                        DataPoint latest = series.getLatest();
                        if (latest != null) {
                            sum += latest.getValue();
                            count++;
                        }
                    }
                }

                if (count > 0) {
                    averageValues.put(metricName, sum / count);
                }
            }
        }

        public void appendTo(StringBuilder builder) {
            builder.append("Tracked Processes: ").append(totalProcesses).append("\n");
            builder.append("Total Metrics: ").append(totalMetrics).append("\n");
            builder.append("Total Data Points: ").append(totalDataPoints).append("\n");

            if (!averageValues.isEmpty()) {
                builder.append("\nAverage Values (Latest):\n");
                averageValues.forEach((metric, value) -> {
                    builder.append("  ").append(metric).append(": ")
                            .append(String.format("%.2f", value)).append("\n");
                });
            }
        }

        // Getters
        public int getTotalProcesses() { return totalProcesses; }
        public int getTotalMetrics() { return totalMetrics; }
        public long getTotalDataPoints() { return totalDataPoints; }
        public Map<String, Double> getAverageValues() { return averageValues; }
    }
}