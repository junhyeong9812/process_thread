package com.study.thread.synchronization;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 데드락 탐지 및 해결
 *
 * 데드락을 감지하고 해결하는 다양한 방법을 제공합니다.
 * - JMX를 이용한 데드락 탐지
 * - Resource Allocation Graph (RAG) 기반 탐지
 * - 주기적 데드락 모니터링
 * - 데드락 해결 전략
 */
public class DeadlockDetector {

    /**
     * 1. JMX 기반 데드락 탐지
     */
    static class JMXDeadlockDetector {
        private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

        public boolean detectDeadlock() {
            long[] deadlockedThreads = threadMXBean.findDeadlockedThreads();

            if (deadlockedThreads != null && deadlockedThreads.length > 0) {
                System.out.println("  ⚠️  DEADLOCK DETECTED!");
                System.out.println("  Number of deadlocked threads: " + deadlockedThreads.length);

                ThreadInfo[] threadInfos = threadMXBean.getThreadInfo(deadlockedThreads);
                for (ThreadInfo info : threadInfos) {
                    System.out.println("\n  Thread: " + info.getThreadName());
                    System.out.println("  State: " + info.getThreadState());
                    System.out.println("  Lock name: " + info.getLockName());
                    System.out.println("  Lock owner: " + info.getLockOwnerName());

                    StackTraceElement[] stackTrace = info.getStackTrace();
                    if (stackTrace.length > 0) {
                        System.out.println("  Stack trace:");
                        for (int i = 0; i < Math.min(5, stackTrace.length); i++) {
                            System.out.println("    at " + stackTrace[i]);
                        }
                    }
                }

                return true;
            }

            System.out.println("  ✓ No deadlock detected");
            return false;
        }

        public long[] findMonitorDeadlockedThreads() {
            return threadMXBean.findMonitorDeadlockedThreads();
        }

        public void printThreadDump() {
            System.out.println("  Thread Dump:");
            ThreadInfo[] threadInfos = threadMXBean.dumpAllThreads(true, true);

            for (ThreadInfo info : threadInfos) {
                System.out.println("\n  Thread: " + info.getThreadName() +
                        " (ID: " + info.getThreadId() + ")");
                System.out.println("  State: " + info.getThreadState());

                if (info.getLockName() != null) {
                    System.out.println("  Waiting on: " + info.getLockName());
                }
                if (info.getLockOwnerName() != null) {
                    System.out.println("  Owned by: " + info.getLockOwnerName());
                }
            }
        }
    }

    /**
     * 2. Resource Allocation Graph (RAG) 기반 탐지
     */
    static class ResourceAllocationGraph {
        private final Map<Long, Set<String>> threadToResources = new ConcurrentHashMap<>();
        private final Map<String, Long> resourceToThread = new ConcurrentHashMap<>();
        private final Map<Long, Set<String>> threadWaitingFor = new ConcurrentHashMap<>();

        public void allocateResource(String resourceId, long threadId) {
            threadToResources.computeIfAbsent(threadId, k -> ConcurrentHashMap.newKeySet())
                    .add(resourceId);
            resourceToThread.put(resourceId, threadId);
            System.out.println("  [RAG] Thread-" + threadId +
                    " allocated resource: " + resourceId);
        }

        public void requestResource(String resourceId, long threadId) {
            threadWaitingFor.computeIfAbsent(threadId, k -> ConcurrentHashMap.newKeySet())
                    .add(resourceId);
            System.out.println("  [RAG] Thread-" + threadId +
                    " requesting resource: " + resourceId);
        }

        public void releaseResource(String resourceId, long threadId) {
            Set<String> resources = threadToResources.get(threadId);
            if (resources != null) {
                resources.remove(resourceId);
            }
            resourceToThread.remove(resourceId);

            Set<String> waiting = threadWaitingFor.get(threadId);
            if (waiting != null) {
                waiting.remove(resourceId);
            }

            System.out.println("  [RAG] Thread-" + threadId +
                    " released resource: " + resourceId);
        }

        public boolean detectCycle() {
            System.out.println("\n  [RAG] Checking for cycles...");

            // 각 스레드에서 DFS로 사이클 검사
            Set<Long> visited = new HashSet<>();
            Set<Long> recursionStack = new HashSet<>();

            for (Long threadId : threadToResources.keySet()) {
                if (hasCycle(threadId, visited, recursionStack)) {
                    System.out.println("  ⚠️  CYCLE DETECTED! Deadlock exists");
                    printCycle(recursionStack);
                    return true;
                }
            }

            System.out.println("  ✓ No cycle detected");
            return false;
        }

        private boolean hasCycle(Long threadId, Set<Long> visited, Set<Long> stack) {
            visited.add(threadId);
            stack.add(threadId);

            Set<String> waiting = threadWaitingFor.get(threadId);
            if (waiting != null) {
                for (String resourceId : waiting) {
                    Long ownerThread = resourceToThread.get(resourceId);

                    if (ownerThread != null) {
                        if (!visited.contains(ownerThread)) {
                            if (hasCycle(ownerThread, visited, stack)) {
                                return true;
                            }
                        } else if (stack.contains(ownerThread)) {
                            return true; // 사이클 발견!
                        }
                    }
                }
            }

            stack.remove(threadId);
            return false;
        }

