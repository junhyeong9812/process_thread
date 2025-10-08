package com.study.thread.scheduling;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MLFQ (Multi-Level Feedback Queue) 스케줄러 구현
 *
 * 여러 개의 우선순위 큐를 사용하는 적응형 스케줄링 알고리즘:
 * - 여러 레벨의 큐 (우선순위별)
 * - 각 레벨마다 다른 time quantum
 * - 작업의 행동에 따라 큐 이동 (feedback)
 * - CPU 바운드와 I/O 바운드 작업을 자동으로 구분
 *
 * 규칙:
 * 1. 새 작업은 최상위 큐에 진입
 * 2. Time quantum 소진 시 하위 큐로 이동
 * 3. 자발적으로 CPU 양보 시 같은 레벨 유지
 * 4. 주기적으로 모든 작업을 최상위 큐로 승격 (기아 방지)
 */
public class MLFQScheduler {

    /**
     * 스케줄링할 작업
     */
    static class Task {
        final int id;
        int remainingTime;
        final int originalBurstTime;
        final long arrivalTime;
        long firstStartTime = -1;
        long completionTime = -1;
        int currentQueue = 0; // 현재 큐 레벨
        int executionCount = 0;
        int demotionCount = 0; // 하위 큐로 내려간 횟수

        public Task(int id, int burstTime) {
            this.id = id;
            this.originalBurstTime = burstTime;
            this.remainingTime = burstTime;
            this.arrivalTime = System.currentTimeMillis();
        }

        public boolean isCompleted() {
            return remainingTime <= 0;
        }

        public int getResponseTime() {
            if (firstStartTime == -1) return 0;
            return (int) (firstStartTime - arrivalTime);
        }

        public int getTurnaroundTime() {
            if (completionTime == -1) return 0;
            return (int) (completionTime - arrivalTime);
        }

        public int getWaitingTime() {
            return getTurnaroundTime() - originalBurstTime;
        }

        @Override
        public String toString() {
            return String.format("Task-%d (Q%d, remaining: %d/%dms)",
                    id, currentQueue, remainingTime, originalBurstTime);
        }
    }

    /**
     * MLFQ 스케줄러 구현
     */
    static class Scheduler {
        private final List<Queue<Task>> queues; // 여러 레벨의 큐
        private final int[] timeQuantums; // 각 레벨의 time quantum
        private final int numQueues;
        private volatile boolean running = false;
        private Thread schedulerThread;
        private Thread boostThread;
        private final AtomicInteger completedTasks = new AtomicInteger(0);
        private final int boostInterval; // 승격 주기 (ms)

        public Scheduler(int[] timeQuantums, int boostInterval) {
            this.numQueues = timeQuantums.length;
            this.timeQuantums = timeQuantums;
            this.boostInterval = boostInterval;
            this.queues = new ArrayList<>();

            for (int i = 0; i < numQueues; i++) {
                queues.add(new LinkedList<>());
            }
        }

        public void start() {
            if (running) return;

            running = true;
            schedulerThread = new Thread(this::schedule, "MLFQ-Scheduler");
            schedulerThread.start();

            if (boostInterval > 0) {
                boostThread = new Thread(this::priorityBoost, "Priority-Boost");
                boostThread.setDaemon(true);
                boostThread.start();
            }

            System.out.println("  [Scheduler] Started (MLFQ)");
            System.out.println("  Queue configuration:");
            for (int i = 0; i < numQueues; i++) {
                System.out.println("    Q" + i + ": quantum = " + timeQuantums[i] + "ms");
            }
            System.out.println("  Boost interval: " + boostInterval + "ms");
        }

        public void stop() {
            running = false;
            if (schedulerThread != null) {
                schedulerThread.interrupt();
            }
            System.out.println("  [Scheduler] Stopped");
        }

        public synchronized void submitTask(Task task) {
            queues.get(0).offer(task); // 새 작업은 최상위 큐에
            System.out.println("  [Scheduler] Task submitted to Q0: " + task);
            notifyAll();
        }

