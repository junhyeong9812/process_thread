package com.study.comparison.resource;

import java.lang.management.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 리소스 프로파일러
 *
 * 스레드 실행 중 시스템 리소스를 종합적으로 모니터링합니다:
 * - Memory: Heap, Non-heap, GC 활동
 * - CPU: 사용률, 시스템 부하
 * - Thread: 생성/종료, 상태 분포
 * - GC: GC 횟수, GC 시간
 *
 * 프로파일링 방식:
 * 1. Snapshot: 특정 시점의 리소스 상태
 * 2. Continuous: 주기적 샘플링 (시계열 데이터)
 * 3. Event-based: 특정 이벤트 발생 시 기록
 *
 * 사용 시나리오:
 * - Platform vs Virtual Thread 리소스 사용량 비교
 * - 메모리 누수 탐지
 * - 성능 병목 지점 발견
 * - 최적 스레드 수 결정
 */
public class ResourceProfiler {

    private final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
    private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
    private final List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
    private final OperatingSystemMXBean osMXBean = ManagementFactory.getOperatingSystemMXBean();
    private final Runtime runtime = Runtime.getRuntime();

    /**
     * 리소스 스냅샷
     */
    static class ResourceSnapshot {
        final Instant timestamp;
        final long heapUsedMB;
        final long heapMaxMB;
        final long nonHeapUsedMB;
        final int threadCount;
        final int daemonThreadCount;
        final int peakThreadCount;
        final long totalGCCount;
        final long totalGCTimeMs;
        final double systemLoadAverage;
        final Map<Thread.State, Integer> threadStateDistribution;

        ResourceSnapshot(Instant timestamp, long heapUsedMB, long heapMaxMB,
                         long nonHeapUsedMB, int threadCount, int daemonThreadCount,
                         int peakThreadCount, long totalGCCount, long totalGCTimeMs,
                         double systemLoadAverage, Map<Thread.State, Integer> threadStateDistribution) {
            this.timestamp = timestamp;
            this.heapUsedMB = heapUsedMB;
            this.heapMaxMB = heapMaxMB;
            this.nonHeapUsedMB = nonHeapUsedMB;
            this.threadCount = threadCount;
            this.daemonThreadCount = daemonThreadCount;
            this.peakThreadCount = peakThreadCount;
            this.totalGCCount = totalGCCount;
            this.totalGCTimeMs = totalGCTimeMs;
            this.systemLoadAverage = systemLoadAverage;
            this.threadStateDistribution = threadStateDistribution;
        }

        void print() {
            System.out.println("  Timestamp: " + timestamp);
            System.out.println("  Memory:");
            System.out.println("    Heap: " + heapUsedMB + " MB / " + heapMaxMB + " MB " +
                    "(" + String.format("%.1f%%", (heapUsedMB * 100.0 / heapMaxMB)) + ")");
            System.out.println("    Non-Heap: " + nonHeapUsedMB + " MB");
            System.out.println("  Threads:");
            System.out.println("    Current: " + threadCount);
            System.out.println("    Daemon: " + daemonThreadCount);
            System.out.println("    Peak: " + peakThreadCount);
            System.out.println("  GC:");
            System.out.println("    Total collections: " + totalGCCount);
            System.out.println("    Total time: " + totalGCTimeMs + " ms");
            System.out.println("  System:");
            System.out.println("    Load average: " + String.format("%.2f", systemLoadAverage));
            System.out.println("  Thread States:");
            threadStateDistribution.forEach((state, count) ->
                    System.out.println("    " + state + ": " + count));
        }
    }

    /**
     * 프로파일링 결과
     */
    static class ProfilingResult {
        final String testName;
        final List<ResourceSnapshot> snapshots;
        final long durationMs;
        final ResourceSnapshot initial;
        final ResourceSnapshot peak;
        final ResourceSnapshot finalSnapshot;

