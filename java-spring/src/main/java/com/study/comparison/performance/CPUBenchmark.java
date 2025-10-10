package com.study.comparison.performance;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.math.BigInteger;
import java.security.MessageDigest;

/**
 * CPU 집약 작업 벤치마크
 *
 * CPU bound 작업에서 스레드 모델별 성능을 측정합니다.
 * Platform Thread가 CPU 작업에서 더 나은 성능을 보일 수 있습니다.
 *
 * 테스트 시나리오:
 * 1. Prime Number Generation: 소수 생성
 * 2. Hash Computation: SHA-256 해시 계산
 * 3. Matrix Multiplication: 행렬 곱셈
 * 4. Fibonacci Calculation: 피보나치 수열
 * 5. Sorting: 대용량 배열 정렬
 * 6. Monte Carlo Simulation: 몬테카를로 시뮬레이션
 *
 * 왜 Platform Thread가 CPU 작업에 유리한가?
 * - Virtual Thread: I/O 대기 시 유리, CPU 작업에서는 carrier thread에 매핑
 * - Platform Thread: OS 스케줄러가 직접 관리, CPU 친화성 활용
 * - CPU bound에서는 코어 수만큼의 스레드가 최적
 */
public class CPUBenchmark {

    private static final int WARMUP_ITERATIONS = 2;
    private static final int MEASUREMENT_ITERATIONS = 3;

    /**
     * 벤치마크 결과
     */
    static class BenchmarkResult {
        final String method;
        final String workloadType;
        final int taskCount;
        final long totalTimeMillis;
        final double avgTimePerTask;
        final double throughput; // tasks/sec

        BenchmarkResult(String method, String workloadType, int taskCount, long totalTimeMillis) {
            this.method = method;
            this.workloadType = workloadType;
            this.taskCount = taskCount;
            this.totalTimeMillis = totalTimeMillis;
            this.avgTimePerTask = (double) totalTimeMillis / taskCount;
            this.throughput = (taskCount * 1000.0) / totalTimeMillis;
        }

        void print() {
            System.out.println("\n  === " + method + " - " + workloadType + " ===");
            System.out.println("  Tasks: " + String.format("%,d", taskCount));
            System.out.println("  Total time: " + String.format("%,d ms", totalTimeMillis));
            System.out.println("  Avg per task: " + String.format("%.2f ms", avgTimePerTask));
            System.out.println("  Throughput: " + String.format("%.2f tasks/sec", throughput));
        }
    }

    /**
     * 1. Prime Number Generation (소수 생성)
     */
    static class PrimeNumberBenchmark {

        static class PrimeTask implements Callable<List<Integer>> {
            private final int start;
            private final int end;

            PrimeTask(int start, int end) {
                this.start = start;
                this.end = end;
            }

            @Override
            public List<Integer> call() {
                List<Integer> primes = new ArrayList<>();
                for (int num = start; num <= end; num++) {
                    if (isPrime(num)) {
                        primes.add(num);
                    }
                }
                return primes;
            }

            private boolean isPrime(int n) {
                if (n <= 1) return false;
                if (n <= 3) return true;
                if (n % 2 == 0 || n % 3 == 0) return false;

                for (int i = 5; i * i <= n; i += 6) {
                    if (n % i == 0 || n % (i + 2) == 0) {
                        return false;
                    }
                }
                return true;
            }
        }

