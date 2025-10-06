package com.study.thread.synchronization;

import java.util.concurrent.locks.StampedLock;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * StampedLock 실습 (Java 8+)
 *
 * StampedLock는 ReadWriteLock보다 향상된 성능을 제공합니다.
 * - Optimistic Read: 락 없이 읽기 시도
 * - Read Lock: 비관적 읽기 락
 * - Write Lock: 배타적 쓰기 락
 * - Stamp 기반 검증
 * - Lock Conversion: 락 변환 지원
 *
 * 주의: StampedLock은 재진입(reentrant)을 지원하지 않습니다!
 */
public class StampedLockDemo {

    /**
     * 1. 기본 StampedLock 사용
     */
    static class Point {
        private final StampedLock lock = new StampedLock();
        private double x, y;

        public Point(double x, double y) {
            this.x = x;
            this.y = y;
        }

        // 쓰기 락
        public void move(double deltaX, double deltaY) {
            long stamp = lock.writeLock();
            try {
                System.out.println("  [" + Thread.currentThread().getName() + "] " +
                        "Write lock acquired (stamp: " + stamp + ")");
                x += deltaX;
                y += deltaY;
                System.out.println("  [" + Thread.currentThread().getName() + "] " +
                        "Moved to (" + x + ", " + y + ")");
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                lock.unlockWrite(stamp);
                System.out.println("  [" + Thread.currentThread().getName() + "] " +
                        "Write lock released");
            }
        }

        // 읽기 락 (비관적)
        public double distanceFromOrigin() {
            long stamp = lock.readLock();
            try {
                System.out.println("  [" + Thread.currentThread().getName() + "] " +
                        "Read lock acquired (stamp: " + stamp + ")");
                double distance = Math.sqrt(x * x + y * y);
                Thread.sleep(50);
                return distance;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return -1;
            } finally {
                lock.unlockRead(stamp);
                System.out.println("  [" + Thread.currentThread().getName() + "] " +
                        "Read lock released");
            }
        }
    }

    /**
     * 2. Optimistic Read (낙관적 읽기)
     */
    static class OptimisticPoint {
        private final StampedLock lock = new StampedLock();
        private double x, y;

