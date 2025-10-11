package com.study.comparison.concurrency;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 동시성 패턴 비교 (Concurrency Patterns Comparison)
 *
 * 다양한 동시성 패턴의 성능과 특성을 비교 분석합니다:
 * 1. Producer-Consumer: 생산자-소비자
 * 2. Reader-Writer: 읽기-쓰기 분리
 * 3. Thread Pool: 스레드 풀
 * 4. Future/Promise: 비동기 결과
 * 5. Fork-Join: 분할 정복
 * 6. Actor Model: 메시지 전달
 * 7. Work Stealing: 작업 훔치기
 * 8. Barrier: 동기화 장벽
 *
 * 비교 항목:
 * - Performance: 성능
 * - Scalability: 확장성
 * - Complexity: 복잡도
 * - Use Cases: 사용 사례
 */
public class ConcurrencyPatterns {

    /**
     * 패턴 비교 결과
     */
    static class PatternResult {
        final String patternName;
        final long executionTimeMs;
        final int taskCount;
        final double throughput;
        final boolean successful;
        final Map<String, Object> metrics;

        PatternResult(String patternName, long executionTimeMs, int taskCount,
                      boolean successful, Map<String, Object> metrics) {
            this.patternName = patternName;
            this.executionTimeMs = executionTimeMs;
            this.taskCount = taskCount;
            this.throughput = executionTimeMs > 0 ?
                    (taskCount * 1000.0) / executionTimeMs : 0;
            this.successful = successful;
            this.metrics = metrics != null ? metrics : new HashMap<>();
        }

        void print() {
            System.out.println("\n  === " + patternName + " ===");
            System.out.println("  Execution time: " + String.format("%,d ms", executionTimeMs));
            System.out.println("  Tasks: " + String.format("%,d", taskCount));
            System.out.println("  Throughput: " + String.format("%.2f tasks/sec", throughput));
            System.out.println("  Status: " + (successful ? "✓ Success" : "✗ Failed"));

            if (!metrics.isEmpty()) {
                System.out.println("  Metrics:");
                metrics.forEach((k, v) -> System.out.println("    " + k + ": " + v));
            }
        }
    }

    /**
     * 1. Producer-Consumer Pattern
     */
    static class ProducerConsumerPattern {

        static class Task {
            final int id;
            final int processingTimeMs;

            Task(int id, int processingTimeMs) {
                this.id = id;
                this.processingTimeMs = processingTimeMs;
            }
        }

