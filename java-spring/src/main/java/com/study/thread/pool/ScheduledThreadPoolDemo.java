package com.study.thread.pool;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ScheduledThreadPool 실습
 *
 * Executors.newScheduledThreadPool()을 사용한 스케줄링 스레드 풀 학습
 * - 지연 실행 (delay)
 * - 주기적 실행 (periodic)
 * - scheduleAtFixedRate vs scheduleWithFixedDelay
 * - 크론(Cron) 스타일 스케줄링
 */
public class ScheduledThreadPoolDemo {

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    /**
     * 1. 기본 지연 실행 (schedule)
     */
    static class DelayedExecution {
        public void demonstrate() throws InterruptedException {
            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

            System.out.println("  Current time: " + currentTime());

            // 1초 후 실행
            scheduler.schedule(() -> {
                System.out.println("    [Task-1] Executed at: " + currentTime() +
                        " (1s delay)");
            }, 1, TimeUnit.SECONDS);

            // 2초 후 실행
            scheduler.schedule(() -> {
                System.out.println("    [Task-2] Executed at: " + currentTime() +
                        " (2s delay)");
            }, 2, TimeUnit.SECONDS);

            // 3초 후 실행 (결과 반환)
            ScheduledFuture<String> future = scheduler.schedule(() -> {
                System.out.println("    [Task-3] Executed at: " + currentTime() +
                        " (3s delay)");
                return "Task-3 Result";
            }, 3, TimeUnit.SECONDS);

            Thread.sleep(3500);

            try {
                System.out.println("  Future result: " + future.get());
            } catch (ExecutionException e) {
                e.printStackTrace();
            }

            scheduler.shutdown();
        }
    }

    /**
     * 2. 고정 주기 실행 (scheduleAtFixedRate)
     */
    static class FixedRateExecution {
        public void demonstrate() throws InterruptedException {
            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

            AtomicInteger counter = new AtomicInteger(0);

            System.out.println("  Starting fixed rate execution (every 500ms)");
            System.out.println("  Start time: " + currentTime());

            ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() -> {
                int count = counter.incrementAndGet();
                System.out.println("    [Tick-" + count + "] " + currentTime());

                // 작업이 주기보다 짧음
                sleep(100);
            }, 0, 500, TimeUnit.MILLISECONDS); // 초기 지연 0, 500ms 주기

            // 3초 동안 실행
            Thread.sleep(3000);

            future.cancel(false);
            System.out.println("  Stopped at: " + currentTime());
            System.out.println("  Total executions: " + counter.get());

