package com.study.thread.structured;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * StructuredTaskScope 실습 (Java 21+)
 *
 * 구조화된 동시성(Structured Concurrency)은 동시 작업의 생명주기를 명확히 합니다:
 * - 부모-자식 관계의 작업 구조
 * - 자동 리소스 관리 (try-with-resources)
 * - 예외 전파 및 취소
 * - 명확한 작업 범위(scope)
 *
 * 주의: Java 21 이상 필요 (Preview Feature)
 */
public class StructuredTaskScopeDemo {

    /**
     * 1. 기본 StructuredTaskScope 사용
     */
    static class BasicUsage {
        public void demonstrate() {
            System.out.println("  Basic StructuredTaskScope usage");

            try {
                // Java 21+ 구문 (Preview)
                /*
                try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
                    Future<String> task1 = scope.fork(() -> {
                        System.out.println("    [Task-1] Starting");
                        Thread.sleep(100);
                        return "Result-1";
                    });

                    Future<String> task2 = scope.fork(() -> {
                        System.out.println("    [Task-2] Starting");
                        Thread.sleep(150);
                        return "Result-2";
                    });

                    scope.join();
                    scope.throwIfFailed();

                    System.out.println("    Task-1: " + task1.resultNow());
                    System.out.println("    Task-2: " + task2.resultNow());
                }
                */

                System.out.println("    Note: StructuredTaskScope requires Java 21+");
                System.out.println("    Demonstrating concept with ExecutorService...");

                // Java 19 이하에서의 대체 구현
                demonstrateWithExecutor();

            } catch (Exception e) {
                System.out.println("    Error: " + e.getMessage());
            }
        }

