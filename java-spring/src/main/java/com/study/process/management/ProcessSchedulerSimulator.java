package com.study.process.management;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

/**
 * 프로세스 스케줄링 시뮬레이션
 * 다양한 스케줄링 알고리즘을 시뮬레이션
 */
public class ProcessSchedulerSimulator {

    /**
     * 시뮬레이션용 프로세스
     */
    public static class SimulatedProcess {
        private final String id;
        private final int burstTime;      // CPU 실행 시간
        private final int arrivalTime;     // 도착 시간
        private final int priority;        // 우선순위 (낮을수록 높은 우선순위)
        private int remainingTime;         // 남은 실행 시간
        private int waitingTime;           // 대기 시간
        private int turnaroundTime;        // 반환 시간
        private int responseTime;          // 응답 시간
        private int completionTime;        // 완료 시간
        private boolean firstResponse;     // 첫 응답 여부

        public SimulatedProcess(String id, int burstTime, int arrivalTime, int priority) {
            this.id = id;
            this.burstTime = burstTime;
            this.arrivalTime = arrivalTime;
            this.priority = priority;
            this.remainingTime = burstTime;
            this.waitingTime = 0;
            this.turnaroundTime = 0;
            this.responseTime = -1;
            this.completionTime = 0;
            this.firstResponse = true;
        }

        /**
         * 프로세스 실행 (1 단위 시간)
         */
        public void execute(int currentTime) {
            if (firstResponse) {
                responseTime = currentTime - arrivalTime;
                firstResponse = false;
            }
            remainingTime--;

            if (isCompleted()) {
                completionTime = currentTime + 1;
                turnaroundTime = completionTime - arrivalTime;
                waitingTime = turnaroundTime - burstTime;
            }
        }

        public boolean isCompleted() {
            return remainingTime <= 0;
        }

        public void reset() {
            this.remainingTime = burstTime;
            this.waitingTime = 0;
            this.turnaroundTime = 0;
            this.responseTime = -1;
            this.completionTime = 0;
            this.firstResponse = true;
        }

        // Getters
        public String getId() { return id; }
        public int getBurstTime() { return burstTime; }
        public int getArrivalTime() { return arrivalTime; }
        public int getPriority() { return priority; }
        public int getRemainingTime() { return remainingTime; }
        public int getWaitingTime() { return waitingTime; }
        public int getTurnaroundTime() { return turnaroundTime; }
        public int getResponseTime() { return responseTime; }
        public int getCompletionTime() { return completionTime; }

        @Override
        public String toString() {
            return String.format("%s(burst=%d, arrival=%d, priority=%d)",
                    id, burstTime, arrivalTime, priority);
        }
    }

    /**
     * 스케줄링 알고리즘 인터페이스
     */
    public interface SchedulingAlgorithm {
        String getName();
        SchedulingResult schedule(List<SimulatedProcess> processes);
    }

    /**
     * 스케줄링 결과
     */
    public static class SchedulingResult {
        private final String algorithm;
        private final List<SimulatedProcess> processes;
        private final List<String> executionOrder;
        private final double averageWaitingTime;
        private final double averageTurnaroundTime;
        private final double averageResponseTime;
        private final int totalTime;
        private final double cpuUtilization;
        private final double throughput;

        public SchedulingResult(String algorithm, List<SimulatedProcess> processes,
                                List<String> executionOrder, int totalTime) {
            this.algorithm = algorithm;
            this.processes = processes;
            this.executionOrder = executionOrder;
            this.totalTime = totalTime;

            // 평균 계산
            this.averageWaitingTime = processes.stream()
                    .mapToInt(SimulatedProcess::getWaitingTime)
                    .average()
                    .orElse(0);

            this.averageTurnaroundTime = processes.stream()
                    .mapToInt(SimulatedProcess::getTurnaroundTime)
                    .average()
                    .orElse(0);

            this.averageResponseTime = processes.stream()
                    .mapToInt(SimulatedProcess::getResponseTime)
                    .average()
                    .orElse(0);

            // CPU 사용률 계산
            int totalBurstTime = processes.stream()
                    .mapToInt(SimulatedProcess::getBurstTime)
                    .sum();
            this.cpuUtilization = (totalBurstTime * 100.0) / totalTime;

            // 처리율 계산 (프로세스/시간 단위)
            this.throughput = (double) processes.size() / totalTime;
        }

