package com.study.thread.pool;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * CachedThreadPool 실습
 *
 * Executors.newCachedThreadPool()을 사용한 캐시형 스레드 풀 학습
 * - 필요에 따라 스레드 생성
 * - 유휴 스레드는 60초 후 종료
 * - 짧고 많은 비동기 작업에 적합
 * - I/O 바운드 작업에 유리
 */
public class CachedThreadPoolDemo {

    /**
     * 1. 기본 CachedThreadPool 사용법
     */
    static class BasicUsage {
        public void demonstrate() throws InterruptedException {
            System.out.println("  Creating CachedThreadPool");
            ExecutorService executor = Executors.newCachedThreadPool();

            ThreadPoolExecutor tpe = (ThreadPoolExecutor) executor;

            // 초기 상태
            System.out.println("  Initial pool size: " + tpe.getPoolSize());

            // 여러 작업 제출
            for (int i = 1; i <= 5; i++) {
                final int taskId = i;
                executor.submit(() -> {
                    System.out.println("    [Task-" + taskId + "] Running on " +
                            Thread.currentThread().getName());
                    sleep(200);
                    System.out.println("    [Task-" + taskId + "] Completed");
                });
                Thread.sleep(50); // 순차적 제출
            }

            Thread.sleep(100);
            System.out.println("  Pool size after tasks: " + tpe.getPoolSize());
            System.out.println("  Active threads: " + tpe.getActiveCount());

            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    /**
     * 2. 동적 스레드 생성 관찰
     */
    static class DynamicThreadCreation {
        public void demonstrate() throws InterruptedException {
            ExecutorService executor = Executors.newCachedThreadPool();
            ThreadPoolExecutor tpe = (ThreadPoolExecutor) executor;

            System.out.println("  Submitting burst of tasks...");

            // 동시에 많은 작업 제출
            for (int i = 1; i <= 10; i++) {
                final int taskId = i;
                executor.submit(() -> {
                    System.out.println("    [Task-" + taskId + "] Started on " +
                            Thread.currentThread().getName());
                    sleep(500);
                });
            }

            // 풀 크기 모니터링
            for (int i = 0; i < 5; i++) {
                Thread.sleep(100);
                System.out.println("  Pool size: " + tpe.getPoolSize() +
                        ", Active: " + tpe.getActiveCount() +
                        ", Queue: " + tpe.getQueue().size());
            }

            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    /**
     * 3. 스레드 재사용 관찰
     */
    static class ThreadReuse {
        public void demonstrate() throws InterruptedException {
            ExecutorService executor = Executors.newCachedThreadPool();

            System.out.println("  First wave of tasks:");
            for (int i = 1; i <= 3; i++) {
                final int taskId = i;
                executor.submit(() -> {
                    System.out.println("    [Task-" + taskId + "] " +
                            Thread.currentThread().getName());
                });
            }

            Thread.sleep(500); // 첫 번째 파동 완료 대기

            System.out.println("\n  Second wave of tasks (reusing threads):");
            for (int i = 4; i <= 6; i++) {
                final int taskId = i;
                executor.submit(() -> {
                    System.out.println("    [Task-" + taskId + "] " +
                            Thread.currentThread().getName());
                });
            }

            Thread.sleep(500);
            executor.shutdown();
        }
    }

    /**
     * 4. 유휴 스레드 타임아웃
     */
    static class IdleThreadTimeout {
        public void demonstrate() throws InterruptedException {
            ThreadPoolExecutor executor = (ThreadPoolExecutor)
                    Executors.newCachedThreadPool();

            System.out.println("  Submitting initial tasks...");
            for (int i = 1; i <= 5; i++) {
                final int taskId = i;
                executor.submit(() -> {
                    System.out.println("    [Task-" + taskId + "] Running");
                    sleep(100);
                });
            }

            Thread.sleep(500);
            int initialSize = executor.getPoolSize();
            System.out.println("  Pool size after tasks: " + initialSize);

            System.out.println("  Waiting for idle timeout (60 seconds default)...");
            System.out.println("  (Simulating with custom keep-alive time)");

            // 커스텀 keep-alive 시간 설정
            executor.setKeepAliveTime(2, TimeUnit.SECONDS);

            for (int i = 0; i <= 3; i++) {
                Thread.sleep(1000);
                System.out.println("    After " + i + "s - Pool size: " +
                        executor.getPoolSize());
            }

            executor.shutdown();
        }
    }

    /**
     * 5. 짧은 작업 대량 처리
     */
    static class ShortTaskProcessing {
        public void demonstrate() throws InterruptedException, ExecutionException {
            ExecutorService executor = Executors.newCachedThreadPool();

            int taskCount = 100;
            System.out.println("  Processing " + taskCount + " short tasks...");

            long start = System.currentTimeMillis();
            List<Future<Integer>> futures = new ArrayList<>();

            for (int i = 1; i <= taskCount; i++) {
                final int taskId = i;
                futures.add(executor.submit(() -> {
                    // 짧은 작업
                    sleep(10);
                    return taskId;
                }));
            }

            // 모든 결과 수집
            int sum = 0;
            for (Future<Integer> future : futures) {
                sum += future.get();
            }

            long duration = System.currentTimeMillis() - start;

            System.out.println("  Completed " + taskCount + " tasks in " + duration + " ms");
            System.out.println("  Sum: " + sum);

            ThreadPoolExecutor tpe = (ThreadPoolExecutor) executor;
            System.out.println("  Peak pool size: " + tpe.getLargestPoolSize());

            executor.shutdown();
        }
    }

    /**
     * 6. CachedThreadPool vs FixedThreadPool 비교
     */
    static class PoolComparison {
        public void demonstrate() throws InterruptedException, ExecutionException {
            int taskCount = 50;

            // CachedThreadPool 테스트
            System.out.println("  Testing CachedThreadPool:");
            ExecutorService cached = Executors.newCachedThreadPool();
            long cachedTime = testPool(cached, taskCount);
            ThreadPoolExecutor cachedTpe = (ThreadPoolExecutor) cached;
            int cachedPeak = cachedTpe.getLargestPoolSize();
            cached.shutdown();

            Thread.sleep(100);

            // FixedThreadPool 테스트
            System.out.println("\n  Testing FixedThreadPool:");
            ExecutorService fixed = Executors.newFixedThreadPool(4);
            long fixedTime = testPool(fixed, taskCount);
            ThreadPoolExecutor fixedTpe = (ThreadPoolExecutor) fixed;
            int fixedPeak = fixedTpe.getLargestPoolSize();
            fixed.shutdown();

            // 비교
            System.out.println("\n  Comparison:");
            System.out.println("    CachedThreadPool - Time: " + cachedTime +
                    " ms, Peak threads: " + cachedPeak);
            System.out.println("    FixedThreadPool  - Time: " + fixedTime +
                    " ms, Peak threads: " + fixedPeak);
        }

        private long testPool(ExecutorService executor, int taskCount)
                throws InterruptedException, ExecutionException {
            long start = System.currentTimeMillis();

            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < taskCount; i++) {
                futures.add(executor.submit(() -> {
                    sleep(100); // I/O 시뮬레이션
                }));
            }

            for (Future<?> future : futures) {
                future.get();
            }

            return System.currentTimeMillis() - start;
        }
    }

    /**
     * 7. 네트워크 요청 시뮬레이션
     */
    static class NetworkRequestSimulation {
        private final AtomicInteger requestCount = new AtomicInteger(0);

        public void demonstrate() throws InterruptedException {
            ExecutorService executor = Executors.newCachedThreadPool();
            ThreadPoolExecutor tpe = (ThreadPoolExecutor) executor;

            System.out.println("  Simulating concurrent network requests...");

            // 다양한 응답 시간을 가진 요청들
            for (int i = 1; i <= 20; i++) {
                final int requestId = i;
                executor.submit(() -> {
                    int duration = 100 + (int) (Math.random() * 200);
                    System.out.println("    [Request-" + requestId + "] " +
                            "Starting (" + duration + "ms)");
                    sleep(duration);
                    requestCount.incrementAndGet();
                    System.out.println("    [Request-" + requestId + "] Completed");
                });

                Thread.sleep(50); // 요청이 계속 들어오는 상황
            }

            Thread.sleep(500);
            System.out.println("  Peak threads: " + tpe.getLargestPoolSize());
            System.out.println("  Completed requests: " + requestCount.get());

            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    /**
     * 8. 작업 제출 전략
     */
    static class SubmissionStrategies {
        public void demonstrate() throws InterruptedException {
            ExecutorService executor = Executors.newCachedThreadPool();

            // 전략 1: 즉시 제출
            System.out.println("  Strategy 1: Immediate submission");
            long start1 = System.currentTimeMillis();
            for (int i = 0; i < 10; i++) {
                executor.submit(() -> sleep(100));
            }
            Thread.sleep(200);
            long time1 = System.currentTimeMillis() - start1;
            System.out.println("    Completed in: " + time1 + " ms");

            Thread.sleep(500);

            // 전략 2: 분산 제출
            System.out.println("\n  Strategy 2: Distributed submission");
            long start2 = System.currentTimeMillis();
            for (int i = 0; i < 10; i++) {
                executor.submit(() -> sleep(100));
                Thread.sleep(20); // 약간의 간격
            }
            Thread.sleep(200);
            long time2 = System.currentTimeMillis() - start2;
            System.out.println("    Completed in: " + time2 + " ms");

            executor.shutdown();
        }
    }

    /**
     * 9. 리소스 사용량 모니터링
     */
    static class ResourceMonitoring {
        public void demonstrate() throws InterruptedException {
            ThreadPoolExecutor executor = (ThreadPoolExecutor)
                    Executors.newCachedThreadPool();

            // 모니터링 스레드
            Thread monitor = new Thread(() -> {
                try {
                    while (!executor.isTerminated()) {
                        System.out.println("    Pool: " + executor.getPoolSize() +
                                ", Active: " + executor.getActiveCount() +
                                ", Completed: " + executor.getCompletedTaskCount() +
                                ", Queue: " + executor.getQueue().size());
                        Thread.sleep(200);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            monitor.setDaemon(true);
            monitor.start();

            System.out.println("  Submitting tasks with monitoring...");

            for (int i = 1; i <= 15; i++) {
                final int taskId = i;
                executor.submit(() -> {
                    sleep(300);
                });
                Thread.sleep(100);
            }

            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    /**
     * 10. 사용 시나리오 및 주의사항
     */
    static class UsageGuidelines {
        public void demonstrate() {
            System.out.println("  CachedThreadPool 사용 가이드라인:");
            System.out.println();

            System.out.println("  ✓ 적합한 경우:");
            System.out.println("    - 짧고 빠른 비동기 작업");
            System.out.println("    - I/O 바운드 작업 (네트워크, 파일)");
            System.out.println("    - 작업 수를 예측하기 어려운 경우");
            System.out.println("    - 작업 간 실행 시간 간격이 있는 경우");
            System.out.println();

            System.out.println("  ✗ 부적합한 경우:");
            System.out.println("    - CPU 집약적 작업");
            System.out.println("    - 무제한으로 작업이 쏟아지는 경우");
            System.out.println("    - 오래 실행되는 작업");
            System.out.println("    - 스레드 수를 엄격히 제한해야 하는 경우");
            System.out.println();

            System.out.println("  주의사항:");
            System.out.println("    - 스레드가 무한정 생성될 수 있음 (OOM 위험)");
            System.out.println("    - 스레드 생성 오버헤드 고려 필요");
            System.out.println("    - 유휴 스레드는 60초 후 자동 종료");
            System.out.println();

            System.out.println("  대안:");
            System.out.println("    - CPU 작업: FixedThreadPool");
            System.out.println("    - 제한된 동시성: CustomThreadPoolExecutor");
            System.out.println("    - 경량 작업: VirtualThreadExecutor (Java 21+)");
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== CachedThreadPool Demo ===\n");

        // 1. 기본 사용법
        System.out.println("1. Basic Usage");
        new BasicUsage().demonstrate();
        System.out.println();

        // 2. 동적 스레드 생성
        System.out.println("2. Dynamic Thread Creation");
        new DynamicThreadCreation().demonstrate();
        System.out.println();

        // 3. 스레드 재사용
        System.out.println("3. Thread Reuse");
        new ThreadReuse().demonstrate();
        System.out.println();

        // 4. 유휴 스레드 타임아웃
        System.out.println("4. Idle Thread Timeout");
        new IdleThreadTimeout().demonstrate();
        System.out.println();

        // 5. 짧은 작업 대량 처리
        System.out.println("5. Short Task Processing");
        new ShortTaskProcessing().demonstrate();
        System.out.println();

        // 6. 풀 비교
        System.out.println("6. Pool Comparison");
        new PoolComparison().demonstrate();
        System.out.println();

        // 7. 네트워크 요청 시뮬레이션
        System.out.println("7. Network Request Simulation");
        new NetworkRequestSimulation().demonstrate();
        System.out.println();

        // 8. 작업 제출 전략
        System.out.println("8. Submission Strategies");
        new SubmissionStrategies().demonstrate();
        System.out.println();

        // 9. 리소스 모니터링
        System.out.println("9. Resource Monitoring");
        new ResourceMonitoring().demonstrate();
        System.out.println();

        // 10. 사용 가이드라인
        System.out.println("10. Usage Guidelines");
        new UsageGuidelines().demonstrate();
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