        private void demonstrateWithExecutor() throws InterruptedException, ExecutionException {
            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                Future<String> task1 = executor.submit(() -> {
                    System.out.println("    [Task-1] Starting");
                    Thread.sleep(100);
                    return "Result-1";
                });

                Future<String> task2 = executor.submit(() -> {
                    System.out.println("    [Task-2] Starting");
                    Thread.sleep(150);
                    return "Result-2";
                });

                System.out.println("    Task-1: " + task1.get());
                System.out.println("    Task-2: " + task2.get());
            }
        }
    }

    /**
     * 2. ShutdownOnFailure - 하나 실패 시 모두 취소
     */
    static class ShutdownOnFailureDemo {
        public void demonstrate() {
            System.out.println("  ShutdownOnFailure pattern");

            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {

                Future<String> successTask = executor.submit(() -> {
                    System.out.println("    [Success-Task] Running");
                    Thread.sleep(500);
                    return "Success";
                });

                Future<String> failTask = executor.submit(() -> {
                    System.out.println("    [Fail-Task] Running");
                    Thread.sleep(100);
                    throw new RuntimeException("Task failed!");
                });

                Future<String> slowTask = executor.submit(() -> {
                    System.out.println("    [Slow-Task] Running");
                    Thread.sleep(1000);
                    return "Slow result";
                });

                // 실패 감지
                try {
                    failTask.get();
                } catch (ExecutionException e) {
                    System.out.println("    Failure detected: " + e.getCause().getMessage());
                    System.out.println("    Cancelling other tasks...");

                    successTask.cancel(true);
                    slowTask.cancel(true);
                }

                System.out.println("    Success task cancelled: " + successTask.isCancelled());
                System.out.println("    Slow task cancelled: " + slowTask.isCancelled());

            } catch (Exception e) {
                System.out.println("    Error: " + e.getMessage());
            }
        }
    }

    /**
     * 3. ShutdownOnSuccess - 첫 성공 시 나머지 취소
     */
    static class ShutdownOnSuccessDemo {
        public void demonstrate() {
            System.out.println("  ShutdownOnSuccess pattern (race scenario)");

            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {

                List<Future<String>> futures = new ArrayList<>();

                // 여러 작업을 동시에 시작 (race)
                futures.add(executor.submit(() -> {
                    System.out.println("    [Task-1] Starting (500ms)");
                    Thread.sleep(500);
                    return "Result from Task-1";
                }));

                futures.add(executor.submit(() -> {
                    System.out.println("    [Task-2] Starting (200ms)");
                    Thread.sleep(200);
                    return "Result from Task-2";
                }));

                futures.add(executor.submit(() -> {
                    System.out.println("    [Task-3] Starting (300ms)");
                    Thread.sleep(300);
                    return "Result from Task-3";
                }));

                // 첫 번째로 완료되는 것을 기다림
                String firstResult = null;
                for (Future<String> future : futures) {
                    try {
                        if (!future.isDone()) continue;
                        firstResult = future.get(100, TimeUnit.MILLISECONDS);
                        if (firstResult != null) {
                            System.out.println("    First result: " + firstResult);

                            // 나머지 취소
                            System.out.println("    Cancelling remaining tasks...");
                            for (Future<String> f : futures) {
                                if (!f.isDone()) {
                                    f.cancel(true);
                                }
                            }
                            break;
                        }
                    } catch (TimeoutException e) {
                        continue;
                    }
                }

                Thread.sleep(300);

                long cancelled = futures.stream().filter(Future::isCancelled).count();
                System.out.println("    Cancelled tasks: " + cancelled);

            } catch (Exception e) {
                System.out.println("    Error: " + e.getMessage());
            }
        }
    }

    /**
     * 4. 타임아웃 처리
     */
    static class TimeoutHandling {
        public void demonstrate() {
            System.out.println("  Timeout handling with structured concurrency");

            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {

                Future<String> longTask = executor.submit(() -> {
                    System.out.println("    [Long-Task] Starting (will take 2s)");
                    Thread.sleep(2000);
                    return "Completed";
                });

                try {
                    String result = longTask.get(500, TimeUnit.MILLISECONDS);
                    System.out.println("    Result: " + result);
                } catch (TimeoutException e) {
                    System.out.println("    Timeout! Cancelling task...");
                    longTask.cancel(true);
                    System.out.println("    Task cancelled: " + longTask.isCancelled());
                }

            } catch (Exception e) {
                System.out.println("    Error: " + e.getMessage());
            }
        }
    }

    /**
     * 5. 계층적 작업 구조
     */
    static class HierarchicalTasks {
        public void demonstrate() throws Exception {
            System.out.println("  Hierarchical task structure");

            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {

                Future<String> parentTask = executor.submit(() -> {
                    System.out.println("    [Parent] Starting");

                    // 부모 작업 내에서 자식 작업 생성
                    try (ExecutorService childExecutor = Executors.newVirtualThreadPerTaskExecutor()) {

                        Future<String> child1 = childExecutor.submit(() -> {
                            System.out.println("      [Child-1] Working");
                            Thread.sleep(100);
                            return "Child-1 result";
                        });

                        Future<String> child2 = childExecutor.submit(() -> {
                            System.out.println("      [Child-2] Working");
                            Thread.sleep(150);
                            return "Child-2 result";
                        });

                        String result1 = child1.get();
                        String result2 = child2.get();

                        System.out.println("    [Parent] Children completed");
                        return "Parent: " + result1 + ", " + result2;
                    }
                });

                String result = parentTask.get();
                System.out.println("    Final result: " + result);
            }
        }
    }

    /**
     * 6. 예외 전파
     */
    static class ExceptionPropagation {
        public void demonstrate() {
            System.out.println("  Exception propagation in structured concurrency");

            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {

                List<Future<String>> futures = new ArrayList<>();

                futures.add(executor.submit(() -> {
                    System.out.println("    [Task-1] Running");
                    Thread.sleep(100);
                    return "Success-1";
                }));

                futures.add(executor.submit(() -> {
                    System.out.println("    [Task-2] Running");
                    Thread.sleep(50);
                    throw new IllegalStateException("Task-2 failed");
                }));

                futures.add(executor.submit(() -> {
                    System.out.println("    [Task-3] Running");
                    Thread.sleep(200);
                    return "Success-3";
                }));

                // 모든 작업 결과 수집 (예외 처리)
                for (int i = 0; i < futures.size(); i++) {
                    try {
                        String result = futures.get(i).get();
                        System.out.println("    Task-" + (i + 1) + " result: " + result);
                    } catch (ExecutionException e) {
                        System.out.println("    Task-" + (i + 1) + " failed: " +
                                e.getCause().getMessage());

                        // 다른 작업들 취소
                        System.out.println("    Cancelling remaining tasks...");
                        for (Future<String> f : futures) {
                            if (!f.isDone()) {
                                f.cancel(true);
                            }
                        }
                        break;
                    }
                }

            } catch (Exception e) {
                System.out.println("    Error: " + e.getMessage());
            }
        }
    }

    /**
     * 7. 실용 예제: 병렬 API 호출
     */
    static class ParallelAPICallsExample {
        public void demonstrate() throws Exception {
            System.out.println("  Parallel API calls example");

            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {

                Future<String> userService = executor.submit(() -> {
                    System.out.println("    [User-Service] Calling...");
                    Thread.sleep(100);
                    return "User: John Doe";
                });

                Future<String> orderService = executor.submit(() -> {
                    System.out.println("    [Order-Service] Calling...");
                    Thread.sleep(150);
                    return "Orders: 5 items";
                });

                Future<String> inventoryService = executor.submit(() -> {
                    System.out.println("    [Inventory-Service] Calling...");
                    Thread.sleep(80);
                    return "Stock: Available";
                });

                // 모든 API 응답 대기
                String user = userService.get();
                String orders = orderService.get();
                String inventory = inventoryService.get();

                System.out.println("    Combined result:");
                System.out.println("      " + user);
                System.out.println("      " + orders);
                System.out.println("      " + inventory);
            }
        }
    }

    /**
     * 8. 구조화된 동시성의 장점
     */
    static class Benefits {
        public void demonstrate() {
            System.out.println("\n  === Benefits of Structured Concurrency ===");

            System.out.println("\n  ✓ Advantages:");
            System.out.println("    - Clear task lifetime management");
            System.out.println("    - Automatic resource cleanup");
            System.out.println("    - Predictable error handling");
            System.out.println("    - Prevents thread leaks");
            System.out.println("    - Better observability");
            System.out.println("    - Easier to reason about");

            System.out.println("\n  Traditional Concurrency Problems:");
            System.out.println("    - Orphaned threads");
            System.out.println("    - Unclear ownership");
            System.out.println("    - Error handling complexity");
            System.out.println("    - Resource leaks");
            System.out.println("    - Race conditions");

            System.out.println("\n  Structured Concurrency Solutions:");
            System.out.println("    - Parent-child relationships");
            System.out.println("    - Scoped lifetime");
            System.out.println("    - Automatic cancellation");
            System.out.println("    - Clear error boundaries");
            System.out.println("    - try-with-resources pattern");

            System.out.println("\n  Best Practices:");
            System.out.println("    - Use try-with-resources");
            System.out.println("    - Keep scopes small and focused");
            System.out.println("    - Handle errors at scope boundaries");
            System.out.println("    - Use virtual threads for scalability");
            System.out.println("    - Avoid nested scopes when possible");
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== StructuredTaskScope Demo (Java 21+) ===\n");

        // 1. 기본 사용법
        System.out.println("1. Basic Usage");
        new BasicUsage().demonstrate();
        System.out.println();

        // 2. ShutdownOnFailure
        System.out.println("2. ShutdownOnFailure Pattern");
        new ShutdownOnFailureDemo().demonstrate();
        System.out.println();

        // 3. ShutdownOnSuccess
        System.out.println("3. ShutdownOnSuccess Pattern");
        new ShutdownOnSuccessDemo().demonstrate();
        System.out.println();

        // 4. 타임아웃 처리
        System.out.println("4. Timeout Handling");
        new TimeoutHandling().demonstrate();
        System.out.println();

        // 5. 계층적 작업
        System.out.println("5. Hierarchical Tasks");
        new HierarchicalTasks().demonstrate();
        System.out.println();

        // 6. 예외 전파
        System.out.println("6. Exception Propagation");
        new ExceptionPropagation().demonstrate();
        System.out.println();

        // 7. 병렬 API 호출
        System.out.println("7. Parallel API Calls");
        new ParallelAPICallsExample().demonstrate();
        System.out.println();

        // 8. 장점 설명
        System.out.println("8. Benefits");
        new Benefits().demonstrate();
        System.out.println();

        System.out.println("=== Demo Completed ===");
    }
}