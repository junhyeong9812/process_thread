package com.study.thread.pool;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Virtual Thread Executor 실습 (Java 21+)
 *
 * Executors.newVirtualThreadPerTaskExecutor()를 사용한 가상 스레드 실행자
 * - 작업마다 새로운 가상 스레드 생성
 * - 수백만 개의 스레드 생성 가능
 * - 블로킹 I/O에 최적화
 * - 플랫폼 스레드보다 훨씬 경량
 */
public class VirtualThreadExecutor {

    /**
     * 1. 기본 VirtualThreadExecutor 사용법
     */
    static class BasicUsage {
        public void demonstrate() throws InterruptedException {
            System.out.println("  Creating VirtualThreadPerTaskExecutor");

            try {
                ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

                // 작업 제출
                for (int i = 1; i <= 5; i++) {
                    final int taskId = i;
                    executor.submit(() -> {
                        System.out.println("    [Task-" + taskId + "] Running on " +
                                Thread.currentThread());
                        System.out.println("    [Task-" + taskId + "] Is virtual: " +
                                Thread.currentThread().isVirtual());
                        sleep(100);
                        System.out.println("    [Task-" + taskId + "] Completed");
                    });
                }

                executor.shutdown();
                executor.awaitTermination(5, TimeUnit.SECONDS);

            } catch (Exception e) {
                System.out.println("  Virtual threads not available (requires Java 21+)");
                System.out.println("  Error: " + e.getMessage());
            }
        }
    }

    /**
     * 2. 대량 가상 스레드 생성
     */
    static class MassiveVirtualThreads {
        public void demonstrate() throws InterruptedException, ExecutionException {
            System.out.println("  Creating massive number of virtual threads");

            try {
                ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

                int taskCount = 10000; // 1만개!
                AtomicInteger completedCount = new AtomicInteger(0);

                System.out.println("  Submitting " + taskCount + " tasks...");

                long start = System.currentTimeMillis();
                List<Future<?>> futures = new ArrayList<>();

                for (int i = 0; i < taskCount; i++) {
                    futures.add(executor.submit(() -> {
                        sleep(100); // I/O 시뮬레이션
                        completedCount.incrementAndGet();
                    }));
                }

                // 모든 작업 완료 대기
                for (Future<?> future : futures) {
                    future.get();
                }

                long duration = System.currentTimeMillis() - start;

                System.out.println("  Completed: " + completedCount.get() + " / " + taskCount);
                System.out.println("  Total time: " + duration + " ms");
                System.out.println("  Avg per task: " + (duration * 1000.0 / taskCount) + " μs");

                executor.shutdown();

            } catch (Exception e) {
                System.out.println("  Virtual threads not available (requires Java 21+)");
            }
        }
    }

    /**
     * 3. Virtual vs Platform Thread 성능 비교
     */
    static class PerformanceComparison {
        public void demonstrate() throws Exception {
            int taskCount = 1000;

            System.out.println("  Comparing Virtual vs Platform threads (" +
                    taskCount + " tasks)");

            // Platform Thread Pool 테스트
            System.out.println("\n  Testing Platform Thread Pool:");
            ExecutorService platformPool = Executors.newFixedThreadPool(
                    Runtime.getRuntime().availableProcessors());
            long platformTime = runTasks(platformPool, taskCount, false);
            platformPool.shutdown();

            Thread.sleep(200);

            // Virtual Thread Executor 테스트
            try {
                System.out.println("\n  Testing Virtual Thread Executor:");
                ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();
                long virtualTime = runTasks(virtualExecutor, taskCount, true);
                virtualExecutor.shutdown();

                // 비교
                System.out.println("\n  Comparison:");
                System.out.println("    Platform threads: " + platformTime + " ms");
                System.out.println("    Virtual threads: " + virtualTime + " ms");
                System.out.println("    Speedup: " +
                        String.format("%.2fx", (double) platformTime / virtualTime));

            } catch (Exception e) {
                System.out.println("  Virtual threads not available (requires Java 21+)");
            }
        }

