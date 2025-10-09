package com.study.comparison.performance;

import com.study.common.util.TimeUtils;
import com.study.common.util.ReportGenerator;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 처리량 분석기
 * 단위 시간당 처리 작업 수를 측정하고 분석합니다.
 */
public class ThroughputAnalyzer {

    /**
     * 처리량 측정 결과
     */
    public static class ThroughputResult {
        private final String name;
        private final long totalTasks;
        private final long totalTimeNanos;
        private final double throughput;  // tasks per second
        private final double avgLatencyNanos;
        private final long minLatencyNanos;
        private final long maxLatencyNanos;
        private final List<Long> latencies;

        public ThroughputResult(String name, long totalTasks, long totalTimeNanos,
                                List<Long> latencies) {
            this.name = name;
            this.totalTasks = totalTasks;
            this.totalTimeNanos = totalTimeNanos;
            this.latencies = new ArrayList<>(latencies);

            // 처리량 계산 (tasks/second)
            this.throughput = (totalTasks * 1_000_000_000.0) / totalTimeNanos;

            // 레이턴시 통계
            if (!latencies.isEmpty()) {
                this.avgLatencyNanos = latencies.stream()
                        .mapToLong(Long::longValue)
                        .average()
                        .orElse(0.0);
                this.minLatencyNanos = latencies.stream()
                        .mapToLong(Long::longValue)
                        .min()
                        .orElse(0L);
                this.maxLatencyNanos = latencies.stream()
                        .mapToLong(Long::longValue)
                        .max()
                        .orElse(0L);
            } else {
                this.avgLatencyNanos = 0;
                this.minLatencyNanos = 0;
                this.maxLatencyNanos = 0;
            }
        }

        public String getName() { return name; }
        public long getTotalTasks() { return totalTasks; }
        public long getTotalTimeNanos() { return totalTimeNanos; }
        public double getThroughput() { return throughput; }
        public double getAvgLatencyNanos() { return avgLatencyNanos; }
        public long getMinLatencyNanos() { return minLatencyNanos; }
        public long getMaxLatencyNanos() { return maxLatencyNanos; }

        public double getPercentile(int percentile) {
            if (latencies.isEmpty()) return 0;

            List<Long> sorted = new ArrayList<>(latencies);
            Collections.sort(sorted);

            int index = (int) Math.ceil(percentile / 100.0 * sorted.size()) - 1;
            index = Math.max(0, Math.min(index, sorted.size() - 1));

            return sorted.get(index);
        }

        @Override
        public String toString() {
            return String.format(
                    "%s: %.2f tasks/sec (total=%d, time=%s, avg_latency=%s)",
                    name, throughput, totalTasks,
                    TimeUtils.formatNanos(totalTimeNanos),
                    TimeUtils.formatNanos((long) avgLatencyNanos)
            );
        }
    }

    /**
     * 작업 인터페이스
     */
    @FunctionalInterface
    public interface Task {
        void execute() throws Exception;
    }

    /**
     * 단일 작업의 처리량 측정
     */
    public ThroughputResult measureThroughput(String name, Task task, int iterations) {
        List<Long> latencies = new ArrayList<>();

        long startTime = System.nanoTime();

        for (int i = 0; i < iterations; i++) {
            long taskStart = System.nanoTime();

            try {
                task.execute();
            } catch (Exception e) {
                System.err.printf("Task failed at iteration %d: %s%n", i, e.getMessage());
            }

            long taskEnd = System.nanoTime();
            latencies.add(taskEnd - taskStart);
        }

        long totalTime = System.nanoTime() - startTime;

        return new ThroughputResult(name, iterations, totalTime, latencies);
    }

    /**
     * 동시성 처리량 측정 (멀티스레드)
     */
    public ThroughputResult measureConcurrentThroughput(String name, Task task,
                                                        int totalTasks, int threadCount)
            throws InterruptedException {

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Long> latencies = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch latch = new CountDownLatch(totalTasks);
        AtomicLong startTime = new AtomicLong(0);

        long overallStart = System.nanoTime();
        startTime.set(overallStart);

        for (int i = 0; i < totalTasks; i++) {
            executor.submit(() -> {
                long taskStart = System.nanoTime();

                try {
                    task.execute();
                } catch (Exception e) {
                    // Silent fail for throughput testing
                } finally {
                    long taskEnd = System.nanoTime();
                    latencies.add(taskEnd - taskStart);
                    latch.countDown();
                }
            });
        }

        latch.await();
        long overallEnd = System.nanoTime();

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        long totalTime = overallEnd - overallStart;

        return new ThroughputResult(name, totalTasks, totalTime, latencies);
    }