        ProfilingResult(String testName, List<ResourceSnapshot> snapshots, long durationMs) {
            this.testName = testName;
            this.snapshots = snapshots;
            this.durationMs = durationMs;
            this.initial = snapshots.isEmpty() ? null : snapshots.get(0);
            this.peak = findPeakSnapshot(snapshots);
            this.finalSnapshot = snapshots.isEmpty() ? null : snapshots.get(snapshots.size() - 1);
        }

        private ResourceSnapshot findPeakSnapshot(List<ResourceSnapshot> snapshots) {
            return snapshots.stream()
                    .max(Comparator.comparingLong(s -> s.heapUsedMB))
                    .orElse(null);
        }

        void printSummary() {
            System.out.println("\n" + "═".repeat(70));
            System.out.println("  PROFILING SUMMARY: " + testName);
            System.out.println("═".repeat(70));
            System.out.println("  Duration: " + durationMs + " ms");
            System.out.println("  Snapshots collected: " + snapshots.size());

            if (initial != null && finalSnapshot != null) {
                System.out.println("\n  Memory Changes:");
                long heapDelta = finalSnapshot.heapUsedMB - initial.heapUsedMB;
                System.out.println("    Heap: " + initial.heapUsedMB + " MB → " +
                        finalSnapshot.heapUsedMB + " MB (" +
                        (heapDelta >= 0 ? "+" : "") + heapDelta + " MB)");

                System.out.println("\n  Thread Changes:");
                int threadDelta = finalSnapshot.threadCount - initial.threadCount;
                System.out.println("    Count: " + initial.threadCount + " → " +
                        finalSnapshot.threadCount + " (" +
                        (threadDelta >= 0 ? "+" : "") + threadDelta + ")");
                System.out.println("    Peak: " +
                        Math.max(initial.peakThreadCount, finalSnapshot.peakThreadCount));

                System.out.println("\n  GC Activity:");
                long gcCountDelta = finalSnapshot.totalGCCount - initial.totalGCCount;
                long gcTimeDelta = finalSnapshot.totalGCTimeMs - initial.totalGCTimeMs;
                System.out.println("    Collections: " + gcCountDelta);
                System.out.println("    Time spent: " + gcTimeDelta + " ms " +
                        "(" + String.format("%.2f%%", (gcTimeDelta * 100.0 / durationMs)) +
                        " of total time)");
            }

            if (peak != null) {
                System.out.println("\n  Peak Resource Usage:");
                System.out.println("    Heap: " + peak.heapUsedMB + " MB");
                System.out.println("    Threads: " + peak.threadCount);
            }
        }

        void printTimeSeries() {
            if (snapshots.isEmpty()) return;

            System.out.println("\n  Time Series Data:");
            System.out.println("  " + "-".repeat(68));
            System.out.println("  Time(s) | Heap(MB) | Threads | GC Count | Load Avg");
            System.out.println("  " + "-".repeat(68));

            Instant startTime = snapshots.get(0).timestamp;
            for (ResourceSnapshot snapshot : snapshots) {
                long elapsedSeconds = Duration.between(startTime, snapshot.timestamp).getSeconds();
                System.out.println(String.format("  %6d  | %8d | %7d | %8d | %8.2f",
                        elapsedSeconds,
                        snapshot.heapUsedMB,
                        snapshot.threadCount,
                        snapshot.totalGCCount,
                        snapshot.systemLoadAverage));
            }
            System.out.println("  " + "-".repeat(68));
        }

        void printChart() {
            if (snapshots.size() < 2) return;

            System.out.println("\n  Memory Usage Chart (Heap):");
            printMemoryChart();

            System.out.println("\n  Thread Count Chart:");
            printThreadChart();
        }

