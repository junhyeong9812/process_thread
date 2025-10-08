package com.study.common.monitor;

import com.study.common.util.SystemInfo;
import com.study.common.util.TimeUtils;

import java.lang.management.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * 메트릭 수집기
 * 다양한 성능 메트릭을 수집하고 집계합니다.
 */
public class MetricsCollector {

    private final String name;
    private final Map<String, MetricData> metrics;
    private final long startTime;

    // MXBeans
    private static final OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
    private static final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    private static final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
    private static final RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();

    /**
     * 생성자
     */
    public MetricsCollector(String name) {
        this.name = name;
        this.metrics = new ConcurrentHashMap<>();
        this.startTime = System.currentTimeMillis();
    }

    /**
     * 메트릭 데이터 클래스
     */
    public static class MetricData {
        private final String name;
        private final LongAdder count;
        private final LongAdder sum;
        private final AtomicLong min;
        private final AtomicLong max;
        private final List<Long> samples;
        private final int maxSamples;

        public MetricData(String name, int maxSamples) {
            this.name = name;
            this.count = new LongAdder();
            this.sum = new LongAdder();
            this.min = new AtomicLong(Long.MAX_VALUE);
            this.max = new AtomicLong(Long.MIN_VALUE);
            this.samples = Collections.synchronizedList(new ArrayList<>());
            this.maxSamples = maxSamples;
        }

        public void record(long value) {
            count.increment();
            sum.add(value);

            // Min/Max 업데이트
            min.updateAndGet(current -> Math.min(current, value));
            max.updateAndGet(current -> Math.max(current, value));

            // 샘플 저장 (최대 개수 제한)
            synchronized (samples) {
                if (samples.size() >= maxSamples) {
                    samples.remove(0);
                }
                samples.add(value);
            }
        }

        public long getCount() {
            return count.sum();
        }

        public long getSum() {
            return sum.sum();
        }

        public long getMin() {
            long minVal = min.get();
            return minVal == Long.MAX_VALUE ? 0 : minVal;
        }

        public long getMax() {
            long maxVal = max.get();
            return maxVal == Long.MIN_VALUE ? 0 : maxVal;
        }

        public double getAverage() {
            long cnt = getCount();
            return cnt == 0 ? 0 : (double) getSum() / cnt;
        }

        public List<Long> getSamples() {
            synchronized (samples) {
                return new ArrayList<>(samples);
            }
        }

        public void reset() {
            count.reset();
            sum.reset();
            min.set(Long.MAX_VALUE);
            max.set(Long.MIN_VALUE);
            synchronized (samples) {
                samples.clear();
            }
        }

        @Override
        public String toString() {
            return String.format("MetricData{name='%s', count=%d, avg=%.2f, min=%d, max=%d}",
                    name, getCount(), getAverage(), getMin(), getMax());
        }
    }

    /**
     * 메트릭 기록
     */
    public void record(String metricName, long value) {
        metrics.computeIfAbsent(metricName, k -> new MetricData(k, 1000))
                .record(value);
    }

    /**
     * 시간 측정 메트릭 기록 (나노초)
     */
    public void recordTiming(String metricName, long nanos) {
        record(metricName, nanos);
    }

    /**
     * 카운터 증가
     */
    public void increment(String metricName) {
        record(metricName, 1);
    }

    /**
     * 카운터 증가 (n만큼)
     */
    public void increment(String metricName, long n) {
        record(metricName, n);
    }

    /**
     * 메트릭 조회
     */
    public MetricData getMetric(String metricName) {
        return metrics.get(metricName);
    }

    /**
     * 모든 메트릭 조회
     */
    public Map<String, MetricData> getAllMetrics() {
        return new HashMap<>(metrics);
    }

    /**
     * 메트릭 이름 목록 조회
     */
    public Set<String> getMetricNames() {
        return new HashSet<>(metrics.keySet());
    }

    /**
     * 특정 메트릭 초기화
     */
    public void resetMetric(String metricName) {
        MetricData metric = metrics.get(metricName);
        if (metric != null) {
            metric.reset();
        }
    }

    /**
     * 모든 메트릭 초기화
     */
    public void resetAll() {
        metrics.values().forEach(MetricData::reset);
    }

    /**
     * 메트릭 제거
     */
    public void removeMetric(String metricName) {
        metrics.remove(metricName);
    }

