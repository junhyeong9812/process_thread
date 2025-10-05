package com.study.thread.synchronization;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Mutex (Mutual Exclusion) 구현 및 실습
 *
 * Mutex는 상호 배제를 보장하는 동기화 메커니즘입니다.
 * - 한 번에 하나의 스레드만 임계 영역에 접근 가능
 * - Binary Semaphore와 유사하지만 소유권 개념이 있음
 * - 락을 획득한 스레드만 해제 가능
 */
public class MutexDemo {

    /**
     * 1. 단순 Mutex 구현 (Spin Lock 기반)
     */
    static class SimpleMutex {
        private final AtomicBoolean locked = new AtomicBoolean(false);
        private volatile Thread owner = null;

        public void lock() {
            Thread current = Thread.currentThread();

            // Spin-wait until lock is acquired
            while (!locked.compareAndSet(false, true)) {
                // Busy waiting
                Thread.onSpinWait(); // CPU 힌트
            }

            owner = current;
        }

        public void unlock() {
            Thread current = Thread.currentThread();

            if (owner != current) {
                throw new IllegalMonitorStateException(
                        "Thread " + current.getName() + " does not own the lock");
            }

            owner = null;
            locked.set(false);
        }

        public boolean tryLock() {
            if (locked.compareAndSet(false, true)) {
                owner = Thread.currentThread();
                return true;
            }
            return false;
        }

        public boolean isLocked() {
            return locked.get();
        }

        public Thread getOwner() {
            return owner;
        }
    }

    /**
     * 2. Blocking Mutex 구현 (synchronized 기반)
     */
    static class BlockingMutex {
        private boolean locked = false;
        private Thread owner = null;
        private int lockCount = 0; // Reentrant 지원

        public synchronized void lock() throws InterruptedException {
            Thread current = Thread.currentThread();

            // 이미 소유하고 있으면 카운트만 증가 (Reentrant)
            if (owner == current) {
                lockCount++;
                return;
            }

            // 다른 스레드가 락을 소유하고 있으면 대기
            while (locked) {
                wait();
            }

            locked = true;
            owner = current;
            lockCount = 1;
        }

        public synchronized void unlock() {
            Thread current = Thread.currentThread();

            if (owner != current) {
                throw new IllegalMonitorStateException(
                        "Thread does not own the lock");
            }

            lockCount--;

            if (lockCount == 0) {
                locked = false;
                owner = null;
                notify(); // 대기 중인 스레드 하나를 깨움
            }
        }

        public synchronized boolean tryLock() {
            Thread current = Thread.currentThread();

            if (owner == current) {
                lockCount++;
                return true;
            }

            if (!locked) {
                locked = true;
                owner = current;
                lockCount = 1;
                return true;
            }

            return false;
        }

        public synchronized boolean isLocked() {
            return locked;
        }

        public synchronized Thread getOwner() {
            return owner;
        }
    }

    /**
     * 공유 카운터 (임계 영역)
     */
    static class SharedCounter {
        private int count = 0;

        public void increment() {
            count++;
        }

        public int getCount() {
            return count;
        }

        public void reset() {
            count = 0;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Mutex Demo ===\n");

        // 1. Mutex 없이 실행 (Race Condition 발생)
        demonstrateRaceCondition();

        // 2. SimpleMutex 사용
        demonstrateSimpleMutex();

        // 3. BlockingMutex 사용
        demonstrateBlockingMutex();

        // 4. synchronized 키워드 비교
        demonstrateSynchronized();

        // 5. ReentrantLock 비교
        demonstrateReentrantLock();

        // 6. Mutex 성능 비교
        performanceComparison();

        System.out.println("\n=== Demo Completed ===");
    }

    private static void demonstrateRaceCondition() throws InterruptedException {
        System.out.println("1. Without Mutex (Race Condition)");

        SharedCounter counter = new SharedCounter();
        int threadCount = 10;
        int iterations = 1000;

        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < iterations; j++) {
                    counter.increment();
                }
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        System.out.println("  Expected count: " + (threadCount * iterations));
        System.out.println("  Actual count: " + counter.getCount());
        System.out.println("  Race condition detected: " +
                (counter.getCount() != threadCount * iterations));
        System.out.println();
    }

