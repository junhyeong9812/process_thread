package com.study.thread.synchronization;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.ArrayList;
import java.util.List;

/**
 * ReentrantLock 실습
 *
 * ReentrantLock는 synchronized보다 유연한 락 메커니즘을 제공합니다.
 * - 명시적 lock/unlock
 * - tryLock(): 타임아웃 지원
 * - lockInterruptibly(): 인터럽트 가능
 * - Condition 변수: 복수의 wait set
 * - 공정성(fairness) 옵션
 * - 재진입(reentrant) 가능
 */
public class ReentrantLockDemo {

    /**
     * 1. 기본 ReentrantLock 사용법
     */
    static class Counter {
        private final Lock lock = new ReentrantLock();
        private int count = 0;

        public void increment() {
            lock.lock();
            try {
                count++;
                System.out.println("  [" + Thread.currentThread().getName() + "] " +
                        "Count: " + count + " (Hold count: " +
                        ((ReentrantLock) lock).getHoldCount() + ")");
            } finally {
                lock.unlock();
            }
        }

        public int getCount() {
            lock.lock();
            try {
                return count;
            } finally {
                lock.unlock();
            }
        }

        public void reset() {
            lock.lock();
            try {
                count = 0;
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * 2. Reentrant(재진입) 특성 시연
     */
    static class ReentrantExample {
        private final ReentrantLock lock = new ReentrantLock();
        private int value = 0;

        public void outerMethod() {
            lock.lock();
            try {
                System.out.println("  [Outer] Hold count: " + lock.getHoldCount());
                value++;
                innerMethod(); // 같은 스레드가 다시 락 획득
            } finally {
                lock.unlock();
            }
        }

        public void innerMethod() {
            lock.lock();
            try {
                System.out.println("  [Inner] Hold count: " + lock.getHoldCount());
                value++;
            } finally {
                lock.unlock();
            }
        }

        public int getValue() {
            return value;
        }
    }

    /**
     * 3. tryLock() 사용 예제
     */
    static class BankAccount {
        private final ReentrantLock lock = new ReentrantLock();
        private double balance;

        public BankAccount(double balance) {
            this.balance = balance;
        }

        public boolean withdraw(double amount) {
            if (lock.tryLock()) {
                try {
                    if (balance >= amount) {
                        System.out.println("  [" + Thread.currentThread().getName() + "] " +
                                "Withdrawing: $" + amount);
                        Thread.sleep(100); // 처리 시뮬레이션
                        balance -= amount;
                        System.out.println("  [" + Thread.currentThread().getName() + "] " +
                                "New balance: $" + balance);
                        return true;
                    } else {
                        System.out.println("  [" + Thread.currentThread().getName() + "] " +
                                "Insufficient funds");
                        return false;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                } finally {
                    lock.unlock();
                }
            } else {
                System.out.println("  [" + Thread.currentThread().getName() + "] " +
                        "Could not acquire lock, will retry later");
                return false;
            }
        }

        public boolean withdrawWithTimeout(double amount, long timeoutMs) {
            try {
                if (lock.tryLock(timeoutMs, TimeUnit.MILLISECONDS)) {
                    try {
                        if (balance >= amount) {
                            System.out.println("  [" + Thread.currentThread().getName() + "] " +
                                    "Withdrawing: $" + amount);
                            balance -= amount;
                            return true;
                        }
                        return false;
                    } finally {
                        lock.unlock();
                    }
                } else {
                    System.out.println("  [" + Thread.currentThread().getName() + "] " +
                            "Timeout waiting for lock");
                    return false;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        public double getBalance() {
            lock.lock();
            try {
                return balance;
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * 4. Condition 변수 사용 (Producer-Consumer)
     */
    static class BoundedQueue<T> {
        private final Lock lock = new ReentrantLock();
        private final Condition notFull = lock.newCondition();
        private final Condition notEmpty = lock.newCondition();
        private final List<T> items = new ArrayList<>();
        private final int capacity;

        public BoundedQueue(int capacity) {
            this.capacity = capacity;
        }

        public void put(T item) throws InterruptedException {
            lock.lock();
            try {
                while (items.size() >= capacity) {
                    System.out.println("  [Producer] Queue full, waiting...");
                    notFull.await();
                }
                items.add(item);
                System.out.println("  [Producer] Added: " + item +
                        " (size: " + items.size() + ")");
                notEmpty.signal();
            } finally {
                lock.unlock();
            }
        }

        public T take() throws InterruptedException {
            lock.lock();
            try {
                while (items.isEmpty()) {
                    System.out.println("  [Consumer] Queue empty, waiting...");
                    notEmpty.await();
                }
                T item = items.remove(0);
                System.out.println("  [Consumer] Removed: " + item +
                        " (size: " + items.size() + ")");
                notFull.signal();
                return item;
            } finally {
                lock.unlock();
            }
        }

        public int size() {
            lock.lock();
            try {
                return items.size();
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * 5. 공정성(Fairness) 비교
     */
    static class FairnessTest {
        private final ReentrantLock lock;
        private int counter = 0;

        public FairnessTest(boolean fair) {
            this.lock = new ReentrantLock(fair);
        }

        public void doWork(int threadId) {
            for (int i = 0; i < 3; i++) {
                lock.lock();
                try {
                    counter++;
                    System.out.println("  Thread-" + threadId + " iteration " + (i + 1));
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    lock.unlock();
                }
            }
        }

        public boolean isFair() {
            return lock.isFair();
        }
    }

    /**
     * 6. lockInterruptibly() 사용
     */
    static class InterruptibleTask {
        private final ReentrantLock lock = new ReentrantLock();

        public void doWork() throws InterruptedException {
            System.out.println("  [" + Thread.currentThread().getName() + "] " +
                    "Trying to acquire lock (interruptibly)...");

            lock.lockInterruptibly(); // 인터럽트 가능한 락 획득
            try {
                System.out.println("  [" + Thread.currentThread().getName() + "] " +
                        "Lock acquired, working...");
                Thread.sleep(5000); // 긴 작업
                System.out.println("  [" + Thread.currentThread().getName() + "] " +
                        "Work completed");
            } finally {
                lock.unlock();
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== ReentrantLock Demo ===\n");

        // 1. 기본 사용법
        demonstrateBasicUsage();

        // 2. Reentrant 특성
        demonstrateReentrant();

        // 3. tryLock()
        demonstrateTryLock();

        // 4. tryLock(timeout)
        demonstrateTryLockWithTimeout();

        // 5. Condition 변수
        demonstrateCondition();

        // 6. 공정성 비교
        demonstrateFairness();

        // 7. lockInterruptibly()
        demonstrateInterruptibly();

        // 8. Lock 정보 조회
        demonstrateLockInfo();

        System.out.println("\n=== Demo Completed ===");
    }

    private static void demonstrateBasicUsage() throws InterruptedException {
        System.out.println("1. Basic ReentrantLock Usage");

        Counter counter = new Counter();
        int threadCount = 5;
        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 2; j++) {
                    counter.increment();
                }
            }, "Thread-" + (i + 1));
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        System.out.println("  Final count: " + counter.getCount());
        System.out.println();
    }

    private static void demonstrateReentrant() {
        System.out.println("2. Reentrant Feature Demo");

        ReentrantExample example = new ReentrantExample();
        example.outerMethod();

        System.out.println("  Final value: " + example.getValue());
        System.out.println("  (Lock was acquired twice by the same thread)");
        System.out.println();
    }

    private static void demonstrateTryLock() throws InterruptedException {
        System.out.println("3. tryLock() Demo");

        BankAccount account = new BankAccount(1000);

        Thread t1 = new Thread(() -> {
            account.withdraw(500);
        }, "Customer-1");

        Thread t2 = new Thread(() -> {
            try {
                Thread.sleep(50); // t1이 먼저 락을 획득하도록
                account.withdraw(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Customer-2");

        t1.start();
        t2.start();

        t1.join();
        t2.join();

        System.out.println("  Final balance: $" + account.getBalance());
        System.out.println();
    }

    private static void demonstrateTryLockWithTimeout() throws InterruptedException {
        System.out.println("4. tryLock(timeout) Demo");

        BankAccount account = new BankAccount(1000);

        Thread t1 = new Thread(() -> {
            account.withdrawWithTimeout(400, 200);
        }, "Customer-1");

        Thread t2 = new Thread(() -> {
            try {
                Thread.sleep(50);
                account.withdrawWithTimeout(300, 200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Customer-2");

        t1.start();
        t2.start();

        t1.join();
        t2.join();

        System.out.println("  Final balance: $" + account.getBalance());
        System.out.println();
    }

    private static void demonstrateCondition() throws InterruptedException {
        System.out.println("5. Condition Variable Demo");

        BoundedQueue<String> queue = new BoundedQueue<>(2);

        Thread producer = new Thread(() -> {
            try {
                for (int i = 1; i <= 4; i++) {
                    queue.put("Item-" + i);
                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Producer");

        Thread consumer = new Thread(() -> {
            try {
                Thread.sleep(500); // Producer가 먼저 채우도록
                for (int i = 1; i <= 4; i++) {
                    queue.take();
                    Thread.sleep(200);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Consumer");

        producer.start();
        consumer.start();

        producer.join();
        consumer.join();
        System.out.println();
    }

    private static void demonstrateFairness() throws InterruptedException {
        System.out.println("6. Fairness Demo");

        System.out.println("  Non-fair lock:");
        testFairness(false);

        System.out.println("\n  Fair lock:");
        testFairness(true);

        System.out.println();
    }

    private static void testFairness(boolean fair) throws InterruptedException {
        FairnessTest test = new FairnessTest(fair);
        int threadCount = 3;
        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i + 1;
            threads[i] = new Thread(() -> test.doWork(threadId));
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }
    }

    private static void demonstrateInterruptibly() throws InterruptedException {
        System.out.println("7. lockInterruptibly() Demo");

        InterruptibleTask task = new InterruptibleTask();

        Thread t1 = new Thread(() -> {
            try {
                task.doWork();
            } catch (InterruptedException e) {
                System.out.println("  [" + Thread.currentThread().getName() + "] " +
                        "Interrupted while waiting for lock");
            }
        }, "Worker-1");

        Thread t2 = new Thread(() -> {
            try {
                task.doWork();
            } catch (InterruptedException e) {
                System.out.println("  [" + Thread.currentThread().getName() + "] " +
                        "Interrupted while waiting for lock");
            }
        }, "Worker-2");

        t1.start();
        Thread.sleep(100); // t1이 먼저 락을 획득하도록
        t2.start();

        Thread.sleep(500);
        t2.interrupt(); // t2를 인터럽트

        t1.join();
        t2.join();
        System.out.println();
    }

    private static void demonstrateLockInfo() throws InterruptedException {
        System.out.println("8. Lock Information Demo");

        ReentrantLock lock = new ReentrantLock();

        System.out.println("  Is fair: " + lock.isFair());
        System.out.println("  Is locked: " + lock.isLocked());
        System.out.println("  Hold count: " + lock.getHoldCount());

        lock.lock();
        System.out.println("\n  After lock():");
        System.out.println("  Is locked: " + lock.isLocked());
        System.out.println("  Hold count: " + lock.getHoldCount());
        System.out.println("  Has queued threads: " + lock.hasQueuedThreads());

        lock.lock(); // 재진입
        System.out.println("\n  After second lock():");
        System.out.println("  Hold count: " + lock.getHoldCount());

        lock.unlock();
        System.out.println("\n  After first unlock():");
        System.out.println("  Hold count: " + lock.getHoldCount());

        lock.unlock();
        System.out.println("\n  After second unlock():");
        System.out.println("  Is locked: " + lock.isLocked());
        System.out.println("  Hold count: " + lock.getHoldCount());
        System.out.println();
    }
}