package com.study.thread.pool;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

/**
 * WorkStealingPool 실습
 *
 * Executors.newWorkStealingPool()을 사용한 작업 훔치기 풀 학습
 * - ForkJoinPool 기반
 * - 각 워커 스레드가 독립적인 큐 보유
 * - 유휴 스레드가 다른 스레드의 작업을 훔침
 * - 병렬 처리 및 분할 정복에 최적화
 */
public class WorkStealingPoolDemo {

    /**
     * 1. 기본 WorkStealingPool 사용법
     */
    static class BasicUsage {
        public void demonstrate() throws InterruptedException {
            System.out.println("  Creating WorkStealingPool");

            // 기본: CPU 코어 수만큼 병렬 수준 설정
            ExecutorService executor = Executors.newWorkStealingPool();

            System.out.println("  Parallelism: " +
                    Runtime.getRuntime().availableProcessors());

            // 작업 제출
            for (int i = 1; i <= 8; i++) {
                final int taskId = i;
                executor.submit(() -> {
                    System.out.println("    [Task-" + taskId + "] Running on " +
                            Thread.currentThread().getName());
                    sleep(100);
                    System.out.println("    [Task-" + taskId + "] Completed");
                });
            }

            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    /**
     * 2. 커스텀 병렬 수준
     */
    static class CustomParallelism {
        public void demonstrate() throws InterruptedException {
            int parallelism = 4;
            System.out.println("  Creating WorkStealingPool with parallelism: " + parallelism);

            ExecutorService executor = Executors.newWorkStealingPool(parallelism);

            CountDownLatch latch = new CountDownLatch(8);
            AtomicInteger activeThreads = new AtomicInteger(0);

            for (int i = 1; i <= 8; i++) {
                final int taskId = i;
                executor.submit(() -> {
                    activeThreads.incrementAndGet();
                    System.out.println("    [Task-" + taskId + "] on " +
                            Thread.currentThread().getName() +
                            " (active: " + activeThreads.get() + ")");
                    sleep(200);
                    activeThreads.decrementAndGet();
                    latch.countDown();
                });
            }

            latch.await();
            executor.shutdown();
        }
    }

    /**
     * 3. RecursiveTask를 사용한 분할 정복
     */
    static class RecursiveTaskExample {
        // 피보나치 계산 (분할 정복)
        static class FibonacciTask extends RecursiveTask<Long> {
            private final int n;

            public FibonacciTask(int n) {
                this.n = n;
            }

            @Override
            protected Long compute() {
                if (n <= 1) {
                    return (long) n;
                }

                // 작은 작업은 직접 계산
                if (n <= 10) {
                    return fib(n);
                }

                // 큰 작업은 분할
                FibonacciTask f1 = new FibonacciTask(n - 1);
                FibonacciTask f2 = new FibonacciTask(n - 2);

                f1.fork(); // 비동기 실행
                long result2 = f2.compute(); // 현재 스레드에서 실행
                long result1 = f1.join(); // f1 결과 대기

                return result1 + result2;
            }

            private long fib(int n) {
                if (n <= 1) return n;
                return fib(n - 1) + fib(n - 2);
            }
        }

        public void demonstrate() throws InterruptedException, ExecutionException {
            ForkJoinPool pool = (ForkJoinPool) Executors.newWorkStealingPool();

            int n = 30;
            System.out.println("  Computing Fibonacci(" + n + ") using divide-and-conquer");

            long start = System.currentTimeMillis();
            FibonacciTask task = new FibonacciTask(n);
            Long result = pool.invoke(task);
            long duration = System.currentTimeMillis() - start;

            System.out.println("  Result: " + result);
            System.out.println("  Time: " + duration + " ms");
            System.out.println("  Parallelism: " + pool.getParallelism());
            System.out.println("  Pool size: " + pool.getPoolSize());

            pool.shutdown();
        }
    }

    /**
     * 4. RecursiveAction을 사용한 배열 처리
     */
    static class RecursiveActionExample {
        // 배열의 각 요소를 제곱
        static class ArraySquareTask extends RecursiveAction {
            private final int[] array;
            private final int start;
            private final int end;
            private static final int THRESHOLD = 10;

            public ArraySquareTask(int[] array, int start, int end) {
                this.array = array;
                this.start = start;
                this.end = end;
            }

            @Override
            protected void compute() {
                int length = end - start;

                if (length <= THRESHOLD) {
                    // 작은 작업은 직접 처리
                    for (int i = start; i < end; i++) {
                        array[i] = array[i] * array[i];
                    }
                    System.out.println("    [" + Thread.currentThread().getName() + "] " +
                            "Processed " + start + " to " + end);
                } else {
                    // 큰 작업은 분할
                    int mid = start + length / 2;
                    ArraySquareTask left = new ArraySquareTask(array, start, mid);
                    ArraySquareTask right = new ArraySquareTask(array, mid, end);

                    invokeAll(left, right); // 두 작업을 병렬 실행
                }
            }
        }

        public void demonstrate() {
            ForkJoinPool pool = (ForkJoinPool) Executors.newWorkStealingPool();

            int size = 50;
            int[] array = IntStream.range(1, size + 1).toArray();

            System.out.println("  Squaring array of " + size + " elements");
            System.out.println("  First 5 elements before: " +
                    java.util.Arrays.toString(java.util.Arrays.copyOfRange(array, 0, 5)));

            ArraySquareTask task = new ArraySquareTask(array, 0, array.length);
            pool.invoke(task);

            System.out.println("  First 5 elements after: " +
                    java.util.Arrays.toString(java.util.Arrays.copyOfRange(array, 0, 5)));

            pool.shutdown();
        }
    }

    /**
     * 5. Work Stealing 동작 관찰
     */
    static class WorkStealingObservation {
        public void demonstrate() throws InterruptedException {
            ForkJoinPool pool = (ForkJoinPool) Executors.newWorkStealingPool(2);

            System.out.println("  Observing work stealing (parallelism: 2)");

            CountDownLatch latch = new CountDownLatch(10);

            // 불균형한 작업 부하
            for (int i = 1; i <= 10; i++) {
                final int taskId = i;
                final int workload = (i <= 5) ? 500 : 50; // 앞쪽 작업이 더 무거움

                pool.submit(() -> {
                    System.out.println("    [Task-" + taskId + "] Start on " +
                            Thread.currentThread().getName() +
                            " (workload: " + workload + "ms)");
                    sleep(workload);
                    System.out.println("    [Task-" + taskId + "] End on " +
                            Thread.currentThread().getName());
                    latch.countDown();
                });
            }

            latch.await();

            System.out.println("  Steal count: " + pool.getStealCount());
            System.out.println("  Note: Light tasks were likely 'stolen' by idle threads");

            pool.shutdown();
        }
    }

    /**
     * 6. 병렬 스트림과의 통합
     */
    static class ParallelStreamIntegration {
        public void demonstrate() {
            ForkJoinPool customPool = (ForkJoinPool) Executors.newWorkStealingPool(4);

            System.out.println("  Using custom ForkJoinPool with parallel stream");

            List<Integer> numbers = IntStream.rangeClosed(1, 20)
                    .boxed()
                    .collect(java.util.stream.Collectors.toList());

            try {
                // 커스텀 풀에서 병렬 스트림 실행
                customPool.submit(() -> {
                    numbers.parallelStream()
                            .forEach(n -> {
                                System.out.println("    [" + n + "] on " +
                                        Thread.currentThread().getName());
                                sleep(50);
                            });
                }).get();

            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }

            customPool.shutdown();
        }
    }

    /**
     * 7. FixedThreadPool vs WorkStealingPool 성능 비교
     */
    static class PerformanceComparison {
        public void demonstrate() throws InterruptedException, ExecutionException {
            int taskCount = 100;

            // FixedThreadPool 테스트
            System.out.println("  Testing FixedThreadPool:");
            ExecutorService fixed = Executors.newFixedThreadPool(
                    Runtime.getRuntime().availableProcessors());
            long fixedTime = runTasks(fixed, taskCount);
            fixed.shutdown();

            Thread.sleep(100);

            // WorkStealingPool 테스트
            System.out.println("\n  Testing WorkStealingPool:");
            ExecutorService workStealing = Executors.newWorkStealingPool();
            long wsTime = runTasks(workStealing, taskCount);
            workStealing.shutdown();

            // 비교
            System.out.println("\n  Comparison:");
            System.out.println("    FixedThreadPool: " + fixedTime + " ms");
            System.out.println("    WorkStealingPool: " + wsTime + " ms");
            System.out.println("    Difference: " +
                    Math.abs(fixedTime - wsTime) + " ms");
        }

        private long runTasks(ExecutorService executor, int taskCount)
                throws InterruptedException, ExecutionException {
            long start = System.currentTimeMillis();

            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < taskCount; i++) {
                final int taskId = i;
                // 불균형한 작업 부하
                int workload = (taskId % 10 == 0) ? 50 : 10;

                futures.add(executor.submit(() -> {
                    sleep(workload);
                }));
            }

            for (Future<?> future : futures) {
                future.get();
            }

            return System.currentTimeMillis() - start;
        }
    }

