package com.study.common.util;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 시간 측정 유틸리티 클래스
 * 나노초 단위 정밀 시간 측정 및 성능 벤치마킹 지원
 */
public class TimeUtils {

    /**
     * 코드 실행 시간을 측정하는 내부 클래스
     */
    public static class Timer {
        private long startTime;
        private long endTime;
        private boolean isRunning;
        private final String label;

        public Timer() {
            this("Timer");
        }

        public Timer(String label) {
            this.label = label;
            this.isRunning = false;
        }

        /**
         * 타이머 시작
         */
        public Timer start() {
            this.startTime = System.nanoTime();
            this.isRunning = true;
            return this;
        }

        /**
         * 타이머 중지
         */
        public Timer stop() {
            this.endTime = System.nanoTime();
            this.isRunning = false;
            return this;
        }

        /**
         * 경과 시간 (나노초)
         */
        public long getElapsedNanos() {
            if (isRunning) {
                return System.nanoTime() - startTime;
            }
            return endTime - startTime;
        }

        /**
         * 경과 시간 (마이크로초)
         */
        public long getElapsedMicros() {
            return TimeUnit.NANOSECONDS.toMicros(getElapsedNanos());
        }

        /**
         * 경과 시간 (밀리초)
         */
        public long getElapsedMillis() {
            return TimeUnit.NANOSECONDS.toMillis(getElapsedNanos());
        }

        /**
         * 경과 시간 (초)
         */
        public double getElapsedSeconds() {
            return getElapsedNanos() / 1_000_000_000.0;
        }

        /**
         * 포맷팅된 경과 시간
         */
        public String getFormattedElapsedTime() {
            long nanos = getElapsedNanos();
            return formatNanos(nanos);
        }

        /**
         * 결과 출력
         */
        public void printElapsed() {
            System.out.printf("[%s] Elapsed: %s%n", label, getFormattedElapsedTime());
        }

        /**
         * 타이머 리셋
         */
        public Timer reset() {
            this.startTime = 0;
            this.endTime = 0;
            this.isRunning = false;
            return this;
        }
    }

    /**
     * 코드 블록 실행 시간 측정
     *
     * @param runnable 실행할 코드
     * @return 경과 시간 (나노초)
     */
    public static long measureNanos(Runnable runnable) {
        long start = System.nanoTime();
        runnable.run();
        return System.nanoTime() - start;
    }

    /**
     * 코드 블록 실행 시간 측정 (밀리초)
     */
    public static long measureMillis(Runnable runnable) {
        return TimeUnit.NANOSECONDS.toMillis(measureNanos(runnable));
    }

    /**
     * 코드 블록 실행 시간 측정 (초)
     */
    public static double measureSeconds(Runnable runnable) {
        return measureNanos(runnable) / 1_000_000_000.0;
    }

    /**
     * 함수 실행 시간 측정 및 결과 반환
     */
    public static <T> TimedResult<T> measureWithResult(Supplier<T> supplier) {
        long start = System.nanoTime();
        T result = supplier.get();
        long elapsed = System.nanoTime() - start;
        return new TimedResult<>(result, elapsed);
    }

    /**
     * 측정된 결과를 담는 클래스
     */
    public static class TimedResult<T> {
        private final T result;
        private final long elapsedNanos;

        public TimedResult(T result, long elapsedNanos) {
            this.result = result;
            this.elapsedNanos = elapsedNanos;
        }

        public T getResult() {
            return result;
        }

        public long getElapsedNanos() {
            return elapsedNanos;
        }

        public long getElapsedMillis() {
            return TimeUnit.NANOSECONDS.toMillis(elapsedNanos);
        }

        public double getElapsedSeconds() {
            return elapsedNanos / 1_000_000_000.0;
        }

        public String getFormattedElapsed() {
            return formatNanos(elapsedNanos);
        }
    }

    /**
     * 평균 실행 시간 측정
     *
     * @param runnable 실행할 코드
     * @param iterations 반복 횟수
     * @return 평균 실행 시간 (나노초)
     */
    public static long measureAverageNanos(Runnable runnable, int iterations) {
        if (iterations <= 0) {
            throw new IllegalArgumentException("Iterations must be positive");
        }

        long totalTime = 0;
        for (int i = 0; i < iterations; i++) {
            totalTime += measureNanos(runnable);
        }
        return totalTime / iterations;
    }

    /**
     * 워밍업 후 평균 실행 시간 측정
     * JVM 최적화를 위해 워밍업 실행 후 측정
     *
     * @param runnable 실행할 코드
     * @param warmupIterations 워밍업 반복 횟수
     * @param measureIterations 측정 반복 횟수
     * @return 측정 통계
     */
    public static BenchmarkResult benchmark(Runnable runnable, int warmupIterations, int measureIterations) {
        // 워밍업
        for (int i = 0; i < warmupIterations; i++) {
            runnable.run();
        }

        // 측정
        long[] times = new long[measureIterations];
        for (int i = 0; i < measureIterations; i++) {
            times[i] = measureNanos(runnable);
        }

        return new BenchmarkResult(times);
    }

    /**
     * 벤치마크 결과를 담는 클래스
     */
    public static class BenchmarkResult {
        private final long[] times;
        private final long min;
        private final long max;
        private final long average;
        private final double median;
        private final double stdDev;

