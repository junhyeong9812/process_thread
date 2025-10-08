package com.study.thread.structured;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 구조화된 동시성 패턴
 *
 * 구조화된 동시성을 활용한 실용적인 패턴들:
 * - Fork-Join 패턴
 * - Scatter-Gather 패턴
 * - Pipeline 패턴
 * - Circuit Breaker 패턴
 * - Retry 패턴
 * - Timeout 패턴
 */
public class StructuredPatterns {

    /**
     * 1. Fork-Join 패턴
     */
    static class ForkJoinPattern {
        public void demonstrate() throws Exception {
            System.out.println("  Fork-Join Pattern");

            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {

                // Fork: 여러 작업으로 분할
                System.out.println("    Forking tasks...");

                Future<Integer> task1 = executor.submit(() -> {
                    System.out.println("      [Task-1] Processing part 1");
                    Thread.sleep(100);
                    return 10;
                });

                Future<Integer> task2 = executor.submit(() -> {
                    System.out.println("      [Task-2] Processing part 2");
                    Thread.sleep(150);
                    return 20;
                });

                Future<Integer> task3 = executor.submit(() -> {
                    System.out.println("      [Task-3] Processing part 3");
                    Thread.sleep(80);
                    return 30;
                });

                // Join: 결과 수집 및 병합
                System.out.println("    Joining results...");
                int sum = task1.get() + task2.get() + task3.get();

                System.out.println("    Final result: " + sum);
            }
        }
    }

    /**
     * 2. Scatter-Gather 패턴
     */
    static class ScatterGatherPattern {
        static class DataSource {
            final String name;
            final int latency;

            DataSource(String name, int latency) {
                this.name = name;
                this.latency = latency;
            }

            String fetch() throws InterruptedException {
                Thread.sleep(latency);
                return "Data from " + name;
            }
        }

        public void demonstrate() throws Exception {
            System.out.println("  Scatter-Gather Pattern");

            List<DataSource> sources = List.of(
                    new DataSource("DB-1", 100),
                    new DataSource("DB-2", 150),
                    new DataSource("Cache", 50),
                    new DataSource("API", 200)
            );

            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {

                // Scatter: 여러 소스에 동시 요청
                System.out.println("    Scattering requests to " + sources.size() + " sources...");

                List<Future<String>> futures = sources.stream()
                        .map(source -> executor.submit(() -> {
                            System.out.println("      [" + source.name + "] Fetching...");
                            return source.fetch();
                        }))
                        .toList();

                // Gather: 결과 수집
                System.out.println("    Gathering results...");

                List<String> results = new ArrayList<>();
                for (Future<String> future : futures) {
                    try {
                        results.add(future.get());
                    } catch (Exception e) {
                        System.out.println("      Failed to get result: " + e.getMessage());
                    }
                }

                System.out.println("    Collected " + results.size() + " results:");
                results.forEach(r -> System.out.println("      - " + r));
            }
        }
    }

    /**
     * 3. Pipeline 패턴
     */
    static class PipelinePattern {
        static class PipelineStage<I, O> {
            final String name;
            final java.util.function.Function<I, O> processor;

            PipelineStage(String name, java.util.function.Function<I, O> processor) {
                this.name = name;
                this.processor = processor;
            }

            O process(I input) throws InterruptedException {
                System.out.println("      [" + name + "] Processing: " + input);
                Thread.sleep(50);
                return processor.apply(input);
            }
        }

        public void demonstrate() throws Exception {
            System.out.println("  Pipeline Pattern");

            // 파이프라인 단계 정의
            PipelineStage<String, String> stage1 = new PipelineStage<>(
                    "Stage-1-Validate",
                    input -> input.toUpperCase()
            );

            PipelineStage<String, String> stage2 = new PipelineStage<>(
                    "Stage-2-Transform",
                    input -> "[" + input + "]"
            );

            PipelineStage<String, Integer> stage3 = new PipelineStage<>(
                    "Stage-3-Analyze",
                    input -> input.length()
            );

            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {

                String input = "hello";
                System.out.println("    Input: " + input);

                // 순차 파이프라인 실행
                Future<String> result1 = executor.submit(() -> stage1.process(input));
                String intermediate1 = result1.get();

                Future<String> result2 = executor.submit(() -> stage2.process(intermediate1));
                String intermediate2 = result2.get();

                Future<Integer> result3 = executor.submit(() -> stage3.process(intermediate2));
                Integer finalResult = result3.get();

                System.out.println("    Final result: " + finalResult);
            }
        }
    }

