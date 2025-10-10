package com.study.thread.comparison.performance;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 스레드 생성 시간 벤치마크
 *
 * 다양한 스레드 생성 방식의 생성 시간을 정밀하게 측정합니다:
 * 1. Platform Thread (OS Thread)
 * 2. Virtual Thread (Java 21+)
 * 3. Thread Pool (Fixed, Cached, WorkStealing)
 * 4. ForkJoinPool
 *
 * 측정 항목:
 * - 생성 시간 (Creation Time)
 * - 시작 시간 (Startup Time)
 * - 전체 실행 시간 (Total Execution Time)
 * - 메모리 사용량
 */
public class CreationBenchmark {

    private static final int WARMUP_ITERATIONS = 3;
    private static final int MEASUREMENT_ITERATIONS = 5;

    /**
     * 벤치마크 결과
     */
    static class BenchmarkResult {
        final String method;
        final int threadCount;
        final long creationTimeNanos;
        final long startupTimeNanos;
        final long executionTimeNanos;
        final long totalTimeNanos;
        final long memoryUsedBytes;
        final double avgTimePerThread;

        BenchmarkResult(String method, int threadCount,
                        long creationTimeNanos, long startupTimeNanos,
                        long executionTimeNanos, long totalTimeNanos,
                        long memoryUsedBytes) {
            this.method = method;
            this.threadCount = threadCount;
            this.creationTimeNanos = creationTimeNanos;
            this.startupTimeNanos = startupTimeNanos;
            this.executionTimeNanos = executionTimeNanos;
            this.totalTimeNanos = totalTimeNanos;
            this.memoryUsedBytes = memoryUsedBytes;
            this.avgTimePerThread = (double) totalTimeNanos / threadCount;
        }

        void print() {
            System.out.println("\n  === " + method + " ===");
            System.out.println("  Thread count: " + threadCount);
            System.out.println("  Creation time: " + String.format("%.3f ms", creationTimeNanos / 1_000_000.0));
            System.out.println("  Startup time: " + String.format("%.3f ms", startupTimeNanos / 1_000_000.0));
            System.out.println("  Execution time: " + String.format("%.3f ms", executionTimeNanos / 1_000_000.0));
            System.out.println("  Total time: " + String.format("%.3f ms", totalTimeNanos / 1_000_000.0));
            System.out.println("  Avg per thread: " + String.format("%.3f μs", avgTimePerThread / 1000));
            System.out.println("  Memory used: " + String.format("%.2f MB", memoryUsedBytes / 1024.0 / 1024.0));
        }
    }

    /**
     * 간단한 작업 (CPU + I/O 혼합)
     */
    static class SimpleTask implements Runnable {
        private final AtomicInteger counter;

        SimpleTask(AtomicInteger counter) {
            this.counter = counter;
        }

        @Override
        public void run() {
            // CPU 작업
            long sum = 0;
            for (int i = 0; i < 100; i++) {
                sum += i;
            }

            // I/O 시뮬레이션
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            counter.incrementAndGet();
        }
    }

    /**
     * Platform Thread 벤치마크
     */
    static class PlatformThreadBenchmark {
        public BenchmarkResult measure(int threadCount) throws InterruptedException {
            Runtime runtime = Runtime.getRuntime();
            System.gc();
            Thread.sleep(100);

            long memBefore = runtime.totalMemory() - runtime.freeMemory();
            AtomicInteger counter = new AtomicInteger(0);

            Instant totalStart = Instant.now();

            // 생성 단계
            Instant creationStart = Instant.now();
            List<Thread> threads = new ArrayList<>();
            for (int i = 0; i < threadCount; i++) {
                Thread thread = new Thread(new SimpleTask(counter));
                threads.add(thread);
            }
            long creationTime = Duration.between(creationStart, Instant.now()).toNanos();

            // 시작 단계
            Instant startupStart = Instant.now();
            for (Thread thread : threads) {
                thread.start();
            }
            long startupTime = Duration.between(startupStart, Instant.now()).toNanos();

            // 실행 대기
            Instant executionStart = Instant.now();
            for (Thread thread : threads) {
                thread.join();
            }
            long executionTime = Duration.between(executionStart, Instant.now()).toNanos();

            long totalTime = Duration.between(totalStart, Instant.now()).toNanos();

            Thread.sleep(100);
            long memAfter = runtime.totalMemory() - runtime.freeMemory();
            long memUsed = Math.max(0, memAfter - memBefore);

            return new BenchmarkResult(
                    "Platform Thread",
                    threadCount,
                    creationTime,
                    startupTime,
                    executionTime,
                    totalTime,
                    memUsed
            );
        }
    }

