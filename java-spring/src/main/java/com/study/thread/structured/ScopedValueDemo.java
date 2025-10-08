package com.study.thread.structured;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Scoped Values 실습 (Java 21+)
 *
 * Scoped Values는 ThreadLocal의 현대적 대안입니다:
 * - 불변(Immutable)
 * - 명확한 스코프(Scope)
 * - Virtual Thread에 최적화
 * - 자동 메모리 관리
 *
 * ThreadLocal의 문제점:
 * - 메모리 누수 위험
 * - 명시적 cleanup 필요
 * - Virtual Thread에서 비효율적
 * - 가변(Mutable) 상태
 *
 * 주의: Java 21 이상 필요 (Preview Feature)
 */
public class ScopedValueDemo {

    /**
     * 1. ThreadLocal 문제점 시연
     */
    static class ThreadLocalProblems {
        private static final ThreadLocal<String> userContext = new ThreadLocal<>();

        public void demonstrate() throws InterruptedException {
            System.out.println("  ThreadLocal problems demonstration");

            // 스레드 1
            Thread thread1 = Thread.ofPlatform().start(() -> {
                userContext.set("User-1");
                System.out.println("    [Thread-1] Set: " + userContext.get());

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                System.out.println("    [Thread-1] Still: " + userContext.get());
                // 문제: 명시적으로 cleanup 하지 않으면 메모리 누수
                userContext.remove(); // 이걸 잊으면 문제 발생
            });

            // 스레드 2
            Thread thread2 = Thread.ofPlatform().start(() -> {
                userContext.set("User-2");
                System.out.println("    [Thread-2] Set: " + userContext.get());

                // 가변성 문제
                userContext.set("User-2-Modified");
                System.out.println("    [Thread-2] Modified: " + userContext.get());

                userContext.remove();
            });

            thread1.join();
            thread2.join();

            System.out.println("\n    ThreadLocal Issues:");
            System.out.println("      - Must remember to call remove()");
            System.out.println("      - Can be mutated accidentally");
            System.out.println("      - Not suitable for virtual threads");
            System.out.println("      - Memory leak risk");
        }
    }

    /**
     * 2. Scoped Value 개념 (Java 21+ 대체 구현)
     */
    static class ScopedValueConcept {
        // Java 21의 ScopedValue를 시뮬레이션
        static class ScopedValue<T> {
            private final ThreadLocal<T> storage = new ThreadLocal<>();

            public void runWhere(T value, Runnable operation) {
                T oldValue = storage.get();
                storage.set(value);
                try {
                    operation.run();
                } finally {
                    if (oldValue == null) {
                        storage.remove();
                    } else {
                        storage.set(oldValue);
                    }
                }
            }

            public T get() {
                return storage.get();
            }
        }

        private static final ScopedValue<String> USER = new ScopedValue<>();

        public void demonstrate() throws InterruptedException, ExecutionException {
            System.out.println("  Scoped Value concept demonstration");

            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {

                Future<?> future1 = executor.submit(() -> {
                    USER.runWhere("Alice", () -> {
                        System.out.println("    [Task-1] User: " + USER.get());

                        USER.runWhere("Alice-Inner", () -> {
                            System.out.println("    [Task-1] Inner scope: " + USER.get());
                        });

                        System.out.println("    [Task-1] Back to: " + USER.get());
                    });

                    System.out.println("    [Task-1] Outside scope: " + USER.get());
                });

                Future<?> future2 = executor.submit(() -> {
                    USER.runWhere("Bob", () -> {
                        System.out.println("    [Task-2] User: " + USER.get());

                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }

                        System.out.println("    [Task-2] Still: " + USER.get());
                    });
                });

                future1.get();
                future2.get();
            }

