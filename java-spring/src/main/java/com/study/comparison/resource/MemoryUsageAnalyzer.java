package com.study.comparison.resource;

import com.study.common.util.SystemInfo;
import com.study.common.util.TimeUtils;
import com.study.common.util.FileUtils;
import com.study.common.util.ReportGenerator;

import java.lang.management.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * 메모리 사용량 분석기
 * Heap, Non-Heap, 네이티브 메모리 사용량을 분석합니다.
 */
public class MemoryUsageAnalyzer {

    private static final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    private static final List<MemoryPoolMXBean> memoryPools = ManagementFactory.getMemoryPoolMXBeans();
    private static final List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();

    /**
     * 메모리 스냅샷
     */
    public static class MemorySnapshot {
        private final long timestamp;
        private final long heapUsed;
        private final long heapCommitted;
        private final long heapMax;
        private final long nonHeapUsed;
        private final long nonHeapCommitted;
        private final long totalMemory;
        private final long freeMemory;
        private final long maxMemory;
        private final Map<String, Long> poolUsages;
        private final GCSnapshot gcSnapshot;

        public MemorySnapshot() {
            this.timestamp = System.currentTimeMillis();

            // Heap 메모리
            MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
            this.heapUsed = heapUsage.getUsed();
            this.heapCommitted = heapUsage.getCommitted();
            this.heapMax = heapUsage.getMax();

            // Non-Heap 메모리
            MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();
            this.nonHeapUsed = nonHeapUsage.getUsed();
            this.nonHeapCommitted = nonHeapUsage.getCommitted();

            // Runtime 메모리
            Runtime runtime = Runtime.getRuntime();
            this.totalMemory = runtime.totalMemory();
            this.freeMemory = runtime.freeMemory();
            this.maxMemory = runtime.maxMemory();

            // 메모리 풀별 사용량
            this.poolUsages = new HashMap<>();
            for (MemoryPoolMXBean pool : memoryPools) {
                poolUsages.put(pool.getName(), pool.getUsage().getUsed());
            }

            // GC 정보
            this.gcSnapshot = new GCSnapshot();
        }

        public long getTimestamp() { return timestamp; }
        public long getHeapUsed() { return heapUsed; }
        public long getHeapCommitted() { return heapCommitted; }
        public long getHeapMax() { return heapMax; }
        public long getNonHeapUsed() { return nonHeapUsed; }
        public long getNonHeapCommitted() { return nonHeapCommitted; }
        public long getTotalMemory() { return totalMemory; }
        public long getFreeMemory() { return freeMemory; }
        public long getMaxMemory() { return maxMemory; }
        public long getUsedMemory() { return totalMemory - freeMemory; }
        public Map<String, Long> getPoolUsages() { return new HashMap<>(poolUsages); }
        public GCSnapshot getGcSnapshot() { return gcSnapshot; }

        public double getHeapUsagePercent() {
            return heapMax > 0 ? (double) heapUsed / heapMax * 100.0 : 0;
        }

        public double getTotalUsagePercent() {
            return maxMemory > 0 ? (double) (totalMemory - freeMemory) / maxMemory * 100.0 : 0;
        }

        @Override
        public String toString() {
            return String.format("MemorySnapshot[heap=%s/%s (%.1f%%), total=%s/%s (%.1f%%)]",
                    FileUtils.formatFileSize(heapUsed),
                    FileUtils.formatFileSize(heapMax),
                    getHeapUsagePercent(),
                    FileUtils.formatFileSize(getUsedMemory()),
                    FileUtils.formatFileSize(maxMemory),
                    getTotalUsagePercent());
        }
    }

    /**
     * GC 스냅샷
     */
    public static class GCSnapshot {
        private final Map<String, Long> gcCounts;
        private final Map<String, Long> gcTimes;
        private final long totalGcCount;
        private final long totalGcTime;

        public GCSnapshot() {
            this.gcCounts = new HashMap<>();
            this.gcTimes = new HashMap<>();

            long totalCount = 0;
            long totalTime = 0;

            for (GarbageCollectorMXBean gcBean : gcBeans) {
                String name = gcBean.getName();
                long count = gcBean.getCollectionCount();
                long time = gcBean.getCollectionTime();

                if (count >= 0) {
                    gcCounts.put(name, count);
                    totalCount += count;
                }
                if (time >= 0) {
                    gcTimes.put(name, time);
                    totalTime += time;
                }
            }

            this.totalGcCount = totalCount;
            this.totalGcTime = totalTime;
        }

        public Map<String, Long> getGcCounts() { return new HashMap<>(gcCounts); }
        public Map<String, Long> getGcTimes() { return new HashMap<>(gcTimes); }
        public long getTotalGcCount() { return totalGcCount; }
        public long getTotalGcTime() { return totalGcTime; }
    }

