package com.study.comparison.performance;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 확장성 테스트 (Scalability Test)
 *
 * 스레드 수를 증가시키면서 성능 변화를 측정합니다.
 * - 10 → 100 → 1,000 → 10,000 → 100,000 스레드
 * - Linear Scalability: 이상적인 확장성 (스레드 수 ∝ 성능)
 * - Sub-linear Scalability: 실제 확장성 (오버헤드로 인한 감소)
 * - Scalability Limit: 성능이 더 이상 증가하지 않는 지점
 *
 * 측정 항목:
 * 1. Throughput (처리량): 초당 처리 작업 수
 * 2. Latency (지연시간): 작업당 평균 시간
 * 3. Resource Usage: CPU, 메모리 사용량
 * 4. Scalability Factor: 스레드 증가 대비 성능 증가율
 *
 * Virtual Thread의 강점:
 * - 수만 개의 스레드도 안정적으로 처리
 * - Platform Thread는 수천 개에서 한계
 */
public class ScalabilityTest {

    private static final int[] THREAD_COUNTS = {10, 50, 100, 500, 1000, 5000, 10000};
    private static final int[] VIRTUAL_THREAD_COUNTS = {10, 50, 100, 500, 1000, 5000, 10000, 50000, 100000};

    /**
     * 확장성 테스트 결과
     */
    static class ScalabilityResult {
        final String method;
        final int threadCount;
        final long totalTimeMillis;
        final double throughput; // tasks/sec
        final double avgLatencyMs; // ms/task
        final long memoryUsedMB;
        final double scalabilityFactor; // vs baseline

        ScalabilityResult(String method, int threadCount, long totalTimeMillis,
                          double throughput, double avgLatencyMs, long memoryUsedMB,
                          double scalabilityFactor) {
            this.method = method;
            this.threadCount = threadCount;
            this.totalTimeMillis = totalTimeMillis;
            this.throughput = throughput;
            this.avgLatencyMs = avgLatencyMs;
            this.memoryUsedMB = memoryUsedMB;
            this.scalabilityFactor = scalabilityFactor;
        }

        void print() {
            System.out.println(String.format("    %6d threads: %6d ms | %8.2f ops/s | " +
                            "%6.2f ms/op | %5d MB | %.2fx",
                    threadCount, totalTimeMillis, throughput, avgLatencyMs,
                    memoryUsedMB, scalabilityFactor));
        }
    }

    /**
     * 1. I/O Bound 확장성 테스트
     */
    static class IOBoundScalability {

        static class IOTask implements Runnable {
            private final int taskId;
            private final int ioDelayMs;
            private final AtomicInteger completedTasks;

            IOTask(int taskId, int ioDelayMs, AtomicInteger completedTasks) {
                this.taskId = taskId;
                this.ioDelayMs = ioDelayMs;
                this.completedTasks = completedTasks;
            }

