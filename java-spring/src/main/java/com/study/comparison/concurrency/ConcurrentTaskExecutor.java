package com.study.comparison.concurrency;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 동시 작업 실행 비교
 *
 * 다양한 동시성 실행 패턴의 성능과 특성을 비교합니다:
 * 1. Sequential Execution: 순차 실행 (베이스라인)
 * 2. Parallel Execution: 병렬 실행
 * 3. Concurrent Execution: 동시 실행
 * 4. Batch Execution: 배치 실행
 * 5. Pipeline Execution: 파이프라인 실행
 *
 * 비교 항목:
 * - Execution Time: 실행 시간
 * - Throughput: 처리량
 * - Resource Usage: 리소스 사용량
 * - Scalability: 확장성
 * - Error Handling: 오류 처리
 *
 * 목표:
 * - 각 실행 패턴의 장단점 파악
 * - 워크로드별 최적 패턴 선택
 */
public class ConcurrentTaskExecutor {

    /**
     * 실행 결과
     */
    static class ExecutionResult {
        final String executionPattern;
        final int taskCount;
        final long executionTimeMs;
        final int successCount;
        final int failureCount;
        final double throughput; // tasks/sec
        final long avgTaskTimeMs;
        final Map<String, Object> metadata;

        ExecutionResult(String executionPattern, int taskCount, long executionTimeMs,
                        int successCount, int failureCount, Map<String, Object> metadata) {
            this.executionPattern = executionPattern;
            this.taskCount = taskCount;
            this.executionTimeMs = executionTimeMs;
            this.successCount = successCount;
            this.failureCount = failureCount;
            this.throughput = executionTimeMs > 0 ?
                    (taskCount * 1000.0) / executionTimeMs : 0;
            this.avgTaskTimeMs = taskCount > 0 ? executionTimeMs / taskCount : 0;
            this.metadata = metadata != null ? metadata : new HashMap<>();
        }

        void print() {
            System.out.println("\n  === " + executionPattern + " ===");
            System.out.println("  Total tasks: " + String.format("%,d", taskCount));
            System.out.println("  Execution time: " + String.format("%,d ms", executionTimeMs));
            System.out.println("  Success: " + successCount + " / Failure: " + failureCount);
            System.out.println("  Throughput: " + String.format("%.2f tasks/sec", throughput));
            System.out.println("  Avg task time: " + avgTaskTimeMs + " ms");

            if (!metadata.isEmpty()) {
                System.out.println("  Metadata:");
                metadata.forEach((key, value) ->
                        System.out.println("    " + key + ": " + value));
            }
        }
    }

    /**
     * 테스트용 Task
     */
    static class TestTask implements Callable<String> {
        private final int taskId;
        private final int processingTimeMs;
        private final double failureRate;
        private static final Random random = new Random();

        TestTask(int taskId, int processingTimeMs, double failureRate) {
            this.taskId = taskId;
            this.processingTimeMs = processingTimeMs;
            this.failureRate = failureRate;
        }

        @Override
        public String call() throws Exception {
            // 실패 시뮬레이션
            if (random.nextDouble() < failureRate) {
                throw new RuntimeException("Task-" + taskId + " failed");
            }

            // 처리 시뮬레이션
            Thread.sleep(processingTimeMs);

            return "Task-" + taskId + " completed";
        }
    }

    /**
     * 1. Sequential Execution (순차 실행)
     */
    static class SequentialExecutor {

        public ExecutionResult execute(List<TestTask> tasks) {
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failureCount = new AtomicInteger(0);

            Instant start = Instant.now();

            for (TestTask task : tasks) {
                try {
                    task.call();
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                }
            }

            long executionTime = Duration.between(start, Instant.now()).toMillis();

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("Pattern", "Single-threaded");
            metadata.put("Concurrency Level", 1);

            return new ExecutionResult(
                    "Sequential Execution",
                    tasks.size(),
                    executionTime,
                    successCount.get(),
                    failureCount.get(),
                    metadata
            );
        }
    }

    /**
     * 2. Parallel Execution (병렬 실행 - Platform Threads)
     */
    static class ParallelExecutor {

