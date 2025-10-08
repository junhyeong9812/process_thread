package com.study.thread.scheduling;

import java.util.*;

/**
 * 스케줄러 성능 비교
 *
 * 모든 스케줄링 알고리즘의 성능을 비교 분석합니다:
 * - FCFS (First-Come First-Served)
 * - SJF (Shortest Job First)
 * - Round Robin
 * - Priority
 * - MLFQ (Multi-Level Feedback Queue)
 *
 * 비교 메트릭:
 * - 평균 대기 시간 (Average Waiting Time)
 * - 평균 응답 시간 (Average Response Time)
 * - 평균 반환 시간 (Average Turnaround Time)
 * - CPU 이용률 (CPU Utilization)
 * - 처리량 (Throughput)
 */
public class SchedulerComparison {

    /**
     * 시뮬레이션용 작업
     */
    static class Task implements Cloneable {
        int id;
        int burstTime;
        int remainingTime;
        int priority;
        long arrivalTime;
        long startTime = -1;
        long firstStartTime = -1;
        long completionTime = -1;
        int executionCount = 0;

        public Task(int id, int burstTime, int priority) {
            this.id = id;
            this.burstTime = burstTime;
            this.remainingTime = burstTime;
            this.priority = priority;
            this.arrivalTime = 0; // 시뮬레이션에서는 상대 시간 사용
        }

        @Override
        protected Task clone() {
            try {
                Task cloned = (Task) super.clone();
                cloned.remainingTime = cloned.burstTime;
                cloned.startTime = -1;
                cloned.firstStartTime = -1;
                cloned.completionTime = -1;
                cloned.executionCount = 0;
                return cloned;
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
        }

        public int getWaitingTime() {
            if (completionTime == -1) return 0;
            return (int) (completionTime - arrivalTime - burstTime);
        }

        public int getResponseTime() {
            if (firstStartTime == -1) return 0;
            return (int) (firstStartTime - arrivalTime);
        }

        public int getTurnaroundTime() {
            if (completionTime == -1) return 0;
            return (int) (completionTime - arrivalTime);
        }
    }

    /**
     * 성능 메트릭
     */
    static class Metrics {
        final String schedulerName;
        final double avgWaitingTime;
        final double avgResponseTime;
        final double avgTurnaroundTime;
        final double cpuUtilization;
        final double throughput;
        final int contextSwitches;

        public Metrics(String name, List<Task> tasks, long totalTime, int contextSwitches) {
            this.schedulerName = name;
            this.avgWaitingTime = tasks.stream()
                    .mapToInt(Task::getWaitingTime)
                    .average()
                    .orElse(0);
            this.avgResponseTime = tasks.stream()
                    .mapToInt(Task::getResponseTime)
                    .average()
                    .orElse(0);
            this.avgTurnaroundTime = tasks.stream()
                    .mapToInt(Task::getTurnaroundTime)
                    .average()
                    .orElse(0);

            long totalBurstTime = tasks.stream()
                    .mapToInt(t -> t.burstTime)
                    .sum();
            this.cpuUtilization = (totalBurstTime * 100.0) / totalTime;
            this.throughput = (tasks.size() * 1000.0) / totalTime;
            this.contextSwitches = contextSwitches;
        }

        public void print() {
            System.out.println("\n  " + schedulerName + ":");
            System.out.println("    Avg Waiting Time:    " + String.format("%.2f ms", avgWaitingTime));
            System.out.println("    Avg Response Time:   " + String.format("%.2f ms", avgResponseTime));
            System.out.println("    Avg Turnaround Time: " + String.format("%.2f ms", avgTurnaroundTime));
            System.out.println("    CPU Utilization:     " + String.format("%.2f%%", cpuUtilization));
            System.out.println("    Throughput:          " + String.format("%.2f tasks/sec", throughput));
            System.out.println("    Context Switches:    " + contextSwitches);
        }
    }

    /**
     * FCFS 시뮬레이터
     */
    static class FCFSSimulator {
        public Metrics simulate(List<Task> tasks) {
            List<Task> tasksCopy = cloneTasks(tasks);
            long currentTime = 0;

            for (Task task : tasksCopy) {
                task.firstStartTime = currentTime;
                task.startTime = currentTime;
                currentTime += task.burstTime;
                task.completionTime = currentTime;
            }

            return new Metrics("FCFS", tasksCopy, currentTime, 0);
        }
    }

