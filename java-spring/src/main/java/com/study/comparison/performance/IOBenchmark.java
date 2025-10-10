package com.study.comparison.performance;


import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * I/O 작업 벤치마크
 *
 * I/O bound 작업에서 스레드 모델별 성능을 측정합니다.
 * Virtual Thread는 I/O 작업에서 특히 강력한 성능을 보입니다.
 *
 * 테스트 시나리오:
 * 1. File I/O: 파일 읽기/쓰기
 * 2. Network I/O: HTTP 요청 시뮬레이션
 * 3. Database I/O: JDBC 연결 시뮬레이션 (sleep으로 대체)
 * 4. Mixed I/O: 파일 + 네트워크 혼합
 * 5. Concurrent I/O: 대량의 동시 I/O 요청
 *
 * 왜 Virtual Thread가 I/O에 강한가?
 * - Platform Thread: I/O 대기 중에도 OS 스레드 점유 (낭비)
 * - Virtual Thread: I/O 대기 시 carrier thread 양보 (효율적)
 * - 수천~수만 개의 동시 I/O 처리 가능
 */
public class IOBenchmark {

    private static final int WARMUP_ITERATIONS = 2;
    private static final int MEASUREMENT_ITERATIONS = 3;
    private static final String TEST_DIR = "benchmark_test_data";

    /**
     * 벤치마크 결과
     */
    static class BenchmarkResult {
        final String method;
        final String ioType;
        final int operationCount;
        final long totalTimeMillis;
        final double avgTimePerOp;
        final double throughput; // ops/sec

        BenchmarkResult(String method, String ioType, int operationCount, long totalTimeMillis) {
            this.method = method;
            this.ioType = ioType;
            this.operationCount = operationCount;
            this.totalTimeMillis = totalTimeMillis;
            this.avgTimePerOp = (double) totalTimeMillis / operationCount;
            this.throughput = (operationCount * 1000.0) / totalTimeMillis;
        }

        void print() {
            System.out.println("\n  === " + method + " - " + ioType + " ===");
            System.out.println("  Operations: " + String.format("%,d", operationCount));
            System.out.println("  Total time: " + String.format("%,d ms", totalTimeMillis));
            System.out.println("  Avg per op: " + String.format("%.2f ms", avgTimePerOp));
            System.out.println("  Throughput: " + String.format("%.2f ops/sec", throughput));
        }
    }

    /**
     * 테스트 데이터 준비
     */
    static class TestDataSetup {
        static void prepare() throws IOException {
            Path testDir = Paths.get(TEST_DIR);
            if (!Files.exists(testDir)) {
                Files.createDirectory(testDir);
            }

            // 테스트 파일 생성
            for (int i = 0; i < 100; i++) {
                Path file = testDir.resolve("test_file_" + i + ".txt");
                if (!Files.exists(file)) {
                    try (BufferedWriter writer = Files.newBufferedWriter(file)) {
                        for (int j = 0; j < 1000; j++) {
                            writer.write("Test data line " + j + "\n");
                        }
                    }
                }
            }
        }