        public ExecutionResult execute(List<TestTask> tasks, int parallelism)
                throws InterruptedException {
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failureCount = new AtomicInteger(0);
            CountDownLatch latch = new CountDownLatch(tasks.size());

            Instant start = Instant.now();

            // Semaphore로 병렬 수준 제어
            Semaphore semaphore = new Semaphore(parallelism);

            List<Thread> threads = new ArrayList<>();
            for (TestTask task : tasks) {
                Thread thread = Thread.ofPlatform().start(() -> {
                    try {
                        semaphore.acquire();
                        try {
                            task.call();
                            successCount.incrementAndGet();
                        } catch (Exception e) {
                            failureCount.incrementAndGet();
                        } finally {
                            semaphore.release();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        latch.countDown();
                    }
                });
                threads.add(thread);
            }

            latch.await();
            long executionTime = Duration.between(start, Instant.now()).toMillis();

            for (Thread thread : threads) {
                thread.join();
            }

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("Pattern", "Platform Threads");
            metadata.put("Concurrency Level", parallelism);

            return new ExecutionResult(
                    "Parallel Execution (Platform)",
                    tasks.size(),
                    executionTime,
                    successCount.get(),
                    failureCount.get(),
                    metadata
            );
        }
    }

    /**
     * 3. Concurrent Execution (동시 실행 - Virtual Threads)
     */
    static class ConcurrentExecutor {

