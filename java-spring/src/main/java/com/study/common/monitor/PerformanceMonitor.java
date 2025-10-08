package com.study.common.monitor;

import com.study.common.util.SystemInfo;
import com.study.common.util.TimeUtils;

import java.util.*;
import java.util.concurrent.*;

/**
 * 성능 모니터
 * 시스템 성능을 주기적으로 모니터링하고 기록합니다.
 */
public class PerformanceMonitor extends BaseMonitor {

    private final MetricsCollector metricsCollector;
    private final long monitoringIntervalMillis;
    private final boolean autoCollectSystemMetrics;

    // 스냅샷 저장
    private final List<PerformanceSnapshot> snapshots;
    private final int maxSnapshots;

    // 알림 임계값
    private final Map<String, ThresholdConfig> thresholds;
    private final List<PerformanceAlert> alerts;

    /**
     * 생성자
     */
    public PerformanceMonitor(String name, long monitoringIntervalMillis) {
        this(name, monitoringIntervalMillis, true);
    }

    /**
     * 생성자
     */
    public PerformanceMonitor(String name, long monitoringIntervalMillis, boolean autoCollectSystemMetrics) {
        super(name);
        this.metricsCollector = new MetricsCollector(name);
        this.monitoringIntervalMillis = monitoringIntervalMillis;
        this.autoCollectSystemMetrics = autoCollectSystemMetrics;
        this.snapshots = Collections.synchronizedList(new ArrayList<>());
        this.maxSnapshots = 1000;
        this.thresholds = new ConcurrentHashMap<>();
        this.alerts = Collections.synchronizedList(new ArrayList<>());
    }

    /**
     * 성능 스냅샷 클래스
     */
    public static class PerformanceSnapshot {
        private final long timestamp;
        private final double cpuUsage;
        private final long heapUsed;
        private final long heapMax;
        private final int threadCount;
        private final long gcCount;
        private final long gcTime;
        private final Map<String, Object> customMetrics;

        public PerformanceSnapshot(long timestamp, double cpuUsage, long heapUsed,
                                   long heapMax, int threadCount, long gcCount, long gcTime) {
            this.timestamp = timestamp;
            this.cpuUsage = cpuUsage;
            this.heapUsed = heapUsed;
            this.heapMax = heapMax;
            this.threadCount = threadCount;
            this.gcCount = gcCount;
            this.gcTime = gcTime;
            this.customMetrics = new HashMap<>();
        }

        public long getTimestamp() { return timestamp; }
        public double getCpuUsage() { return cpuUsage; }
        public long getHeapUsed() { return heapUsed; }
        public long getHeapMax() { return heapMax; }
        public int getThreadCount() { return threadCount; }
        public long getGcCount() { return gcCount; }
        public long getGcTime() { return gcTime; }
        public Map<String, Object> getCustomMetrics() { return customMetrics; }

        public double getHeapUsagePercent() {
            return heapMax > 0 ? (double) heapUsed / heapMax * 100.0 : 0;
        }

        @Override
        public String toString() {
            return String.format("Snapshot{time=%d, cpu=%.1f%%, heap=%d/%d MB, threads=%d}",
                    timestamp, cpuUsage, heapUsed / 1024 / 1024, heapMax / 1024 / 1024, threadCount);
        }
    }

    /**
     * 임계값 설정 클래스
     */
    public static class ThresholdConfig {
        private final String metricName;
        private final double warningThreshold;
        private final double criticalThreshold;

        public ThresholdConfig(String metricName, double warningThreshold, double criticalThreshold) {
            this.metricName = metricName;
            this.warningThreshold = warningThreshold;
            this.criticalThreshold = criticalThreshold;
        }

        public String getMetricName() { return metricName; }
        public double getWarningThreshold() { return warningThreshold; }
        public double getCriticalThreshold() { return criticalThreshold; }
    }

    /**
     * 성능 알림 클래스
     */
    public static class PerformanceAlert {
        public enum Level { WARNING, CRITICAL }

        private final long timestamp;
        private final String metricName;
        private final double value;
        private final double threshold;
        private final Level level;

        public PerformanceAlert(String metricName, double value, double threshold, Level level) {
            this.timestamp = System.currentTimeMillis();
            this.metricName = metricName;
            this.value = value;
            this.threshold = threshold;
            this.level = level;
        }

        public long getTimestamp() { return timestamp; }
        public String getMetricName() { return metricName; }
        public double getValue() { return value; }
        public double getThreshold() { return threshold; }
        public Level getLevel() { return level; }

        @Override
        public String toString() {
            return String.format("[%s] %s: %.2f (threshold: %.2f) at %s",
                    level, metricName, value, threshold, new Date(timestamp));
        }
    }

    @Override
    protected void onStart() {
        log("Performance monitoring started");
        snapshots.clear();
        alerts.clear();
        metricsCollector.resetAll();
        startPeriodicMonitoring(monitoringIntervalMillis);
    }