    /**
     * 8. 재귀적 작업 분할 - 병합 정렬
     */
    static class MergeSortTask extends RecursiveAction {
        private final int[] array;
        private final int start;
        private final int end;
        private static final int THRESHOLD = 10;

        public MergeSortTask(int[] array, int start, int end) {
            this.array = array;
            this.start = start;
            this.end = end;
        }

        @Override
        protected void compute() {
            int length = end - start;

            if (length <= THRESHOLD) {
                // 작은 배열은 직접 정렬
                insertionSort(array, start, end);
            } else {
                // 큰 배열은 분할하여 병렬 정렬
                int mid = start + length / 2;

                MergeSortTask left = new MergeSortTask(array, start, mid);
                MergeSortTask right = new MergeSortTask(array, mid, end);

                invokeAll(left, right);

                // 병합
                merge(array, start, mid, end);
            }
        }

        private void insertionSort(int[] arr, int start, int end) {
            for (int i = start + 1; i < end; i++) {
                int key = arr[i];
                int j = i - 1;
                while (j >= start && arr[j] > key) {
                    arr[j + 1] = arr[j];
                    j--;
                }
                arr[j + 1] = key;
            }
        }

        private void merge(int[] arr, int start, int mid, int end) {
            int[] temp = new int[end - start];
            int i = start, j = mid, k = 0;

            while (i < mid && j < end) {
                temp[k++] = (arr[i] <= arr[j]) ? arr[i++] : arr[j++];
            }

            while (i < mid) temp[k++] = arr[i++];
            while (j < end) temp[k++] = arr[j++];

            System.arraycopy(temp, 0, arr, start, temp.length);
        }
    }