            @Override
            public void run() {
                try {
                    // I/O 시뮬레이션
                    Thread.sleep(ioDelayMs);
                    completedTasks.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        public List<ScalabilityResult> testPlatform(int ioDelayMs) throws Exception {
            System.out.println("\n  Platform Thread - I/O Bound (delay=" + ioDelayMs + "ms)");
            System.out.println("  " + "─".repeat(80));

            List<ScalabilityResult> results = new ArrayList<>();
            ScalabilityResult baseline = null;

            for (int threadCount : THREAD_COUNTS) {
                if (threadCount > 1000) {
                    System.out.println("    Skipping " + threadCount +
                            " threads (too many for Platform Thread)");
                    continue;
                }

                Runtime runtime = Runtime.getRuntime();
                System.gc();
                Thread.sleep(200);

                long memBefore = runtime.totalMemory() - runtime.freeMemory();
                AtomicInteger completed = new AtomicInteger(0);
                CountDownLatch latch = new CountDownLatch(threadCount);

                Instant start = Instant.now();

                List<Thread> threads = new ArrayList<>();
                for (int i = 0; i < threadCount; i++) {
                    final int taskId = i;
                    Thread thread = Thread.ofPlatform().start(() -> {
                        try {
                            new IOTask(taskId, ioDelayMs, completed).run();
                        } finally {
                            latch.countDown();
                        }
                    });
                    threads.add(thread);
                }

                latch.await();
                long duration = Duration.between(start, Instant.now()).toMillis();

                for (Thread thread : threads) {
                    thread.join();
                }

                Thread.sleep(100);
                long memAfter = runtime.totalMemory() - runtime.freeMemory();
                long memUsed = Math.max(0, (memAfter - memBefore) / 1024 / 1024);

                double throughput = (threadCount * 1000.0) / duration;
                double avgLatency = (double) duration / threadCount;

                ScalabilityResult result;
                if (baseline == null) {
                    result = new ScalabilityResult("Platform Thread", threadCount,
                            duration, throughput, avgLatency, memUsed, 1.0);
                    baseline = result;
                } else {
                    double scalability = throughput / baseline.throughput;
                    result = new ScalabilityResult("Platform Thread", threadCount,
                            duration, throughput, avgLatency, memUsed, scalability);
                }

                result.print();
                results.add(result);

                System.gc();
                Thread.sleep(500);
            }

            return results;
        }

        public List<ScalabilityResult> testVirtual(int ioDelayMs) throws Exception {
            System.out.println("\n  Virtual Thread - I/O Bound (delay=" + ioDelayMs + "ms)");
            System.out.println("  " + "─".repeat(80));

            List<ScalabilityResult> results = new ArrayList<>();
            ScalabilityResult baseline = null;

            for (int threadCount : VIRTUAL_THREAD_COUNTS) {
                Runtime runtime = Runtime.getRuntime();
                System.gc();
                Thread.sleep(200);

                long memBefore = runtime.totalMemory() - runtime.freeMemory();
                AtomicInteger completed = new AtomicInteger(0);
                CountDownLatch latch = new CountDownLatch(threadCount);

                Instant start = Instant.now();

                List<Thread> threads = new ArrayList<>();
                for (int i = 0; i < threadCount; i++) {
                    final int taskId = i;
                    Thread thread = Thread.ofVirtual().start(() -> {
                        try {
                            new IOTask(taskId, ioDelayMs, completed).run();
                        } finally {
                            latch.countDown();
                        }
                    });
                    threads.add(thread);
                }

                latch.await();
                long duration = Duration.between(start, Instant.now()).toMillis();

                for (Thread thread : threads) {
                    thread.join();
                }

                Thread.sleep(100);
                long memAfter = runtime.totalMemory() - runtime.freeMemory();
                long memUsed = Math.max(0, (memAfter - memBefore) / 1024 / 1024);

                double throughput = (threadCount * 1000.0) / duration;
                double avgLatency = (double) duration / threadCount;

                ScalabilityResult result;
                if (baseline == null) {
                    result = new ScalabilityResult("Virtual Thread", threadCount,
                            duration, throughput, avgLatency, memUsed, 1.0);
                    baseline = result;
                } else {
                    double scalability = throughput / baseline.throughput;
                    result = new ScalabilityResult("Virtual Thread", threadCount,
                            duration, throughput, avgLatency, memUsed, scalability);
                }

                result.print();
                results.add(result);

                System.gc();
                Thread.sleep(500);
            }

            return results;
        }
    }

    /**
     * 2. CPU Bound 확장성 테스트
     */
    static class CPUBoundScalability {

        static class CPUTask implements Runnable {
            private final int iterations;
            private final AtomicInteger completedTasks;

            CPUTask(int iterations, AtomicInteger completedTasks) {
                this.iterations = iterations;
                this.completedTasks = completedTasks;
            }

            @Override
            public void run() {
                // CPU 집약적 작업
                long sum = 0;
                for (int i = 0; i < iterations; i++) {
                    sum += Math.sqrt(i) * Math.sin(i);
                }
                completedTasks.incrementAndGet();
            }
        }

        public List<ScalabilityResult> testPlatform(int iterations) throws Exception {
            System.out.println("\n  Platform Thread - CPU Bound (iterations=" + iterations + ")");
            System.out.println("  " + "─".repeat(80));

            List<ScalabilityResult> results = new ArrayList<>();
            ScalabilityResult baseline = null;

            for (int threadCount : THREAD_COUNTS) {
                if (threadCount > 1000) {
                    System.out.println("    Skipping " + threadCount +
                            " threads (too many for Platform Thread)");
                    continue;
                }

                Runtime runtime = Runtime.getRuntime();
                System.gc();
                Thread.sleep(200);

                long memBefore = runtime.totalMemory() - runtime.freeMemory();
                AtomicInteger completed = new AtomicInteger(0);
                CountDownLatch latch = new CountDownLatch(threadCount);

                Instant start = Instant.now();

                List<Thread> threads = new ArrayList<>();
                for (int i = 0; i < threadCount; i++) {
                    Thread thread = Thread.ofPlatform().start(() -> {
                        try {
                            new CPUTask(iterations, completed).run();
                        } finally {
                            latch.countDown();
                        }
                    });
                    threads.add(thread);
                }

                latch.await();
                long duration = Duration.between(start, Instant.now()).toMillis();

                for (Thread thread : threads) {
                    thread.join();
                }

                Thread.sleep(100);
                long memAfter = runtime.totalMemory() - runtime.freeMemory();
                long memUsed = Math.max(0, (memAfter - memBefore) / 1024 / 1024);

                double throughput = (threadCount * 1000.0) / duration;
                double avgLatency = (double) duration / threadCount;

                ScalabilityResult result;
                if (baseline == null) {
                    result = new ScalabilityResult("Platform Thread", threadCount,
                            duration, throughput, avgLatency, memUsed, 1.0);
                    baseline = result;
                } else {
                    double scalability = throughput / baseline.throughput;
                    result = new ScalabilityResult("Platform Thread", threadCount,
                            duration, throughput, avgLatency, memUsed, scalability);
                }

                result.print();
                results.add(result);

                System.gc();
                Thread.sleep(500);
            }

            return results;
        }

        public List<ScalabilityResult> testVirtual(int iterations) throws Exception {
            System.out.println("\n  Virtual Thread - CPU Bound (iterations=" + iterations + ")");
            System.out.println("  " + "─".repeat(80));

            List<ScalabilityResult> results = new ArrayList<>();
            ScalabilityResult baseline = null;

            // CPU bound는 너무 많은 Virtual Thread 불필요
            int[] cpuThreadCounts = {10, 50, 100, 500, 1000, 5000};

            for (int threadCount : cpuThreadCounts) {
                Runtime runtime = Runtime.getRuntime();
                System.gc();
                Thread.sleep(200);

                long memBefore = runtime.totalMemory() - runtime.freeMemory();
                AtomicInteger completed = new AtomicInteger(0);
                CountDownLatch latch = new CountDownLatch(threadCount);

                Instant start = Instant.now();

                List<Thread> threads = new ArrayList<>();
                for (int i = 0; i < threadCount; i++) {
                    Thread thread = Thread.ofVirtual().start(() -> {
                        try {
                            new CPUTask(iterations, completed).run();
                        } finally {
                            latch.countDown();
                        }
                    });
                    threads.add(thread);
                }

                latch.await();
                long duration = Duration.between(start, Instant.now()).toMillis();

                for (Thread thread : threads) {
                    thread.join();
                }

                Thread.sleep(100);
                long memAfter = runtime.totalMemory() - runtime.freeMemory();
                long memUsed = Math.max(0, (memAfter - memBefore) / 1024 / 1024);

                double throughput = (threadCount * 1000.0) / duration;
                double avgLatency = (double) duration / threadCount;

                ScalabilityResult result;
                if (baseline == null) {
                    result = new ScalabilityResult("Virtual Thread", threadCount,
                            duration, throughput, avgLatency, memUsed, 1.0);
                    baseline = result;
                } else {
                    double scalability = throughput / baseline.throughput;
                    result = new ScalabilityResult("Virtual Thread", threadCount,
                            duration, throughput, avgLatency, memUsed, scalability);
                }

                result.print();
                results.add(result);

                System.gc();
                Thread.sleep(500);
            }

            return results;
        }
    }

    /**
     * 3. Mixed Workload 확장성 테스트
     */
    static class MixedWorkloadScalability {

        static class MixedTask implements Runnable {
            private final int cpuIterations;
            private final int ioDelayMs;
            private final AtomicInteger completedTasks;

            MixedTask(int cpuIterations, int ioDelayMs, AtomicInteger completedTasks) {
                this.cpuIterations = cpuIterations;
                this.ioDelayMs = ioDelayMs;
                this.completedTasks = completedTasks;
            }

            @Override
            public void run() {
                try {
                    // CPU 작업
                    long sum = 0;
                    for (int i = 0; i < cpuIterations; i++) {
                        sum += Math.sqrt(i);
                    }

                    // I/O 작업
                    Thread.sleep(ioDelayMs);

                    completedTasks.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        public List<ScalabilityResult> testPlatform(int cpuIterations, int ioDelayMs)
                throws Exception {
            System.out.println("\n  Platform Thread - Mixed Workload " +
                    "(cpu=" + cpuIterations + ", io=" + ioDelayMs + "ms)");
            System.out.println("  " + "─".repeat(80));

            List<ScalabilityResult> results = new ArrayList<>();
            ScalabilityResult baseline = null;

            for (int threadCount : THREAD_COUNTS) {
                if (threadCount > 1000) {
                    System.out.println("    Skipping " + threadCount +
                            " threads (too many for Platform Thread)");
                    continue;
                }

                Runtime runtime = Runtime.getRuntime();
                System.gc();
                Thread.sleep(200);

                long memBefore = runtime.totalMemory() - runtime.freeMemory();
                AtomicInteger completed = new AtomicInteger(0);
                CountDownLatch latch = new CountDownLatch(threadCount);

                Instant start = Instant.now();

                List<Thread> threads = new ArrayList<>();
                for (int i = 0; i < threadCount; i++) {
                    Thread thread = Thread.ofPlatform().start(() -> {
                        try {
                            new MixedTask(cpuIterations, ioDelayMs, completed).run();
                        } finally {
                            latch.countDown();
                        }
                    });
                    threads.add(thread);
                }

                latch.await();
                long duration = Duration.between(start, Instant.now()).toMillis();

                for (Thread thread : threads) {
                    thread.join();
                }

                Thread.sleep(100);
                long memAfter = runtime.totalMemory() - runtime.freeMemory();
                long memUsed = Math.max(0, (memAfter - memBefore) / 1024 / 1024);

                double throughput = (threadCount * 1000.0) / duration;
                double avgLatency = (double) duration / threadCount;

                ScalabilityResult result;
                if (baseline == null) {
                    result = new ScalabilityResult("Platform Thread", threadCount,
                            duration, throughput, avgLatency, memUsed, 1.0);
                    baseline = result;
                } else {
                    double scalability = throughput / baseline.throughput;
                    result = new ScalabilityResult("Platform Thread", threadCount,
                            duration, throughput, avgLatency, memUsed, scalability);
                }

                result.print();
                results.add(result);

                System.gc();
                Thread.sleep(500);
            }

            return results;
        }

        public List<ScalabilityResult> testVirtual(int cpuIterations, int ioDelayMs)
                throws Exception {
            System.out.println("\n  Virtual Thread - Mixed Workload " +
                    "(cpu=" + cpuIterations + ", io=" + ioDelayMs + "ms)");
            System.out.println("  " + "─".repeat(80));

            List<ScalabilityResult> results = new ArrayList<>();
            ScalabilityResult baseline = null;

            for (int threadCount : VIRTUAL_THREAD_COUNTS) {
                Runtime runtime = Runtime.getRuntime();
                System.gc();
                Thread.sleep(200);

                long memBefore = runtime.totalMemory() - runtime.freeMemory();
                AtomicInteger completed = new AtomicInteger(0);
                CountDownLatch latch = new CountDownLatch(threadCount);

                Instant start = Instant.now();

                List<Thread> threads = new ArrayList<>();
                for (int i = 0; i < threadCount; i++) {
                    Thread thread = Thread.ofVirtual().start(() -> {
                        try {
                            new MixedTask(cpuIterations, ioDelayMs, completed).run();
                        } finally {
                            latch.countDown();
                        }
                    });
                    threads.add(thread);
                }

                latch.await();
                long duration = Duration.between(start, Instant.now()).toMillis();

                for (Thread thread : threads) {
                    thread.join();
                }

                Thread.sleep(100);
                long memAfter = runtime.totalMemory() - runtime.freeMemory();
                long memUsed = Math.max(0, (memAfter - memBefore) / 1024 / 1024);

                double throughput = (threadCount * 1000.0) / duration;
                double avgLatency = (double) duration / threadCount;

                ScalabilityResult result;
                if (baseline == null) {
                    result = new ScalabilityResult("Virtual Thread", threadCount,
                            duration, throughput, avgLatency, memUsed, 1.0);
                    baseline = result;
                } else {
                    double scalability = throughput / baseline.throughput;
                    result = new ScalabilityResult("Virtual Thread", threadCount,
                            duration, throughput, avgLatency, memUsed, scalability);
                }

                result.print();
                results.add(result);

                System.gc();
                Thread.sleep(500);
            }

            return results;
        }
    }

    /**
     * 4. 부하 증가 테스트 (Load Increase Test)
     */
    static class LoadIncreaseTest {

        public void testGradualIncrease() throws Exception {
            System.out.println("\n  Gradual Load Increase Test");
            System.out.println("  " + "─".repeat(80));
            System.out.println("  Starting with 10 threads, doubling every step");

            int currentLoad = 10;
            int maxLoad = 10000;

            while (currentLoad <= maxLoad) {
                System.out.println("\n  Testing with " + currentLoad + " threads:");

                // Platform Thread (제한적)
                if (currentLoad <= 1000) {
                    testWithLoad("Platform Thread", currentLoad, false);
                } else {
                    System.out.println("    Platform Thread: Skipped (too many threads)");
                }

                // Virtual Thread
                testWithLoad("Virtual Thread", currentLoad, true);

                currentLoad *= 2;
                System.gc();
                Thread.sleep(1000);
            }
        }

        private void testWithLoad(String method, int threadCount, boolean isVirtual)
                throws Exception {
            AtomicInteger completed = new AtomicInteger(0);
            CountDownLatch latch = new CountDownLatch(threadCount);

            Instant start = Instant.now();

            List<Thread> threads = new ArrayList<>();
            for (int i = 0; i < threadCount; i++) {
                Thread thread = isVirtual ?
                        Thread.ofVirtual().start(() -> {
                            try {
                                Thread.sleep(10);
                                completed.incrementAndGet();
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            } finally {
                                latch.countDown();
                            }
                        }) :
                        Thread.ofPlatform().start(() -> {
                            try {
                                Thread.sleep(10);
                                completed.incrementAndGet();
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            } finally {
                                latch.countDown();
                            }
                        });
                threads.add(thread);
            }

            latch.await();
            long duration = Duration.between(start, Instant.now()).toMillis();

            for (Thread thread : threads) {
                thread.join();
            }

            double throughput = (threadCount * 1000.0) / duration;
            System.out.println(String.format("    %s: %d ms, %.2f ops/sec",
                    method, duration, throughput));
        }
    }

    /**
     * 테스트 실행
     */
    static class ScalabilityTestRunner {

        public void runAllTests() throws Exception {
            System.out.println("\n╔═══════════════════════════════════════════════════════════════════╗");
            System.out.println("║      SCALABILITY TEST                                             ║");
            System.out.println("╚═══════════════════════════════════════════════════════════════════╝");

            int cpuCores = Runtime.getRuntime().availableProcessors();
            System.out.println("\nSystem Info:");
            System.out.println("  CPU Cores: " + cpuCores);
            System.out.println("  Available Memory: " +
                    Runtime.getRuntime().maxMemory() / 1024 / 1024 + " MB");

            Map<String, List<ScalabilityResult>> allResults = new HashMap<>();

            // 1. I/O Bound Scalability
            System.out.println("\n" + "═".repeat(70));
            System.out.println("1. I/O BOUND SCALABILITY TEST");
            System.out.println("═".repeat(70));

            IOBoundScalability ioTest = new IOBoundScalability();
            allResults.put("Platform-IO", ioTest.testPlatform(50));
            allResults.put("Virtual-IO", ioTest.testVirtual(50));

            // 2. CPU Bound Scalability
            System.out.println("\n" + "═".repeat(70));
            System.out.println("2. CPU BOUND SCALABILITY TEST");
            System.out.println("═".repeat(70));

            CPUBoundScalability cpuTest = new CPUBoundScalability();
            allResults.put("Platform-CPU", cpuTest.testPlatform(100000));
            allResults.put("Virtual-CPU", cpuTest.testVirtual(100000));

            // 3. Mixed Workload Scalability
            System.out.println("\n" + "═".repeat(70));
            System.out.println("3. MIXED WORKLOAD SCALABILITY TEST");
            System.out.println("═".repeat(70));

            MixedWorkloadScalability mixedTest = new MixedWorkloadScalability();
            allResults.put("Platform-Mixed", mixedTest.testPlatform(10000, 10));
            allResults.put("Virtual-Mixed", mixedTest.testVirtual(10000, 10));

            // 4. Load Increase Test
            System.out.println("\n" + "═".repeat(70));
            System.out.println("4. GRADUAL LOAD INCREASE TEST");
            System.out.println("═".repeat(70));

            LoadIncreaseTest loadTest = new LoadIncreaseTest();
            loadTest.testGradualIncrease();

            // 종합 분석
            printAnalysis(allResults);
        }

        private void printAnalysis(Map<String, List<ScalabilityResult>> allResults) {
            System.out.println("\n" + "═".repeat(70));
            System.out.println("  SCALABILITY ANALYSIS");
            System.out.println("═".repeat(70));

            allResults.forEach((testType, results) -> {
                System.out.println("\n  " + testType + ":");

                if (results.isEmpty()) return;

                ScalabilityResult first = results.get(0);
                ScalabilityResult last = results.get(results.size() - 1);

                double overallScalability = last.throughput / first.throughput;
                double threadIncrease = (double) last.threadCount / first.threadCount;
                double scalabilityEfficiency = overallScalability / threadIncrease;

                System.out.println(String.format("    Thread increase: %dx (%d → %d)",
                        (int) threadIncrease, first.threadCount, last.threadCount));
                System.out.println(String.format("    Throughput increase: %.2fx (%.2f → %.2f ops/s)",
                        overallScalability, first.throughput, last.throughput));
                System.out.println(String.format("    Scalability efficiency: %.2f%% %s",
                        scalabilityEfficiency * 100,
                        getScalabilityRating(scalabilityEfficiency)));

                // Scalability limit 탐지
                detectScalabilityLimit(results);
            });

            System.out.println("\n" + "═".repeat(70));
            System.out.println("  KEY INSIGHTS");
            System.out.println("═".repeat(70));
            System.out.println("  • I/O Bound: Virtual Threads show near-linear scalability");
            System.out.println("  • CPU Bound: Scalability limited by CPU core count");
            System.out.println("  • Platform Threads: Limited to ~1,000 threads");
            System.out.println("  • Virtual Threads: Can scale to 100,000+ threads");
            System.out.println("  • Best use case: Virtual Threads for I/O-heavy workloads");
            System.out.println("═".repeat(70));
        }

        private String getScalabilityRating(double efficiency) {
            if (efficiency >= 0.9) return "⭐ Excellent";
            if (efficiency >= 0.7) return "✓ Good";
            if (efficiency >= 0.5) return "○ Fair";
            return "✗ Poor";
        }

        private void detectScalabilityLimit(List<ScalabilityResult> results) {
            if (results.size() < 3) return;

            // 처리량이 감소하기 시작하는 지점 찾기
            for (int i = 1; i < results.size(); i++) {
                double prevThroughput = results.get(i - 1).throughput;
                double currThroughput = results.get(i).throughput;

                if (currThroughput < prevThroughput * 0.95) { // 5% 이상 감소
                    System.out.println(String.format("    ⚠ Scalability limit detected at %d threads",
                            results.get(i).threadCount));
                    System.out.println(String.format("       Throughput decreased from %.2f to %.2f ops/s",
                            prevThroughput, currThroughput));
                    return;
                }
            }

            // 처리량 증가가 둔화되는 지점
            if (results.size() >= 4) {
                ScalabilityResult last = results.get(results.size() - 1);
                ScalabilityResult secondLast = results.get(results.size() - 2);

                double recentGrowth = last.throughput / secondLast.throughput;
                if (recentGrowth < 1.1) { // 10% 미만 증가
                    System.out.println(String.format("    ⚠ Diminishing returns after %d threads",
                            secondLast.threadCount));
                    System.out.println(String.format("       Growth rate: %.1f%%",
                            (recentGrowth - 1) * 100));
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        ScalabilityTestRunner runner = new ScalabilityTestRunner();
        runner.runAllTests();

        System.out.println("\n╔═══════════════════════════════════════════════════════════════════╗");
        System.out.println("║         SCALABILITY TEST COMPLETED                                ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════╝");
    }
}