        static void cleanup() throws IOException {
            Path testDir = Paths.get(TEST_DIR);
            if (Files.exists(testDir)) {
                Files.walk(testDir)
                        .sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                // Ignore
                            }
                        });
            }
        }
    }

    /**
     * 1. File I/O 벤치마크
     */
    static class FileIOBenchmark {

        static class FileReadTask implements Callable<Long> {
            private final int fileIndex;

            FileReadTask(int fileIndex) {
                this.fileIndex = fileIndex;
            }

            @Override
            public Long call() throws Exception {
                Path file = Paths.get(TEST_DIR, "test_file_" + fileIndex + ".txt");
                long lineCount = 0;

                try (BufferedReader reader = Files.newBufferedReader(file)) {
                    while (reader.readLine() != null) {
                        lineCount++;
                    }
                }

                return lineCount;
            }
        }

        public BenchmarkResult measurePlatform(int fileCount) throws Exception {
            List<Thread> threads = new ArrayList<>();
            AtomicLong totalLines = new AtomicLong(0);
            CountDownLatch latch = new CountDownLatch(fileCount);

            Instant start = Instant.now();

            for (int i = 0; i < fileCount; i++) {
                final int index = i % 100;
                Thread thread = Thread.ofPlatform().start(() -> {
                    try {
                        FileReadTask task = new FileReadTask(index);
                        long lines = task.call();
                        totalLines.addAndGet(lines);
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

            return new BenchmarkResult("Platform Thread", "File Read", fileCount, duration);
        }

        public BenchmarkResult measureVirtual(int fileCount) throws Exception {
            List<Thread> threads = new ArrayList<>();
            AtomicLong totalLines = new AtomicLong(0);
            CountDownLatch latch = new CountDownLatch(fileCount);

            Instant start = Instant.now();

            for (int i = 0; i < fileCount; i++) {
                final int index = i % 100;
                Thread thread = Thread.ofVirtual().start(() -> {
                    try {
                        FileReadTask task = new FileReadTask(index);
                        long lines = task.call();
                        totalLines.addAndGet(lines);
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

            return new BenchmarkResult("Virtual Thread", "File Read", fileCount, duration);
        }

        public BenchmarkResult measureThreadPool(int fileCount) throws Exception {
            ExecutorService executor = Executors.newFixedThreadPool(
                    Runtime.getRuntime().availableProcessors());
            AtomicLong totalLines = new AtomicLong(0);

            Instant start = Instant.now();

            List<Future<Long>> futures = new ArrayList<>();
            for (int i = 0; i < fileCount; i++) {
                final int index = i % 100;
                Future<Long> future = executor.submit(new FileReadTask(index));
                futures.add(future);
            }

            for (Future<Long> future : futures) {
                totalLines.addAndGet(future.get());
            }

            long duration = Duration.between(start, Instant.now()).toMillis();

            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);

            return new BenchmarkResult("Thread Pool", "File Read", fileCount, duration);
        }
    }

    /**
     * 2. Network I/O 벤치마크 (시뮬레이션)
     */
    static class NetworkIOBenchmark {

        static class NetworkRequestTask implements Callable<String> {
            private final int latencyMs;

            NetworkRequestTask(int latencyMs) {
                this.latencyMs = latencyMs;
            }

            @Override
            public String call() throws Exception {
                // 네트워크 요청 시뮬레이션 (sleep으로 대체)
                Thread.sleep(latencyMs);
                return "Response-" + Thread.currentThread().getName();
            }
        }

        public BenchmarkResult measurePlatform(int requestCount, int latencyMs) throws Exception {
            AtomicInteger completed = new AtomicInteger(0);
            CountDownLatch latch = new CountDownLatch(requestCount);

            Instant start = Instant.now();

            List<Thread> threads = new ArrayList<>();
            for (int i = 0; i < requestCount; i++) {
                Thread thread = Thread.ofPlatform().start(() -> {
                    try {
                        NetworkRequestTask task = new NetworkRequestTask(latencyMs);
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
                    "Network I/O (latency=" + latencyMs + "ms)", requestCount, duration);
        }

        public BenchmarkResult measureVirtual(int requestCount, int latencyMs) throws Exception {
            AtomicInteger completed = new AtomicInteger(0);
            CountDownLatch latch = new CountDownLatch(requestCount);

            Instant start = Instant.now();

            List<Thread> threads = new ArrayList<>();
            for (int i = 0; i < requestCount; i++) {
                Thread thread = Thread.ofVirtual().start(() -> {
                    try {
                        NetworkRequestTask task = new NetworkRequestTask(latencyMs);
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
                    "Network I/O (latency=" + latencyMs + "ms)", requestCount, duration);
        }

        public BenchmarkResult measureThreadPool(int requestCount, int latencyMs) throws Exception {
            ExecutorService executor = Executors.newFixedThreadPool(
                    Runtime.getRuntime().availableProcessors() * 2);

            Instant start = Instant.now();

            List<Future<String>> futures = new ArrayList<>();
            for (int i = 0; i < requestCount; i++) {
                Future<String> future = executor.submit(new NetworkRequestTask(latencyMs));
                futures.add(future);
            }

            for (Future<String> future : futures) {
                future.get();
            }

            long duration = Duration.between(start, Instant.now()).toMillis();

            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);

            return new BenchmarkResult("Thread Pool",
                    "Network I/O (latency=" + latencyMs + "ms)", requestCount, duration);
        }
    }

    /**
     * 3. Database I/O 벤치마크 (시뮬레이션)
     */
    static class DatabaseIOBenchmark {

        static class DatabaseQueryTask implements Callable<List<String>> {
            private final int queryTimeMs;

            DatabaseQueryTask(int queryTimeMs) {
                this.queryTimeMs = queryTimeMs;
            }

            @Override
            public List<String> call() throws Exception {
                // DB 쿼리 시뮬레이션
                Thread.sleep(queryTimeMs);

                // 결과 생성
                List<String> results = new ArrayList<>();
                for (int i = 0; i < 10; i++) {
                    results.add("Row-" + i);
                }
                return results;
            }
        }

        public BenchmarkResult measurePlatform(int queryCount, int queryTimeMs) throws Exception {
            AtomicInteger completed = new AtomicInteger(0);
            CountDownLatch latch = new CountDownLatch(queryCount);

            Instant start = Instant.now();

            List<Thread> threads = new ArrayList<>();
            for (int i = 0; i < queryCount; i++) {
                Thread thread = Thread.ofPlatform().start(() -> {
                    try {
                        DatabaseQueryTask task = new DatabaseQueryTask(queryTimeMs);
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
                    "Database Query (time=" + queryTimeMs + "ms)", queryCount, duration);
        }

        public BenchmarkResult measureVirtual(int queryCount, int queryTimeMs) throws Exception {
            AtomicInteger completed = new AtomicInteger(0);
            CountDownLatch latch = new CountDownLatch(queryCount);

            Instant start = Instant.now();

            List<Thread> threads = new ArrayList<>();
            for (int i = 0; i < queryCount; i++) {
                Thread thread = Thread.ofVirtual().start(() -> {
                    try {
                        DatabaseQueryTask task = new DatabaseQueryTask(queryTimeMs);
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
                    "Database Query (time=" + queryTimeMs + "ms)", queryCount, duration);
        }
    }

    /**
     * 4. Mixed I/O 벤치마크 (파일 + 네트워크)
     */
    static class MixedIOBenchmark {

        static class MixedTask implements Callable<String> {
            private final int fileIndex;
            private final int networkLatencyMs;

            MixedTask(int fileIndex, int networkLatencyMs) {
                this.fileIndex = fileIndex;
                this.networkLatencyMs = networkLatencyMs;
            }

            @Override
            public String call() throws Exception {
                // 1. File I/O
                Path file = Paths.get(TEST_DIR, "test_file_" + (fileIndex % 100) + ".txt");
                long lineCount = 0;
                try (BufferedReader reader = Files.newBufferedReader(file)) {
                    while (reader.readLine() != null) {
                        lineCount++;
                    }
                }

                // 2. Network I/O
                Thread.sleep(networkLatencyMs);

                return "Processed " + lineCount + " lines";
            }
        }

        public BenchmarkResult measurePlatform(int taskCount, int networkLatencyMs) throws Exception {
            CountDownLatch latch = new CountDownLatch(taskCount);

            Instant start = Instant.now();

            List<Thread> threads = new ArrayList<>();
            for (int i = 0; i < taskCount; i++) {
                final int index = i;
                Thread thread = Thread.ofPlatform().start(() -> {
                    try {
                        MixedTask task = new MixedTask(index, networkLatencyMs);
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

            return new BenchmarkResult("Platform Thread", "Mixed I/O", taskCount, duration);
        }

        public BenchmarkResult measureVirtual(int taskCount, int networkLatencyMs) throws Exception {
            CountDownLatch latch = new CountDownLatch(taskCount);

            Instant start = Instant.now();

            List<Thread> threads = new ArrayList<>();
            for (int i = 0; i < taskCount; i++) {
                final int index = i;
                Thread thread = Thread.ofVirtual().start(() -> {
                    try {
                        MixedTask task = new MixedTask(index, networkLatencyMs);
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

            return new BenchmarkResult("Virtual Thread", "Mixed I/O", taskCount, duration);
        }
    }

    /**
     * 5. 대량 동시 I/O 벤치마크
     */
    static class MassiveConcurrentIOBenchmark {

        public BenchmarkResult measurePlatform(int concurrentCount, int ioLatencyMs) throws Exception {
            // Platform Thread는 너무 많으면 OOM 위험
            int safeCount = Math.min(concurrentCount, 1000);

            CountDownLatch latch = new CountDownLatch(safeCount);
            AtomicInteger completed = new AtomicInteger(0);

            Instant start = Instant.now();

            List<Thread> threads = new ArrayList<>();
            for (int i = 0; i < safeCount; i++) {
                Thread thread = Thread.ofPlatform().start(() -> {
                    try {
                        Thread.sleep(ioLatencyMs);
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

            String note = safeCount < concurrentCount ?
                    " (limited to " + safeCount + " for safety)" : "";

            return new BenchmarkResult("Platform Thread" + note,
                    "Massive Concurrent I/O", safeCount, duration);
        }

        public BenchmarkResult measureVirtual(int concurrentCount, int ioLatencyMs) throws Exception {
            CountDownLatch latch = new CountDownLatch(concurrentCount);
            AtomicInteger completed = new AtomicInteger(0);

            Instant start = Instant.now();

            List<Thread> threads = new ArrayList<>();
            for (int i = 0; i < concurrentCount; i++) {
                Thread thread = Thread.ofVirtual().start(() -> {
                    try {
                        Thread.sleep(ioLatencyMs);
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

            return new BenchmarkResult("Virtual Thread",
                    "Massive Concurrent I/O", concurrentCount, duration);
        }
    }

    /**
     * 벤치마크 실행
     */
    static class BenchmarkRunner {

        public void runAllBenchmarks() throws Exception {
            System.out.println("\n╔═══════════════════════════════════════════════════════════════════╗");
            System.out.println("║      I/O BENCHMARK - Platform vs Virtual Threads                 ║");
            System.out.println("╚═══════════════════════════════════════════════════════════════════╝");

            // 테스트 데이터 준비
            System.out.println("\n[Preparing test data...]");
            TestDataSetup.prepare();
            System.out.println("  ✓ Test data ready");

            List<BenchmarkResult> allResults = new ArrayList<>();

            // Warmup
            System.out.println("\n[Warmup Phase]");
            for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                System.out.print("  Warmup iteration " + (i + 1) + "/" + WARMUP_ITERATIONS + "...");
                new FileIOBenchmark().measurePlatform(10);
                System.out.println(" Done");
                System.gc();
                Thread.sleep(300);
            }

            System.out.println("\n[Measurement Phase]");

            // 1. File I/O
            System.out.println("\n1. File I/O Benchmark (100 files)");
            System.out.println("   " + "─".repeat(60));
            allResults.addAll(runFileIOBenchmark());

            // 2. Network I/O
            System.out.println("\n2. Network I/O Benchmark (100 requests, 50ms latency)");
            System.out.println("   " + "─".repeat(60));
            allResults.addAll(runNetworkIOBenchmark());

            // 3. Database I/O
            System.out.println("\n3. Database I/O Benchmark (100 queries, 30ms each)");
            System.out.println("   " + "─".repeat(60));
            allResults.addAll(runDatabaseIOBenchmark());

            // 4. Mixed I/O
            System.out.println("\n4. Mixed I/O Benchmark (50 tasks)");
            System.out.println("   " + "─".repeat(60));
            allResults.addAll(runMixedIOBenchmark());

            // 5. Massive Concurrent I/O
            System.out.println("\n5. Massive Concurrent I/O (10,000 operations)");
            System.out.println("   " + "─".repeat(60));
            allResults.addAll(runMassiveIOBenchmark());

            // 종합 분석
            printSummary(allResults);

            // Cleanup
            System.out.println("\n[Cleaning up test data...]");
            TestDataSetup.cleanup();
            System.out.println("  ✓ Cleanup complete");
        }

        private List<BenchmarkResult> runFileIOBenchmark() throws Exception {
            List<BenchmarkResult> results = new ArrayList<>();
            FileIOBenchmark benchmark = new FileIOBenchmark();

            // Platform Thread
            List<BenchmarkResult> platformResults = new ArrayList<>();
            for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
                platformResults.add(benchmark.measurePlatform(100));
                System.gc();
                Thread.sleep(200);
            }
            results.add(getMedianResult(platformResults));

            // Virtual Thread
            List<BenchmarkResult> virtualResults = new ArrayList<>();
            for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
                virtualResults.add(benchmark.measureVirtual(100));
                System.gc();
                Thread.sleep(200);
            }
            results.add(getMedianResult(virtualResults));

            // Thread Pool
            List<BenchmarkResult> poolResults = new ArrayList<>();
            for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
                poolResults.add(benchmark.measureThreadPool(100));
                System.gc();
                Thread.sleep(200);
            }
            results.add(getMedianResult(poolResults));

            for (BenchmarkResult result : results) {
                result.print();
            }

            return results;
        }

        private List<BenchmarkResult> runNetworkIOBenchmark() throws Exception {
            List<BenchmarkResult> results = new ArrayList<>();
            NetworkIOBenchmark benchmark = new NetworkIOBenchmark();

            List<BenchmarkResult> platformResults = new ArrayList<>();
            for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
                platformResults.add(benchmark.measurePlatform(100, 50));
                System.gc();
                Thread.sleep(200);
            }
            results.add(getMedianResult(platformResults));

            List<BenchmarkResult> virtualResults = new ArrayList<>();
            for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
                virtualResults.add(benchmark.measureVirtual(100, 50));
                System.gc();
                Thread.sleep(200);
            }
            results.add(getMedianResult(virtualResults));

            List<BenchmarkResult> poolResults = new ArrayList<>();
            for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
                poolResults.add(benchmark.measureThreadPool(100, 50));
                System.gc();
                Thread.sleep(200);
            }
            results.add(getMedianResult(poolResults));

            for (BenchmarkResult result : results) {
                result.print();
            }

            return results;
        }

        private List<BenchmarkResult> runDatabaseIOBenchmark() throws Exception {
            List<BenchmarkResult> results = new ArrayList<>();
            DatabaseIOBenchmark benchmark = new DatabaseIOBenchmark();

            List<BenchmarkResult> platformResults = new ArrayList<>();
            for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
                platformResults.add(benchmark.measurePlatform(100, 30));
                System.gc();
                Thread.sleep(200);
            }
            results.add(getMedianResult(platformResults));

            List<BenchmarkResult> virtualResults = new ArrayList<>();
            for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
                virtualResults.add(benchmark.measureVirtual(100, 30));
                System.gc();
                Thread.sleep(200);
            }
            results.add(getMedianResult(virtualResults));

            for (BenchmarkResult result : results) {
                result.print();
            }

            return results;
        }

        private List<BenchmarkResult> runMixedIOBenchmark() throws Exception {
            List<BenchmarkResult> results = new ArrayList<>();
            MixedIOBenchmark benchmark = new MixedIOBenchmark();

            List<BenchmarkResult> platformResults = new ArrayList<>();
            for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
                platformResults.add(benchmark.measurePlatform(50, 20));
                System.gc();
                Thread.sleep(200);
            }
            results.add(getMedianResult(platformResults));

            List<BenchmarkResult> virtualResults = new ArrayList<>();
            for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
                virtualResults.add(benchmark.measureVirtual(50, 20));
                System.gc();
                Thread.sleep(200);
            }
            results.add(getMedianResult(virtualResults));

            for (BenchmarkResult result : results) {
                result.print();
            }

            return results;
        }

        private List<BenchmarkResult> runMassiveIOBenchmark() throws Exception {
            List<BenchmarkResult> results = new ArrayList<>();
            MassiveConcurrentIOBenchmark benchmark = new MassiveConcurrentIOBenchmark();

            List<BenchmarkResult> platformResults = new ArrayList<>();
            for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
                platformResults.add(benchmark.measurePlatform(10000, 10));
                System.gc();
                Thread.sleep(500);
            }
            results.add(getMedianResult(platformResults));

            List<BenchmarkResult> virtualResults = new ArrayList<>();
            for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
                virtualResults.add(benchmark.measureVirtual(10000, 10));
                System.gc();
                Thread.sleep(500);
            }
            results.add(getMedianResult(virtualResults));

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
            System.out.println("  SUMMARY - I/O Performance Comparison");
            System.out.println("═".repeat(70));

            // I/O 타입별 그룹화
            results.stream()
                    .collect(java.util.stream.Collectors.groupingBy(r -> r.ioType))
                    .forEach((ioType, ioResults) -> {
                        System.out.println("\n  " + ioType + ":");

                        BenchmarkResult fastest = ioResults.stream()
                                .min((a, b) -> Long.compare(a.totalTimeMillis, b.totalTimeMillis))
                                .orElse(null);

                        ioResults.forEach(r -> {
                            double speedup = fastest != null ?
                                    (double) r.totalTimeMillis / fastest.totalTimeMillis : 1.0;
                            String badge = r == fastest ? " 🏆" : "";
                            System.out.println(String.format("    %s: %,d ms (%.2fx)%s - %.2f ops/sec",
                                    r.method, r.totalTimeMillis, speedup, badge, r.throughput));
                        });
                    });

            System.out.println("\n" + "═".repeat(70));
            System.out.println("  KEY INSIGHTS");
            System.out.println("═".repeat(70));
            System.out.println("  • Virtual Threads excel at I/O-bound workloads");
            System.out.println("  • Platform Threads waste resources during I/O waits");
            System.out.println("  • Virtual Threads can handle 10,000+ concurrent I/O operations");
            System.out.println("  • Best for: Web servers, API clients, database queries");
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