    static class MergeSortExample {
        public void demonstrate() {
            ForkJoinPool pool = (ForkJoinPool) Executors.newWorkStealingPool();

            int size = 100;
            int[] array = new int[size];
            for (int i = 0; i < size; i++) {
                array[i] = (int) (Math.random() * 1000);
            }

            System.out.println("  Parallel merge sort of " + size + " elements");
            System.out.println("  First 10 before: " +
                    java.util.Arrays.toString(java.util.Arrays.copyOfRange(array, 0, 10)));

            long start = System.currentTimeMillis();
            pool.invoke(new MergeSortTask(array, 0, array.length));
            long duration = System.currentTimeMillis() - start;

            System.out.println("  First 10 after: " +
                    java.util.Arrays.toString(java.util.Arrays.copyOfRange(array, 0, 10)));
            System.out.println("  Time: " + duration + " ms");

            pool.shutdown();
        }
    }

    /**
     * 9. 통계 정보
     */
    static class PoolStatistics {
        public void demonstrate() throws InterruptedException {
            ForkJoinPool pool = (ForkJoinPool) Executors.newWorkStealingPool();

            System.out.println("  Submitting tasks and monitoring statistics");

            CountDownLatch latch = new CountDownLatch(20);

            for (int i = 1; i <= 20; i++) {
                pool.submit(() -> {
                    sleep(100 + (int) (Math.random() * 100));
                    latch.countDown();
                });
            }

            // 주기적으로 통계 출력
            for (int i = 0; i < 5; i++) {
                Thread.sleep(100);
                printStatistics(pool);
            }

            latch.await();
            printStatistics(pool);

            pool.shutdown();
        }

