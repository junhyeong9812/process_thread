package com.study.thread.comparison.resource;

import java.lang.management.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 리소스 사용량 비교
 *
 * Platform Thread vs Virtual Thread vs Thread Pool의
 * 리소스 사용량을 종합적으로 비교 분석합니다.
 *
 * 비교 항목:
 * 1. Memory Footprint: 메모리 사용량
 * 2. Thread Overhead: 스레드당 메모리 오버헤드
 * 3. GC Pressure: GC 부하
 * 4. CPU Usage: CPU 사용률
 * 5. Scalability: 확장성
 *
 * 목표:
 * - 각 스레드 모델의 리소스 효율성 평가
 * - 워크로드별 최적 모델 선택 가이드
 * - 리소스 한계 파악
 */
public class ResourceComparison {

    private final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
    private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
    private final List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
    private final Runtime runtime = Runtime.getRuntime();

    /**
     * 비교 결과
     */
    static class ComparisonResult {
        final String threadModel;
        final int threadCount;
        final long executionTimeMs;
        final long peakMemoryMB;
        final long memoryPerThreadKB;
        final long gcCount;
        final long gcTimeMs;
        final double gcOverheadPercent;
        final int peakThreadCount;
        final double throughput; // tasks/sec

        ComparisonResult(String threadModel, int threadCount, long executionTimeMs,
                         long peakMemoryMB, long memoryPerThreadKB, long gcCount,
                         long gcTimeMs, int peakThreadCount) {
            this.threadModel = threadModel;
            this.threadCount = threadCount;
            this.executionTimeMs = executionTimeMs;
            this.peakMemoryMB = peakMemoryMB;
            this.memoryPerThreadKB = memoryPerThreadKB;
            this.gcCount = gcCount;
            this.gcTimeMs = gcTimeMs;
            this.gcOverheadPercent = executionTimeMs > 0 ?
                    (gcTimeMs * 100.0) / executionTimeMs : 0;
            this.peakThreadCount = peakThreadCount;
            this.throughput = executionTimeMs > 0 ?
                    (threadCount * 1000.0) / executionTimeMs : 0;
        }

        void print() {
            System.out.println("\n  === " + threadModel + " ===");
            System.out.println("  Thread count: " + String.format("%,d", threadCount));
            System.out.println("  Execution time: " + String.format("%,d ms", executionTimeMs));
            System.out.println("  Peak memory: " + String.format("%,d MB", peakMemoryMB));
            System.out.println("  Memory/thread: " + String.format("%,d KB", memoryPerThreadKB));
            System.out.println("  GC count: " + gcCount);
            System.out.println("  GC time: " + gcTimeMs + " ms (" +
                    String.format("%.2f%%", gcOverheadPercent) + ")");
            System.out.println("  Peak threads: " + peakThreadCount);
            System.out.println("  Throughput: " + String.format("%.2f tasks/sec", throughput));
        }
    }

    /**
     * 워크로드 타입
     */
    enum WorkloadType {
        IO_BOUND,
        CPU_BOUND,
        MIXED
    }

    /**
     * 워크로드 정의
     */
    static class Workload {
        final WorkloadType type;
        final int iterations;

        Workload(WorkloadType type, int iterations) {
            this.type = type;
            this.iterations = iterations;
        }

        void execute() {
            switch (type) {
                case IO_BOUND -> executeIOBound();
                case CPU_BOUND -> executeCPUBound();
                case MIXED -> executeMixed();
            }
        }