        public PatternResult test(int producers, int consumers, int tasksPerProducer)
                throws Exception {
            BlockingQueue<Task> queue = new LinkedBlockingQueue<>(100);
            AtomicInteger producedCount = new AtomicInteger(0);
            AtomicInteger consumedCount = new AtomicInteger(0);
            AtomicInteger poisonPillsSent = new AtomicInteger(0);

            int totalTasks = producers * tasksPerProducer;

            Instant start = Instant.now();

            // Producers
            List<Thread> producerThreads = new ArrayList<>();
            for (int i = 0; i < producers; i++) {
                final int producerId = i;
                Thread producer = Thread.ofVirtual().start(() -> {
                    try {
                        for (int j = 0; j < tasksPerProducer; j++) {
                            Task task = new Task(producerId * tasksPerProducer + j, 5);
                            queue.put(task);
                            producedCount.incrementAndGet();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
                producerThreads.add(producer);
            }

            // Wait for producers to finish
            for (Thread producer : producerThreads) {
                producer.join();
            }

            // Send poison pills
            for (int i = 0; i < consumers; i++) {
                queue.put(new Task(-1, 0)); // Poison pill
                poisonPillsSent.incrementAndGet();
            }

            // Consumers
            List<Thread> consumerThreads = new ArrayList<>();
            for (int i = 0; i < consumers; i++) {
                Thread consumer = Thread.ofVirtual().start(() -> {
                    try {
                        while (true) {
                            Task task = queue.take();
                            if (task.id == -1) { // Poison pill
                                break;
                            }
                            Thread.sleep(task.processingTimeMs);
                            consumedCount.incrementAndGet();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
                consumerThreads.add(consumer);
            }

            // Wait for consumers
            for (Thread consumer : consumerThreads) {
                consumer.join();
            }

            long duration = Duration.between(start, Instant.now()).toMillis();

            Map<String, Object> metrics = new HashMap<>();
            metrics.put("Producers", producers);
            metrics.put("Consumers", consumers);
            metrics.put("Queue size", queue.remainingCapacity());
            metrics.put("Produced", producedCount.get());
            metrics.put("Consumed", consumedCount.get());

            return new PatternResult(
                    "Producer-Consumer",
                    duration,
                    totalTasks,
                    consumedCount.get() == totalTasks,
                    metrics
            );
        }
    }

    /**
     * 2. Reader-Writer Pattern
     */
    static class ReaderWriterPattern {

        static class SharedData {
            private int value = 0;
            private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
            private final Lock readLock = rwLock.readLock();
            private final Lock writeLock = rwLock.writeLock();

            public int read() {
                readLock.lock();
                try {
                    Thread.sleep(5); // Simulate read
                    return value;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return -1;
                } finally {
                    readLock.unlock();
                }
            }

            public void write(int newValue) {
                writeLock.lock();
                try {
                    Thread.sleep(10); // Simulate write
                    value = newValue;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    writeLock.unlock();
                }
            }
        }

        public PatternResult test(int readers, int writers, int operationsPerThread)
                throws Exception {
            SharedData data = new SharedData();
            CountDownLatch latch = new CountDownLatch(readers + writers);
            AtomicInteger readCount = new AtomicInteger(0);
            AtomicInteger writeCount = new AtomicInteger(0);

            Instant start = Instant.now();

            List<Thread> threads = new ArrayList<>();

            // Readers
            for (int i = 0; i < readers; i++) {
                Thread reader = Thread.ofVirtual().start(() -> {
                    for (int j = 0; j < operationsPerThread; j++) {
                        data.read();
                        readCount.incrementAndGet();
                    }
                    latch.countDown();
                });
                threads.add(reader);
            }

            // Writers
            for (int i = 0; i < writers; i++) {
                final int writerId = i;
                Thread writer = Thread.ofVirtual().start(() -> {
                    for (int j = 0; j < operationsPerThread; j++) {
                        data.write(writerId * 1000 + j);
                        writeCount.incrementAndGet();
                    }
                    latch.countDown();
                });
                threads.add(writer);
            }

            latch.await();
            long duration = Duration.between(start, Instant.now()).toMillis();

            for (Thread thread : threads) {
                thread.join();
            }

            Map<String, Object> metrics = new HashMap<>();
            metrics.put("Readers", readers);
            metrics.put("Writers", writers);
            metrics.put("Read operations", readCount.get());
            metrics.put("Write operations", writeCount.get());
            metrics.put("Read/Write ratio",
                    String.format("%.1f", (double) readCount.get() / writeCount.get()));

            return new PatternResult(
                    "Reader-Writer",
                    duration,
                    readCount.get() + writeCount.get(),
                    true,
                    metrics
            );
        }
    }

    /**
     * 3. Thread Pool Pattern
     */
    static class ThreadPoolPattern {

        public PatternResult test(int poolSize, int taskCount) throws Exception {
            ExecutorService executor = Executors.newFixedThreadPool(poolSize);
            AtomicInteger completedTasks = new AtomicInteger(0);

            Instant start = Instant.now();

            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < taskCount; i++) {
                Future<?> future = executor.submit(() -> {
                    try {
                        Thread.sleep(10); // Simulate work
                        completedTasks.incrementAndGet();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
                futures.add(future);
            }

            for (Future<?> future : futures) {
                future.get();
            }

            long duration = Duration.between(start, Instant.now()).toMillis();

            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);

            Map<String, Object> metrics = new HashMap<>();
            metrics.put("Pool size", poolSize);
            metrics.put("Completed tasks", completedTasks.get());
            metrics.put("Tasks per thread", taskCount / poolSize);

            return new PatternResult(
                    "Thread Pool (size=" + poolSize + ")",
                    duration,
                    taskCount,
                    completedTasks.get() == taskCount,
                    metrics
            );
        }
    }

    /**
     * 4. Future/CompletableFuture Pattern
     */
    static class FuturePattern {

        public PatternResult test(int taskCount) throws Exception {
            AtomicInteger completedTasks = new AtomicInteger(0);

            Instant start = Instant.now();

            List<CompletableFuture<Integer>> futures = new ArrayList<>();

            for (int i = 0; i < taskCount; i++) {
                final int taskId = i;
                CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
                    try {
                        Thread.sleep(10);
                        completedTasks.incrementAndGet();
                        return taskId;
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return -1;
                    }
                });
                futures.add(future);
            }

            // Wait for all
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();

            long duration = Duration.between(start, Instant.now()).toMillis();

            Map<String, Object> metrics = new HashMap<>();
            metrics.put("Completed tasks", completedTasks.get());
            metrics.put("Async composition", "CompletableFuture");

            return new PatternResult(
                    "Future/CompletableFuture",
                    duration,
                    taskCount,
                    completedTasks.get() == taskCount,
                    metrics
            );
        }
    }

    /**
     * 5. Fork-Join Pattern
     */
    static class ForkJoinPattern {

        static class ComputeTask extends RecursiveTask<Long> {
            private final long start;
            private final long end;
            private final int threshold;

            ComputeTask(long start, long end, int threshold) {
                this.start = start;
                this.end = end;
                this.threshold = threshold;
            }

            @Override
            protected Long compute() {
                long length = end - start;

                if (length <= threshold) {
                    // Compute directly
                    long sum = 0;
                    for (long i = start; i < end; i++) {
                        sum += Math.sqrt(i);
                    }
                    return sum;
                } else {
                    // Split
                    long mid = start + length / 2;
                    ComputeTask left = new ComputeTask(start, mid, threshold);
                    ComputeTask right = new ComputeTask(mid, end, threshold);

                    left.fork();
                    long rightResult = right.compute();
                    long leftResult = left.join();

                    return leftResult + rightResult;
                }
            }
        }

        public PatternResult test(long rangeSize, int threshold) {
            ForkJoinPool pool = new ForkJoinPool();

            Instant start = Instant.now();

            ComputeTask task = new ComputeTask(0, rangeSize, threshold);
            long result = pool.invoke(task);

            long duration = Duration.between(start, Instant.now()).toMillis();

            pool.shutdown();

            Map<String, Object> metrics = new HashMap<>();
            metrics.put("Range size", String.format("%,d", rangeSize));
            metrics.put("Threshold", threshold);
            metrics.put("Parallelism", pool.getParallelism());
            metrics.put("Result", result);

            return new PatternResult(
                    "Fork-Join",
                    duration,
                    (int) (rangeSize / threshold),
                    true,
                    metrics
            );
        }
    }

    /**
     * 6. Work Stealing Pattern
     */
    static class WorkStealingPattern {

        public PatternResult test(int taskCount) throws Exception {
            ExecutorService executor = Executors.newWorkStealingPool();
            AtomicInteger completedTasks = new AtomicInteger(0);

            Instant start = Instant.now();

            List<Future<?>> futures = new ArrayList<>();
            Random random = new Random();

            for (int i = 0; i < taskCount; i++) {
                final int workload = 5 + random.nextInt(20); // Variable workload
                Future<?> future = executor.submit(() -> {
                    try {
                        Thread.sleep(workload);
                        completedTasks.incrementAndGet();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
                futures.add(future);
            }

            for (Future<?> future : futures) {
                future.get();
            }

            long duration = Duration.between(start, Instant.now()).toMillis();

            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);

            Map<String, Object> metrics = new HashMap<>();
            metrics.put("Completed tasks", completedTasks.get());
            metrics.put("Workload", "Variable (5-25ms)");
            metrics.put("Strategy", "Work stealing for load balance");

            return new PatternResult(
                    "Work Stealing Pool",
                    duration,
                    taskCount,
                    completedTasks.get() == taskCount,
                    metrics
            );
        }
    }

    /**
     * 7. Barrier Pattern (CyclicBarrier)
     */
    static class BarrierPattern {

        static class Worker implements Runnable {
            private final int workerId;
            private final CyclicBarrier barrier;
            private final int phases;

            Worker(int workerId, CyclicBarrier barrier, int phases) {
                this.workerId = workerId;
                this.barrier = barrier;
                this.phases = phases;
            }

            @Override
            public void run() {
                try {
                    for (int phase = 0; phase < phases; phase++) {
                        // Do work
                        Thread.sleep(10 + workerId % 5);

                        // Wait at barrier
                        barrier.await();
                    }
                } catch (Exception e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        public PatternResult test(int workers, int phases) throws Exception {
            AtomicInteger phaseCompletions = new AtomicInteger(0);

            CyclicBarrier barrier = new CyclicBarrier(workers, () -> {
                phaseCompletions.incrementAndGet();
            });

            Instant start = Instant.now();

            List<Thread> threads = new ArrayList<>();
            for (int i = 0; i < workers; i++) {
                Thread thread = Thread.ofPlatform().start(new Worker(i, barrier, phases));
                threads.add(thread);
            }

            for (Thread thread : threads) {
                thread.join();
            }

            long duration = Duration.between(start, Instant.now()).toMillis();

            Map<String, Object> metrics = new HashMap<>();
            metrics.put("Workers", workers);
            metrics.put("Phases", phases);
            metrics.put("Phase completions", phaseCompletions.get());
            metrics.put("Synchronization points", phases);

            return new PatternResult(
                    "CyclicBarrier",
                    duration,
                    workers * phases,
                    phaseCompletions.get() == phases,
                    metrics
            );
        }
    }

    /**
     * 8. Phaser Pattern
     */
    static class PhaserPattern {

        static class PhasedWorker implements Runnable {
            private final int workerId;
            private final Phaser phaser;
            private final int phases;

            PhasedWorker(int workerId, Phaser phaser, int phases) {
                this.workerId = workerId;
                this.phaser = phaser;
                this.phases = phases;
            }

            @Override
            public void run() {
                for (int phase = 0; phase < phases; phase++) {
                    try {
                        // Do work
                        Thread.sleep(10);

                        // Arrive and wait for others
                        phaser.arriveAndAwaitAdvance();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                phaser.arriveAndDeregister(); // Unregister
            }
        }

        public PatternResult test(int workers, int phases) throws Exception {
            Phaser phaser = new Phaser(1); // Register main thread

            Instant start = Instant.now();

            List<Thread> threads = new ArrayList<>();
            for (int i = 0; i < workers; i++) {
                phaser.register(); // Register each worker
                Thread thread = Thread.ofPlatform().start(new PhasedWorker(i, phaser, phases));
                threads.add(thread);
            }

            // Wait for all phases
            for (int i = 0; i < phases; i++) {
                phaser.arriveAndAwaitAdvance();
            }

            phaser.arriveAndDeregister(); // Deregister main thread

            for (Thread thread : threads) {
                thread.join();
            }

            long duration = Duration.between(start, Instant.now()).toMillis();

            Map<String, Object> metrics = new HashMap<>();
            metrics.put("Workers", workers);
            metrics.put("Phases", phases);
            metrics.put("Final phase", phaser.getPhase());
            metrics.put("Registered parties", phaser.getRegisteredParties());

            return new PatternResult(
                    "Phaser",
                    duration,
                    workers * phases,
                    phaser.getPhase() == phases,
                    metrics
            );
        }
    }

    /**
     * 패턴 비교 분석
     */
    static class PatternComparison {

        public void compare(List<PatternResult> results) {
            System.out.println("\n" + "═".repeat(70));
            System.out.println("  PATTERN COMPARISON");
            System.out.println("═".repeat(70));

            if (results.isEmpty()) return;

            // 1. Execution Time
            System.out.println("\n  1. Execution Time:");
            PatternResult fastest = results.stream()
                    .min(Comparator.comparingLong(r -> r.executionTimeMs))
                    .orElse(null);

            results.forEach(r -> {
                double ratio = fastest != null ?
                        (double) r.executionTimeMs / fastest.executionTimeMs : 1.0;
                String badge = r == fastest ? " 🏆" : "";
                System.out.println(String.format("    %s: %,d ms (%.2fx)%s",
                        r.patternName, r.executionTimeMs, ratio, badge));
            });

            // 2. Throughput
            System.out.println("\n  2. Throughput:");
            PatternResult highestThroughput = results.stream()
                    .max(Comparator.comparingDouble(r -> r.throughput))
                    .orElse(null);

            results.forEach(r -> {
                double ratio = highestThroughput != null ?
                        r.throughput / highestThroughput.throughput : 1.0;
                String badge = r == highestThroughput ? " 🏆" : "";
                System.out.println(String.format("    %s: %.2f tasks/sec (%.1f%%)%s",
                        r.patternName, r.throughput, ratio * 100, badge));
            });

            // 3. Pattern Characteristics
            printPatternCharacteristics();
        }

        private void printPatternCharacteristics() {
            System.out.println("\n  3. Pattern Characteristics:");

            System.out.println("\n    Producer-Consumer:");
            System.out.println("      ✓ Use: Decoupling production from consumption");
            System.out.println("      ✓ Good: Rate control, buffering");
            System.out.println("      ✗ Avoid: Low latency requirements");

            System.out.println("\n    Reader-Writer:");
            System.out.println("      ✓ Use: Read-heavy workloads");
            System.out.println("      ✓ Good: Multiple concurrent readers");
            System.out.println("      ✗ Avoid: Write-heavy workloads");

            System.out.println("\n    Thread Pool:");
            System.out.println("      ✓ Use: Controlled concurrency");
            System.out.println("      ✓ Good: Resource management");
            System.out.println("      ✗ Avoid: I/O-heavy with high concurrency");

            System.out.println("\n    Future/CompletableFuture:");
            System.out.println("      ✓ Use: Async operations, composition");
            System.out.println("      ✓ Good: Non-blocking, chainable");
            System.out.println("      ✗ Avoid: Simple synchronous tasks");

            System.out.println("\n    Fork-Join:");
            System.out.println("      ✓ Use: Divide-and-conquer, recursive");
            System.out.println("      ✓ Good: CPU-intensive, parallelizable");
            System.out.println("      ✗ Avoid: I/O-bound, unbalanced work");

            System.out.println("\n    Work Stealing:");
            System.out.println("      ✓ Use: Variable workload, load balance");
            System.out.println("      ✓ Good: Dynamic load distribution");
            System.out.println("      ✗ Avoid: Uniform short tasks");

            System.out.println("\n    Barrier/Phaser:");
            System.out.println("      ✓ Use: Multi-phase algorithms");
            System.out.println("      ✓ Good: Synchronization points");
            System.out.println("      ✗ Avoid: Independent tasks");
        }
    }

    /**
     * 종합 테스트
     */
    static class ComprehensiveTest {

        public void runAllTests() throws Exception {
            System.out.println("\n╔═══════════════════════════════════════════════════════════════════╗");
            System.out.println("║      CONCURRENCY PATTERNS COMPARISON                              ║");
            System.out.println("╚═══════════════════════════════════════════════════════════════════╝");

            List<PatternResult> results = new ArrayList<>();

            // 1. Producer-Consumer
            System.out.println("\n1. Testing Producer-Consumer Pattern...");
            ProducerConsumerPattern pcPattern = new ProducerConsumerPattern();
            results.add(pcPattern.test(5, 5, 100));

            // 2. Reader-Writer
            System.out.println("2. Testing Reader-Writer Pattern...");
            ReaderWriterPattern rwPattern = new ReaderWriterPattern();
            results.add(rwPattern.test(20, 5, 50));

            // 3. Thread Pool
            System.out.println("3. Testing Thread Pool Pattern...");
            ThreadPoolPattern tpPattern = new ThreadPoolPattern();
            results.add(tpPattern.test(
                    Runtime.getRuntime().availableProcessors() * 2, 1000));

            // 4. Future
            System.out.println("4. Testing Future/CompletableFuture Pattern...");
            FuturePattern futurePattern = new FuturePattern();
            results.add(futurePattern.test(500));

            // 5. Fork-Join
            System.out.println("5. Testing Fork-Join Pattern...");
            ForkJoinPattern fjPattern = new ForkJoinPattern();
            results.add(fjPattern.test(100000, 1000));

            // 6. Work Stealing
            System.out.println("6. Testing Work Stealing Pattern...");
            WorkStealingPattern wsPattern = new WorkStealingPattern();
            results.add(wsPattern.test(500));

            // 7. Barrier
            System.out.println("7. Testing CyclicBarrier Pattern...");
            BarrierPattern barrierPattern = new BarrierPattern();
            results.add(barrierPattern.test(10, 10));

            // 8. Phaser
            System.out.println("8. Testing Phaser Pattern...");
            PhaserPattern phaserPattern = new PhaserPattern();
            results.add(phaserPattern.test(10, 10));

            // Print results
            System.out.println("\n" + "═".repeat(70));
            System.out.println("  ALL PATTERN RESULTS");
            System.out.println("═".repeat(70));

            results.forEach(PatternResult::print);

            // Comparison
            PatternComparison comparison = new PatternComparison();
            comparison.compare(results);

            printFinalSummary();
        }

        private void printFinalSummary() {
            System.out.println("\n" + "═".repeat(70));
            System.out.println("  FINAL SUMMARY");
            System.out.println("═".repeat(70));

            System.out.println("\n  Pattern Selection Guide:");
            System.out.println("    • High I/O, variable rate → Producer-Consumer");
            System.out.println("    • Read-heavy workload → Reader-Writer");
            System.out.println("    • Controlled resources → Thread Pool");
            System.out.println("    • Async composition → Future/CompletableFuture");
            System.out.println("    • Recursive divide-conquer → Fork-Join");
            System.out.println("    • Variable workload → Work Stealing");
            System.out.println("    • Multi-phase sync → Barrier/Phaser");

            System.out.println("\n  Key Takeaways:");
            System.out.println("    ✓ Choose pattern based on workload characteristics");
            System.out.println("    ✓ Consider scalability and resource constraints");
            System.out.println("    ✓ Test under realistic load conditions");
            System.out.println("    ✓ Monitor performance metrics in production");
            System.out.println("═".repeat(70));
        }
    }

    public static void main(String[] args) throws Exception {
        ComprehensiveTest test = new ComprehensiveTest();
        test.runAllTests();

        System.out.println("\n╔═══════════════════════════════════════════════════════════════════╗");
        System.out.println("║         ALL PATTERNS COMPLETED                                    ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════╝");
    }
}