        private void printMemoryChart() {
            long maxHeap = snapshots.stream()
                    .mapToLong(s -> s.heapUsedMB)
                    .max()
                    .orElse(100);

            int chartWidth = 60;
            System.out.println("  0 MB " + "-".repeat(chartWidth) + " " + maxHeap + " MB");

            for (int i = 0; i < snapshots.size(); i++) {
                ResourceSnapshot snapshot = snapshots.get(i);
                int barLength = (int) ((snapshot.heapUsedMB * chartWidth) / maxHeap);
                String bar = "█".repeat(Math.max(0, barLength));
                System.out.println(String.format("  %2d  |%s %d MB",
                        i, bar, snapshot.heapUsedMB));
            }
        }

        private void printThreadChart() {
            int maxThreads = snapshots.stream()
                    .mapToInt(s -> s.threadCount)
                    .max()
                    .orElse(100);

            int chartWidth = 60;
            System.out.println("  0 " + "-".repeat(chartWidth) + " " + maxThreads);

            for (int i = 0; i < snapshots.size(); i++) {
                ResourceSnapshot snapshot = snapshots.get(i);
                int barLength = (int) ((snapshot.threadCount * chartWidth) / maxThreads);
                String bar = "█".repeat(Math.max(0, barLength));
                System.out.println(String.format("  %2d  |%s %d",
                        i, bar, snapshot.threadCount));
            }
        }
    }

    /**
     * 현재 리소스 스냅샷 캡처
     */
    public ResourceSnapshot captureSnapshot() {
        Instant timestamp = Instant.now();

        // Memory
        MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = memoryMXBean.getNonHeapMemoryUsage();
        long heapUsedMB = heapUsage.getUsed() / 1024 / 1024;
        long heapMaxMB = heapUsage.getMax() / 1024 / 1024;
        long nonHeapUsedMB = nonHeapUsage.getUsed() / 1024 / 1024;

        // Threads
        int threadCount = threadMXBean.getThreadCount();
        int daemonThreadCount = threadMXBean.getDaemonThreadCount();
        int peakThreadCount = threadMXBean.getPeakThreadCount();

        // Thread States
        Map<Thread.State, Integer> stateDistribution = new EnumMap<>(Thread.State.class);
        long[] threadIds = threadMXBean.getAllThreadIds();
        ThreadInfo[] threadInfos = threadMXBean.getThreadInfo(threadIds);

        for (ThreadInfo info : threadInfos) {
            if (info != null) {
                Thread.State state = info.getThreadState();
                stateDistribution.merge(state, 1, Integer::sum);
            }
        }

        // GC
        long totalGCCount = 0;
        long totalGCTime = 0;
        for (GarbageCollectorMXBean gcBean : gcBeans) {
            totalGCCount += gcBean.getCollectionCount();
            totalGCTime += gcBean.getCollectionTime();
        }

        // System
        double systemLoadAverage = osMXBean.getSystemLoadAverage();

        return new ResourceSnapshot(
                timestamp, heapUsedMB, heapMaxMB, nonHeapUsedMB,
                threadCount, daemonThreadCount, peakThreadCount,
                totalGCCount, totalGCTime, systemLoadAverage,
                stateDistribution
        );
    }

