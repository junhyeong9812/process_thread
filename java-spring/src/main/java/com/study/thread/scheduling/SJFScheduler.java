package com.study.thread.scheduling;

import java.util.PriorityQueue;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * SJF (Shortest Job First) 스케줄러 구현
 *
 * 실행 시간이 가장 짧은 작업을 먼저 처리하는 알고리즘:
 * - 평균 대기 시간이 최소화됨 (최적)
 * - 비선점형 (Non-preemptive) 버전
 * - Priority Queue 사용
 *
 * 특징:
 * - 평균 대기 시간이 가장 짧음
 * - 긴 작업은 기아(Starvation) 발생 가능
 * - 실행 시간을 미리 알아야 함
 */
public class SJFScheduler {

    /**
     * 스케줄링할 작업
     */
    static class Task {
        final int id;
        final int burstTime; // 실행 시간 (ms)
        final long arrivalTime; // 도착 시간
        long startTime = -1; // 시작 시간
        long completionTime = -1; // 완료 시간

        public Task(int id, int burstTime) {
            this.id = id;
            this.burstTime = burstTime;
            this.arrivalTime = System.currentTimeMillis();
        }

        public int getWaitingTime() {
            if (startTime == -1) return 0;
            return (int) (startTime - arrivalTime);
        }

        public int getTurnaroundTime() {
            if (completionTime == -1) return 0;
            return (int) (completionTime - arrivalTime);
        }

        @Override
        public String toString() {
            return String.format("Task-%d (burst: %dms)", id, burstTime);
        }
    }

    /**
     * SJF 스케줄러 구현
     */
    static class Scheduler {
        // burstTime이 짧은 순서대로 정렬
        private final PriorityQueue<Task> readyQueue = new PriorityQueue<>(
                Comparator.comparingInt(t -> t.burstTime)
        );
        private volatile boolean running = false;
        private Thread schedulerThread;
        private final AtomicInteger completedTasks = new AtomicInteger(0);

        public void start() {
            if (running) return;

            running = true;
            schedulerThread = new Thread(this::schedule, "SJF-Scheduler");
            schedulerThread.start();

            System.out.println("  [Scheduler] Started (SJF - Shortest Job First)");
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
            System.out.println("  [Scheduler] Task submitted: " + task +
                    " (queue size: " + readyQueue.size() + ")");
            notifyAll(); // 대기 중인 스케줄러 깨우기
        }

