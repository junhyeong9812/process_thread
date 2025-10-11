package com.study.comparison.concurrency;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 병렬성 분석기 (Parallelism Analyzer)
 *
 * 병렬 실행의 효율성과 확장성을 측정하고 분석합니다:
 * 1. Speedup: 속도 향상 (Sequential vs Parallel)
 * 2. Efficiency: 병렬 효율성 (Speedup / 프로세서 수)
 * 3. Scalability: 확장성 (작업 수 증가에 따른 성능)
 * 4. Amdahl's Law: 이론적 한계 분석
 * 5. Gustafson's Law: 확장 가능한 병렬성
 *
 * 주요 개념:
 * - Speedup = T(1) / T(n)
 * - Efficiency = Speedup / n
 * - Linear Speedup: Speedup = n (이상적)
 * - Sub-linear Speedup: Speedup < n (현실적)
 *
 * 병목 현상:
 * - Synchronization Overhead: 동기화 오버헤드
 * - Load Imbalance: 부하 불균형
 * - Memory Contention: 메모리 경합
 * - False Sharing: 거짓 공유
 */
public class ParallelismAnalyzer {

    private final int processorCount = Runtime.getRuntime().availableProcessors();

    /**
     * 병렬성 분석 결과
     */
    static class ParallelismResult {
        final int workerCount;
        final long executionTimeMs;
        final double speedup;
        final double efficiency;
        final double scalability;
        final long taskCount;
        final Map<String, Object> metrics;

        ParallelismResult(int workerCount, long executionTimeMs, double speedup,
                          double efficiency, double scalability, long taskCount,
                          Map<String, Object> metrics) {
            this.workerCount = workerCount;
            this.executionTimeMs = executionTimeMs;
            this.speedup = speedup;
            this.efficiency = efficiency;
            this.scalability = scalability;
            this.taskCount = taskCount;
            this.metrics = metrics != null ? metrics : new HashMap<>();
        }

        void print() {
            System.out.println(String.format("  Workers: %3d | Time: %6d ms | " +
                            "Speedup: %5.2fx | Efficiency: %5.1f%% | Scalability: %.2f",
                    workerCount, executionTimeMs, speedup, efficiency * 100, scalability));
        }
    }

    /**
     * Amdahl's Law 분석
     */
    static class AmdahlsLaw {

        /**
         * Amdahl's Law 계산
         * Speedup = 1 / ((1 - P) + P/N)
         * P = 병렬화 가능한 부분의 비율
         * N = 프로세서 수
         */
        public double calculateMaxSpeedup(double parallelFraction, int processorCount) {
            double serialFraction = 1.0 - parallelFraction;
            return 1.0 / (serialFraction + (parallelFraction / processorCount));
        }

        public void analyze(double parallelFraction) {
            System.out.println("\n  Amdahl's Law Analysis (P = " +
                    String.format("%.1f%%", parallelFraction * 100) + ")");
            System.out.println("  " + "-".repeat(60));
            System.out.println("  Processors | Max Speedup | Efficiency");
            System.out.println("  " + "-".repeat(60));

            int[] processorCounts = {1, 2, 4, 8, 16, 32, 64, 128};
            for (int n : processorCounts) {
                double speedup = calculateMaxSpeedup(parallelFraction, n);
                double efficiency = speedup / n;
                System.out.println(String.format("  %10d | %11.2fx | %10.1f%%",
                        n, speedup, efficiency * 100));
            }

            System.out.println("\n  Key Insight: Even with 95% parallelizable code,");
            System.out.println("  maximum speedup is limited by the serial portion (5%)");
        }

        public void visualizeSpeedup(double parallelFraction, int maxProcessors) {
            System.out.println("\n  Speedup Visualization (P = " +
                    String.format("%.0f%%", parallelFraction * 100) + ")");
            System.out.println("  " + "-".repeat(60));

            for (int n = 1; n <= maxProcessors; n *= 2) {
                double speedup = calculateMaxSpeedup(parallelFraction, n);
                int barLength = (int) (speedup * 5); // Scale for display
                String bar = "█".repeat(Math.min(barLength, 50));
                System.out.println(String.format("  %3d cores: %s %.2fx",
                        n, bar, speedup));
            }
        }
    }

