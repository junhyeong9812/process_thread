package com.study.thread.synchronization;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * ReadWriteLock 실습
 *
 * ReadWriteLock는 읽기와 쓰기를 분리하여 동시성을 향상시킵니다.
 * - 읽기 락: 여러 스레드가 동시에 획득 가능
 * - 쓰기 락: 배타적 접근 (읽기/쓰기 모두 차단)
 * - 읽기가 많은 환경에서 성능 향상
 * - 쓰기 기아(starvation) 방지 옵션
 */
public class ReadWriteLockDemo {

    /**
     * 1. 기본 ReadWriteLock 사용
     */
    static class SharedData {
        private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
        private final Lock readLock = rwLock.readLock();
        private final Lock writeLock = rwLock.writeLock();
        private int data = 0;

        public int read() {
            readLock.lock();
            try {
                System.out.println("  [" + Thread.currentThread().getName() + "] " +
                        "Reading: " + data);
                Thread.sleep(100); // 읽기 시뮬레이션
                return data;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return -1;
            } finally {
                readLock.unlock();
            }
        }

        public void write(int value) {
            writeLock.lock();
            try {
                System.out.println("  [" + Thread.currentThread().getName() + "] " +
                        "Writing: " + value);
                Thread.sleep(200); // 쓰기 시뮬레이션
                data = value;
                System.out.println("  [" + Thread.currentThread().getName() + "] " +
                        "Write completed");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                writeLock.unlock();
            }
        }
    }

    /**
     * 2. 캐시 구현 (읽기 최적화)
     */
    static class Cache<K, V> {
        private final Map<K, V> cache = new HashMap<>();
        private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
        private final Lock readLock = rwLock.readLock();
        private final Lock writeLock = rwLock.writeLock();
        private int hits = 0;
        private int misses = 0;

        public V get(K key) {
            readLock.lock();
            try {
                V value = cache.get(key);
                if (value != null) {
                    hits++;
                    System.out.println("  [" + Thread.currentThread().getName() + "] " +
                            "Cache HIT for key: " + key);
                } else {
                    misses++;
                    System.out.println("  [" + Thread.currentThread().getName() + "] " +
                            "Cache MISS for key: " + key);
                }
                return value;
            } finally {
                readLock.unlock();
            }
        }

        public void put(K key, V value) {
            writeLock.lock();
            try {
                System.out.println("  [" + Thread.currentThread().getName() + "] " +
                        "Caching key: " + key + " -> " + value);
                cache.put(key, value);
                Thread.sleep(50); // 쓰기 시뮬레이션
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                writeLock.unlock();
            }
        }

        public void printStats() {
            readLock.lock();
            try {
                System.out.println("  Cache Stats - Hits: " + hits +
                        ", Misses: " + misses +
                        ", Hit Rate: " +
                        (hits * 100.0 / (hits + misses)) + "%");
            } finally {
                readLock.unlock();
            }
        }
    }

    /**
     * 3. 락 다운그레이드 (Write -> Read)
     */
    static class DowngradableData {
        private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
        private final Lock readLock = rwLock.readLock();
        private final Lock writeLock = rwLock.writeLock();
        private int data = 0;

        public void writeAndRead() {
            writeLock.lock();
            try {
                System.out.println("  [" + Thread.currentThread().getName() + "] " +
                        "Write lock acquired");
                data++;
                System.out.println("  [" + Thread.currentThread().getName() + "] " +
                        "Data updated to: " + data);

                // 락 다운그레이드: 쓰기 락을 유지한 채 읽기 락 획득
                readLock.lock();
                System.out.println("  [" + Thread.currentThread().getName() + "] " +
                        "Read lock acquired (downgrade)");
            } finally {
                writeLock.unlock(); // 쓰기 락 해제
                System.out.println("  [" + Thread.currentThread().getName() + "] " +
                        "Write lock released");
            }

            try {
                // 이제 읽기 락만 보유
                System.out.println("  [" + Thread.currentThread().getName() + "] " +
                        "Reading with read lock: " + data);
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                readLock.unlock();
                System.out.println("  [" + Thread.currentThread().getName() + "] " +
                        "Read lock released");
            }
        }
    }