        private void schedule() {
            while (running) {
                Task task = null;

                synchronized (this) {
                    while (readyQueue.isEmpty() && running) {
                        try {
                            wait(100); // 작업이 들어올 때까지 대기
                        } catch (InterruptedException e) {
                            if (!running) break;
                        }
                    }

                    if (!readyQueue.isEmpty()) {
                        task = readyQueue.poll(); // 가장 짧은 작업 선택
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

        public int getQueueSize() {
            synchronized (this) {
                return readyQueue.size();
            }
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

            // 완료 시간 순으로 정렬
            tasks.sort(Comparator.comparingLong(t -> t.completionTime));

            // 각 작업의 통계
            System.out.println("\n  Task Details (execution order):");
            System.out.println("  ID | Burst | Arrival | Start | Completion | Wait | Turnaround");
            System.out.println("  ---|-------|---------|-------|------------|------|------------");

            long baseTime = tasks.stream()
                    .mapToLong(t -> t.arrivalTime)
                    .min()
                    .orElse(0);

            for (Task task : tasks) {
                System.out.printf("  %2d | %5d | %7d | %5d | %10d | %4d | %10d%n",
                        task.id,
                        task.burstTime,
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

            double avgBurstTime = tasks.stream()
                    .mapToInt(t -> t.burstTime)
                    .average()
                    .orElse(0);

            System.out.println("\n  Summary:");
            System.out.println("    Total tasks: " + tasks.size());
            System.out.println("    Avg burst time: " + String.format("%.2f ms", avgBurstTime));
            System.out.println("    Avg waiting time: " + String.format("%.2f ms", avgWaitingTime));
            System.out.println("    Avg turnaround time: " + String.format("%.2f ms", avgTurnaroundTime));

            // CPU 이용률
            long totalBurstTime = tasks.stream().mapToInt(t -> t.burstTime).sum();
            Task lastTask = tasks.stream()
                    .max(Comparator.comparingLong(t -> t.completionTime))
                    .orElse(null);
            Task firstTask = tasks.stream()
                    .min(Comparator.comparingLong(t -> t.arrivalTime))
                    .orElse(null);

            if (lastTask != null && firstTask != null) {
                long totalTime = lastTask.completionTime - firstTask.arrivalTime;
                double cpuUtilization = (totalBurstTime * 100.0) / totalTime;
                System.out.println("    CPU utilization: " + String.format("%.2f%%", cpuUtilization));
            }
        }
    }

    /**
     * SJF vs FCFS 비교
     */
    static class SJFvsFCFSComparison {
        public void demonstrate() throws InterruptedException {
            System.out.println("  Comparing SJF vs FCFS with same tasks");

            int[] burstTimes = {400, 100, 200, 500, 150};

            // SJF 테스트
            System.out.println("\n  === SJF Scheduling ===");
            Scheduler sjfScheduler = new Scheduler();
            PerformanceAnalyzer sjfAnalyzer = new PerformanceAnalyzer();

            sjfScheduler.start();

            for (int i = 0; i < burstTimes.length; i++) {
                Task task = new Task(i + 1, burstTimes[i]);
                sjfScheduler.submitTask(task);
                sjfAnalyzer.addTask(task);
            }

            while (sjfScheduler.getCompletedTaskCount() < burstTimes.length) {
                Thread.sleep(100);
            }

            sjfScheduler.stop();
            Thread.sleep(100);

            sjfAnalyzer.printAnalysis();

            System.out.println("\n  Note: Tasks executed in order of shortest burst time");
        }
    }

    /**
     * 기아(Starvation) 시연
     */
    static class StarvationDemo {
        public void demonstrate() throws InterruptedException {
            System.out.println("  Demonstrating potential starvation");
            System.out.println("  (Long task waiting while short tasks keep arriving)");

            Scheduler scheduler = new Scheduler();
            PerformanceAnalyzer analyzer = new PerformanceAnalyzer();

            scheduler.start();

            // 긴 작업 제출
            Task longTask = new Task(1, 1000);
            scheduler.submitTask(longTask);
            analyzer.addTask(longTask);

            Thread.sleep(50);

            // 짧은 작업들이 계속 도착
            for (int i = 2; i <= 6; i++) {
                Task shortTask = new Task(i, 100);
                scheduler.submitTask(shortTask);
                analyzer.addTask(shortTask);
                Thread.sleep(100); // 짧은 작업이 계속 들어옴
            }

            while (scheduler.getCompletedTaskCount() < 6) {
                Thread.sleep(100);
            }

            scheduler.stop();

            analyzer.printAnalysis();

            System.out.println("\n  Note: Long task (Task-1) had to wait for all short tasks");
        }
    }

    /**
     * 최적성 증명
     */
    static class OptimalityProof {
        public void demonstrate() throws InterruptedException {
            System.out.println("  Demonstrating SJF optimality (minimum avg waiting time)");

            Scheduler scheduler = new Scheduler();
            PerformanceAnalyzer analyzer = new PerformanceAnalyzer();

            scheduler.start();

            // 모든 작업이 동시에 도착
            int[] burstTimes = {100, 200, 300, 400, 500};

            System.out.println("  Submitting tasks: " + java.util.Arrays.toString(burstTimes));

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

            System.out.println("\n  Note: Executing shortest tasks first minimizes average waiting time");
        }
    }

    /**
     * 동적 도착 시나리오
     */
    static class DynamicArrivalScenario {
        public void demonstrate() throws InterruptedException {
            System.out.println("  Testing with dynamic task arrivals");

            Scheduler scheduler = new Scheduler();
            PerformanceAnalyzer analyzer = new PerformanceAnalyzer();

            scheduler.start();

            // 작업들이 시간차를 두고 도착
            Task task1 = new Task(1, 500);
            scheduler.submitTask(task1);
            analyzer.addTask(task1);

            Thread.sleep(100);

            Task task2 = new Task(2, 100);
            scheduler.submitTask(task2);
            analyzer.addTask(task2);

            Thread.sleep(100);

            Task task3 = new Task(3, 200);
            scheduler.submitTask(task3);
            analyzer.addTask(task3);

            Thread.sleep(100);

            Task task4 = new Task(4, 150);
            scheduler.submitTask(task4);
            analyzer.addTask(task4);

            while (scheduler.getCompletedTaskCount() < 4) {
                Thread.sleep(100);
            }

            scheduler.stop();

            analyzer.printAnalysis();
        }
    }

    /**
     * SJF의 장단점 분석
     */
    static class ProsCons {
        public void demonstrate() {
            System.out.println("\n  === SJF Scheduler Analysis ===");

            System.out.println("\n  ✓ Advantages:");
            System.out.println("    - Minimum average waiting time (optimal)");
            System.out.println("    - Better than FCFS for mixed workloads");
            System.out.println("    - Good for batch systems");
            System.out.println("    - Maximizes throughput");
            System.out.println("    - Reduces average response time");

            System.out.println("\n  ✗ Disadvantages:");
            System.out.println("    - Starvation (long tasks may wait indefinitely)");
            System.out.println("    - Requires knowing burst time in advance");
            System.out.println("    - Non-preemptive version can't adapt");
            System.out.println("    - Unfair to long processes");
            System.out.println("    - Difficult to predict burst time accurately");

            System.out.println("\n  Best Use Cases:");
            System.out.println("    - Batch processing with known execution times");
            System.out.println("    - Background task queues");
            System.out.println("    - Systems where short tasks are priority");
            System.out.println("    - Minimizing average waiting time is critical");

            System.out.println("\n  Worst Use Cases:");
            System.out.println("    - Interactive systems");
            System.out.println("    - Real-time systems");
            System.out.println("    - When execution time is unpredictable");
            System.out.println("    - Systems with many long-running tasks");

            System.out.println("\n  Improvements:");
            System.out.println("    - SRTF (Shortest Remaining Time First) - preemptive version");
            System.out.println("    - Aging to prevent starvation");
            System.out.println("    - Exponential averaging to predict burst time");
        }
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== SJF Scheduler Demo ===\n");

        // 1. 기본 SJF 동작
        System.out.println("1. Basic SJF Operation");
        demonstrateBasicSJF();
        System.out.println();

        // 2. SJF vs FCFS 비교
        System.out.println("2. SJF vs FCFS Comparison");
        new SJFvsFCFSComparison().demonstrate();
        System.out.println();

        // 3. 기아 현상
        System.out.println("3. Starvation Demo");
        new StarvationDemo().demonstrate();
        System.out.println();

        // 4. 최적성 증명
        System.out.println("4. Optimality Proof");
        new OptimalityProof().demonstrate();
        System.out.println();

        // 5. 동적 도착
        System.out.println("5. Dynamic Arrival Scenario");
        new DynamicArrivalScenario().demonstrate();
        System.out.println();

        // 6. 장단점 분석
        System.out.println("6. Pros and Cons Analysis");
        new ProsCons().demonstrate();
        System.out.println();

        System.out.println("=== Demo Completed ===");
    }

    private static void demonstrateBasicSJF() throws InterruptedException {
        Scheduler scheduler = new Scheduler();
        PerformanceAnalyzer analyzer = new PerformanceAnalyzer();

        scheduler.start();

        // 다양한 실행 시간을 가진 작업들
        int[] burstTimes = {300, 100, 500, 150, 200};

        System.out.println("  Submitting tasks in order: " + java.util.Arrays.toString(burstTimes));

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

        System.out.println("\n  Note: Tasks executed in shortest-first order: 100, 150, 200, 300, 500");
    }

    private static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}