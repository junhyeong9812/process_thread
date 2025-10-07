package com.study.thread.pool;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * FixedThreadPool 실습
 *
 * Executors.newFixedThreadPool()을 사용한 고정 크기 스레드 풀 학습
 * - 고정된 수의 워커 스레드
 * - 무제한 작업 큐 (LinkedBlockingQueue)
 * - 스레드 재사용
 * - CPU 바운드 작업에 적합
 */
public class FixedThreadPoolDemo {

    /**
     * 1. 기본 FixedThreadPool 사용법
     */
    static class BasicUsage {
        public void demonstrate() throws InterruptedException {
            System.out.println("  Creating FixedThreadPool with 3 threads");
            ExecutorService executor = Executors.newFixedThreadPool(3);

            // 작업 제출
            for (int i = 1; i <= 6; i++) {
                final int taskId = i;
                executor.submit(() -> {
                    System.out.println("    [Task-" + taskId + "] Running on " +
                            Thread.currentThread().getName());
                    sleep(200);
                    System.out.println("    [Task-" + taskId + "] Completed");
                });
            }

            // 우아한 종료
            executor.shutdown();
            if (executor.awaitTermination(5, TimeUnit.SECONDS)) {
                System.out.println("  All tasks completed");
            } else {
                System.out.println("  Timeout occurred");
            }
        }
    }