    /**
     * SJF 시뮬레이터
     */
    static class SJFSimulator {
        public Metrics simulate(List<Task> tasks) {
            List<Task> tasksCopy = cloneTasks(tasks);
            tasksCopy.sort(Comparator.comparingInt(t -> t.burstTime));

            long currentTime = 0;

            for (Task task : tasksCopy) {
                task.firstStartTime = currentTime;
                task.startTime = currentTime;
                currentTime += task.burstTime;
                task.completionTime = currentTime;
            }

            return new Metrics("SJF", tasksCopy, currentTime, 0);
        }
    }

    /**
     * Round Robin 시뮬레이터
     */
    static class RoundRobinSimulator {
        private final int timeQuantum;

        public RoundRobinSimulator(int timeQuantum) {
            this.timeQuantum = timeQuantum;
        }

        public Metrics simulate(List<Task> tasks) {
            List<Task> tasksCopy = cloneTasks(tasks);
            Queue<Task> queue = new LinkedList<>(tasksCopy);
            long currentTime = 0;
            int contextSwitches = 0;

            while (!queue.isEmpty()) {
                Task task = queue.poll();

                if (task.firstStartTime == -1) {
                    task.firstStartTime = currentTime;
                }

                int executeTime = Math.min(timeQuantum, task.remainingTime);
                currentTime += executeTime;
                task.remainingTime -= executeTime;
                task.executionCount++;

                if (task.remainingTime > 0) {
                    queue.offer(task);
                    contextSwitches++;
                } else {
                    task.completionTime = currentTime;
                }
            }

            return new Metrics("Round Robin (Q=" + timeQuantum + ")",
                    tasksCopy, currentTime, contextSwitches);
        }
    }

    /**
     * Priority 시뮬레이터
     */
    static class PrioritySimulator {
        public Metrics simulate(List<Task> tasks) {
            List<Task> tasksCopy = cloneTasks(tasks);
            tasksCopy.sort(Comparator.comparingInt(t -> t.priority));

            long currentTime = 0;

            for (Task task : tasksCopy) {
                task.firstStartTime = currentTime;
                task.startTime = currentTime;
                currentTime += task.burstTime;
                task.completionTime = currentTime;
            }

            return new Metrics("Priority", tasksCopy, currentTime, 0);
        }
    }

    /**
     * MLFQ 시뮬레이터 (단순화)
     */
    static class MLFQSimulator {
        private final int[] timeQuantums;

        public MLFQSimulator(int[] timeQuantums) {
            this.timeQuantums = timeQuantums;
        }

        public Metrics simulate(List<Task> tasks) {
            List<Task> tasksCopy = cloneTasks(tasks);
            List<Queue<Task>> queues = new ArrayList<>();

            for (int i = 0; i < timeQuantums.length; i++) {
                queues.add(new LinkedList<>());
            }

            // 모든 작업을 최상위 큐에
            queues.get(0).addAll(tasksCopy);

            long currentTime = 0;
            int contextSwitches = 0;

            while (true) {
                Task task = null;
                int queueLevel = -1;

                // 높은 우선순위 큐부터 확인
                for (int i = 0; i < queues.size(); i++) {
                    if (!queues.get(i).isEmpty()) {
                        task = queues.get(i).poll();
                        queueLevel = i;
                        break;
                    }
                }

                if (task == null) break;

                if (task.firstStartTime == -1) {
                    task.firstStartTime = currentTime;
                }

                int quantum = timeQuantums[queueLevel];
                int executeTime = Math.min(quantum, task.remainingTime);
                currentTime += executeTime;
                task.remainingTime -= executeTime;
                task.executionCount++;

                if (task.remainingTime > 0) {
                    int newQueue = Math.min(queueLevel + 1, timeQuantums.length - 1);
                    queues.get(newQueue).offer(task);
                    contextSwitches++;
                } else {
                    task.completionTime = currentTime;
                }
            }

            return new Metrics("MLFQ", tasksCopy, currentTime, contextSwitches);
        }
    }