    /**
     * 모든 메트릭 제거
     */
    public void clear() {
        metrics.clear();
    }

    // ========== 시스템 메트릭 수집 ==========

    /**
     * CPU 사용률 수집
     */
    public void collectCpuMetrics() {
        double loadAverage = osBean.getSystemLoadAverage();
        if (loadAverage >= 0) {
            record("cpu.load.average", (long) (loadAverage * 100));
        }

        if (osBean instanceof com.sun.management.OperatingSystemMXBean sunOsBean) {
            double processCpuLoad = sunOsBean.getProcessCpuLoad();
            double systemCpuLoad = sunOsBean.getCpuLoad();

            if (processCpuLoad >= 0) {
                record("cpu.process.usage", (long) (processCpuLoad * 100));
            }
            if (systemCpuLoad >= 0) {
                record("cpu.system.usage", (long) (systemCpuLoad * 100));
            }
        }
    }

    /**
     * 메모리 메트릭 수집
     */
    public void collectMemoryMetrics() {
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        record("memory.heap.used", heapUsage.getUsed());
        record("memory.heap.committed", heapUsage.getCommitted());
        record("memory.heap.max", heapUsage.getMax());

        MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();
        record("memory.nonheap.used", nonHeapUsage.getUsed());
        record("memory.nonheap.committed", nonHeapUsage.getCommitted());

        Runtime runtime = Runtime.getRuntime();
        record("memory.runtime.free", runtime.freeMemory());
        record("memory.runtime.total", runtime.totalMemory());
        record("memory.runtime.max", runtime.maxMemory());
    }

    /**
     * 스레드 메트릭 수집
     */
    public void collectThreadMetrics() {
        record("thread.count", threadBean.getThreadCount());
        record("thread.peak", threadBean.getPeakThreadCount());
        record("thread.daemon", threadBean.getDaemonThreadCount());
        record("thread.started", threadBean.getTotalStartedThreadCount());
    }

    /**
     * GC 메트릭 수집
     */
    public void collectGcMetrics() {
        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();

        long totalGcCount = 0;
        long totalGcTime = 0;

        for (GarbageCollectorMXBean gcBean : gcBeans) {
            totalGcCount += gcBean.getCollectionCount();
            totalGcTime += gcBean.getCollectionTime();
        }

        record("gc.count", totalGcCount);
        record("gc.time", totalGcTime);
    }

    /**
     * 클래스 로딩 메트릭 수집
     */
    public void collectClassLoadingMetrics() {
        ClassLoadingMXBean classBean = ManagementFactory.getClassLoadingMXBean();
        record("class.loaded", classBean.getLoadedClassCount());
        record("class.total", classBean.getTotalLoadedClassCount());
        record("class.unloaded", classBean.getUnloadedClassCount());
    }

    /**
     * 모든 시스템 메트릭 수집
     */
    public void collectAllSystemMetrics() {
        collectCpuMetrics();
        collectMemoryMetrics();
        collectThreadMetrics();
        collectGcMetrics();
        collectClassLoadingMetrics();
    }

    // ========== 보고서 생성 ==========