        private void printCycle(Set<Long> cycle) {
            System.out.println("  Deadlock cycle:");
            for (Long threadId : cycle) {
                Set<String> holding = threadToResources.get(threadId);
                Set<String> waiting = threadWaitingFor.get(threadId);

                System.out.println("    Thread-" + threadId +
                        " holding: " + holding +
                        " waiting for: " + waiting);
            }
        }

        public void printGraph() {
            System.out.println("\n  Resource Allocation Graph:");
            System.out.println("  Allocations:");
            threadToResources.forEach((threadId, resources) -> {
                System.out.println("    Thread-" + threadId + " -> " + resources);
            });

            System.out.println("  Requests:");
            threadWaitingFor.forEach((threadId, resources) -> {
                System.out.println("    Thread-" + threadId + " waiting for " + resources);
            });
        }
    }

    /**
     * 3. 주기적 데드락 모니터
     */
    static class DeadlockMonitor {
        private final JMXDeadlockDetector detector = new JMXDeadlockDetector();
        private volatile boolean running = false;
        private Thread monitorThread;
        private int checkCount = 0;
        private int deadlockCount = 0;

        public void startMonitoring(long intervalMs) {
            if (running) {
                return;
            }

            running = true;
            monitorThread = new Thread(() -> {
                System.out.println("  [Monitor] Started (interval: " + intervalMs + "ms)");

                while (running) {
                    try {
                        Thread.sleep(intervalMs);
                        checkCount++;

                        System.out.println("\n  [Monitor] Check #" + checkCount);
                        if (detector.detectDeadlock()) {
                            deadlockCount++;
                            handleDeadlock();
                        }

                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }

                System.out.println("  [Monitor] Stopped");
            }, "DeadlockMonitor");

            monitorThread.setDaemon(true);
            monitorThread.start();
        }

        public void stopMonitoring() {
            running = false;
            if (monitorThread != null) {
                monitorThread.interrupt();
            }
        }

        private void handleDeadlock() {
            System.out.println("  [Monitor] Deadlock handling triggered!");
            System.out.println("  [Monitor] Total deadlocks detected: " + deadlockCount);

            // 실제 환경에서는 여기서 복구 작업 수행
            // 예: 스레드 재시작, 리소스 강제 해제 등
        }

        public void printStats() {
            System.out.println("  [Monitor] Statistics:");
            System.out.println("    Total checks: " + checkCount);
            System.out.println("    Deadlocks found: " + deadlockCount);
        }
    }

    /**
     * 4. 데드락 회피 - Wait-Die 전략
     */
    static class WaitDieStrategy {
        static class Resource {
            private final String id;
            private final Lock lock = new ReentrantLock();
            private Long ownerTimestamp = null;

            public Resource(String id) {
                this.id = id;
            }

            public boolean tryAcquire(long timestamp) {
                if (lock.tryLock()) {
                    ownerTimestamp = timestamp;
                    System.out.println("  [Thread-" + Thread.currentThread().getId() +
                            " TS:" + timestamp + "] Acquired " + id);
                    return true;
                }

                // Wait-Die: 오래된 트랜잭션(작은 timestamp)은 대기, 새 트랜잭션은 죽음
                if (ownerTimestamp != null && timestamp < ownerTimestamp) {
                    System.out.println("  [Thread-" + Thread.currentThread().getId() +
                            " TS:" + timestamp + "] Waiting for " + id);
                    lock.lock();
                    ownerTimestamp = timestamp;
                    return true;
                } else {
                    System.out.println("  [Thread-" + Thread.currentThread().getId() +
                            " TS:" + timestamp + "] Aborted (younger transaction)");
                    return false;
                }
            }

            public void release() {
                ownerTimestamp = null;
                lock.unlock();
                System.out.println("  [Thread-" + Thread.currentThread().getId() +
                        "] Released " + id);
            }
        }
    }

    /**
     * 5. 데드락 복구 - 희생자 선택
     */
    static class VictimSelection {
        static class Transaction {
            final long id;
            final long startTime;
            final int priority;
            final Set<String> heldResources;

            public Transaction(long id, int priority) {
                this.id = id;
                this.startTime = System.currentTimeMillis();
                this.priority = priority;
                this.heldResources = new HashSet<>();
            }

            public int getCost() {
                // 비용 = 실행 시간 + 보유 리소스 수
                long runtime = System.currentTimeMillis() - startTime;
                return (int) (runtime / 1000) + heldResources.size();
            }
        }

        public Transaction selectVictim(List<Transaction> deadlockedTransactions) {
            System.out.println("  [Recovery] Selecting victim from " +
                    deadlockedTransactions.size() + " transactions");

            Transaction victim = null;
            int minCost = Integer.MAX_VALUE;

            for (Transaction tx : deadlockedTransactions) {
                int cost = tx.getCost();
                System.out.println("    Transaction-" + tx.id +
                        " cost: " + cost +
                        " (priority: " + tx.priority + ")");

                if (cost < minCost || (cost == minCost && tx.priority < victim.priority)) {
                    minCost = cost;
                    victim = tx;
                }
            }

            System.out.println("  [Recovery] Selected victim: Transaction-" + victim.id);
            return victim;
        }