    /**
     * 비교 테스트
     */
    static class ComparisonTest {
        public void runComparison(String testName, List<Task> tasks) {
            System.out.println("=== " + testName + " ===");

            // 작업 정보 출력
            System.out.println("\nTasks:");
            System.out.println("  ID | Burst | Priority");
            System.out.println("  ---|-------|----------");
            for (Task task : tasks) {
                System.out.printf("  %2d | %5d | %8d%n",
                        task.id, task.burstTime, task.priority);
            }

            List<Metrics> allMetrics = new ArrayList<>();

            // 각 스케줄러로 시뮬레이션
            allMetrics.add(new FCFSSimulator().simulate(tasks));
            allMetrics.add(new SJFSimulator().simulate(tasks));
            allMetrics.add(new RoundRobinSimulator(50).simulate(tasks));
            allMetrics.add(new RoundRobinSimulator(100).simulate(tasks));
            allMetrics.add(new PrioritySimulator().simulate(tasks));
            allMetrics.add(new MLFQSimulator(new int[]{50, 100, 200}).simulate(tasks));

            // 결과 출력
            System.out.println("\n=== Results ===");
            for (Metrics metrics : allMetrics) {
                metrics.print();
            }

            // 최적 스케줄러 찾기
            System.out.println("\n=== Best Performers ===");

            Metrics bestWait = allMetrics.stream()
                    .min(Comparator.comparingDouble(m -> m.avgWaitingTime))
                    .orElse(null);
            System.out.println("  Best Avg Waiting Time:    " + bestWait.schedulerName +
                    " (" + String.format("%.2f ms", bestWait.avgWaitingTime) + ")");

            Metrics bestResponse = allMetrics.stream()
                    .min(Comparator.comparingDouble(m -> m.avgResponseTime))
                    .orElse(null);
            System.out.println("  Best Avg Response Time:   " + bestResponse.schedulerName +
                    " (" + String.format("%.2f ms", bestResponse.avgResponseTime) + ")");

            Metrics bestTurnaround = allMetrics.stream()
                    .min(Comparator.comparingDouble(m -> m.avgTurnaroundTime))
                    .orElse(null);
            System.out.println("  Best Avg Turnaround Time: " + bestTurnaround.schedulerName +
                    " (" + String.format("%.2f ms", bestTurnaround.avgTurnaroundTime) + ")");

            Metrics fewestSwitches = allMetrics.stream()
                    .min(Comparator.comparingInt(m -> m.contextSwitches))
                    .orElse(null);
            System.out.println("  Fewest Context Switches:  " + fewestSwitches.schedulerName +
                    " (" + fewestSwitches.contextSwitches + ")");
        }
    }

    /**
     * 시나리오 1: 균일한 작업 부하
     */
    static class UniformWorkload {
        public List<Task> generateTasks() {
            List<Task> tasks = new ArrayList<>();
            for (int i = 1; i <= 5; i++) {
                tasks.add(new Task(i, 100, 5));
            }
            return tasks;
        }
    }

    /**
     * 시나리오 2: 혼합 작업 부하 (짧은 + 긴)
     */
    static class MixedWorkload {
        public List<Task> generateTasks() {
            List<Task> tasks = new ArrayList<>();
            tasks.add(new Task(1, 300, 3));
            tasks.add(new Task(2, 50, 5));
            tasks.add(new Task(3, 200, 4));
            tasks.add(new Task(4, 100, 6));
            tasks.add(new Task(5, 400, 2));
            return tasks;
        }
    }

    /**
     * 시나리오 3: CPU 집약적 작업들
     */
    static class CPUIntensiveWorkload {
        public List<Task> generateTasks() {
            List<Task> tasks = new ArrayList<>();
            for (int i = 1; i <= 5; i++) {
                tasks.add(new Task(i, 200 + (i * 50), i));
            }
            return tasks;
        }
    }

    /**
     * 시나리오 4: I/O 집약적 작업들 (짧은 burst)
     */
    static class IOIntensiveWorkload {
        public List<Task> generateTasks() {
            List<Task> tasks = new ArrayList<>();
            for (int i = 1; i <= 8; i++) {
                tasks.add(new Task(i, 30 + (i * 10), i));
            }
            return tasks;
        }
    }