    /**
     * 메모리 분석 결과
     */
    public static class MemoryAnalysisResult {
        private final String name;
        private final List<MemorySnapshot> snapshots;
        private final long durationMillis;

        public MemoryAnalysisResult(String name, List<MemorySnapshot> snapshots, long durationMillis) {
            this.name = name;
            this.snapshots = new ArrayList<>(snapshots);
            this.durationMillis = durationMillis;
        }

        public String getName() { return name; }
        public List<MemorySnapshot> getSnapshots() { return new ArrayList<>(snapshots); }
        public long getDurationMillis() { return durationMillis; }

        public long getAverageHeapUsed() {
            return (long) snapshots.stream()
                    .mapToLong(MemorySnapshot::getHeapUsed)
                    .average()
                    .orElse(0);
        }

        public long getPeakHeapUsed() {
            return snapshots.stream()
                    .mapToLong(MemorySnapshot::getHeapUsed)
                    .max()
                    .orElse(0);
        }

        public long getMinHeapUsed() {
            return snapshots.stream()
                    .mapToLong(MemorySnapshot::getHeapUsed)
                    .min()
                    .orElse(0);
        }

        public long getMemoryGrowth() {
            if (snapshots.isEmpty()) return 0;
            return snapshots.get(snapshots.size() - 1).getHeapUsed() - snapshots.get(0).getHeapUsed();
        }

        public long getTotalGcCount() {
            if (snapshots.isEmpty()) return 0;
            GCSnapshot first = snapshots.get(0).getGcSnapshot();
            GCSnapshot last = snapshots.get(snapshots.size() - 1).getGcSnapshot();
            return last.getTotalGcCount() - first.getTotalGcCount();
        }

        public long getTotalGcTime() {
            if (snapshots.isEmpty()) return 0;
            GCSnapshot first = snapshots.get(0).getGcSnapshot();
            GCSnapshot last = snapshots.get(snapshots.size() - 1).getGcSnapshot();
            return last.getTotalGcTime() - first.getTotalGcTime();
        }

        @Override
        public String toString() {
            return String.format("%s: avg=%s, peak=%s, growth=%s, gc_count=%d, gc_time=%dms",
                    name,
                    FileUtils.formatFileSize(getAverageHeapUsed()),
                    FileUtils.formatFileSize(getPeakHeapUsed()),
                    FileUtils.formatFileSize(getMemoryGrowth()),
                    getTotalGcCount(),
                    getTotalGcTime());
        }
    }

    /**
     * 작업 실행 중 메모리 사용량 측정
     */
    public MemoryAnalysisResult analyzeMemoryUsage(String name, Runnable task,
                                                   long samplingIntervalMillis) {
        List<MemorySnapshot> snapshots = new CopyOnWriteArrayList<>();
        ScheduledExecutorService sampler = Executors.newSingleThreadScheduledExecutor();

        // 초기 스냅샷
        snapshots.add(new MemorySnapshot());

        // 주기적 샘플링 시작
        ScheduledFuture<?> samplingTask = sampler.scheduleAtFixedRate(
                () -> snapshots.add(new MemorySnapshot()),
                samplingIntervalMillis,
                samplingIntervalMillis,
                TimeUnit.MILLISECONDS
        );

        long startTime = System.currentTimeMillis();

        try {
            // 작업 실행
            task.run();
        } finally {
            // 샘플링 중지
            samplingTask.cancel(false);
            sampler.shutdown();

            try {
                sampler.awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // 최종 스냅샷
            snapshots.add(new MemorySnapshot());
        }

        long duration = System.currentTimeMillis() - startTime;

        return new MemoryAnalysisResult(name, snapshots, duration);
    }

    /**
     * 메모리 누수 감지
     */
    public boolean detectMemoryLeak(Runnable task, int iterations, long thresholdBytes) {
        MemorySnapshot before = new MemorySnapshot();

        // GC 실행
        System.gc();
        try { Thread.sleep(100); } catch (InterruptedException e) {}

        MemorySnapshot afterGc = new MemorySnapshot();

        // 작업 반복 실행
        for (int i = 0; i < iterations; i++) {
            task.run();
        }

        // GC 실행
        System.gc();
        try { Thread.sleep(100); } catch (InterruptedException e) {}

        MemorySnapshot after = new MemorySnapshot();

        long growth = after.getHeapUsed() - afterGc.getHeapUsed();

        System.out.printf("Memory Leak Detection: before=%s, after=%s, growth=%s%n",
                FileUtils.formatFileSize(afterGc.getHeapUsed()),
                FileUtils.formatFileSize(after.getHeapUsed()),
                FileUtils.formatFileSize(growth));

        return growth > thresholdBytes;
    }

    /**
     * 메모리 압력 테스트
     */
    public MemoryAnalysisResult memoryPressureTest(String name, int allocationSizeMB,
                                                   int iterations) {
        List<MemorySnapshot> snapshots = new ArrayList<>();
        List<byte[]> allocations = new ArrayList<>();

        snapshots.add(new MemorySnapshot());

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < iterations; i++) {
            // 메모리 할당
            byte[] allocation = new byte[allocationSizeMB * 1024 * 1024];
            allocations.add(allocation);

            snapshots.add(new MemorySnapshot());

            // GC 유도 (50% 확률)
            if (Math.random() < 0.5) {
                System.gc();
            }
        }

        long duration = System.currentTimeMillis() - startTime;

        // 메모리 해제
        allocations.clear();
        System.gc();

        return new MemoryAnalysisResult(name, snapshots, duration);
    }

