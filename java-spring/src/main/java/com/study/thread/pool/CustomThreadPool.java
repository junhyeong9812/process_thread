package com.study.thread.pool;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 커스텀 스레드 풀 구현
 *
 * 스레드 풀의 내부 동작 원리를 이해하기 위해 직접 구현합니다.
 * - 고정 크기 워커 스레드
 * - 작업 큐 (BlockingQueue)
 * - 우아한 종료 (Graceful Shutdown)
 * - 통계 수집
 */
public class CustomThreadPool {

    /**
     * 1. 기본 스레드 풀 구현
     */
    static class BasicThreadPool {
        private final BlockingQueue<Runnable> taskQueue;
        private final List<WorkerThread> workers;
        private volatile boolean isShutdown = false;
        private final AtomicInteger completedTasks = new AtomicInteger(0);
        private final int poolSize;

        public BasicThreadPool(int poolSize, int queueCapacity) {
            this.poolSize = poolSize;
            this.taskQueue = new LinkedBlockingQueue<>(queueCapacity);
            this.workers = new ArrayList<>();

            // 워커 스레드 생성 및 시작
            for (int i = 0; i < poolSize; i++) {
                WorkerThread worker = new WorkerThread(i);
                workers.add(worker);
                worker.start();
            }

            System.out.println("  [Pool] Created with " + poolSize + " workers");
        }

        public void submit(Runnable task) {
            if (isShutdown) {
                throw new RejectedExecutionException("Pool is shut down");
            }

            try {
                taskQueue.put(task);
                System.out.println("  [Pool] Task submitted (queue size: " +
                        taskQueue.size() + ")");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RejectedExecutionException("Task submission interrupted", e);
            }
        }

        public void shutdown() {
            System.out.println("  [Pool] Shutting down...");
            isShutdown = true;

            // 모든 워커 인터럽트
            for (WorkerThread worker : workers) {
                worker.interrupt();
            }
        }

        public void awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            long deadline = System.nanoTime() + unit.toNanos(timeout);

            for (WorkerThread worker : workers) {
                long remaining = deadline - System.nanoTime();
                if (remaining <= 0) {
                    return;
                }
                worker.join(TimeUnit.NANOSECONDS.toMillis(remaining));
            }

            System.out.println("  [Pool] All workers terminated");
        }

        public int getCompletedTaskCount() {
            return completedTasks.get();
        }

        public int getQueueSize() {
            return taskQueue.size();
        }

        public int getActiveCount() {
            return (int) workers.stream().filter(w -> w.isProcessing).count();
        }

        private class WorkerThread extends Thread {
            private final int id;
            private volatile boolean isProcessing = false;

            public WorkerThread(int id) {
                super("Worker-" + id);
                this.id = id;
            }