    /**
     * 종합 분석
     */
    static class ComprehensiveAnalysis {
        public void analyze() {
            System.out.println("\n╔══════════════════════════════════════════════════════════╗");
            System.out.println("║           Scheduler Comparison Summary                   ║");
            System.out.println("╚══════════════════════════════════════════════════════════╝");

            System.out.println("\n┌─────────────────┬──────────────────────────────────────┐");
            System.out.println("│ FCFS            │ - Simple, fair                        │");
            System.out.println("│                 │ - High waiting time                   │");
            System.out.println("│                 │ - Convoy effect                       │");
            System.out.println("│                 │ ✓ Batch systems                       │");
            System.out.println("├─────────────────┼──────────────────────────────────────┤");
            System.out.println("│ SJF             │ - Optimal avg waiting time            │");
            System.out.println("│                 │ - Potential starvation                │");
            System.out.println("│                 │ - Needs burst time prediction         │");
            System.out.println("│                 │ ✓ Batch with known times              │");
            System.out.println("├─────────────────┼──────────────────────────────────────┤");
            System.out.println("│ Round Robin     │ - Fair, no starvation                 │");
            System.out.println("│                 │ - Good response time                  │");
            System.out.println("│                 │ - Context switch overhead             │");
            System.out.println("│                 │ ✓ Time-sharing systems                │");
            System.out.println("├─────────────────┼──────────────────────────────────────┤");
            System.out.println("│ Priority        │ - Important tasks first               │");
            System.out.println("│                 │ - Can cause starvation                │");
            System.out.println("│                 │ - Needs aging                         │");
            System.out.println("│                 │ ✓ Real-time systems                   │");
            System.out.println("├─────────────────┼──────────────────────────────────────┤");
            System.out.println("│ MLFQ            │ - Adaptive, versatile                 │");
            System.out.println("│                 │ - Good for mixed workloads            │");
            System.out.println("│                 │ - Complex to tune                     │");
            System.out.println("│                 │ ✓ General-purpose OS                  │");
            System.out.println("└─────────────────┴──────────────────────────────────────┘");

            System.out.println("\nKey Metrics:");
            System.out.println("  • Waiting Time:    Time spent in ready queue");
            System.out.println("  • Response Time:   Time until first execution");
            System.out.println("  • Turnaround Time: Total time from arrival to completion");
            System.out.println("  • CPU Utilization: Percentage of time CPU is busy");
            System.out.println("  • Throughput:      Tasks completed per time unit");
        }
    }

    public static void main(String[] args) {
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("           SCHEDULER COMPARISON ANALYSIS");
        System.out.println("═══════════════════════════════════════════════════════\n");

        ComparisonTest comparison = new ComparisonTest();

        // 시나리오 1: 균일한 작업
        System.out.println("\n" + "=".repeat(60));
        comparison.runComparison("Scenario 1: Uniform Workload",
                new UniformWorkload().generateTasks());

        // 시나리오 2: 혼합 작업
        System.out.println("\n" + "=".repeat(60));
        comparison.runComparison("Scenario 2: Mixed Workload (Short + Long)",
                new MixedWorkload().generateTasks());

        // 시나리오 3: CPU 집약적
        System.out.println("\n" + "=".repeat(60));
        comparison.runComparison("Scenario 3: CPU-Intensive Workload",
                new CPUIntensiveWorkload().generateTasks());

        // 시나리오 4: I/O 집약적
        System.out.println("\n" + "=".repeat(60));
        comparison.runComparison("Scenario 4: I/O-Intensive Workload",
                new IOIntensiveWorkload().generateTasks());

        // 종합 분석
        System.out.println("\n" + "=".repeat(60));
        new ComprehensiveAnalysis().analyze();

        System.out.println("\n═══════════════════════════════════════════════════════");
        System.out.println("           COMPARISON COMPLETED");
        System.out.println("═══════════════════════════════════════════════════════");
    }

    private static List<Task> cloneTasks(List<Task> tasks) {
        List<Task> cloned = new ArrayList<>();
        for (Task task : tasks) {
            cloned.add(task.clone());
        }
        return cloned;
    }
}