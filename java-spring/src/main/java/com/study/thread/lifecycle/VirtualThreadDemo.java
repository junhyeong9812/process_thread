package com.study.thread.lifecycle;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Virtual Thread 실습 (Java 21+)
 *
 * 가상 스레드의 특징과 플랫폼 스레드와의 차이점을 학습합니다.
 * - 경량 스레드 (OS 스레드와 분리)
 * - 대량 생성 가능
 * - 블로킹 작업에 최적화
 * - 스케줄링은 JVM이 담당
 */
public class VirtualThreadDemo {

    private static final AtomicInteger taskCounter = new AtomicInteger(0);

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Virtual Thread Demo (Java 21+) ===\n");

        // Java 버전 확인
        checkJavaVersion();

        // 1. 기본 Virtual Thread 생성
        demonstrateBasicVirtualThread();

        // 2. Thread.ofVirtual() 사용
        demonstrateOfVirtual();

        // 3. Virtual Thread Factory
        demonstrateVirtualThreadFactory();

        // 4. 대량 Virtual Thread 생성
        demonstrateMassiveVirtualThreads();

        // 5. Blocking I/O 시뮬레이션
        demonstrateBlockingIO();

        // 6. Virtual vs Platform 성능 비교
        compareVirtualVsPlatform();

        // 7. Virtual Thread 특성
        demonstrateVirtualThreadCharacteristics();
    }

    private static void checkJavaVersion() {
        String version = System.getProperty("java.version");
        System.out.println("Java Version: " + version);

        try {
            // Virtual Thread가 사용 가능한지 확인
            Thread.ofVirtual().start(() -> {}).join();
            System.out.println("Virtual Threads: Available ✓\n");
        } catch (Exception e) {
            System.out.println("Virtual Threads: Not Available ✗");
            System.out.println("Requires Java 21 or higher\n");
        }
    }

    private static void demonstrateBasicVirtualThread() throws InterruptedException {
        System.out.println("1. Basic Virtual Thread Demo");

        // Thread.startVirtualThread() 사용
        Thread vThread = Thread.startVirtualThread(() -> {
            System.out.println("  [Virtual] Thread name: " + Thread.currentThread().getName());
            System.out.println("  [Virtual] Is virtual: " + Thread.currentThread().isVirtual());
            System.out.println("  [Virtual] Thread ID: " + Thread.currentThread().getId());

            try {
                Thread.sleep(100);
                System.out.println("  [Virtual] Task completed");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        vThread.join();
        System.out.println();
    }

    private static void demonstrateOfVirtual() throws InterruptedException {
        System.out.println("2. Thread.ofVirtual() Demo");

        List<Thread> threads = new ArrayList<>();

        for (int i = 1; i <= 5; i++) {
            final int taskId = i;

            Thread vThread = Thread.ofVirtual()
                    .name("VirtualWorker-" + taskId)
                    .start(() -> {
                        System.out.println("  [" + Thread.currentThread().getName() + "] " +
                                "Task " + taskId + " running");
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    });

            threads.add(vThread);
        }

        for (Thread thread : threads) {
            thread.join();
        }

        System.out.println("  All virtual threads completed");
        System.out.println();
    }

    private static void demonstrateVirtualThreadFactory() throws InterruptedException {
        System.out.println("3. Virtual Thread Factory Demo");

        // Virtual Thread Factory 생성
        var factory = Thread.ofVirtual()
                .name("FactoryThread-", 0)
                .factory();

        List<Thread> threads = new ArrayList<>();

        for (int i = 1; i <= 5; i++) {
            final int taskId = i;
            Thread thread = factory.newThread(() -> {
                System.out.println("  [" + Thread.currentThread().getName() + "] " +
                        "Factory task " + taskId);
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });

            threads.add(thread);
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        System.out.println();
    }

    private static void demonstrateMassiveVirtualThreads() throws InterruptedException {
        System.out.println("4. Massive Virtual Threads Demo");

        int threadCount = 10000; // 1만개의 가상 스레드!
        AtomicInteger completedTasks = new AtomicInteger(0);

        System.out.println("  Creating " + threadCount + " virtual threads...");

        Instant start = Instant.now();

        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            Thread vThread = Thread.ofVirtual().start(() -> {
                try {
                    Thread.sleep(10); // 짧은 작업
                    completedTasks.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            threads.add(vThread);
        }

        for (Thread thread : threads) {
            thread.join();
        }

        Duration elapsed = Duration.between(start, Instant.now());

        System.out.println("  Completed: " + completedTasks.get() + " / " + threadCount);
        System.out.println("  Total time: " + elapsed.toMillis() + " ms");
        System.out.println("  Avg per thread: " +
                (elapsed.toNanos() / threadCount / 1000) + " μs");
        System.out.println("  Virtual threads are lightweight - can create millions!");
        System.out.println();
    }

    private static void demonstrateBlockingIO() throws InterruptedException {
        System.out.println("5. Blocking I/O Simulation Demo");

        int taskCount = 100;

        System.out.println("  Simulating " + taskCount + " blocking I/O tasks...");

        Instant start = Instant.now();

        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < taskCount; i++) {
            final int taskId = i;

            Thread vThread = Thread.ofVirtual().start(() -> {
                try {
                    // Blocking I/O 시뮬레이션 (예: 네트워크 요청, 파일 읽기)
                    Thread.sleep(100);

                    if (taskId % 20 == 0) {
                        System.out.println("  Task " + taskId + " completed");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });

            threads.add(vThread);
        }

        for (Thread thread : threads) {
            thread.join();
        }

        Duration elapsed = Duration.between(start, Instant.now());

        System.out.println("  All " + taskCount + " blocking tasks completed");
        System.out.println("  Total time: " + elapsed.toMillis() + " ms");
        System.out.println("  Virtual threads don't block OS threads during I/O");
        System.out.println();
    }

    private static void compareVirtualVsPlatform() throws InterruptedException {
        System.out.println("6. Virtual vs Platform Thread Performance");

        int taskCount = 1000;

        // Platform Thread 테스트
        System.out.println("  Testing Platform Threads...");
        Instant platformStart = Instant.now();

        List<Thread> platformThreads = new ArrayList<>();
        for (int i = 0; i < taskCount; i++) {
            Thread thread = new Thread(() -> {
                try {
                    Thread.sleep(10);
                    taskCounter.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            platformThreads.add(thread);
            thread.start();
        }

        for (Thread thread : platformThreads) {
            thread.join();
        }

        Duration platformTime = Duration.between(platformStart, Instant.now());

        // Virtual Thread 테스트
        System.out.println("  Testing Virtual Threads...");
        taskCounter.set(0);
        Instant virtualStart = Instant.now();

        List<Thread> virtualThreads = new ArrayList<>();
        for (int i = 0; i < taskCount; i++) {
            Thread vThread = Thread.ofVirtual().start(() -> {
                try {
                    Thread.sleep(10);
                    taskCounter.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            virtualThreads.add(vThread);
        }

        for (Thread thread : virtualThreads) {
            thread.join();
        }

        Duration virtualTime = Duration.between(virtualStart, Instant.now());

        // 결과 비교
        System.out.println("\n  Results for " + taskCount + " threads:");
        System.out.println("  Platform Thread time: " + platformTime.toMillis() + " ms");
        System.out.println("  Virtual Thread time:  " + virtualTime.toMillis() + " ms");
        System.out.println("  Speedup: " +
                String.format("%.2fx", (double) platformTime.toMillis() / virtualTime.toMillis()));
        System.out.println();
    }

    private static void demonstrateVirtualThreadCharacteristics() throws InterruptedException {
        System.out.println("7. Virtual Thread Characteristics");

        Thread vThread = Thread.ofVirtual().name("CharacteristicTest").start(() -> {
            Thread current = Thread.currentThread();

            System.out.println("  Thread name: " + current.getName());
            System.out.println("  Is virtual: " + current.isVirtual());
            System.out.println("  Is daemon: " + current.isDaemon());
            System.out.println("  Priority: " + current.getPriority());
            System.out.println("  Thread group: " +
                    (current.getThreadGroup() != null ?
                            current.getThreadGroup().getName() : "null"));
            System.out.println("  State: " + current.getState());

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        vThread.join();

        System.out.println("\n  Key Points:");
        System.out.println("  - Virtual threads are always daemon threads");
        System.out.println("  - Priority setting has no effect");
        System.out.println("  - Not tied to specific thread groups");
        System.out.println("  - Managed by JVM, not OS");
        System.out.println();

        System.out.println("=== Demo Completed ===");
    }
}