    /**
     * 2. Future를 사용한 결과 반환
     */
    static class FutureUsage {
        public void demonstrate() throws InterruptedException, ExecutionException {
            ExecutorService executor = Executors.newFixedThreadPool(3);

            List<Future<Integer>> futures = new ArrayList<>();

            // Callable 작업 제출
            for (int i = 1; i <= 5; i++) {
                final int taskId = i;
                Future<Integer> future = executor.submit(() -> {
                    System.out.println("    [Task-" + taskId + "] Computing...");
                    sleep(100);
                    int result = taskId * taskId;
                    System.out.println("    [Task-" + taskId + "] Result: " + result);
                    return result;
                });
                futures.add(future);
            }

            // 결과 수집
            System.out.println("  Collecting results:");
            int sum = 0;
            for (int i = 0; i < futures.size(); i++) {
                int result = futures.get(i).get();
                sum += result;
                System.out.println("    Task-" + (i + 1) + " result: " + result);
            }
            System.out.println("  Total sum: " + sum);

            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    /**
     * 3. invokeAll을 사용한 일괄 처리
     */
    static class InvokeAllUsage {
        public void demonstrate() throws InterruptedException {
            ExecutorService executor = Executors.newFixedThreadPool(3);

            List<Callable<String>> tasks = new ArrayList<>();
            for (int i = 1; i <= 5; i++) {
                final int taskId = i;
                tasks.add(() -> {
                    sleep(100 + taskId * 20);
                    return "Result-" + taskId;
                });
            }

            System.out.println("  Invoking all tasks...");
            List<Future<String>> futures = executor.invokeAll(tasks);

            System.out.println("  All tasks completed, collecting results:");
            for (int i = 0; i < futures.size(); i++) {
                try {
                    String result = futures.get(i).get();
                    System.out.println("    Task-" + (i + 1) + ": " + result);
                } catch (ExecutionException e) {
                    System.out.println("    Task-" + (i + 1) + " failed: " +
                            e.getCause().getMessage());
                }
            }

            executor.shutdown();
        }
    }

    /**
     * 4. invokeAny를 사용한 최초 완료 대기
     */
    static class InvokeAnyUsage {
        public void demonstrate() throws InterruptedException, ExecutionException {
            ExecutorService executor = Executors.newFixedThreadPool(3);

            List<Callable<String>> tasks = new ArrayList<>();
            tasks.add(() -> {
                System.out.println("    [Slow] Starting (500ms)");
                sleep(500);
                return "Slow Result";
            });
            tasks.add(() -> {
                System.out.println("    [Medium] Starting (200ms)");
                sleep(200);
                return "Medium Result";
            });
            tasks.add(() -> {
                System.out.println("    [Fast] Starting (100ms)");
                sleep(100);
                return "Fast Result";
            });

            System.out.println("  Invoking any (first to complete)...");
            String result = executor.invokeAny(tasks);
            System.out.println("  First result: " + result);

            executor.shutdown();
        }
    }

    /**
     * 5. 예외 처리
     */
    static class ExceptionHandling {
        public void demonstrate() throws InterruptedException {
            ExecutorService executor = Executors.newFixedThreadPool(2);

            // 성공하는 작업
            Future<Integer> successFuture = executor.submit(() -> {
                System.out.println("    [Success Task] Running");
                return 42;
            });

            // 실패하는 작업
            Future<Integer> failureFuture = executor.submit(() -> {
                System.out.println("    [Failure Task] Running");
                throw new RuntimeException("Task failed!");
            });

            // 결과 확인
            try {
                Integer result = successFuture.get();
                System.out.println("  Success task result: " + result);
            } catch (ExecutionException e) {
                System.out.println("  Success task failed: " + e.getCause());
            }

            try {
                Integer result = failureFuture.get();
                System.out.println("  Failure task result: " + result);
            } catch (ExecutionException e) {
                System.out.println("  Failure task failed: " + e.getCause().getMessage());
            }

            executor.shutdown();
        }
    }

    /**
     * 6. 타임아웃 처리
     */
    static class TimeoutHandling {
        public void demonstrate() throws InterruptedException {
            ExecutorService executor = Executors.newFixedThreadPool(2);

            Future<String> future = executor.submit(() -> {
                System.out.println("    [Long Task] Starting (will take 2 seconds)");
                sleep(2000);
                return "Completed";
            });

            try {
                System.out.println("  Waiting for result (timeout: 500ms)...");
                String result = future.get(500, TimeUnit.MILLISECONDS);
                System.out.println("  Result: " + result);
            } catch (TimeoutException e) {
                System.out.println("  Timeout! Cancelling task...");
                future.cancel(true);
                System.out.println("  Task cancelled: " + future.isCancelled());
            } catch (ExecutionException e) {
                System.out.println("  Task failed: " + e.getCause());
            }

            executor.shutdown();
        }
    }

    /**
     * 7. CompletionService 사용
     */
    static class CompletionServiceUsage {
        public void demonstrate() throws InterruptedException, ExecutionException {
            ExecutorService executor = Executors.newFixedThreadPool(3);
            CompletionService<String> completionService =
                    new ExecutorCompletionService<>(executor);

            // 다양한 실행 시간을 가진 작업 제출
            int taskCount = 5;
            for (int i = 1; i <= taskCount; i++) {
                final int taskId = i;
                completionService.submit(() -> {
                    int duration = 100 + (taskId * 50);
                    System.out.println("    [Task-" + taskId + "] Starting (" +
                            duration + "ms)");
                    sleep(duration);
                    return "Result-" + taskId;
                });
            }

            // 완료되는 순서대로 결과 처리
            System.out.println("  Processing results as they complete:");
            for (int i = 0; i < taskCount; i++) {
                Future<String> future = completionService.take();
                String result = future.get();
                System.out.println("    Completed: " + result);
            }

            executor.shutdown();
        }
    }

    /**
     * 8. 작업 취소
     */
    static class TaskCancellation {
        public void demonstrate() throws InterruptedException {
            ExecutorService executor = Executors.newFixedThreadPool(2);

            AtomicInteger counter = new AtomicInteger(0);

            Future<?> future = executor.submit(() -> {
                System.out.println("    [Task] Starting long computation");
                try {
                    for (int i = 0; i < 100; i++) {
                        if (Thread.interrupted()) {
                            System.out.println("    [Task] Interrupted at iteration " + i);
                            return;
                        }
                        counter.incrementAndGet();
                        Thread.sleep(50);
                    }
                    System.out.println("    [Task] Completed all iterations");
                } catch (InterruptedException e) {
                    System.out.println("    [Task] Interrupted during sleep");
                    Thread.currentThread().interrupt();
                }
            });

            Thread.sleep(300); // 작업 실행 시간

            System.out.println("  Cancelling task...");
            boolean cancelled = future.cancel(true);
            System.out.println("  Task cancelled: " + cancelled);
            System.out.println("  Iterations completed: " + counter.get());

            executor.shutdown();
        }
    }

    /**
     * 9. 성능 측정
     */
    static class PerformanceMeasurement {
        public void demonstrate() throws InterruptedException, ExecutionException {
            int taskCount = 100;

            // Single thread
            long start1 = System.currentTimeMillis();
            for (int i = 0; i < taskCount; i++) {
                computeTask();
            }
            long time1 = System.currentTimeMillis() - start1;

            // Fixed thread pool
            ExecutorService executor = Executors.newFixedThreadPool(
                    Runtime.getRuntime().availableProcessors());

            long start2 = System.currentTimeMillis();
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < taskCount; i++) {
                futures.add(executor.submit(() -> computeTask()));
            }

            for (Future<?> future : futures) {
                future.get();
            }
            long time2 = System.currentTimeMillis() - start2;

            System.out.println("  Single thread time: " + time1 + " ms");
            System.out.println("  Thread pool time: " + time2 + " ms");
            System.out.println("  Speedup: " + String.format("%.2fx", (double) time1 / time2));

            executor.shutdown();
        }

        private void computeTask() {
            // CPU 작업 시뮬레이션
            long sum = 0;
            for (int i = 0; i < 100000; i++) {
                sum += i;
            }
        }
    }