    @Override
    protected void onStop() {
        log("Performance monitoring stopped");
        logf("Collected %d snapshots", snapshots.size());
        logf("Triggered %d alerts", alerts.size());
    }

    @Override
    protected void collectMetrics() {
        try {
            // 시스템 메트릭 수집
            if (autoCollectSystemMetrics) {
                metricsCollector.collectAllSystemMetrics();
            }

            // 스냅샷 생성
            PerformanceSnapshot snapshot = createSnapshot();

            // 스냅샷 저장
            synchronized (snapshots) {
                if (snapshots.size() >= maxSnapshots) {
                    snapshots.remove(0);
                }
                snapshots.add(snapshot);
            }

            // 임계값 체크
            checkThresholds(snapshot);

        } catch (Exception e) {
            log("Error collecting metrics: " + e.getMessage());
        }
    }

    /**
     * 현재 성능 스냅샷 생성
     */
    private PerformanceSnapshot createSnapshot() {
        long timestamp = System.currentTimeMillis();

        // CPU 사용률
        double cpuUsage = 0;
        MetricsCollector.MetricData cpuMetric = metricsCollector.getMetric("cpu.process.usage");
        if (cpuMetric != null && cpuMetric.getCount() > 0) {
            cpuUsage = cpuMetric.getAverage();
        }

        // 메모리 사용량
        long heapUsed = SystemInfo.getUsedMemory();
        long heapMax = SystemInfo.getMaxMemory();

        // 스레드 수
        int threadCount = (int) SystemInfo.getThreadInfo().get("threadCount");

        // GC 정보
        MetricsCollector.MetricData gcCountMetric = metricsCollector.getMetric("gc.count");
        MetricsCollector.MetricData gcTimeMetric = metricsCollector.getMetric("gc.time");

        long gcCount = gcCountMetric != null ? (long) gcCountMetric.getAverage() : 0;
        long gcTime = gcTimeMetric != null ? (long) gcTimeMetric.getAverage() : 0;

        return new PerformanceSnapshot(timestamp, cpuUsage, heapUsed, heapMax,
                threadCount, gcCount, gcTime);
    }

    /**
     * 임계값 체크
     */
    private void checkThresholds(PerformanceSnapshot snapshot) {
        // CPU 사용률 체크
        checkThreshold("cpu.usage", snapshot.getCpuUsage());

        // 메모리 사용률 체크
        checkThreshold("memory.usage", snapshot.getHeapUsagePercent());

        // 스레드 수 체크
        checkThreshold("thread.count", snapshot.getThreadCount());
    }

    /**
     * 특정 메트릭의 임계값 체크
     */
    private void checkThreshold(String metricName, double value) {
        ThresholdConfig config = thresholds.get(metricName);
        if (config == null) {
            return;
        }

        if (value >= config.getCriticalThreshold()) {
            PerformanceAlert alert = new PerformanceAlert(
                    metricName, value, config.getCriticalThreshold(),
                    PerformanceAlert.Level.CRITICAL
            );
            alerts.add(alert);
            log("CRITICAL: " + alert);
        } else if (value >= config.getWarningThreshold()) {
            PerformanceAlert alert = new PerformanceAlert(
                    metricName, value, config.getWarningThreshold(),
                    PerformanceAlert.Level.WARNING
            );
            alerts.add(alert);
            log("WARNING: " + alert);
        }
    }

    /**
     * 임계값 설정
     */
    public void setThreshold(String metricName, double warningThreshold, double criticalThreshold) {
        thresholds.put(metricName, new ThresholdConfig(metricName, warningThreshold, criticalThreshold));
        logf("Threshold set for %s: warning=%.2f, critical=%.2f",
                metricName, warningThreshold, criticalThreshold);
    }

    /**
     * 커스텀 메트릭 기록
     */
    public void recordMetric(String metricName, long value) {
        metricsCollector.record(metricName, value);
    }

    /**
     * 시간 측정 메트릭 기록
     */
    public void recordTiming(String metricName, long nanos) {
        metricsCollector.recordTiming(metricName, nanos);
    }

    /**
     * 실행 시간 측정 및 기록
     */
    public <T> T measureAndRecord(String metricName, Callable<T> callable) throws Exception {
        long start = System.nanoTime();
        try {
            return callable.call();
        } finally {
            long elapsed = System.nanoTime() - start;
            recordTiming(metricName, elapsed);
        }
    }

    /**
     * 실행 시간 측정 및 기록 (Runnable)
     */
    public void measureAndRecord(String metricName, Runnable runnable) {
        long start = System.nanoTime();
        try {
            runnable.run();
        } finally {
            long elapsed = System.nanoTime() - start;
            recordTiming(metricName, elapsed);
        }
    }

    @Override
    public Object getCollectedData() {
        Map<String, Object> data = new HashMap<>();
        data.put("snapshots", new ArrayList<>(snapshots));
        data.put("metrics", metricsCollector.getAllMetrics());
        data.put("alerts", new ArrayList<>(alerts));
        return data;
    }