    /**
     * 워밍업 후 측정
     */
    public ThroughputResult measureWithWarmup(String name, Task task,
                                              int warmupIterations, int measureIterations) {
        // 워밍업
        for (int i = 0; i < warmupIterations; i++) {
            try {
                task.execute();
            } catch (Exception e) {
                // Silent fail
            }
        }

        // 실제 측정
        return measureThroughput(name, task, measureIterations);
    }

    /**
     * 여러 작업의 처리량 비교
     */
    public List<ThroughputResult> compareAll(Map<String, Task> tasks, int iterations) {
        List<ThroughputResult> results = new ArrayList<>();

        for (Map.Entry<String, Task> entry : tasks.entrySet()) {
            ThroughputResult result = measureThroughput(entry.getKey(), entry.getValue(), iterations);
            results.add(result);
        }

        return results;
    }

    /**
     * 부하 테스트 (점진적으로 부하 증가)
     */
    public List<ThroughputResult> loadTest(String name, Task task,
                                           int startLoad, int endLoad, int step) {
        List<ThroughputResult> results = new ArrayList<>();

        for (int load = startLoad; load <= endLoad; load += step) {
            ThroughputResult result = measureThroughput(
                    name + "_load_" + load,
                    task,
                    load
            );
            results.add(result);
        }

        return results;
    }

    /**
     * 지속 시간 기반 처리량 측정
     */
    public ThroughputResult measureForDuration(String name, Task task, long durationMillis) {
        List<Long> latencies = new ArrayList<>();
        long startTime = System.nanoTime();
        long endTime = startTime + (durationMillis * 1_000_000);
        long taskCount = 0;

        while (System.nanoTime() < endTime) {
            long taskStart = System.nanoTime();

            try {
                task.execute();
                taskCount++;
            } catch (Exception e) {
                // Continue testing
            }

            long taskEnd = System.nanoTime();
            latencies.add(taskEnd - taskStart);
        }

        long totalTime = System.nanoTime() - startTime;

        return new ThroughputResult(name, taskCount, totalTime, latencies);
    }

    /**
     * 결과 비교 리포트 생성
     */
    public String generateComparisonReport(List<ThroughputResult> results) {
        StringBuilder report = new StringBuilder();
        report.append("=== Throughput Analysis Report ===\n\n");

        // 처리량 순위
        report.append("Throughput Ranking (tasks/sec):\n");
        results.stream()
                .sorted((a, b) -> Double.compare(b.getThroughput(), a.getThroughput()))
                .forEach(r -> report.append(String.format("  %s: %.2f\n", r.getName(), r.getThroughput())));

        report.append("\n");

        // 레이턴시 순위
        report.append("Average Latency Ranking:\n");
        results.stream()
                .sorted(Comparator.comparingDouble(ThroughputResult::getAvgLatencyNanos))
                .forEach(r -> report.append(String.format("  %s: %s\n",
                        r.getName(),
                        TimeUtils.formatNanos((long) r.getAvgLatencyNanos()))));

        report.append("\n");

        // 상세 통계
        report.append("Detailed Statistics:\n");
        for (ThroughputResult result : results) {
            report.append(String.format("\n[%s]\n", result.getName()));
            report.append(String.format("  Throughput: %.2f tasks/sec\n", result.getThroughput()));
            report.append(String.format("  Total Tasks: %d\n", result.getTotalTasks()));
            report.append(String.format("  Total Time: %s\n", TimeUtils.formatNanos(result.getTotalTimeNanos())));
            report.append(String.format("  Avg Latency: %s\n", TimeUtils.formatNanos((long) result.getAvgLatencyNanos())));
            report.append(String.format("  Min Latency: %s\n", TimeUtils.formatNanos(result.getMinLatencyNanos())));
            report.append(String.format("  Max Latency: %s\n", TimeUtils.formatNanos(result.getMaxLatencyNanos())));
            report.append(String.format("  P50 Latency: %s\n", TimeUtils.formatNanos((long) result.getPercentile(50))));
            report.append(String.format("  P95 Latency: %s\n", TimeUtils.formatNanos((long) result.getPercentile(95))));
            report.append(String.format("  P99 Latency: %s\n", TimeUtils.formatNanos((long) result.getPercentile(99))));
        }

        return report.toString();
    }