        private void schedule() {
            while (running) {
                Task task = null;
                int queueLevel = -1;

                synchronized (this) {
                    // 높은 우선순위 큐부터 확인
                    for (int i = 0; i < numQueues; i++) {
                        if (!queues.get(i).isEmpty()) {
                            task = queues.get(i).poll();
                            queueLevel = i;
                            break;
                        }
                    }

                    if (task == null) {
                        try {
                            wait(100);
                        } catch (InterruptedException e) {
                            if (!running) break;
                        }
                        continue;
                    }
                }

                if (task != null) {
                    executeTask(task, queueLevel);
                }
            }
        }

        private void executeTask(Task task, int queueLevel) {
            if (task.firstStartTime == -1) {
                task.firstStartTime = System.currentTimeMillis();
            }

            task.executionCount++;
            int quantum = timeQuantums[queueLevel];
            int executeTime = Math.min(quantum, task.remainingTime);

            System.out.println("  [Scheduler] Executing: " + task +
                    " for " + executeTime + "ms (execution #" +
                    task.executionCount + ")");

            sleep(executeTime);

            task.remainingTime -= executeTime;

            synchronized (this) {
                if (task.isCompleted()) {
                    task.completionTime = System.currentTimeMillis();
                    completedTasks.incrementAndGet();
                    System.out.println("  [Scheduler] Completed: Task-" + task.id +
                            " (turnaround: " + task.getTurnaroundTime() +
                            "ms, demotions: " + task.demotionCount + ")");
                } else if (executeTime >= quantum) {
                    // Time quantum 소진 -> 하위 큐로 이동
                    int newQueue = Math.min(queueLevel + 1, numQueues - 1);

                    if (newQueue != queueLevel) {
                        task.demotionCount++;
                        System.out.println("  [Scheduler] Demoted: Task-" + task.id +
                                " Q" + queueLevel + " -> Q" + newQueue);
                    }

                    task.currentQueue = newQueue;
                    queues.get(newQueue).offer(task);
                } else {
                    // Time quantum 내에 완료 못함 (I/O 등) -> 같은 레벨 유지
                    queues.get(queueLevel).offer(task);
                    System.out.println("  [Scheduler] Requeued: " + task +
                            " (stayed in Q" + queueLevel + ")");
                }
            }
        }

        private void priorityBoost() {
            while (running) {
                sleep(boostInterval);

                synchronized (this) {
                    System.out.println("  [Boost] Priority boost triggered!");

                    // 모든 작업을 최상위 큐로 이동
                    for (int i = 1; i < numQueues; i++) {
                        Queue<Task> queue = queues.get(i);
                        while (!queue.isEmpty()) {
                            Task task = queue.poll();
                            task.currentQueue = 0;
                            queues.get(0).offer(task);
                            System.out.println("  [Boost] Task-" + task.id +
                                    " promoted to Q0");
                        }
                    }
                }
            }
        }

        public int getCompletedTaskCount() {
            return completedTasks.get();
        }
    }

    /**
     * 성능 분석
     */
    static class PerformanceAnalyzer {
        private final List<Task> tasks = new ArrayList<>();

        public void addTask(Task task) {
            tasks.add(task);
        }

        public void printAnalysis() {
            if (tasks.isEmpty()) {
                System.out.println("  No tasks to analyze");
                return;
            }

            System.out.println("\n  === Performance Analysis ===");

            System.out.println("\n  Task Details:");
            System.out.println("  ID | Burst | First Start | Completion | Response | Wait | Turnaround | Execs | Demotions");
            System.out.println("  ---|-------|-------------|------------|----------|------|------------|-------|----------");

            long baseTime = tasks.stream()
                    .mapToLong(t -> t.arrivalTime)
                    .min()
                    .orElse(0);

            for (Task task : tasks) {
                System.out.printf("  %2d | %5d | %11d | %10d | %8d | %4d | %10d | %5d | %9d%n",
                        task.id,
                        task.originalBurstTime,
                        task.firstStartTime - baseTime,
                        task.completionTime - baseTime,
                        task.getResponseTime(),
                        task.getWaitingTime(),
                        task.getTurnaroundTime(),
                        task.executionCount,
                        task.demotionCount);
            }

            // 평균 통계
            double avgResponseTime = tasks.stream()
                    .mapToInt(Task::getResponseTime)
                    .average()
                    .orElse(0);

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
            System.out.println("    Avg response time: " + String.format("%.2f ms", avgResponseTime));
            System.out.println("    Avg waiting time: " + String.format("%.2f ms", avgWaitingTime));
            System.out.println("    Avg turnaround time: " + String.format("%.2f ms", avgTurnaroundTime));
        }
    }

