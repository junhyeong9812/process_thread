package com.virtualthread.os.thread.lifecycle;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 스레드 생명주기 관찰자
 *
 * 스레드의 상태 변화를 실시간으로 추적하고 기록합니다.
 * - 상태 전이 추적
 * - 실행 시간 측정
 * - 통계 정보 수집
 */
public class ThreadLifecycleObserver {

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    /**
     * 스레드 상태 전이 이벤트
     */
    static class StateTransition {
        final long threadId;
        final String threadName;
        final Thread.State fromState;
        final Thread.State toState;
        final LocalDateTime timestamp;
        final long elapsedMillis;

        StateTransition(long threadId, String threadName,
                        Thread.State fromState, Thread.State toState,
                        long elapsedMillis) {
            this.threadId = threadId;
            this.threadName = threadName;
            this.fromState = fromState;
            this.toState = toState;
            this.timestamp = LocalDateTime.now();
            this.elapsedMillis = elapsedMillis;
        }

        @Override
        public String toString() {
            return String.format("[%s] Thread-%d (%s): %s -> %s (elapsed: %dms)",
                    timestamp.format(TIME_FORMATTER),
                    threadId,
                    threadName,
                    fromState,
                    toState,
                    elapsedMillis);
        }
    }

    /**
     * 관찰 대상 스레드 정보
     */
    static class ObservedThread {
        final Thread thread;
        final long startTime;
        Thread.State lastState;
        long lastStateChangeTime;
        final List<StateTransition> transitions;

        ObservedThread(Thread thread) {
            this.thread = thread;
            this.startTime = System.currentTimeMillis();
            this.lastState = thread.getState();
            this.lastStateChangeTime = startTime;
            this.transitions = new ArrayList<>();
        }

        void recordTransition(Thread.State newState) {
            long now = System.currentTimeMillis();
            long elapsed = now - lastStateChangeTime;

            StateTransition transition = new StateTransition(
                    thread.getId(),
                    thread.getName(),
                    lastState,
                    newState,
                    elapsed
            );

            transitions.add(transition);
            lastState = newState;
            lastStateChangeTime = now;
        }

        long getTotalLifetime() {
            return System.currentTimeMillis() - startTime;
        }
    }

    private final ConcurrentHashMap<Long, ObservedThread> observedThreads;
    private final Thread observerThread;
    private volatile boolean running;
    private final long pollIntervalMs;
    private final AtomicInteger transitionCount;

    public ThreadLifecycleObserver(long pollIntervalMs) {
        this.observedThreads = new ConcurrentHashMap<>();
        this.pollIntervalMs = pollIntervalMs;
        this.running = false;
        this.transitionCount = new AtomicInteger(0);
        this.observerThread = new Thread(this::observe, "LifecycleObserver");
        this.observerThread.setDaemon(true);
    }

    /**
     * 관찰 시작
     */
    public void start() {
        if (!running) {
            running = true;
            observerThread.start();
            System.out.println("ThreadLifecycleObserver started (poll interval: " +
                    pollIntervalMs + "ms)");
        }
    }

    /**
     * 관찰 중지
     */
    public void stop() {
        running = false;
        try {
            observerThread.join(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println("ThreadLifecycleObserver stopped");
    }

    /**
     * 스레드 관찰 등록
     */
    public void observe(Thread thread) {
        ObservedThread observed = new ObservedThread(thread);
        observedThreads.put(thread.getId(), observed);
        System.out.println("Started observing: " + thread.getName() +
                " (ID: " + thread.getId() + ")");
    }

    /**
     * 관찰 루프
     */
    private void observe() {
        while (running) {
            try {
                // 각 관찰 대상 스레드의 상태 확인
                observedThreads.values().forEach(observed -> {
                    Thread.State currentState = observed.thread.getState();

                    if (currentState != observed.lastState) {
                        observed.recordTransition(currentState);
                        transitionCount.incrementAndGet();

                        // 실시간 출력
                        System.out.println("  " +
                                observed.transitions.get(observed.transitions.size() - 1));
                    }

                    // TERMINATED 상태면 관찰 종료
                    if (currentState == Thread.State.TERMINATED) {
                        observedThreads.remove(observed.thread.getId());
                        System.out.println("Stopped observing: " +
                                observed.thread.getName() +
                                " (Total lifetime: " +
                                observed.getTotalLifetime() + "ms)");
                    }
                });

                Thread.sleep(pollIntervalMs);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * 통계 정보 출력
     */
    public void printStatistics() {
        System.out.println("\n=== Lifecycle Statistics ===");
        System.out.println("Total state transitions: " + transitionCount.get());
        System.out.println("Currently observing: " + observedThreads.size() + " threads");

        observedThreads.values().forEach(observed -> {
            System.out.println("\nThread: " + observed.thread.getName() +
                    " (ID: " + observed.thread.getId() + ")");
            System.out.println("  Current state: " + observed.thread.getState());
            System.out.println("  Lifetime: " + observed.getTotalLifetime() + "ms");
            System.out.println("  Transitions: " + observed.transitions.size());

            if (!observed.transitions.isEmpty()) {
                System.out.println("  Transition history:");
                observed.transitions.forEach(t ->
                        System.out.println("    " + t));
            }
        });
        System.out.println("===========================\n");
    }

    // ============= 데모 =============

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Thread Lifecycle Observer Demo ===\n");

        ThreadLifecycleObserver observer = new ThreadLifecycleObserver(50); // 50ms 폴링
        observer.start();

        // 테스트 스레드 1: 간단한 작업
        Thread simpleThread = new Thread(() -> {
            try {
                System.out.println("SimpleThread: Starting work");
                Thread.sleep(300);
                System.out.println("SimpleThread: Work completed");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "SimpleWorker");

        // 테스트 스레드 2: 여러 상태 전이
        Thread complexThread = new Thread(() -> {
            try {
                System.out.println("ComplexThread: Phase 1");
                Thread.sleep(200);

                System.out.println("ComplexThread: Phase 2 - Computing");
                long sum = 0;
                for (int i = 0; i < 1_000_000; i++) {
                    sum += i;
                }

                System.out.println("ComplexThread: Phase 3 - Sleeping");
                Thread.sleep(200);

                System.out.println("ComplexThread: Done (sum=" + sum + ")");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "ComplexWorker");

        // 테스트 스레드 3: 대기 상태
        Object lock = new Object();
        Thread waitingThread = new Thread(() -> {
            synchronized (lock) {
                try {
                    System.out.println("WaitingThread: Going to wait");
                    lock.wait(500);
                    System.out.println("WaitingThread: Resumed");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "WaitingWorker");

        // 관찰 시작
        observer.observe(simpleThread);
        observer.observe(complexThread);
        observer.observe(waitingThread);

        // 스레드 시작
        simpleThread.start();
        Thread.sleep(100);

        complexThread.start();
        Thread.sleep(100);

        waitingThread.start();

        // 모든 스레드 종료 대기
        simpleThread.join();
        complexThread.join();
        waitingThread.join();

        Thread.sleep(200); // 마지막 상태 변화를 캡처할 시간

        // 통계 출력
        observer.printStatistics();

        // 관찰자 중지
        observer.stop();

        System.out.println("\n=== Demo Completed ===");
    }
}