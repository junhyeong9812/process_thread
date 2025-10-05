package com.study.thread.synchronization;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Monitor 패턴 구현
 *
 * Monitor는 mutual exclusion과 condition variable을 결합한 동기화 메커니즘입니다.
 * - 한 번에 하나의 스레드만 모니터 내부에 접근 가능
 * - wait/notify를 통한 조건 기반 대기/통지
 * - Java의 synchronized는 암묵적 모니터
 */
public class MonitorDemo {

    /**
     * 1. 기본 Monitor 패턴 (synchronized 기반)
     */
    static class BasicMonitor {
        private int value = 0;
        private final Object lock = new Object();

        public void increment() {
            synchronized (lock) {
                value++;
                System.out.println("  [" + Thread.currentThread().getName() + "] " +
                        "Incremented to: " + value);
                lock.notifyAll(); // 대기 중인 모든 스레드 깨우기
            }
        }

        public void waitForValue(int target) throws InterruptedException {
            synchronized (lock) {
                while (value < target) {
                    System.out.println("  [" + Thread.currentThread().getName() + "] " +
                            "Waiting for value >= " + target +
                            " (current: " + value + ")");
                    lock.wait();
                }
                System.out.println("  [" + Thread.currentThread().getName() + "] " +
                        "Condition met! value = " + value);
            }
        }

        public int getValue() {
            synchronized (lock) {
                return value;
            }
        }
    }

    /**
     * 2. Bounded Buffer Monitor (생산자-소비자 패턴)
     */
    static class BoundedBuffer<T> {
        private final Queue<T> buffer = new LinkedList<>();
        private final int capacity;

        public BoundedBuffer(int capacity) {
            this.capacity = capacity;
        }

        public synchronized void put(T item) throws InterruptedException {
            while (buffer.size() >= capacity) {
                System.out.println("  [Producer] Buffer full, waiting...");
                wait(); // 버퍼가 비워질 때까지 대기
            }

            buffer.offer(item);
            System.out.println("  [Producer] Produced: " + item +
                    " (size: " + buffer.size() + ")");
            notifyAll(); // 소비자들을 깨움
        }

        public synchronized T take() throws InterruptedException {
            while (buffer.isEmpty()) {
                System.out.println("  [Consumer] Buffer empty, waiting...");
                wait(); // 버퍼에 데이터가 들어올 때까지 대기
            }

            T item = buffer.poll();
            System.out.println("  [Consumer] Consumed: " + item +
                    " (size: " + buffer.size() + ")");
            notifyAll(); // 생산자들을 깨움
            return item;
        }

        public synchronized int size() {
            return buffer.size();
        }
    }

    /**
     * 3. Condition Variable을 사용한 Monitor (Lock 기반)
     */
    static class ConditionMonitor {
        private final Lock lock = new ReentrantLock();
        private final Condition notFull = lock.newCondition();
        private final Condition notEmpty = lock.newCondition();
        private final Queue<Integer> queue = new LinkedList<>();
        private final int capacity;

        public ConditionMonitor(int capacity) {
            this.capacity = capacity;
        }

        public void put(int value) throws InterruptedException {
            lock.lock();
            try {
                while (queue.size() >= capacity) {
                    System.out.println("  [Producer] Queue full, waiting on notFull");
                    notFull.await(); // notFull 조건 대기
                }

                queue.offer(value);
                System.out.println("  [Producer] Put: " + value +
                        " (size: " + queue.size() + ")");
                notEmpty.signal(); // notEmpty 조건을 기다리는 스레드 깨우기
            } finally {
                lock.unlock();
            }
        }

