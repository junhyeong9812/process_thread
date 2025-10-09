package com.study.comparison.resource;

import com.study.common.util.SystemInfo;
import com.study.common.util.TimeUtils;
import com.study.common.util.ReportGenerator;
import com.study.common.monitor.MetricsCollector;

import java.lang.management.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * CPU 사용률 분석기
 * CPU 사용률, 코어 활용도, 스레드 활용도를 분석합니다.
 */
public class CPUUsageAnalyzer {

    private static final OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
    private static final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
    private static final RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();

    /**
     * CPU 스냅샷
     */
    public static class CPUSnapshot {
        private final long timestamp;
        private final double systemCpuLoad;
        private final double processCpuLoad;
        private final double systemLoadAverage;
        private final int availableProcessors;
        private final int threadCount;
        private final long processCpuTime;
        private final long uptime;

        public CPUSnapshot() {
            this.timestamp = System.currentTimeMillis();
            this.availableProcessors = Runtime.getRuntime().availableProcessors();
            this.systemLoadAverage = osBean.getSystemLoadAverage();
            this.threadCount = threadBean.getThreadCount();
            this.uptime = runtimeBean.getUptime();

            // CPU 로드
            if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
                com.sun.management.OperatingSystemMXBean sunOsBean =
                        (com.sun.management.OperatingSystemMXBean) osBean;
                this.systemCpuLoad = sunOsBean.getCpuLoad() * 100;
                this.processCpuLoad = sunOsBean.getProcessCpuLoad() * 100;
                this.processCpuTime = sunOsBean.getProcessCpuTime();
            } else {
                this.systemCpuLoad = 0;
                this.processCpuLoad = 0;
                this.processCpuTime = 0;
            }
        }

        public long getTimestamp() { return timestamp; }
        public double getSystemCpuLoad() { return systemCpuLoad; }
        public double getProcessCpuLoad() { return processCpuLoad; }
        public double getSystemLoadAverage() { return systemLoadAverage; }
        public int getAvailableProcessors() { return availableProcessors; }
        public int getThreadCount() { return threadCount; }
        public long getProcessCpuTime() { return processCpuTime; }
        public long getUptime() { return uptime; }