        public void printResult() {
            System.out.println("\n" + "=".repeat(60));
            System.out.println("   " + algorithm + " Scheduling Result");
            System.out.println("=".repeat(60));

            // 간트 차트 출력
            printGanttChart();

            // 통계 출력
            System.out.println("\n--- Statistics ---");
            System.out.printf("Total Time: %d\n", totalTime);
            System.out.printf("CPU Utilization: %.2f%%\n", cpuUtilization);
            System.out.printf("Throughput: %.3f processes/time\n", throughput);
            System.out.printf("Average Waiting Time: %.2f\n", averageWaitingTime);
            System.out.printf("Average Turnaround Time: %.2f\n", averageTurnaroundTime);
            System.out.printf("Average Response Time: %.2f\n", averageResponseTime);

            // 프로세스별 상세 정보
            System.out.println("\n--- Process Details ---");
            System.out.println("ID\tBurst\tArrival\tWait\tTurnaround\tResponse\tCompletion");
            System.out.println("-".repeat(70));
            for (SimulatedProcess p : processes) {
                System.out.printf("%s\t%d\t%d\t%d\t%d\t\t%d\t\t%d\n",
                        p.getId(), p.getBurstTime(), p.getArrivalTime(),
                        p.getWaitingTime(), p.getTurnaroundTime(),
                        p.getResponseTime(), p.getCompletionTime());
            }
        }

        private void printGanttChart() {
            System.out.println("\n--- Gantt Chart ---");

            // 간트 차트를 10개씩 끊어서 출력
            for (int i = 0; i < executionOrder.size(); i += 20) {
                int end = Math.min(i + 20, executionOrder.size());
                List<String> subList = executionOrder.subList(i, end);

                // 상단 경계
                System.out.print("|");
                for (String pid : subList) {
                    System.out.print("---");
                }
                System.out.println("|");

                // 프로세스 ID
                System.out.print("|");
                for (String pid : subList) {
                    System.out.printf("%2s ", pid != null ? pid : "--");
                }
                System.out.println("|");

                // 하단 경계
                System.out.print("|");
                for (String pid : subList) {
                    System.out.print("---");
                }
                System.out.println("|");

                // 시간 표시
                System.out.print(i + " ");
                for (int j = i; j < end; j++) {
                    System.out.printf("%-3d", j + 1);
                }
                System.out.println();
            }
        }

        // Getters
        public String getAlgorithm() { return algorithm; }
        public double getAverageWaitingTime() { return averageWaitingTime; }
        public double getAverageTurnaroundTime() { return averageTurnaroundTime; }
        public double getAverageResponseTime() { return averageResponseTime; }
        public double getCpuUtilization() { return cpuUtilization; }
        public double getThroughput() { return throughput; }
    }

    /**
     * FCFS (First-Come First-Served) 알고리즘
     */
    public static class FCFSAlgorithm implements SchedulingAlgorithm {
        @Override
        public String getName() {
            return "FCFS (First-Come First-Served)";
        }

        @Override
        public SchedulingResult schedule(List<SimulatedProcess> originalProcesses) {
            // 프로세스 복사 및 리셋
            List<SimulatedProcess> processes = copyProcesses(originalProcesses);
            List<String> executionOrder = new ArrayList<>();

            // 도착 시간으로 정렬
            processes.sort(Comparator.comparingInt(SimulatedProcess::getArrivalTime));

            int currentTime = 0;

            for (SimulatedProcess process : processes) {
                // 프로세스 도착까지 대기
                if (currentTime < process.getArrivalTime()) {
                    // IDLE 시간 추가
                    for (int i = currentTime; i < process.getArrivalTime(); i++) {
                        executionOrder.add(null);
                    }
                    currentTime = process.getArrivalTime();
                }

                // 프로세스 실행
                while (!process.isCompleted()) {
                    process.execute(currentTime);
                    executionOrder.add(process.getId());
                    currentTime++;
                }
            }

            return new SchedulingResult(getName(), processes, executionOrder, currentTime);
        }
    }

    /**
     * SJF (Shortest Job First) 알고리즘
     */
    public static class SJFAlgorithm implements SchedulingAlgorithm {
        private final boolean preemptive;

        public SJFAlgorithm(boolean preemptive) {
            this.preemptive = preemptive;
        }

        @Override
        public String getName() {
            return preemptive ? "SRTF (Shortest Remaining Time First)" :
                    "SJF (Shortest Job First)";
        }

