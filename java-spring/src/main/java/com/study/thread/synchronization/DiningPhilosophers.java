package com.study.thread.synchronization;

import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.TimeUnit;

/**
 * 철학자들의 만찬 문제 (Dining Philosophers Problem)
 *
 * Edsger Dijkstra가 제안한 고전적인 동시성 문제입니다.
 *
 * 시나리오:
 * - 5명의 철학자가 원탁에 앉아 있음
 * - 각 철학자 사이에 포크 1개 (총 5개)
 * - 철학자는 생각하거나 식사함
 * - 식사하려면 양쪽 포크 2개가 모두 필요
 *
 * 문제:
 * - 데드락: 모든 철학자가 왼쪽 포크를 잡고 오른쪽을 기다림
 * - 기아(Starvation): 특정 철학자가 계속 식사 못함
 *
 * 해결책:
 * 1. 포크 순서 제한 (자원 순서화)
 * 2. 홀수/짝수 철학자 구분
 * 3. 세마포어로 동시 식사 제한
 * 4. 타임아웃 사용
 */
public class DiningPhilosophers {

    /**
     * 1. 데드락 발생 버전 (잘못된 구현)
     */
    static class DeadlockVersion {
        static class Fork {
            private final int id;
            private final Lock lock = new ReentrantLock();

            public Fork(int id) {
                this.id = id;
            }

            public void pickUp(int philosopherId) {
                lock.lock();
                System.out.println("  Philosopher-" + philosopherId +
                        " picked up fork-" + id);
            }

            public void putDown(int philosopherId) {
                System.out.println("  Philosopher-" + philosopherId +
                        " put down fork-" + id);
                lock.unlock();
            }
        }

        static class Philosopher extends Thread {
            private final int id;
            private final Fork leftFork;
            private final Fork rightFork;
            private int eatCount = 0;

            public Philosopher(int id, Fork leftFork, Fork rightFork) {
                super("Philosopher-" + id);
                this.id = id;
                this.leftFork = leftFork;
                this.rightFork = rightFork;
            }

            @Override
            public void run() {
                try {
                    while (eatCount < 3) {
                        think();
                        eat();
                    }
                    System.out.println("  Philosopher-" + id + " finished dining");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            private void think() throws InterruptedException {
                System.out.println("  Philosopher-" + id + " is thinking");
                Thread.sleep(100);
            }

            private void eat() throws InterruptedException {
                // 왼쪽 포크를 먼저 집음 (데드락 위험!)
                leftFork.pickUp(id);
                Thread.sleep(50); // 데드락 발생 확률 높임

                // 오른쪽 포크를 집으려 시도
                rightFork.pickUp(id);

                System.out.println("  Philosopher-" + id + " is eating (" +
                        (++eatCount) + "/3)");
                Thread.sleep(100);

                rightFork.putDown(id);
                leftFork.putDown(id);
            }
        }
    }

    /**
     * 2. 자원 순서화 해결책 (포크 번호가 작은 것부터)
     */
    static class ResourceOrderingSolution {
        static class Fork {
            private final int id;
            private final Lock lock = new ReentrantLock();

            public Fork(int id) {
                this.id = id;
            }

            public int getId() {
                return id;
            }

            public void pickUp(int philosopherId) {
                lock.lock();
                System.out.println("  Philosopher-" + philosopherId +
                        " picked up fork-" + id);
            }

            public void putDown(int philosopherId) {
                System.out.println("  Philosopher-" + philosopherId +
                        " put down fork-" + id);
                lock.unlock();
            }
        }

        static class Philosopher extends Thread {
            private final int id;
            private final Fork leftFork;
            private final Fork rightFork;
            private int eatCount = 0;

            public Philosopher(int id, Fork leftFork, Fork rightFork) {
                super("Philosopher-" + id);
                this.id = id;
                this.leftFork = leftFork;
                this.rightFork = rightFork;
            }