        public int take() throws InterruptedException {
            lock.lock();
            try {
                while (queue.isEmpty()) {
                    System.out.println("  [Consumer] Queue empty, waiting on notEmpty");
                    notEmpty.await(); // notEmpty 조건 대기
                }

                int value = queue.poll();
                System.out.println("  [Consumer] Took: " + value +
                        " (size: " + queue.size() + ")");
                notFull.signal(); // notFull 조건을 기다리는 스레드 깨우기
                return value;
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * 4. Read-Write Monitor (읽기/쓰기 분리)
     */
    static class ReadWriteMonitor {
        private int data = 0;
        private int readers = 0;
        private boolean writing = false;

        public synchronized void startRead() throws InterruptedException {
            while (writing) {
                wait(); // 쓰기가 끝날 때까지 대기
            }
            readers++;
            System.out.println("  [Reader] Started reading (active readers: " +
                    readers + ")");
        }

        public synchronized void endRead() {
            readers--;
            System.out.println("  [Reader] Finished reading (active readers: " +
                    readers + ")");
            if (readers == 0) {
                notifyAll(); // 쓰기를 기다리는 스레드 깨우기
            }
        }

        public synchronized void startWrite() throws InterruptedException {
            while (writing || readers > 0) {
                wait(); // 읽기/쓰기가 모두 끝날 때까지 대기
            }
            writing = true;
            System.out.println("  [Writer] Started writing");
        }

        public synchronized void endWrite() {
            writing = false;
            System.out.println("  [Writer] Finished writing");
            notifyAll(); // 대기 중인 모든 스레드 깨우기
        }

        public int read() throws InterruptedException {
            startRead();
            try {
                Thread.sleep(100); // 읽기 시뮬레이션
                return data;
            } finally {
                endRead();
            }
        }

        public void write(int value) throws InterruptedException {
            startWrite();
            try {
                Thread.sleep(200); // 쓰기 시뮬레이션
                data = value;
                System.out.println("  [Writer] Wrote value: " + value);
            } finally {
                endWrite();
            }
        }
    }

    /**
     * 5. 상태 머신 Monitor
     */
    static class StateMachineMonitor {
        enum State {
            READY, RUNNING, PAUSED, STOPPED
        }

        private State currentState = State.READY;

        public synchronized void start() throws InterruptedException {
            while (currentState != State.READY) {
                wait();
            }
            currentState = State.RUNNING;
            System.out.println("  [StateMachine] State: READY -> RUNNING");
            notifyAll();
        }

        public synchronized void pause() throws InterruptedException {
            while (currentState != State.RUNNING) {
                wait();
            }
            currentState = State.PAUSED;
            System.out.println("  [StateMachine] State: RUNNING -> PAUSED");
            notifyAll();
        }

        public synchronized void resume() throws InterruptedException {
            while (currentState != State.PAUSED) {
                wait();
            }
            currentState = State.RUNNING;
            System.out.println("  [StateMachine] State: PAUSED -> RUNNING");
            notifyAll();
        }

        public synchronized void stop() throws InterruptedException {
            while (currentState == State.STOPPED) {
                wait();
            }
            currentState = State.STOPPED;
            System.out.println("  [StateMachine] State: " + currentState + " -> STOPPED");
            notifyAll();
        }

        public synchronized State getState() {
            return currentState;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Monitor Pattern Demo ===\n");

        // 1. 기본 Monitor
        demonstrateBasicMonitor();

        // 2. Bounded Buffer
        demonstrateBoundedBuffer();

        // 3. Condition Monitor
        demonstrateConditionMonitor();

        // 4. Read-Write Monitor
        demonstrateReadWriteMonitor();

        // 5. State Machine Monitor
        demonstrateStateMachineMonitor();

        System.out.println("\n=== Demo Completed ===");
    }

    private static void demonstrateBasicMonitor() throws InterruptedException {
        System.out.println("1. Basic Monitor Demo");

        BasicMonitor monitor = new BasicMonitor();

        // Waiter 스레드
        Thread waiter = new Thread(() -> {
            try {
                monitor.waitForValue(5);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Waiter");

        waiter.start();
        Thread.sleep(100); // Waiter가 먼저 대기하도록

        // Incrementer 스레드들
        for (int i = 1; i <= 5; i++) {
            final int num = i;
            new Thread(() -> {
                monitor.increment();
            }, "Incrementer-" + num).start();
            Thread.sleep(100);
        }

        waiter.join();
        System.out.println();
    }

    private static void demonstrateBoundedBuffer() throws InterruptedException {
        System.out.println("2. Bounded Buffer Monitor Demo");

        BoundedBuffer<String> buffer = new BoundedBuffer<>(3);

        // Producer
        Thread producer = new Thread(() -> {
            try {
                for (int i = 1; i <= 5; i++) {
                    buffer.put("Item-" + i);
                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Producer");

        // Consumer
        Thread consumer = new Thread(() -> {
            try {
                Thread.sleep(500); // Producer가 먼저 채우도록
                for (int i = 1; i <= 5; i++) {
                    buffer.take();
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

    private static void demonstrateConditionMonitor() throws InterruptedException {
        System.out.println("3. Condition Monitor Demo");

        ConditionMonitor monitor = new ConditionMonitor(2);

        // Producer
        Thread producer = new Thread(() -> {
            try {
                for (int i = 1; i <= 4; i++) {
                    monitor.put(i);
                    Thread.sleep(150);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Producer");

        // Consumer
        Thread consumer = new Thread(() -> {
            try {
                Thread.sleep(500); // Producer가 먼저 채우도록
                for (int i = 1; i <= 4; i++) {
                    monitor.take();
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

    private static void demonstrateReadWriteMonitor() throws InterruptedException {
        System.out.println("4. Read-Write Monitor Demo");

        ReadWriteMonitor monitor = new ReadWriteMonitor();

        // Writers
        Thread writer1 = new Thread(() -> {
            try {
                monitor.write(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Writer-1");

        // Readers
        Thread reader1 = new Thread(() -> {
            try {
                Thread.sleep(100);
                int value = monitor.read();
                System.out.println("  [Reader-1] Read value: " + value);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Reader-1");

        Thread reader2 = new Thread(() -> {
            try {
                Thread.sleep(100);
                int value = monitor.read();
                System.out.println("  [Reader-2] Read value: " + value);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Reader-2");

        Thread writer2 = new Thread(() -> {
            try {
                Thread.sleep(400);
                monitor.write(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Writer-2");

        writer1.start();
        reader1.start();
        reader2.start();
        writer2.start();

        writer1.join();
        reader1.join();
        reader2.join();
        writer2.join();
        System.out.println();
    }

    private static void demonstrateStateMachineMonitor() throws InterruptedException {
        System.out.println("5. State Machine Monitor Demo");

        StateMachineMonitor monitor = new StateMachineMonitor();

        // 상태 전이 시퀀스
        Thread controller = new Thread(() -> {
            try {
                monitor.start();
                Thread.sleep(200);

                monitor.pause();
                Thread.sleep(200);

                monitor.resume();
                Thread.sleep(200);

                monitor.stop();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Controller");

        controller.start();
        controller.join();

        System.out.println("  Final state: " + monitor.getState());
        System.out.println();
    }
}