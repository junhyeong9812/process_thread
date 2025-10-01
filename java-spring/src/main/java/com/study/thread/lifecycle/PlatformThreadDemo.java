package com.study.thread.lifecycle;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Platform Thread 실습
 *
 * 전통적인 Java 플랫폼 스레드(OS 스레드와 1:1 매핑)의 특징과 동작을 학습합니다.
 * - 스레드 생성 방법 (Thread, Runnable)
 * - 스레드 속성 (우선순위, 데몬, 이름)
 * - 스레드 그룹
 * - 스레드 생성 비용 측정
 */
public class PlatformThreadDemo {

    private static final AtomicInteger threadCounter = new AtomicInteger(0);

    /**
     * 1. Thread 클래스 상속 방식
     */
    static class CustomThread extends Thread {
        private final int taskId;

        public CustomThread(int taskId) {
            this.taskId = taskId;
            setName("CustomThread-" + taskId);
        }

        @Override
        public void run() {
            System.out.println("  [" + getName() + "] Task " + taskId +
                    " running on thread ID: " + getId());
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.out.println("  [" + getName() + "] Task " + taskId + " completed");
        }
    }

    /**
     * 2. Runnable 인터페이스 구현 방식
     */
    static class CustomTask implements Runnable {
        private final int taskId;

        public CustomTask(int taskId) {
            this.taskId = taskId;
        }

        @Override
        public void run() {
            threadCounter.incrementAndGet();
            Thread current = Thread.currentThread();
            System.out.println("  [" + current.getName() + "] Task " + taskId +
                    " running (Thread ID: " + current.getId() + ")");
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Platform Thread Demo ===\n");

        // 1. Thread 상속 방식
        demonstrateThreadInheritance();

        // 2. Runnable 구현 방식
        demonstrateRunnableImplementation();

        // 3. 람다 표현식 사용
        demonstrateLambdaExpression();

        // 4. 스레드 속성 설정
        demonstrateThreadProperties();

        // 5. 데몬 스레드
        demonstrateDaemonThread();

        // 6. 스레드 그룹
        demonstrateThreadGroup();

        // 7. 스레드 생성 비용 측정
        measureThreadCreationCost();

        // 8. 스레드 풀 없이 대량 작업 처리
        demonstrateMassiveThreadCreation();
    }

    private static void demonstrateThreadInheritance() throws InterruptedException {
        System.out.println("1. Thread Inheritance Demo");

        List<Thread> threads = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            Thread thread = new CustomThread(i);
            threads.add(thread);
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        System.out.println();
    }