        public OptimisticPoint(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public void move(double deltaX, double deltaY) {
            long stamp = lock.writeLock();
            try {
                x += deltaX;
                y += deltaY;
                System.out.println("  [Writer] Moved to (" + x + ", " + y + ")");
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                lock.unlockWrite(stamp);
            }
        }

        // Optimistic Read: 락 없이 읽기 시도
        public double distanceFromOriginOptimistic() {
            long stamp = lock.tryOptimisticRead();
            System.out.println("  [" + Thread.currentThread().getName() + "] " +
                    "Trying optimistic read (stamp: " + stamp + ")");

            // 데이터를 임시 변수에 복사
            double currentX = x;
            double currentY = y;

            try {
                Thread.sleep(30); // 읽기 시뮬레이션
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // 검증: 읽는 동안 쓰기가 발생했는지 확인
            if (!lock.validate(stamp)) {
                System.out.println("  [" + Thread.currentThread().getName() + "] " +
                        "Optimistic read FAILED, upgrading to read lock");

                // 실패 시 읽기 락으로 업그레이드
                stamp = lock.readLock();
                try {
                    currentX = x;
                    currentY = y;
                    System.out.println("  [" + Thread.currentThread().getName() + "] " +
                            "Read with read lock");
                } finally {
                    lock.unlockRead(stamp);
                }
            } else {
                System.out.println("  [" + Thread.currentThread().getName() + "] " +
                        "Optimistic read SUCCEEDED");
            }

            return Math.sqrt(currentX * currentX + currentY * currentY);
        }
    }

    /**
     * 3. Lock Conversion (락 변환)
     */
    static class ConvertiblePoint {
        private final StampedLock lock = new StampedLock();
        private double x, y;

        public ConvertiblePoint(double x, double y) {
            this.x = x;
            this.y = y;
        }

        // Read -> Write 변환
        public void moveIfAtOrigin(double newX, double newY) {
            long stamp = lock.readLock();
            try {
                System.out.println("  [Converter] Read lock acquired");

                while (x == 0.0 && y == 0.0) {
                    // 조건이 맞으면 쓰기 락으로 변환 시도
                    long writeStamp = lock.tryConvertToWriteLock(stamp);

                    if (writeStamp != 0L) {
                        // 변환 성공
                        stamp = writeStamp;
                        System.out.println("  [Converter] Converted to write lock");
                        x = newX;
                        y = newY;
                        System.out.println("  [Converter] Moved to (" + x + ", " + y + ")");
                        break;
                    } else {
                        // 변환 실패: 락 해제 후 쓰기 락 획득
                        System.out.println("  [Converter] Conversion failed, acquiring write lock");
                        lock.unlockRead(stamp);
                        stamp = lock.writeLock();
                    }
                }
            } finally {
                lock.unlock(stamp);
            }
        }

        // Optimistic -> Read 변환
        public double distanceWithConversion() {
            long stamp = lock.tryOptimisticRead();
            double currentX = x;
            double currentY = y;

            if (!lock.validate(stamp)) {
                System.out.println("  [Reader] Converting optimistic to read lock");
                stamp = lock.readLock();
                try {
                    currentX = x;
                    currentY = y;
                } finally {
                    lock.unlockRead(stamp);
                }
            }

            return Math.sqrt(currentX * currentX + currentY * currentY);
        }
    }

    /**
     * 4. tryLock 사용
     */
    static class TryLockPoint {
        private final StampedLock lock = new StampedLock();
        private double x, y;
        private AtomicInteger successCount = new AtomicInteger(0);
        private AtomicInteger failCount = new AtomicInteger(0);

        public TryLockPoint(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public boolean tryMove(double deltaX, double deltaY, long timeoutNanos) {
            long stamp;
            try {
                stamp = lock.tryWriteLock(timeoutNanos, java.util.concurrent.TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("  [" + Thread.currentThread().getName() + "] " +
                        "Interrupted while waiting for lock");
                return false;
            }

            if (stamp == 0L) {
                failCount.incrementAndGet();
                System.out.println("  [" + Thread.currentThread().getName() + "] " +
                        "Failed to acquire write lock");
                return false;
            }

            try {
                successCount.incrementAndGet();
                x += deltaX;
                y += deltaY;
                System.out.println("  [" + Thread.currentThread().getName() + "] " +
                        "Moved to (" + x + ", " + y + ")");
                Thread.sleep(50);
                return true;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            } finally {
                lock.unlockWrite(stamp);
            }
        }

        public void printStats() {
            System.out.println("  Success: " + successCount.get() +
                    ", Failed: " + failCount.get());
        }
    }

    /**
     * 5. 성능 비교: StampedLock vs ReadWriteLock
     */
    static class PerformanceComparison {
        private double data = 0;

        // StampedLock
        private final StampedLock stampedLock = new StampedLock();

        // ReadWriteLock
        private final java.util.concurrent.locks.ReadWriteLock rwLock =
                new java.util.concurrent.locks.ReentrantReadWriteLock();

        public double readWithStamped() {
            long stamp = stampedLock.tryOptimisticRead();
            double value = data;

            if (!stampedLock.validate(stamp)) {
                stamp = stampedLock.readLock();
                try {
                    value = data;
                } finally {
                    stampedLock.unlockRead(stamp);
                }
            }
            return value;
        }

        public void writeWithStamped(double value) {
            long stamp = stampedLock.writeLock();
            try {
                data = value;
            } finally {
                stampedLock.unlockWrite(stamp);
            }
        }

        public double readWithRWLock() {
            rwLock.readLock().lock();
            try {
                return data;
            } finally {
                rwLock.readLock().unlock();
            }
        }

        public void writeWithRWLock(double value) {
            rwLock.writeLock().lock();
            try {
                data = value;
            } finally {
                rwLock.writeLock().unlock();
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== StampedLock Demo ===\n");

        // 1. 기본 사용법
        demonstrateBasicUsage();

        // 2. Optimistic Read
        demonstrateOptimisticRead();

        // 3. Lock Conversion
        demonstrateLockConversion();

        // 4. tryLock
        demonstrateTryLock();

        // 5. 성능 비교
        demonstratePerformance();

        // 6. 주의사항
        demonstrateCaveats();

        System.out.println("\n=== Demo Completed ===");
    }

    private static void demonstrateBasicUsage() throws InterruptedException {
        System.out.println("1. Basic StampedLock Usage");

        Point point = new Point(0, 0);

        Thread writer = new Thread(() -> point.move(3, 4), "Writer");
        Thread reader1 = new Thread(() -> {
            double distance = point.distanceFromOrigin();
            System.out.println("  [Reader-1] Distance: " + distance);
        }, "Reader-1");
        Thread reader2 = new Thread(() -> {
            double distance = point.distanceFromOrigin();
            System.out.println("  [Reader-2] Distance: " + distance);
        }, "Reader-2");

        writer.start();
        Thread.sleep(50);
        reader1.start();
        reader2.start();

        writer.join();
        reader1.join();
        reader2.join();

        System.out.println();
    }

    private static void demonstrateOptimisticRead() throws InterruptedException {
        System.out.println("2. Optimistic Read Demo");

        OptimisticPoint point = new OptimisticPoint(3, 4);

        // Case 1: Optimistic read 성공 (쓰기 없음)
        System.out.println("  Case 1: No concurrent write");
        Thread reader1 = new Thread(() -> {
            double distance = point.distanceFromOriginOptimistic();
            System.out.println("  [Reader] Distance: " + distance);
        }, "OptimisticReader-1");

        reader1.start();
        reader1.join();

        // Case 2: Optimistic read 실패 (쓰기 발생)
        System.out.println("\n  Case 2: Concurrent write occurs");
        Thread writer = new Thread(() -> point.move(1, 1), "Writer");
        Thread reader2 = new Thread(() -> {
            try {
                Thread.sleep(20); // Writer가 먼저 시작하도록
                double distance = point.distanceFromOriginOptimistic();
                System.out.println("  [Reader] Distance: " + distance);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "OptimisticReader-2");

        writer.start();
        reader2.start();

        writer.join();
        reader2.join();

        System.out.println();
    }

    private static void demonstrateLockConversion() throws InterruptedException {
        System.out.println("3. Lock Conversion Demo");

        ConvertiblePoint point = new ConvertiblePoint(0, 0);

        Thread converter = new Thread(() -> point.moveIfAtOrigin(5, 5), "Converter");
        converter.start();
        converter.join();

        System.out.println();
    }

    private static void demonstrateTryLock() throws InterruptedException {
        System.out.println("4. tryLock Demo");

        TryLockPoint point = new TryLockPoint(0, 0);

        // 여러 스레드가 동시에 쓰기 시도
        Thread[] threads = new Thread[5];
        for (int i = 0; i < 5; i++) {
            final int id = i;
            threads[i] = new Thread(() -> {
                // 100ms 타임아웃
                point.tryMove(id, id, 100_000_000L);
            }, "TryWriter-" + id);
        }

        for (Thread thread : threads) {
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        point.printStats();
        System.out.println();
    }

    private static void demonstratePerformance() throws InterruptedException {
        System.out.println("5. Performance Comparison");

        PerformanceComparison test = new PerformanceComparison();
        int iterations = 100000;
        int readerCount = 8;
        int writerCount = 2;

        // StampedLock 테스트
        long stampedStart = System.currentTimeMillis();
        Thread[] stampedThreads = new Thread[readerCount + writerCount];

        for (int i = 0; i < readerCount; i++) {
            stampedThreads[i] = new Thread(() -> {
                for (int j = 0; j < iterations; j++) {
                    test.readWithStamped();
                }
            });
        }

        for (int i = 0; i < writerCount; i++) {
            final int id = i;
            stampedThreads[readerCount + i] = new Thread(() -> {
                for (int j = 0; j < iterations / 10; j++) {
                    test.writeWithStamped(id);
                }
            });
        }

        for (Thread thread : stampedThreads) {
            thread.start();
        }
        for (Thread thread : stampedThreads) {
            thread.join();
        }

        long stampedTime = System.currentTimeMillis() - stampedStart;

        // ReadWriteLock 테스트
        long rwStart = System.currentTimeMillis();
        Thread[] rwThreads = new Thread[readerCount + writerCount];

        for (int i = 0; i < readerCount; i++) {
            rwThreads[i] = new Thread(() -> {
                for (int j = 0; j < iterations; j++) {
                    test.readWithRWLock();
                }
            });
        }

        for (int i = 0; i < writerCount; i++) {
            final int id = i;
            rwThreads[readerCount + i] = new Thread(() -> {
                for (int j = 0; j < iterations / 10; j++) {
                    test.writeWithRWLock(id);
                }
            });
        }

        for (Thread thread : rwThreads) {
            thread.start();
        }
        for (Thread thread : rwThreads) {
            thread.join();
        }

        long rwTime = System.currentTimeMillis() - rwStart;

        System.out.println("  StampedLock (Optimistic): " + stampedTime + " ms");
        System.out.println("  ReadWriteLock: " + rwTime + " ms");
        System.out.println("  Speedup: " + String.format("%.2fx", (double) rwTime / stampedTime));
        System.out.println();
    }

    private static void demonstrateCaveats() {
        System.out.println("6. Important Caveats");

        StampedLock lock = new StampedLock();

        System.out.println("  ⚠️  StampedLock is NOT reentrant!");
        System.out.println("  Example of DEADLOCK:");

        Thread thread = new Thread(() -> {
            long stamp1 = lock.writeLock();
            System.out.println("  Acquired first write lock");

            try {
                // 같은 스레드가 다시 락을 획득하려고 하면 데드락!
                System.out.println("  Trying to acquire write lock again...");

                // 타임아웃을 사용하여 데드락 방지
                long stamp2 = lock.tryWriteLock(100, java.util.concurrent.TimeUnit.MILLISECONDS);

                if (stamp2 == 0L) {
                    System.out.println("  ❌ Failed! StampedLock is not reentrant");
                } else {
                    lock.unlockWrite(stamp2);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                lock.unlockWrite(stamp1);
            }
        });

        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("\n  Key Points:");
        System.out.println("  - Not reentrant (unlike ReentrantLock/synchronized)");
        System.out.println("  - No Condition variables support");
        System.out.println("  - Optimistic read can improve performance significantly");
        System.out.println("  - Use unlock(stamp) or specific unlock methods");
        System.out.println();
    }
}