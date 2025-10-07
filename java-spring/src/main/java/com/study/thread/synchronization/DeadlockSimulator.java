package com.study.thread.synchronization;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.TimeUnit;

/**
 * 데드락 시뮬레이션
 *
 * 데드락(Deadlock)이 발생하는 다양한 시나리오를 시뮬레이션합니다.
 *
 * 데드락 발생 조건 (Coffman conditions):
 * 1. Mutual Exclusion (상호 배제): 리소스를 한 번에 하나의 스레드만 사용
 * 2. Hold and Wait (보유 및 대기): 리소스를 보유한 채 다른 리소스 대기
 * 3. No Preemption (비선점): 리소스를 강제로 빼앗을 수 없음
 * 4. Circular Wait (순환 대기): 리소스 대기가 순환 구조
 */
public class DeadlockSimulator {

    /**
     * 1. 단순 데드락 (2개 리소스, 2개 스레드)
     */
    static class SimpleDeadlock {
        private final Object resourceA = new Object();
        private final Object resourceB = new Object();

        public void demonstrateDeadlock() {
            Thread thread1 = new Thread(() -> {
                synchronized (resourceA) {
                    System.out.println("  [Thread-1] Acquired Resource A");
                    sleep(50); // 데드락 발생 확률 높임

                    System.out.println("  [Thread-1] Waiting for Resource B...");
                    synchronized (resourceB) {
                        System.out.println("  [Thread-1] Acquired Resource B");
                    }
                }
            }, "Thread-1");

            Thread thread2 = new Thread(() -> {
                synchronized (resourceB) {
                    System.out.println("  [Thread-2] Acquired Resource B");
                    sleep(50);

                    System.out.println("  [Thread-2] Waiting for Resource A...");
                    synchronized (resourceA) {
                        System.out.println("  [Thread-2] Acquired Resource A");
                    }
                }
            }, "Thread-2");

            thread1.start();
            thread2.start();

            // 데드락 감지를 위한 타임아웃
            try {
                thread1.join(2000);
                thread2.join(2000);

                if (thread1.isAlive() || thread2.isAlive()) {
                    System.out.println("  ⚠️  DEADLOCK DETECTED!");
                    System.out.println("  Thread-1 state: " + thread1.getState());
                    System.out.println("  Thread-2 state: " + thread2.getState());

                    // 스레드 강제 종료
                    thread1.interrupt();
                    thread2.interrupt();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 2. 다중 리소스 데드락 (3개 리소스, 3개 스레드)
     */
    static class MultiResourceDeadlock {
        private final Object resourceA = new Object();
        private final Object resourceB = new Object();
        private final Object resourceC = new Object();

        public void demonstrateDeadlock() {
            // Thread-1: A -> B
            Thread thread1 = new Thread(() -> {
                synchronized (resourceA) {
                    System.out.println("  [Thread-1] Acquired Resource A");
                    sleep(50);

                    System.out.println("  [Thread-1] Waiting for Resource B...");
                    synchronized (resourceB) {
                        System.out.println("  [Thread-1] Acquired Resource B");
                    }
                }
            }, "Thread-1");

            // Thread-2: B -> C
            Thread thread2 = new Thread(() -> {
                synchronized (resourceB) {
                    System.out.println("  [Thread-2] Acquired Resource B");
                    sleep(50);

                    System.out.println("  [Thread-2] Waiting for Resource C...");
                    synchronized (resourceC) {
                        System.out.println("  [Thread-2] Acquired Resource C");
                    }
                }
            }, "Thread-2");

            // Thread-3: C -> A (순환!)
            Thread thread3 = new Thread(() -> {
                synchronized (resourceC) {
                    System.out.println("  [Thread-3] Acquired Resource C");
                    sleep(50);

                    System.out.println("  [Thread-3] Waiting for Resource A...");
                    synchronized (resourceA) {
                        System.out.println("  [Thread-3] Acquired Resource A");
                    }
                }
            }, "Thread-3");

            thread1.start();
            thread2.start();
            thread3.start();

            checkForDeadlock(thread1, thread2, thread3);
        }

        private void checkForDeadlock(Thread... threads) {
            try {
                for (Thread thread : threads) {
                    thread.join(2000);
                }

                boolean deadlocked = false;
                for (Thread thread : threads) {
                    if (thread.isAlive()) {
                        deadlocked = true;
                        System.out.println("  " + thread.getName() +
                                " state: " + thread.getState());
                    }
                }

                if (deadlocked) {
                    System.out.println("  ⚠️  CIRCULAR DEADLOCK DETECTED!");
                    for (Thread thread : threads) {
                        thread.interrupt();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 3. 은행 계좌 이체 데드락
     */
    static class BankTransferDeadlock {
        static class Account {
            private final int id;
            private double balance;
            private final Lock lock = new ReentrantLock();

            public Account(int id, double balance) {
                this.id = id;
                this.balance = balance;
            }

            public void lock() {
                lock.lock();
            }

            public void unlock() {
                lock.unlock();
            }

            public void deposit(double amount) {
                balance += amount;
            }

            public void withdraw(double amount) {
                balance -= amount;
            }

            public double getBalance() {
                return balance;
            }

            public int getId() {
                return id;
            }
        }

        public void demonstrateDeadlock() {
            Account account1 = new Account(1, 1000);
            Account account2 = new Account(2, 1000);

            // Thread-1: account1 -> account2로 이체
            Thread thread1 = new Thread(() -> {
                transferDeadlock(account1, account2, 100);
            }, "Transfer-1");

            // Thread-2: account2 -> account1로 이체
            Thread thread2 = new Thread(() -> {
                transferDeadlock(account2, account1, 200);
            }, "Transfer-2");

            thread1.start();
            thread2.start();

            try {
                thread1.join(2000);
                thread2.join(2000);

                if (thread1.isAlive() || thread2.isAlive()) {
                    System.out.println("  ⚠️  DEADLOCK in money transfer!");
                    thread1.interrupt();
                    thread2.interrupt();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // 데드락 발생 버전
        private void transferDeadlock(Account from, Account to, double amount) {
            from.lock();
            System.out.println("  [" + Thread.currentThread().getName() + "] " +
                    "Locked account " + from.getId());

            sleep(50);

            System.out.println("  [" + Thread.currentThread().getName() + "] " +
                    "Waiting for account " + to.getId() + "...");
            to.lock();

            try {
                from.withdraw(amount);
                to.deposit(amount);
                System.out.println("  [" + Thread.currentThread().getName() + "] " +
                        "Transferred $" + amount + " from account " +
                        from.getId() + " to " + to.getId());
            } finally {
                to.unlock();
                from.unlock();
            }
        }
    }

    /**
     * 4. 데드락 방지: 리소스 순서화
     */
    static class DeadlockPrevention {
        static class Account {
            private final int id;
            private double balance;
            private final Lock lock = new ReentrantLock();

            public Account(int id, double balance) {
                this.id = id;
                this.balance = balance;
            }

            public int getId() {
                return id;
            }

            public Lock getLock() {
                return lock;
            }

            public void deposit(double amount) {
                balance += amount;
            }

            public void withdraw(double amount) {
                balance -= amount;
            }

            public double getBalance() {
                return balance;
            }
        }

        public void demonstratePreventionByOrdering() {
            Account account1 = new Account(1, 1000);
            Account account2 = new Account(2, 1000);

            Thread thread1 = new Thread(() -> {
                transferSafe(account1, account2, 100);
            }, "Transfer-1");

            Thread thread2 = new Thread(() -> {
                transferSafe(account2, account1, 200);
            }, "Transfer-2");

            thread1.start();
            thread2.start();

            try {
                thread1.join();
                thread2.join();
                System.out.println("  ✓ No deadlock! Both transfers completed");
                System.out.println("  Account 1 balance: $" + account1.getBalance());
                System.out.println("  Account 2 balance: $" + account2.getBalance());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // 리소스 순서화로 데드락 방지
        private void transferSafe(Account from, Account to, double amount) {
            Account first, second;

            // ID가 작은 계좌를 먼저 락
            if (from.getId() < to.getId()) {
                first = from;
                second = to;
            } else {
                first = to;
                second = from;
            }

            first.getLock().lock();
            System.out.println("  [" + Thread.currentThread().getName() + "] " +
                    "Locked account " + first.getId());

            try {
                second.getLock().lock();
                System.out.println("  [" + Thread.currentThread().getName() + "] " +
                        "Locked account " + second.getId());

                try {
                    from.withdraw(amount);
                    to.deposit(amount);
                    System.out.println("  [" + Thread.currentThread().getName() + "] " +
                            "Transferred $" + amount);
                } finally {
                    second.getLock().unlock();
                }
            } finally {
                first.getLock().unlock();
            }
        }
    }

    /**
     * 5. 데드락 방지: 타임아웃
     */
    static class TimeoutPrevention {
        static class Account {
            private final int id;
            private double balance;
            private final Lock lock = new ReentrantLock();

            public Account(int id, double balance) {
                this.id = id;
                this.balance = balance;
            }

            public int getId() {
                return id;
            }

            public boolean tryLock(long timeout) throws InterruptedException {
                return lock.tryLock(timeout, TimeUnit.MILLISECONDS);
            }

            public void unlock() {
                lock.unlock();
            }

            public void deposit(double amount) {
                balance += amount;
            }

            public void withdraw(double amount) {
                balance -= amount;
            }

            public double getBalance() {
                return balance;
            }
        }

        public void demonstrateTimeoutPrevention() {
            Account account1 = new Account(1, 1000);
            Account account2 = new Account(2, 1000);

            Thread thread1 = new Thread(() -> {
                transferWithTimeout(account1, account2, 100);
            }, "Transfer-1");

            Thread thread2 = new Thread(() -> {
                transferWithTimeout(account2, account1, 200);
            }, "Transfer-2");

            thread1.start();
            thread2.start();

            try {
                thread1.join();
                thread2.join();
                System.out.println("  ✓ No deadlock with timeout strategy!");
                System.out.println("  Account 1 balance: $" + account1.getBalance());
                System.out.println("  Account 2 balance: $" + account2.getBalance());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        private void transferWithTimeout(Account from, Account to, double amount) {
            int retries = 0;
            int maxRetries = 5;

            while (retries < maxRetries) {
                try {
                    if (from.tryLock(50)) {
                        System.out.println("  [" + Thread.currentThread().getName() + "] " +
                                "Locked account " + from.getId());

                        try {
                            if (to.tryLock(50)) {
                                System.out.println("  [" + Thread.currentThread().getName() +
                                        "] Locked account " + to.getId());

                                try {
                                    from.withdraw(amount);
                                    to.deposit(amount);
                                    System.out.println("  [" + Thread.currentThread().getName() +
                                            "] Transferred $" + amount);
                                    return; // 성공
                                } finally {
                                    to.unlock();
                                }
                            } else {
                                System.out.println("  [" + Thread.currentThread().getName() +
                                        "] Timeout on account " + to.getId() +
                                        ", retrying...");
                            }
                        } finally {
                            from.unlock();
                        }
                    }

                    retries++;
                    sleep(10); // 재시도 전 대기

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }

            System.out.println("  [" + Thread.currentThread().getName() + "] " +
                    "Failed after " + maxRetries + " retries");
        }
    }

    /**
     * 6. 라이브락 시뮬레이션
     */
    static class LivelockSimulation {
        static class Person {
            private final String name;
            private boolean hasPath = false;

            public Person(String name) {
                this.name = name;
            }

            public void passBy(Person other) {
                while (!hasPath) {
                    if (!other.hasPath) {
                        System.out.println("  " + name + ": After you!");
                        sleep(10);
                    } else {
                        hasPath = true;
                        System.out.println("  " + name + ": Thank you, passing through");
                    }
                }
            }

            public void setHasPath(boolean hasPath) {
                this.hasPath = hasPath;
            }
        }

        public void demonstrateLivelock() {
            Person person1 = new Person("Person-1");
            Person person2 = new Person("Person-2");

            Thread thread1 = new Thread(() -> person1.passBy(person2), "Thread-1");
            Thread thread2 = new Thread(() -> person2.passBy(person1), "Thread-2");

            thread1.start();
            thread2.start();

            try {
                Thread.sleep(500); // 라이브락 관찰

                System.out.println("  ⚠️  LIVELOCK DETECTED! Both keep yielding to each other");

                // 라이브락 해결: 한쪽에 우선권 부여
                person1.setHasPath(true);

                thread1.join();
                thread2.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static void main(String[] args) {
        System.out.println("=== Deadlock Simulator ===\n");

        // 1. 단순 데드락
        System.out.println("1. Simple Deadlock (2 resources, 2 threads)");
        new SimpleDeadlock().demonstrateDeadlock();
        System.out.println();

        // 2. 다중 리소스 데드락
        System.out.println("2. Multi-Resource Deadlock (3 resources, 3 threads)");
        new MultiResourceDeadlock().demonstrateDeadlock();
        System.out.println();

        // 3. 은행 이체 데드락
        System.out.println("3. Bank Transfer Deadlock");
        new BankTransferDeadlock().demonstrateDeadlock();
        System.out.println();

        // 4. 데드락 방지: 리소스 순서화
        System.out.println("4. Deadlock Prevention: Resource Ordering");
        new DeadlockPrevention().demonstratePreventionByOrdering();
        System.out.println();

        // 5. 데드락 방지: 타임아웃
        System.out.println("5. Deadlock Prevention: Timeout Strategy");
        new TimeoutPrevention().demonstrateTimeoutPrevention();
        System.out.println();

        // 6. 라이브락
        System.out.println("6. Livelock Simulation");
        new LivelockSimulation().demonstrateLivelock();
        System.out.println();

        System.out.println("=== Demo Completed ===");
    }
}