        @Override
        public String toString() {
            return String.format("CPUSnapshot[system=%.1f%%, process=%.1f%%, load=%.2f, threads=%d]",
                    systemCpuLoad, processCpuLoad, systemLoadAverage, threadCount);
        }
    }

    /**
     * CPU 분석 결과
     */
    public static class CPUAnalysisResult {
        private final String name;
        private final List<CPUSnapshot> snapshots;
        private final long durationMillis;

        public CPUAnalysisResult(String name, List<CPUSnapshot> snapshots, long durationMillis) {
            this.name = name;
            this.snapshots = new ArrayList<>(snapshots);
            this.durationMillis = durationMillis;
        }

        public String getName() { return name; }
        public List<CPUSnapshot> getSnapshots() { return new ArrayList<>(snapshots); }
        public long getDurationMillis() { return durationMillis; }

        public double getAverageProcessCpuLoad() {
            return snapshots.stream()
                    .mapToDouble(CPUSnapshot::getProcessCpuLoad)
                    .average()
                    .orElse(0.0);
        }

        public double getPeakProcessCpuLoad() {
            return snapshots.stream()
                    .mapToDouble(CPUSnapshot::getProcessCpuLoad)
                    .max()
                    .orElse(0.0);
        }

        public double getMinProcessCpuLoad() {
            return snapshots.stream()
                    .mapToDouble(CPUSnapshot::getProcessCpuLoad)
                    .min()
                    .orElse(0.0);
        }

        public double getAverageSystemCpuLoad() {
            return snapshots.stream()
                    .mapToDouble(CPUSnapshot::getSystemCpuLoad)
                    .average()
                    .orElse(0.0);
        }

        public double getAverageThreadCount() {
            return snapshots.stream()
                    .mapToInt(CPUSnapshot::getThreadCount)
                    .average()
                    .orElse(0.0);
        }

        public long getTotalCpuTime() {
            if (snapshots.isEmpty()) return 0;
            return snapshots.get(snapshots.size() - 1).getProcessCpuTime() -
                    snapshots.get(0).getProcessCpuTime();
        }

        public double getCpuEfficiency() {
            // CPU 효율성 = (CPU Time / Wall Time) / Core Count
            if (snapshots.isEmpty()) return 0;

            long cpuTimeNanos = getTotalCpuTime();
            long wallTimeNanos = durationMillis * 1_000_000;
            int cores = snapshots.get(0).getAvailableProcessors();

            if (wallTimeNanos == 0 || cores == 0) return 0;

            return (double) cpuTimeNanos / wallTimeNanos / cores * 100;
        }

        @Override
        public String toString() {
            return String.format("%s: avg_cpu=%.1f%%, peak=%.1f%%, efficiency=%.1f%%, threads=%.1f",
                    name, getAverageProcessCpuLoad(), getPeakProcessCpuLoad(),
                    getCpuEfficiency(), getAverageThreadCount());
        }
    }

    /**
     * 스레드 CPU 사용 정보
     */
    public static class ThreadCpuInfo {
        private final long threadId;
        private final String threadName;
        private final long cpuTime;
        private final long userTime;
        private final Thread.State state;

        public ThreadCpuInfo(long threadId) {
            this.threadId = threadId;
            ThreadInfo info = threadBean.getThreadInfo(threadId);
            this.threadName = info != null ? info.getThreadName() : "Unknown";
            this.cpuTime = threadBean.getThreadCpuTime(threadId);
            this.userTime = threadBean.getThreadUserTime(threadId);
            this.state = info != null ? info.getThreadState() : Thread.State.TERMINATED;
        }

        public long getThreadId() { return threadId; }
        public String getThreadName() { return threadName; }
        public long getCpuTime() { return cpuTime; }
        public long getUserTime() { return userTime; }
        public Thread.State getState() { return state; }

        @Override
        public String toString() {
            return String.format("Thread[%d:%s] cpu=%s, user=%s, state=%s",
                    threadId, threadName,
                    TimeUtils.formatNanos(cpuTime),
                    TimeUtils.formatNanos(userTime),
                    state);
        }
    }

    /**
     * 작업 실행 중 CPU 사용률 측정
     */
    public CPUAnalysisResult analyzeCpuUsage(String name, Runnable task,
                                             long samplingIntervalMillis) {
        List<CPUSnapshot> snapshots = new CopyOnWriteArrayList<>();
        ScheduledExecutorService sampler = Executors.newSingleThreadScheduledExecutor();

        // 초기 스냅샷
        snapshots.add(new CPUSnapshot());

        // 주기적 샘플링 시작
        ScheduledFuture<?> samplingTask = sampler.scheduleAtFixedRate(
                () -> snapshots.add(new CPUSnapshot()),
                samplingIntervalMillis,
                samplingIntervalMillis,
                TimeUnit.MILLISECONDS
        );

        long startTime = System.currentTimeMillis();

        try {
            // 작업 실행
            task.run();
        } finally {
            // 샘플링 중지
            samplingTask.cancel(false);
            sampler.shutdown();

            try {
                sampler.awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // 최종 스냅샷
            snapshots.add(new CPUSnapshot());
        }

        long duration = System.currentTimeMillis() - startTime;

        return new CPUAnalysisResult(name, snapshots, duration);
    }

    /**
     * CPU 집약적 작업 벤치마크
     */
    public CPUAnalysisResult cpuIntensiveBenchmark(String name, int workload) {
        return analyzeCpuUsage(name, () -> {
            // CPU 집약적 작업 시뮬레이션
            long result = 0;
            for (int i = 0; i < workload; i++) {
                result += fibonacci(30);
            }
        }, 100);
    }

    /**
     * 멀티스레드 CPU 사용률 분석
     */
    public CPUAnalysisResult analyzeMultithreadedCpuUsage(String name, Runnable task,
                                                          int threadCount,
                                                          long samplingIntervalMillis) {
        List<CPUSnapshot> snapshots = new CopyOnWriteArrayList<>();
        ScheduledExecutorService sampler = Executors.newSingleThreadScheduledExecutor();
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // 초기 스냅샷
        snapshots.add(new CPUSnapshot());

        // 주기적 샘플링 시작
        ScheduledFuture<?> samplingTask = sampler.scheduleAtFixedRate(
                () -> snapshots.add(new CPUSnapshot()),
                samplingIntervalMillis,
                samplingIntervalMillis,
                TimeUnit.MILLISECONDS
        );

        long startTime = System.currentTimeMillis();
        CountDownLatch latch = new CountDownLatch(threadCount);

        try {
            // 멀티스레드 작업 실행
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        task.run();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            // 샘플링 중지
            samplingTask.cancel(false);
            sampler.shutdown();
            executor.shutdown();

            try {
                sampler.awaitTermination(1, TimeUnit.SECONDS);
                executor.awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // 최종 스냅샷
            snapshots.add(new CPUSnapshot());
        }

        long duration = System.currentTimeMillis() - startTime;

        return new CPUAnalysisResult(name, snapshots, duration);
    }

    /**
     * 스레드별 CPU 사용 시간 분석
     */
    public List<ThreadCpuInfo> getThreadCpuUsage() {
        List<ThreadCpuInfo> threadInfos = new ArrayList<>();

        long[] threadIds = threadBean.getAllThreadIds();
        for (long threadId : threadIds) {
            threadInfos.add(new ThreadCpuInfo(threadId));
        }

        // CPU 시간으로 정렬
        threadInfos.sort((a, b) -> Long.compare(b.getCpuTime(), a.getCpuTime()));

        return threadInfos;
    }

    /**
     * 코어 활용도 분석
     */
    public CoreUtilizationAnalysis analyzeCoreUtilization(Runnable task, long durationMillis) {
        int cores = Runtime.getRuntime().availableProcessors();
        List<Double> cpuSamples = new CopyOnWriteArrayList<>();

        ScheduledExecutorService sampler = Executors.newSingleThreadScheduledExecutor();

        // CPU 샘플링 시작
        ScheduledFuture<?> samplingTask = sampler.scheduleAtFixedRate(() -> {
            CPUSnapshot snapshot = new CPUSnapshot();
            cpuSamples.add(snapshot.getProcessCpuLoad());
        }, 0, 50, TimeUnit.MILLISECONDS);

        long startTime = System.currentTimeMillis();

        // 작업 실행
        task.run();

        // 남은 시간 대기
        long elapsed = System.currentTimeMillis() - startTime;
        if (elapsed < durationMillis) {
            try {
                Thread.sleep(durationMillis - elapsed);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // 샘플링 중지
        samplingTask.cancel(false);
        sampler.shutdown();

        try {
            sampler.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        double avgCpuUsage = cpuSamples.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        double coreUtilization = avgCpuUsage / cores;

        return new CoreUtilizationAnalysis(cores, avgCpuUsage, coreUtilization, cpuSamples.size());
    }

    /**
     * 코어 활용도 분석 결과
     */
    public static class CoreUtilizationAnalysis {
        private final int totalCores;
        private final double avgCpuUsage;
        private final double coreUtilization;
        private final int sampleCount;

        public CoreUtilizationAnalysis(int totalCores, double avgCpuUsage,
                                       double coreUtilization, int sampleCount) {
            this.totalCores = totalCores;
            this.avgCpuUsage = avgCpuUsage;
            this.coreUtilization = coreUtilization;
            this.sampleCount = sampleCount;
        }

        public int getTotalCores() { return totalCores; }
        public double getAvgCpuUsage() { return avgCpuUsage; }
        public double getCoreUtilization() { return coreUtilization; }
        public int getSampleCount() { return sampleCount; }

        @Override
        public String toString() {
            return String.format("CoreUtilization[cores=%d, cpu=%.1f%%, utilization=%.1f%%, samples=%d]",
                    totalCores, avgCpuUsage, coreUtilization, sampleCount);
        }
    }

    /**
     * 여러 작업의 CPU 사용률 비교
     */
    public List<CPUAnalysisResult> compareCpuUsage(Map<String, Runnable> tasks,
                                                   long samplingIntervalMillis) {
        List<CPUAnalysisResult> results = new ArrayList<>();

        for (Map.Entry<String, Runnable> entry : tasks.entrySet()) {
            // 각 테스트 전 대기
            try { Thread.sleep(500); } catch (InterruptedException e) {}

            CPUAnalysisResult result = analyzeCpuUsage(
                    entry.getKey(),
                    entry.getValue(),
                    samplingIntervalMillis
            );
            results.add(result);
        }

        return results;
    }

    /**
     * 현재 CPU 상태 출력
     */
    public void printCurrentCpuState() {
        CPUSnapshot snapshot = new CPUSnapshot();

        System.out.println("=== Current CPU State ===");
        System.out.println(snapshot);
        System.out.printf("Available Processors: %d%n", snapshot.getAvailableProcessors());
        System.out.printf("System CPU Load: %.1f%%%n", snapshot.getSystemCpuLoad());
        System.out.printf("Process CPU Load: %.1f%%%n", snapshot.getProcessCpuLoad());
        System.out.printf("System Load Average: %.2f%n", snapshot.getSystemLoadAverage());
        System.out.printf("Thread Count: %d%n", snapshot.getThreadCount());
        System.out.printf("Process CPU Time: %s%n", TimeUtils.formatNanos(snapshot.getProcessCpuTime()));
    }

    /**
     * 비교 리포트 생성
     */
    public String generateComparisonReport(List<CPUAnalysisResult> results) {
        StringBuilder report = new StringBuilder();
        report.append("=== CPU Usage Comparison Report ===\n\n");

        // 평균 CPU 사용률 순위
        report.append("Average CPU Usage Ranking:\n");
        results.stream()
                .sorted((a, b) -> Double.compare(b.getAverageProcessCpuLoad(), a.getAverageProcessCpuLoad()))
                .forEach(r -> report.append(String.format("  %s: %.1f%%\n",
                        r.getName(), r.getAverageProcessCpuLoad())));

        report.append("\n");

        // CPU 효율성 순위
        report.append("CPU Efficiency Ranking:\n");
        results.stream()
                .sorted((a, b) -> Double.compare(b.getCpuEfficiency(), a.getCpuEfficiency()))
                .forEach(r -> report.append(String.format("  %s: %.1f%%\n",
                        r.getName(), r.getCpuEfficiency())));

        report.append("\n");

        // 상세 정보
        report.append("Detailed Statistics:\n");
        for (CPUAnalysisResult result : results) {
            report.append(String.format("\n[%s]\n", result.getName()));
            report.append(String.format("  Duration: %dms\n", result.getDurationMillis()));
            report.append(String.format("  Samples: %d\n", result.getSnapshots().size()));
            report.append(String.format("  Average CPU: %.1f%%\n", result.getAverageProcessCpuLoad()));
            report.append(String.format("  Peak CPU: %.1f%%\n", result.getPeakProcessCpuLoad()));
            report.append(String.format("  Min CPU: %.1f%%\n", result.getMinProcessCpuLoad()));
            report.append(String.format("  Average System CPU: %.1f%%\n", result.getAverageSystemCpuLoad()));
            report.append(String.format("  Average Threads: %.1f\n", result.getAverageThreadCount()));
            report.append(String.format("  Total CPU Time: %s\n", TimeUtils.formatNanos(result.getTotalCpuTime())));
            report.append(String.format("  CPU Efficiency: %.1f%%\n", result.getCpuEfficiency()));
        }

        return report.toString();
    }

    /**
     * HTML 리포트 생성
     */
    public void generateHtmlReport(List<CPUAnalysisResult> results, String filename)
            throws Exception {

        ReportGenerator.ReportBuilder builder = ReportGenerator.html("CPU Usage Analysis Report")
                .author("CPUUsageAnalyzer");

        // 테이블 데이터 준비
        String[] headers = {"Name", "Avg CPU", "Peak CPU", "Efficiency", "Avg Threads", "Duration"};
        List<String[]> rows = results.stream()
                .map(r -> new String[]{
                        r.getName(),
                        String.format("%.1f%%", r.getAverageProcessCpuLoad()),
                        String.format("%.1f%%", r.getPeakProcessCpuLoad()),
                        String.format("%.1f%%", r.getCpuEfficiency()),
                        String.format("%.1f", r.getAverageThreadCount()),
                        r.getDurationMillis() + "ms"
                })
                .collect(Collectors.toList());

        builder.addTable("CPU Usage Results", headers, rows);

        // 파일 저장
        builder.saveToFile(filename);
    }

    /**
     * 피보나치 계산 (CPU 집약적 작업)
     */
    private static long fibonacci(int n) {
        if (n <= 1) return n;
        return fibonacci(n - 1) + fibonacci(n - 2);
    }

    /**
     * 테스트 메인 메서드
     */
    public static void main(String[] args) {
        System.out.println("=== CPUUsageAnalyzer 테스트 ===\n");

        CPUUsageAnalyzer analyzer = new CPUUsageAnalyzer();

        try {
            // 1. 현재 CPU 상태
            System.out.println("1. 현재 CPU 상태:");
            analyzer.printCurrentCpuState();

            // 2. 단일 작업 CPU 분석
            System.out.println("\n2. 단일 작업 CPU 분석:");
            CPUAnalysisResult singleResult = analyzer.analyzeCpuUsage(
                    "Fibonacci Calculation",
                    () -> {
                        for (int i = 0; i < 10; i++) {
                            fibonacci(35);
                        }
                    },
                    100
            );
            System.out.println(singleResult);

            // 3. 여러 작업 비교
            System.out.println("\n3. 여러 작업 CPU 사용률 비교:");
            Map<String, Runnable> tasks = new LinkedHashMap<>();

            tasks.put("Light CPU Task", () -> {
                int sum = 0;
                for (int i = 0; i < 1_000_000; i++) {
                    sum += i;
                }
            });

            tasks.put("Medium CPU Task", () -> {
                for (int i = 0; i < 5; i++) {
                    fibonacci(30);
                }
            });

            tasks.put("Heavy CPU Task", () -> {
                for (int i = 0; i < 3; i++) {
                    fibonacci(35);
                }
            });

            List<CPUAnalysisResult> compareResults = analyzer.compareCpuUsage(tasks, 50);
            compareResults.forEach(System.out::println);

            // 4. 멀티스레드 CPU 분석
            System.out.println("\n4. 멀티스레드 CPU 분석:");
            CPUAnalysisResult multiResult = analyzer.analyzeMultithreadedCpuUsage(
                    "Multithreaded Task",
                    () -> fibonacci(33),
                    4,
                    100
            );
            System.out.println(multiResult);

            // 5. 코어 활용도 분석
            System.out.println("\n5. 코어 활용도 분석:");
            CoreUtilizationAnalysis coreUtil = analyzer.analyzeCoreUtilization(
                    () -> {
                        for (int i = 0; i < 5; i++) {
                            fibonacci(32);
                        }
                    },
                    2000
            );
            System.out.println(coreUtil);

            // 6. 스레드별 CPU 사용 시간
            System.out.println("\n6. 스레드별 CPU 사용 시간 (Top 10):");
            List<ThreadCpuInfo> threadInfos = analyzer.getThreadCpuUsage();
            threadInfos.stream()
                    .limit(10)
                    .forEach(System.out::println);

            // 7. 비교 리포트
            System.out.println("\n7. 비교 리포트:");
            System.out.println(analyzer.generateComparisonReport(compareResults));

            // 8. HTML 리포트 생성
            System.out.println("\n8. HTML 리포트 생성:");
            analyzer.generateHtmlReport(compareResults, "cpu_analysis_report.html");
            System.out.println("리포트 저장: cpu_analysis_report.html");

        } catch (Exception e) {
            System.err.println("테스트 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("\n테스트 완료!");
    }
}