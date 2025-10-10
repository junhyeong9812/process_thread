package com.study.comparison.performance;


import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

/**
 * 컨텍스트 스위칭 벤치마크
 *
 * 컨텍스트 스위칭(Context Switching) 오버헤드를 측정합니다:
 * - Platform Thread: OS 레벨 컨텍스트 스위칭 (무겁고 느림)
 * - Virtual Thread: JVM 레벨 컨텍스트 스위칭 (가볍고 빠름)
 * - Thread Pool: 재사용으로 스위칭 최소화
 *
 * 측정 방법:
 * 1. Ping-Pong 패턴: 두 스레드가 번갈아 실행
 * 2. Producer-Consumer: 블로킹으로 인한 스위칭
 * 3. Sleep/Wake 패턴: 대기 상태 전환
 * 4. Lock Contention: 락 경쟁으로 인한 스위칭
 *
 * 컨텍스트 스위칭이란?
 * - CPU가 한 스레드에서 다른 스레드로 실행을 전환하는 과정
 * - 레지스터, 스택, 메모리 맵 등을 저장/복원
 * - Platform Thread: 1-10 마이크로초 (OS 의존적)
 * - Virtual Thread: 수십 나노초 (JVM 관리)
 */
public class ContextSwitchBenchmark {

    private static final int WARMUP_ITERATIONS = 3;
    private static final int MEASUREMENT_ITERATIONS = 5;

    /**
     * 벤치마크 결과
     */
    static class BenchmarkResult {
        final String method;
        final String pattern;
        final int switchCount;
        final long totalTimeNanos;
        final double avgSwitchTimeNanos;
        final long switchesPerSecond;

        BenchmarkResult(String method, String pattern, int switchCount, long totalTimeNanos) {
            this.method = method;
            this.pattern = pattern;
            this.switchCount = switchCount;
            this.totalTimeNanos = totalTimeNanos;
            this.avgSwitchTimeNanos = (double) totalTimeNanos / switchCount;
            this.switchesPerSecond = (long) ((switchCount / (totalTimeNanos / 1_000_000_000.0)));
        }

        void print() {
            System.out.println("\n  === " + method + " - " + pattern + " ===");
            System.out.println("  Total switches: " + String.format("%,d", switchCount));
            System.out.println("  Total time: " + String.format("%.3f ms", totalTimeNanos / 1_000_000.0));
            System.out.println("  Avg per switch: " + String.format("%.0f ns", avgSwitchTimeNanos));
            System.out.println("  Switches/sec: " + String.format("%,d", switchesPerSecond));
        }
    }

    /**
     * 1. Ping-Pong 패턴
     * 두 스레드가 번갈아가며 실행되는 패턴
     */
    static class PingPongBenchmark {

        static class PingPongCounter {
            private volatile int value = 0;
            private final int maxCount;
            private final Object lock = new Object();
            private volatile boolean pingTurn = true;

            PingPongCounter(int maxCount) {
                this.maxCount = maxCount;
            }

            void ping() throws InterruptedException {
                while (value < maxCount) {
                    synchronized (lock) {
                        while (!pingTurn && value < maxCount) {
                            lock.wait();
                        }
                        if (value >= maxCount) break;

                        value++;
                        pingTurn = false;
                        lock.notifyAll();
                    }
                }
            }

            void pong() throws InterruptedException {
                while (value < maxCount) {
                    synchronized (lock) {
                        while (pingTurn && value < maxCount) {
                            lock.wait();
                        }
                        if (value >= maxCount) break;

                        value++;
                        pingTurn = true;
                        lock.notifyAll();
                    }
                }
            }
        }

