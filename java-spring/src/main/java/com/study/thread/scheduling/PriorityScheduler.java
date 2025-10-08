package com.study.thread.scheduling;

import java.util.PriorityQueue;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Priority 스케줄러 구현
 *
 * 우선순위가 높은 작업을 먼저 처리하는 알고리즘:
 * - 각 작업에 우선순위 부여
 * - 비선점형(Non-preemptive) / 선점형(Preemptive) 모두 가능
 * - PriorityQueue 사용
 *
 * 특징:
 * - 중요한 작업을 먼저 처리
 * - 낮은 우선순위 작업은 기아(Starvation) 발생 가능
 * - Aging 기법으로 기아 방지 가능
 */
public class PriorityScheduler {

    /**
     * 스케줄링할 작업
     */
    static class Task {
        final int id;
        final int burstTime; // 실행 시간 (ms)
        int priority; // 우선순위 (낮을수록 높은 우선순위)
        final int originalPriority;
        final long arrivalTime; // 도착 시간
        long startTime = -1; // 시작 시간
        long completionTime = -1; // 완료 시간
        long waitStartTime; // 대기 시작 시간

        public Task(int id, int burstTime, int priority) {
            this.id = id;
            this.burstTime = burstTime;
            this.priority = priority;
            this.originalPriority = priority;
            this.arrivalTime = System.currentTimeMillis();
            this.waitStartTime = arrivalTime;
        }

        public int getWaitingTime() {
            if (startTime == -1) return 0;
            return (int) (startTime - arrivalTime);
        }

        public int getTurnaroundTime() {
            if (completionTime == -1) return 0;
            return (int) (completionTime - arrivalTime);
        }

        public long getWaitDuration() {
            return System.currentTimeMillis() - waitStartTime;
        }

        @Override
        public String toString() {
            return String.format("Task-%d (burst: %dms, priority: %d)",
                    id, burstTime, priority);
        }
    }

    /**
     * 비선점형 Priority 스케줄러
     */
    static class NonPreemptiveScheduler {
        // priority가 낮을수록 높은 우선순위
        private final PriorityQueue<Task> readyQueue = new PriorityQueue<>(
                Comparator.comparingInt(t -> t.priority)
        );
        private volatile boolean running = false;
        private Thread schedulerThread;
        private final AtomicInteger completedTasks = new AtomicInteger(0);

        public void start() {
            if (running) return;

            running = true;
            schedulerThread = new Thread(this::schedule, "Priority-Scheduler");
            schedulerThread.start();

            System.out.println("  [Scheduler] Started (Non-Preemptive Priority)");
        }

        public void stop() {
            running = false;
            if (schedulerThread != null) {
                schedulerThread.interrupt();
            }
            System.out.println("  [Scheduler] Stopped");
        }

        public synchronized void submitTask(Task task) {
            readyQueue.offer(task);
            System.out.println("  [Scheduler] Task submitted: " + task);
            notifyAll();
        }

        private void schedule() {
            while (running) {
                Task task = null;

                synchronized (this) {
                    while (readyQueue.isEmpty() && running) {
                        try {
                            wait(100);
                        } catch (InterruptedException e) {
                            if (!running) break;
                        }
                    }

                    if (!readyQueue.isEmpty()) {
                        task = readyQueue.poll(); // 가장 높은 우선순위 작업
                    }
                }

                if (task != null) {
                    executeTask(task);
                }
            }
        }

        private void executeTask(Task task) {
            task.startTime = System.currentTimeMillis();

            System.out.println("  [Scheduler] Executing: " + task +
                    " (waiting: " + task.getWaitingTime() + "ms)");

            // 작업 실행 시뮬레이션
            sleep(task.burstTime);

            task.completionTime = System.currentTimeMillis();
            completedTasks.incrementAndGet();

            System.out.println("  [Scheduler] Completed: " + task +
                    " (turnaround: " + task.getTurnaroundTime() + "ms)");
        }

        public int getCompletedTaskCount() {
            return completedTasks.get();
        }
    }