    private static void demonstrateRunnableImplementation() throws InterruptedException {
        System.out.println("2. Runnable Implementation Demo");

        threadCounter.set(0);
        List<Thread> threads = new ArrayList<>();

        for (int i = 1; i <= 3; i++) {
            Thread thread = new Thread(new CustomTask(i), "RunnableThread-" + i);
            threads.add(thread);
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        System.out.println("  Total tasks executed: " + threadCounter.get());
        System.out.println();
    }

    private static void demonstrateLambdaExpression() throws InterruptedException {
        System.out.println("3. Lambda Expression Demo");

        List<Thread> threads = new ArrayList<>();

        for (int i = 1; i <= 3; i++) {
            final int taskId = i;
            Thread thread = new Thread(() -> {
                System.out.println("  [Lambda-" + taskId + "] Running on thread: " +
                        Thread.currentThread().getName());
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "LambdaThread-" + i);

            threads.add(thread);
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        System.out.println();
    }

    private static void demonstrateThreadProperties() throws InterruptedException {
        System.out.println("4. Thread Properties Demo");

        Thread thread = new Thread(() -> {
            Thread current = Thread.currentThread();
            System.out.println("  Thread Name: " + current.getName());
            System.out.println("  Thread ID: " + current.getId());
            System.out.println("  Thread Priority: " + current.getPriority());
            System.out.println("  Is Daemon: " + current.isDaemon());
            System.out.println("  Thread Group: " + current.getThreadGroup().getName());
            System.out.println("  Thread State: " + current.getState());
        });

        // 스레드 속성 설정
        thread.setName("PropertyTestThread");
        thread.setPriority(Thread.MAX_PRIORITY);

        System.out.println("  Before start - State: " + thread.getState());
        thread.start();

        Thread.sleep(50);
        System.out.println("  During execution - State: " + thread.getState());

        thread.join();
        System.out.println("  After join - State: " + thread.getState());
        System.out.println();
    }

    private static void demonstrateDaemonThread() throws InterruptedException {
        System.out.println("5. Daemon Thread Demo");

        // 일반 스레드 (User Thread)
        Thread userThread = new Thread(() -> {
            System.out.println("  User thread started");
            try {
                Thread.sleep(500);
                System.out.println("  User thread completed");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "UserThread");

        // 데몬 스레드
        Thread daemonThread = new Thread(() -> {
            System.out.println("  Daemon thread started");
            try {
                while (true) {
                    Thread.sleep(100);
                    System.out.println("  Daemon thread running...");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "DaemonThread");

        daemonThread.setDaemon(true); // 데몬으로 설정

        System.out.println("  UserThread is daemon: " + userThread.isDaemon());
        System.out.println("  DaemonThread is daemon: " + daemonThread.isDaemon());

        userThread.start();
        daemonThread.start();

        userThread.join();
        System.out.println("  Main thread ending (daemon will be terminated automatically)");
        Thread.sleep(200);
        System.out.println();
    }

    private static void demonstrateThreadGroup() throws InterruptedException {
        System.out.println("6. Thread Group Demo");

        ThreadGroup workerGroup = new ThreadGroup("WorkerGroup");

        System.out.println("  Created thread group: " + workerGroup.getName());

        List<Thread> threads = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            final int taskId = i;
            Thread thread = new Thread(workerGroup, () -> {
                System.out.println("  [Worker-" + taskId + "] Running in group: " +
                        Thread.currentThread().getThreadGroup().getName());
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "Worker-" + i);

            threads.add(thread);
            thread.start();
        }

        Thread.sleep(50);
        System.out.println("  Active threads in group: " + workerGroup.activeCount());

        for (Thread thread : threads) {
            thread.join();
        }

        System.out.println("  Active threads after join: " + workerGroup.activeCount());
        System.out.println();
    }

    private static void measureThreadCreationCost() throws InterruptedException {
        System.out.println("7. Thread Creation Cost Measurement");

        int threadCount = 1000;

        // 생성 시간 측정
        long startCreate = System.nanoTime();
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            Thread thread = new Thread(() -> {
                // 최소한의 작업
                long sum = 0;
                for (int j = 0; j < 100; j++) {
                    sum += j;
                }
            });
            threads.add(thread);
        }
        long createTime = System.nanoTime() - startCreate;

        // 시작 시간 측정
        long startRun = System.nanoTime();
        for (Thread thread : threads) {
            thread.start();
        }
        long startTime = System.nanoTime() - startRun;

        // 종료 대기
        long startJoin = System.nanoTime();
        for (Thread thread : threads) {
            thread.join();
        }
        long joinTime = System.nanoTime() - startJoin;

        System.out.println("  Thread count: " + threadCount);
        System.out.println("  Creation time: " + createTime / 1_000_000 + " ms");
        System.out.println("  Start time: " + startTime / 1_000_000 + " ms");
        System.out.println("  Join time: " + joinTime / 1_000_000 + " ms");
        System.out.println("  Total time: " + (createTime + startTime + joinTime) / 1_000_000 + " ms");
        System.out.println("  Avg per thread: " +
                (createTime + startTime + joinTime) / threadCount / 1000 + " μs");
        System.out.println();
    }

    private static void demonstrateMassiveThreadCreation() throws InterruptedException {
        System.out.println("8. Massive Thread Creation Demo (Warning: Resource intensive)");

        int threadCount = 100;
        AtomicInteger completedTasks = new AtomicInteger(0);

        System.out.println("  Creating " + threadCount + " platform threads...");

        long startTime = System.currentTimeMillis();

        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            final int taskId = i;
            Thread thread = new Thread(() -> {
                try {
                    Thread.sleep(10); // 짧은 작업 시뮬레이션
                    completedTasks.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "MassThread-" + i);

            threads.add(thread);
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        long elapsedTime = System.currentTimeMillis() - startTime;

        System.out.println("  Completed: " + completedTasks.get() + " / " + threadCount);
        System.out.println("  Total time: " + elapsedTime + " ms");
        System.out.println("  Note: Platform threads are expensive - each maps to an OS thread");
        System.out.println();

        System.out.println("=== Demo Completed ===");
    }
}