    /**
     * 여러 작업의 메모리 사용량 비교
     */
    public List<MemoryAnalysisResult> compareMemoryUsage(Map<String, Runnable> tasks,
                                                         long samplingIntervalMillis) {
        List<MemoryAnalysisResult> results = new ArrayList<>();

        for (Map.Entry<String, Runnable> entry : tasks.entrySet()) {
            // 각 테스트 전 GC
            System.gc();
            try { Thread.sleep(500); } catch (InterruptedException e) {}

            MemoryAnalysisResult result = analyzeMemoryUsage(
                    entry.getKey(),
                    entry.getValue(),
                    samplingIntervalMillis
            );
            results.add(result);
        }

        return results;
    }

    /**
     * 현재 메모리 상태 출력
     */
    public void printCurrentMemoryState() {
        MemorySnapshot snapshot = new MemorySnapshot();

        System.out.println("=== Current Memory State ===");
        System.out.println(snapshot);
        System.out.println("\nMemory Pools:");

        snapshot.getPoolUsages().forEach((name, usage) -> {
            System.out.printf("  %-30s: %s%n", name, FileUtils.formatFileSize(usage));
        });

        System.out.println("\nGarbage Collectors:");
        GCSnapshot gc = snapshot.getGcSnapshot();
        gc.getGcCounts().forEach((name, count) -> {
            long time = gc.getGcTimes().getOrDefault(name, 0L);
            System.out.printf("  %-30s: count=%d, time=%dms%n", name, count, time);
        });
    }

    /**
     * 비교 리포트 생성
     */
    public String generateComparisonReport(List<MemoryAnalysisResult> results) {
        StringBuilder report = new StringBuilder();
        report.append("=== Memory Usage Comparison Report ===\n\n");

        // 평균 메모리 사용량 순위
        report.append("Average Memory Usage Ranking:\n");
        results.stream()
                .sorted(Comparator.comparingLong(MemoryAnalysisResult::getAverageHeapUsed).reversed())
                .forEach(r -> report.append(String.format("  %s: %s\n",
                        r.getName(),
                        FileUtils.formatFileSize(r.getAverageHeapUsed()))));

        report.append("\n");

        // 피크 메모리 사용량 순위
        report.append("Peak Memory Usage Ranking:\n");
        results.stream()
                .sorted(Comparator.comparingLong(MemoryAnalysisResult::getPeakHeapUsed).reversed())
                .forEach(r -> report.append(String.format("  %s: %s\n",
                        r.getName(),
                        FileUtils.formatFileSize(r.getPeakHeapUsed()))));

        report.append("\n");

        // 메모리 증가량 순위
        report.append("Memory Growth Ranking:\n");
        results.stream()
                .sorted(Comparator.comparingLong(MemoryAnalysisResult::getMemoryGrowth).reversed())
                .forEach(r -> report.append(String.format("  %s: %s\n",
                        r.getName(),
                        FileUtils.formatFileSize(r.getMemoryGrowth()))));

        report.append("\n");

        // 상세 정보
        report.append("Detailed Statistics:\n");
        for (MemoryAnalysisResult result : results) {
            report.append(String.format("\n[%s]\n", result.getName()));
            report.append(String.format("  Duration: %dms\n", result.getDurationMillis()));
            report.append(String.format("  Samples: %d\n", result.getSnapshots().size()));
            report.append(String.format("  Average Heap: %s\n", FileUtils.formatFileSize(result.getAverageHeapUsed())));
            report.append(String.format("  Peak Heap: %s\n", FileUtils.formatFileSize(result.getPeakHeapUsed())));
            report.append(String.format("  Min Heap: %s\n", FileUtils.formatFileSize(result.getMinHeapUsed())));
            report.append(String.format("  Memory Growth: %s\n", FileUtils.formatFileSize(result.getMemoryGrowth())));
            report.append(String.format("  GC Count: %d\n", result.getTotalGcCount()));
            report.append(String.format("  GC Time: %dms\n", result.getTotalGcTime()));
        }

        return report.toString();
    }

