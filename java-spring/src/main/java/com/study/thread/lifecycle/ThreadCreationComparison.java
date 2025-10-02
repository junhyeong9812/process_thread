package com.study.thread.lifecycle;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 스레드 생성 방식 비교
 *
 * 다양한 스레드 생성 방식의 성능과 특성을 비교합니다:
 * 1. Platform Thread (직접 생성)
 * 2. Virtual Thread (Java 21+)
 * 3. Thread Pool (ExecutorService)
 * 4. ForkJoinPool
 * 5. Cached Thread Pool
 */
public class ThreadCreationComparison {

    private static final int WARMUP_ITERATIONS = 3;

    /**
     * 벤치마크 결과
     */
    static class BenchmarkResult {
        final String method;
        final int threadCount;
        final long creationTimeNanos;
        final long executionTimeNanos;
        final long totalTimeNanos;
        final int completedTasks;
        final long memoryUsedBytes;

        BenchmarkResult(String method, int threadCount, long creationTimeNanos,
                        long executionTimeNanos, long totalTimeNanos,
                        int completedTasks, long memoryUsedBytes) {
            this.method = method;
            this.threadCount = threadCount;
            this.creationTimeNanos = creationTimeNanos;
            this.executionTimeNanos = executionTimeNanos;
            this.totalTimeNanos = totalTimeNanos;
            this.completedTasks = completedTasks;
            this.memoryUsedBytes = memoryUsedBytes;
        }

        void print() {
            System.out.println("  Method: " + method);
            System.out.println("  Thread count: " + threadCount);
            System.out.println("  Creation time: " + creationTimeNanos / 1_000_000 + " ms");
            System.out.println("  Execution time: " + executionTimeNanos / 1_000_000 + " ms");
            System.out.println("  Total time: " + totalTimeNanos / 1_000_000 + " ms");
            System.out.println("  Avg per thread: " + totalTimeNanos / threadCount / 1000 + " μs");
            System.out.println("  Completed tasks: " + completedTasks);
            System.out.println("  Memory used: " + memoryUsedBytes / 1024 / 1024 + " MB");
            System.out.println();
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== Thread Creation Method Comparison ===\n");

        checkJavaVersion();

        // Warmup
        System.out.println("Warming up JVM...");
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            benchmarkPlatformThreads(100, false);
            benchmarkThreadPool(100, false);
            System.gc();
            Thread.sleep(100);
        }
        System.out.println("Warmup completed\n");

        // 소규모 테스트 (100개)
        System.out.println("=== Small Scale Test (100 threads) ===\n");
        runComparison(100);

        // 중규모 테스트 (1,000개)
        System.out.println("\n=== Medium Scale Test (1,000 threads) ===\n");
        runComparison(1000);

        // 대규모 테스트 (10,000개) - Virtual Thread만
        System.out.println("\n=== Large Scale Test (10,000 threads) ===\n");
        System.out.println("Note: Only Virtual Thread and Thread Pool tested for 10,000 threads");
        System.out.println("(Platform threads would consume too many OS resources)\n");

        BenchmarkResult virtualLarge = benchmarkVirtualThreads(10000, true);
        BenchmarkResult poolLarge = benchmarkThreadPool(10000, true);

        System.out.println("\n=== Comparison Summary ===");
        compareResults(List.of(virtualLarge, poolLarge));

        System.out.println("\n=== Demo Completed ===");
    }

    private static void checkJavaVersion() {
        String version = System.getProperty("java.version");
        System.out.println("Java Version: " + version);

        try {
            Thread.ofVirtual().start(() -> {}).join();
            System.out.println("Virtual Thread Support: Available ✓\n");
        } catch (Exception e) {
            System.out.println("Virtual Thread Support: Not Available (Java 21+ required)\n");
        }
    }

    private static void runComparison(int threadCount) throws Exception {
        List<BenchmarkResult> results = new ArrayList<>();

        // 1. Platform Thread
        System.out.println("1. Testing Platform Threads...");
        results.add(benchmarkPlatformThreads(threadCount, true));
        System.gc();
        Thread.sleep(500);

        // 2. Virtual Thread
        System.out.println("2. Testing Virtual Threads...");
        results.add(benchmarkVirtualThreads(threadCount, true));
        System.gc();
        Thread.sleep(500);

        // 3. Fixed Thread Pool
        System.out.println("3. Testing Fixed Thread Pool...");
        results.add(benchmarkThreadPool(threadCount, true));
        System.gc();
        Thread.sleep(500);

        // 4. Cached Thread Pool
        System.out.println("4. Testing Cached Thread Pool...");
        results.add(benchmarkCachedThreadPool(threadCount, true));
        System.gc();
        Thread.sleep(500);

        // 5. ForkJoinPool
        System.out.println("5. Testing ForkJoinPool...");
        results.add(benchmarkForkJoinPool(threadCount, true));
        System.gc();
        Thread.sleep(500);

        // 비교 결과 출력
        System.out.println("=== Comparison Summary ===");
        compareResults(results);
    }