            @Override
            public void run() {
                try {
                    while (eatCount < 3) {
                        think();
                        eat();
                    }
                    System.out.println("  Philosopher-" + id + " finished dining");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            private void think() throws InterruptedException {
                System.out.println("  Philosopher-" + id + " is thinking");
                Thread.sleep(100);
            }

            private void eat() throws InterruptedException {
                // 번호가 작은 포크를 먼저 집음 (데드락 방지!)
                Fork first, second;
                if (leftFork.getId() < rightFork.getId()) {
                    first = leftFork;
                    second = rightFork;
                } else {
                    first = rightFork;
                    second = leftFork;
                }

                first.pickUp(id);
                second.pickUp(id);

                System.out.println("  Philosopher-" + id + " is eating (" +
                        (++eatCount) + "/3)");
                Thread.sleep(100);

                second.putDown(id);
                first.putDown(id);
            }
        }
    }

    /**
     * 3. 세마포어 해결책 (동시 식사 제한)
     */
    static class SemaphoreSolution {
        static class Fork {
            private final int id;
            private final Semaphore semaphore = new Semaphore(1);

            public Fork(int id) {
                this.id = id;
            }

            public void pickUp(int philosopherId) throws InterruptedException {
                semaphore.acquire();
                System.out.println("  Philosopher-" + philosopherId +
                        " picked up fork-" + id);
            }

            public void putDown(int philosopherId) {
                System.out.println("  Philosopher-" + philosopherId +
                        " put down fork-" + id);
                semaphore.release();
            }
        }

        static class Philosopher extends Thread {
            private final int id;
            private final Fork leftFork;
            private final Fork rightFork;
            private final Semaphore diningRoom;
            private int eatCount = 0;

            public Philosopher(int id, Fork leftFork, Fork rightFork,
                               Semaphore diningRoom) {
                super("Philosopher-" + id);
                this.id = id;
                this.leftFork = leftFork;
                this.rightFork = rightFork;
                this.diningRoom = diningRoom;
            }

            @Override
            public void run() {
                try {
                    while (eatCount < 3) {
                        think();
                        eat();
                    }
                    System.out.println("  Philosopher-" + id + " finished dining");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            private void think() throws InterruptedException {
                System.out.println("  Philosopher-" + id + " is thinking");
                Thread.sleep(100);
            }

            private void eat() throws InterruptedException {
                // 최대 4명만 동시에 식탁에 앉을 수 있음 (데드락 방지)
                diningRoom.acquire();

                leftFork.pickUp(id);
                rightFork.pickUp(id);

                System.out.println("  Philosopher-" + id + " is eating (" +
                        (++eatCount) + "/3)");
                Thread.sleep(100);

                rightFork.putDown(id);
                leftFork.putDown(id);

                diningRoom.release();
            }
        }
    }

    /**
     * 4. 타임아웃 해결책
     */
    static class TimeoutSolution {
        static class Fork {
            private final int id;
            private final Lock lock = new ReentrantLock();

            public Fork(int id) {
                this.id = id;
            }

            public boolean tryPickUp(int philosopherId, long timeout)
                    throws InterruptedException {
                if (lock.tryLock(timeout, TimeUnit.MILLISECONDS)) {
                    System.out.println("  Philosopher-" + philosopherId +
                            " picked up fork-" + id);
                    return true;
                }
                return false;
            }

            public void putDown(int philosopherId) {
                System.out.println("  Philosopher-" + philosopherId +
                        " put down fork-" + id);
                lock.unlock();
            }
        }

        static class Philosopher extends Thread {
            private final int id;
            private final Fork leftFork;
            private final Fork rightFork;
            private int eatCount = 0;
            private int retryCount = 0;

            public Philosopher(int id, Fork leftFork, Fork rightFork) {
                super("Philosopher-" + id);
                this.id = id;
                this.leftFork = leftFork;
                this.rightFork = rightFork;
            }