    /**
     * HTML 리포트 생성
     */
    public void generateHtmlReport(List<ThroughputResult> results, String filename)
            throws Exception {

        ReportGenerator.ReportBuilder builder = ReportGenerator.html("Throughput Analysis Report")
                .author("ThroughputAnalyzer");

        // 테이블 데이터 준비
        String[] headers = {"Name", "Throughput (tasks/sec)", "Avg Latency", "P95 Latency", "Total Tasks"};
        List<String[]> rows = results.stream()
                .map(r -> new String[]{
                        r.getName(),
                        String.format("%.2f", r.getThroughput()),
                        TimeUtils.formatNanos((long) r.getAvgLatencyNanos()),
                        TimeUtils.formatNanos((long) r.getPercentile(95)),
                        String.valueOf(r.getTotalTasks())
                })
                .collect(Collectors.toList());

        builder.addTable("Results", headers, rows);

        // 파일 저장
        builder.saveToFile(filename);
    }

    /**
     * 테스트 메인 메서드
     */
    public static void main(String[] args) {
        System.out.println("=== ThroughputAnalyzer 테스트 ===\n");

        ThroughputAnalyzer analyzer = new ThroughputAnalyzer();

        // 테스트 작업 정의
        Map<String, Task> tasks = new LinkedHashMap<>();

        // 1. 간단한 계산 작업
        tasks.put("Simple Calculation", () -> {
            int sum = 0;
            for (int i = 0; i < 1000; i++) {
                sum += i;
            }
        });

        // 2. 문자열 연산
        tasks.put("String Operations", () -> {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 100; i++) {
                sb.append("test").append(i);
            }
        });

        // 3. 리스트 연산
        tasks.put("List Operations", () -> {
            List<Integer> list = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                list.add(i);
            }
            Collections.sort(list);
        });

        // 4. Sleep 작업
        tasks.put("Sleep Task", () -> {
            Thread.sleep(1);
        });

        try {
            // 1. 기본 처리량 측정
            System.out.println("1. 기본 처리량 측정 (1000 iterations):");
            List<ThroughputResult> results = analyzer.compareAll(tasks, 1000);
            results.forEach(System.out::println);

            // 2. 동시성 처리량 측정
            System.out.println("\n2. 동시성 처리량 측정 (10000 tasks, 4 threads):");
            ThroughputResult concurrentResult = analyzer.measureConcurrentThroughput(
                    "Concurrent Calculation",
                    tasks.get("Simple Calculation"),
                    10000,
                    4
            );
            System.out.println(concurrentResult);

            // 3. 워밍업 후 측정
            System.out.println("\n3. 워밍업 후 측정:");
            ThroughputResult warmupResult = analyzer.measureWithWarmup(
                    "Warmed-up Calculation",
                    tasks.get("Simple Calculation"),
                    100,    // 워밍업
                    1000    // 측정
            );
            System.out.println(warmupResult);

            // 4. 부하 테스트
            System.out.println("\n4. 부하 테스트 (100 ~ 1000, step 100):");
            List<ThroughputResult> loadResults = analyzer.loadTest(
                    "Load Test",
                    tasks.get("Simple Calculation"),
                    100, 1000, 100
            );
            loadResults.forEach(System.out::println);

            // 5. 지속 시간 기반 측정
            System.out.println("\n5. 지속 시간 기반 측정 (5초):");
            ThroughputResult durationResult = analyzer.measureForDuration(
                    "5-Second Test",
                    tasks.get("Simple Calculation"),
                    5000
            );
            System.out.println(durationResult);

            // 6. 비교 리포트
            System.out.println("\n6. 비교 리포트:");
            System.out.println(analyzer.generateComparisonReport(results));

            // 7. HTML 리포트 생성
            System.out.println("\n7. HTML 리포트 생성:");
            analyzer.generateHtmlReport(results, "throughput_report.html");
            System.out.println("리포트 저장: throughput_report.html");

        } catch (Exception e) {
            System.err.println("테스트 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("\n테스트 완료!");
    }
}