        private void executeIOBound() {
            try {
                Thread.sleep(10); // I/O 시뮬레이션
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        private void executeCPUBound() {
            long sum = 0;
            for (int i = 0; i < iterations; i++) {
                sum += Math.sqrt(i) * Math.sin(i);
            }
        }

        private void executeMixed() {
            // CPU
            long sum = 0;
            for (int i = 0; i < iterations / 10; i++) {
                sum += Math.sqrt(i);
            }

            // I/O
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Platform Thread 테스트
     */
    public ComparisonResult testPlatformThread(int threadCount, Workload workload)
            throws Exception {
        System.gc();
        Thread.sleep(200);

        long memBefore = getUsedMemoryMB();
        long gcCountBefore = getTotalGCCount();
        long gcTimeBefore = getTotalGCTime();
        int threadCountBefore = threadMXBean.getThreadCount();

        Instant start = Instant.now();
        CountDownLatch latch = new CountDownLatch(threadCount);
        long peakMemory = memBefore;

        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            Thread thread = Thread.ofPlatform().start(() -> {
                try {
                    workload.execute();
                } finally {
                    latch.countDown();
                }
            });
            threads.add(thread);

            // 주기적으로 peak memory 측정
            if (i % 100 == 0) {
                peakMemory = Math.max(peakMemory, getUsedMemoryMB());
            }
        }

        latch.await();
        long executionTime = Duration.between(start, Instant.now()).toMillis();

        for (Thread thread : threads) {
            thread.join();
        }

        Thread.sleep(100);
        peakMemory = Math.max(peakMemory, getUsedMemoryMB());

        long memAfter = getUsedMemoryMB();
        long gcCountAfter = getTotalGCCount();
        long gcTimeAfter = getTotalGCTime();
        int peakThreadCount = threadMXBean.getPeakThreadCount();

        long memoryUsed = Math.max(0, peakMemory - memBefore);
        long memoryPerThread = threadCount > 0 ? (memoryUsed * 1024) / threadCount : 0;

        threadMXBean.resetPeakThreadCount();

        return new ComparisonResult(
                "Platform Thread",
                threadCount,
                executionTime,
                memoryUsed,
                memoryPerThread,
                gcCountAfter - gcCountBefore,
                gcTimeAfter - gcTimeBefore,
                peakThreadCount - threadCountBefore
        );
    }

    /**
     * Virtual Thread 테스트
     */
    public ComparisonResult testVirtualThread(int threadCount, Workload workload)
            throws Exception {
        System.gc();
        Thread.sleep(200);

        long memBefore = getUsedMemoryMB();
        long gcCountBefore = getTotalGCCount();
        long gcTimeBefore = getTotalGCTime();
        int threadCountBefore = threadMXBean.getThreadCount();

        Instant start = Instant.now();
        CountDownLatch latch = new CountDownLatch(threadCount);
        long peakMemory = memBefore;

        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            Thread thread = Thread.ofVirtual().start(() -> {
                try {
                    workload.execute();
                } finally {
                    latch.countDown();
                }
            });
            threads.add(thread);

            if (i % 1000 == 0) {
                peakMemory = Math.max(peakMemory, getUsedMemoryMB());
            }
        }

        latch.await();
        long executionTime = Duration.between(start, Instant.now()).toMillis();

        for (Thread thread : threads) {
            thread.join();
        }

        Thread.sleep(100);
        peakMemory = Math.max(peakMemory, getUsedMemoryMB());

        long memAfter = getUsedMemoryMB();
        long gcCountAfter = getTotalGCCount();
        long gcTimeAfter = getTotalGCTime();
        int peakThreadCount = threadMXBean.getPeakThreadCount();

        long memoryUsed = Math.max(0, peakMemory - memBefore);
        long memoryPerThread = threadCount > 0 ? (memoryUsed * 1024) / threadCount : 0;

        threadMXBean.resetPeakThreadCount();

        return new ComparisonResult(
                "Virtual Thread",
                threadCount,
                executionTime,
                memoryUsed,
                memoryPerThread,
                gcCountAfter - gcCountBefore,
                gcTimeAfter - gcTimeBefore,
                peakThreadCount - threadCountBefore
        );
    }

    /**
     * Thread Pool 테스트
     */
    public ComparisonResult testThreadPool(int taskCount, Workload workload)
            throws Exception {
        System.gc();
        Thread.sleep(200);

        long memBefore = getUsedMemoryMB();
        long gcCountBefore = getTotalGCCount();
        long gcTimeBefore = getTotalGCTime();
        int threadCountBefore = threadMXBean.getThreadCount();

        int poolSize = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(poolSize);

        Instant start = Instant.now();
        long peakMemory = memBefore;

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < taskCount; i++) {
            Future<?> future = executor.submit(() -> workload.execute());
            futures.add(future);

            if (i % 100 == 0) {
                peakMemory = Math.max(peakMemory, getUsedMemoryMB());
            }
        }

        for (Future<?> future : futures) {
            future.get();
        }

        long executionTime = Duration.between(start, Instant.now()).toMillis();

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        Thread.sleep(100);
        peakMemory = Math.max(peakMemory, getUsedMemoryMB());

        long memAfter = getUsedMemoryMB();
        long gcCountAfter = getTotalGCCount();
        long gcTimeAfter = getTotalGCTime();
        int peakThreadCount = threadMXBean.getPeakThreadCount();

        long memoryUsed = Math.max(0, peakMemory - memBefore);
        long memoryPerThread = poolSize > 0 ? (memoryUsed * 1024) / poolSize : 0;

        threadMXBean.resetPeakThreadCount();

        return new ComparisonResult(
                "Thread Pool (size=" + poolSize + ")",
                taskCount,
                executionTime,
                memoryUsed,
                memoryPerThread,
                gcCountAfter - gcCountBefore,
                gcTimeAfter - gcTimeBefore,
                peakThreadCount - threadCountBefore
        );
    }