            scheduler.shutdown();
        }
    }

    /**
     * 3. 고정 지연 실행 (scheduleWithFixedDelay)
     */
    static class FixedDelayExecution {
        public void demonstrate() throws InterruptedException {
            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

            AtomicInteger counter = new AtomicInteger(0);

            System.out.println("  Starting fixed delay execution (500ms delay after completion)");
            System.out.println("  Start time: " + currentTime());

            ScheduledFuture<?> future = scheduler.scheduleWithFixedDelay(() -> {
                int count = counter.incrementAndGet();
                System.out.println("    [Task-" + count + "] Start: " + currentTime());

                // 작업 실행 시간
                sleep(200);

                System.out.println("    [Task-" + count + "] End: " + currentTime());
            }, 0, 500, TimeUnit.MILLISECONDS); // 초기 지연 0, 완료 후 500ms 대기

            // 3초 동안 실행
            Thread.sleep(3000);

            future.cancel(false);
            System.out.println("  Stopped at: " + currentTime());
            System.out.println("  Total executions: " + counter.get());

            scheduler.shutdown();
        }
    }

    /**
     * 4. FixedRate vs FixedDelay 비교
     */
    static class RateVsDelayComparison {
        public void demonstrate() throws InterruptedException {
            System.out.println("  Comparing scheduleAtFixedRate vs scheduleWithFixedDelay");
            System.out.println("  Task duration: 300ms, Period/Delay: 500ms\n");

            // Fixed Rate
            System.out.println("  scheduleAtFixedRate:");
            ScheduledExecutorService scheduler1 = Executors.newScheduledThreadPool(1);
            AtomicInteger count1 = new AtomicInteger(0);

            ScheduledFuture<?> future1 = scheduler1.scheduleAtFixedRate(() -> {
                int n = count1.incrementAndGet();
                System.out.println("    [Rate-" + n + "] Start: " + currentTime());
                sleep(300);
                System.out.println("    [Rate-" + n + "] End: " + currentTime());
            }, 0, 500, TimeUnit.MILLISECONDS);

            Thread.sleep(2000);
            future1.cancel(false);
            scheduler1.shutdown();

            Thread.sleep(500);

            // Fixed Delay
            System.out.println("\n  scheduleWithFixedDelay:");
            ScheduledExecutorService scheduler2 = Executors.newScheduledThreadPool(1);
            AtomicInteger count2 = new AtomicInteger(0);

            ScheduledFuture<?> future2 = scheduler2.scheduleWithFixedDelay(() -> {
                int n = count2.incrementAndGet();
                System.out.println("    [Delay-" + n + "] Start: " + currentTime());
                sleep(300);
                System.out.println("    [Delay-" + n + "] End: " + currentTime());
            }, 0, 500, TimeUnit.MILLISECONDS);

            Thread.sleep(2000);
            future2.cancel(false);
            scheduler2.shutdown();

            System.out.println("\n  FixedRate executions: " + count1.get());
            System.out.println("  FixedDelay executions: " + count2.get());
        }
    }

    /**
     * 5. 여러 스케줄 작업 동시 실행
     */
    static class MultipleScheduledTasks {
        public void demonstrate() throws InterruptedException {
            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3);

            System.out.println("  Starting multiple scheduled tasks");

            // 작업 1: 매 1초
            scheduler.scheduleAtFixedRate(() -> {
                System.out.println("    [Every 1s] " + currentTime());
            }, 0, 1, TimeUnit.SECONDS);

            // 작업 2: 매 2초
            scheduler.scheduleAtFixedRate(() -> {
                System.out.println("    [Every 2s] " + currentTime());
            }, 0, 2, TimeUnit.SECONDS);

            // 작업 3: 매 3초
            scheduler.scheduleAtFixedRate(() -> {
                System.out.println("    [Every 3s] " + currentTime());
            }, 0, 3, TimeUnit.SECONDS);

            // 5초 동안 실행
            Thread.sleep(5000);

            scheduler.shutdownNow();
        }
    }

    /**
     * 6. 예외 처리
     */
    static class ExceptionHandling {
        public void demonstrate() throws InterruptedException {
            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

            AtomicInteger counter = new AtomicInteger(0);

            System.out.println("  Testing exception handling in scheduled tasks");

            scheduler.scheduleAtFixedRate(() -> {
                int count = counter.incrementAndGet();
                System.out.println("    [Task-" + count + "] Executing");

                if (count == 3) {
                    System.out.println("    [Task-" + count + "] Throwing exception!");
                    throw new RuntimeException("Simulated error");
                }

                System.out.println("    [Task-" + count + "] Completed");
            }, 0, 500, TimeUnit.MILLISECONDS);

            Thread.sleep(3000);

            System.out.println("  Total executions before exception: " + counter.get());
            System.out.println("  Note: Task stops after uncaught exception!");

            scheduler.shutdown();
        }
    }

    /**
     * 7. 안전한 예외 처리
     */
    static class SafeExceptionHandling {
        public void demonstrate() throws InterruptedException {
            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

            AtomicInteger counter = new AtomicInteger(0);
            AtomicInteger errors = new AtomicInteger(0);

            System.out.println("  Testing safe exception handling");

            ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() -> {
                int count = counter.incrementAndGet();

                try {
                    System.out.println("    [Task-" + count + "] Executing");

                    if (count % 3 == 0) {
                        throw new RuntimeException("Simulated error at count " + count);
                    }

                    System.out.println("    [Task-" + count + "] Completed");
                } catch (Exception e) {
                    errors.incrementAndGet();
                    System.out.println("    [Task-" + count + "] Error: " + e.getMessage());
                }
            }, 0, 300, TimeUnit.MILLISECONDS);

            Thread.sleep(3000);
            future.cancel(false);

            System.out.println("  Total executions: " + counter.get());
            System.out.println("  Total errors: " + errors.get());
            System.out.println("  Task continued after exceptions!");

            scheduler.shutdown();
        }
    }

    /**
     * 8. 작업 취소
     */
    static class TaskCancellation {
        public void demonstrate() throws InterruptedException {
            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

            AtomicInteger counter = new AtomicInteger(0);

            System.out.println("  Starting task with cancellation");

            ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() -> {
                int count = counter.incrementAndGet();
                System.out.println("    [Task-" + count + "] Executing at " + currentTime());
                sleep(100);
            }, 0, 500, TimeUnit.MILLISECONDS);

            Thread.sleep(1500);

            System.out.println("  Cancelling task at " + currentTime());
            boolean cancelled = future.cancel(false); // mayInterruptIfRunning = false

            System.out.println("  Task cancelled: " + cancelled);
            System.out.println("  Is cancelled: " + future.isCancelled());
            System.out.println("  Is done: " + future.isDone());
            System.out.println("  Final count: " + counter.get());

            scheduler.shutdown();
        }
    }

    /**
     * 9. 지연 시간 계산
     */
    static class DelayCalculation {
        public void demonstrate() throws InterruptedException {
            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

            System.out.println("  Scheduling tasks with calculated delays");

            // 현재 시각 기준으로 다음 시간 계산
            long now = System.currentTimeMillis();
            long next5Seconds = ((now / 5000) + 1) * 5000; // 다음 5초 단위
            long delay = next5Seconds - now;

            System.out.println("  Current time: " + currentTime());
            System.out.println("  Delay until next 5-second mark: " + delay + " ms");

            scheduler.schedule(() -> {
                System.out.println("    [Task] Executed at: " + currentTime());
                System.out.println("    [Task] Aligned to 5-second boundary!");
            }, delay, TimeUnit.MILLISECONDS);

            Thread.sleep(delay + 100);

            scheduler.shutdown();
        }
    }

    /**
     * 10. 실용 예제: 주기적 상태 체크
     */
    static class HealthCheckExample {
        private final AtomicInteger healthScore = new AtomicInteger(100);

        public void demonstrate() throws InterruptedException {
            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

            System.out.println("  Starting health check system");

            // 상태 체크 작업 (매 1초)
            scheduler.scheduleAtFixedRate(() -> {
                int score = healthScore.get();
                String status = score > 70 ? "HEALTHY" : score > 40 ? "WARNING" : "CRITICAL";

                System.out.println("    [HealthCheck] Score: " + score + " - " + status);

                // 점수 감소 시뮬레이션
                healthScore.addAndGet(-10);
            }, 0, 1, TimeUnit.SECONDS);

            // 복구 작업 (매 2초)
            scheduler.scheduleAtFixedRate(() -> {
                int recovered = healthScore.addAndGet(15);
                System.out.println("    [Recovery] Recovered to: " + recovered);
            }, 2, 2, TimeUnit.SECONDS);

            // 알림 작업 (매 3초)
            scheduler.scheduleAtFixedRate(() -> {
                int score = healthScore.get();
                if (score < 50) {
                    System.out.println("    [Alert] ⚠️  System health critical: " + score);
                }
            }, 3, 3, TimeUnit.SECONDS);

            Thread.sleep(10000);

            scheduler.shutdownNow();
        }
    }

    /**
     * 11. 실용 예제: 데이터 수집 및 집계
     */
    static class DataCollectionExample {
        private final ConcurrentLinkedQueue<Integer> dataQueue = new ConcurrentLinkedQueue<>();
        private final AtomicInteger totalProcessed = new AtomicInteger(0);

        public void demonstrate() throws InterruptedException {
            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

            System.out.println("  Starting data collection system");

            // 데이터 수집 (매 200ms)
            scheduler.scheduleAtFixedRate(() -> {
                int data = (int) (Math.random() * 100);
                dataQueue.offer(data);
                System.out.println("    [Collect] Data: " + data +
                        " (queue: " + dataQueue.size() + ")");
            }, 0, 200, TimeUnit.MILLISECONDS);

            // 데이터 처리 (매 1초)
            scheduler.scheduleAtFixedRate(() -> {
                int processed = 0;
                int sum = 0;

                while (!dataQueue.isEmpty()) {
                    Integer data = dataQueue.poll();
                    if (data != null) {
                        sum += data;
                        processed++;
                    }
                }

                if (processed > 0) {
                    totalProcessed.addAndGet(processed);
                    double avg = (double) sum / processed;
                    System.out.println("    [Process] Batch: " + processed +
                            " items, Avg: " + String.format("%.2f", avg));
                }
            }, 1, 1, TimeUnit.SECONDS);

            Thread.sleep(5000);

            scheduler.shutdownNow();
            System.out.println("  Total processed: " + totalProcessed.get() + " items");
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== ScheduledThreadPool Demo ===\n");

        // 1. 지연 실행
        System.out.println("1. Delayed Execution");
        new DelayedExecution().demonstrate();
        System.out.println();

        // 2. 고정 주기 실행
        System.out.println("2. Fixed Rate Execution");
        new FixedRateExecution().demonstrate();
        System.out.println();

        // 3. 고정 지연 실행
        System.out.println("3. Fixed Delay Execution");
        new FixedDelayExecution().demonstrate();
        System.out.println();

        // 4. Rate vs Delay 비교
        System.out.println("4. Rate vs Delay Comparison");
        new RateVsDelayComparison().demonstrate();
        System.out.println();

        // 5. 여러 스케줄 작업
        System.out.println("5. Multiple Scheduled Tasks");
        new MultipleScheduledTasks().demonstrate();
        System.out.println();

        // 6. 예외 처리 (위험)
        System.out.println("6. Exception Handling (Unsafe)");
        new ExceptionHandling().demonstrate();
        System.out.println();

        // 7. 안전한 예외 처리
        System.out.println("7. Safe Exception Handling");
        new SafeExceptionHandling().demonstrate();
        System.out.println();

        // 8. 작업 취소
        System.out.println("8. Task Cancellation");
        new TaskCancellation().demonstrate();
        System.out.println();

        // 9. 지연 시간 계산
        System.out.println("9. Delay Calculation");
        new DelayCalculation().demonstrate();
        System.out.println();

        // 10. 헬스 체크 예제
        System.out.println("10. Health Check Example");
        new HealthCheckExample().demonstrate();
        System.out.println();

        // 11. 데이터 수집 예제
        System.out.println("11. Data Collection Example");
        new DataCollectionExample().demonstrate();
        System.out.println();

        System.out.println("=== Demo Completed ===");
    }

    private static String currentTime() {
        return LocalDateTime.now().format(TIME_FORMAT);
    }

    private static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}