package com.study.thread.scheduling;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Round Robin 스케줄러 구현
 *
 * 각 작업에 동일한 시간 할당량(time quantum)을 부여하는 선점형 알고리즘:
 * - 선점형 (Preemptive)
 * - 순환 큐(Circular Queue) 사용
 * - 모든 작업에 공평한 CPU 시간 할당
 *
 * 특징:
 * - 공정함 (모든 작업이 차례대로 실행)
 * - 기아 현상 없음
 * - 대화형 시스템에 적합
 * - Time quantum 크기가 성능에 큰 영향
 */
public class RoundRobinScheduler {

    /**
     * 스케줄링할 작업
     */
    static class Task {
        final int id;
        int remainingTime; // 남은 실행 시간 (ms)
        final int originalBurstTime; // 원래 실행 시간
        final long arrivalTime; // 도착 시간
        long firstStartTime = -1; // 첫 시작 시간
        long lastExecutionTime = -1; // 마지막 실행 시간
        long completionTime = -1; // 완료 시간
        int executionCount = 0; // 실행 횟수

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
            return String.format("Task-%d (remaining: %dms/%dms)",
                    id, remainingTime, originalBurstTime);
        }
    }

    /**
     * Round Robin 스케줄러 구현
     */
    static class Scheduler {
        private final Queue<Task> readyQueue = new LinkedList<>();
        private final int timeQuantum; // 시간 할당량 (ms)
        private volatile boolean running = false;
        private Thread schedulerThread;
        private final AtomicInteger completedTasks = new AtomicInteger(0);
        private final AtomicInteger contextSwitches = new AtomicInteger(0);

        public Scheduler(int timeQuantum) {
            this.timeQuantum = timeQuantum;
        }

        public void start() {
            if (running) return;

            running = true;
            schedulerThread = new Thread(this::schedule, "RR-Scheduler");
            schedulerThread.start();

            System.out.println("  [Scheduler] Started (Round Robin, quantum: " +
                    timeQuantum + "ms)");
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

        private void executeTask(Task task) {
            long startTime = System.currentTimeMillis();

            if (task.firstStartTime == -1) {
                task.firstStartTime = startTime;
            }

            task.executionCount++;

            // 실행할 시간 결정 (timeQuantum 또는 남은 시간 중 작은 값)
            int executeTime = Math.min(timeQuantum, task.remainingTime);

            System.out.println("  [Scheduler] Executing: " + task +
                    " for " + executeTime + "ms (execution #" +
                    task.executionCount + ")");

            // 작업 실행 시뮬레이션
            sleep(executeTime);

            task.remainingTime -= executeTime;
            task.lastExecutionTime = System.currentTimeMillis();

            if (task.isCompleted()) {
                task.completionTime = task.lastExecutionTime;
                completedTasks.incrementAndGet();
                System.out.println("  [Scheduler] Completed: Task-" + task.id +
                        " (turnaround: " + task.getTurnaroundTime() +
                        "ms, executions: " + task.executionCount + ")");
            } else {
                // 아직 완료되지 않았으면 큐의 뒤로
                synchronized (this) {
                    readyQueue.offer(task);
                    contextSwitches.incrementAndGet();
                }
                System.out.println("  [Scheduler] Preempted: " + task +
                        " (context switch #" + contextSwitches.get() + ")");
            }
        }

        public int getCompletedTaskCount() {
            return completedTasks.get();
        }

        public int getContextSwitches() {
            return contextSwitches.get();
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
        private final int timeQuantum;
        private int contextSwitches;

        public PerformanceAnalyzer(int timeQuantum) {
            this.timeQuantum = timeQuantum;
        }

        public void addTask(Task task) {
            tasks.add(task);
        }

        public void setContextSwitches(int count) {
            this.contextSwitches = count;
        }

        public void printAnalysis() {
            if (tasks.isEmpty()) {
                System.out.println("  No tasks to analyze");
                return;
            }

            System.out.println("\n  === Performance Analysis ===");
            System.out.println("  Time Quantum: " + timeQuantum + "ms");
            System.out.println("  Context Switches: " + contextSwitches);

            // 각 작업의 통계
            System.out.println("\n  Task Details:");
            System.out.println("  ID | Burst | Arrival | First Start | Completion | Response | Wait | Turnaround | Execs");
            System.out.println("  ---|-------|---------|-------------|------------|----------|------|------------|-------");

            long baseTime = tasks.stream()
                    .mapToLong(t -> t.arrivalTime)
                    .min()
                    .orElse(0);

            for (Task task : tasks) {
                System.out.printf("  %2d | %5d | %7d | %11d | %10d | %8d | %4d | %10d | %5d%n",
                        task.id,
                        task.originalBurstTime,
                        task.arrivalTime - baseTime,
                        task.firstStartTime - baseTime,
                        task.completionTime - baseTime,
                        task.getResponseTime(),
                        task.getWaitingTime(),
                        task.getTurnaroundTime(),
                        task.executionCount);
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

            double avgExecutions = tasks.stream()
                    .mapToInt(t -> t.executionCount)
                    .average()
                    .orElse(0);

            System.out.println("\n  Summary:");
            System.out.println("    Total tasks: " + tasks.size());
            System.out.println("    Avg response time: " + String.format("%.2f ms", avgResponseTime));
            System.out.println("    Avg waiting time: " + String.format("%.2f ms", avgWaitingTime));
            System.out.println("    Avg turnaround time: " + String.format("%.2f ms", avgTurnaroundTime));
            System.out.println("    Avg executions per task: " + String.format("%.2f", avgExecutions));
            System.out.println("    Context switch overhead: " + contextSwitches + " switches");
        }
    }

    /**
     * Time Quantum 크기 비교
     */
    static class QuantumSizeComparison {
        public void demonstrate() throws InterruptedException {
            int[] quantumSizes = {50, 100, 200, 500};
            int[] burstTimes = {300, 200, 400, 150, 250};

            System.out.println("  Comparing different time quantum sizes");
            System.out.println("  Tasks: " + java.util.Arrays.toString(burstTimes));

            for (int quantum : quantumSizes) {
                System.out.println("\n  === Time Quantum: " + quantum + "ms ===");

                Scheduler scheduler = new Scheduler(quantum);
                PerformanceAnalyzer analyzer = new PerformanceAnalyzer(quantum);

                scheduler.start();

                for (int i = 0; i < burstTimes.length; i++) {
                    Task task = new Task(i + 1, burstTimes[i]);
                    scheduler.submitTask(task);
                    analyzer.addTask(task);
                }

                while (scheduler.getCompletedTaskCount() < burstTimes.length) {
                    Thread.sleep(100);
                }

                analyzer.setContextSwitches(scheduler.getContextSwitches());
                scheduler.stop();
                Thread.sleep(100);

                analyzer.printAnalysis();
            }

            System.out.println("\n  Key Observation:");
            System.out.println("    - Small quantum: More responsive, but more context switches");
            System.out.println("    - Large quantum: Fewer switches, but less responsive");
        }
    }

    /**
     * 대화형 시스템 시뮬레이션
     */
    static class InteractiveSystemSimulation {
        public void demonstrate() throws InterruptedException {
            System.out.println("  Simulating interactive system");
            System.out.println("  (Multiple short tasks requiring quick response)");

            Scheduler scheduler = new Scheduler(50); // 작은 quantum으로 빠른 응답
            PerformanceAnalyzer analyzer = new PerformanceAnalyzer(50);

            scheduler.start();

            // 짧은 대화형 작업들
            for (int i = 1; i <= 8; i++) {
                Task task = new Task(i, 100 + (i * 20));
                scheduler.submitTask(task);
                analyzer.addTask(task);
                Thread.sleep(50); // 작업이 계속 들어옴
            }

            while (scheduler.getCompletedTaskCount() < 8) {
                Thread.sleep(100);
            }

            analyzer.setContextSwitches(scheduler.getContextSwitches());
            scheduler.stop();

            analyzer.printAnalysis();

            System.out.println("\n  Note: All tasks got CPU time quickly (good response time)");
        }
    }

    /**
     * 긴 작업 vs 짧은 작업
     */
    static class MixedWorkloadTest {
        public void demonstrate() throws InterruptedException {
            System.out.println("  Testing with mixed workload (long + short tasks)");

            Scheduler scheduler = new Scheduler(100);
            PerformanceAnalyzer analyzer = new PerformanceAnalyzer(100);

            scheduler.start();

            // 긴 작업
            Task longTask1 = new Task(1, 800);
            scheduler.submitTask(longTask1);
            analyzer.addTask(longTask1);

            // 짧은 작업들
            Task shortTask1 = new Task(2, 100);
            Task shortTask2 = new Task(3, 150);
            Task shortTask3 = new Task(4, 120);

            scheduler.submitTask(shortTask1);
            scheduler.submitTask(shortTask2);
            scheduler.submitTask(shortTask3);

            analyzer.addTask(shortTask1);
            analyzer.addTask(shortTask2);
            analyzer.addTask(shortTask3);

            // 또 다른 긴 작업
            Task longTask2 = new Task(5, 700);
            scheduler.submitTask(longTask2);
            analyzer.addTask(longTask2);

            while (scheduler.getCompletedTaskCount() < 5) {
                Thread.sleep(100);
            }

            analyzer.setContextSwitches(scheduler.getContextSwitches());
            scheduler.stop();

            analyzer.printAnalysis();

            System.out.println("\n  Note: Short tasks don't have to wait for long tasks to complete");
        }
    }

    /**
     * RR의 장단점 분석
     */
    static class ProsCons {
        public void demonstrate() {
            System.out.println("\n  === Round Robin Scheduler Analysis ===");

            System.out.println("\n  ✓ Advantages:");
            System.out.println("    - Fair to all processes");
            System.out.println("    - No starvation");
            System.out.println("    - Good response time");
            System.out.println("    - Suitable for time-sharing systems");
            System.out.println("    - Predictable behavior");
            System.out.println("    - Good for interactive systems");

            System.out.println("\n  ✗ Disadvantages:");
            System.out.println("    - Context switch overhead");
            System.out.println("    - Performance depends on quantum size");
            System.out.println("    - Higher average turnaround time than SJF");
            System.out.println("    - Not optimal for CPU utilization");
            System.out.println("    - May cause thrashing if quantum too small");

            System.out.println("\n  Time Quantum Selection:");
            System.out.println("    - Too small: Excessive context switches");
            System.out.println("    - Too large: Poor response time (like FCFS)");
            System.out.println("    - Rule of thumb: 80% of bursts < quantum");
            System.out.println("    - Typical range: 10-100ms");

            System.out.println("\n  Best Use Cases:");
            System.out.println("    - Time-sharing systems");
            System.out.println("    - Interactive applications");
            System.out.println("    - Multi-user systems");
            System.out.println("    - Fair resource allocation needed");

            System.out.println("\n  Worst Use Cases:");
            System.out.println("    - Real-time systems");
            System.out.println("    - Batch processing");
            System.out.println("    - CPU-intensive workloads");
            System.out.println("    - When context switch cost is high");
        }
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Round Robin Scheduler Demo ===\n");

        // 1. 기본 Round Robin 동작
        System.out.println("1. Basic Round Robin Operation");
        demonstrateBasicRR();
        System.out.println();

        // 2. Quantum 크기 비교
        System.out.println("2. Time Quantum Size Comparison");
        new QuantumSizeComparison().demonstrate();
        System.out.println();

        // 3. 대화형 시스템
        System.out.println("3. Interactive System Simulation");
        new InteractiveSystemSimulation().demonstrate();
        System.out.println();

        // 4. 혼합 작업 부하
        System.out.println("4. Mixed Workload Test");
        new MixedWorkloadTest().demonstrate();
        System.out.println();

        // 5. 장단점 분석
        System.out.println("5. Pros and Cons Analysis");
        new ProsCons().demonstrate();
        System.out.println();

        System.out.println("=== Demo Completed ===");
    }

    private static void demonstrateBasicRR() throws InterruptedException {
        Scheduler scheduler = new Scheduler(100);
        PerformanceAnalyzer analyzer = new PerformanceAnalyzer(100);

        scheduler.start();

        int[] burstTimes = {300, 200, 400, 150};

        System.out.println("  Submitting tasks: " + java.util.Arrays.toString(burstTimes));

        for (int i = 0; i < burstTimes.length; i++) {
            Task task = new Task(i + 1, burstTimes[i]);
            scheduler.submitTask(task);
            analyzer.addTask(task);
        }

        while (scheduler.getCompletedTaskCount() < burstTimes.length) {
            Thread.sleep(100);
        }

        analyzer.setContextSwitches(scheduler.getContextSwitches());
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