    /**
     * 헬퍼 메서드
     */
    private long getUsedMemoryMB() {
        return memoryMXBean.getHeapMemoryUsage().getUsed() / 1024 / 1024;
    }

    private long getTotalGCCount() {
        return gcBeans.stream()
                .mapToLong(GarbageCollectorMXBean::getCollectionCount)
                .sum();
    }

    private long getTotalGCTime() {
        return gcBeans.stream()
                .mapToLong(GarbageCollectorMXBean::getCollectionTime)
                .sum();
    }

    /**
     * 비교 분석
     */
    static class ComparisonAnalyzer {

        public void compareResults(List<ComparisonResult> results) {
            System.out.println("\n" + "═".repeat(70));
            System.out.println("  COMPARISON ANALYSIS");
            System.out.println("═".repeat(70));

            if (results.isEmpty()) return;

            // 1. 메모리 효율성
            System.out.println("\n  1. Memory Efficiency:");
            ComparisonResult bestMemory = results.stream()
                    .min(Comparator.comparingLong(r -> r.peakMemoryMB))
                    .orElse(null);

            results.forEach(r -> {
                double ratio = bestMemory != null ?
                        (double) r.peakMemoryMB / bestMemory.peakMemoryMB : 1.0;
                String badge = r == bestMemory ? " 🏆 BEST" : "";
                System.out.println(String.format("    %s: %,d MB (%.2fx)%s",
                        r.threadModel, r.peakMemoryMB, ratio, badge));
            });

            // 2. 스레드당 메모리
            System.out.println("\n  2. Memory per Thread:");
            ComparisonResult bestMemPerThread = results.stream()
                    .filter(r -> r.memoryPerThreadKB > 0)
                    .min(Comparator.comparingLong(r -> r.memoryPerThreadKB))
                    .orElse(null);

            results.forEach(r -> {
                if (r.memoryPerThreadKB > 0) {
                    double ratio = bestMemPerThread != null ?
                            (double) r.memoryPerThreadKB / bestMemPerThread.memoryPerThreadKB : 1.0;
                    String badge = r == bestMemPerThread ? " 🏆 BEST" : "";
                    System.out.println(String.format("    %s: %,d KB (%.2fx)%s",
                            r.threadModel, r.memoryPerThreadKB, ratio, badge));
                }
            });

            // 3. 실행 시간
            System.out.println("\n  3. Execution Time:");
            ComparisonResult fastest = results.stream()
                    .min(Comparator.comparingLong(r -> r.executionTimeMs))
                    .orElse(null);

            results.forEach(r -> {
                double ratio = fastest != null ?
                        (double) r.executionTimeMs / fastest.executionTimeMs : 1.0;
                String badge = r == fastest ? " 🏆 FASTEST" : "";
                System.out.println(String.format("    %s: %,d ms (%.2fx)%s",
                        r.threadModel, r.executionTimeMs, ratio, badge));
            });

            // 4. 처리량
            System.out.println("\n  4. Throughput:");
            ComparisonResult highestThroughput = results.stream()
                    .max(Comparator.comparingDouble(r -> r.throughput))
                    .orElse(null);

            results.forEach(r -> {
                double ratio = highestThroughput != null ?
                        r.throughput / highestThroughput.throughput : 1.0;
                String badge = r == highestThroughput ? " 🏆 HIGHEST" : "";
                System.out.println(String.format("    %s: %.2f tasks/sec (%.2f%%)%s",
                        r.threadModel, r.throughput, ratio * 100, badge));
            });

            // 5. GC 부하
            System.out.println("\n  5. GC Overhead:");
            ComparisonResult lowestGC = results.stream()
                    .min(Comparator.comparingDouble(r -> r.gcOverheadPercent))
                    .orElse(null);

            results.forEach(r -> {
                String badge = r == lowestGC ? " 🏆 LOWEST" : "";
                System.out.println(String.format("    %s: %.2f%% (%d collections, %d ms)%s",
                        r.threadModel, r.gcOverheadPercent, r.gcCount, r.gcTimeMs, badge));
            });
        }