        public ExecutionResult execute(List<TestTask> tasks) throws InterruptedException {
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failureCount = new AtomicInteger(0);
            CountDownLatch latch = new CountDownLatch(tasks.size());

            Instant start = Instant.now();

            List<Thread> threads = new ArrayList<>();
            for (TestTask task : tasks) {
                Thread thread = Thread.ofVirtual().start(() -> {
                    try {
                        task.call();
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        failureCount.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
                threads.add(thread);
            }

            latch.await();
            long executionTime = Duration.between(start, Instant.now()).toMillis();

            for (Thread thread : threads) {
                thread.join();
            }

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("Pattern", "Virtual Threads");
            metadata.put("Concurrency Level", "Unlimited");

            return new ExecutionResult(
                    "Concurrent Execution (Virtual)",
                    tasks.size(),
                    executionTime,
                    successCount.get(),
                    failureCount.get(),
                    metadata
            );
        }
    }

    /**
     * 4. Batch Execution (배치 실행 - Thread Pool)
     */
    static class BatchExecutor {

        public ExecutionResult execute(List<TestTask> tasks, int batchSize)
                throws Exception {
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failureCount = new AtomicInteger(0);

            int poolSize = Runtime.getRuntime().availableProcessors();
            ExecutorService executor = Executors.newFixedThreadPool(poolSize);

            Instant start = Instant.now();

            // 배치로 분할
            List<List<TestTask>> batches = partitionList(tasks, batchSize);
            int totalBatches = batches.size();

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("Pattern", "Fixed Thread Pool");
            metadata.put("Pool Size", poolSize);
            metadata.put("Batch Size", batchSize);
            metadata.put("Total Batches", totalBatches);

            for (int i = 0; i < batches.size(); i++) {
                List<TestTask> batch = batches.get(i);

                List<Future<String>> futures = new ArrayList<>();
                for (TestTask task : batch) {
                    Future<String> future = executor.submit(task);
                    futures.add(future);
                }

                // 배치 완료 대기
                for (Future<String> future : futures) {
                    try {
                        future.get();
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        failureCount.incrementAndGet();
                    }
                }
            }

            long executionTime = Duration.between(start, Instant.now()).toMillis();

            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);

            return new ExecutionResult(
                    "Batch Execution",
                    tasks.size(),
                    executionTime,
                    successCount.get(),
                    failureCount.get(),
                    metadata
            );
        }

        private <T> List<List<T>> partitionList(List<T> list, int batchSize) {
            List<List<T>> partitions = new ArrayList<>();
            for (int i = 0; i < list.size(); i += batchSize) {
                partitions.add(list.subList(i, Math.min(i + batchSize, list.size())));
            }
            return partitions;
        }
    }

    /**
     * 5. Pipeline Execution (파이프라인 실행)
     */
    static class PipelineExecutor {

        static class PipelineStage {
            final String name;
            final BlockingQueue<TestTask> inputQueue;
            final BlockingQueue<String> outputQueue;
            final int workerCount;

            PipelineStage(String name, int workerCount, int queueSize) {
                this.name = name;
                this.inputQueue = new LinkedBlockingQueue<>(queueSize);
                this.outputQueue = new LinkedBlockingQueue<>(queueSize);
                this.workerCount = workerCount;
            }
        }

        public ExecutionResult execute(List<TestTask> tasks, int stageWorkers)
                throws Exception {
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failureCount = new AtomicInteger(0);

            // 3단계 파이프라인
            PipelineStage stage1 = new PipelineStage("Validation", stageWorkers, 100);
            PipelineStage stage2 = new PipelineStage("Processing", stageWorkers, 100);
            PipelineStage stage3 = new PipelineStage("Completion", stageWorkers, 100);

            Instant start = Instant.now();

            // Stage 1 Workers
            List<Thread> stage1Workers = startStageWorkers(stage1, task -> {
                try {
                    task.call(); // Validation
                    stage2.inputQueue.put(task);
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                }
            });

            // Stage 2 Workers
            List<Thread> stage2Workers = startStageWorkers(stage2, task -> {
                try {
                    String result = task.call(); // Processing
                    stage3.outputQueue.put(result);
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                }
            });

            // Stage 3 Workers (Output)
            CountDownLatch completionLatch = new CountDownLatch(tasks.size() - failureCount.get());
            List<Thread> stage3Workers = new ArrayList<>();
            for (int i = 0; i < stageWorkers; i++) {
                Thread worker = Thread.ofVirtual().start(() -> {
                    while (!Thread.currentThread().isInterrupted()) {
                        try {
                            String result = stage3.outputQueue.poll(100, TimeUnit.MILLISECONDS);
                            if (result != null) {
                                successCount.incrementAndGet();
                                completionLatch.countDown();
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                });
                stage3Workers.add(worker);
            }

            // 작업 투입
            for (TestTask task : tasks) {
                stage1.inputQueue.put(task);
            }

            // 완료 대기 (타임아웃 설정)
            boolean completed = completionLatch.await(30, TimeUnit.SECONDS);

            long executionTime = Duration.between(start, Instant.now()).toMillis();

            // 모든 워커 종료
            stage1Workers.forEach(Thread::interrupt);
            stage2Workers.forEach(Thread::interrupt);
            stage3Workers.forEach(Thread::interrupt);

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("Pattern", "Pipeline");
            metadata.put("Stages", 3);
            metadata.put("Workers per Stage", stageWorkers);
            metadata.put("Completed", completed);

            return new ExecutionResult(
                    "Pipeline Execution",
                    tasks.size(),
                    executionTime,
                    successCount.get(),
                    failureCount.get(),
                    metadata
            );
        }

        private List<Thread> startStageWorkers(PipelineStage stage,
                                               java.util.function.Consumer<TestTask> processor) {
            List<Thread> workers = new ArrayList<>();
            for (int i = 0; i < stage.workerCount; i++) {
                Thread worker = Thread.ofVirtual().start(() -> {
                    while (!Thread.currentThread().isInterrupted()) {
                        try {
                            TestTask task = stage.inputQueue.poll(100, TimeUnit.MILLISECONDS);
                            if (task != null) {
                                processor.accept(task);
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                });
                workers.add(worker);
            }
            return workers;
        }
    }

    /**
     * 6. Fork-Join Execution
     */
    static class ForkJoinExecutor {

        static class TaskGroup extends RecursiveTask<Integer> {
            private final List<TestTask> tasks;
            private final int threshold;
            private final AtomicInteger failureCount;

            TaskGroup(List<TestTask> tasks, int threshold, AtomicInteger failureCount) {
                this.tasks = tasks;
                this.threshold = threshold;
                this.failureCount = failureCount;
            }

            @Override
            protected Integer compute() {
                if (tasks.size() <= threshold) {
                    // 직접 실행
                    int success = 0;
                    for (TestTask task : tasks) {
                        try {
                            task.call();
                            success++;
                        } catch (Exception e) {
                            failureCount.incrementAndGet();
                        }
                    }
                    return success;
                } else {
                    // 분할
                    int mid = tasks.size() / 2;
                    TaskGroup left = new TaskGroup(
                            tasks.subList(0, mid), threshold, failureCount);
                    TaskGroup right = new TaskGroup(
                            tasks.subList(mid, tasks.size()), threshold, failureCount);

                    left.fork();
                    int rightResult = right.compute();
                    int leftResult = left.join();

                    return leftResult + rightResult;
                }
            }
        }

        public ExecutionResult execute(List<TestTask> tasks, int threshold) {
            AtomicInteger failureCount = new AtomicInteger(0);
            ForkJoinPool pool = new ForkJoinPool();

            Instant start = Instant.now();

            TaskGroup rootTask = new TaskGroup(tasks, threshold, failureCount);
            int successCount = pool.invoke(rootTask);

            long executionTime = Duration.between(start, Instant.now()).toMillis();

            pool.shutdown();

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("Pattern", "Fork-Join");
            metadata.put("Threshold", threshold);
            metadata.put("Parallelism", pool.getParallelism());

            return new ExecutionResult(
                    "Fork-Join Execution",
                    tasks.size(),
                    executionTime,
                    successCount,
                    failureCount.get(),
                    metadata
            );
        }
    }

    /**
     * 비교 분석
     */
    static class ExecutionAnalyzer {

        public void compare(List<ExecutionResult> results) {
            System.out.println("\n" + "═".repeat(70));
            System.out.println("  EXECUTION PATTERN COMPARISON");
            System.out.println("═".repeat(70));

            if (results.isEmpty()) return;

            // 1. 실행 시간 비교
            System.out.println("\n  1. Execution Time:");
            ExecutionResult fastest = results.stream()
                    .min(Comparator.comparingLong(r -> r.executionTimeMs))
                    .orElse(null);

            results.forEach(r -> {
                double speedup = fastest != null ?
                        (double) fastest.executionTimeMs / r.executionTimeMs : 1.0;
                String badge = r == fastest ? " 🏆 FASTEST" : "";
                System.out.println(String.format("    %s: %,d ms (%.2fx)%s",
                        r.executionPattern, r.executionTimeMs, speedup, badge));
            });

            // 2. 처리량 비교
            System.out.println("\n  2. Throughput:");
            ExecutionResult highestThroughput = results.stream()
                    .max(Comparator.comparingDouble(r -> r.throughput))
                    .orElse(null);

            results.forEach(r -> {
                double ratio = highestThroughput != null ?
                        r.throughput / highestThroughput.throughput : 1.0;
                String badge = r == highestThroughput ? " 🏆 HIGHEST" : "";
                System.out.println(String.format("    %s: %.2f tasks/sec (%.1f%%)%s",
                        r.executionPattern, r.throughput, ratio * 100, badge));
            });

            // 3. 성공률 비교
            System.out.println("\n  3. Success Rate:");
            results.forEach(r -> {
                double successRate = r.taskCount > 0 ?
                        (r.successCount * 100.0) / r.taskCount : 0;
                System.out.println(String.format("    %s: %.1f%% (%d/%d)",
                        r.executionPattern, successRate, r.successCount, r.taskCount));
            });

            // 4. 패턴별 특성
            System.out.println("\n  4. Pattern Characteristics:");
            results.forEach(r -> {
                System.out.println("    " + r.executionPattern + ":");
                r.metadata.forEach((key, value) ->
                        System.out.println("      - " + key + ": " + value));
            });
        }

        public void printRecommendations() {
            System.out.println("\n" + "═".repeat(70));
            System.out.println("  RECOMMENDATIONS");
            System.out.println("═".repeat(70));

            System.out.println("\n  Sequential Execution:");
            System.out.println("    ✓ Use for: Simple tasks, debugging, guaranteed order");
            System.out.println("    ✗ Avoid for: I/O-heavy, large task sets");

            System.out.println("\n  Parallel Execution (Platform Threads):");
            System.out.println("    ✓ Use for: CPU-bound tasks, controlled parallelism");
            System.out.println("    ✗ Avoid for: Massive concurrency (>1000 threads)");

            System.out.println("\n  Concurrent Execution (Virtual Threads):");
            System.out.println("    ✓ Use for: I/O-bound, high concurrency, microservices");
            System.out.println("    ✗ Avoid for: CPU-intensive, Java <21");

            System.out.println("\n  Batch Execution:");
            System.out.println("    ✓ Use for: Rate limiting, resource control, stability");
            System.out.println("    ✗ Avoid for: Real-time processing");

            System.out.println("\n  Pipeline Execution:");
            System.out.println("    ✓ Use for: Multi-stage processing, data transformation");
            System.out.println("    ✗ Avoid for: Simple single-step tasks");

            System.out.println("\n  Fork-Join Execution:");
            System.out.println("    ✓ Use for: Divide-and-conquer, recursive tasks");
            System.out.println("    ✗ Avoid for: I/O-heavy, unbalanced workloads");
        }
    }

    /**
     * 종합 테스트
     */
    static class ComprehensiveTest {

        public void runTests() throws Exception {
            System.out.println("\n╔═══════════════════════════════════════════════════════════════════╗");
            System.out.println("║      CONCURRENT TASK EXECUTOR COMPARISON                          ║");
            System.out.println("╚═══════════════════════════════════════════════════════════════════╝");

            int taskCount = 500;
            int processingTime = 10; // ms
            double failureRate = 0.05; // 5% failure

            // 작업 생성
            List<TestTask> tasks = new ArrayList<>();
            for (int i = 0; i < taskCount; i++) {
                tasks.add(new TestTask(i, processingTime, failureRate));
            }

            List<ExecutionResult> results = new ArrayList<>();

            // 1. Sequential
            System.out.println("\n1. Testing Sequential Execution...");
            SequentialExecutor sequential = new SequentialExecutor();
            results.add(sequential.execute(tasks));

            // 2. Parallel (Platform)
            System.out.println("2. Testing Parallel Execution (Platform Threads)...");
            ParallelExecutor parallel = new ParallelExecutor();
            results.add(parallel.execute(tasks,
                    Runtime.getRuntime().availableProcessors() * 2));

            // 3. Concurrent (Virtual)
            System.out.println("3. Testing Concurrent Execution (Virtual Threads)...");
            ConcurrentExecutor concurrent = new ConcurrentExecutor();
            results.add(concurrent.execute(tasks));

            // 4. Batch
            System.out.println("4. Testing Batch Execution...");
            BatchExecutor batch = new BatchExecutor();
            results.add(batch.execute(tasks, 50)); // 50 tasks per batch

            // 5. Pipeline
            System.out.println("5. Testing Pipeline Execution...");
            PipelineExecutor pipeline = new PipelineExecutor();
            results.add(pipeline.execute(tasks, 4)); // 4 workers per stage

            // 6. Fork-Join
            System.out.println("6. Testing Fork-Join Execution...");
            ForkJoinExecutor forkJoin = new ForkJoinExecutor();
            results.add(forkJoin.execute(tasks, 25)); // threshold = 25

            // 결과 출력
            System.out.println("\n" + "═".repeat(70));
            System.out.println("  RESULTS");
            System.out.println("═".repeat(70));
            results.forEach(ExecutionResult::print);

            // 분석
            ExecutionAnalyzer analyzer = new ExecutionAnalyzer();
            analyzer.compare(results);
            analyzer.printRecommendations();
        }
    }

    public static void main(String[] args) throws Exception {
        ComprehensiveTest test = new ComprehensiveTest();
        test.runTests();

        System.out.println("\n╔═══════════════════════════════════════════════════════════════════╗");
        System.out.println("║         COMPARISON COMPLETED                                      ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════╝");
    }
}