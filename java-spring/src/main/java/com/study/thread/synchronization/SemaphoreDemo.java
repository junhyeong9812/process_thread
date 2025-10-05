package com.study.thread.synchronization;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.LinkedList;

/**
 * Semaphore 구현 및 실습
 *
 * Semaphore는 제한된 수의 스레드가 동시에 리소스에 접근하도록 제어합니다.
 * - Counting Semaphore: 여러 개의 허가(permit) 관리
 * - Binary Semaphore: 0 또는 1 (Mutex와 유사)
 * - 리소스 풀 관리에 유용
 */
public class SemaphoreDemo {

    /**
     * 1. 커스텀 Counting Semaphore 구현
     */
    static class CountingSemaphore {
        private int permits;
        private final int maxPermits;

        public CountingSemaphore(int permits) {
            if (permits < 0) {
                throw new IllegalArgumentException("Permits cannot be negative");
            }
            this.permits = permits;
            this.maxPermits = permits;
        }

        public synchronized void acquire() throws InterruptedException {
            while (permits == 0) {
                wait();
            }
            permits--;
        }

        public synchronized void release() {
            if (permits < maxPermits) {
                permits++;
                notify();
            }
        }

        public synchronized boolean tryAcquire() {
            if (permits > 0) {
                permits--;
                return true;
            }
            return false;
        }

        public synchronized int availablePermits() {
            return permits;
        }

        public synchronized void drainPermits() {
            permits = 0;
        }
    }

    /**
     * 2. Binary Semaphore 구현 (Mutex와 유사)
     */
    static class BinarySemaphore {
        private boolean available = true;

        public synchronized void acquire() throws InterruptedException {
            while (!available) {
                wait();
            }
            available = false;
        }

        public synchronized void release() {
            available = true;
            notify();
        }

        public synchronized boolean tryAcquire() {
            if (available) {
                available = false;
                return true;
            }
            return false;
        }
    }

    /**
     * 3. 제한된 리소스 풀 시뮬레이션
     */
    static class ResourcePool {
        private final Queue<String> resources = new LinkedList<>();
        private final Semaphore semaphore;

        public ResourcePool(int size) {
            this.semaphore = new Semaphore(size);
            for (int i = 1; i <= size; i++) {
                resources.offer("Resource-" + i);
            }
        }

        public String acquire() throws InterruptedException {
            semaphore.acquire();
            synchronized (resources) {
                return resources.poll();
            }
        }

        public void release(String resource) {
            synchronized (resources) {
                resources.offer(resource);
            }
            semaphore.release();
        }

        public int availableResources() {
            return semaphore.availablePermits();
        }
    }

    /**
     * 4. 데이터베이스 커넥션 풀 시뮬레이션
     */
    static class DatabaseConnectionPool {
        private final List<Connection> connections = new ArrayList<>();
        private final Semaphore semaphore;

        static class Connection {
            private final int id;
            private boolean inUse = false;

            Connection(int id) {
                this.id = id;
            }

            public void execute(String query) throws InterruptedException {
                System.out.println("    [Connection-" + id + "] Executing: " + query);
                Thread.sleep(100); // 쿼리 실행 시뮬레이션
            }

            public int getId() {
                return id;
            }

            public boolean isInUse() {
                return inUse;
            }

            public void setInUse(boolean inUse) {
                this.inUse = inUse;
            }
        }

        public DatabaseConnectionPool(int poolSize) {
            this.semaphore = new Semaphore(poolSize);
            for (int i = 1; i <= poolSize; i++) {
                connections.add(new Connection(i));
            }
        }

        public Connection getConnection() throws InterruptedException {
            semaphore.acquire();
            synchronized (connections) {
                for (Connection conn : connections) {
                    if (!conn.isInUse()) {
                        conn.setInUse(true);
                        return conn;
                    }
                }
            }
            throw new IllegalStateException("No connection available");
        }

        public void releaseConnection(Connection connection) {
            synchronized (connections) {
                connection.setInUse(false);
            }
            semaphore.release();
        }

        public int availableConnections() {
            return semaphore.availablePermits();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Semaphore Demo ===\n");

        // 1. Counting Semaphore 기본 사용
        demonstrateCountingSemaphore();

        // 2. Binary Semaphore
        demonstrateBinarySemaphore();

        // 3. 리소스 풀 관리
        demonstrateResourcePool();

        // 4. 데이터베이스 커넥션 풀
        demonstrateDatabaseConnectionPool();

        // 5. Java Semaphore API
        demonstrateJavaSemaphore();

        // 6. 공정성 (Fairness)
        demonstrateFairness();

        // 7. Rate Limiting
        demonstrateRateLimiting();

        System.out.println("\n=== Demo Completed ===");
    }