        public BenchmarkResult(long[] times) {
            this.times = times.clone();
            this.min = calculateMin(times);
            this.max = calculateMax(times);
            this.average = calculateAverage(times);
            this.median = calculateMedian(times);
            this.stdDev = calculateStdDev(times, average);
        }

        private long calculateMin(long[] times) {
            long min = Long.MAX_VALUE;
            for (long time : times) {
                if (time < min) min = time;
            }
            return min;
        }

        private long calculateMax(long[] times) {
            long max = Long.MIN_VALUE;
            for (long time : times) {
                if (time > max) max = time;
            }
            return max;
        }

        private long calculateAverage(long[] times) {
            long sum = 0;
            for (long time : times) {
                sum += time;
            }
            return sum / times.length;
        }

        private double calculateMedian(long[] times) {
            long[] sorted = times.clone();
            java.util.Arrays.sort(sorted);
            int mid = sorted.length / 2;
            if (sorted.length % 2 == 0) {
                return (sorted[mid - 1] + sorted[mid]) / 2.0;
            }
            return sorted[mid];
        }

        private double calculateStdDev(long[] times, long average) {
            double sumSquaredDiff = 0;
            for (long time : times) {
                double diff = time - average;
                sumSquaredDiff += diff * diff;
            }
            return Math.sqrt(sumSquaredDiff / times.length);
        }

        public long getMin() { return min; }
        public long getMax() { return max; }
        public long getAverage() { return average; }
        public double getMedian() { return median; }
        public double getStdDev() { return stdDev; }
        public int getSampleCount() { return times.length; }

        public String getFormattedReport() {
            StringBuilder sb = new StringBuilder();
            sb.append("Benchmark Results:\n");
            sb.append(String.format("  Samples:  %d\n", getSampleCount()));
            sb.append(String.format("  Min:      %s\n", formatNanos(min)));
            sb.append(String.format("  Max:      %s\n", formatNanos(max)));
            sb.append(String.format("  Average:  %s\n", formatNanos(average)));
            sb.append(String.format("  Median:   %s\n", formatNanos((long) median)));
            sb.append(String.format("  StdDev:   %s\n", formatNanos((long) stdDev)));
            return sb.toString();
        }
    }

    /**
     * 나노초를 읽기 좋은 형식으로 변환
     */
    public static String formatNanos(long nanos) {
        if (nanos < 1_000) {
            return String.format("%d ns", nanos);
        } else if (nanos < 1_000_000) {
            return String.format("%.2f μs", nanos / 1_000.0);
        } else if (nanos < 1_000_000_000) {
            return String.format("%.2f ms", nanos / 1_000_000.0);
        } else {
            return String.format("%.3f s", nanos / 1_000_000_000.0);
        }
    }

    /**
     * Duration을 읽기 좋은 형식으로 변환
     */
    public static String formatDuration(Duration duration) {
        return formatNanos(duration.toNanos());
    }

    /**
     * 현재 타임스탬프 (밀리초)
     */
    public static long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    /**
     * 현재 나노 시간
     */
    public static long nanoTime() {
        return System.nanoTime();
    }

    /**
     * 현재 Instant
     */
    public static Instant now() {
        return Instant.now();
    }

    /**
     * Sleep (예외 처리 포함)
     */
    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 테스트 메인 메서드
     */
    public static void main(String[] args) {
        System.out.println("=== TimeUtils 테스트 ===\n");

        // 1. Timer 사용
        System.out.println("1. Timer 테스트:");
        Timer timer = new Timer("Sample Task");
        timer.start();
        sleep(100);
        timer.stop();
        timer.printElapsed();

        // 2. measureNanos 사용
        System.out.println("\n2. measureNanos 테스트:");
        long elapsed = measureNanos(() -> {
            int sum = 0;
            for (int i = 0; i < 1_000_000; i++) {
                sum += i;
            }
        });
        System.out.printf("Elapsed: %s%n", formatNanos(elapsed));

        // 3. measureWithResult 사용
        System.out.println("\n3. measureWithResult 테스트:");
        TimedResult<Integer> result = measureWithResult(() -> {
            int sum = 0;
            for (int i = 0; i < 1_000_000; i++) {
                sum += i;
            }
            return sum;
        });
        System.out.printf("Result: %d, Time: %s%n",
                result.getResult(), result.getFormattedElapsed());

        // 4. benchmark 사용
        System.out.println("\n4. Benchmark 테스트:");
        BenchmarkResult benchResult = benchmark(
                () -> {
                    int sum = 0;
                    for (int i = 0; i < 100_000; i++) {
                        sum += i;
                    }
                },
                10,  // 워밍업
                100  // 측정
        );
        System.out.println(benchResult.getFormattedReport());

        // 5. 다양한 시간 포맷 테스트
        System.out.println("\n5. 시간 포맷 테스트:");
        System.out.println("500 ns: " + formatNanos(500));
        System.out.println("1500 ns: " + formatNanos(1_500));
        System.out.println("1.5 ms: " + formatNanos(1_500_000));
        System.out.println("1.5 s: " + formatNanos(1_500_000_000L));
    }
}