    /**
     * 메트릭 요약 문자열 생성
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("=== %s Metrics Summary ===%n", name));
        sb.append(String.format("Collection time: %.2f seconds%n%n",
                (System.currentTimeMillis() - startTime) / 1000.0));

        if (metrics.isEmpty()) {
            sb.append("No metrics collected%n");
            return sb.toString();
        }

        List<String> sortedNames = new ArrayList<>(metrics.keySet());
        Collections.sort(sortedNames);

        for (String metricName : sortedNames) {
            MetricData data = metrics.get(metricName);
            sb.append(String.format("%-30s : ", metricName));
            sb.append(String.format("count=%d, avg=%.2f, min=%d, max=%d%n",
                    data.getCount(), data.getAverage(), data.getMin(), data.getMax()));
        }

        return sb.toString();
    }

    /**
     * 상세 보고서 생성
     */
    public String getDetailedReport() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("=== %s Detailed Metrics Report ===%n%n", name));
        sb.append(String.format("Started: %s%n", new Date(startTime)));
        sb.append(String.format("Duration: %.2f seconds%n%n",
                (System.currentTimeMillis() - startTime) / 1000.0));

        if (metrics.isEmpty()) {
            sb.append("No metrics collected%n");
            return sb.toString();
        }

        // 카테고리별 그룹화
        Map<String, List<String>> categories = new TreeMap<>();
        for (String metricName : metrics.keySet()) {
            String category = metricName.contains(".")
                    ? metricName.substring(0, metricName.indexOf("."))
                    : "other";
            categories.computeIfAbsent(category, k -> new ArrayList<>()).add(metricName);
        }

        // 카테고리별 출력
        for (Map.Entry<String, List<String>> entry : categories.entrySet()) {
            sb.append(String.format("[%s]%n", entry.getKey().toUpperCase()));

            for (String metricName : entry.getValue()) {
                MetricData data = metrics.get(metricName);
                sb.append(String.format("  %-28s : ", metricName));

                if (metricName.contains("time") || metricName.contains("duration")) {
                    // 시간 메트릭은 포맷팅
                    sb.append(String.format("avg=%s, min=%s, max=%s (count=%d)%n",
                            TimeUtils.formatNanos((long) data.getAverage()),
                            TimeUtils.formatNanos(data.getMin()),
                            TimeUtils.formatNanos(data.getMax()),
                            data.getCount()));
                } else if (metricName.contains("memory")) {
                    // 메모리 메트릭은 MB로
                    sb.append(String.format("avg=%.2f MB, min=%.2f MB, max=%.2f MB%n",
                            data.getAverage() / 1024.0 / 1024.0,
                            data.getMin() / 1024.0 / 1024.0,
                            data.getMax() / 1024.0 / 1024.0));
                } else {
                    // 일반 메트릭
                    sb.append(String.format("avg=%.2f, min=%d, max=%d (count=%d)%n",
                            data.getAverage(), data.getMin(), data.getMax(), data.getCount()));
                }
            }
            sb.append("%n");
        }

        return sb.toString();
    }

    /**
     * 특정 메트릭의 샘플 데이터 조회
     */
    public List<Long> getSamples(String metricName) {
        MetricData metric = metrics.get(metricName);
        return metric != null ? metric.getSamples() : Collections.emptyList();
    }

    /**
     * 테스트 메인 메서드
     */
    public static void main(String[] args) {
        System.out.println("=== MetricsCollector 테스트 ===\n");

        MetricsCollector collector = new MetricsCollector("TestCollector");

        try {
            // 1. 커스텀 메트릭 기록
            System.out.println("1. 커스텀 메트릭 기록:");
            for (int i = 0; i < 10; i++) {
                collector.record("request.duration", 100 + i * 10);
                collector.increment("request.count");
            }
            System.out.println(collector.getSummary());

            // 2. 시스템 메트릭 수집
            System.out.println("\n2. 시스템 메트릭 수집:");
            for (int i = 0; i < 5; i++) {
                collector.collectAllSystemMetrics();
                Thread.sleep(500);
            }

            // 3. 요약 보고서
            System.out.println("\n3. 요약 보고서:");
            System.out.println(collector.getSummary());

            // 4. 상세 보고서
            System.out.println("\n4. 상세 보고서:");
            System.out.println(collector.getDetailedReport());

            // 5. 특정 메트릭 조회
            System.out.println("\n5. 특정 메트릭 상세:");
            MetricData requestMetric = collector.getMetric("request.duration");
            if (requestMetric != null) {
                System.out.println("Request Duration:");
                System.out.println("  Count: " + requestMetric.getCount());
                System.out.println("  Average: " + TimeUtils.formatNanos((long) requestMetric.getAverage()));
                System.out.println("  Min: " + TimeUtils.formatNanos(requestMetric.getMin()));
                System.out.println("  Max: " + TimeUtils.formatNanos(requestMetric.getMax()));
                System.out.println("  Samples: " + requestMetric.getSamples());
            }

            // 6. 메트릭 리셋 테스트
            System.out.println("\n6. 메트릭 리셋:");
            System.out.println("리셋 전 카운트: " + collector.getMetric("request.count").getCount());
            collector.resetMetric("request.count");
            System.out.println("리셋 후 카운트: " + collector.getMetric("request.count").getCount());

        } catch (Exception e) {
            System.err.println("오류 발생: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("\n테스트 완료!");
    }
}