        @Override
        public SchedulingResult schedule(List<SimulatedProcess> originalProcesses) {
            List<SimulatedProcess> processes = copyProcesses(originalProcesses);
            List<SimulatedProcess> readyQueue = new ArrayList<>();
            List<String> executionOrder = new ArrayList<>();

            int currentTime = 0;
            int completed = 0;

            while (completed < processes.size()) {
                // 도착한 프로세스를 준비 큐에 추가
                for (SimulatedProcess p : processes) {
                    if (p.getArrivalTime() <= currentTime &&
                            !readyQueue.contains(p) &&
                            !p.isCompleted()) {
                        readyQueue.add(p);
                    }
                }

                if (readyQueue.isEmpty()) {
                    executionOrder.add(null);
                    currentTime++;
                    continue;
                }

                // 가장 짧은 프로세스 선택
                SimulatedProcess shortest = readyQueue.stream()
                        .min(Comparator.comparingInt(SimulatedProcess::getRemainingTime))
                        .orElse(null);

                if (shortest != null) {
                    if (!preemptive) {
                        // Non-preemptive: 완료까지 실행
                        while (!shortest.isCompleted()) {
                            shortest.execute(currentTime);
                            executionOrder.add(shortest.getId());
                            currentTime++;
                        }
                        readyQueue.remove(shortest);
                        completed++;
                    } else {
                        // Preemptive: 1 단위 시간만 실행
                        shortest.execute(currentTime);
                        executionOrder.add(shortest.getId());
                        currentTime++;

                        if (shortest.isCompleted()) {
                            readyQueue.remove(shortest);
                            completed++;
                        }
                    }
                }
            }

            return new SchedulingResult(getName(), processes, executionOrder, currentTime);
        }
    }

    /**
     * Round Robin 알고리즘
     */
    public static class RoundRobinAlgorithm implements SchedulingAlgorithm {
        private final int timeQuantum;

        public RoundRobinAlgorithm(int timeQuantum) {
            this.timeQuantum = timeQuantum;
        }

        @Override
        public String getName() {
            return String.format("Round Robin (Quantum=%d)", timeQuantum);
        }

        @Override
        public SchedulingResult schedule(List<SimulatedProcess> originalProcesses) {
            List<SimulatedProcess> processes = copyProcesses(originalProcesses);
            Queue<SimulatedProcess> readyQueue = new LinkedList<>();
            List<String> executionOrder = new ArrayList<>();

            // 도착 시간으로 정렬
            processes.sort(Comparator.comparingInt(SimulatedProcess::getArrivalTime));

            int currentTime = 0;
            int completed = 0;
            int processIndex = 0;

            // 초기 프로세스 추가
            while (processIndex < processes.size() &&
                    processes.get(processIndex).getArrivalTime() <= currentTime) {
                readyQueue.offer(processes.get(processIndex++));
            }

            while (completed < processes.size()) {
                if (readyQueue.isEmpty()) {
                    executionOrder.add(null);
                    currentTime++;

                    // 새로 도착한 프로세스 확인
                    while (processIndex < processes.size() &&
                            processes.get(processIndex).getArrivalTime() <= currentTime) {
                        readyQueue.offer(processes.get(processIndex++));
                    }
                    continue;
                }

                SimulatedProcess current = readyQueue.poll();

                // 타임 퀀텀만큼 실행
                int executionTime = Math.min(timeQuantum, current.getRemainingTime());

                for (int i = 0; i < executionTime; i++) {
                    current.execute(currentTime);
                    executionOrder.add(current.getId());
                    currentTime++;

                    // 실행 중 도착한 프로세스를 큐에 추가
                    while (processIndex < processes.size() &&
                            processes.get(processIndex).getArrivalTime() <= currentTime) {
                        readyQueue.offer(processes.get(processIndex++));
                    }
                }

                if (current.isCompleted()) {
                    completed++;
                } else {
                    readyQueue.offer(current);
                }
            }

            return new SchedulingResult(getName(), processes, executionOrder, currentTime);
        }
    }

    /**
     * Priority 스케줄링 알고리즘
     */
    public static class PriorityAlgorithm implements SchedulingAlgorithm {
        private final boolean preemptive;

        public PriorityAlgorithm(boolean preemptive) {
            this.preemptive = preemptive;
        }

        @Override
        public String getName() {
            return preemptive ? "Preemptive Priority" : "Non-Preemptive Priority";
        }