    /**
     * HTML 리포트 생성
     */
    public void generateHtmlReport(List<MemoryAnalysisResult> results, String filename)
            throws Exception {

        ReportGenerator.ReportBuilder builder = ReportGenerator.html("Memory Usage Analysis Report")
                .author("MemoryUsageAnalyzer");

        // 테이블 데이터 준비
        String[] headers = {"Name", "Avg Heap", "Peak Heap", "Growth", "GC Count", "GC Time"};
        List<String[]> rows = results.stream()
                .map(r -> new String[]{
                        r.getName(),
                        FileUtils.formatFileSize(r.getAverageHeapUsed()),
                        FileUtils.formatFileSize(r.getPeakHeapUsed()),
                        FileUtils.formatFileSize(r.getMemoryGrowth()),
                        String.valueOf(r.getTotalGcCount()),
                        r.getTotalGcTime() + "ms"
                })
                .collect(Collectors.toList());

        builder.addTable("Memory Usage Results", headers, rows);

        // 파일 저장
        builder.saveToFile(filename);
    }

    /**
     * 테스트 메인 메서드
     */
    public static void main(String[] args) {
        System.out.println("=== MemoryUsageAnalyzer 테스트 ===\n");

        MemoryUsageAnalyzer analyzer = new MemoryUsageAnalyzer();

        try {
            // 1. 현재 메모리 상태
            System.out.println("1. 현재 메모리 상태:");
            analyzer.printCurrentMemoryState();

            // 2. 단일 작업 메모리 분석
            System.out.println("\n2. 단일 작업 메모리 분석:");
            MemoryAnalysisResult singleResult = analyzer.analyzeMemoryUsage(
                    "List Allocation Test",
                    () -> {
                        List<byte[]> list = new ArrayList<>();
                        for (int i = 0; i < 1000; i++) {
                            list.add(new byte[10 * 1024]); // 10KB each
                        }
                    },
                    100 // 100ms 샘플링
            );
            System.out.println(singleResult);

            // 3. 여러 작업 비교
            System.out.println("\n3. 여러 작업 메모리 사용량 비교:");
            Map<String, Runnable> tasks = new LinkedHashMap<>();

            tasks.put("ArrayList", () -> {
                List<Integer> list = new ArrayList<>();
                for (int i = 0; i < 100000; i++) {
                    list.add(i);
                }
            });

            tasks.put("LinkedList", () -> {
                List<Integer> list = new LinkedList<>();
                for (int i = 0; i < 100000; i++) {
                    list.add(i);
                }
            });

            tasks.put("HashMap", () -> {
                Map<Integer, String> map = new HashMap<>();
                for (int i = 0; i < 100000; i++) {
                    map.put(i, "value" + i);
                }
            });

            List<MemoryAnalysisResult> compareResults = analyzer.compareMemoryUsage(tasks, 50);
            compareResults.forEach(System.out::println);

            // 4. 메모리 누수 감지
            System.out.println("\n4. 메모리 누수 감지:");
            boolean hasLeak = analyzer.detectMemoryLeak(
                    () -> {
                        // 의도적 누수 시뮬레이션
                        List<byte[]> leaked = new ArrayList<>();
                        leaked.add(new byte[1024 * 1024]); // 1MB
                    },
                    10,
                    5 * 1024 * 1024 // 5MB threshold
            );
            System.out.println("Memory leak detected: " + hasLeak);

            // 5. 메모리 압력 테스트
            System.out.println("\n5. 메모리 압력 테스트:");
            MemoryAnalysisResult pressureResult = analyzer.memoryPressureTest(
                    "Pressure Test",
                    10, // 10MB per allocation
                    5   // 5 iterations
            );
            System.out.println(pressureResult);

            // 6. 비교 리포트
            System.out.println("\n6. 비교 리포트:");
            System.out.println(analyzer.generateComparisonReport(compareResults));

            // 7. HTML 리포트 생성
            System.out.println("\n7. HTML 리포트 생성:");
            analyzer.generateHtmlReport(compareResults, "memory_analysis_report.html");
            System.out.println("리포트 저장: memory_analysis_report.html");

        } catch (Exception e) {
            System.err.println("테스트 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("\n테스트 완료!");
    }
}