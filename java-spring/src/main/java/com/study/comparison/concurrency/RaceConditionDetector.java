package com.study.comparison.concurrency;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 경쟁 조건 탐지기 (Race Condition Detector)
 *
 * 멀티스레드 환경에서 발생하는 경쟁 조건을 탐지하고 분석합니다.
 *
 * 경쟁 조건(Race Condition)이란?
 * - 여러 스레드가 공유 자원에 동시 접근할 때
 * - 실행 순서에 따라 결과가 달라지는 상황
 * - 예측 불가능한 동작과 버그의 원인
 *
 * 탐지 방법:
 * 1. Counter Inconsistency: 카운터 불일치
 * 2. Lost Updates: 업데이트 손실
 * 3. Dirty Reads: 중간 상태 읽기
 * 4. Check-Then-Act: 검사 후 실행 문제
 * 5. Read-Modify-Write: 읽기-수정-쓰기 문제
 *
 * 해결 방법:
 * - Synchronization (synchronized)
 * - Locks (ReentrantLock)
 * - Atomic Variables (AtomicInteger)
 * - Immutable Objects
 * - Thread-safe Collections
 */
public class RaceConditionDetector {

    /**
     * 탐지 결과
     */
    static class DetectionResult {
        final String testName;
        final int threadCount;
        final int iterations;
        final boolean raceConditionDetected;
        final long expectedValue;
        final long actualValue;
        final double errorRate;
        final List<String> anomalies;
        final Map<String, Object> details;

        DetectionResult(String testName, int threadCount, int iterations,
                        boolean raceConditionDetected, long expectedValue,
                        long actualValue, List<String> anomalies,
                        Map<String, Object> details) {
            this.testName = testName;
            this.threadCount = threadCount;
            this.iterations = iterations;
            this.raceConditionDetected = raceConditionDetected;
            this.expectedValue = expectedValue;
            this.actualValue = actualValue;
            this.errorRate = expectedValue != 0 ?
                    Math.abs(expectedValue - actualValue) * 100.0 / expectedValue : 0;
            this.anomalies = anomalies != null ? anomalies : new ArrayList<>();
            this.details = details != null ? details : new HashMap<>();
        }

        void print() {
            System.out.println("\n  === " + testName + " ===");
            System.out.println("  Threads: " + threadCount + " | Iterations: " +
                    String.format("%,d", iterations));
            System.out.println("  Expected: " + String.format("%,d", expectedValue));
            System.out.println("  Actual: " + String.format("%,d", actualValue));

            if (raceConditionDetected) {
                System.out.println("  Status: ⚠️  RACE CONDITION DETECTED");
                System.out.println("  Error Rate: " + String.format("%.2f%%", errorRate));

                if (!anomalies.isEmpty()) {
                    System.out.println("  Anomalies:");
                    anomalies.forEach(a -> System.out.println("    - " + a));
                }
            } else {
                System.out.println("  Status: ✓ No race condition detected");
            }

            if (!details.isEmpty()) {
                System.out.println("  Details:");
                details.forEach((k, v) -> System.out.println("    " + k + ": " + v));
            }
        }
    }

    /**
     * 1. Counter Increment Race Condition
     */
    static class CounterRaceCondition {

        // Unsafe counter (race condition)
        static class UnsafeCounter {
            private int count = 0;

            public void increment() {
                count++; // NOT atomic!
            }

            public int getCount() {
                return count;
            }
        }

        // Safe counter with synchronized
        static class SynchronizedCounter {
            private int count = 0;

            public synchronized void increment() {
                count++;
            }

            public synchronized int getCount() {
                return count;
            }
        }

        // Safe counter with AtomicInteger
        static class AtomicCounter {
            private AtomicInteger count = new AtomicInteger(0);

            public void increment() {
                count.incrementAndGet();
            }

            public int getCount() {
                return count.get();
            }
        }

        public DetectionResult testUnsafeCounter(int threads, int iterations)
                throws InterruptedException {
            UnsafeCounter counter = new UnsafeCounter();
            CountDownLatch latch = new CountDownLatch(threads);

            List<Thread> threadList = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                Thread thread = Thread.ofPlatform().start(() -> {
                    for (int j = 0; j < iterations; j++) {
                        counter.increment();
                    }
                    latch.countDown();
                });
                threadList.add(thread);
            }

            latch.await();
            for (Thread t : threadList) {
                t.join();
            }