    /**
     * CPU 바운드 vs I/O 바운드 작업 구분
     */
    static class WorkloadSeparationDemo {
        public void demonstrate() throws InterruptedException {
            System.out.println("  Demonstrating CPU-bound vs I/O-bound separation");

            int[] quantums = {50, 100, 200}; // 레벨별 quantum
            Scheduler scheduler = new Scheduler(quantums, 0); // boost 비활성화
            PerformanceAnalyzer analyzer = new PerformanceAnalyzer();

            scheduler.start();

            // I/O 바운드 작업들 (짧은 CPU burst)
            System.out.println("  Submitting I/O-bound tasks (short bursts):");
            for (int i = 1; i <= 3; i++) {
                Task ioTask = new Task(i, 30); // 30ms (quantum보다 짧음)
                scheduler.submitTask(ioTask);
                analyzer.addTask(ioTask);
            }

            // CPU 바운드 작업들 (긴 CPU burst)
            System.out.println("  Submitting CPU-bound tasks (long bursts):");
            for (int i = 4; i <= 6; i++) {
                Task cpuTask = new Task(i, 500); // 500ms (여러 quantum)
                scheduler.submitTask(cpuTask);
                analyzer.addTask(cpuTask);
            }

            while (scheduler.getCompletedTaskCount() < 6) {
                Thread.sleep(100);
            }

            scheduler.stop();

            analyzer.printAnalysis();

            System.out.println("\n  Note: I/O-bound tasks stayed in high-priority queues");
            System.out.println("        CPU-bound tasks moved to lower-priority queues");
        }
    }

    /**
     * Priority Boost 효과
     */
    static class PriorityBoostDemo {
        public void demonstrate() throws InterruptedException {
            System.out.println("  Demonstrating priority boost (starvation prevention)");

            int[] quantums = {50, 100, 200};
            Scheduler scheduler = new Scheduler(quantums, 800); // 800ms마다 boost
            PerformanceAnalyzer analyzer = new PerformanceAnalyzer();

            scheduler.start();

            // 긴 CPU 바운드 작업
            Task longTask = new Task(1, 1000);
            scheduler.submitTask(longTask);
            analyzer.addTask(longTask);

            Thread.sleep(200);

            // 짧은 I/O 바운드 작업들이 계속 도착
            for (int i = 2; i <= 5; i++) {
                Task shortTask = new Task(i, 50);
                scheduler.submitTask(shortTask);
                analyzer.addTask(shortTask);
                Thread.sleep(150);
            }

            while (scheduler.getCompletedTaskCount() < 5) {
                Thread.sleep(100);
            }

            scheduler.stop();

            analyzer.printAnalysis();

            System.out.println("\n  Note: Priority boost prevented long task from starving");
        }
    }