    private static BenchmarkResult benchmarkPlatformThreads(int count, boolean print)
            throws InterruptedException {
        Runtime runtime = Runtime.getRuntime();
        System.gc();
        long memBefore = runtime.totalMemory() - runtime.freeMemory();

        AtomicInteger completedTasks = new AtomicInteger(0);

        Instant start = Instant.now();

        // 생성 시간 측정
        Instant createStart = Instant.now();
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Thread thread = new Thread(() -> {
                doWork();
                completedTasks.incrementAndGet();
            });
            threads.add(thread);
        }
        long createTime = Duration.between(createStart, Instant.now()).toNanos();

        // 실행 시간 측정
        Instant execStart = Instant.now();
        for (Thread thread : threads) {
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }
        long execTime = Duration.between(execStart, Instant.now()).toNanos();

        long totalTime = Duration.between(start, Instant.now()).toNanos();

        long memAfter = runtime.totalMemory() - runtime.freeMemory();
        long memUsed = memAfter - memBefore;

        BenchmarkResult result = new BenchmarkResult(
                "Platform Thread",
                count,
                createTime,
                execTime,
                totalTime,
                completedTasks.get(),
                memUsed
        );

        if (print) {
            result.print();
        }

        return result;
    }

    private static BenchmarkResult benchmarkVirtualThreads(int count, boolean print)
            throws InterruptedException {
        Runtime runtime = Runtime.getRuntime();
        System.gc();
        long memBefore = runtime.totalMemory() - runtime.freeMemory();

        AtomicInteger completedTasks = new AtomicInteger(0);

        Instant start = Instant.now();

        // 생성 시간 측정
        Instant createStart = Instant.now();
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Thread thread = Thread.ofVirtual().unstarted(() -> {
                doWork();
                completedTasks.incrementAndGet();
            });
            threads.add(thread);
        }
        long createTime = Duration.between(createStart, Instant.now()).toNanos();

        // 실행 시간 측정
        Instant execStart = Instant.now();
        for (Thread thread : threads) {
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }
        long execTime = Duration.between(execStart, Instant.now()).toNanos();

        long totalTime = Duration.between(start, Instant.now()).toNanos();

        long memAfter = runtime.totalMemory() - runtime.freeMemory();
        long memUsed = memAfter - memBefore;

        BenchmarkResult result = new BenchmarkResult(
                "Virtual Thread",
                count,
                createTime,
                execTime,
                totalTime,
                completedTasks.get(),
                memUsed
        );

        if (print) {
            result.print();
        }

        return result;
    }

    private static BenchmarkResult benchmarkThreadPool(int count, boolean print)
            throws Exception {
        Runtime runtime = Runtime.getRuntime();
        System.gc();
        long memBefore = runtime.totalMemory() - runtime.freeMemory();

        AtomicInteger completedTasks = new AtomicInteger(0);

        Instant start = Instant.now();

        // 고정 크기 스레드 풀 (CPU 코어 수)
        int poolSize = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(poolSize);

        Instant createStart = Instant.now();
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Future<?> future = executor.submit(() -> {
                doWork();
                completedTasks.incrementAndGet();
            });
            futures.add(future);
        }
        long createTime = Duration.between(createStart, Instant.now()).toNanos();

        Instant execStart = Instant.now();
        for (Future<?> future : futures) {
            future.get();
        }
        long execTime = Duration.between(execStart, Instant.now()).toNanos();

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        long totalTime = Duration.between(start, Instant.now()).toNanos();

        long memAfter = runtime.totalMemory() - runtime.freeMemory();
        long memUsed = memAfter - memBefore;

        BenchmarkResult result = new BenchmarkResult(
                "Fixed Thread Pool (" + poolSize + " threads)",
                count,
                createTime,
                execTime,
                totalTime,
                completedTasks.get(),
                memUsed
        );

        if (print) {
            result.print();
        }

        return result;
    }

    private static BenchmarkResult benchmarkCachedThreadPool(int count, boolean print)
            throws Exception {
        Runtime runtime = Runtime.getRuntime();
        System.gc();
        long memBefore = runtime.totalMemory() - runtime.freeMemory();

        AtomicInteger completedTasks = new AtomicInteger(0);

        Instant start = Instant.now();

        ExecutorService executor = Executors.newCachedThreadPool();

        Instant createStart = Instant.now();
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Future<?> future = executor.submit(() -> {
                doWork();
                completedTasks.incrementAndGet();
            });
            futures.add(future);
        }
        long createTime = Duration.between(createStart, Instant.now()).toNanos();

        Instant execStart = Instant.now();
        for (Future<?> future : futures) {
            future.get();
        }
        long execTime = Duration.between(execStart, Instant.now()).toNanos();

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        long totalTime = Duration.between(start, Instant.now()).toNanos();

        long memAfter = runtime.totalMemory() - runtime.freeMemory();
        long memUsed = memAfter - memBefore;

        BenchmarkResult result = new BenchmarkResult(
                "Cached Thread Pool",
                count,
                createTime,
                execTime,
                totalTime,
                completedTasks.get(),
                memUsed
        );

        if (print) {
            result.print();
        }

        return result;
    }

    private static BenchmarkResult benchmarkForkJoinPool(int count, boolean print)
            throws Exception {
        Runtime runtime = Runtime.getRuntime();
        System.gc();
        long memBefore = runtime.totalMemory() - runtime.freeMemory();

        AtomicInteger completedTasks = new AtomicInteger(0);

        Instant start = Instant.now();

        ForkJoinPool pool = ForkJoinPool.commonPool();

        Instant createStart = Instant.now();
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Future<?> future = pool.submit(() -> {
                doWork();
                completedTasks.incrementAndGet();
            });
            futures.add(future);
        }
        long createTime = Duration.between(createStart, Instant.now()).toNanos();

        Instant execStart = Instant.now();
        for (Future<?> future : futures) {
            future.get();
        }
        long execTime = Duration.between(execStart, Instant.now()).toNanos();

        long totalTime = Duration.between(start, Instant.now()).toNanos();

        long memAfter = runtime.totalMemory() - runtime.freeMemory();
        long memUsed = memAfter - memBefore;

        BenchmarkResult result = new BenchmarkResult(
                "ForkJoinPool (common)",
                count,
                createTime,
                execTime,
                totalTime,
                completedTasks.get(),
                memUsed
        );

        if (print) {
            result.print();
        }

        return result;
    }

    /**
     * 간단한 작업 시뮬레이션
     */
    private static void doWork() {
        try {
            // CPU 바운드 작업
            long sum = 0;
            for (int i = 0; i < 1000; i++) {
                sum += i;
            }

            // I/O 바운드 작업 시뮬레이션
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 결과 비교 및 분석
     */
    private static void compareResults(List<BenchmarkResult> results) {
        if (results.isEmpty()) return;

        // 가장 빠른 결과 찾기
        BenchmarkResult fastest = results.stream()
                .min((a, b) -> Long.compare(a.totalTimeNanos, b.totalTimeNanos))
                .orElse(null);

        BenchmarkResult leastMemory = results.stream()
                .min((a, b) -> Long.compare(a.memoryUsedBytes, b.memoryUsedBytes))
                .orElse(null);

        System.out.println("\nPerformance Ranking (by total time):");
        results.stream()
                .sorted((a, b) -> Long.compare(a.totalTimeNanos, b.totalTimeNanos))
                .forEach(r -> {
                    double speedup = (double) fastest.totalTimeNanos / r.totalTimeNanos;
                    System.out.println("  " + r.method + ": " +
                            r.totalTimeNanos / 1_000_000 + " ms" +
                            (r == fastest ? " (fastest)" :
                                    String.format(" (%.2fx slower)", 1/speedup)));
                });

        System.out.println("\nMemory Usage Ranking:");
        results.stream()
                .sorted((a, b) -> Long.compare(a.memoryUsedBytes, b.memoryUsedBytes))
                .forEach(r -> {
                    System.out.println("  " + r.method + ": " +
                            r.memoryUsedBytes / 1024 / 1024 + " MB" +
                            (r == leastMemory ? " (least memory)" : ""));
                });

        System.out.println("\nKey Insights:");
        System.out.println("  - Virtual Threads: Best for I/O-bound tasks with high concurrency");
        System.out.println("  - Platform Threads: Best for CPU-bound tasks, limited scalability");
        System.out.println("  - Thread Pool: Good balance, reuses threads efficiently");
        System.out.println("  - ForkJoinPool: Optimized for divide-and-conquer algorithms");
    }
}