    /**
     * Gustafson's Law 분석
     */
    static class GustafsonsLaw {

        /**
         * Gustafson's Law 계산
         * Speedup = N - α(N - 1)
         * α = 직렬 부분의 비율
         * N = 프로세서 수
         */
        public double calculateScaledSpeedup(double serialFraction, int processorCount) {
            return processorCount - serialFraction * (processorCount - 1);
        }

        public void analyze(double serialFraction) {
            System.out.println("\n  Gustafson's Law Analysis (Serial = " +
                    String.format("%.1f%%", serialFraction * 100) + ")");
            System.out.println("  " + "-".repeat(60));
            System.out.println("  Processors | Scaled Speedup | Efficiency");
            System.out.println("  " + "-".repeat(60));

            int[] processorCounts = {1, 2, 4, 8, 16, 32, 64, 128};
            for (int n : processorCounts) {
                double speedup = calculateScaledSpeedup(serialFraction, n);
                double efficiency = speedup / n;
                System.out.println(String.format("  %10d | %14.2fx | %10.1f%%",
                        n, speedup, efficiency * 100));
            }

            System.out.println("\n  Key Insight: As problem size scales with processors,");
            System.out.println("  speedup can be nearly linear (better than Amdahl's)");
        }
    }

    /**
     * CPU-bound 작업 분석
     */
    static class CPUBoundAnalysis {

        static class CPUTask implements Callable<Long> {
            private final long iterations;

            CPUTask(long iterations) {
                this.iterations = iterations;
            }

            @Override
            public Long call() {
                long sum = 0;
                for (long i = 0; i < iterations; i++) {
                    sum += Math.sqrt(i) * Math.sin(i);
                }
                return sum;
            }
        }

        public List<ParallelismResult> analyzeParallelism(int maxWorkers, long taskSize)
                throws Exception {
            List<ParallelismResult> results = new ArrayList<>();

            // Sequential baseline
            System.out.println("\n  Measuring sequential baseline...");
            long sequentialTime = measureSequential(taskSize);
            System.out.println("  Sequential time: " + sequentialTime + " ms");

            // Test different worker counts
            for (int workers = 1; workers <= maxWorkers; workers *= 2) {
                System.out.print("  Testing with " + workers + " workers...");

                long parallelTime = measureParallel(workers, taskSize);
                double speedup = (double) sequentialTime / parallelTime;
                double efficiency = speedup / workers;
                double scalability = speedup / workers;

                Map<String, Object> metrics = new HashMap<>();
                metrics.put("Task Size", taskSize);
                metrics.put("Overhead", parallelTime - (sequentialTime / speedup));

                ParallelismResult result = new ParallelismResult(
                        workers, parallelTime, speedup, efficiency, scalability,
                        workers, metrics);

                results.add(result);
                System.out.println(" Done");
            }

            return results;
        }

        private long measureSequential(long taskSize) throws Exception {
            CPUTask task = new CPUTask(taskSize);

            Instant start = Instant.now();
            task.call();
            return Duration.between(start, Instant.now()).toMillis();
        }

        private long measureParallel(int workers, long taskSize) throws Exception {
            ExecutorService executor = Executors.newFixedThreadPool(workers);
            long taskPerWorker = taskSize / workers;

            Instant start = Instant.now();

            List<Future<Long>> futures = new ArrayList<>();
            for (int i = 0; i < workers; i++) {
                futures.add(executor.submit(new CPUTask(taskPerWorker)));
            }

            for (Future<Long> future : futures) {
                future.get();
            }

            long duration = Duration.between(start, Instant.now()).toMillis();

            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);

