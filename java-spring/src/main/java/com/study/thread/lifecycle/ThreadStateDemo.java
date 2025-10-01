package com.study.thread.lifecycle;

import java.util.concurrent.locks.LockSupport;

/**
 * 스레드 상태 전이 데모
 *
 * 스레드의 6가지 상태를 실제로 확인:
 * 1. NEW - 생성됨
 * 2. RUNNABLE - 실행 가능
 * 3. BLOCKED - 동기화 블록 대기
 * 4. WAITING - 무기한 대기
 * 5. TIMED_WAITING - 시간 제한 대기
 * 6. TERMINATED - 종료됨
 */
public class ThreadStateDemo {

    private static final Object lock = new Object();
    private static volatile boolean keepRunning = true;

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Thread State Transition Demo ===\n");

        // 1. NEW 상태 데모
        demonstrateNewState();

        // 2. RUNNABLE 상태 데모
        demonstrateRunnableState();

        // 3. BLOCKED 상태 데모
        demonstrateBlockedState();

        // 4. WAITING 상태 데모
        demonstrateWaitingState();

        // 5. TIMED_WAITING 상태 데모
        demonstrateTimedWaitingState();

        // 6. TERMINATED 상태 데모
        demonstrateTerminatedState();

        // 7. 전체 상태 전이 추적
        demonstrateCompleteLifecycle();
    }

    /**
     * NEW 상태: 스레드가 생성되었지만 아직 start()되지 않음
     */
    private static void demonstrateNewState() {
        System.out.println("1. NEW State Demo");
        Thread thread = new Thread(() -> {
            System.out.println("Thread is running");
        });

        System.out.println("   Thread state: " + thread.getState());
        System.out.println("   Is alive: " + thread.isAlive());
        System.out.println();
    }

    /**
     * RUNNABLE 상태: 스레드가 실행 중이거나 실행 가능한 상태
     */
    private static void demonstrateRunnableState() throws InterruptedException {
        System.out.println("2. RUNNABLE State Demo");
        Thread thread = new Thread(() -> {
            long sum = 0;
            for (int i = 0; i < 1_000_000; i++) {
                sum += i;
            }
            System.out.println("   Computation completed: " + sum);
        });

        thread.start();
        Thread.sleep(10); // 스레드가 실행될 시간을 줌

        System.out.println("   Thread state: " + thread.getState());
        System.out.println("   Is alive: " + thread.isAlive());

        thread.join();
        System.out.println();
    }

    /**
     * BLOCKED 상태: synchronized 블록 진입을 위해 락을 기다림
     */
    private static void demonstrateBlockedState() throws InterruptedException {
        System.out.println("3. BLOCKED State Demo");

        Thread thread1 = new Thread(() -> {
            synchronized (lock) {
                System.out.println("   Thread1 acquired lock");
                try {
                    Thread.sleep(2000); // 락을 오래 보유
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        Thread thread2 = new Thread(() -> {
            synchronized (lock) {
                System.out.println("   Thread2 acquired lock");
            }
        });

        thread1.start();
        Thread.sleep(100); // thread1이 락을 획득할 시간
        thread2.start();
        Thread.sleep(100); // thread2가 BLOCKED 상태가 될 시간

        System.out.println("   Thread1 state: " + thread1.getState());
        System.out.println("   Thread2 state: " + thread2.getState() + " (waiting for lock)");

        thread1.join();
        thread2.join();
        System.out.println();
    }

    /**
     * WAITING 상태: 다른 스레드의 특정 작업을 무기한 대기
     */
    private static void demonstrateWaitingState() throws InterruptedException {
        System.out.println("4. WAITING State Demo");

        Thread waiterThread = new Thread(() -> {
            synchronized (lock) {
                try {
                    System.out.println("   Thread calling wait()");
                    lock.wait(); // 무기한 대기
                    System.out.println("   Thread resumed after notify");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        waiterThread.start();
        Thread.sleep(100); // wait() 호출을 기다림

        System.out.println("   Waiter thread state: " + waiterThread.getState());

        // notify를 통해 깨움
        synchronized (lock) {
            lock.notify();
        }

        waiterThread.join();
        System.out.println();
    }

    /**
     * TIMED_WAITING 상태: 지정된 시간 동안 대기
     */
    private static void demonstrateTimedWaitingState() throws InterruptedException {
        System.out.println("5. TIMED_WAITING State Demo");

        Thread thread = new Thread(() -> {
            try {
                System.out.println("   Thread sleeping for 2 seconds");
                Thread.sleep(2000);
                System.out.println("   Thread woke up");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        thread.start();
        Thread.sleep(100); // sleep() 호출을 기다림

        System.out.println("   Thread state: " + thread.getState());

        thread.join();
        System.out.println();
    }

    /**
     * TERMINATED 상태: 스레드 실행이 완료됨
     */
    private static void demonstrateTerminatedState() throws InterruptedException {
        System.out.println("6. TERMINATED State Demo");

        Thread thread = new Thread(() -> {
            System.out.println("   Thread execution completed");
        });

        thread.start();
        thread.join(); // 스레드 종료 대기

        System.out.println("   Thread state: " + thread.getState());
        System.out.println("   Is alive: " + thread.isAlive());
        System.out.println();
    }

    /**
     * 전체 생명주기 추적: NEW -> RUNNABLE -> TIMED_WAITING -> RUNNABLE -> TERMINATED
     */
    private static void demonstrateCompleteLifecycle() throws InterruptedException {
        System.out.println("7. Complete Lifecycle Demo");

        Thread thread = new Thread(() -> {
            System.out.println("   [RUNNABLE] Thread started execution");

            try {
                Thread.sleep(500);
                System.out.println("   [RUNNABLE] Thread resumed from sleep");

                // 짧은 연산 수행
                long sum = 0;
                for (int i = 0; i < 100_000; i++) {
                    sum += i;
                }

                System.out.println("   [RUNNABLE] Computation done: " + sum);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            System.out.println("   [TERMINATING] Thread about to finish");
        });

        System.out.println("   State: " + thread.getState() + " (NEW)");

        thread.start();
        System.out.println("   State: " + thread.getState() + " (RUNNABLE)");

        Thread.sleep(100);
        System.out.println("   State: " + thread.getState() + " (TIMED_WAITING)");

        Thread.sleep(500);
        System.out.println("   State: " + thread.getState() + " (RUNNABLE or TERMINATED)");

        thread.join();
        System.out.println("   State: " + thread.getState() + " (TERMINATED)");

        System.out.println("\n=== Demo Completed ===");
    }
}