    private static void demonstrateCountingSemaphore() throws InterruptedException {
        System.out.println("1. Counting Semaphore Demo");

        CountingSemaphore semaphore = new CountingSemaphore(3);
        int threadCount = 6;

        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i + 1;
            threads[i] = new Thread(() -> {
                try {
                    System.out.println("  Thread-" + threadId + " trying to acquire...");
                    semaphore.acquire();
                    System.out.println("  Thread-" + threadId + " acquired permit " +
                            "(available: " + semaphore.availablePermits() + ")");

                    Thread.sleep(200); // 작업 시뮬레이션

                    System.out.println("  Thread-" + threadId + " releasing permit");
                    semaphore.release();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        System.out.println("  Final available permits: " + semaphore.availablePermits());
        System.out.println();
    }

    private static void demonstrateBinarySemaphore() throws InterruptedException {
        System.out.println("2. Binary Semaphore Demo");

        BinarySemaphore semaphore = new BinarySemaphore();
        AtomicInteger counter = new AtomicInteger(0);
        int threadCount = 5;

        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i + 1;
            threads[i] = new Thread(() -> {
                try {
                    semaphore.acquire();
                    System.out.println("  Thread-" + threadId + " in critical section");
                    counter.incrementAndGet();
                    Thread.sleep(100);
                    System.out.println("  Thread-" + threadId + " leaving critical section");
                    semaphore.release();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        System.out.println("  Counter value: " + counter.get());
        System.out.println();
    }

    private static void demonstrateResourcePool() throws InterruptedException {
        System.out.println("3. Resource Pool Management Demo");

        ResourcePool pool = new ResourcePool(3);
        int workerCount = 6;

        Thread[] workers = new Thread[workerCount];

        for (int i = 0; i < workerCount; i++) {
            final int workerId = i + 1;
            workers[i] = new Thread(() -> {
                try {
                    System.out.println("  Worker-" + workerId + " requesting resource " +
                            "(available: " + pool.availableResources() + ")");

                    String resource = pool.acquire();
                    System.out.println("  Worker-" + workerId + " got " + resource);

                    Thread.sleep(200); // 리소스 사용

                    System.out.println("  Worker-" + workerId + " releasing " + resource);
                    pool.release(resource);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            workers[i].start();
        }

        for (Thread worker : workers) {
            worker.join();
        }

        System.out.println("  Final available resources: " + pool.availableResources());
        System.out.println();
    }

    private static void demonstrateDatabaseConnectionPool() throws InterruptedException {
        System.out.println("4. Database Connection Pool Demo");

        DatabaseConnectionPool pool = new DatabaseConnectionPool(3);
        int queryCount = 8;

        Thread[] threads = new Thread[queryCount];

        for (int i = 0; i < queryCount; i++) {
            final int queryId = i + 1;
            threads[i] = new Thread(() -> {
                try {
                    System.out.println("  Query-" + queryId + " waiting for connection " +
                            "(available: " + pool.availableConnections() + ")");

                    DatabaseConnectionPool.Connection conn = pool.getConnection();
                    System.out.println("  Query-" + queryId + " got Connection-" + conn.getId());

                    conn.execute("SELECT * FROM users WHERE id=" + queryId);

                    pool.releaseConnection(conn);
                    System.out.println("  Query-" + queryId + " released Connection-" + conn.getId());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            threads[i].start();
            Thread.sleep(50); // 쿼리가 순차적으로 들어오도록
        }

        for (Thread thread : threads) {
            thread.join();
        }

        System.out.println();
    }

    private static void demonstrateJavaSemaphore() throws InterruptedException {
        System.out.println("5. Java Semaphore API Demo");

        Semaphore semaphore = new Semaphore(2);
        int threadCount = 5;

        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i + 1;
            threads[i] = new Thread(() -> {
                try {
                    System.out.println("  Thread-" + threadId + " trying to acquire");

                    semaphore.acquire();
                    System.out.println("  Thread-" + threadId + " acquired " +
                            "(available: " + semaphore.availablePermits() + ")");

                    Thread.sleep(200);

                    semaphore.release();
                    System.out.println("  Thread-" + threadId + " released");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        System.out.println();
    }

    private static void demonstrateFairness() throws InterruptedException {
        System.out.println("6. Fairness Demo");

        System.out.println("  Non-fair Semaphore:");
        Semaphore nonFair = new Semaphore(1, false);
        testSemaphoreFairness(nonFair);

        System.out.println("\n  Fair Semaphore:");
        Semaphore fair = new Semaphore(1, true);
        testSemaphoreFairness(fair);

        System.out.println();
    }

    private static void testSemaphoreFairness(Semaphore semaphore)
            throws InterruptedException {
        int threadCount = 5;
        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i + 1;
            threads[i] = new Thread(() -> {
                try {
                    for (int j = 0; j < 2; j++) {
                        semaphore.acquire();
                        System.out.println("    Thread-" + threadId + " iteration " + (j + 1));
                        Thread.sleep(50);
                        semaphore.release();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }
    }

    private static void demonstrateRateLimiting() throws InterruptedException {
        System.out.println("7. Rate Limiting Demo");

        // 초당 3개의 요청만 허용
        Semaphore rateLimiter = new Semaphore(3);
        AtomicInteger requestCount = new AtomicInteger(0);

        // 백그라운드에서 매초 permit 보충
        Thread replenisher = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Thread.sleep(1000);
                    int available = rateLimiter.availablePermits();
                    int toAdd = Math.min(3 - available, 3);
                    if (toAdd > 0) {
                        rateLimiter.release(toAdd);
                        System.out.println("  [Replenisher] Added " + toAdd + " permits");
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        replenisher.setDaemon(true);
        replenisher.start();

        // 10개의 요청 시도
        for (int i = 1; i <= 10; i++) {
            final int requestId = i;
            new Thread(() -> {
                try {
                    System.out.println("  Request-" + requestId + " waiting...");
                    rateLimiter.acquire();
                    requestCount.incrementAndGet();
                    System.out.println("  Request-" + requestId + " processed " +
                            "(total: " + requestCount.get() + ")");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();

            Thread.sleep(200); // 요청 간격
        }

        Thread.sleep(3000); // 모든 요청이 처리될 때까지 대기
        System.out.println("  Total requests processed: " + requestCount.get());
        System.out.println();
    }
}