    /**
     * 4. Circuit Breaker 패턴
     */
    static class CircuitBreakerPattern {
        enum State { CLOSED, OPEN, HALF_OPEN }

        static class CircuitBreaker {
            private State state = State.CLOSED;
            private final AtomicInteger failureCount = new AtomicInteger(0);
            private final int failureThreshold;
            private long openedAt;
            private final long cooldownMs;

            CircuitBreaker(int failureThreshold, long cooldownMs) {
                this.failureThreshold = failureThreshold;
                this.cooldownMs = cooldownMs;
            }

            <T> T execute(Callable<T> operation) throws Exception {
                if (state == State.OPEN) {
                    if (System.currentTimeMillis() - openedAt > cooldownMs) {
                        System.out.println("      Circuit: OPEN -> HALF_OPEN");
                        state = State.HALF_OPEN;
                    } else {
                        throw new RuntimeException("Circuit breaker is OPEN");
                    }
                }

                try {
                    T result = operation.call();
                    onSuccess();
                    return result;
                } catch (Exception e) {
                    onFailure();
                    throw e;
                }
            }

            private void onSuccess() {
                if (state == State.HALF_OPEN) {
                    System.out.println("      Circuit: HALF_OPEN -> CLOSED");
                    state = State.CLOSED;
                }
                failureCount.set(0);
            }

            private void onFailure() {
                int failures = failureCount.incrementAndGet();
                System.out.println("      Circuit: Failure " + failures + "/" + failureThreshold);

                if (failures >= failureThreshold && state == State.CLOSED) {
                    System.out.println("      Circuit: CLOSED -> OPEN");
                    state = State.OPEN;
                    openedAt = System.currentTimeMillis();
                }
            }

            State getState() {
                return state;
            }
        }

        public void demonstrate() throws Exception {
            System.out.println("  Circuit Breaker Pattern");

            CircuitBreaker breaker = new CircuitBreaker(3, 500);

            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {

                // 실패하는 작업들
                for (int i = 1; i <= 5; i++) {
                    final int attempt = i;

                    executor.submit(() -> {
                        try {
                            String result = breaker.execute(() -> {
                                System.out.println("      Attempt " + attempt);
                                if (attempt <= 3) {
                                    throw new RuntimeException("Service unavailable");
                                }
                                return "Success";
                            });
                            System.out.println("      Result: " + result);
                        } catch (Exception e) {
                            System.out.println("      Error: " + e.getMessage());
                        }
                        return null;
                    }).get();

                    Thread.sleep(100);
                }

                System.out.println("    Circuit state: " + breaker.getState());

                // Cooldown 후 재시도
                Thread.sleep(600);
                System.out.println("    After cooldown, retrying...");

                executor.submit(() -> {
                    try {
                        String result = breaker.execute(() -> "Recovered");
                        System.out.println("      Result: " + result);
                    } catch (Exception e) {
                        System.out.println("      Error: " + e.getMessage());
                    }
                    return null;
                }).get();

                System.out.println("    Final circuit state: " + breaker.getState());
            }
        }
    }

    /**
     * 5. Retry 패턴
     */
    static class RetryPattern {
        static class RetryPolicy {
            final int maxRetries;
            final long initialDelayMs;
            final double backoffMultiplier;

            RetryPolicy(int maxRetries, long initialDelayMs, double backoffMultiplier) {
                this.maxRetries = maxRetries;
                this.initialDelayMs = initialDelayMs;
                this.backoffMultiplier = backoffMultiplier;
            }

            <T> T execute(Callable<T> operation) throws Exception {
                Exception lastException = null;
                long delay = initialDelayMs;

                for (int attempt = 1; attempt <= maxRetries; attempt++) {
                    try {
                        System.out.println("      Attempt " + attempt + "/" + maxRetries);
                        return operation.call();
                    } catch (Exception e) {
                        lastException = e;
                        System.out.println("      Failed: " + e.getMessage());

                        if (attempt < maxRetries) {
                            System.out.println("      Retrying in " + delay + "ms...");
                            Thread.sleep(delay);
                            delay = (long) (delay * backoffMultiplier);
                        }
                    }
                }

                throw new RuntimeException("All retries failed", lastException);
            }
        }