        public void abortTransaction(Transaction tx) {
            System.out.println("  [Recovery] Aborting Transaction-" + tx.id);
            System.out.println("  [Recovery] Releasing resources: " + tx.heldResources);
            tx.heldResources.clear();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Deadlock Detector Demo ===\n");

        // 1. JMX 기반 탐지
        demonstrateJMXDetection();

        // 2. RAG 기반 탐지
        demonstrateRAGDetection();

        // 3. 주기적 모니터링
        demonstrateMonitoring();

        // 4. Wait-Die 전략
        demonstrateWaitDie();

        // 5. 희생자 선택
        demonstrateVictimSelection();

        System.out.println("\n=== Demo Completed ===");
    }

    private static void demonstrateJMXDetection() throws InterruptedException {
        System.out.println("1. JMX-based Deadlock Detection");

        JMXDeadlockDetector detector = new JMXDeadlockDetector();

        // 데드락 생성
        Object lock1 = new Object();
        Object lock2 = new Object();

        Thread t1 = new Thread(() -> {
            synchronized (lock1) {
                sleep(50);
                synchronized (lock2) {
                    System.out.println("  Thread-1 completed");
                }
            }
        }, "DeadlockThread-1");

        Thread t2 = new Thread(() -> {
            synchronized (lock2) {
                sleep(50);
                synchronized (lock1) {
                    System.out.println("  Thread-2 completed");
                }
            }
        }, "DeadlockThread-2");

        t1.start();
        t2.start();

        Thread.sleep(200); // 데드락 발생 대기

        detector.detectDeadlock();

        t1.interrupt();
        t2.interrupt();

        System.out.println();
    }

    private static void demonstrateRAGDetection() {
        System.out.println("2. Resource Allocation Graph Detection");

        ResourceAllocationGraph rag = new ResourceAllocationGraph();

        // 시나리오: Thread-1: R1 보유, R2 요청
        //          Thread-2: R2 보유, R1 요청
        rag.allocateResource("R1", 1);
        rag.allocateResource("R2", 2);

        rag.requestResource("R2", 1);
        rag.requestResource("R1", 2);

        rag.printGraph();
        rag.detectCycle();

        System.out.println();
    }

    private static void demonstrateMonitoring() throws InterruptedException {
        System.out.println("3. Periodic Deadlock Monitoring");

        DeadlockMonitor monitor = new DeadlockMonitor();
        monitor.startMonitoring(500); // 500ms 간격

        // 정상 작업 시뮬레이션
        System.out.println("  Running normal operations...");
        Thread.sleep(1500);

        monitor.stopMonitoring();
        monitor.printStats();

        System.out.println();
    }

    private static void demonstrateWaitDie() {
        System.out.println("4. Wait-Die Deadlock Avoidance");

        WaitDieStrategy.Resource r1 = new WaitDieStrategy.Resource("Resource-1");
        WaitDieStrategy.Resource r2 = new WaitDieStrategy.Resource("Resource-2");

        // 오래된 트랜잭션 (timestamp 1)
        Thread older = new Thread(() -> {
            if (r1.tryAcquire(1)) {
                sleep(50);
                if (r2.tryAcquire(1)) {
                    sleep(100);
                    r2.release();
                }
                r1.release();
            }
        }, "OlderTransaction");

        // 새로운 트랜잭션 (timestamp 2)
        Thread younger = new Thread(() -> {
            if (r2.tryAcquire(2)) {
                sleep(50);
                if (r1.tryAcquire(2)) {
                    sleep(100);
                    r1.release();
                }
                r2.release();
            }
        }, "YoungerTransaction");

        older.start();
        younger.start();

        try {
            older.join();
            younger.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println();
    }

    private static void demonstrateVictimSelection() {
        System.out.println("5. Victim Selection for Recovery");

        VictimSelection selector = new VictimSelection();

        // 데드락에 걸린 트랜잭션들
        List<VictimSelection.Transaction> deadlocked = new ArrayList<>();

        VictimSelection.Transaction tx1 = new VictimSelection.Transaction(1, 5);
        tx1.heldResources.add("R1");
        tx1.heldResources.add("R2");

        VictimSelection.Transaction tx2 = new VictimSelection.Transaction(2, 3);
        tx2.heldResources.add("R3");

        VictimSelection.Transaction tx3 = new VictimSelection.Transaction(3, 7);
        tx3.heldResources.add("R4");
        tx3.heldResources.add("R5");
        tx3.heldResources.add("R6");

        deadlocked.add(tx1);
        deadlocked.add(tx2);
        deadlocked.add(tx3);

        sleep(100); // 실행 시간 시뮬레이션

        VictimSelection.Transaction victim = selector.selectVictim(deadlocked);
        selector.abortTransaction(victim);

        System.out.println();
    }

    private static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}