    /**
     * Aging을 적용한 Priority 스케줄러
     */
    static class AgingScheduler {
        private final PriorityQueue<Task> readyQueue = new PriorityQueue<>(
                Comparator.comparingInt(t -> t.priority)
        );
        private volatile boolean running = false;
        private Thread schedulerThread;
        private Thread agingThread;
        private final AtomicInteger completedTasks = new AtomicInteger(0);
        private final int agingInterval; // Aging 주기 (ms)
        private final int agingAmount; // 우선순위 증가량

        public AgingScheduler(int agingInterval, int agingAmount) {
            this.agingInterval = agingInterval;
            this.agingAmount = agingAmount;
        }

        public void start() {
            if (running) return;

            running = true;

            schedulerThread = new Thread(this::schedule, "Priority-Scheduler");
            schedulerThread.start();

            agingThread = new Thread(this::aging, "Aging-Thread");
            agingThread.setDaemon(true);
            agingThread.start();

            System.out.println("  [Scheduler] Started (Priority with Aging, interval: " +
                    agingInterval + "ms, amount: " + agingAmount + ")");
        }

        public void stop() {
            running = false;
            if (schedulerThread != null) {
                schedulerThread.interrupt();
            }
            System.out.println("  [Scheduler] Stopped");
        }

        public synchronized void submitTask(Task task) {
            readyQueue.offer(task);
            System.out.println("  [Scheduler] Task submitted: " + task);
            notifyAll();
        }

        private void schedule() {
            while (running) {
                Task task = null;

                synchronized (this) {
                    while (readyQueue.isEmpty() && running) {
                        try {
                            wait(100);
                        } catch (InterruptedException e) {
                            if (!running) break;
                        }
                    }

                    if (!readyQueue.isEmpty()) {
                        task = readyQueue.poll();
                    }
                }

                if (task != null) {
                    executeTask(task);
                }
            }
        }

        private void aging() {
            while (running) {
                sleep(agingInterval);

                synchronized (this) {
                    java.util.List<Task> tasks = new java.util.ArrayList<>(readyQueue);
                    readyQueue.clear();

                    for (Task task : tasks) {
                        long waitTime = task.getWaitDuration();

                        // 일정 시간 대기한 작업의 우선순위 증가
                        if (waitTime > agingInterval) {
                            int oldPriority = task.priority;
                            task.priority = Math.max(0, task.priority - agingAmount);

                            if (oldPriority != task.priority) {
                                System.out.println("  [Aging] Task-" + task.id +
                                        " priority: " + oldPriority +
                                        " -> " + task.priority);
                            }
                        }

                        readyQueue.offer(task);
                    }
                }
            }
        }

        private void executeTask(Task task) {
            task.startTime = System.currentTimeMillis();

            System.out.println("  [Scheduler] Executing: " + task +
                    " (original priority: " + task.originalPriority +
                    ", waiting: " + task.getWaitingTime() + "ms)");

            sleep(task.burstTime);

            task.completionTime = System.currentTimeMillis();
            completedTasks.incrementAndGet();

            System.out.println("  [Scheduler] Completed: " + task);
        }

        public int getCompletedTaskCount() {
            return completedTasks.get();
        }
    }

    /**
     * 성능 분석
     */
    static class PerformanceAnalyzer {
        private final java.util.List<Task> tasks = new java.util.ArrayList<>();

        public void addTask(Task task) {
            tasks.add(task);
        }