            @Override
            public void run() {
                try {
                    while (eatCount < 3) {
                        think();
                        eat();
                    }
                    System.out.println("  Philosopher-" + id + " finished dining " +
                            "(retries: " + retryCount + ")");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            private void think() throws InterruptedException {
                System.out.println("  Philosopher-" + id + " is thinking");
                Thread.sleep(100);
            }

            private void eat() throws InterruptedException {
                while (true) {
                    // 왼쪽 포크를 타임아웃으로 시도
                    if (leftFork.tryPickUp(id, 50)) {
                        // 오른쪽 포크도 타임아웃으로 시도
                        if (rightFork.tryPickUp(id, 50)) {
                            // 둘 다 성공!
                            System.out.println("  Philosopher-" + id +
                                    " is eating (" + (++eatCount) + "/3)");
                            Thread.sleep(100);

                            rightFork.putDown(id);
                            leftFork.putDown(id);
                            break;
                        } else {
                            // 오른쪽 실패: 왼쪽 포크 내려놓고 재시도
                            System.out.println("  Philosopher-" + id +
                                    " couldn't get right fork, retrying...");
                            leftFork.putDown(id);
                            retryCount++;
                            Thread.sleep(50);
                        }
                    } else {
                        // 왼쪽 실패: 재시도
                        retryCount++;
                        Thread.sleep(50);
                    }
                }
            }
        }
    }

    /**
     * 5. 홀수/짝수 구분 해결책
     */
    static class OddEvenSolution {
        static class Fork {
            private final int id;
            private final Lock lock = new ReentrantLock();

            public Fork(int id) {
                this.id = id;
            }

            public void pickUp(int philosopherId) {
                lock.lock();
                System.out.println("  Philosopher-" + philosopherId +
                        " picked up fork-" + id);
            }

            public void putDown(int philosopherId) {
                System.out.println("  Philosopher-" + philosopherId +
                        " put down fork-" + id);
                lock.unlock();
            }
        }

        static class Philosopher extends Thread {
            private final int id;
            private final Fork leftFork;
            private final Fork rightFork;
            private int eatCount = 0;

            public Philosopher(int id, Fork leftFork, Fork rightFork) {
                super("Philosopher-" + id);
                this.id = id;
                this.leftFork = leftFork;
                this.rightFork = rightFork;
            }