        public void demonstrate() throws Exception {
            System.out.println("  Retry Pattern (Exponential Backoff)");

            RetryPolicy policy = new RetryPolicy(4, 100, 2.0);

            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {

                AtomicInteger attemptCount = new AtomicInteger(0);

                Future<String> future = executor.submit(() -> {
                    return policy.execute(() -> {
                        int count = attemptCount.incrementAndGet();

                        // 3번째 시도에서 성공
                        if (count < 3) {
                            throw new RuntimeException("Temporary failure");
                        }

                        return "Success on attempt " + count;
                    });
                });

                String result = future.get();
                System.out.println("    Result: " + result);
            }
        }
    }

    /**
     * 6. Timeout 패턴
     */
    static class TimeoutPattern {
        public void demonstrate() throws Exception {
            System.out.println("  Timeout Pattern");

            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {

                // 성공 케이스
                System.out.println("\n    Case 1: Task completes within timeout");
                Future<String> quickTask = executor.submit(() -> {
                    System.out.println("      [Quick] Working...");
                    Thread.sleep(100);
                    return "Quick result";
                });

                try {
                    String result = quickTask.get(500, TimeUnit.MILLISECONDS);
                    System.out.println("      Result: " + result);
                } catch (TimeoutException e) {
                    System.out.println("      Timeout!");
                    quickTask.cancel(true);
                }

                // 타임아웃 케이스
                System.out.println("\n    Case 2: Task exceeds timeout");
                Future<String> slowTask = executor.submit(() -> {
                    System.out.println("      [Slow] Working...");
                    Thread.sleep(1000);
                    return "Slow result";
                });

                try {
                    String result = slowTask.get(200, TimeUnit.MILLISECONDS);
                    System.out.println("      Result: " + result);
                } catch (TimeoutException e) {
                    System.out.println("      Timeout! Cancelling task...");
                    slowTask.cancel(true);
                    System.out.println("      Task cancelled: " + slowTask.isCancelled());
                }
            }
        }
    }

    /**
     * 7. Map-Reduce 패턴
     */
    static class MapReducePattern {
        public void demonstrate() throws Exception {
            System.out.println("  Map-Reduce Pattern");

            List<Integer> data = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {

                // Map 단계: 각 요소를 병렬 처리
                System.out.println("    Map phase: Processing " + data.size() + " items");

                List<Future<Integer>> mappedFutures = data.stream()
                        .map(item -> executor.submit(() -> {
                            System.out.println("      [Map] Processing: " + item);
                            Thread.sleep(50);
                            return item * item; // 제곱
                        }))
                        .toList();

                // 중간 결과 수집
                List<Integer> mappedResults = new ArrayList<>();
                for (Future<Integer> future : mappedFutures) {
                    mappedResults.add(future.get());
                }

                System.out.println("    Mapped results: " + mappedResults);

                // Reduce 단계: 결과 병합
                System.out.println("    Reduce phase: Aggregating results");

                Future<Integer> reducedFuture = executor.submit(() -> {
                    int sum = 0;
                    for (Integer value : mappedResults) {
                        System.out.println("      [Reduce] Adding: " + value);
                        sum += value;
                        Thread.sleep(20);
                    }
                    return sum;
                });

                Integer finalResult = reducedFuture.get();
                System.out.println("    Final result: " + finalResult);
            }
        }
    }

    /**
     * 8. Bulkhead 패턴 (격리)
     */
    static class BulkheadPattern {
        static class Bulkhead {
            private final Semaphore semaphore;
            private final String name;

            Bulkhead(String name, int maxConcurrent) {
                this.name = name;
                this.semaphore = new Semaphore(maxConcurrent);
            }

            <T> T execute(Callable<T> operation) throws Exception {
                boolean acquired = semaphore.tryAcquire(100, TimeUnit.MILLISECONDS);

                if (!acquired) {
                    throw new RejectedExecutionException("Bulkhead " + name + " is full");
                }

                try {
                    System.out.println("      [" + name + "] Executing (available: " +
                            semaphore.availablePermits() + ")");
                    return operation.call();
                } finally {
                    semaphore.release();
                }
            }
        }