            return duration;
        }
    }

    /**
     * I/O-bound 작업 분석
     */
    static class IOBoundAnalysis {

        static class IOTask implements Callable<String> {
            private final int ioDelayMs;

            IOTask(int ioDelayMs) {
                this.ioDelayMs = ioDelayMs;
            }

            @Override
            public String call() throws Exception {
                Thread.sleep(ioDelayMs);
                return "Completed";
            }
        }

        public List<ParallelismResult> analyzeParallelism(int maxWorkers,
                                                          int taskCount, int ioDelayMs) throws Exception {
            List<ParallelismResult> results = new ArrayList<>();

            // Sequential baseline
            System.out.println("\n  Measuring sequential baseline...");
            long sequentialTime = measureSequential(taskCount, ioDelayMs);
            System.out.println("  Sequential time: " + sequentialTime + " ms");

            // Test different worker counts
            int[] workerCounts = {1, 10, 50, 100, 500, 1000, Math.min(maxWorkers, 5000)};

            for (int workers : workerCounts) {
                if (workers > maxWorkers) continue;

                System.out.print("  Testing with " + workers + " workers...");

                long parallelTime = measureParallel(workers, taskCount, ioDelayMs);
                double speedup = (double) sequentialTime / parallelTime;
                double efficiency = speedup / workers;
                double scalability = speedup / workers;

                Map<String, Object> metrics = new HashMap<>();
                metrics.put("I/O Delay", ioDelayMs + " ms");
                metrics.put("Tasks per Worker", taskCount / workers);

                ParallelismResult result = new ParallelismResult(
                        workers, parallelTime, speedup, efficiency, scalability,
                        taskCount, metrics);

                results.add(result);
                System.out.println(" Done");
            }

            return results;
        }

        private long measureSequential(int taskCount, int ioDelayMs) throws Exception {
            Instant start = Instant.now();

            for (int i = 0; i < taskCount; i++) {
                new IOTask(ioDelayMs).call();
            }

            return Duration.between(start, Instant.now()).toMillis();
        }

        private long measureParallel(int workers, int taskCount, int ioDelayMs)
                throws Exception {
            // Virtual Threads for I/O
            CountDownLatch latch = new CountDownLatch(taskCount);

            Instant start = Instant.now();

            List<Thread> threads = new ArrayList<>();
            for (int i = 0; i < taskCount; i++) {
                Thread thread = Thread.ofVirtual().start(() -> {
                    try {
                        new IOTask(ioDelayMs).call();
                    } catch (Exception e) {
                        e.printStackTrace();
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

            return duration;
        }
    }

    /**
     * 부하 불균형 분석
     */
    static class LoadBalanceAnalysis {

        static class VariableTask implements Callable<Long> {
            private final long minIterations;
            private final long maxIterations;
            private static final Random random = new Random();

            VariableTask(long minIterations, long maxIterations) {
                this.minIterations = minIterations;
                this.maxIterations = maxIterations;
            }

            @Override
            public Long call() {
                long iterations = minIterations +
                        random.nextLong() % (maxIterations - minIterations + 1);

                long sum = 0;
                for (long i = 0; i < iterations; i++) {
                    sum += Math.sqrt(i);
                }
                return sum;
            }
        }

        public void analyzeLoadBalance(int workers, int taskCount) throws Exception {
            System.out.println("\n  Load Balance Analysis:");
            System.out.println("  " + "-".repeat(60));

            // Balanced workload
            System.out.println("\n  1. Balanced Workload:");
            long balancedTime = measureWithTasks(workers, taskCount, 100000, 100000);
            System.out.println("     Execution time: " + balancedTime + " ms");

            // Slightly imbalanced
            System.out.println("\n  2. Slightly Imbalanced (±20%):");
            long slightlyImbalanced = measureWithTasks(workers, taskCount, 80000, 120000);
            System.out.println("     Execution time: " + slightlyImbalanced + " ms");
            System.out.println("     Overhead: +" +
                    String.format("%.1f%%", ((slightlyImbalanced - balancedTime) * 100.0 / balancedTime)));

            // Highly imbalanced
            System.out.println("\n  3. Highly Imbalanced (±50%):");
            long highlyImbalanced = measureWithTasks(workers, taskCount, 50000, 150000);
            System.out.println("     Execution time: " + highlyImbalanced + " ms");
            System.out.println("     Overhead: +" +
                    String.format("%.1f%%", ((highlyImbalanced - balancedTime) * 100.0 / balancedTime)));

            System.out.println("\n  Insight: Load imbalance causes idle workers and reduces efficiency");
        }

        private long measureWithTasks(int workers, int taskCount,
                                      long minIter, long maxIter) throws Exception {
            ExecutorService executor = Executors.newFixedThreadPool(workers);

            Instant start = Instant.now();

            List<Future<Long>> futures = new ArrayList<>();
            for (int i = 0; i < taskCount; i++) {
                futures.add(executor.submit(new VariableTask(minIter, maxIter)));
            }

            for (Future<Long> future : futures) {
                future.get();
            }

            long duration = Duration.between(start, Instant.now()).toMillis();

            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);

            return duration;
        }
    }

    /**
     * 결과 시각화
     */
    static class ResultVisualizer {

        public void visualizeSpeedup(List<ParallelismResult> results) {
            if (results.isEmpty()) return;

            System.out.println("\n  Speedup Chart:");
            System.out.println("  " + "-".repeat(60));

            double maxSpeedup = results.stream()
                    .mapToDouble(r -> r.speedup)
                    .max()
                    .orElse(1.0);

            for (ParallelismResult result : results) {
                int barLength = (int) ((result.speedup / maxSpeedup) * 40);
                String bar = "█".repeat(Math.max(0, barLength));

                String linearMark = "";
                double linearSpeedup = result.workerCount;
                if (result.speedup >= linearSpeedup * 0.9) {
                    linearMark = " ⭐ Near-linear";
                }

                System.out.println(String.format("  %4d: %s %.2fx%s",
                        result.workerCount, bar, result.speedup, linearMark));
            }
        }

        public void visualizeEfficiency(List<ParallelismResult> results) {
            if (results.isEmpty()) return;

            System.out.println("\n  Efficiency Chart:");
            System.out.println("  " + "-".repeat(60));

            for (ParallelismResult result : results) {
                int barLength = (int) (result.efficiency * 40);
                String bar = "█".repeat(Math.max(0, barLength));

                String rating = "";
                if (result.efficiency >= 0.9) rating = " Excellent";
                else if (result.efficiency >= 0.7) rating = " Good";
                else if (result.efficiency >= 0.5) rating = " Fair";
                else rating = " Poor";

                System.out.println(String.format("  %4d: %s %.1f%%%s",
                        result.workerCount, bar, result.efficiency * 100, rating));
            }
        }

        public void printTable(List<ParallelismResult> results) {
            if (results.isEmpty()) return;

            System.out.println("\n  Detailed Results:");
            System.out.println("  " + "-".repeat(75));
            System.out.println("  Workers | Time (ms) | Speedup | Efficiency | Scalability");
            System.out.println("  " + "-".repeat(75));

            results.forEach(ParallelismResult::print);
            System.out.println("  " + "-".repeat(75));
        }
    }

    /**
     * 종합 분석
     */
    static class ComprehensiveAnalysis {

        public void runAnalysis() throws Exception {
            System.out.println("\n╔═══════════════════════════════════════════════════════════════════╗");
            System.out.println("║      PARALLELISM ANALYZER                                         ║");
            System.out.println("╚═══════════════════════════════════════════════════════════════════╝");

            int processorCount = Runtime.getRuntime().availableProcessors();
            System.out.println("\nSystem Info:");
            System.out.println("  CPU Cores: " + processorCount);

            ResultVisualizer visualizer = new ResultVisualizer();

            // 1. Amdahl's Law
            System.out.println("\n" + "═".repeat(70));
            System.out.println("1. AMDAHL'S LAW (Theoretical Limits)");
            System.out.println("═".repeat(70));

            AmdahlsLaw amdahl = new AmdahlsLaw();
            amdahl.analyze(0.95); // 95% parallelizable
            amdahl.visualizeSpeedup(0.95, 64);

            // 2. Gustafson's Law
            System.out.println("\n" + "═".repeat(70));
            System.out.println("2. GUSTAFSON'S LAW (Scaled Problem)");
            System.out.println("═".repeat(70));

            GustafsonsLaw gustafson = new GustafsonsLaw();
            gustafson.analyze(0.05); // 5% serial

            // 3. CPU-bound Analysis
            System.out.println("\n" + "═".repeat(70));
            System.out.println("3. CPU-BOUND WORKLOAD ANALYSIS");
            System.out.println("═".repeat(70));

            CPUBoundAnalysis cpuAnalysis = new CPUBoundAnalysis();
            List<ParallelismResult> cpuResults = cpuAnalysis.analyzeParallelism(
                    processorCount * 2, 10_000_000L);

            visualizer.printTable(cpuResults);
            visualizer.visualizeSpeedup(cpuResults);
            visualizer.visualizeEfficiency(cpuResults);

            // 4. I/O-bound Analysis
            System.out.println("\n" + "═".repeat(70));
            System.out.println("4. I/O-BOUND WORKLOAD ANALYSIS");
            System.out.println("═".repeat(70));

            IOBoundAnalysis ioAnalysis = new IOBoundAnalysis();
            List<ParallelismResult> ioResults = ioAnalysis.analyzeParallelism(
                    10000, 1000, 10);

            visualizer.printTable(ioResults);
            visualizer.visualizeSpeedup(ioResults);
            visualizer.visualizeEfficiency(ioResults);

            // 5. Load Balance Analysis
            System.out.println("\n" + "═".repeat(70));
            System.out.println("5. LOAD BALANCE ANALYSIS");
            System.out.println("═".repeat(70));

            LoadBalanceAnalysis loadBalance = new LoadBalanceAnalysis();
            loadBalance.analyzeLoadBalance(processorCount, 100);

            // 6. Summary
            printSummary(cpuResults, ioResults);
        }

        private void printSummary(List<ParallelismResult> cpuResults,
                                  List<ParallelismResult> ioResults) {
            System.out.println("\n" + "═".repeat(70));
            System.out.println("  SUMMARY");
            System.out.println("═".repeat(70));

            System.out.println("\n  CPU-Bound Workloads:");
            if (!cpuResults.isEmpty()) {
                ParallelismResult best = cpuResults.stream()
                        .max(Comparator.comparingDouble(r -> r.efficiency))
                        .orElse(null);
                if (best != null) {
                    System.out.println("    Best efficiency at " + best.workerCount +
                            " workers: " + String.format("%.1f%%", best.efficiency * 100));
                    System.out.println("    Maximum speedup: " +
                            String.format("%.2fx", best.speedup));
                }
            }

            System.out.println("\n  I/O-Bound Workloads:");
            if (!ioResults.isEmpty()) {
                ParallelismResult best = ioResults.stream()
                        .max(Comparator.comparingDouble(r -> r.speedup))
                        .orElse(null);
                if (best != null) {
                    System.out.println("    Best speedup at " + best.workerCount +
                            " workers: " + String.format("%.2fx", best.speedup));
                    System.out.println("    Efficiency: " +
                            String.format("%.1f%%", best.efficiency * 100));
                }
            }

            System.out.println("\n  Key Insights:");
            System.out.println("    • CPU-bound: Optimal workers ≈ CPU cores");
            System.out.println("    • I/O-bound: Can scale to thousands of workers");
            System.out.println("    • Load balance critical for efficiency");
            System.out.println("    • Serial portions limit maximum speedup (Amdahl's Law)");
            System.out.println("═".repeat(70));
        }
    }

    public static void main(String[] args) throws Exception {
        ComprehensiveAnalysis analysis = new ComprehensiveAnalysis();
        analysis.runAnalysis();

        System.out.println("\n╔═══════════════════════════════════════════════════════════════════╗");
        System.out.println("║         ANALYSIS COMPLETED                                        ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════╝");
    }
}