    /**
     * 4. 공정성(Fairness) 비교
     */
    static class FairReadWriteData {
        private final ReentrantReadWriteLock rwLock;
        private final Lock readLock;
        private final Lock writeLock;
        private int data = 0;

        public FairReadWriteData(boolean fair) {
            this.rwLock = new ReentrantReadWriteLock(fair);
            this.readLock = rwLock.readLock();
            this.writeLock = rwLock.writeLock();
        }

        public void read(int threadId) {
            readLock.lock();
            try {
                System.out.println("  Reader-" + threadId + " reading: " + data);
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                readLock.unlock();
            }
        }

        public void write(int threadId, int value) {
            writeLock.lock();
            try {
                System.out.println("  Writer-" + threadId + " writing: " + value);
                data = value;
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                writeLock.unlock();
            }
        }

        public boolean isFair() {
            return rwLock.isFair();
        }
    }

    /**
     * 5. 성능 비교: ReadWriteLock vs synchronized
     */
    static class PerformanceTest {
        private int data = 0;

        // ReadWriteLock 버전
        private final ReadWriteLock rwLock = new ReentrantReadWriteLock();

        public int readWithRWLock() {
            rwLock.readLock().lock();
            try {
                return data;
            } finally {
                rwLock.readLock().unlock();
            }
        }

        public void writeWithRWLock(int value) {
            rwLock.writeLock().lock();
            try {
                data = value;
            } finally {
                rwLock.writeLock().unlock();
            }
        }

        // synchronized 버전
        public synchronized int readWithSynchronized() {
            return data;
        }

        public synchronized void writeWithSynchronized(int value) {
            data = value;
        }
    }

    /**
     * 6. 락 정보 조회
     */
    static class LockInfoExample {
        private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

        public void demonstrateLockInfo() throws InterruptedException {
            System.out.println("  Initial state:");
            printLockInfo();

            System.out.println("\n  Acquiring read locks:");
            rwLock.readLock().lock();
            rwLock.readLock().lock();
            printLockInfo();

            System.out.println("\n  Starting writer thread:");
            Thread writer = new Thread(() -> {
                System.out.println("    Writer waiting for write lock...");
                rwLock.writeLock().lock();
                System.out.println("    Writer acquired write lock");
                rwLock.writeLock().unlock();
            });
            writer.start();

            Thread.sleep(100);
            printLockInfo();

            System.out.println("\n  Releasing read locks:");
            rwLock.readLock().unlock();
            rwLock.readLock().unlock();

            writer.join();
            printLockInfo();
        }

        private void printLockInfo() {
            System.out.println("    Read lock count: " + rwLock.getReadLockCount());
            System.out.println("    Write lock held: " + rwLock.isWriteLocked());
            System.out.println("    Read hold count: " + rwLock.getReadHoldCount());
            System.out.println("    Write hold count: " + rwLock.getWriteHoldCount());
            System.out.println("    Has queued threads: " + rwLock.hasQueuedThreads());
        }
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== ReadWriteLock Demo ===\n");

        // 1. 기본 사용법
        demonstrateBasicUsage();

        // 2. 캐시 구현
        demonstrateCache();

        // 3. 락 다운그레이드
        demonstrateLockDowngrade();

        // 4. 공정성 비교
        demonstrateFairness();

        // 5. 성능 비교
        demonstratePerformance();

        // 6. 락 정보 조회
        demonstrateLockInfo();

        System.out.println("\n=== Demo Completed ===");
    }

    private static void demonstrateBasicUsage() throws InterruptedException {
        System.out.println("1. Basic ReadWriteLock Usage");

        SharedData data = new SharedData();

        // 여러 리더
        Thread reader1 = new Thread(() -> data.read(), "Reader-1");
        Thread reader2 = new Thread(() -> data.read(), "Reader-2");
        Thread reader3 = new Thread(() -> data.read(), "Reader-3");

        // 라이터
        Thread writer = new Thread(() -> data.write(100), "Writer-1");

        reader1.start();
        reader2.start();
        Thread.sleep(50);
        writer.start();
        Thread.sleep(50);
        reader3.start();

        reader1.join();
        reader2.join();
        writer.join();
        reader3.join();

        System.out.println();
    }