        public void printAnalysis() {
            if (tasks.isEmpty()) {
                System.out.println("  No tasks to analyze");
                return;
            }

            System.out.println("\n  === Performance Analysis ===");

            // 완료 순서대로 정렬
            tasks.sort(Comparator.comparingLong(t -> t.completionTime));

            System.out.println("\n  Task Details (execution order):");
            System.out.println("  ID | Burst | Priority | Arrival | Start | Completion | Wait | Turnaround");
            System.out.println("  ---|-------|----------|---------|-------|------------|------|------------");

            long baseTime = tasks.stream()
                    .mapToLong(t -> t.arrivalTime)
                    .min()
                    .orElse(0);

            for (Task task : tasks) {
                System.out.printf("  %2d | %5d | %8d | %7d | %5d | %10d | %4d | %10d%n",
                        task.id,
                        task.burstTime,
                        task.originalPriority,
                        task.arrivalTime - baseTime,
                        task.startTime - baseTime,
                        task.completionTime - baseTime,
                        task.getWaitingTime(),
                        task.getTurnaroundTime());
            }

            // 평균 통계
            double avgWaitingTime = tasks.stream()
                    .mapToInt(Task::getWaitingTime)
                    .average()
                    .orElse(0);

            double avgTurnaroundTime = tasks.stream()
                    .mapToInt(Task::getTurnaroundTime)
                    .average()
                    .orElse(0);

            System.out.println("\n  Summary:");
            System.out.println("    Total tasks: " + tasks.size());
            System.out.println("    Avg waiting time: " + String.format("%.2f ms", avgWaitingTime));
            System.out.println("    Avg turnaround time: " + String.format("%.2f ms", avgTurnaroundTime));

            // 우선순위별 통계
            System.out.println("\n  By Priority:");
            tasks.stream()
                    .collect(java.util.stream.Collectors.groupingBy(t -> t.originalPriority))
                    .forEach((priority, taskList) -> {
                        double avgWait = taskList.stream()
                                .mapToInt(Task::getWaitingTime)
                                .average()
                                .orElse(0);
                        System.out.println("    Priority " + priority + ": " +
                                "avg wait = " + String.format("%.2f ms", avgWait));
                    });
        }
    }

    /**
     * 기아 현상 시연
     */
    static class StarvationDemo {
        public void demonstrate() throws InterruptedException {
            System.out.println("  Demonstrating starvation (without aging)");

            NonPreemptiveScheduler scheduler = new NonPreemptiveScheduler();
            PerformanceAnalyzer analyzer = new PerformanceAnalyzer();

            scheduler.start();

            // 낮은 우선순위 작업
            Task lowPriority = new Task(1, 200, 10);
            scheduler.submitTask(lowPriority);
            analyzer.addTask(lowPriority);

            Thread.sleep(50);

            // 높은 우선순위 작업들이 계속 도착
            for (int i = 2; i <= 6; i++) {
                Task highPriority = new Task(i, 100, 1);
                scheduler.submitTask(highPriority);
                analyzer.addTask(highPriority);
                Thread.sleep(100);
            }

            while (scheduler.getCompletedTaskCount() < 6) {
                Thread.sleep(100);
            }

            scheduler.stop();

            analyzer.printAnalysis();

            System.out.println("\n  Note: Low priority task (Task-1) starved by high priority tasks");
        }
    }

    /**
     * Aging으로 기아 방지
     */
    static class AgingDemo {
        public void demonstrate() throws InterruptedException {
            System.out.println("  Demonstrating aging to prevent starvation");

            AgingScheduler scheduler = new AgingScheduler(200, 1); // 200ms마다 우선순위 1씩 증가
            PerformanceAnalyzer analyzer = new PerformanceAnalyzer();

            scheduler.start();

            // 낮은 우선순위 작업
            Task lowPriority = new Task(1, 200, 10);
            scheduler.submitTask(lowPriority);
            analyzer.addTask(lowPriority);

            Thread.sleep(50);

            // 높은 우선순위 작업들
            for (int i = 2; i <= 6; i++) {
                Task highPriority = new Task(i, 100, 1);
                scheduler.submitTask(highPriority);
                analyzer.addTask(highPriority);
                Thread.sleep(100);
            }

            while (scheduler.getCompletedTaskCount() < 6) {
                Thread.sleep(100);
            }

            scheduler.stop();

            analyzer.printAnalysis();

            System.out.println("\n  Note: Aging increased priority of waiting tasks");
        }
    }

    /**
     * 다양한 우선순위 레벨 테스트
     */
    static class MultiLevelPriorityTest {
        public void demonstrate() throws InterruptedException {
            System.out.println("  Testing multiple priority levels");

            NonPreemptiveScheduler scheduler = new NonPreemptiveScheduler();
            PerformanceAnalyzer analyzer = new PerformanceAnalyzer();

            scheduler.start();

            // 우선순위: 1(highest), 3, 5, 7, 9(lowest)
            Task task1 = new Task(1, 150, 5);
            Task task2 = new Task(2, 200, 1);
            Task task3 = new Task(3, 100, 9);
            Task task4 = new Task(4, 180, 3);
            Task task5 = new Task(5, 120, 7);

            scheduler.submitTask(task1);
            scheduler.submitTask(task2);
            scheduler.submitTask(task3);
            scheduler.submitTask(task4);
            scheduler.submitTask(task5);

            analyzer.addTask(task1);
            analyzer.addTask(task2);
            analyzer.addTask(task3);
            analyzer.addTask(task4);
            analyzer.addTask(task5);

            while (scheduler.getCompletedTaskCount() < 5) {
                Thread.sleep(100);
            }

            scheduler.stop();

            analyzer.printAnalysis();

            System.out.println("\n  Note: Tasks executed in priority order: 1, 3, 5, 7, 9");
        }
    }

