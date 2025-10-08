package com.study.thread.pool;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 스레드 풀 모니터링
 *
 * ThreadPoolExecutor의 상태를 실시간으로 모니터링하고 분석합니다.
 * - 실시간 통계 수집
 * - 성능 메트릭 추적
 * - 알림 및 경고
 * - 히스토리 기록
 */
public class ThreadPoolMonitor {

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    /**
     * 1. 기본 모니터링 정보
     */
    static class BasicMonitoring {
        public void demonstrate() throws InterruptedException {
            System.out.println("  Creating monitored thread pool");

            ThreadPoolExecutor executor = (ThreadPoolExecutor)
                    Executors.newFixedThreadPool(3);

            // 초기 상태
            printPoolState("Initial", executor);

            // 작업 제출
            for (int i = 1; i <= 10; i++) {
                final int taskId = i;
                executor.submit(() -> {
                    System.out.println("    [Task-" + taskId + "] Executing");
                    sleep(200);
                });
            }

            Thread.sleep(100);
            printPoolState("During execution", executor);

            Thread.sleep(500);
            printPoolState("Mid execution", executor);

            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);

            printPoolState("After completion", executor);
        }

        private void printPoolState(String label, ThreadPoolExecutor executor) {
            System.out.println("\n  [" + label + "]");
            System.out.println("    Pool size: " + executor.getPoolSize());
            System.out.println("    Active threads: " + executor.getActiveCount());
            System.out.println("    Core pool size: " + executor.getCorePoolSize());
            System.out.println("    Max pool size: " + executor.getMaximumPoolSize());
            System.out.println("    Largest pool size: " + executor.getLargestPoolSize());
            System.out.println("    Task count: " + executor.getTaskCount());
            System.out.println("    Completed tasks: " + executor.getCompletedTaskCount());
            System.out.println("    Queue size: " + executor.getQueue().size());
        }
    }

    /**
     * 2. 실시간 모니터
     */
    static class RealTimeMonitor {
        private final ThreadPoolExecutor executor;
        private volatile boolean monitoring = false;
        private Thread monitorThread;

        public RealTimeMonitor(ThreadPoolExecutor executor) {
            this.executor = executor;
        }

        public void startMonitoring(long intervalMs) {
            if (monitoring) return;

            monitoring = true;
            monitorThread = new Thread(() -> {
                System.out.println("  [Monitor] Started (interval: " + intervalMs + "ms)");

                while (monitoring && !executor.isTerminated()) {
                    printSnapshot();
                    sleep((int) intervalMs);
                }

                System.out.println("  [Monitor] Stopped");
            }, "PoolMonitor");

            monitorThread.setDaemon(true);
            monitorThread.start();
        }

        public void stopMonitoring() {
            monitoring = false;
            if (monitorThread != null) {
                monitorThread.interrupt();
            }
        }

        private void printSnapshot() {
            System.out.println("    [" + currentTime() + "] " +
                    "Pool: " + executor.getPoolSize() +
                    ", Active: " + executor.getActiveCount() +
                    ", Queue: " + executor.getQueue().size() +
                    ", Completed: " + executor.getCompletedTaskCount());
        }

        public void demonstrate() throws InterruptedException {
            System.out.println("  Starting real-time monitoring");

            startMonitoring(500);

            // 작업 제출
            for (int i = 1; i <= 15; i++) {
                executor.submit(() -> sleep(300));
                Thread.sleep(100);
            }

            Thread.sleep(2000);
            stopMonitoring();
        }
    }

    /**
     * 3. 통계 수집기
     */
    static class StatisticsCollector {
        private final ThreadPoolExecutor executor;
        private final List<Snapshot> history = new ArrayList<>();

        static class Snapshot {
            final LocalDateTime timestamp;
            final int poolSize;
            final int activeCount;
            final int queueSize;
            final long completedTasks;

            Snapshot(ThreadPoolExecutor executor) {
                this.timestamp = LocalDateTime.now();
                this.poolSize = executor.getPoolSize();
                this.activeCount = executor.getActiveCount();
                this.queueSize = executor.getQueue().size();
                this.completedTasks = executor.getCompletedTaskCount();
            }

            @Override
            public String toString() {
                return String.format("[%s] Pool: %d, Active: %d, Queue: %d, Completed: %d",
                        timestamp.format(TIME_FORMAT), poolSize, activeCount,
                        queueSize, completedTasks);
            }
        }

        public StatisticsCollector(ThreadPoolExecutor executor) {
            this.executor = executor;
        }

        public void takeSnapshot() {
            history.add(new Snapshot(executor));
        }

        public void printStatistics() {
            System.out.println("\n  Statistics Summary:");
            System.out.println("    Total snapshots: " + history.size());

            if (history.isEmpty()) return;

            int maxPoolSize = history.stream()
                    .mapToInt(s -> s.poolSize)
                    .max()
                    .orElse(0);

            int maxActiveCount = history.stream()
                    .mapToInt(s -> s.activeCount)
                    .max()
                    .orElse(0);

            int maxQueueSize = history.stream()
                    .mapToInt(s -> s.queueSize)
                    .max()
                    .orElse(0);

            System.out.println("    Max pool size: " + maxPoolSize);
            System.out.println("    Max active threads: " + maxActiveCount);
            System.out.println("    Max queue size: " + maxQueueSize);
            System.out.println("    Final completed tasks: " +
                    history.get(history.size() - 1).completedTasks);
        }

        public void printHistory() {
            System.out.println("\n  History:");
            history.forEach(s -> System.out.println("    " + s));
        }

        public void demonstrate() throws InterruptedException {
            System.out.println("  Collecting statistics");

            // 주기적으로 스냅샷 수집
            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
            scheduler.scheduleAtFixedRate(
                    this::takeSnapshot, 0, 200, TimeUnit.MILLISECONDS);

            // 작업 제출
            for (int i = 1; i <= 10; i++) {
                executor.submit(() -> sleep(300));
                Thread.sleep(100);
            }

            Thread.sleep(2000);
            scheduler.shutdown();

            printStatistics();
            printHistory();
        }
    }

    /**
     * 4. 성능 메트릭
     */
    static class PerformanceMetrics {
        private final ThreadPoolExecutor executor;
        private long startTime;
        private final AtomicInteger submittedTasks = new AtomicInteger(0);

        public PerformanceMetrics(ThreadPoolExecutor executor) {
            this.executor = executor;
            this.startTime = System.currentTimeMillis();
        }

        public void trackSubmission() {
            submittedTasks.incrementAndGet();
        }

        public void printMetrics() {
            long elapsed = System.currentTimeMillis() - startTime;
            long completed = executor.getCompletedTaskCount();

            System.out.println("\n  Performance Metrics:");
            System.out.println("    Elapsed time: " + elapsed + " ms");
            System.out.println("    Submitted tasks: " + submittedTasks.get());
            System.out.println("    Completed tasks: " + completed);
            System.out.println("    Success rate: " +
                    String.format("%.2f%%", (completed * 100.0 / submittedTasks.get())));
            System.out.println("    Throughput: " +
                    String.format("%.2f tasks/sec", (completed * 1000.0 / elapsed)));
            System.out.println("    Avg task time: " +
                    String.format("%.2f ms", (elapsed / (double) completed)));

            double utilization = (executor.getCompletedTaskCount() * 100.0) /
                    (executor.getPoolSize() * elapsed);
            System.out.println("    Thread utilization: " +
                    String.format("%.2f%%", utilization));
        }

        public void demonstrate() throws InterruptedException {
            System.out.println("  Tracking performance metrics");

            for (int i = 1; i <= 20; i++) {
                executor.submit(() -> sleep(100));
                trackSubmission();
                Thread.sleep(50);
            }

            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);

            printMetrics();
        }
    }

    /**
     * 5. 알림 시스템
     */
    static class AlertSystem {
        private final ThreadPoolExecutor executor;
        private final int queueSizeThreshold;
        private final int activeThreadThreshold;
        private final AtomicInteger alertCount = new AtomicInteger(0);

        public AlertSystem(ThreadPoolExecutor executor,
                           int queueSizeThreshold,
                           int activeThreadThreshold) {
            this.executor = executor;
            this.queueSizeThreshold = queueSizeThreshold;
            this.activeThreadThreshold = activeThreadThreshold;
        }

        public void checkAndAlert() {
            int queueSize = executor.getQueue().size();
            int activeThreads = executor.getActiveCount();

            if (queueSize > queueSizeThreshold) {
                alertCount.incrementAndGet();
                System.out.println("    [ALERT] Queue size exceeded: " + queueSize +
                        " > " + queueSizeThreshold);
            }

            if (activeThreads >= activeThreadThreshold) {
                alertCount.incrementAndGet();
                System.out.println("    [ALERT] Active threads at capacity: " +
                        activeThreads + " / " + activeThreadThreshold);
            }
        }

        public void demonstrate() throws InterruptedException {
            System.out.println("  Alert system monitoring (queue > 5, threads >= 3)");

            // 주기적으로 체크
            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
            scheduler.scheduleAtFixedRate(
                    this::checkAndAlert, 0, 100, TimeUnit.MILLISECONDS);

            // 부하 생성
            for (int i = 1; i <= 15; i++) {
                executor.submit(() -> sleep(500));
                Thread.sleep(50);
            }

            Thread.sleep(2000);
            scheduler.shutdown();

            System.out.println("  Total alerts: " + alertCount.get());
        }
    }

    /**
     * 6. Rejected Execution Handler 모니터링
     */
    static class RejectionMonitor {
        private final AtomicInteger rejectedCount = new AtomicInteger(0);

        class MonitoredRejectionHandler implements RejectedExecutionHandler {
            @Override
            public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                rejectedCount.incrementAndGet();
                System.out.println("    [REJECTED] Task rejected (total: " +
                        rejectedCount.get() + ")");
                System.out.println("      Pool size: " + executor.getPoolSize());
                System.out.println("      Queue size: " + executor.getQueue().size());
            }
        }

        public void demonstrate() throws InterruptedException {
            System.out.println("  Monitoring rejected executions");

            // 작은 큐를 가진 풀 생성
            ThreadPoolExecutor executor = new ThreadPoolExecutor(
                    2, 2, 60, TimeUnit.SECONDS,
                    new ArrayBlockingQueue<>(3),
                    new MonitoredRejectionHandler()
            );

            // 용량 초과 작업 제출
            for (int i = 1; i <= 10; i++) {
                final int taskId = i;
                try {
                    executor.submit(() -> {
                        System.out.println("    [Task-" + taskId + "] Executing");
                        sleep(500);
                    });
                } catch (RejectedExecutionException e) {
                    // 이미 핸들러에서 처리됨
                }
                Thread.sleep(50);
            }

            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);

            System.out.println("  Total rejections: " + rejectedCount.get());
        }
    }

    /**
     * 7. 커스텀 ThreadFactory 모니터링
     */
    static class MonitoredThreadFactory implements ThreadFactory {
        private final AtomicInteger threadCount = new AtomicInteger(0);
        private final String namePrefix;

        public MonitoredThreadFactory(String namePrefix) {
            this.namePrefix = namePrefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            int count = threadCount.incrementAndGet();
            Thread thread = new Thread(r, namePrefix + count);

            System.out.println("    [Factory] Created thread: " + thread.getName());

            return thread;
        }

        public int getThreadCount() {
            return threadCount.get();
        }

        public void demonstrate() throws InterruptedException {
            System.out.println("  Monitoring thread creation");

            ThreadPoolExecutor executor = new ThreadPoolExecutor(
                    0, 5, 60, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(),
                    this
            );

            for (int i = 1; i <= 8; i++) {
                executor.submit(() -> sleep(200));
                Thread.sleep(100);
            }

            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);

            System.out.println("  Total threads created: " + getThreadCount());
        }
    }

    /**
     * 8. 대시보드
     */
    static class Dashboard {
        private final ThreadPoolExecutor executor;

        public Dashboard(ThreadPoolExecutor executor) {
            this.executor = executor;
        }

        public void display() {
            System.out.println("\n╔════════════════════════════════════════════════╗");
            System.out.println("║         Thread Pool Dashboard                  ║");
            System.out.println("╠════════════════════════════════════════════════╣");
            System.out.println("║ Time: " + currentTime() + "                          ║");
            System.out.println("╠════════════════════════════════════════════════╣");

            int poolSize = executor.getPoolSize();
            int coreSize = executor.getCorePoolSize();
            int maxSize = executor.getMaximumPoolSize();
            int active = executor.getActiveCount();
            int queueSize = executor.getQueue().size();
            long completed = executor.getCompletedTaskCount();
            long total = executor.getTaskCount();

            System.out.println("║ Pool Size:        " +
                    String.format("%-3d / %-3d (max: %-3d)       ║",
                            poolSize, coreSize, maxSize));
            System.out.println("║ Active Threads:   " +
                    String.format("%-30d║", active));
            System.out.println("║ Queue Size:       " +
                    String.format("%-30d║", queueSize));
            System.out.println("║ Completed Tasks:  " +
                    String.format("%-30d║", completed));
            System.out.println("║ Total Tasks:      " +
                    String.format("%-30d║", total));

            double progress = (total > 0) ? (completed * 100.0 / total) : 0;
            System.out.println("║ Progress:         " +
                    String.format("%.2f%%", progress) +
                    "                          ║");

            String utilizationBar = getUtilizationBar(active, coreSize);
            System.out.println("║ Utilization:      " + utilizationBar + "║");

            System.out.println("╚════════════════════════════════════════════════╝");
        }

        private String getUtilizationBar(int active, int total) {
            int barLength = 30;
            int filled = (int) ((active / (double) total) * barLength);

            StringBuilder bar = new StringBuilder("[");
            for (int i = 0; i < barLength; i++) {
                bar.append(i < filled ? "█" : "░");
            }
            bar.append("]");

            return bar.toString();
        }

        public void demonstrate() throws InterruptedException {
            System.out.println("  Starting dashboard");

            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
            scheduler.scheduleAtFixedRate(this::display, 0, 500, TimeUnit.MILLISECONDS);

            // 작업 제출
            for (int i = 1; i <= 20; i++) {
                executor.submit(() -> sleep(300));
                Thread.sleep(100);
            }

            Thread.sleep(3000);
            scheduler.shutdown();

            display(); // 최종 상태
        }
    }

    /**
     * 9. 종합 모니터링 시스템
     */
    static class ComprehensiveMonitor {
        public void demonstrate() throws InterruptedException {
            System.out.println("  Comprehensive monitoring system");

            ThreadPoolExecutor executor = new ThreadPoolExecutor(
                    3, 6, 60, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(10)
            );

            // 여러 모니터 동시 실행
            RealTimeMonitor realTimeMonitor = new RealTimeMonitor(executor);
            StatisticsCollector statsCollector = new StatisticsCollector(executor);
            PerformanceMetrics perfMetrics = new PerformanceMetrics(executor);
            AlertSystem alertSystem = new AlertSystem(executor, 5, 5);

            realTimeMonitor.startMonitoring(500);

            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
            scheduler.scheduleAtFixedRate(statsCollector::takeSnapshot,
                    0, 200, TimeUnit.MILLISECONDS);
            scheduler.scheduleAtFixedRate(alertSystem::checkAndAlert,
                    0, 300, TimeUnit.MILLISECONDS);

            // 작업 부하
            for (int i = 1; i <= 25; i++) {
                executor.submit(() -> sleep(200));
                perfMetrics.trackSubmission();
                Thread.sleep(80);
            }

            Thread.sleep(2000);

            realTimeMonitor.stopMonitoring();
            scheduler.shutdown();
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);

            // 최종 리포트
            System.out.println("\n=== Final Report ===");
            statsCollector.printStatistics();
            perfMetrics.printMetrics();
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== Thread Pool Monitor Demo ===\n");

        // 1. 기본 모니터링
        System.out.println("1. Basic Monitoring");
        new BasicMonitoring().demonstrate();
        System.out.println();

        // 2. 실시간 모니터
        System.out.println("2. Real-Time Monitor");
        ThreadPoolExecutor executor2 = (ThreadPoolExecutor)
                Executors.newFixedThreadPool(3);
        new RealTimeMonitor(executor2).demonstrate();
        executor2.shutdown();
        System.out.println();

        // 3. 통계 수집
        System.out.println("3. Statistics Collector");
        ThreadPoolExecutor executor3 = (ThreadPoolExecutor)
                Executors.newFixedThreadPool(2);
        new StatisticsCollector(executor3).demonstrate();
        executor3.shutdown();
        System.out.println();

        // 4. 성능 메트릭
        System.out.println("4. Performance Metrics");
        ThreadPoolExecutor executor4 = (ThreadPoolExecutor)
                Executors.newFixedThreadPool(4);
        new PerformanceMetrics(executor4).demonstrate();
        System.out.println();

        // 5. 알림 시스템
        System.out.println("5. Alert System");
        ThreadPoolExecutor executor5 = (ThreadPoolExecutor)
                Executors.newFixedThreadPool(3);
        new AlertSystem(executor5, 5, 3).demonstrate();
        executor5.shutdown();
        System.out.println();

        // 6. Rejection 모니터링
        System.out.println("6. Rejection Monitor");
        new RejectionMonitor().demonstrate();
        System.out.println();

        // 7. ThreadFactory 모니터링
        System.out.println("7. Thread Factory Monitor");
        new MonitoredThreadFactory("MonitoredThread-").demonstrate();
        System.out.println();

        // 8. 대시보드
        System.out.println("8. Dashboard");
        ThreadPoolExecutor executor8 = (ThreadPoolExecutor)
                Executors.newFixedThreadPool(4);
        new Dashboard(executor8).demonstrate();
        executor8.shutdown();
        System.out.println();

        // 9. 종합 모니터링
        System.out.println("9. Comprehensive Monitor");
        new ComprehensiveMonitor().demonstrate();
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