        public void demonstrate() throws Exception {
            System.out.println("  Bulkhead Pattern (Resource Isolation)");

            // 두 개의 독립적인 리소스 풀
            Bulkhead criticalBulkhead = new Bulkhead("Critical", 2);
            Bulkhead normalBulkhead = new Bulkhead("Normal", 3);

            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {

                List<Future<?>> futures = new ArrayList<>();

                // Critical 작업들
                for (int i = 1; i <= 4; i++) {
                    final int taskId = i;
                    futures.add(executor.submit(() -> {
                        try {
                            criticalBulkhead.execute(() -> {
                                System.out.println("        [Critical-" + taskId + "] Working");
                                Thread.sleep(200);
                                return null;
                            });
                        } catch (Exception e) {
                            System.out.println("        [Critical-" + taskId + "] Rejected");
                        }
                        return null;
                    }));
                }

                // Normal 작업들
                for (int i = 1; i <= 4; i++) {
                    final int taskId = i;
                    futures.add(executor.submit(() -> {
                        try {
                            normalBulkhead.execute(() -> {
                                System.out.println("        [Normal-" + taskId + "] Working");
                                Thread.sleep(150);
                                return null;
                            });
                        } catch (Exception e) {
                            System.out.println("        [Normal-" + taskId + "] Rejected");
                        }
                        return null;
                    }));
                }

                for (Future<?> future : futures) {
                    future.get();
                }

                System.out.println("    Note: Critical and Normal pools are isolated");
            }
        }
    }

    /**
     * 9. 패턴 요약
     */
    static class PatternSummary {
        public void demonstrate() {
            System.out.println("\n  === Structured Concurrency Patterns Summary ===");

            System.out.println("\n  1. Fork-Join:");
            System.out.println("     - Split work, process in parallel, merge results");
            System.out.println("     - Use: Divide-and-conquer problems");

            System.out.println("\n  2. Scatter-Gather:");
            System.out.println("     - Query multiple sources simultaneously");
            System.out.println("     - Use: Aggregating data from multiple services");

            System.out.println("\n  3. Pipeline:");
            System.out.println("     - Sequential stages of processing");
            System.out.println("     - Use: Multi-step transformations");

            System.out.println("\n  4. Circuit Breaker:");
            System.out.println("     - Prevent cascading failures");
            System.out.println("     - Use: Protecting against failing dependencies");

            System.out.println("\n  5. Retry:");
            System.out.println("     - Retry failed operations with backoff");
            System.out.println("     - Use: Handling transient failures");

            System.out.println("\n  6. Timeout:");
            System.out.println("     - Limit operation execution time");
            System.out.println("     - Use: Preventing resource exhaustion");

            System.out.println("\n  7. Map-Reduce:");
            System.out.println("     - Parallel map, sequential reduce");
            System.out.println("     - Use: Large-scale data processing");

            System.out.println("\n  8. Bulkhead:");
            System.out.println("     - Isolate resources to prevent cascade");
            System.out.println("     - Use: Resource isolation and fault tolerance");

            System.out.println("\n  Key Benefits:");
            System.out.println("    ✓ Predictable resource management");
            System.out.println("    ✓ Clear error boundaries");
            System.out.println("    ✓ Automatic cleanup");
            System.out.println("    ✓ Improved reliability");
            System.out.println("    ✓ Better observability");
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== Structured Concurrency Patterns ===\n");

        // 1. Fork-Join
        System.out.println("1. Fork-Join Pattern");
        new ForkJoinPattern().demonstrate();
        System.out.println();

        // 2. Scatter-Gather
        System.out.println("2. Scatter-Gather Pattern");
        new ScatterGatherPattern().demonstrate();
        System.out.println();

        // 3. Pipeline
        System.out.println("3. Pipeline Pattern");
        new PipelinePattern().demonstrate();
        System.out.println();

        // 4. Circuit Breaker
        System.out.println("4. Circuit Breaker Pattern");
        new CircuitBreakerPattern().demonstrate();
        System.out.println();

        // 5. Retry
        System.out.println("5. Retry Pattern");
        new RetryPattern().demonstrate();
        System.out.println();

        // 6. Timeout
        System.out.println("6. Timeout Pattern");
        new TimeoutPattern().demonstrate();
        System.out.println();

        // 7. Map-Reduce
        System.out.println("7. Map-Reduce Pattern");
        new MapReducePattern().demonstrate();
        System.out.println();

        // 8. Bulkhead
        System.out.println("8. Bulkhead Pattern");
        new BulkheadPattern().demonstrate();
        System.out.println();

        // 9. 패턴 요약
        System.out.println("9. Pattern Summary");
        new PatternSummary().demonstrate();
        System.out.println();

        System.out.println("═══════════════════════════════════════════════");
        System.out.println("     ALL PATTERNS COMPLETED");
        System.out.println("═══════════════════════════════════════════════");
    }
}