    /**
     * Priority 스케줄러의 장단점
     */
    static class ProsCons {
        public void demonstrate() {
            System.out.println("\n  === Priority Scheduler Analysis ===");

            System.out.println("\n  ✓ Advantages:");
            System.out.println("    - Important tasks processed first");
            System.out.println("    - Flexible (can adjust priorities)");
            System.out.println("    - Suitable for real-time systems");
            System.out.println("    - Can reflect task importance");
            System.out.println("    - Good for meeting deadlines");

            System.out.println("\n  ✗ Disadvantages:");
            System.out.println("    - Starvation of low-priority tasks");
            System.out.println("    - Priority inversion problem");
            System.out.println("    - Requires priority assignment");
            System.out.println("    - Complex priority management");
            System.out.println("    - Unfair to low-priority processes");

            System.out.println("\n  Starvation Prevention:");
            System.out.println("    - Aging: Gradually increase priority");
            System.out.println("    - Priority ceiling");
            System.out.println("    - Time limits for high-priority tasks");
            System.out.println("    - Dynamic priority adjustment");

            System.out.println("\n  Best Use Cases:");
            System.out.println("    - Real-time systems");
            System.out.println("    - Systems with clear task importance");
            System.out.println("    - Mission-critical applications");
            System.out.println("    - Systems requiring deadline guarantees");

            System.out.println("\n  Worst Use Cases:");
            System.out.println("    - Systems requiring fairness");
            System.out.println("    - When priorities are hard to determine");
            System.out.println("    - Interactive systems (without aging)");
            System.out.println("    - Systems with many equal-priority tasks");
        }
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Priority Scheduler Demo ===\n");

        // 1. 기본 Priority 스케줄링
        System.out.println("1. Basic Priority Scheduling");
        demonstrateBasicPriority();
        System.out.println();

        // 2. 기아 현상
        System.out.println("2. Starvation Demo");
        new StarvationDemo().demonstrate();
        System.out.println();

        // 3. Aging으로 기아 방지
        System.out.println("3. Aging Demo (Starvation Prevention)");
        new AgingDemo().demonstrate();
        System.out.println();

        // 4. 다중 우선순위 레벨
        System.out.println("4. Multi-Level Priority Test");
        new MultiLevelPriorityTest().demonstrate();
        System.out.println();

        // 5. 장단점 분석
        System.out.println("5. Pros and Cons Analysis");
        new ProsCons().demonstrate();
        System.out.println();

        System.out.println("=== Demo Completed ===");
    }

    private static void demonstrateBasicPriority() throws InterruptedException {
        NonPreemptiveScheduler scheduler = new NonPreemptiveScheduler();
        PerformanceAnalyzer analyzer = new PerformanceAnalyzer();

        scheduler.start();

        // 다양한 우선순위의 작업들
        Task task1 = new Task(1, 200, 3);
        Task task2 = new Task(2, 150, 1);
        Task task3 = new Task(3, 180, 5);
        Task task4 = new Task(4, 120, 2);
        Task task5 = new Task(5, 160, 4);

        scheduler.submitTask(task1);
        scheduler.submitTask(task2);
        scheduler.submitTask(task3);
        scheduler.submitTask(task4);
        scheduler.submitTask(task5);

        analyzer.addTask(task1);
        analyzer.addTask(task2);
        analyzer.addTask(task3);
        analyzer.addTask(task4);
        analyzer.addTask(task5);

        while (scheduler.getCompletedTaskCount() < 5) {
            Thread.sleep(100);
        }

        scheduler.stop();

        analyzer.printAnalysis();
    }

    private static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}