    private static void demonstrateCache() throws InterruptedException {
        System.out.println("2. Cache Implementation Demo");

        Cache<String, Integer> cache = new Cache<>();
        Random random = new Random();

        // 캐시에 초기 데이터 추가
        cache.put("A", 1);
        cache.put("B", 2);
        cache.put("C", 3);

        // 여러 스레드가 동시에 읽기
        List<Thread> threads = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            final int threadId = i;
            threads.add(new Thread(() -> {
                String[] keys = {"A", "B", "C", "D"};
                String key = keys[random.nextInt(keys.length)];
                cache.get(key);
            }, "Reader-" + threadId));
        }

        for (Thread thread : threads) {
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        cache.printStats();
        System.out.println();
    }

    private static void demonstrateLockDowngrade() throws InterruptedException {
        System.out.println("3. Lock Downgrade Demo");

        DowngradableData data = new DowngradableData();

        Thread thread = new Thread(() -> data.writeAndRead(), "Downgrader");
        thread.start();
        thread.join();

        System.out.println();
    }

    private static void demonstrateFairness() throws InterruptedException {
        System.out.println("4. Fairness Comparison");

        System.out.println("  Non-fair ReadWriteLock:");
        testFairness(false);

        System.out.println("\n  Fair ReadWriteLock:");
        testFairness(true);

        System.out.println();
    }

    private static void testFairness(boolean fair) throws InterruptedException {
        FairReadWriteData data = new FairReadWriteData(fair);

        // 리더와 라이터 혼합
        List<Thread> threads = new ArrayList<>();

        for (int i = 1; i <= 3; i++) {
            final int id = i;
            threads.add(new Thread(() -> data.read(id), "Reader-" + id));
        }

        for (int i = 1; i <= 2; i++) {
            final int id = i;
            threads.add(new Thread(() -> data.write(id, id * 10), "Writer-" + id));
        }

        for (Thread thread : threads) {
            thread.start();
            Thread.sleep(20);
        }

        for (Thread thread : threads) {
            thread.join();
        }
    }

    private static void demonstratePerformance() throws InterruptedException {
        System.out.println("5. Performance Comparison");

        PerformanceTest test = new PerformanceTest();
        int iterations = 10000;
        int readerCount = 8;
        int writerCount = 2;

        // ReadWriteLock 테스트
        long rwStart = System.currentTimeMillis();
        List<Thread> rwThreads = new ArrayList<>();

        for (int i = 0; i < readerCount; i++) {
            rwThreads.add(new Thread(() -> {
                for (int j = 0; j < iterations; j++) {
                    test.readWithRWLock();
                }
            }));
        }

        for (int i = 0; i < writerCount; i++) {
            final int id = i;
            rwThreads.add(new Thread(() -> {
                for (int j = 0; j < iterations / 10; j++) {
                    test.writeWithRWLock(id);
                }
            }));
        }

        for (Thread thread : rwThreads) {
            thread.start();
        }
        for (Thread thread : rwThreads) {
            thread.join();
        }

        long rwTime = System.currentTimeMillis() - rwStart;

        // synchronized 테스트
        long syncStart = System.currentTimeMillis();
        List<Thread> syncThreads = new ArrayList<>();

        for (int i = 0; i < readerCount; i++) {
            syncThreads.add(new Thread(() -> {
                for (int j = 0; j < iterations; j++) {
                    test.readWithSynchronized();
                }
            }));
        }

        for (int i = 0; i < writerCount; i++) {
            final int id = i;
            syncThreads.add(new Thread(() -> {
                for (int j = 0; j < iterations / 10; j++) {
                    test.writeWithSynchronized(id);
                }
            }));
        }

        for (Thread thread : syncThreads) {
            thread.start();
        }
        for (Thread thread : syncThreads) {
            thread.join();
        }

        long syncTime = System.currentTimeMillis() - syncStart;

        System.out.println("  ReadWriteLock time: " + rwTime + " ms");
        System.out.println("  synchronized time: " + syncTime + " ms");
        System.out.println("  Speedup: " + String.format("%.2fx", (double) syncTime / rwTime));
        System.out.println();
    }

    private static void demonstrateLockInfo() throws InterruptedException {
        System.out.println("6. Lock Information Demo");

        LockInfoExample example = new LockInfoExample();
        example.demonstrateLockInfo();

        System.out.println();
    }
}