            long expected = (long) threads * iterations;
            long actual = counter.getCount();
            boolean detected = (expected != actual);

            List<String> anomalies = new ArrayList<>();
            if (detected) {
                long lost = expected - actual;
                anomalies.add("Lost updates: " + String.format("%,d", lost));
                anomalies.add("This is a classic race condition in count++");
            }

            Map<String, Object> details = new HashMap<>();
            details.put("Lost increments", expected - actual);
            details.put("Success rate", String.format("%.2f%%", (actual * 100.0 / expected)));

            return new DetectionResult(
                    "Unsafe Counter (No Synchronization)",
                    threads, iterations, detected, expected, actual,
                    anomalies, details
            );
        }

        public DetectionResult testSynchronizedCounter(int threads, int iterations)
                throws InterruptedException {
            SynchronizedCounter counter = new SynchronizedCounter();
            CountDownLatch latch = new CountDownLatch(threads);

            List<Thread> threadList = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                Thread thread = Thread.ofPlatform().start(() -> {
                    for (int j = 0; j < iterations; j++) {
                        counter.increment();
                    }
                    latch.countDown();
                });
                threadList.add(thread);
            }

            latch.await();
            for (Thread t : threadList) {
                t.join();
            }

            long expected = (long) threads * iterations;
            long actual = counter.getCount();
            boolean detected = (expected != actual);