            @Override
            public void run() {
                System.out.println("  [" + getName() + "] Started");

                while (!isShutdown || !taskQueue.isEmpty()) {
                    try {
                        Runnable task = taskQueue.poll(100, TimeUnit.MILLISECONDS);

                        if (task != null) {
                            isProcessing = true;
                            System.out.println("  [" + getName() + "] Processing task");

                            try {
                                task.run();
                                completedTasks.incrementAndGet();
                                System.out.println("  [" + getName() + "] Task completed");
                            } catch (Exception e) {
                                System.out.println("  [" + getName() + "] Task failed: " +
                                        e.getMessage());
                            } finally {
                                isProcessing = false;
                            }
                        }
                    } catch (InterruptedException e) {
                        if (isShutdown) {
                            break;
                        }
                        Thread.currentThread().interrupt();
                    }
                }

                System.out.println("  [" + getName() + "] Stopped");
            }
        }
    }

    /**
     * 2. 동적 크기 조절 스레드 풀
     */
    static class DynamicThreadPool {
        private final BlockingQueue<Runnable> taskQueue;
        private final List<WorkerThread> workers;
        private final int corePoolSize;
        private final int maxPoolSize;
        private final long keepAliveTime;
        private volatile boolean isShutdown = false;
        private final AtomicInteger poolSize = new AtomicInteger(0);

        public DynamicThreadPool(int corePoolSize, int maxPoolSize,
                                 long keepAliveTime, TimeUnit unit) {
            this.corePoolSize = corePoolSize;
            this.maxPoolSize = maxPoolSize;
            this.keepAliveTime = unit.toMillis(keepAliveTime);
            this.taskQueue = new LinkedBlockingQueue<>();
            this.workers = new ArrayList<>();

            // 코어 스레드 생성
            for (int i = 0; i < corePoolSize; i++) {
                addWorker();
            }

            System.out.println("  [DynamicPool] Created (core: " + corePoolSize +
                    ", max: " + maxPoolSize + ")");
        }

        public void submit(Runnable task) {
            if (isShutdown) {
                throw new RejectedExecutionException("Pool is shut down");
            }

            taskQueue.offer(task);

            // 작업이 많으면 워커 추가
            if (taskQueue.size() > 2 && poolSize.get() < maxPoolSize) {
                synchronized (this) {
                    if (poolSize.get() < maxPoolSize) {
                        addWorker();
                        System.out.println("  [DynamicPool] Added worker (size: " +
                                poolSize.get() + ")");
                    }
                }
            }
        }

        private synchronized void addWorker() {
            WorkerThread worker = new WorkerThread(poolSize.getAndIncrement());
            workers.add(worker);
            worker.start();
        }

        public void shutdown() {
            isShutdown = true;
            synchronized (this) {
                for (WorkerThread worker : workers) {
                    worker.interrupt();
                }
            }
        }

        public int getPoolSize() {
            return poolSize.get();
        }

        private class WorkerThread extends Thread {
            private final int id;

            public WorkerThread(int id) {
                super("DynamicWorker-" + id);
                this.id = id;
            }

            @Override
            public void run() {
                long lastTaskTime = System.currentTimeMillis();

                while (!isShutdown) {
                    try {
                        Runnable task = taskQueue.poll(keepAliveTime, TimeUnit.MILLISECONDS);

                        if (task != null) {
                            task.run();
                            lastTaskTime = System.currentTimeMillis();
                        } else {
                            // 타임아웃: 코어 스레드가 아니면 종료
                            if (id >= corePoolSize) {
                                long idle = System.currentTimeMillis() - lastTaskTime;
                                if (idle >= keepAliveTime) {
                                    System.out.println("  [" + getName() +
                                            "] Idle timeout, terminating");
                                    poolSize.decrementAndGet();
                                    break;
                                }
                            }
                        }
                    } catch (InterruptedException e) {
                        if (isShutdown) {
                            break;
                        }
                    }
                }
            }
        }
    }

    /**
     * 3. 우선순위 스레드 풀
     */
    static class PriorityThreadPool {
        private final PriorityBlockingQueue<PriorityTask> taskQueue;
        private final List<WorkerThread> workers;
        private volatile boolean isShutdown = false;

        static class PriorityTask implements Comparable<PriorityTask> {
            private final Runnable task;
            private final int priority;
            private final long submissionTime;

            public PriorityTask(Runnable task, int priority) {
                this.task = task;
                this.priority = priority;
                this.submissionTime = System.currentTimeMillis();
            }

            @Override
            public int compareTo(PriorityTask other) {
                // 높은 우선순위 먼저
                int priorityCompare = Integer.compare(other.priority, this.priority);
                if (priorityCompare != 0) {
                    return priorityCompare;
                }
                // 같은 우선순위면 먼저 제출된 것 먼저
                return Long.compare(this.submissionTime, other.submissionTime);
            }
        }

        public PriorityThreadPool(int poolSize) {
            this.taskQueue = new PriorityBlockingQueue<>();
            this.workers = new ArrayList<>();

            for (int i = 0; i < poolSize; i++) {
                WorkerThread worker = new WorkerThread(i);
                workers.add(worker);
                worker.start();
            }

            System.out.println("  [PriorityPool] Created with " + poolSize + " workers");
        }

        public void submit(Runnable task, int priority) {
            if (isShutdown) {
                throw new RejectedExecutionException("Pool is shut down");
            }

            taskQueue.offer(new PriorityTask(task, priority));
            System.out.println("  [PriorityPool] Task submitted with priority " + priority);
        }

        public void shutdown() {
            isShutdown = true;
            for (WorkerThread worker : workers) {
                worker.interrupt();
            }
        }

        private class WorkerThread extends Thread {
            public WorkerThread(int id) {
                super("PriorityWorker-" + id);
            }

            @Override
            public void run() {
                while (!isShutdown || !taskQueue.isEmpty()) {
                    try {
                        PriorityTask priorityTask = taskQueue.poll(100, TimeUnit.MILLISECONDS);

                        if (priorityTask != null) {
                            System.out.println("  [" + getName() + "] Processing priority " +
                                    priorityTask.priority + " task");
                            priorityTask.task.run();
                        }
                    } catch (InterruptedException e) {
                        if (isShutdown) {
                            break;
                        }
                    }
                }
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Custom Thread Pool Demo ===\n");

        // 1. 기본 스레드 풀
        demonstrateBasicThreadPool();

        // 2. 동적 크기 조절 스레드 풀
        demonstrateDynamicThreadPool();

        // 3. 우선순위 스레드 풀
        demonstratePriorityThreadPool();

        System.out.println("\n=== Demo Completed ===");
    }

    private static void demonstrateBasicThreadPool() throws InterruptedException {
        System.out.println("1. Basic Thread Pool Demo");

        BasicThreadPool pool = new BasicThreadPool(3, 10);

        // 작업 제출
        for (int i = 1; i <= 8; i++) {
            final int taskId = i;
            pool.submit(() -> {
                System.out.println("    [Task-" + taskId + "] Executing");
                sleep(200);
                System.out.println("    [Task-" + taskId + "] Done");
            });
            Thread.sleep(50);
        }

        System.out.println("  [Main] Active workers: " + pool.getActiveCount());
        System.out.println("  [Main] Queue size: " + pool.getQueueSize());

        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);

        System.out.println("  [Main] Completed tasks: " + pool.getCompletedTaskCount());
        System.out.println();
    }

    private static void demonstrateDynamicThreadPool() throws InterruptedException {
        System.out.println("2. Dynamic Thread Pool Demo");

        DynamicThreadPool pool = new DynamicThreadPool(2, 5, 1, TimeUnit.SECONDS);

        // 초기 부하
        System.out.println("  [Main] Submitting initial tasks...");
        for (int i = 1; i <= 3; i++) {
            final int taskId = i;
            pool.submit(() -> {
                System.out.println("    [Task-" + taskId + "] Executing");
                sleep(300);
            });
        }

        Thread.sleep(100);
        System.out.println("  [Main] Pool size: " + pool.getPoolSize());

        // 추가 부하 (워커 증가 유도)
        System.out.println("  [Main] Submitting more tasks...");
        for (int i = 4; i <= 8; i++) {
            final int taskId = i;
            pool.submit(() -> {
                System.out.println("    [Task-" + taskId + "] Executing");
                sleep(300);
            });
            Thread.sleep(50);
        }

        Thread.sleep(100);
        System.out.println("  [Main] Pool size after load: " + pool.getPoolSize());

        pool.shutdown();
        Thread.sleep(2000);
        System.out.println();
    }

    private static void demonstratePriorityThreadPool() throws InterruptedException {
        System.out.println("3. Priority Thread Pool Demo");

        PriorityThreadPool pool = new PriorityThreadPool(2);

        // 다양한 우선순위 작업 제출
        pool.submit(() -> {
            System.out.println("    [Low Priority] Executing");
            sleep(100);
        }, 1);

        pool.submit(() -> {
            System.out.println("    [High Priority] Executing");
            sleep(100);
        }, 10);

        pool.submit(() -> {
            System.out.println("    [Medium Priority] Executing");
            sleep(100);
        }, 5);

        pool.submit(() -> {
            System.out.println("    [Highest Priority] Executing");
            sleep(100);
        }, 20);

        Thread.sleep(50); // 모든 작업이 큐에 들어가도록

        pool.shutdown();
        Thread.sleep(1000);

        System.out.println("  Note: Tasks executed in priority order");
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