    /**
     * 10. 적절한 풀 크기 결정
     */
    static class PoolSizeComparison {
        public void demonstrate() throws InterruptedException, ExecutionException {
            int[] poolSizes = {1, 2, 4, 8, 16};
            int taskCount = 50;

            System.out.println("  Comparing different pool sizes:");

            for (int poolSize : poolSizes) {
                ExecutorService executor = Executors.newFixedThreadPool(poolSize);

                long start = System.currentTimeMillis();
                List<Future<?>> futures = new ArrayList<>();

                for (int i = 0; i < taskCount; i++) {
                    futures.add(executor.submit(() -> {
                        sleep(100); // I/O 작업 시뮬레이션
                    }));
                }

                for (Future<?> future : futures) {
                    future.get();
                }

                long duration = System.currentTimeMillis() - start;

                System.out.println("    Pool size " + poolSize + ": " + duration + " ms");

                executor.shutdown();
            }

            int optimalSize = Runtime.getRuntime().availableProcessors();
            System.out.println("\n  Optimal pool size for CPU-bound tasks: " + optimalSize);
            System.out.println("  Optimal pool size for I/O-bound tasks: " +
                    (optimalSize * 2) + " (approx)");
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== FixedThreadPool Demo ===\n");

        // 1. 기본 사용법
        System.out.println("1. Basic Usage");
        new BasicUsage().demonstrate();
        System.out.println();

        // 2. Future 사용
        System.out.println("2. Future Usage");
        new FutureUsage().demonstrate();
        System.out.println();

        // 3. invokeAll
        System.out.println("3. invokeAll Usage");
        new InvokeAllUsage().demonstrate();
        System.out.println();

        // 4. invokeAny
        System.out.println("4. invokeAny Usage");
        new InvokeAnyUsage().demonstrate();
        System.out.println();

        // 5. 예외 처리
        System.out.println("5. Exception Handling");
        new ExceptionHandling().demonstrate();
        System.out.println();

        // 6. 타임아웃 처리
        System.out.println("6. Timeout Handling");
        new TimeoutHandling().demonstrate();
        System.out.println();

        // 7. CompletionService
        System.out.println("7. CompletionService Usage");
        new CompletionServiceUsage().demonstrate();
        System.out.println();

        // 8. 작업 취소
        System.out.println("8. Task Cancellation");
        new TaskCancellation().demonstrate();
        System.out.println();

        // 9. 성능 측정
        System.out.println("9. Performance Measurement");
        new PerformanceMeasurement().demonstrate();
        System.out.println();

        // 10. 풀 크기 비교
        System.out.println("10. Pool Size Comparison");
        new PoolSizeComparison().demonstrate();
        System.out.println();

        System.out.println("=== Demo Completed ===");
    }

    private static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}