    /**
     * 연속 프로파일링 (주기적 샘플링)
     */
    public ProfilingResult profileContinuous(String testName, Runnable workload,
                                             long samplingIntervalMs, long maxDurationMs)
            throws InterruptedException {
        List<ResourceSnapshot> snapshots = new CopyOnWriteArrayList<>();
        AtomicInteger running = new AtomicInteger(1);

        // 샘플링 스레드
        Thread samplerThread = Thread.ofPlatform().start(() -> {
            while (running.get() > 0) {
                snapshots.add(captureSnapshot());
                try {
                    Thread.sleep(samplingIntervalMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });

        // 워크로드 실행
        Instant start = Instant.now();
        Thread workloadThread = Thread.ofPlatform().start(workload);

        // 최대 시간 대기
        workloadThread.join(maxDurationMs);
        if (workloadThread.isAlive()) {
            workloadThread.interrupt();
            workloadThread.join(1000);
        }

        long duration = Duration.between(start, Instant.now()).toMillis();

        // 샘플링 중단
        running.set(0);
        samplerThread.interrupt();
        samplerThread.join(1000);

        // 최종 스냅샷
        snapshots.add(captureSnapshot());

        return new ProfilingResult(testName, snapshots, duration);
    }

    /**
     * 스냅샷 기반 프로파일링 (전/후 비교)
     */
    public ProfilingResult profileSnapshot(String testName, Runnable workload)
            throws InterruptedException {
        List<ResourceSnapshot> snapshots = new ArrayList<>();

        // Before
        System.gc();
        Thread.sleep(100);
        snapshots.add(captureSnapshot());

        // Execute
        Instant start = Instant.now();
        Thread workloadThread = Thread.ofPlatform().start(workload);
        workloadThread.join();
        long duration = Duration.between(start, Instant.now()).toMillis();

        // After
        Thread.sleep(100);
        snapshots.add(captureSnapshot());

        return new ProfilingResult(testName, snapshots, duration);
    }

    /**
     * 메모리 누수 탐지
     */
    static class MemoryLeakDetector {
        private final ResourceProfiler profiler;

        MemoryLeakDetector(ResourceProfiler profiler) {
            this.profiler = profiler;
        }

        public boolean detectLeak(List<ResourceSnapshot> snapshots, double thresholdMBPerSnapshot) {
            if (snapshots.size() < 3) return false;

            // 메모리가 지속적으로 증가하는지 확인
            int increasingCount = 0;
            for (int i = 1; i < snapshots.size(); i++) {
                long delta = snapshots.get(i).heapUsedMB - snapshots.get(i - 1).heapUsedMB;
                if (delta > thresholdMBPerSnapshot) {
                    increasingCount++;
                }
            }

            // 80% 이상의 스냅샷에서 메모리가 증가하면 누수 의심
            double leakRatio = (double) increasingCount / (snapshots.size() - 1);
            return leakRatio > 0.8;
        }

        public void printLeakAnalysis(ProfilingResult result) {
            System.out.println("\n  Memory Leak Analysis:");

            boolean leakDetected = detectLeak(result.snapshots, 10); // 10MB threshold

            if (leakDetected) {
                System.out.println("    ⚠️  Potential memory leak detected!");
                System.out.println("    Memory consistently increasing across snapshots");

                if (result.initial != null && result.finalSnapshot != null) {
                    long totalIncrease = result.finalSnapshot.heapUsedMB - result.initial.heapUsedMB;
                    double increaseRate = (totalIncrease * 1000.0) / result.durationMs; // MB/s
                    System.out.println("    Total increase: " + totalIncrease + " MB");
                    System.out.println("    Increase rate: " +
                            String.format("%.2f MB/s", increaseRate));
                }
            } else {
                System.out.println("    ✓ No memory leak detected");
            }
        }
    }

    /**
     * 병목 현상 탐지
     */
    static class BottleneckDetector {

        public void detectBottlenecks(ProfilingResult result) {
            System.out.println("\n  Bottleneck Analysis:");

            // CPU 병목
            boolean cpuBottleneck = result.snapshots.stream()
                    .anyMatch(s -> s.systemLoadAverage > Runtime.getRuntime().availableProcessors() * 0.8);

            if (cpuBottleneck) {
                System.out.println("    ⚠️  CPU bottleneck detected");
                System.out.println("       System load average exceeds 80% of CPU cores");
            }

            // 메모리 병목
            boolean memoryBottleneck = result.snapshots.stream()
                    .anyMatch(s -> (s.heapUsedMB * 100.0 / s.heapMaxMB) > 90);

            if (memoryBottleneck) {
                System.out.println("    ⚠️  Memory bottleneck detected");
                System.out.println("       Heap usage exceeds 90%");
            }

            // GC 병목
            if (result.initial != null && result.finalSnapshot != null) {
                long gcTime = result.finalSnapshot.totalGCTimeMs - result.initial.totalGCTimeMs;
                double gcRatio = (gcTime * 100.0) / result.durationMs;

                if (gcRatio > 10) {
                    System.out.println("    ⚠️  GC bottleneck detected");
                    System.out.println("       GC time is " + String.format("%.1f%%", gcRatio) +
                            " of total execution time");
                }
            }

            // Thread 병목 (대기 상태가 많은 경우)
            if (result.peak != null) {
                Integer blockedCount = result.peak.threadStateDistribution.get(Thread.State.BLOCKED);
                Integer waitingCount = result.peak.threadStateDistribution.get(Thread.State.WAITING);
                int totalBlocked = (blockedCount != null ? blockedCount : 0) +
                        (waitingCount != null ? waitingCount : 0);

                if (totalBlocked > result.peak.threadCount * 0.5) {
                    System.out.println("    ⚠️  Thread contention detected");
                    System.out.println("       More than 50% of threads are blocked/waiting");
                }
            }

            if (!cpuBottleneck && !memoryBottleneck) {
                System.out.println("    ✓ No major bottlenecks detected");
            }
        }
    }

    /**
     * 테스트 시나리오
     */
    static class ProfilerDemo {

        public void runDemos() throws Exception {
            System.out.println("\n╔═══════════════════════════════════════════════════════════════════╗");
            System.out.println("║      RESOURCE PROFILER DEMO                                       ║");
            System.out.println("╚═══════════════════════════════════════════════════════════════════╝");

            ResourceProfiler profiler = new ResourceProfiler();

            // 1. Snapshot 프로파일링
            System.out.println("\n1. Snapshot Profiling (Before/After)");
            System.out.println("═".repeat(70));

            ProfilingResult snapshotResult = profiler.profileSnapshot(
                    "Platform Thread Test",
                    () -> {
                        List<Thread> threads = new ArrayList<>();
                        for (int i = 0; i < 100; i++) {
                            Thread thread = Thread.ofPlatform().start(() -> {
                                try {
                                    Thread.sleep(100);
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                }
                            });
                            threads.add(thread);
                        }

                        for (Thread thread : threads) {
                            try {
                                thread.join();
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        }
                    });

            snapshotResult.printSummary();

            // 2. Continuous 프로파일링
            System.out.println("\n\n2. Continuous Profiling (Time Series)");
            System.out.println("═".repeat(70));

            ProfilingResult continuousResult = profiler.profileContinuous(
                    "Virtual Thread Test",
                    () -> {
                        List<Thread> threads = new ArrayList<>();
                        for (int i = 0; i < 1000; i++) {
                            Thread thread = Thread.ofVirtual().start(() -> {
                                try {
                                    Thread.sleep(50);
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                }
                            });
                            threads.add(thread);
                        }

                        for (Thread thread : threads) {
                            try {
                                thread.join();
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        }
                    },
                    500,  // 500ms sampling interval
                    10000 // 10s max duration
            );

            continuousResult.printSummary();
            continuousResult.printTimeSeries();
            continuousResult.printChart();

            // 3. 메모리 누수 탐지
            System.out.println("\n\n3. Memory Leak Detection");
            System.out.println("═".repeat(70));

            MemoryLeakDetector leakDetector = new MemoryLeakDetector(profiler);
            leakDetector.printLeakAnalysis(continuousResult);

            // 4. 병목 현상 탐지
            System.out.println("\n4. Bottleneck Detection");
            System.out.println("═".repeat(70));

            BottleneckDetector bottleneckDetector = new BottleneckDetector();
            bottleneckDetector.detectBottlenecks(continuousResult);
        }
    }

    public static void main(String[] args) throws Exception {
        ProfilerDemo demo = new ProfilerDemo();
        demo.runDemos();

        System.out.println("\n╔═══════════════════════════════════════════════════════════════════╗");
        System.out.println("║         PROFILING COMPLETED                                       ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════╝");
    }
}