            return new DetectionResult(
                    "Synchronized Counter",
                    threads, iterations, detected, expected, actual,
                    new ArrayList<>(), new HashMap<>()
            );
        }

        public DetectionResult testAtomicCounter(int threads, int iterations)
                throws InterruptedException {
            AtomicCounter counter = new AtomicCounter();
            CountDownLatch latch = new CountDownLatch(threads);

            List<Thread> threadList = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                Thread thread = Thread.ofPlatform().start(() -> {
                    for (int j = 0; j < iterations; j++) {
                        counter.increment();
                    }
                    latch.countDown();
                });
                threadList.add(thread);
            }

            latch.await();
            for (Thread t : threadList) {
                t.join();
            }

            long expected = (long) threads * iterations;
            long actual = counter.getCount();
            boolean detected = (expected != actual);

            return new DetectionResult(
                    "Atomic Counter",
                    threads, iterations, detected, expected, actual,
                    new ArrayList<>(), new HashMap<>()
            );
        }
    }

    /**
     * 2. Check-Then-Act Race Condition
     */
    static class CheckThenActRaceCondition {

        // Unsafe implementation
        static class UnsafeSingleton {
            private static UnsafeSingleton instance;

            public static UnsafeSingleton getInstance() {
                if (instance == null) { // CHECK
                    instance = new UnsafeSingleton(); // ACT
                }
                return instance;
            }
        }

        // Safe implementation
        static class SafeSingleton {
            private static volatile SafeSingleton instance;
            private static final Object lock = new Object();

            public static SafeSingleton getInstance() {
                if (instance == null) {
                    synchronized (lock) {
                        if (instance == null) { // Double-checked locking
                            instance = new SafeSingleton();
                        }
                    }
                }
                return instance;
            }
        }

        public DetectionResult testUnsafeSingleton(int threads)
                throws InterruptedException {
            // Reset
            try {
                java.lang.reflect.Field field = UnsafeSingleton.class.getDeclaredField("instance");
                field.setAccessible(true);
                field.set(null, null);
            } catch (Exception e) {
                // Ignore
            }

            Set<UnsafeSingleton> instances = ConcurrentHashMap.newKeySet();
            CountDownLatch latch = new CountDownLatch(threads);

            List<Thread> threadList = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                Thread thread = Thread.ofPlatform().start(() -> {
                    instances.add(UnsafeSingleton.getInstance());
                    latch.countDown();
                });
                threadList.add(thread);
            }

            latch.await();
            for (Thread t : threadList) {
                t.join();
            }

            boolean detected = instances.size() > 1;

            List<String> anomalies = new ArrayList<>();
            if (detected) {
                anomalies.add("Multiple instances created: " + instances.size());
                anomalies.add("Check-then-act without synchronization");
            }

            Map<String, Object> details = new HashMap<>();
            details.put("Unique instances", instances.size());

            return new DetectionResult(
                    "Unsafe Singleton (Check-Then-Act)",
                    threads, 1, detected, 1, instances.size(),
                    anomalies, details
            );
        }

        public DetectionResult testSafeSingleton(int threads)
                throws InterruptedException {
            // Reset
            try {
                java.lang.reflect.Field field = SafeSingleton.class.getDeclaredField("instance");
                field.setAccessible(true);
                field.set(null, null);
            } catch (Exception e) {
                // Ignore
            }

            Set<SafeSingleton> instances = ConcurrentHashMap.newKeySet();
            CountDownLatch latch = new CountDownLatch(threads);

            List<Thread> threadList = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                Thread thread = Thread.ofPlatform().start(() -> {
                    instances.add(SafeSingleton.getInstance());
                    latch.countDown();
                });
                threadList.add(thread);
            }

            latch.await();
            for (Thread t : threadList) {
                t.join();
            }

            boolean detected = instances.size() > 1;

            return new DetectionResult(
                    "Safe Singleton (Double-Checked Locking)",
                    threads, 1, detected, 1, instances.size(),
                    new ArrayList<>(), new HashMap<>()
            );
        }
    }

    /**
     * 3. Read-Modify-Write Race Condition
     */
    static class ReadModifyWriteRaceCondition {

        static class BankAccount {
            private double balance;
            private final Lock lock = new ReentrantLock();

            public BankAccount(double initialBalance) {
                this.balance = initialBalance;
            }

            // Unsafe withdrawal
            public boolean unsafeWithdraw(double amount) {
                if (balance >= amount) { // READ
                    // Simulate processing delay
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    balance -= amount; // MODIFY-WRITE
                    return true;
                }
                return false;
            }

            // Safe withdrawal
            public boolean safeWithdraw(double amount) {
                lock.lock();
                try {
                    if (balance >= amount) {
                        Thread.sleep(1); // Simulate processing
                        balance -= amount;
                        return true;
                    }
                    return false;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                } finally {
                    lock.unlock();
                }
            }

            public double getBalance() {
                return balance;
            }
        }

        public DetectionResult testUnsafeWithdrawal(int threads, double withdrawAmount)
                throws InterruptedException {
            double initialBalance = 1000.0;
            BankAccount account = new BankAccount(initialBalance);

            CountDownLatch latch = new CountDownLatch(threads);
            AtomicInteger successCount = new AtomicInteger(0);

            List<Thread> threadList = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                Thread thread = Thread.ofPlatform().start(() -> {
                    if (account.unsafeWithdraw(withdrawAmount)) {
                        successCount.incrementAndGet();
                    }
                    latch.countDown();
                });
                threadList.add(thread);
            }

            latch.await();
            for (Thread t : threadList) {
                t.join();
            }

            double finalBalance = account.getBalance();
            double expectedBalance = initialBalance - (successCount.get() * withdrawAmount);
            boolean detected = Math.abs(finalBalance - expectedBalance) > 0.01;

            List<String> anomalies = new ArrayList<>();
            if (detected || finalBalance < 0) {
                anomalies.add("Balance inconsistency detected");
                if (finalBalance < 0) {
                    anomalies.add("CRITICAL: Negative balance! " +
                            String.format("%.2f", finalBalance));
                }
                anomalies.add("Multiple threads read balance before any withdrew");
            }

            Map<String, Object> details = new HashMap<>();
            details.put("Initial balance", String.format("$%.2f", initialBalance));
            details.put("Final balance", String.format("$%.2f", finalBalance));
            details.put("Successful withdrawals", successCount.get());
            details.put("Expected balance", String.format("$%.2f", expectedBalance));

            return new DetectionResult(
                    "Unsafe Bank Withdrawal (Read-Modify-Write)",
                    threads, 1, detected || finalBalance < 0,
                    (long) expectedBalance, (long) finalBalance,
                    anomalies, details
            );
        }

        public DetectionResult testSafeWithdrawal(int threads, double withdrawAmount)
                throws InterruptedException {
            double initialBalance = 1000.0;
            BankAccount account = new BankAccount(initialBalance);

            CountDownLatch latch = new CountDownLatch(threads);
            AtomicInteger successCount = new AtomicInteger(0);

            List<Thread> threadList = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                Thread thread = Thread.ofPlatform().start(() -> {
                    if (account.safeWithdraw(withdrawAmount)) {
                        successCount.incrementAndGet();
                    }
                    latch.countDown();
                });
                threadList.add(thread);
            }

            latch.await();
            for (Thread t : threadList) {
                t.join();
            }

            double finalBalance = account.getBalance();
            double expectedBalance = initialBalance - (successCount.get() * withdrawAmount);
            boolean detected = Math.abs(finalBalance - expectedBalance) > 0.01 ||
                    finalBalance < 0;

            Map<String, Object> details = new HashMap<>();
            details.put("Initial balance", String.format("$%.2f", initialBalance));
            details.put("Final balance", String.format("$%.2f", finalBalance));
            details.put("Successful withdrawals", successCount.get());

            return new DetectionResult(
                    "Safe Bank Withdrawal (Synchronized)",
                    threads, 1, detected,
                    (long) expectedBalance, (long) finalBalance,
                    new ArrayList<>(), details
            );
        }
    }

    /**
     * 4. Dirty Read Race Condition
     */
    static class DirtyReadRaceCondition {

        static class DataProcessor {
            private int value = 0;
            private boolean valid = true;
            private final Lock lock = new ReentrantLock();

            // Unsafe update (no atomicity)
            public void unsafeUpdate(int newValue) {
                valid = false; // Step 1
                try {
                    Thread.sleep(10); // Simulate processing
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                value = newValue; // Step 2
                valid = true; // Step 3
            }

            // Safe update
            public void safeUpdate(int newValue) {
                lock.lock();
                try {
                    valid = false;
                    Thread.sleep(10);
                    value = newValue;
                    valid = true;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    lock.unlock();
                }
            }

            public int unsafeRead() {
                if (valid) {
                    return value;
                }
                return -1; // Invalid
            }

            public int safeRead() {
                lock.lock();
                try {
                    if (valid) {
                        return value;
                    }
                    return -1;
                } finally {
                    lock.unlock();
                }
            }
        }

        public DetectionResult testDirtyRead(int readers, int writers, boolean useSafe)
                throws InterruptedException {
            DataProcessor processor = new DataProcessor();
            CountDownLatch latch = new CountDownLatch(readers + writers);
            AtomicInteger dirtyReads = new AtomicInteger(0);
            AtomicInteger totalReads = new AtomicInteger(0);

            // Writers
            List<Thread> threadList = new ArrayList<>();
            for (int i = 0; i < writers; i++) {
                final int writerId = i;
                Thread thread = Thread.ofPlatform().start(() -> {
                    for (int j = 0; j < 10; j++) {
                        if (useSafe) {
                            processor.safeUpdate(writerId * 100 + j);
                        } else {
                            processor.unsafeUpdate(writerId * 100 + j);
                        }
                    }
                    latch.countDown();
                });
                threadList.add(thread);
            }

            // Readers
            for (int i = 0; i < readers; i++) {
                Thread thread = Thread.ofPlatform().start(() -> {
                    for (int j = 0; j < 100; j++) {
                        int value = useSafe ? processor.safeRead() : processor.unsafeRead();
                        totalReads.incrementAndGet();
                        if (value == -1) {
                            dirtyReads.incrementAndGet();
                        }
                        try {
                            Thread.sleep(1);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                    latch.countDown();
                });
                threadList.add(thread);
            }

            latch.await();
            for (Thread t : threadList) {
                t.join();
            }

            boolean detected = dirtyReads.get() > 0;

            List<String> anomalies = new ArrayList<>();
            if (detected) {
                anomalies.add("Dirty reads detected: " + dirtyReads.get() +
                        " out of " + totalReads.get());
                anomalies.add("Readers saw intermediate invalid state");
            }

            Map<String, Object> details = new HashMap<>();
            details.put("Total reads", totalReads.get());
            details.put("Dirty reads", dirtyReads.get());
            details.put("Clean reads", totalReads.get() - dirtyReads.get());

            return new DetectionResult(
                    useSafe ? "Safe Read (Synchronized)" : "Unsafe Read (Dirty Reads)",
                    readers + writers, 1, detected,
                    totalReads.get(), totalReads.get() - dirtyReads.get(),
                    anomalies, details
            );
        }
    }

    /**
     * 종합 테스트
     */
    static class ComprehensiveDetection {

        public void runAllTests() throws Exception {
            System.out.println("\n╔═══════════════════════════════════════════════════════════════════╗");
            System.out.println("║      RACE CONDITION DETECTOR                                      ║");
            System.out.println("╚═══════════════════════════════════════════════════════════════════╝");

            List<DetectionResult> allResults = new ArrayList<>();

            // 1. Counter Race Condition
            System.out.println("\n" + "═".repeat(70));
            System.out.println("1. COUNTER INCREMENT RACE CONDITION");
            System.out.println("═".repeat(70));

            CounterRaceCondition counterTest = new CounterRaceCondition();

            System.out.println("\nTesting unsafe counter...");
            allResults.add(counterTest.testUnsafeCounter(10, 10000));

            System.out.println("\nTesting synchronized counter...");
            allResults.add(counterTest.testSynchronizedCounter(10, 10000));

            System.out.println("\nTesting atomic counter...");
            allResults.add(counterTest.testAtomicCounter(10, 10000));

            // 2. Check-Then-Act
            System.out.println("\n" + "═".repeat(70));
            System.out.println("2. CHECK-THEN-ACT RACE CONDITION");
            System.out.println("═".repeat(70));

            CheckThenActRaceCondition singletonTest = new CheckThenActRaceCondition();

            System.out.println("\nTesting unsafe singleton...");
            allResults.add(singletonTest.testUnsafeSingleton(50));

            System.out.println("\nTesting safe singleton...");
            allResults.add(singletonTest.testSafeSingleton(50));

            // 3. Read-Modify-Write
            System.out.println("\n" + "═".repeat(70));
            System.out.println("3. READ-MODIFY-WRITE RACE CONDITION");
            System.out.println("═".repeat(70));

            ReadModifyWriteRaceCondition bankTest = new ReadModifyWriteRaceCondition();

            System.out.println("\nTesting unsafe withdrawal...");
            allResults.add(bankTest.testUnsafeWithdrawal(20, 100.0));

            System.out.println("\nTesting safe withdrawal...");
            allResults.add(bankTest.testSafeWithdrawal(20, 100.0));

            // 4. Dirty Read
            System.out.println("\n" + "═".repeat(70));
            System.out.println("4. DIRTY READ RACE CONDITION");
            System.out.println("═".repeat(70));

            DirtyReadRaceCondition dirtyReadTest = new DirtyReadRaceCondition();

            System.out.println("\nTesting dirty read (unsafe)...");
            allResults.add(dirtyReadTest.testDirtyRead(5, 5, false));

            System.out.println("\nTesting clean read (safe)...");
            allResults.add(dirtyReadTest.testDirtyRead(5, 5, true));

            // Print all results
            System.out.println("\n" + "═".repeat(70));
            System.out.println("  ALL DETECTION RESULTS");
            System.out.println("═".repeat(70));

            allResults.forEach(DetectionResult::print);

            // Summary
            printSummary(allResults);
        }

        private void printSummary(List<DetectionResult> results) {
            System.out.println("\n" + "═".repeat(70));
            System.out.println("  SUMMARY");
            System.out.println("═".repeat(70));

            long totalTests = results.size();
            long detectedCount = results.stream()
                    .filter(r -> r.raceConditionDetected)
                    .count();

            System.out.println("\n  Total Tests: " + totalTests);
            System.out.println("  Race Conditions Detected: " + detectedCount);
            System.out.println("  Safe Implementations: " + (totalTests - detectedCount));

            System.out.println("\n  Common Race Condition Patterns:");
            System.out.println("    1. Counter Increment (count++)");
            System.out.println("       → Use: synchronized, AtomicInteger, or locks");

            System.out.println("\n    2. Check-Then-Act (if-then pattern)");
            System.out.println("       → Use: synchronized block, double-checked locking");

            System.out.println("\n    3. Read-Modify-Write (balance -= amount)");
            System.out.println("       → Use: synchronized methods, locks");

            System.out.println("\n    4. Dirty Reads (inconsistent state)");
            System.out.println("       → Use: locks for both read and write");

            System.out.println("\n  Prevention Best Practices:");
            System.out.println("    ✓ Use immutable objects when possible");
            System.out.println("    ✓ Minimize shared mutable state");
            System.out.println("    ✓ Use thread-safe collections (ConcurrentHashMap)");
            System.out.println("    ✓ Prefer higher-level concurrency utilities");
            System.out.println("    ✓ Test with multiple threads under load");
            System.out.println("═".repeat(70));
        }
    }

    public static void main(String[] args) throws Exception {
        ComprehensiveDetection detection = new ComprehensiveDetection();
        detection.runAllTests();

        System.out.println("\n╔═══════════════════════════════════════════════════════════════════╗");
        System.out.println("║         DETECTION COMPLETED                                       ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════╝");
    }
}