        @Override
        public SchedulingResult schedule(List<SimulatedProcess> originalProcesses) {
            List<SimulatedProcess> processes = copyProcesses(originalProcesses);
            PriorityQueue<SimulatedProcess> readyQueue = new PriorityQueue<>(
                    Comparator.comparingInt(SimulatedProcess::getPriority)
            );
            List<String> executionOrder = new ArrayList<>();

            int currentTime = 0;
            int completed = 0;

            while (completed < processes.size()) {
                // 도착한 프로세스를 준비 큐에 추가
                for (SimulatedProcess p : processes) {
                    if (p.getArrivalTime() <= currentTime &&
                            !readyQueue.contains(p) &&
                            !p.isCompleted()) {
                        readyQueue.offer(p);
                    }
                }

                if (readyQueue.isEmpty()) {
                    executionOrder.add(null);
                    currentTime++;
                    continue;
                }

                SimulatedProcess highestPriority = readyQueue.poll();

                if (!preemptive) {
                    // Non-preemptive: 완료까지 실행
                    while (!highestPriority.isCompleted()) {
                        highestPriority.execute(currentTime);
                        executionOrder.add(highestPriority.getId());
                        currentTime++;
                    }
                    completed++;
                } else {
                    // Preemptive: 1 단위 시간만 실행
                    highestPriority.execute(currentTime);
                    executionOrder.add(highestPriority.getId());
                    currentTime++;

                    if (highestPriority.isCompleted()) {
                        completed++;
                    } else {
                        readyQueue.offer(highestPriority);
                    }
                }
            }

            return new SchedulingResult(getName(), processes, executionOrder, currentTime);
        }
    }

    /**
     * 프로세스 복사 유틸리티
     */
    private static List<SimulatedProcess> copyProcesses(List<SimulatedProcess> original) {
        List<SimulatedProcess> copy = new ArrayList<>();
        for (SimulatedProcess p : original) {
            SimulatedProcess newProcess = new SimulatedProcess(
                    p.getId(), p.getBurstTime(), p.getArrivalTime(), p.getPriority()
            );
            copy.add(newProcess);
        }
        return copy;
    }

    /**
     * 시뮬레이션 실행
     */
    public static void runSimulation() {
        System.out.println("=".repeat(60));
        System.out.println("   PROCESS SCHEDULING SIMULATION");
        System.out.println("=".repeat(60));

        // 테스트 프로세스 생성
        List<SimulatedProcess> processes = Arrays.asList(
                new SimulatedProcess("P1", 10, 0, 3),
                new SimulatedProcess("P2", 5, 1, 1),
                new SimulatedProcess("P3", 8, 2, 4),
                new SimulatedProcess("P4", 3, 3, 2),
                new SimulatedProcess("P5", 6, 4, 5)
        );

        System.out.println("\n--- Test Processes ---");
        for (SimulatedProcess p : processes) {
            System.out.println(p);
        }

        // 알고리즘 실행
        List<SchedulingAlgorithm> algorithms = Arrays.asList(
                new FCFSAlgorithm(),
                new SJFAlgorithm(false),
                new SJFAlgorithm(true),
                new RoundRobinAlgorithm(3),
                new PriorityAlgorithm(false),
                new PriorityAlgorithm(true)
        );

        List<SchedulingResult> results = new ArrayList<>();

        for (SchedulingAlgorithm algorithm : algorithms) {
            SchedulingResult result = algorithm.schedule(processes);
            result.printResult();
            results.add(result);
        }

        // 비교 요약
        printComparison(results);
    }

    /**
     * 결과 비교 출력
     */
    private static void printComparison(List<SchedulingResult> results) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("   ALGORITHM COMPARISON");
        System.out.println("=".repeat(60));

        System.out.println("\nAlgorithm\t\t\t\tAvg Wait\tAvg Turn\tAvg Resp\tCPU%\tThruput");
        System.out.println("-".repeat(90));

        for (SchedulingResult result : results) {
            System.out.printf("%-35s\t%.2f\t\t%.2f\t\t%.2f\t\t%.1f\t%.3f\n",
                    result.getAlgorithm(),
                    result.getAverageWaitingTime(),
                    result.getAverageTurnaroundTime(),
                    result.getAverageResponseTime(),
                    result.getCpuUtilization(),
                    result.getThroughput());
        }

        // 최적 알고리즘 찾기
        System.out.println("\n--- Best Algorithms ---");

        SchedulingResult bestWaitTime = results.stream()
                .min(Comparator.comparingDouble(SchedulingResult::getAverageWaitingTime))
                .orElse(null);
        if (bestWaitTime != null) {
            System.out.printf("Best Waiting Time: %s (%.2f)\n",
                    bestWaitTime.getAlgorithm(), bestWaitTime.getAverageWaitingTime());
        }

        SchedulingResult bestResponseTime = results.stream()
                .min(Comparator.comparingDouble(SchedulingResult::getAverageResponseTime))
                .orElse(null);
        if (bestResponseTime != null) {
            System.out.printf("Best Response Time: %s (%.2f)\n",
                    bestResponseTime.getAlgorithm(), bestResponseTime.getAverageResponseTime());
        }
    }

    /**
     * 메인 메서드
     */
    public static void main(String[] args) {
        runSimulation();
    }
}