        public void printRecommendations(List<ComparisonResult> results, WorkloadType workloadType) {
            System.out.println("\n" + "═".repeat(70));
            System.out.println("  RECOMMENDATIONS");
            System.out.println("═".repeat(70));

            System.out.println("\n  For " + workloadType + " workloads:");

            switch (workloadType) {
                case IO_BOUND -> {
                    System.out.println("    ✓ Virtual Threads are optimal");
                    System.out.println("      - Lowest memory per thread");
                    System.out.println("      - Can handle 100,000+ concurrent I/O operations");
                    System.out.println("      - Best throughput for I/O-heavy tasks");
                }
                case CPU_BOUND -> {
                    System.out.println("    ✓ Thread Pool is optimal");
                    System.out.println("      - Pool size = CPU cores");
                    System.out.println("      - Minimal context switching");
                    System.out.println("      - Best performance for CPU-intensive tasks");
                }
                case MIXED -> {
                    System.out.println("    ✓ Choice depends on I/O ratio:");
                    System.out.println("      - High I/O ratio (>70%): Virtual Threads");
                    System.out.println("      - Balanced: Thread Pool");
                    System.out.println("      - High CPU ratio (>70%): Thread Pool");
                }
            }
        }

        public void printScalabilityComparison(Map<Integer, List<ComparisonResult>> scalabilityData) {
            System.out.println("\n" + "═".repeat(70));
            System.out.println("  SCALABILITY COMPARISON");
            System.out.println("═".repeat(70));

            System.out.println("\n  Thread Count | Platform Memory | Virtual Memory | Pool Memory");
            System.out.println("  " + "-".repeat(66));

            scalabilityData.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> {
                        int threadCount = entry.getKey();
                        List<ComparisonResult> results = entry.getValue();

                        long platformMem = results.stream()
                                .filter(r -> r.threadModel.contains("Platform"))
                                .mapToLong(r -> r.peakMemoryMB)
                                .findFirst()
                                .orElse(0);

                        long virtualMem = results.stream()
                                .filter(r -> r.threadModel.contains("Virtual"))
                                .mapToLong(r -> r.peakMemoryMB)
                                .findFirst()
                                .orElse(0);

                        long poolMem = results.stream()
                                .filter(r -> r.threadModel.contains("Pool"))
                                .mapToLong(r -> r.peakMemoryMB)
                                .findFirst()
                                .orElse(0);

                        System.out.println(String.format("  %12d | %15d | %14d | %11d MB",
                                threadCount, platformMem, virtualMem, poolMem));
                    });
        }
    }

    /**
     * 종합 테스트
     */
    static class ComprehensiveTest {