            System.out.println("\n    Scoped Value Benefits:");
            System.out.println("      - Automatic cleanup");
            System.out.println("      - Immutable within scope");
            System.out.println("      - Clear boundaries");
            System.out.println("      - Virtual thread friendly");
        }
    }

    /**
     * 3. 요청 컨텍스트 패턴
     */
    static class RequestContextPattern {
        static class RequestContext {
            final String requestId;
            final String userId;
            final long timestamp;

            RequestContext(String requestId, String userId) {
                this.requestId = requestId;
                this.userId = userId;
                this.timestamp = System.currentTimeMillis();
            }

            @Override
            public String toString() {
                return String.format("Request[id=%s, user=%s, time=%d]",
                        requestId, userId, timestamp);
            }
        }

        private static final ThreadLocal<RequestContext> CONTEXT = new ThreadLocal<>();

        public void demonstrate() throws Exception {
            System.out.println("  Request context pattern");

            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {

                // 여러 요청 시뮬레이션
                List<Future<?>> futures = new ArrayList<>();

                for (int i = 1; i <= 3; i++) {
                    final int requestNum = i;

                    futures.add(executor.submit(() -> {
                        // 각 요청마다 컨텍스트 설정
                        RequestContext context = new RequestContext(
                                "REQ-" + requestNum,
                                "User-" + requestNum
                        );

                        CONTEXT.set(context);

                        try {
                            processRequest();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();  // 인터럽트 상태 복원
                        } finally {
                            CONTEXT.remove();
                        }
                    }));
                }

                for (Future<?> future : futures) {
                    future.get();
                }
            }
        }

        private void processRequest() throws InterruptedException {
            RequestContext context = CONTEXT.get();
            System.out.println("    [Processing] " + context);

            Thread.sleep(100);

            // 다른 메서드 호출 시에도 컨텍스트 접근 가능
            logRequest();
        }

        private void logRequest() {
            RequestContext context = CONTEXT.get();
            System.out.println("    [Logging] " + context);
        }
    }

    /**
     * 4. 트랜잭션 컨텍스트
     */
    static class TransactionContext {
        static class Transaction {
            final String txId;
            final long startTime;
            boolean active = true;

            Transaction(String txId) {
                this.txId = txId;
                this.startTime = System.currentTimeMillis();
            }

            void commit() {
                active = false;
                System.out.println("    [TX-" + txId + "] Committed");
            }

            void rollback() {
                active = false;
                System.out.println("    [TX-" + txId + "] Rolled back");
            }
        }

        private static final ThreadLocal<Transaction> CURRENT_TX = new ThreadLocal<>();

        public void demonstrate() throws Exception {
            System.out.println("  Transaction context pattern");

            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {

                Future<?> successTx = executor.submit(() -> {
                    Transaction tx = new Transaction("TX-1");
                    CURRENT_TX.set(tx);

                    try {
                        System.out.println("    [TX-1] Starting transaction");
                        performDatabaseOperation("INSERT data");
                        performDatabaseOperation("UPDATE data");

                        getCurrentTransaction().commit();
                    } catch (Exception e) {
                        getCurrentTransaction().rollback();
                    } finally {
                        CURRENT_TX.remove();
                    }
                });

                Future<?> failTx = executor.submit(() -> {
                    Transaction tx = new Transaction("TX-2");
                    CURRENT_TX.set(tx);

                    try {
                        System.out.println("    [TX-2] Starting transaction");
                        performDatabaseOperation("INSERT data");

                        // 에러 발생
                        throw new RuntimeException("Database error");

                    } catch (Exception e) {
                        System.out.println("    [TX-2] Error: " + e.getMessage());
                        getCurrentTransaction().rollback();
                    } finally {
                        CURRENT_TX.remove();
                    }
                });

                successTx.get();
                failTx.get();
            }
        }

        private void performDatabaseOperation(String operation) throws InterruptedException {
            Transaction tx = getCurrentTransaction();
            System.out.println("    [" + tx.txId + "] " + operation);
            Thread.sleep(50);
        }

        private Transaction getCurrentTransaction() {
            Transaction tx = CURRENT_TX.get();
            if (tx == null) {
                throw new IllegalStateException("No transaction context");
            }
            return tx;
        }
    }

    /**
     * 5. 보안 컨텍스트
     */
    static class SecurityContext {
        static class Principal {
            final String username;
            final String[] roles;

            Principal(String username, String... roles) {
                this.username = username;
                this.roles = roles;
            }

            boolean hasRole(String role) {
                for (String r : roles) {
                    if (r.equals(role)) return true;
                }
                return false;
            }
        }

        private static final ThreadLocal<Principal> CURRENT_USER = new ThreadLocal<>();

        public void demonstrate() throws Exception {
            System.out.println("  Security context pattern");

            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {

                // Admin 사용자
                executor.submit(() -> {
                    CURRENT_USER.set(new Principal("admin", "ADMIN", "USER"));

                    try {
                        accessResource("admin-panel");
                        accessResource("user-data");
                    } finally {
                        CURRENT_USER.remove();
                    }
                }).get();

                // 일반 사용자
                executor.submit(() -> {
                    CURRENT_USER.set(new Principal("john", "USER"));

                    try {
                        accessResource("user-data");
                        accessResource("admin-panel"); // 실패할 것
                    } finally {
                        CURRENT_USER.remove();
                    }
                }).get();
            }
        }

        private void accessResource(String resource) {
            Principal user = CURRENT_USER.get();

            boolean allowed = switch (resource) {
                case "admin-panel" -> user.hasRole("ADMIN");
                case "user-data" -> user.hasRole("USER");
                default -> false;
            };

            if (allowed) {
                System.out.println("    [" + user.username + "] ✓ Accessed: " + resource);
            } else {
                System.out.println("    [" + user.username + "] ✗ Denied: " + resource);
            }
        }
    }

    /**
     * 6. Virtual Thread와의 효율성 비교
     */
    static class VirtualThreadEfficiency {
        private static final ThreadLocal<byte[]> HEAVY_DATA =
                ThreadLocal.withInitial(() -> new byte[1024 * 1024]); // 1MB

        public void demonstrate() throws Exception {
            System.out.println("  Virtual thread efficiency comparison");

            Runtime runtime = Runtime.getRuntime();

            // ThreadLocal with virtual threads (비효율적)
            System.out.println("\n    ThreadLocal with 1000 virtual threads:");
            System.gc();
            long beforeTL = runtime.totalMemory() - runtime.freeMemory();

            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                List<Future<?>> futures = new ArrayList<>();

                for (int i = 0; i < 1000; i++) {
                    futures.add(executor.submit(() -> {
                        byte[] data = HEAVY_DATA.get(); // 각 스레드마다 1MB 할당
                        Thread.sleep(10);
                        return null;
                    }));
                }

                for (Future<?> future : futures) {
                    future.get();
                }
            }

            long afterTL = runtime.totalMemory() - runtime.freeMemory();
            System.out.println("      Memory used: ~" +
                    ((afterTL - beforeTL) / 1024 / 1024) + " MB");

            System.out.println("\n    Note: With ScopedValue, memory would be reclaimed automatically");
            System.out.println("          and virtual threads would be much more efficient");
        }
    }

    /**
     * 7. 모범 사례
     */
    static class BestPractices {
        public void demonstrate() {
            System.out.println("\n  === Scoped Values Best Practices ===");

            System.out.println("\n  ✓ When to use Scoped Values:");
            System.out.println("    - Request context propagation");
            System.out.println("    - Transaction management");
            System.out.println("    - Security context");
            System.out.println("    - Logging context");
            System.out.println("    - With virtual threads");

            System.out.println("\n  ✗ When NOT to use:");
            System.out.println("    - Mutable state needed");
            System.out.println("    - Long-lived values");
            System.out.println("    - Cross-thread communication");
            System.out.println("    - Performance-critical paths (overhead)");

            System.out.println("\n  ScopedValue vs ThreadLocal:");
            System.out.println("    ScopedValue:");
            System.out.println("      + Immutable");
            System.out.println("      + Automatic cleanup");
            System.out.println("      + Virtual thread optimized");
            System.out.println("      + Clear scope boundaries");
            System.out.println("      - Requires Java 21+");
            System.out.println("      - Preview feature");

            System.out.println("\n    ThreadLocal:");
            System.out.println("      + Available in all Java versions");
            System.out.println("      + Mutable");
            System.out.println("      + Widely understood");
            System.out.println("      - Manual cleanup required");
            System.out.println("      - Memory leak risk");
            System.out.println("      - Inefficient with virtual threads");

            System.out.println("\n  Migration Strategy:");
            System.out.println("    1. Identify ThreadLocal usage");
            System.out.println("    2. Check if values are immutable");
            System.out.println("    3. Replace with ScopedValue");
            System.out.println("    4. Use runWhere() pattern");
            System.out.println("    5. Remove manual cleanup code");
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== Scoped Values Demo (Java 21+) ===\n");

        // 1. ThreadLocal 문제점
        System.out.println("1. ThreadLocal Problems");
        new ThreadLocalProblems().demonstrate();
        System.out.println();

        // 2. Scoped Value 개념
        System.out.println("2. Scoped Value Concept");
        new ScopedValueConcept().demonstrate();
        System.out.println();

        // 3. 요청 컨텍스트
        System.out.println("3. Request Context Pattern");
        new RequestContextPattern().demonstrate();
        System.out.println();

        // 4. 트랜잭션 컨텍스트
        System.out.println("4. Transaction Context");
        new TransactionContext().demonstrate();
        System.out.println();

        // 5. 보안 컨텍스트
        System.out.println("5. Security Context");
        new SecurityContext().demonstrate();
        System.out.println();

        // 6. Virtual Thread 효율성
        System.out.println("6. Virtual Thread Efficiency");
        new VirtualThreadEfficiency().demonstrate();
        System.out.println();

        // 7. 모범 사례
        System.out.println("7. Best Practices");
        new BestPractices().demonstrate();
        System.out.println();

        System.out.println("=== Demo Completed ===");
    }
}