        public BenchmarkResult measurePlatform(int switchCount) throws Exception {
            PingPongCounter counter = new PingPongCounter(switchCount);

            Instant start = Instant.now();

            Thread ping = Thread.ofPlatform().start(() -> {
                try {
                    counter.ping();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });

            Thread pong = Thread.ofPlatform().start(() -> {
                try {
                    counter.pong();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });

            ping.join();
            pong.join();

            long duration = Duration.between(start, Instant.now()).toNanos();

            return new BenchmarkResult("Platform Thread", "Ping-Pong", switchCount, duration);
        }

        public BenchmarkResult measureVirtual(int switchCount) throws Exception {
            PingPongCounter counter = new PingPongCounter(switchCount);

            Instant start = Instant.now();

            Thread ping = Thread.ofVirtual().start(() -> {
                try {
                    counter.ping();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });

            Thread pong = Thread.ofVirtual().start(() -> {
                try {
                    counter.pong();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });

            ping.join();
            pong.join();

            long duration = Duration.between(start, Instant.now()).toNanos();

            return new BenchmarkResult("Virtual Thread", "Ping-Pong", switchCount, duration);
        }
    }

    /**
     * 2. Producer-Consumer 패턴
     * BlockingQueue를 사용한 컨텍스트 스위칭
     */
    static class ProducerConsumerBenchmark {

        public BenchmarkResult measurePlatform(int itemCount) throws Exception {
            BlockingQueue<Integer> queue = new LinkedBlockingQueue<>(10);
            AtomicInteger consumed = new AtomicInteger(0);

            Instant start = Instant.now();

            Thread producer = Thread.ofPlatform().start(() -> {
                try {
                    for (int i = 0; i < itemCount; i++) {
                        queue.put(i);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });

            Thread consumer = Thread.ofPlatform().start(() -> {
                try {
                    for (int i = 0; i < itemCount; i++) {
                        queue.take();
                        consumed.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });

            producer.join();
            consumer.join();

            long duration = Duration.between(start, Instant.now()).toNanos();

            // 각 put/take에서 스위칭 발생 = itemCount * 2
            return new BenchmarkResult("Platform Thread", "Producer-Consumer",
                    itemCount * 2, duration);
        }

        public BenchmarkResult measureVirtual(int itemCount) throws Exception {
            BlockingQueue<Integer> queue = new LinkedBlockingQueue<>(10);
            AtomicInteger consumed = new AtomicInteger(0);

            Instant start = Instant.now();

            Thread producer = Thread.ofVirtual().start(() -> {
                try {
                    for (int i = 0; i < itemCount; i++) {
                        queue.put(i);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });

            Thread consumer = Thread.ofVirtual().start(() -> {
                try {
                    for (int i = 0; i < itemCount; i++) {
                        queue.take();
                        consumed.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });

            producer.join();
            consumer.join();

            long duration = Duration.between(start, Instant.now()).toNanos();

            return new BenchmarkResult("Virtual Thread", "Producer-Consumer",
                    itemCount * 2, duration);
        }
    }

    /**
     * 3. Sleep/Wake 패턴
     * 짧은 sleep으로 인한 컨텍스트 스위칭
     */
    static class SleepWakeBenchmark {

        public BenchmarkResult measurePlatform(int iterations) throws Exception {
            AtomicInteger counter = new AtomicInteger(0);

            Instant start = Instant.now();

            Thread thread = Thread.ofPlatform().start(() -> {
                for (int i = 0; i < iterations; i++) {
                    try {
                        Thread.sleep(0, 1000); // 1 마이크로초
                        counter.incrementAndGet();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });

            thread.join();

            long duration = Duration.between(start, Instant.now()).toNanos();

            return new BenchmarkResult("Platform Thread", "Sleep-Wake", iterations, duration);
        }

        public BenchmarkResult measureVirtual(int iterations) throws Exception {
            AtomicInteger counter = new AtomicInteger(0);

            Instant start = Instant.now();

            Thread thread = Thread.ofVirtual().start(() -> {
                for (int i = 0; i < iterations; i++) {
                    try {
                        Thread.sleep(0, 1000); // 1 마이크로초
                        counter.incrementAndGet();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });

            thread.join();

            long duration = Duration.between(start, Instant.now()).toNanos();

            return new BenchmarkResult("Virtual Thread", "Sleep-Wake", iterations, duration);
        }
    }

    /**
     * 4. Lock Contention 패턴
     * 락 경쟁으로 인한 컨텍스트 스위칭
     */
    static class LockContentionBenchmark {

        static class SharedResource {
            private final Object lock = new Object();
            private int value = 0;

            void increment() {
                synchronized (lock) {
                    value++;
                    // 짧은 critical section
                    for (int i = 0; i < 10; i++) {
                        value += i;
                    }
                }
            }

            int getValue() {
                synchronized (lock) {
                    return value;
                }
            }
        }

        public BenchmarkResult measurePlatform(int iterations, int threadCount) throws Exception {
            SharedResource resource = new SharedResource();
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);

            List<Thread> threads = new ArrayList<>();
            int iterationsPerThread = iterations / threadCount;

            for (int i = 0; i < threadCount; i++) {
                Thread thread = Thread.ofPlatform().start(() -> {
                    try {
                        startLatch.await();
                        for (int j = 0; j < iterationsPerThread; j++) {
                            resource.increment();
                        }
                        doneLatch.countDown();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
                threads.add(thread);
            }

            Instant start = Instant.now();
            startLatch.countDown(); // 모든 스레드 동시 시작

            doneLatch.await();
            long duration = Duration.between(start, Instant.now()).toNanos();

            for (Thread thread : threads) {
                thread.join();
            }

            return new BenchmarkResult("Platform Thread", "Lock Contention (" + threadCount + " threads)",
                    iterations, duration);
        }

        public BenchmarkResult measureVirtual(int iterations, int threadCount) throws Exception {
            SharedResource resource = new SharedResource();
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);

            List<Thread> threads = new ArrayList<>();
            int iterationsPerThread = iterations / threadCount;

            for (int i = 0; i < threadCount; i++) {
                Thread thread = Thread.ofVirtual().start(() -> {
                    try {
                        startLatch.await();
                        for (int j = 0; j < iterationsPerThread; j++) {
                            resource.increment();
                        }
                        doneLatch.countDown();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
                threads.add(thread);
            }

            Instant start = Instant.now();
            startLatch.countDown();

            doneLatch.await();
            long duration = Duration.between(start, Instant.now()).toNanos();

            for (Thread thread : threads) {
                thread.join();
            }

            return new BenchmarkResult("Virtual Thread", "Lock Contention (" + threadCount + " threads)",
                    iterations, duration);
        }
    }

    /**
     * 5. Yield 패턴
     * 명시적 양보로 인한 스위칭
     */
    static class YieldBenchmark {

        public BenchmarkResult measurePlatform(int iterations) throws Exception {
            AtomicInteger counter = new AtomicInteger(0);
            int threadCount = Runtime.getRuntime().availableProcessors();
            CountDownLatch latch = new CountDownLatch(threadCount);

            Instant start = Instant.now();

            List<Thread> threads = new ArrayList<>();
            for (int i = 0; i < threadCount; i++) {
                Thread thread = Thread.ofPlatform().start(() -> {
                    for (int j = 0; j < iterations / threadCount; j++) {
                        counter.incrementAndGet();
                        Thread.yield(); // 명시적 양보
                    }
                    latch.countDown();
                });
                threads.add(thread);
            }

            latch.await();
            long duration = Duration.between(start, Instant.now()).toNanos();

            for (Thread thread : threads) {
                thread.join();
            }

            return new BenchmarkResult("Platform Thread", "Yield Pattern", iterations, duration);
        }

        public BenchmarkResult measureVirtual(int iterations) throws Exception {
            AtomicInteger counter = new AtomicInteger(0);
            int threadCount = Runtime.getRuntime().availableProcessors() * 2;
            CountDownLatch latch = new CountDownLatch(threadCount);

            Instant start = Instant.now();

            List<Thread> threads = new ArrayList<>();
            for (int i = 0; i < threadCount; i++) {
                Thread thread = Thread.ofVirtual().start(() -> {
                    for (int j = 0; j < iterations / threadCount; j++) {
                        counter.incrementAndGet();
                        Thread.yield();
                    }
                    latch.countDown();
                });
                threads.add(thread);
            }

            latch.await();
            long duration = Duration.between(start, Instant.now()).toNanos();

            for (Thread thread : threads) {
                thread.join();
            }

            return new BenchmarkResult("Virtual Thread", "Yield Pattern", iterations, duration);
        }
    }

    /**
     * 벤치마크 실행
     */
    static class BenchmarkRunner {

        public void runAllBenchmarks() throws Exception {
            System.out.println("\n╔═══════════════════════════════════════════════════════════════════╗");
            System.out.println("║      CONTEXT SWITCHING BENCHMARK                                  ║");
            System.out.println("╚═══════════════════════════════════════════════════════════════════╝");

            List<BenchmarkResult> allResults = new ArrayList<>();

            // Warmup
            System.out.println("\n[Warmup Phase]");
            for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                System.out.print("  Warmup iteration " + (i + 1) + "/" + WARMUP_ITERATIONS + "...");
                new PingPongBenchmark().measurePlatform(100);
                System.out.println(" Done");
                System.gc();
                Thread.sleep(200);
            }

            System.out.println("\n[Measurement Phase]");

            // 1. Ping-Pong Pattern
            System.out.println("\n1. Ping-Pong Pattern (10,000 switches)");
            System.out.println("   Two threads alternating execution");
            System.out.println("   " + "─".repeat(60));
            allResults.addAll(runPingPongBenchmark());

            // 2. Producer-Consumer Pattern
            System.out.println("\n2. Producer-Consumer Pattern (10,000 items)");
            System.out.println("   Blocking queue operations");
            System.out.println("   " + "─".repeat(60));
            allResults.addAll(runProducerConsumerBenchmark());

            // 3. Sleep-Wake Pattern
            System.out.println("\n3. Sleep-Wake Pattern (1,000 sleeps)");
            System.out.println("   Short sleep durations");
            System.out.println("   " + "─".repeat(60));
            allResults.addAll(runSleepWakeBenchmark());

            // 4. Lock Contention Pattern
            System.out.println("\n4. Lock Contention Pattern (10,000 ops)");
            System.out.println("   Multiple threads competing for lock");
            System.out.println("   " + "─".repeat(60));
            allResults.addAll(runLockContentionBenchmark());

            // 5. Yield Pattern
            System.out.println("\n5. Yield Pattern (10,000 yields)");
            System.out.println("   Explicit thread yielding");
            System.out.println("   " + "─".repeat(60));
            allResults.addAll(runYieldBenchmark());

            // 종합 분석
            printSummary(allResults);
        }

        private List<BenchmarkResult> runPingPongBenchmark() throws Exception {
            List<BenchmarkResult> results = new ArrayList<>();
            PingPongBenchmark benchmark = new PingPongBenchmark();

            // Platform Thread
            List<BenchmarkResult> platformResults = new ArrayList<>();
            for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
                platformResults.add(benchmark.measurePlatform(10000));
                System.gc();
                Thread.sleep(100);
            }
            results.add(getMedianResult(platformResults));

            // Virtual Thread
            List<BenchmarkResult> virtualResults = new ArrayList<>();
            for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
                virtualResults.add(benchmark.measureVirtual(10000));
                System.gc();
                Thread.sleep(100);
            }
            results.add(getMedianResult(virtualResults));

            for (BenchmarkResult result : results) {
                result.print();
            }

            return results;
        }

        private List<BenchmarkResult> runProducerConsumerBenchmark() throws Exception {
            List<BenchmarkResult> results = new ArrayList<>();
            ProducerConsumerBenchmark benchmark = new ProducerConsumerBenchmark();

            List<BenchmarkResult> platformResults = new ArrayList<>();
            for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
                platformResults.add(benchmark.measurePlatform(10000));
                System.gc();
                Thread.sleep(100);
            }
            results.add(getMedianResult(platformResults));

            List<BenchmarkResult> virtualResults = new ArrayList<>();
            for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
                virtualResults.add(benchmark.measureVirtual(10000));
                System.gc();
                Thread.sleep(100);
            }
            results.add(getMedianResult(virtualResults));

            for (BenchmarkResult result : results) {
                result.print();
            }

            return results;
        }

        private List<BenchmarkResult> runSleepWakeBenchmark() throws Exception {
            List<BenchmarkResult> results = new ArrayList<>();
            SleepWakeBenchmark benchmark = new SleepWakeBenchmark();

            List<BenchmarkResult> platformResults = new ArrayList<>();
            for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
                platformResults.add(benchmark.measurePlatform(1000));
                System.gc();
                Thread.sleep(100);
            }
            results.add(getMedianResult(platformResults));

            List<BenchmarkResult> virtualResults = new ArrayList<>();
            for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
                virtualResults.add(benchmark.measureVirtual(1000));
                System.gc();
                Thread.sleep(100);
            }
            results.add(getMedianResult(virtualResults));

            for (BenchmarkResult result : results) {
                result.print();
            }

            return results;
        }

        private List<BenchmarkResult> runLockContentionBenchmark() throws Exception {
            List<BenchmarkResult> results = new ArrayList<>();
            LockContentionBenchmark benchmark = new LockContentionBenchmark();

            int threadCount = 4;

            List<BenchmarkResult> platformResults = new ArrayList<>();
            for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
                platformResults.add(benchmark.measurePlatform(10000, threadCount));
                System.gc();
                Thread.sleep(100);
            }
            results.add(getMedianResult(platformResults));

            List<BenchmarkResult> virtualResults = new ArrayList<>();
            for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
                virtualResults.add(benchmark.measureVirtual(10000, threadCount));
                System.gc();
                Thread.sleep(100);
            }
            results.add(getMedianResult(virtualResults));

            for (BenchmarkResult result : results) {
                result.print();
            }

            return results;
        }

        private List<BenchmarkResult> runYieldBenchmark() throws Exception {
            List<BenchmarkResult> results = new ArrayList<>();
            YieldBenchmark benchmark = new YieldBenchmark();

            List<BenchmarkResult> platformResults = new ArrayList<>();
            for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
                platformResults.add(benchmark.measurePlatform(10000));
                System.gc();
                Thread.sleep(100);
            }
            results.add(getMedianResult(platformResults));

            List<BenchmarkResult> virtualResults = new ArrayList<>();
            for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
                virtualResults.add(benchmark.measureVirtual(10000));
                System.gc();
                Thread.sleep(100);
            }
            results.add(getMedianResult(virtualResults));

            for (BenchmarkResult result : results) {
                result.print();
            }

            return results;
        }

        private BenchmarkResult getMedianResult(List<BenchmarkResult> results) {
            results.sort((a, b) -> Long.compare(a.totalTimeNanos, b.totalTimeNanos));
            return results.get(results.size() / 2);
        }

        private void printSummary(List<BenchmarkResult> results) {
            System.out.println("\n" + "═".repeat(70));
            System.out.println("  SUMMARY - Platform vs Virtual Thread Context Switching");
            System.out.println("═".repeat(70));

            // 패턴별로 그룹화
            results.stream()
                    .collect(java.util.stream.Collectors.groupingBy(r -> r.pattern))
                    .forEach((pattern, patternResults) -> {
                        System.out.println("\n  " + pattern + ":");

                        BenchmarkResult platform = patternResults.stream()
                                .filter(r -> r.method.contains("Platform"))
                                .findFirst()
                                .orElse(null);

                        BenchmarkResult virtual = patternResults.stream()
                                .filter(r -> r.method.contains("Virtual"))
                                .findFirst()
                                .orElse(null);

                        if (platform != null && virtual != null) {
                            double speedup = (double) platform.avgSwitchTimeNanos /
                                    virtual.avgSwitchTimeNanos;

                            System.out.println(String.format("    Platform: %.0f ns/switch",
                                    platform.avgSwitchTimeNanos));
                            System.out.println(String.format("    Virtual:  %.0f ns/switch (%.2fx faster)",
                                    virtual.avgSwitchTimeNanos, speedup));
                        }
                    });

            System.out.println("\n" + "═".repeat(70));
            System.out.println("  KEY INSIGHTS");
            System.out.println("═".repeat(70));
            System.out.println("  • Virtual Threads have significantly lower context switch overhead");
            System.out.println("  • Platform Threads involve OS-level scheduling (slower)");
            System.out.println("  • Virtual Threads are managed by JVM (faster)");
            System.out.println("  • Best use case: High I/O, frequent blocking operations");
            System.out.println("═".repeat(70));
        }
    }

    public static void main(String[] args) throws Exception {
        BenchmarkRunner runner = new BenchmarkRunner();
        runner.runAllBenchmarks();

        System.out.println("\n╔═══════════════════════════════════════════════════════════════════╗");
        System.out.println("║         BENCHMARK COMPLETED                                       ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════╝");
    }
}