    /**
     * 다양한 Quantum 설정 비교
     */
    static class QuantumConfigComparison {
        public void demonstrate() throws InterruptedException {
            System.out.println("  Comparing different quantum configurations");

            int[][] configs = {
                    {50, 100, 200},    // 빠르게 증가
                    {100, 200, 400},   // 느리게 증가
                    {50, 50, 50}       // 동일 (Round Robin과 유사)
            };

            int[] burstTimes = {150, 200, 350, 100, 300};

            for (int c = 0; c < configs.length; c++) {
                System.out.println("\n  Configuration " + (c + 1) + ": " +
                        java.util.Arrays.toString(configs[c]));

                Scheduler scheduler = new Scheduler(configs[c], 0);
                PerformanceAnalyzer analyzer = new PerformanceAnalyzer();

                scheduler.start();

                for (int i = 0; i < burstTimes.length; i++) {
                    Task task = new Task(i + 1, burstTimes[i]);
                    scheduler.submitTask(task);
                    analyzer.addTask(task);
                }

                while (scheduler.getCompletedTaskCount() < burstTimes.length) {
                    Thread.sleep(100);
                }

                scheduler.stop();
                Thread.sleep(100);

                analyzer.printAnalysis();
            }
        }
    }

    /**
     * MLFQ의 장단점
     */
    static class ProsCons {
        public void demonstrate() {
            System.out.println("\n  === MLFQ Scheduler Analysis ===");

            System.out.println("\n  ✓ Advantages:");
            System.out.println("    - Adaptive (learns task behavior)");
            System.out.println("    - Good response time for I/O-bound tasks");
            System.out.println("    - No starvation (with priority boost)");
            System.out.println("    - Separates CPU-bound and I/O-bound automatically");
            System.out.println("    - Fair to all types of tasks");
            System.out.println("    - No need to know burst time in advance");

            System.out.println("\n  ✗ Disadvantages:");
            System.out.println("    - Complex to implement");
            System.out.println("    - Many parameters to tune");
            System.out.println("    - Can be gamed by malicious processes");
            System.out.println("    - Higher overhead than simple schedulers");
            System.out.println("    - Priority boost can cause delays");

            System.out.println("\n  Key Parameters:");
            System.out.println("    - Number of queues");
            System.out.println("    - Time quantum for each queue");
            System.out.println("    - Priority boost interval");
            System.out.println("    - Demotion policy");

            System.out.println("\n  Best Use Cases:");
            System.out.println("    - General-purpose operating systems");
            System.out.println("    - Time-sharing systems");
            System.out.println("    - Mixed workloads (CPU + I/O)");
            System.out.println("    - Interactive systems");

            System.out.println("\n  Real-world Usage:");
            System.out.println("    - BSD Unix variants");
            System.out.println("    - Windows (partially)");
            System.out.println("    - Modern Linux schedulers (influenced by MLFQ)");
        }
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== MLFQ Scheduler Demo ===\n");

        // 1. 기본 MLFQ 동작
        System.out.println("1. Basic MLFQ Operation");
        demonstrateBasicMLFQ();
        System.out.println();

        // 2. CPU vs I/O 작업 구분
        System.out.println("2. Workload Separation (CPU-bound vs I/O-bound)");
        new WorkloadSeparationDemo().demonstrate();
        System.out.println();

        // 3. Priority Boost
        System.out.println("3. Priority Boost Demo");
        new PriorityBoostDemo().demonstrate();
        System.out.println();

        // 4. Quantum 설정 비교
        System.out.println("4. Quantum Configuration Comparison");
        new QuantumConfigComparison().demonstrate();
        System.out.println();

        // 5. 장단점 분석
        System.out.println("5. Pros and Cons Analysis");
        new ProsCons().demonstrate();
        System.out.println();

        System.out.println("=== Demo Completed ===");
    }

    private static void demonstrateBasicMLFQ() throws InterruptedException {
        int[] quantums = {50, 100, 200};
        Scheduler scheduler = new Scheduler(quantums, 1000);
        PerformanceAnalyzer analyzer = new PerformanceAnalyzer();

        scheduler.start();

        // 다양한 작업들
        int[] burstTimes = {150, 80, 300, 40, 200};

        for (int i = 0; i < burstTimes.length; i++) {
            Task task = new Task(i + 1, burstTimes[i]);
            scheduler.submitTask(task);
            analyzer.addTask(task);
        }

        while (scheduler.getCompletedTaskCount() < burstTimes.length) {
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