        private void printStatistics(ForkJoinPool pool) {
            System.out.println("    Parallelism: " + pool.getParallelism() +
                    ", Pool size: " + pool.getPoolSize() +
                    ", Active: " + pool.getActiveThreadCount() +
                    ", Running: " + pool.getRunningThreadCount() +
                    ", Queued: " + pool.getQueuedTaskCount() +
                    ", Steal: " + pool.getStealCount());
        }
    }

    /**
     * 10. 사용 가이드라인
     */
    static class UsageGuidelines {
        public void demonstrate() {
            System.out.println("  WorkStealingPool 사용 가이드라인:");
            System.out.println();

            System.out.println("  ✓ 적합한 경우:");
            System.out.println("    - 분할 정복 알고리즘");
            System.out.println("    - 재귀적 작업");
            System.out.println("    - 작업 부하가 불균형한 경우");
            System.out.println("    - CPU 집약적 병렬 처리");
            System.out.println("    - 독립적인 작업들의 병렬 실행");
            System.out.println();

            System.out.println("  ✗ 부적합한 경우:");
            System.out.println("    - 블로킹 I/O 작업");
            System.out.println("    - 작업 간 의존성이 있는 경우");
            System.out.println("    - 작업 순서가 중요한 경우");
            System.out.println("    - 매우 짧은 작업 (오버헤드)");
            System.out.println();

            System.out.println("  특징:");
            System.out.println("    - ForkJoinPool 기반");
            System.out.println("    - 각 워커가 자신의 deque 보유");
            System.out.println("    - Work stealing으로 부하 분산");
            System.out.println("    - 병렬 스트림의 기본 풀");
            System.out.println();

            System.out.println("  vs FixedThreadPool:");
            System.out.println("    - WorkStealing: 동적 부하 분산, 재귀 작업");
            System.out.println("    - Fixed: 단순 작업 큐, 예측 가능한 동작");
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== WorkStealingPool Demo ===\n");

        // 1. 기본 사용법
        System.out.println("1. Basic Usage");
        new BasicUsage().demonstrate();
        System.out.println();

        // 2. 커스텀 병렬 수준
        System.out.println("2. Custom Parallelism");
        new CustomParallelism().demonstrate();
        System.out.println();

        // 3. RecursiveTask 예제
        System.out.println("3. RecursiveTask Example (Fibonacci)");
        new RecursiveTaskExample().demonstrate();
        System.out.println();

        // 4. RecursiveAction 예제
        System.out.println("4. RecursiveAction Example (Array Square)");
        new RecursiveActionExample().demonstrate();
        System.out.println();

        // 5. Work Stealing 관찰
        System.out.println("5. Work Stealing Observation");
        new WorkStealingObservation().demonstrate();
        System.out.println();

        // 6. 병렬 스트림 통합
        System.out.println("6. Parallel Stream Integration");
        new ParallelStreamIntegration().demonstrate();
        System.out.println();

        // 7. 성능 비교
        System.out.println("7. Performance Comparison");
        new PerformanceComparison().demonstrate();
        System.out.println();

        // 8. 병합 정렬
        System.out.println("8. Merge Sort Example");
        new MergeSortExample().demonstrate();
        System.out.println();

        // 9. 통계 정보
        System.out.println("9. Pool Statistics");
        new PoolStatistics().demonstrate();
        System.out.println();

        // 10. 사용 가이드라인
        System.out.println("10. Usage Guidelines");
        new UsageGuidelines().demonstrate();
        System.out.println();

        System.out.println("=== Demo Completed ===");
    }

    private static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}