            @Override
            public void run() {
                try {
                    while (eatCount < 3) {
                        think();
                        eat();
                    }
                    System.out.println("  Philosopher-" + id + " finished dining");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            private void think() throws InterruptedException {
                System.out.println("  Philosopher-" + id + " is thinking");
                Thread.sleep(100);
            }

            private void eat() throws InterruptedException {
                // 홀수 철학자: 왼쪽 -> 오른쪽
                // 짝수 철학자: 오른쪽 -> 왼쪽
                if (id % 2 == 1) {
                    leftFork.pickUp(id);
                    rightFork.pickUp(id);
                } else {
                    rightFork.pickUp(id);
                    leftFork.pickUp(id);
                }

                System.out.println("  Philosopher-" + id + " is eating (" +
                        (++eatCount) + "/3)");
                Thread.sleep(100);

                rightFork.putDown(id);
                leftFork.putDown(id);
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Dining Philosophers Problem ===\n");

        // 1. 데드락 버전 (주의: 데드락 발생 가능)
        // demonstrateDeadlock();

        // 2. 자원 순서화 해결책
        demonstrateResourceOrdering();

        // 3. 세마포어 해결책
        demonstrateSemaphore();

        // 4. 타임아웃 해결책
        demonstrateTimeout();

        // 5. 홀수/짝수 해결책
        demonstrateOddEven();

        System.out.println("\n=== Demo Completed ===");
    }

    @SuppressWarnings("unused")
    private static void demonstrateDeadlock() throws InterruptedException {
        System.out.println("1. Deadlock Version (WARNING: May deadlock!)");
        System.out.println("   Skipping to avoid actual deadlock...\n");

        // 실제로 실행하면 데드락 발생 가능
        // DeadlockVersion.Fork[] forks = new DeadlockVersion.Fork[5];
        // for (int i = 0; i < 5; i++) {
        //     forks[i] = new DeadlockVersion.Fork(i);
        // }
        //
        // DeadlockVersion.Philosopher[] philosophers =
        //     new DeadlockVersion.Philosopher[5];
        // for (int i = 0; i < 5; i++) {
        //     philosophers[i] = new DeadlockVersion.Philosopher(
        //         i, forks[i], forks[(i + 1) % 5]);
        //     philosophers[i].start();
        // }
    }

    private static void demonstrateResourceOrdering() throws InterruptedException {
        System.out.println("2. Resource Ordering Solution");

        ResourceOrderingSolution.Fork[] forks =
                new ResourceOrderingSolution.Fork[5];
        for (int i = 0; i < 5; i++) {
            forks[i] = new ResourceOrderingSolution.Fork(i);
        }

        ResourceOrderingSolution.Philosopher[] philosophers =
                new ResourceOrderingSolution.Philosopher[5];
        for (int i = 0; i < 5; i++) {
            philosophers[i] = new ResourceOrderingSolution.Philosopher(
                    i, forks[i], forks[(i + 1) % 5]);
            philosophers[i].start();
        }

        for (ResourceOrderingSolution.Philosopher p : philosophers) {
            p.join();
        }

        System.out.println("  All philosophers finished (no deadlock!)\n");
    }

    private static void demonstrateSemaphore() throws InterruptedException {
        System.out.println("3. Semaphore Solution");

        SemaphoreSolution.Fork[] forks = new SemaphoreSolution.Fork[5];
        for (int i = 0; i < 5; i++) {
            forks[i] = new SemaphoreSolution.Fork(i);
        }

        Semaphore diningRoom = new Semaphore(4); // 최대 4명만 식탁에

        SemaphoreSolution.Philosopher[] philosophers =
                new SemaphoreSolution.Philosopher[5];
        for (int i = 0; i < 5; i++) {
            philosophers[i] = new SemaphoreSolution.Philosopher(
                    i, forks[i], forks[(i + 1) % 5], diningRoom);
            philosophers[i].start();
        }

        for (SemaphoreSolution.Philosopher p : philosophers) {
            p.join();
        }

        System.out.println("  All philosophers finished (no deadlock!)\n");
    }

    private static void demonstrateTimeout() throws InterruptedException {
        System.out.println("4. Timeout Solution");

        TimeoutSolution.Fork[] forks = new TimeoutSolution.Fork[5];
        for (int i = 0; i < 5; i++) {
            forks[i] = new TimeoutSolution.Fork(i);
        }

        TimeoutSolution.Philosopher[] philosophers =
                new TimeoutSolution.Philosopher[5];
        for (int i = 0; i < 5; i++) {
            philosophers[i] = new TimeoutSolution.Philosopher(
                    i, forks[i], forks[(i + 1) % 5]);
            philosophers[i].start();
        }

        for (TimeoutSolution.Philosopher p : philosophers) {
            p.join();
        }

        System.out.println("  All philosophers finished (no deadlock!)\n");
    }

    private static void demonstrateOddEven() throws InterruptedException {
        System.out.println("5. Odd-Even Solution");

        OddEvenSolution.Fork[] forks = new OddEvenSolution.Fork[5];
        for (int i = 0; i < 5; i++) {
            forks[i] = new OddEvenSolution.Fork(i);
        }

        OddEvenSolution.Philosopher[] philosophers =
                new OddEvenSolution.Philosopher[5];
        for (int i = 0; i < 5; i++) {
            philosophers[i] = new OddEvenSolution.Philosopher(
                    i, forks[i], forks[(i + 1) % 5]);
            philosophers[i].start();
        }

        for (OddEvenSolution.Philosopher p : philosophers) {
            p.join();
        }

        System.out.println("  All philosophers finished (no deadlock!)\n");
    }
}