    @Override
    public void clearData() {
        snapshots.clear();
        alerts.clear();
        metricsCollector.resetAll();
    }

    /**
     * 스냅샷 목록 조회
     */
    public List<PerformanceSnapshot> getSnapshots() {
        synchronized (snapshots) {
            return new ArrayList<>(snapshots);
        }
    }

    /**
     * 최근 스냅샷 조회
     */
    public PerformanceSnapshot getLatestSnapshot() {
        synchronized (snapshots) {
            return snapshots.isEmpty() ? null : snapshots.get(snapshots.size() - 1);
        }
    }

    /**
     * 알림 목록 조회
     */
    public List<PerformanceAlert> getAlerts() {
        synchronized (alerts) {
            return new ArrayList<>(alerts);
        }
    }

    /**
     * 메트릭 컬렉터 조회
     */
    public MetricsCollector getMetricsCollector() {
        return metricsCollector;
    }

    /**
     * 성능 보고서 생성
     */
    public String generateReport() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("=== %s Performance Report ===%n%n", name));

        // 모니터링 기간
        sb.append(String.format("Monitoring Duration: %.2f seconds%n", getDurationSeconds()));
        sb.append(String.format("Snapshots Collected: %d%n", snapshots.size()));
        sb.append(String.format("Alerts Triggered: %d%n%n", alerts.size()));

        // 최근 스냅샷
        PerformanceSnapshot latest = getLatestSnapshot();
        if (latest != null) {
            sb.append("Current Status:%n");
            sb.append(String.format("  CPU Usage: %.2f%%%n", latest.getCpuUsage()));
            sb.append(String.format("  Heap Usage: %d MB / %d MB (%.2f%%)%n",
                    latest.getHeapUsed() / 1024 / 1024,
                    latest.getHeapMax() / 1024 / 1024,
                    latest.getHeapUsagePercent()));
            sb.append(String.format("  Thread Count: %d%n", latest.getThreadCount()));
            sb.append(String.format("  GC Count: %d, GC Time: %d ms%n%n",
                    latest.getGcCount(), latest.getGcTime()));
        }

        // 알림
        if (!alerts.isEmpty()) {
            sb.append("Recent Alerts:%n");
            synchronized (alerts) {
                int displayCount = Math.min(10, alerts.size());
                for (int i = alerts.size() - displayCount; i < alerts.size(); i++) {
                    sb.append("  ").append(alerts.get(i)).append("%n");
                }
            }
            sb.append("%n");
        }

        // 메트릭 요약
        sb.append(metricsCollector.getSummary());

        return sb.toString();
    }

    /**
     * 테스트 메인 메서드
     */
    public static void main(String[] args) {
        System.out.println("=== PerformanceMonitor 테스트 ===\n");

        // 모니터 생성 (1초 간격)
        PerformanceMonitor monitor = new PerformanceMonitor("TestMonitor", 1000);

        // 임계값 설정
        monitor.setThreshold("cpu.usage", 50.0, 80.0);
        monitor.setThreshold("memory.usage", 70.0, 90.0);

        try {
            // 모니터링 시작
            System.out.println("1. 모니터링 시작");
            monitor.start();

            // 일부 작업 시뮬레이션
            System.out.println("\n2. 작업 시뮬레이션 (5초)...");
            for (int i = 0; i < 5; i++) {
                // 커스텀 메트릭 기록
                monitor.measureAndRecord("task.execution", () -> {
                    // CPU 사용 시뮬레이션
                    long sum = 0;
                    for (int j = 0; j < 10_000_000; j++) {
                        sum += j;
                    }
                    return sum;
                });

                // 메모리 사용 시뮬레이션
                List<byte[]> memoryHog = new ArrayList<>();
                for (int j = 0; j < 10; j++) {
                    memoryHog.add(new byte[1024 * 1024]); // 1MB
                }

                Thread.sleep(1000);
            }

            // 상태 확인
            System.out.println("\n3. 최근 스냅샷:");
            PerformanceSnapshot latest = monitor.getLatestSnapshot();
            if (latest != null) {
                System.out.println(latest);
            }

            // 알림 확인
            System.out.println("\n4. 알림:");
            List<PerformanceAlert> alerts = monitor.getAlerts();
            if (alerts.isEmpty()) {
                System.out.println("알림 없음");
            } else {
                alerts.forEach(System.out::println);
            }

            // 보고서 생성
            System.out.println("\n5. 성능 보고서:");
            System.out.println(monitor.generateReport());

            // 모니터링 중지
            System.out.println("\n6. 모니터링 중지");
            monitor.stop();

        } catch (Exception e) {
            System.err.println("오류 발생: " + e.getMessage());
            e.printStackTrace();
        } finally {
            monitor.cleanup();
        }

        System.out.println("\n테스트 완료!");
    }
}