        public void runComparison() throws Exception {
            System.out.println("\n╔═══════════════════════════════════════════════════════════════════╗");
            System.out.println("║      RESOURCE COMPARISON - Platform vs Virtual vs Pool           ║");
            System.out.println("╚═══════════════════════════════════════════════════════════════════╝");

            ResourceComparison comparison = new ResourceComparison();
            ComparisonAnalyzer analyzer = new ComparisonAnalyzer();

            // Test 1: I/O Bound
            System.out.println("\n" + "═".repeat(70));
            System.out.println("TEST 1: I/O BOUND WORKLOAD (1,000 threads)");
            System.out.println("═".repeat(70));

            List<ComparisonResult> ioResults = new ArrayList<>();
            Workload ioWorkload = new Workload(WorkloadType.IO_BOUND, 0);

            // Platform Thread (제한)
            System.out.println("\nTesting Platform Thread...");
            ioResults.add(comparison.testPlatformThread(1000, ioWorkload));

            // Virtual Thread
            System.out.println("Testing Virtual Thread...");
            ioResults.add(comparison.testVirtualThread(1000, ioWorkload));

            // Thread Pool
            System.out.println("Testing Thread Pool...");
            ioResults.add(comparison.testThreadPool(1000, ioWorkload));

            ioResults.forEach(ComparisonResult::print);
            analyzer.compareResults(ioResults);
            analyzer.printRecommendations(ioResults, WorkloadType.IO_BOUND);

            // Test 2: CPU Bound
            System.out.println("\n\n" + "═".repeat(70));
            System.out.println("TEST 2: CPU BOUND WORKLOAD (100 threads)");
            System.out.println("═".repeat(70));

            List<ComparisonResult> cpuResults = new ArrayList<>();
            Workload cpuWorkload = new Workload(WorkloadType.CPU_BOUND, 100000);

            System.out.println("\nTesting Platform Thread...");
            cpuResults.add(comparison.testPlatformThread(100, cpuWorkload));

            System.out.println("Testing Virtual Thread...");
            cpuResults.add(comparison.testVirtualThread(100, cpuWorkload));

            System.out.println("Testing Thread Pool...");
            cpuResults.add(comparison.testThreadPool(100, cpuWorkload));

            cpuResults.forEach(ComparisonResult::print);
            analyzer.compareResults(cpuResults);
            analyzer.printRecommendations(cpuResults, WorkloadType.CPU_BOUND);

            // Test 3: Scalability Test
            System.out.println("\n\n" + "═".repeat(70));
            System.out.println("TEST 3: SCALABILITY TEST (10 → 1,000 → 10,000 threads)");
            System.out.println("═".repeat(70));

            Map<Integer, List<ComparisonResult>> scalabilityData = new LinkedHashMap<>();
            int[] threadCounts = {10, 100, 1000};
            Workload mixedWorkload = new Workload(WorkloadType.MIXED, 10000);

            for (int count : threadCounts) {
                System.out.println("\nTesting with " + count + " threads...");
                List<ComparisonResult> results = new ArrayList<>();

                if (count <= 1000) {
                    results.add(comparison.testPlatformThread(count, mixedWorkload));
                }
                results.add(comparison.testVirtualThread(count, mixedWorkload));
                results.add(comparison.testThreadPool(count, mixedWorkload));

                scalabilityData.put(count, results);
            }

            analyzer.printScalabilityComparison(scalabilityData);

            // Final Summary
            printFinalSummary();
        }

        private void printFinalSummary() {
            System.out.println("\n" + "═".repeat(70));
            System.out.println("  FINAL SUMMARY");
            System.out.println("═".repeat(70));

            System.out.println("\n  Platform Threads:");
            System.out.println("    ✓ Pros: Direct OS mapping, good for CPU-bound");
            System.out.println("    ✗ Cons: High memory footprint, limited scalability");
            System.out.println("    → Use: CPU-intensive tasks, legacy code");

            System.out.println("\n  Virtual Threads:");
            System.out.println("    ✓ Pros: Lightweight, excellent for I/O, scales to millions");
            System.out.println("    ✗ Cons: Overhead for CPU-bound, requires Java 21+");
            System.out.println("    → Use: I/O-heavy applications, web servers, microservices");

            System.out.println("\n  Thread Pool:");
            System.out.println("    ✓ Pros: Resource control, thread reuse, stable performance");
            System.out.println("    ✗ Cons: Queue management, potential contention");
            System.out.println("    → Use: Controlled concurrency, mixed workloads");

            System.out.println("\n" + "═".repeat(70));
        }
    }

    public static void main(String[] args) throws Exception {
        ComprehensiveTest test = new ComprehensiveTest();
        test.runComparison();

        System.out.println("\n╔═══════════════════════════════════════════════════════════════════╗");
        System.out.println("║         COMPARISON COMPLETED                                      ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════╝");
    }
}