        public BenchmarkResult measurePlatform(int taskCount, int rangePerTask) throws Exception {
            List<Thread> threads = new ArrayList<>();
            AtomicInteger totalPrimes = new AtomicInteger(0);
            CountDownLatch latch = new CountDownLatch(taskCount);

            Instant start = Instant.now();

            for (int i = 0; i < taskCount; i++) {
                final int taskStart = i * rangePerTask;
                final int taskEnd = taskStart + rangePerTask;

                Thread thread = Thread.ofPlatform().start(() -> {
                    try {
                        PrimeTask task = new PrimeTask(taskStart, taskEnd);
                        List<Integer> primes = task.call();
                        totalPrimes.addAndGet(primes.size());
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

            return new BenchmarkResult("Platform Thread",
                    "Prime Generation (range=" + rangePerTask + ")", taskCount, duration);
        }

        public BenchmarkResult measureVirtual(int taskCount, int rangePerTask) throws Exception {
            List<Thread> threads = new ArrayList<>();
            AtomicInteger totalPrimes = new AtomicInteger(0);
            CountDownLatch latch = new CountDownLatch(taskCount);

            Instant start = Instant.now();

            for (int i = 0; i < taskCount; i++) {
                final int taskStart = i * rangePerTask;
                final int taskEnd = taskStart + rangePerTask;

                Thread thread = Thread.ofVirtual().start(() -> {
                    try {
                        PrimeTask task = new PrimeTask(taskStart, taskEnd);
                        List<Integer> primes = task.call();
                        totalPrimes.addAndGet(primes.size());
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

            return new BenchmarkResult("Virtual Thread",
                    "Prime Generation (range=" + rangePerTask + ")", taskCount, duration);
        }

        public BenchmarkResult measureThreadPool(int taskCount, int rangePerTask) throws Exception {
            ExecutorService executor = Executors.newFixedThreadPool(
                    Runtime.getRuntime().availableProcessors());
            AtomicInteger totalPrimes = new AtomicInteger(0);

            Instant start = Instant.now();

            List<Future<List<Integer>>> futures = new ArrayList<>();
            for (int i = 0; i < taskCount; i++) {
                final int taskStart = i * rangePerTask;
                final int taskEnd = taskStart + rangePerTask;

                Future<List<Integer>> future = executor.submit(new PrimeTask(taskStart, taskEnd));
                futures.add(future);
            }

            for (Future<List<Integer>> future : futures) {
                totalPrimes.addAndGet(future.get().size());
            }

            long duration = Duration.between(start, Instant.now()).toMillis();

            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);

            return new BenchmarkResult("Thread Pool",
                    "Prime Generation (range=" + rangePerTask + ")", taskCount, duration);
        }
    }

    /**
     * 2. Hash Computation (SHA-256)
     */
    static class HashComputationBenchmark {

        static class HashTask implements Callable<String> {
            private final String data;
            private final int iterations;

            HashTask(String data, int iterations) {
                this.data = data;
                this.iterations = iterations;
            }

            @Override
            public String call() throws Exception {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                String result = data;

                for (int i = 0; i < iterations; i++) {
                    byte[] hash = digest.digest(result.getBytes());
                    result = bytesToHex(hash);
                }

                return result;
            }

            private String bytesToHex(byte[] bytes) {
                StringBuilder sb = new StringBuilder();
                for (byte b : bytes) {
                    sb.append(String.format("%02x", b));
                }
                return sb.toString();
            }
        }

        public BenchmarkResult measurePlatform(int taskCount, int hashIterations) throws Exception {
            CountDownLatch latch = new CountDownLatch(taskCount);

            Instant start = Instant.now();

            List<Thread> threads = new ArrayList<>();
            for (int i = 0; i < taskCount; i++) {
                final String data = "Task-" + i + "-Data";

                Thread thread = Thread.ofPlatform().start(() -> {
                    try {
                        HashTask task = new HashTask(data, hashIterations);
                        task.call();
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

            return new BenchmarkResult("Platform Thread",
                    "Hash Computation (iterations=" + hashIterations + ")", taskCount, duration);
        }

        public BenchmarkResult measureVirtual(int taskCount, int hashIterations) throws Exception {
            CountDownLatch latch = new CountDownLatch(taskCount);

            Instant start = Instant.now();

            List<Thread> threads = new ArrayList<>();
            for (int i = 0; i < taskCount; i++) {
                final String data = "Task-" + i + "-Data";

                Thread thread = Thread.ofVirtual().start(() -> {
                    try {
                        HashTask task = new HashTask(data, hashIterations);
                        task.call();
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

            return new BenchmarkResult("Virtual Thread",
                    "Hash Computation (iterations=" + hashIterations + ")", taskCount, duration);
        }

        public BenchmarkResult measureThreadPool(int taskCount, int hashIterations) throws Exception {
            ExecutorService executor = Executors.newFixedThreadPool(
                    Runtime.getRuntime().availableProcessors());

            Instant start = Instant.now();

            List<Future<String>> futures = new ArrayList<>();
            for (int i = 0; i < taskCount; i++) {
                final String data = "Task-" + i + "-Data";
                Future<String> future = executor.submit(new HashTask(data, hashIterations));
                futures.add(future);
            }

            for (Future<String> future : futures) {
                future.get();
            }

            long duration = Duration.between(start, Instant.now()).toMillis();

            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);

            return new BenchmarkResult("Thread Pool",
                    "Hash Computation (iterations=" + hashIterations + ")", taskCount, duration);
        }
    }

    /**
     * 3. Matrix Multiplication (행렬 곱셈)
     */
    static class MatrixMultiplicationBenchmark {

        static class MatrixTask implements Callable<double[][]> {
            private final double[][] matrixA;
            private final double[][] matrixB;

            MatrixTask(int size) {
                this.matrixA = generateMatrix(size);
                this.matrixB = generateMatrix(size);
            }

            @Override
            public double[][] call() {
                int n = matrixA.length;
                double[][] result = new double[n][n];

                for (int i = 0; i < n; i++) {
                    for (int j = 0; j < n; j++) {
                        for (int k = 0; k < n; k++) {
                            result[i][j] += matrixA[i][k] * matrixB[k][j];
                        }
                    }
                }

                return result;
            }

            private double[][] generateMatrix(int size) {
                double[][] matrix = new double[size][size];
                Random random = new Random();
                for (int i = 0; i < size; i++) {
                    for (int j = 0; j < size; j++) {
                        matrix[i][j] = random.nextDouble();
                    }
                }
                return matrix;
            }
        }

        public BenchmarkResult measurePlatform(int taskCount, int matrixSize) throws Exception {
            CountDownLatch latch = new CountDownLatch(taskCount);

            Instant start = Instant.now();

            List<Thread> threads = new ArrayList<>();
            for (int i = 0; i < taskCount; i++) {
                Thread thread = Thread.ofPlatform().start(() -> {
                    try {
                        MatrixTask task = new MatrixTask(matrixSize);
                        task.call();
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

            return new BenchmarkResult("Platform Thread",
                    "Matrix Multiplication (" + matrixSize + "x" + matrixSize + ")",
                    taskCount, duration);
        }

        public BenchmarkResult measureVirtual(int taskCount, int matrixSize) throws Exception {
            CountDownLatch latch = new CountDownLatch(taskCount);

            Instant start = Instant.now();

            List<Thread> threads = new ArrayList<>();
            for (int i = 0; i < taskCount; i++) {
                Thread thread = Thread.ofVirtual().start(() -> {
                    try {
                        MatrixTask task = new MatrixTask(matrixSize);
                        task.call();
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

            return new BenchmarkResult("Virtual Thread",
                    "Matrix Multiplication (" + matrixSize + "x" + matrixSize + ")",
                    taskCount, duration);
        }

        public BenchmarkResult measureThreadPool(int taskCount, int matrixSize) throws Exception {
            ExecutorService executor = Executors.newFixedThreadPool(
                    Runtime.getRuntime().availableProcessors());

            Instant start = Instant.now();

            List<Future<double[][]>> futures = new ArrayList<>();
            for (int i = 0; i < taskCount; i++) {
                Future<double[][]> future = executor.submit(new MatrixTask(matrixSize));
                futures.add(future);
            }

            for (Future<double[][]> future : futures) {
                future.get();
            }

            long duration = Duration.between(start, Instant.now()).toMillis();

            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);

            return new BenchmarkResult("Thread Pool",
                    "Matrix Multiplication (" + matrixSize + "x" + matrixSize + ")",
                    taskCount, duration);
        }
    }

    /**
     * 4. Fibonacci Calculation (재귀)
     */
    static class FibonacciBenchmark {

        static class FibonacciTask implements Callable<Long> {
            private final int n;

            FibonacciTask(int n) {
                this.n = n;
            }

            @Override
            public Long call() {
                return fibonacci(n);
            }

            private long fibonacci(int n) {
                if (n <= 1) return n;

                // Iterative approach (faster)
                long a = 0, b = 1;
                for (int i = 2; i <= n; i++) {
                    long temp = a + b;
                    a = b;
                    b = temp;
                }
                return b;
            }
        }

        public BenchmarkResult measurePlatform(int taskCount, int fibNumber) throws Exception {
            CountDownLatch latch = new CountDownLatch(taskCount);
            AtomicLong totalSum = new AtomicLong(0);

            Instant start = Instant.now();

            List<Thread> threads = new ArrayList<>();
            for (int i = 0; i < taskCount; i++) {
                Thread thread = Thread.ofPlatform().start(() -> {
                    try {
                        FibonacciTask task = new FibonacciTask(fibNumber);
                        long result = task.call();
                        totalSum.addAndGet(result);
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

            return new BenchmarkResult("Platform Thread",
                    "Fibonacci (n=" + fibNumber + ")", taskCount, duration);
        }

        public BenchmarkResult measureVirtual(int taskCount, int fibNumber) throws Exception {
            CountDownLatch latch = new CountDownLatch(taskCount);
            AtomicLong totalSum = new AtomicLong(0);

            Instant start = Instant.now();

            List<Thread> threads = new ArrayList<>();
            for (int i = 0; i < taskCount; i++) {
                Thread thread = Thread.ofVirtual().start(() -> {
                    try {
                        FibonacciTask task = new FibonacciTask(fibNumber);
                        long result = task.call();
                        totalSum.addAndGet(result);
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

            return new BenchmarkResult("Virtual Thread",
                    "Fibonacci (n=" + fibNumber + ")", taskCount, duration);
        }

        public BenchmarkResult measureThreadPool(int taskCount, int fibNumber) throws Exception {
            ExecutorService executor = Executors.newFixedThreadPool(
                    Runtime.getRuntime().availableProcessors());
            AtomicLong totalSum = new AtomicLong(0);

            Instant start = Instant.now();

            List<Future<Long>> futures = new ArrayList<>();
            for (int i = 0; i < taskCount; i++) {
                Future<Long> future = executor.submit(new FibonacciTask(fibNumber));
                futures.add(future);
            }

            for (Future<Long> future : futures) {
                totalSum.addAndGet(future.get());
            }

            long duration = Duration.between(start, Instant.now()).toMillis();

            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);

            return new BenchmarkResult("Thread Pool",
                    "Fibonacci (n=" + fibNumber + ")", taskCount, duration);
        }
    }

    /**
     * 5. Sorting Benchmark (배열 정렬)
     */
    static class SortingBenchmark {

        static class SortTask implements Callable<int[]> {
            private final int arraySize;

            SortTask(int arraySize) {
                this.arraySize = arraySize;
            }

            @Override
            public int[] call() {
                int[] array = generateRandomArray(arraySize);
                Arrays.sort(array);
                return array;
            }

            private int[] generateRandomArray(int size) {
                Random random = new Random();
                int[] array = new int[size];
                for (int i = 0; i < size; i++) {
                    array[i] = random.nextInt(1000000);
                }
                return array;
            }
        }

        public BenchmarkResult measurePlatform(int taskCount, int arraySize) throws Exception {
            CountDownLatch latch = new CountDownLatch(taskCount);

            Instant start = Instant.now();

            List<Thread> threads = new ArrayList<>();
            for (int i = 0; i < taskCount; i++) {
                Thread thread = Thread.ofPlatform().start(() -> {
                    try {
                        SortTask task = new SortTask(arraySize);
                        task.call();
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

            return new BenchmarkResult("Platform Thread",
                    "Array Sorting (size=" + arraySize + ")", taskCount, duration);
        }

        public BenchmarkResult measureVirtual(int taskCount, int arraySize) throws Exception {
            CountDownLatch latch = new CountDownLatch(taskCount);

            Instant start = Instant.now();

            List<Thread> threads = new ArrayList<>();
            for (int i = 0; i < taskCount; i++) {
                Thread thread = Thread.ofVirtual().start(() -> {
                    try {
                        SortTask task = new SortTask(arraySize);
                        task.call();
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

            return new BenchmarkResult("Virtual Thread",
                    "Array Sorting (size=" + arraySize + ")", taskCount, duration);
        }

        public BenchmarkResult measureThreadPool(int taskCount, int arraySize) throws Exception {
            ExecutorService executor = Executors.newFixedThreadPool(
                    Runtime.getRuntime().availableProcessors());

            Instant start = Instant.now();

            List<Future<int[]>> futures = new ArrayList<>();
            for (int i = 0; i < taskCount; i++) {
                Future<int[]> future = executor.submit(new SortTask(arraySize));
                futures.add(future);
            }

            for (Future<int[]> future : futures) {
                future.get();
            }

            long duration = Duration.between(start, Instant.now()).toMillis();

            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);

            return new BenchmarkResult("Thread Pool",
                    "Array Sorting (size=" + arraySize + ")", taskCount, duration);
        }
    }

    /**
     * 6. Monte Carlo Simulation (몬테카를로 시뮬레이션 - π 계산)
     */
    static class MonteCarloSimulation {

        static class MonteCarloTask implements Callable<Double> {
            private final int iterations;

            MonteCarloTask(int iterations) {
                this.iterations = iterations;
            }

            @Override
            public Double call() {
                Random random = new Random();
                int insideCircle = 0;

                for (int i = 0; i < iterations; i++) {
                    double x = random.nextDouble();
                    double y = random.nextDouble();

                    if (x * x + y * y <= 1.0) {
                        insideCircle++;
                    }
                }

                return 4.0 * insideCircle / iterations;
            }
        }

        public BenchmarkResult measurePlatform(int taskCount, int iterations) throws Exception {
            CountDownLatch latch = new CountDownLatch(taskCount);
            AtomicInteger completed = new AtomicInteger(0);

            Instant start = Instant.now();

            List<Thread> threads = new ArrayList<>();
            for (int i = 0; i < taskCount; i++) {
                Thread thread = Thread.ofPlatform().start(() -> {
                    try {
                        MonteCarloTask task = new MonteCarloTask(iterations);
                        task.call();
                        completed.incrementAndGet();
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

            return new BenchmarkResult("Platform Thread",
                    "Monte Carlo (iterations=" + iterations + ")", taskCount, duration);
        }

        public BenchmarkResult measureVirtual(int taskCount, int iterations) throws Exception {
            CountDownLatch latch = new CountDownLatch(taskCount);
            AtomicInteger completed = new AtomicInteger(0);

            Instant start = Instant.now();

            List<Thread> threads = new ArrayList<>();
            for (int i = 0; i < taskCount; i++) {
                Thread thread = Thread.ofVirtual().start(() -> {
                    try {
                        MonteCarloTask task = new MonteCarloTask(iterations);
                        task.call();
                        completed.incrementAndGet();
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

            return new BenchmarkResult("Virtual Thread",
                    "Monte Carlo (iterations=" + iterations + ")", taskCount, duration);
        }

        public BenchmarkResult measureThreadPool(int taskCount, int iterations) throws Exception {
            ExecutorService executor = Executors.newFixedThreadPool(
                    Runtime.getRuntime().availableProcessors());

            Instant start = Instant.now();

            List<Future<Double>> futures = new ArrayList<>();
            for (int i = 0; i < taskCount; i++) {
                Future<Double> future = executor.submit(new MonteCarloTask(iterations));
                futures.add(future);
            }

            for (Future<Double> future : futures) {
                future.get();
            }

            long duration = Duration.between(start, Instant.now()).toMillis();

            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);

            return new BenchmarkResult("Thread Pool",
                    "Monte Carlo (iterations=" + iterations + ")", taskCount, duration);
        }
    }

    /**
     * 벤치마크 실행
     */
    static class BenchmarkRunner {

        public void runAllBenchmarks() throws Exception {
            System.out.println("\n╔═══════════════════════════════════════════════════════════════════╗");
            System.out.println("║      CPU BENCHMARK - Platform vs Virtual Threads                 ║");
            System.out.println("╚═══════════════════════════════════════════════════════════════════╝");

            int cpuCores = Runtime.getRuntime().availableProcessors();
            System.out.println("\nSystem Info:");
            System.out.println("  CPU Cores: " + cpuCores);
            System.out.println("  Optimal Thread Count for CPU: " + cpuCores);

            List<BenchmarkResult> allResults = new ArrayList<>();

            // Warmup
            System.out.println("\n[Warmup Phase]");
            for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                System.out.print("  Warmup iteration " + (i + 1) + "/" + WARMUP_ITERATIONS + "...");
                new PrimeNumberBenchmark().measureThreadPool(cpuCores, 1000);
                System.out.println(" Done");
                System.gc();
                Thread.sleep(300);
            }

            System.out.println("\n[Measurement Phase]");

            // 1. Prime Number Generation
            System.out.println("\n1. Prime Number Generation");
            System.out.println("   " + "─".repeat(60));
            allResults.addAll(runPrimeBenchmark(cpuCores));

            // 2. Hash Computation
            System.out.println("\n2. Hash Computation (SHA-256)");
            System.out.println("   " + "─".repeat(60));
            allResults.addAll(runHashBenchmark(cpuCores));

            // 3. Matrix Multiplication
            System.out.println("\n3. Matrix Multiplication");
            System.out.println("   " + "─".repeat(60));
            allResults.addAll(runMatrixBenchmark(cpuCores));

            // 4. Fibonacci
            System.out.println("\n4. Fibonacci Calculation");
            System.out.println("   " + "─".repeat(60));
            allResults.addAll(runFibonacciBenchmark(cpuCores));

            // 5. Sorting
            System.out.println("\n5. Array Sorting");
            System.out.println("   " + "─".repeat(60));
            allResults.addAll(runSortingBenchmark(cpuCores));

            // 6. Monte Carlo
            System.out.println("\n6. Monte Carlo Simulation");
            System.out.println("   " + "─".repeat(60));
            allResults.addAll(runMonteCarloBenchmark(cpuCores));

            // 종합 분석
            printSummary(allResults);
        }

        private List<BenchmarkResult> runPrimeBenchmark(int taskCount) throws Exception {
            List<BenchmarkResult> results = new ArrayList<>();
            PrimeNumberBenchmark benchmark = new PrimeNumberBenchmark();

            List<BenchmarkResult> platformResults = new ArrayList<>();
            for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
                platformResults.add(benchmark.measurePlatform(taskCount, 10000));
                System.gc();
                Thread.sleep(200);
            }
            results.add(getMedianResult(platformResults));

            List<BenchmarkResult> virtualResults = new ArrayList<>();
            for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
                virtualResults.add(benchmark.measureVirtual(taskCount, 10000));
                System.gc();
                Thread.sleep(200);
            }
            results.add(getMedianResult(virtualResults));

            List<BenchmarkResult> poolResults = new ArrayList<>();
            for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
                poolResults.add(benchmark.measureThreadPool(taskCount, 10000));
                System.gc();
                Thread.sleep(200);
            }
            results.add(getMedianResult(poolResults));

            for (BenchmarkResult result : results) {
                result.print();
            }

            return results;
        }

        private List<BenchmarkResult> runHashBenchmark(int taskCount) throws Exception {
            List<BenchmarkResult> results = new ArrayList<>();
            HashComputationBenchmark benchmark = new HashComputationBenchmark();

            List<BenchmarkResult> platformResults = new ArrayList<>();
            for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
                platformResults.add(benchmark.measurePlatform(taskCount, 10000));
                System.gc();
                Thread.sleep(200);
            }
            results.add(getMedianResult(platformResults));

            List<BenchmarkResult> virtualResults = new ArrayList<>();
            for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
                virtualResults.add(benchmark.measureVirtual(taskCount, 10000));
                System.gc();
                Thread.sleep(200);
            }
            results.add(getMedianResult(virtualResults));

            List<BenchmarkResult> poolResults = new ArrayList<>();
            for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
                poolResults.add(benchmark.measureThreadPool(taskCount, 10000));
                System.gc();
                Thread.sleep(200);
            }
            results.add(getMedianResult(poolResults));

            for (BenchmarkResult result : results) {
                result.print();
            }

            return results;
        }

        private List<BenchmarkResult> runMatrixBenchmark(int taskCount) throws Exception {
            List<BenchmarkResult> results = new ArrayList<>();
            MatrixMultiplicationBenchmark benchmark = new MatrixMultiplicationBenchmark();

            List<BenchmarkResult> platformResults = new ArrayList<>();
            for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
                platformResults.add(benchmark.measurePlatform(taskCount, 100));
                System.gc();
                Thread.sleep(200);
            }
            results.add(getMedianResult(platformResults));

            List<BenchmarkResult> virtualResults = new ArrayList<>();
            for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
                virtualResults.add(benchmark.measureVirtual(taskCount, 100));
                System.gc();
                Thread.sleep(200);
            }
            results.add(getMedianResult(virtualResults));

            List<BenchmarkResult> poolResults = new ArrayList<>();
            for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
                poolResults.add(benchmark.measureThreadPool(taskCount, 100));
                System.gc();
                Thread.sleep(200);
            }
            results.add(getMedianResult(poolResults));

            for (BenchmarkResult result : results) {
                result.print();
            }

            return results;
        }

        private List<BenchmarkResult> runFibonacciBenchmark(int taskCount) throws Exception {
            List<BenchmarkResult> results = new ArrayList<>();
            FibonacciBenchmark benchmark = new FibonacciBenchmark();

            List<BenchmarkResult> platformResults = new ArrayList<>();
            for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
                platformResults.add(benchmark.measurePlatform(taskCount, 100000));
                System.gc();
                Thread.sleep(200);
            }
            results.add(getMedianResult(platformResults));

            List<BenchmarkResult> virtualResults = new ArrayList<>();
            for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
                virtualResults.add(benchmark.measureVirtual(taskCount, 100000));
                System.gc();
                Thread.sleep(200);
            }
            results.add(getMedianResult(virtualResults));

            List<BenchmarkResult> poolResults = new ArrayList<>();
            for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
                poolResults.add(benchmark.measureThreadPool(taskCount, 100000));
                System.gc();
                Thread.sleep(200);
            }
            results.add(getMedianResult(poolResults));

            for (BenchmarkResult result : results) {
                result.print();
            }

            return results;
        }

        private List<BenchmarkResult> runSortingBenchmark(int taskCount) throws Exception {
            List<BenchmarkResult> results = new ArrayList<>();
            SortingBenchmark benchmark = new SortingBenchmark();

            List<BenchmarkResult> platformResults = new ArrayList<>();
            for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
                platformResults.add(benchmark.measurePlatform(taskCount, 100000));
                System.gc();
                Thread.sleep(200);
            }
            results.add(getMedianResult(platformResults));

            List<BenchmarkResult> virtualResults = new ArrayList<>();
            for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
                virtualResults.add(benchmark.measureVirtual(taskCount, 100000));
                System.gc();
                Thread.sleep(200);
            }
            results.add(getMedianResult(virtualResults));

            List<BenchmarkResult> poolResults = new ArrayList<>();
            for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
                poolResults.add(benchmark.measureThreadPool(taskCount, 100000));
                System.gc();
                Thread.sleep(200);
            }
            results.add(getMedianResult(poolResults));

            for (BenchmarkResult result : results) {
                result.print();
            }

            return results;
        }

        private List<BenchmarkResult> runMonteCarloBenchmark(int taskCount) throws Exception {
            List<BenchmarkResult> results = new ArrayList<>();
            MonteCarloSimulation benchmark = new MonteCarloSimulation();

            List<BenchmarkResult> platformResults = new ArrayList<>();
            for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
                platformResults.add(benchmark.measurePlatform(taskCount, 1000000));
                System.gc();
                Thread.sleep(200);
            }
            results.add(getMedianResult(platformResults));

            List<BenchmarkResult> virtualResults = new ArrayList<>();
            for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
                virtualResults.add(benchmark.measureVirtual(taskCount, 1000000));
                System.gc();
                Thread.sleep(200);
            }
            results.add(getMedianResult(virtualResults));

            List<BenchmarkResult> poolResults = new ArrayList<>();
            for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
                poolResults.add(benchmark.measureThreadPool(taskCount, 1000000));
                System.gc();
                Thread.sleep(200);
            }
            results.add(getMedianResult(poolResults));

            for (BenchmarkResult result : results) {
                result.print();
            }

            return results;
        }

        private BenchmarkResult getMedianResult(List<BenchmarkResult> results) {
            results.sort((a, b) -> Long.compare(a.totalTimeMillis, b.totalTimeMillis));
            return results.get(results.size() / 2);
        }

        private void printSummary(List<BenchmarkResult> results) {
            System.out.println("\n" + "═".repeat(70));
            System.out.println("  SUMMARY - CPU Performance Comparison");
            System.out.println("═".repeat(70));

            // 워크로드 타입별 그룹화
            results.stream()
                    .collect(java.util.stream.Collectors.groupingBy(r -> r.workloadType))
                    .forEach((workload, workloadResults) -> {
                        System.out.println("\n  " + workload + ":");

                        BenchmarkResult fastest = workloadResults.stream()
                                .min((a, b) -> Long.compare(a.totalTimeMillis, b.totalTimeMillis))
                                .orElse(null);

                        workloadResults.forEach(r -> {
                            double ratio = fastest != null ?
                                    (double) r.totalTimeMillis / fastest.totalTimeMillis : 1.0;
                            String badge = r == fastest ? " 🏆" : "";
                            System.out.println(String.format("    %s: %,d ms (%.2fx)%s",
                                    r.method, r.totalTimeMillis, ratio, badge));
                        });
                    });

            System.out.println("\n" + "═".repeat(70));
            System.out.println("  KEY INSIGHTS");
            System.out.println("═".repeat(70));
            System.out.println("  • Thread Pool typically performs best for CPU-bound tasks");
            System.out.println("  • Optimal thread count = CPU cores for pure CPU workloads");
            System.out.println("  • Platform Threads have lower overhead than Virtual Threads for CPU");
            System.out.println("  • Virtual Threads shine in I/O, not CPU-intensive operations");
            System.out.println("═".repeat(70));
        }
    }

    public static void main(String[] args) throws Exception {
        BenchmarkRunner runner = new BenchmarkRunner();
        runner.runAllBenchmarks();

        System.out.println("\n╔═══════════════════════════════════════════════════════════════════╗");
        System.out.println("║         BENCHMARK COMPLETED                                       ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════╝");
    }
}