package com.study.thread.scheduling;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * FCFS (First-Come First-Served) 스케줄러 구현
 *
 * 가장 단순한 스케줄링 알고리즘:
 * - 먼저 도착한 작업을 먼저 처리
 * - FIFO (First In First Out) 큐 사용
 * - 비선점형 (Non-preemptive)
 *
 * 특징:
 * - 구현이 간단
 * - 공정함 (모든 작업이 순서대로 처리)
 * - Convoy Effect 발생 가능 (긴 작업이 짧은 작업을 블로킹)
 */
public class FCFSScheduler {

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
     * FCFS 스케줄러 구현
     */
    static class Scheduler {
        private final Queue<Task> readyQueue = new LinkedList<>();
        private volatile boolean running = false;
        private Thread schedulerThread;
        private final AtomicInteger completedTasks = new AtomicInteger(0);

        public void start() {
            if (running) return;

            running = true;
            schedulerThread = new Thread(this::schedule, "FCFS-Scheduler");
            schedulerThread.start();

            System.out.println("  [Scheduler] Started");
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
                        task = readyQueue.poll();
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

            // 각 작업의 통계
            System.out.println("\n  Task Details:");
            System.out.println("  ID | Arrival | Start | Completion | Burst | Wait | Turnaround");
            System.out.println("  ---|---------|-------|------------|-------|------|------------");

            long baseTime = tasks.get(0).arrivalTime;
            for (Task task : tasks) {
                System.out.printf("  %2d | %7d | %5d | %10d | %5d | %4d | %10d%n",
                        task.id,
                        task.arrivalTime - baseTime,
                        task.startTime - baseTime,
                        task.completionTime - baseTime,
                        task.burstTime,
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

            // CPU 이용률 계산
            long totalBurstTime = tasks.stream().mapToInt(t -> t.burstTime).sum();
            long totalTime = tasks.get(tasks.size() - 1).completionTime - tasks.get(0).arrivalTime;
            double cpuUtilization = (totalBurstTime * 100.0) / totalTime;

            System.out.println("    CPU utilization: " + String.format("%.2f%%", cpuUtilization));
        }
    }

    /**
     * Convoy Effect 시연
     */
    static class ConvoyEffectDemo {
        public void demonstrate() throws InterruptedException {
            System.out.println("  Demonstrating Convoy Effect");
            System.out.println("  (Long task followed by short tasks)");

            Scheduler scheduler = new Scheduler();
            PerformanceAnalyzer analyzer = new PerformanceAnalyzer();

            scheduler.start();

            // 긴 작업 1개
            Task longTask = new Task(1, 1000);
            scheduler.submitTask(longTask);
            analyzer.addTask(longTask);

            Thread.sleep(50);

            // 짧은 작업 여러개
            for (int i = 2; i <= 5; i++) {
                Task shortTask = new Task(i, 100);
                scheduler.submitTask(shortTask);
                analyzer.addTask(shortTask);
                Thread.sleep(50);
            }

            // 모든 작업 완료 대기
            while (scheduler.getCompletedTaskCount() < 5) {
                Thread.sleep(100);
            }

            scheduler.stop();

            analyzer.printAnalysis();

            System.out.println("\n  Note: Short tasks had to wait for the long task (Convoy Effect)");
        }
    }

    /**
     * 균일한 작업 부하 테스트
     */
    static class UniformWorkloadTest {
        public void demonstrate() throws InterruptedException {
            System.out.println("  Testing with uniform workload");

            Scheduler scheduler = new Scheduler();
            PerformanceAnalyzer analyzer = new PerformanceAnalyzer();

            scheduler.start();

            // 동일한 실행 시간을 가진 작업들
            for (int i = 1; i <= 5; i++) {
                Task task = new Task(i, 200);
                scheduler.submitTask(task);
                analyzer.addTask(task);
                Thread.sleep(100);
            }

            while (scheduler.getCompletedTaskCount() < 5) {
                Thread.sleep(100);
            }

            scheduler.stop();

            analyzer.printAnalysis();
        }
    }

    /**
     * 동시 도착 테스트
     */
    static class SimultaneousArrivalTest {
        public void demonstrate() throws InterruptedException {
            System.out.println("  Testing with simultaneous arrivals");

            Scheduler scheduler = new Scheduler();
            PerformanceAnalyzer analyzer = new PerformanceAnalyzer();

            scheduler.start();

            // 모든 작업이 거의 동시에 도착
            for (int i = 1; i <= 5; i++) {
                Task task = new Task(i, 100 + (i * 50));
                scheduler.submitTask(task);
                analyzer.addTask(task);
            }

            while (scheduler.getCompletedTaskCount() < 5) {
                Thread.sleep(100);
            }

            scheduler.stop();

            analyzer.printAnalysis();
        }
    }

    /**
     * FCFS의 장단점 분석
     */
    static class ProsCons {
        public void demonstrate() {
            System.out.println("\n  === FCFS Scheduler Analysis ===");

            System.out.println("\n  ✓ Advantages:");
            System.out.println("    - Simple to implement");
            System.out.println("    - Fair (FIFO order)");
            System.out.println("    - No starvation");
            System.out.println("    - Low overhead");
            System.out.println("    - Predictable behavior");

            System.out.println("\n  ✗ Disadvantages:");
            System.out.println("    - Convoy Effect (long tasks block short ones)");
            System.out.println("    - High average waiting time");
            System.out.println("    - Poor for interactive systems");
            System.out.println("    - Non-preemptive (can't interrupt)");
            System.out.println("    - Not optimal for response time");

            System.out.println("\n  Best Use Cases:");
            System.out.println("    - Batch processing systems");
            System.out.println("    - Background task processing");
            System.out.println("    - Simple task queues");
            System.out.println("    - When fairness is critical");

            System.out.println("\n  Worst Use Cases:");
            System.out.println("    - Interactive systems");
            System.out.println("    - Real-time systems");
            System.out.println("    - Mixed workload (long + short tasks)");
            System.out.println("    - Systems requiring responsiveness");
        }
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== FCFS Scheduler Demo ===\n");

        // 1. 기본 FCFS 동작
        System.out.println("1. Basic FCFS Operation");
        demonstrateBasicFCFS();
        System.out.println();

        // 2. Convoy Effect
        System.out.println("2. Convoy Effect");
        new ConvoyEffectDemo().demonstrate();
        System.out.println();

        // 3. 균일한 작업 부하
        System.out.println("3. Uniform Workload");
        new UniformWorkloadTest().demonstrate();
        System.out.println();

        // 4. 동시 도착
        System.out.println("4. Simultaneous Arrival");
        new SimultaneousArrivalTest().demonstrate();
        System.out.println();

        // 5. 장단점 분석
        System.out.println("5. Pros and Cons Analysis");
        new ProsCons().demonstrate();
        System.out.println();

        System.out.println("=== Demo Completed ===");
    }

    private static void demonstrateBasicFCFS() throws InterruptedException {
        Scheduler scheduler = new Scheduler();
        PerformanceAnalyzer analyzer = new PerformanceAnalyzer();

        scheduler.start();

        // 다양한 실행 시간을 가진 작업들
        int[] burstTimes = {300, 150, 400, 100, 200};

        for (int i = 0; i < burstTimes.length; i++) {
            Task task = new Task(i + 1, burstTimes[i]);
            scheduler.submitTask(task);
            analyzer.addTask(task);
            Thread.sleep(50);
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