    /**
     * Virtual Thread 벤치마크
     */
    static class VirtualThreadBenchmark {
        public BenchmarkResult measure(int threadCount) throws InterruptedException {
            Runtime runtime = Runtime.getRuntime();
            System.gc();
            Thread.sleep(100);

            long memBefore = runtime.totalMemory() - runtime.freeMemory();
            AtomicInteger counter = new AtomicInteger(0);

            Instant totalStart = Instant.now();

            try {
                // 생성 단계
                Instant creationStart = Instant.now();
                List<Thread> threads = new ArrayList<>();
                for (int i = 0; i < threadCount; i++) {
                    Thread thread = Thread.ofVirtual().unstarted(new SimpleTask(counter));
                    threads.add(thread);
                }
                long creationTime = Duration.between(creationStart, Instant.now()).toNanos();

                // 시작 단계
                Instant startupStart = Instant.now();
                for (Thread thread : threads) {
                    thread.start();
                }
                long startupTime = Duration.between(startupStart, Instant.now()).toNanos();

                // 실행 대기
                Instant executionStart = Instant.now();
                for (Thread thread : threads) {
                    thread.join();
                }
                long executionTime = Duration.between(executionStart, Instant.now()).toNanos();

                long totalTime = Duration.between(totalStart, Instant.now()).toNanos();

                Thread.sleep(100);
                long memAfter = runtime.totalMemory() - runtime.freeMemory();
                long memUsed = Math.max(0, memAfter - memBefore);

                return new BenchmarkResult(
                        "Virtual Thread",
                        threadCount,
                        creationTime,
                        startupTime,
                        executionTime,
                        totalTime,
                        memUsed
                );
            } catch (Exception e) {
                // Java 21 미만
                return new BenchmarkResult(
                        "Virtual Thread (Not Available)",
                        threadCount, 0, 0, 0, 0, 0
                );
            }
        }
    }

    /**
     * Fixed Thread Pool 벤치마크
     */
    static class FixedThreadPoolBenchmark {
        public BenchmarkResult measure(int threadCount) throws Exception {
            Runtime runtime = Runtime.getRuntime();
            System.gc();
            Thread.sleep(100);

            long memBefore = runtime.totalMemory() - runtime.freeMemory();
            AtomicInteger counter = new AtomicInteger(0);

            int poolSize = Runtime.getRuntime().availableProcessors();
            ExecutorService executor = Executors.newFixedThreadPool(poolSize);

            Instant totalStart = Instant.now();

            // 생성 및 제출 단계
            Instant creationStart = Instant.now();
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < threadCount; i++) {
                Future<?> future = executor.submit(new SimpleTask(counter));
                futures.add(future);
            }
            long creationTime = Duration.between(creationStart, Instant.now()).toNanos();

            // 실행 대기 (시작 시간은 pool에서 측정 어려움)
            Instant executionStart = Instant.now();
            for (Future<?> future : futures) {
                future.get();
            }
            long executionTime = Duration.between(executionStart, Instant.now()).toNanos();

            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);

            long totalTime = Duration.between(totalStart, Instant.now()).toNanos();

            Thread.sleep(100);
            long memAfter = runtime.totalMemory() - runtime.freeMemory();
            long memUsed = Math.max(0, memAfter - memBefore);