    private static void demonstrateSimpleMutex() throws InterruptedException {
        System.out.println("2. With SimpleMutex (Spin Lock)");

        SharedCounter counter = new SharedCounter();
        SimpleMutex mutex = new SimpleMutex();
        int threadCount = 10;
        int iterations = 1000;

        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < iterations; j++) {
                    mutex.lock();
                    try {
                        counter.increment();

                        // 첫 스레드만 소유권 확인
                        if (threadId == 0 && j == 0) {
                            System.out.println("  Thread " + threadId +
                                    " owns the lock: " +
                                    (mutex.getOwner() == Thread.currentThread()));
                        }
                    } finally {
                        mutex.unlock();
                    }
                }
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        System.out.println("  Expected count: " + (threadCount * iterations));
        System.out.println("  Actual count: " + counter.getCount());
        System.out.println("  Synchronized correctly: " +
                (counter.getCount() == threadCount * iterations));
        System.out.println();
    }

    private static void demonstrateBlockingMutex() throws InterruptedException {
        System.out.println("3. With BlockingMutex (Wait/Notify)");

        SharedCounter counter = new SharedCounter();
        BlockingMutex mutex = new BlockingMutex();
        int threadCount = 10;
        int iterations = 1000;

        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                try {
                    for (int j = 0; j < iterations; j++) {
                        mutex.lock();
                        try {
                            counter.increment();
                        } finally {
                            mutex.unlock();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        System.out.println("  Expected count: " + (threadCount * iterations));
        System.out.println("  Actual count: " + counter.getCount());
        System.out.println("  Synchronized correctly: " +
                (counter.getCount() == threadCount * iterations));
        System.out.println();
    }

    private static void demonstrateSynchronized() throws InterruptedException {
        System.out.println("4. With synchronized keyword");

        SharedCounter counter = new SharedCounter();
        Object lock = new Object();
        int threadCount = 10;
        int iterations = 1000;

        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < iterations; j++) {
                    synchronized (lock) {
                        counter.increment();
                    }
                }
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        System.out.println("  Expected count: " + (threadCount * iterations));
        System.out.println("  Actual count: " + counter.getCount());
        System.out.println("  Synchronized correctly: " +
                (counter.getCount() == threadCount * iterations));
        System.out.println();
    }

    private static void demonstrateReentrantLock() throws InterruptedException {
        System.out.println("5. With ReentrantLock");

        SharedCounter counter = new SharedCounter();
        Lock lock = new ReentrantLock();
        int threadCount = 10;
        int iterations = 1000;

        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < iterations; j++) {
                    lock.lock();
                    try {
                        counter.increment();
                    } finally {
                        lock.unlock();
                    }
                }
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        System.out.println("  Expected count: " + (threadCount * iterations));
        System.out.println("  Actual count: " + counter.getCount());
        System.out.println("  Synchronized correctly: " +
                (counter.getCount() == threadCount * iterations));
        System.out.println();
    }

    private static void performanceComparison() throws InterruptedException {
        System.out.println("6. Performance Comparison");

        int threadCount = 10;
        int iterations = 10000;

        // SimpleMutex
        SharedCounter counter1 = new SharedCounter();
        SimpleMutex mutex1 = new SimpleMutex();
        long time1 = measureTime(() -> {
            Thread[] threads = new Thread[threadCount];
            for (int i = 0; i < threadCount; i++) {
                threads[i] = new Thread(() -> {
                    for (int j = 0; j < iterations; j++) {
                        mutex1.lock();
                        try {
                            counter1.increment();
                        } finally {
                            mutex1.unlock();
                        }
                    }
                });
                threads[i].start();
            }
            for (Thread thread : threads) {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        // BlockingMutex
        SharedCounter counter2 = new SharedCounter();
        BlockingMutex mutex2 = new BlockingMutex();
        long time2 = measureTime(() -> {
            Thread[] threads = new Thread[threadCount];
            for (int i = 0; i < threadCount; i++) {
                threads[i] = new Thread(() -> {
                    try {
                        for (int j = 0; j < iterations; j++) {
                            mutex2.lock();
                            try {
                                counter2.increment();
                            } finally {
                                mutex2.unlock();
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
                threads[i].start();
            }
            for (Thread thread : threads) {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        // synchronized
        SharedCounter counter3 = new SharedCounter();
        Object lock = new Object();
        long time3 = measureTime(() -> {
            Thread[] threads = new Thread[threadCount];
            for (int i = 0; i < threadCount; i++) {
                threads[i] = new Thread(() -> {
                    for (int j = 0; j < iterations; j++) {
                        synchronized (lock) {
                            counter3.increment();
                        }
                    }
                });
                threads[i].start();
            }
            for (Thread thread : threads) {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        System.out.println("  SimpleMutex (Spin Lock): " + time1 + " ms");
        System.out.println("  BlockingMutex (Wait/Notify): " + time2 + " ms");
        System.out.println("  synchronized keyword: " + time3 + " ms");
        System.out.println();
    }

    private static long measureTime(Runnable task) {
        long start = System.currentTimeMillis();
        task.run();
        return System.currentTimeMillis() - start;
    }
}