        private long runTasks(ExecutorService executor, int taskCount, boolean isVirtual)
                throws InterruptedException, ExecutionException {
            long start = System.currentTimeMillis();

            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < taskCount; i++) {
                futures.add(executor.submit(() -> {
                    sleep(10); // I/O 작업 시뮬레이션
                }));
            }

            for (Future<?> future : futures) {
                future.get();
            }

            long duration = System.currentTimeMillis() - start;

            if (isVirtual) {
                System.out.println("    Each task created a new virtual thread");
            } else {
                System.out.println("    Tasks shared " +
                        Runtime.getRuntime().availableProcessors() + " platform threads");
            }

            return duration;
        }
    }

    /**
     * 4. 블로킹 I/O 시뮬레이션
     */
    static class BlockingIOSimulation {
        public void demonstrate() throws InterruptedException {
            System.out.println("  Simulating blocking I/O operations");

            try {
                ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

                int requestCount = 100;
                AtomicInteger successCount = new AtomicInteger(0);

                System.out.println("  Processing " + requestCount + " I/O requests...");

                long start = System.currentTimeMillis();
                CountDownLatch latch = new CountDownLatch(requestCount);

                for (int i = 1; i <= requestCount; i++) {
                    final int requestId = i;
                    executor.submit(() -> {
                        try {
                            // 네트워크 요청 시뮬레이션
                            sleep(50 + (int) (Math.random() * 100));
                            successCount.incrementAndGet();

                            if (requestId % 20 == 0) {
                                System.out.println("    [Request-" + requestId + "] Completed");
                            }
                        } finally {
                            latch.countDown();
                        }
                    });
                }

                latch.await();
                long duration = System.currentTimeMillis() - start;

                System.out.println("  All requests completed");
                System.out.println("  Success: " + successCount.get() + " / " + requestCount);
                System.out.println("  Total time: " + duration + " ms");
                System.out.println("  Virtual threads efficiently handled blocking I/O");

                executor.shutdown();

            } catch (Exception e) {
                System.out.println("  Virtual threads not available (requires Java 21+)");
            }
        }
    }

    /**
     * 5. 커스텀 ThreadFactory 사용
     */
    static class CustomThreadFactory {
        public void demonstrate() throws InterruptedException {
            System.out.println("  Using custom virtual thread factory");

            try {
                // 커스텀 ThreadFactory 생성
                ThreadFactory factory = Thread.ofVirtual()
                        .name("CustomVirtual-", 0)
                        .factory();

                ExecutorService executor = Executors.newThreadPerTaskExecutor(factory);

                for (int i = 1; i <= 5; i++) {
                    final int taskId = i;
                    executor.submit(() -> {
                        Thread current = Thread.currentThread();
                        System.out.println("    [Task-" + taskId + "] " +
                                current.getName() +
                                " (virtual: " + current.isVirtual() + ")");
                        sleep(100);
                    });
                }

                executor.shutdown();
                executor.awaitTermination(5, TimeUnit.SECONDS);

            } catch (Exception e) {
                System.out.println("  Virtual threads not available (requires Java 21+)");
            }
        }
    }

    /**
     * 6. 구조화된 동시성 (Structured Concurrency)
     */
    static class StructuredConcurrencyExample {
        public void demonstrate() {
            System.out.println("  Structured Concurrency with Virtual Threads");

            try {
                ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

                System.out.println("  Starting concurrent operations...");

                Future<String> future1 = executor.submit(() -> {
                    System.out.println("    [Operation-1] Starting");
                    sleep(200);
                    System.out.println("    [Operation-1] Completed");
                    return "Result-1";
                });

                Future<String> future2 = executor.submit(() -> {
                    System.out.println("    [Operation-2] Starting");
                    sleep(150);
                    System.out.println("    [Operation-2] Completed");
                    return "Result-2";
                });

                Future<String> future3 = executor.submit(() -> {
                    System.out.println("    [Operation-3] Starting");
                    sleep(100);
                    System.out.println("    [Operation-3] Completed");
                    return "Result-3";
                });

                // 모든 결과 수집
                String result1 = future1.get();
                String result2 = future2.get();
                String result3 = future3.get();

                System.out.println("  All results: " + result1 + ", " +
                        result2 + ", " + result3);

                executor.shutdown();

            } catch (Exception e) {
                System.out.println("  Virtual threads not available (requires Java 21+)");
            }
        }
    }

    /**
     * 7. 리소스 사용량 비교
     */
    static class ResourceUsageComparison {
        public void demonstrate() throws Exception {
            System.out.println("  Comparing resource usage");

            Runtime runtime = Runtime.getRuntime();

            // Platform threads
            System.out.println("\n  Creating 1000 platform threads:");
            System.gc();
            long beforePlatform = runtime.totalMemory() - runtime.freeMemory();

            ExecutorService platformPool = Executors.newCachedThreadPool();
            CountDownLatch latch1 = new CountDownLatch(1000);

            for (int i = 0; i < 1000; i++) {
                platformPool.submit(() -> {
                    sleep(1000);
                    latch1.countDown();
                });
            }

            Thread.sleep(100);
            long afterPlatform = runtime.totalMemory() - runtime.freeMemory();
            long platformMemory = afterPlatform - beforePlatform;

            System.out.println("    Memory used: ~" + (platformMemory / 1024 / 1024) + " MB");

            latch1.await();
            platformPool.shutdown();

            Thread.sleep(500);
            System.gc();

            // Virtual threads
            try {
                System.out.println("\n  Creating 1000 virtual threads:");
                long beforeVirtual = runtime.totalMemory() - runtime.freeMemory();

                ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();
                CountDownLatch latch2 = new CountDownLatch(1000);

                for (int i = 0; i < 1000; i++) {
                    virtualExecutor.submit(() -> {
                        sleep(1000);
                        latch2.countDown();
                    });
                }

                Thread.sleep(100);
                long afterVirtual = runtime.totalMemory() - runtime.freeMemory();
                long virtualMemory = afterVirtual - beforeVirtual;

                System.out.println("    Memory used: ~" + (virtualMemory / 1024 / 1024) + " MB");

                latch2.await();
                virtualExecutor.shutdown();

                System.out.println("\n  Virtual threads use significantly less memory!");

            } catch (Exception e) {
                System.out.println("  Virtual threads not available (requires Java 21+)");
            }
        }
    }

    /**
     * 8. 대규모 동시 연결 시뮬레이션
     */
    static class ConcurrentConnectionsSimulation {
        public void demonstrate() throws InterruptedException {
            System.out.println("  Simulating concurrent connections (e.g., web server)");

            try {
                ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

                int connectionCount = 5000;
                AtomicInteger activeConnections = new AtomicInteger(0);
                AtomicInteger peakConnections = new AtomicInteger(0);

                System.out.println("  Handling " + connectionCount + " connections...");

                long start = System.currentTimeMillis();
                CountDownLatch latch = new CountDownLatch(connectionCount);

                for (int i = 1; i <= connectionCount; i++) {
                    executor.submit(() -> {
                        int active = activeConnections.incrementAndGet();

                        // 피크 연결 수 업데이트
                        peakConnections.updateAndGet(current -> Math.max(current, active));

                        try {
                            // 연결 처리 시뮬레이션
                            sleep(10 + (int) (Math.random() * 50));
                        } finally {
                            activeConnections.decrementAndGet();
                            latch.countDown();
                        }
                    });

                    // 연결이 계속 들어오는 상황
                    if (i % 100 == 0) {
                        Thread.sleep(5);
                    }
                }

                latch.await();
                long duration = System.currentTimeMillis() - start;

                System.out.println("  All connections handled");
                System.out.println("  Peak concurrent connections: " + peakConnections.get());
                System.out.println("  Total time: " + duration + " ms");
                System.out.println("  Throughput: " + (connectionCount * 1000 / duration) + " req/s");

                executor.shutdown();

            } catch (Exception e) {
                System.out.println("  Virtual threads not available (requires Java 21+)");
            }
        }
    }

    /**
     * 9. 사용 시나리오
     */
    static class UsageScenarios {
        public void demonstrate() {
            System.out.println("  Virtual Thread Executor 사용 시나리오:");
            System.out.println();

            System.out.println("  ✓ 이상적인 경우:");
            System.out.println("    - 블로킹 I/O 작업 (네트워크, 파일, DB)");
            System.out.println("    - 수천~수백만 개의 동시 작업");
            System.out.println("    - 짧고 많은 비동기 작업");
            System.out.println("    - 웹 서버의 요청 처리");
            System.out.println("    - 마이크로서비스 간 통신");
            System.out.println();

            System.out.println("  ✗ 부적합한 경우:");
            System.out.println("    - CPU 집약적 작업");
            System.out.println("    - 작업이 매우 긴 경우");
            System.out.println("    - synchronized 블록이 많은 경우");
            System.out.println("    - 네이티브 코드 호출");
            System.out.println();

            System.out.println("  특징:");
            System.out.println("    - 작업마다 새 가상 스레드 생성");
            System.out.println("    - 플랫폼 스레드보다 1000배 경량");
            System.out.println("    - 블로킹 시 플랫폼 스레드 해제");
            System.out.println("    - 스택 크기가 동적으로 조절");
            System.out.println();

            System.out.println("  vs Platform Thread Pool:");
            System.out.println("    - Virtual: 수백만 스레드, I/O 바운드");
            System.out.println("    - Platform: 제한된 스레드, CPU 바운드");
        }
    }

    /**
     * 10. 모범 사례
     */
    static class BestPractices {
        public void demonstrate() {
            System.out.println("  Virtual Thread 모범 사례:");
            System.out.println();

            System.out.println("  ✓ 권장사항:");
            System.out.println("    - 블로킹 작업에 사용");
            System.out.println("    - try-with-resources로 자동 종료");
            System.out.println("    - 예외 처리 철저히");
            System.out.println("    - 스레드 풀링 하지 말 것");
            System.out.println();

            System.out.println("  ✗ 피해야 할 것:");
            System.out.println("    - 가상 스레드 풀링 (불필요)");
            System.out.println("    - 과도한 synchronized 사용");
            System.out.println("    - ThreadLocal 과다 사용");
            System.out.println("    - 긴 CPU 연산");
            System.out.println();

            System.out.println("  예제:");
            System.out.println("    // 권장");
            System.out.println("    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {");
            System.out.println("        executor.submit(() -> blockingIO());");
            System.out.println("    }");
            System.out.println();

            System.out.println("    // 비권장");
            System.out.println("    synchronized (lock) {");
            System.out.println("        longRunningTask(); // 가상 스레드 고정(pinning)");
            System.out.println("    }");
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== Virtual Thread Executor Demo ===\n");

        // 1. 기본 사용법
        System.out.println("1. Basic Usage");
        new BasicUsage().demonstrate();
        System.out.println();

        // 2. 대량 가상 스레드
        System.out.println("2. Massive Virtual Threads");
        new MassiveVirtualThreads().demonstrate();
        System.out.println();

        // 3. 성능 비교
        System.out.println("3. Performance Comparison");
        new PerformanceComparison().demonstrate();
        System.out.println();

        // 4. 블로킹 I/O
        System.out.println("4. Blocking I/O Simulation");
        new BlockingIOSimulation().demonstrate();
        System.out.println();

        // 5. 커스텀 ThreadFactory
        System.out.println("5. Custom Thread Factory");
        new CustomThreadFactory().demonstrate();
        System.out.println();

        // 6. 구조화된 동시성
        System.out.println("6. Structured Concurrency");
        new StructuredConcurrencyExample().demonstrate();
        System.out.println();

        // 7. 리소스 사용량 비교
        System.out.println("7. Resource Usage Comparison");
        new ResourceUsageComparison().demonstrate();
        System.out.println();

        // 8. 동시 연결 시뮬레이션
        System.out.println("8. Concurrent Connections Simulation");
        new ConcurrentConnectionsSimulation().demonstrate();
        System.out.println();

        // 9. 사용 시나리오
        System.out.println("9. Usage Scenarios");
        new UsageScenarios().demonstrate();
        System.out.println();

        // 10. 모범 사례
        System.out.println("10. Best Practices");
        new BestPractices().demonstrate();
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