            return new BenchmarkResult(
                    "Fixed Thread Pool (" + poolSize + " threads)",
                    threadCount,
                    creationTime,
                    0, // startup time은 pool에서 의미 없음
                    executionTime,
                    totalTime,
                    memUsed
            );
        }
    }

    /**
     * Cached Thread Pool 벤치마크
     */
    static class CachedThreadPoolBenchmark {
        public BenchmarkResult measure(int threadCount) throws Exception {
            Runtime runtime = Runtime.getRuntime();
            System.gc();
            Thread.sleep(100);

            long memBefore = runtime.totalMemory() - runtime.freeMemory();
            AtomicInteger counter = new AtomicInteger(0);

            ExecutorService executor = Executors.newCachedThreadPool();

            Instant totalStart = Instant.now();

            Instant creationStart = Instant.now();
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < threadCount; i++) {
                Future<?> future = executor.submit(new SimpleTask(counter));
                futures.add(future);
            }
            long creationTime = Duration.between(creationStart, Instant.now()).toNanos();

            Instant executionStart = Instant.now();
            for (Future<?> future : futures) {
                future.get();
            }
            long executionTime = Duration.between(executionStart, Instant.now()).toNanos();

            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);

            long totalTime = Duration.between(totalStart, Instant.now()).toNanos();

            Thread.sleep(100);
            long memAfter = runtime.totalMemory() - runtime.freeMemory();
            long memUsed = Math.max(0, memAfter - memBefore);

            return new BenchmarkResult(
                    "Cached Thread Pool",
                    threadCount,
                    creationTime,
                    0,
                    executionTime,
                    totalTime,
                    memUsed
            );
        }
    }

    /**
     * Work Stealing Pool 벤치마크
     */
    static class WorkStealingPoolBenchmark {
        public BenchmarkResult measure(int threadCount) throws Exception {
            Runtime runtime = Runtime.getRuntime();
            System.gc();
            Thread.sleep(100);

            long memBefore = runtime.totalMemory() - runtime.freeMemory();
            AtomicInteger counter = new AtomicInteger(0);

            ExecutorService executor = Executors.newWorkStealingPool();

            Instant totalStart = Instant.now();

            Instant creationStart = Instant.now();
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < threadCount; i++) {
                Future<?> future = executor.submit(new SimpleTask(counter));
                futures.add(future);
            }
            long creationTime = Duration.between(creationStart, Instant.now()).toNanos();

            Instant executionStart = Instant.now();
            for (Future<?> future : futures) {
                future.get();
            }
            long executionTime = Duration.between(executionStart, Instant.now()).toNanos();

            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);

            long totalTime = Duration.between(totalStart, Instant.now()).toNanos();

            Thread.sleep(100);
            long memAfter = runtime.totalMemory() - runtime.freeMemory();
            long memUsed = Math.max(0, memAfter - memBefore);

            return new BenchmarkResult(
                    "Work Stealing Pool",
                    threadCount,
                    creationTime,
                    0,
                    executionTime,
                    totalTime,
                    memUsed
            );
        }
    }

    /**
     * 벤치마크 실행 및 분석
     */
    static class BenchmarkRunner {
        public void runBenchmark(int threadCount) throws Exception {
            System.out.println("\n" + "=".repeat(70));
            System.out.println("  Thread Creation Benchmark - " + threadCount + " threads");
            System.out.println("=".repeat(70));

            List<BenchmarkResult> results = new ArrayList<>();

            // Warmup
            System.out.println("\n[Warmup Phase]");
            for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                System.out.print("  Warmup iteration " + (i + 1) + "/" + WARMUP_ITERATIONS + "...");
                new PlatformThreadBenchmark().measure(Math.min(100, threadCount));
                System.out.println(" Done");
                System.gc();
                Thread.sleep(200);
            }

            System.out.println("\n[Measurement Phase]");

            // 1. Platform Thread
            System.out.println("\n  Testing Platform Threads...");
            List<BenchmarkResult> platformResults = new ArrayList<>();
            for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
                platformResults.add(new PlatformThreadBenchmark().measure(threadCount));
                System.gc();
                Thread.sleep(200);
            }
            results.add(getMedianResult(platformResults));

            // 2. Virtual Thread
            System.out.println("  Testing Virtual Threads...");
            List<BenchmarkResult> virtualResults = new ArrayList<>();
            for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
                virtualResults.add(new VirtualThreadBenchmark().measure(threadCount));
                System.gc();
                Thread.sleep(200);
            }
            BenchmarkResult virtualMedian = getMedianResult(virtualResults);
            if (virtualMedian.totalTimeNanos > 0) {
                results.add(virtualMedian);
            }

            // 3. Fixed Thread Pool
            System.out.println("  Testing Fixed Thread Pool...");
            List<BenchmarkResult> fixedResults = new ArrayList<>();
            for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
                fixedResults.add(new FixedThreadPoolBenchmark().measure(threadCount));
                System.gc();
                Thread.sleep(200);
            }
            results.add(getMedianResult(fixedResults));

            // 4. Cached Thread Pool
            if (threadCount <= 1000) { // Cached pool은 많은 스레드에 비효율적
                System.out.println("  Testing Cached Thread Pool...");
                List<BenchmarkResult> cachedResults = new ArrayList<>();
                for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
                    cachedResults.add(new CachedThreadPoolBenchmark().measure(threadCount));
                    System.gc();
                    Thread.sleep(200);
                }
                results.add(getMedianResult(cachedResults));
            }

            // 5. Work Stealing Pool
            System.out.println("  Testing Work Stealing Pool...");
            List<BenchmarkResult> wsResults = new ArrayList<>();
            for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
                wsResults.add(new WorkStealingPoolBenchmark().measure(threadCount));
                System.gc();
                Thread.sleep(200);
            }
            results.add(getMedianResult(wsResults));

            // 결과 출력
            printResults(results);
        }

        private BenchmarkResult getMedianResult(List<BenchmarkResult> results) {
            results.sort((a, b) -> Long.compare(a.totalTimeNanos, b.totalTimeNanos));
            return results.get(results.size() / 2);
        }

        private void printResults(List<BenchmarkResult> results) {
            System.out.println("\n" + "=".repeat(70));
            System.out.println("  BENCHMARK RESULTS");
            System.out.println("=".repeat(70));

            for (BenchmarkResult result : results) {
                result.print();
            }

            // 비교 분석
            System.out.println("\n" + "=".repeat(70));
            System.out.println("  PERFORMANCE COMPARISON");
            System.out.println("=".repeat(70));

            BenchmarkResult fastest = results.stream()
                    .min((a, b) -> Long.compare(a.totalTimeNanos, b.totalTimeNanos))
                    .orElse(null);

            BenchmarkResult leastMemory = results.stream()
                    .min((a, b) -> Long.compare(a.memoryUsedBytes, b.memoryUsedBytes))
                    .orElse(null);

            System.out.println("\n  Total Time Ranking:");
            results.stream()
                    .sorted((a, b) -> Long.compare(a.totalTimeNanos, b.totalTimeNanos))
                    .forEach(r -> {
                        double speedup = (double) fastest.totalTimeNanos / r.totalTimeNanos;
                        String badge = r == fastest ? " 🏆 FASTEST" : "";
                        System.out.println(String.format("    %s: %.3f ms (%.2fx)%s",
                                r.method,
                                r.totalTimeNanos / 1_000_000.0,
                                1 / speedup,
                                badge));
                    });

            System.out.println("\n  Memory Usage Ranking:");
            results.stream()
                    .sorted((a, b) -> Long.compare(a.memoryUsedBytes, b.memoryUsedBytes))
                    .forEach(r -> {
                        String badge = r == leastMemory ? " 💾 LEAST MEMORY" : "";
                        System.out.println(String.format("    %s: %.2f MB%s",
                                r.method,
                                r.memoryUsedBytes / 1024.0 / 1024.0,
                                badge));
                    });

            System.out.println("\n  Avg Time per Thread Ranking:");
            results.stream()
                    .sorted((a, b) -> Double.compare(a.avgTimePerThread, b.avgTimePerThread))
                    .forEach(r -> {
                        System.out.println(String.format("    %s: %.3f μs",
                                r.method,
                                r.avgTimePerThread / 1000));
                    });
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("╔═══════════════════════════════════════════════════════════════════╗");
        System.out.println("║         THREAD CREATION BENCHMARK                                 ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════╝");

        BenchmarkRunner runner = new BenchmarkRunner();

        // 소규모 테스트
        runner.runBenchmark(100);

        // 중규모 테스트
        runner.runBenchmark(1000);

        // 대규모 테스트 (Virtual Thread 강점)
        System.out.println("\n\n" + "=".repeat(70));
        System.out.println("  LARGE SCALE TEST (10,000 threads)");
        System.out.println("  Note: Only Virtual Thread and Thread Pools tested");
        System.out.println("=".repeat(70));

        runner.runBenchmark(10000);

        System.out.println("\n╔═══════════════════════════════════════════════════════════════════╗");
        System.out.println("║         BENCHMARK COMPLETED                                       ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════╝");
    }
}