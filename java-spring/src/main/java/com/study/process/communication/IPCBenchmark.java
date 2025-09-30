package com.study.process.communication;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * IPC 성능 측정 및 비교
 * 다양한 IPC 메커니즘의 성능을 벤치마크
 */
public class IPCBenchmark {

    /**
     * 벤치마크 결과
     */
    public static class BenchmarkResult {
        private final String method;
        private final int messageCount;
        private final int messageSize;
        private final long totalTime;
        private final double throughput;
        private final double avgLatency;
        private final double minLatency;
        private final double maxLatency;
        private final double p50Latency;
        private final double p95Latency;
        private final double p99Latency;

        public BenchmarkResult(String method, int messageCount, int messageSize,
                               long totalTime, List<Long> latencies) {
            this.method = method;
            this.messageCount = messageCount;
            this.messageSize = messageSize;
            this.totalTime = totalTime;

            // Throughput 계산 (messages/second)
            this.throughput = (messageCount * 1000.0) / totalTime;

            // Latency 통계 계산
            Collections.sort(latencies);

            this.avgLatency = latencies.stream()
                    .mapToLong(Long::longValue)
                    .average()
                    .orElse(0.0) / 1_000_000.0; // ns to ms

            this.minLatency = latencies.isEmpty() ? 0 :
                    latencies.get(0) / 1_000_000.0;

            this.maxLatency = latencies.isEmpty() ? 0 :
                    latencies.get(latencies.size() - 1) / 1_000_000.0;

            this.p50Latency = getPercentile(latencies, 50) / 1_000_000.0;
            this.p95Latency = getPercentile(latencies, 95) / 1_000_000.0;
            this.p99Latency = getPercentile(latencies, 99) / 1_000_000.0;
        }

        private double getPercentile(List<Long> sorted, int percentile) {
            if (sorted.isEmpty()) return 0;

            int index = (int) Math.ceil(percentile / 100.0 * sorted.size()) - 1;
            return sorted.get(Math.max(0, Math.min(index, sorted.size() - 1)));
        }

        @Override
        public String toString() {
            return String.format(
                    """
                    === %s Benchmark Results ===
                    Message Count: %d
                    Message Size: %d bytes
                    Total Time: %.2f ms
                    Throughput: %.2f msg/sec
                    
                    Latency Statistics (ms):
                      Average: %.3f
                      Min: %.3f
                      Max: %.3f
                      P50: %.3f
                      P95: %.3f
                      P99: %.3f
                    """,
                    method, messageCount, messageSize, totalTime / 1_000_000.0,
                    throughput, avgLatency, minLatency, maxLatency,
                    p50Latency, p95Latency, p99Latency
            );
        }

        // Getters
        public String getMethod() { return method; }
        public int getMessageCount() { return messageCount; }
        public int getMessageSize() { return messageSize; }
        public long getTotalTime() { return totalTime; }
        public double getThroughput() { return throughput; }
        public double getAvgLatency() { return avgLatency; }
    }

    /**
     * Socket IPC 벤치마크
     */
    public static class SocketBenchmark {

        public BenchmarkResult benchmarkTCP(int messageCount, int messageSize)
                throws Exception {
            int port = 9000 + new Random().nextInt(1000);

            // 서버 시작
            SocketIPC.TCPServer server = new SocketIPC.TCPServer(port);
            server.start();
            Thread.sleep(100); // 서버 시작 대기

            // 클라이언트 연결
            SocketIPC.TCPClient client = new SocketIPC.TCPClient("localhost", port);
            client.connect();

            List<Long> latencies = new ArrayList<>();
            byte[] data = generateRandomData(messageSize);

            long startTime = System.nanoTime();

            for (int i = 0; i < messageCount; i++) {
                long msgStart = System.nanoTime();

                SocketIPC.Message message = new SocketIPC.Message(
                        "BENCHMARK",
                        new String(data, StandardCharsets.UTF_8)
                );

                client.send(message);
                SocketIPC.Message response = client.receive(1, TimeUnit.SECONDS);

                long msgEnd = System.nanoTime();
                latencies.add(msgEnd - msgStart);
            }

            long totalTime = System.nanoTime() - startTime;

            // 정리
            client.disconnect();
            server.stop();

            return new BenchmarkResult("TCP Socket", messageCount, messageSize,
                    totalTime, latencies);
        }

        public BenchmarkResult benchmarkUDP(int messageCount, int messageSize)
                throws Exception {
            int port1 = 8000 + new Random().nextInt(1000);
            int port2 = port1 + 1;

            // 송수신자 생성
            SocketIPC.UDPCommunicator sender = new SocketIPC.UDPCommunicator(port1);
            SocketIPC.UDPCommunicator receiver = new SocketIPC.UDPCommunicator(port2);

            sender.start();
            receiver.start();
            Thread.sleep(100);

            List<Long> latencies = new ArrayList<>();
            byte[] data = generateRandomData(messageSize);
            String message = new String(data, StandardCharsets.UTF_8);

            long startTime = System.nanoTime();

            for (int i = 0; i < messageCount; i++) {
                long msgStart = System.nanoTime();

                sender.send(message, "localhost", port2);
                SocketIPC.UDPCommunicator.DatagramMessage received =
                        receiver.receive(1, TimeUnit.SECONDS);

                long msgEnd = System.nanoTime();
                latencies.add(msgEnd - msgStart);
            }

            long totalTime = System.nanoTime() - startTime;

            // 정리
            sender.stop();
            receiver.stop();

            return new BenchmarkResult("UDP Socket", messageCount, messageSize,
                    totalTime, latencies);
        }
    }

    /**
     * Pipe IPC 벤치마크
     */
    public static class PipeBenchmark {

        public BenchmarkResult benchmarkAnonymousPipe(int messageCount, int messageSize)
                throws Exception {
            PipeIPC.AnonymousPipe pipe = new PipeIPC.AnonymousPipe();

            List<Long> latencies = Collections.synchronizedList(new ArrayList<>());
            byte[] data = generateRandomData(messageSize);
            String message = new String(data, StandardCharsets.UTF_8);

            CountDownLatch latch = new CountDownLatch(messageCount);

            // 리더 스레드
            Thread reader = new Thread(() -> {
                try {
                    for (int i = 0; i < messageCount; i++) {
                        String received = pipe.readLine();
                        latch.countDown();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            reader.start();

            long startTime = System.nanoTime();

            // 라이터
            for (int i = 0; i < messageCount; i++) {
                long msgStart = System.nanoTime();
                pipe.write(message);
                long msgEnd = System.nanoTime();
                latencies.add(msgEnd - msgStart);
            }

            latch.await();
            long totalTime = System.nanoTime() - startTime;

            pipe.close();
            reader.join();

            return new BenchmarkResult("Anonymous Pipe", messageCount, messageSize,
                    totalTime, latencies);
        }

        public BenchmarkResult benchmarkBidirectionalPipe(int messageCount, int messageSize)
                throws Exception {
            PipeIPC.BidirectionalPipe pipe = new PipeIPC.BidirectionalPipe();

            List<Long> latencies = Collections.synchronizedList(new ArrayList<>());
            byte[] data = generateRandomData(messageSize);
            String message = new String(data, StandardCharsets.UTF_8);

            // Echo 서버 스레드
            Thread echoServer = new Thread(() -> {
                try {
                    for (int i = 0; i < messageCount; i++) {
                        String received = pipe.readFromA();
                        pipe.writeToA("Echo: " + received.substring(0,
                                Math.min(10, received.length())));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            echoServer.start();

            long startTime = System.nanoTime();

            // 클라이언트
            for (int i = 0; i < messageCount; i++) {
                long msgStart = System.nanoTime();

                pipe.writeToB(message);
                String response = pipe.readFromB();

                long msgEnd = System.nanoTime();
                latencies.add(msgEnd - msgStart);
            }

            long totalTime = System.nanoTime() - startTime;

            pipe.close();
            echoServer.join();

            return new BenchmarkResult("Bidirectional Pipe", messageCount, messageSize,
                    totalTime, latencies);
        }
    }

    /**
     * Shared Memory 벤치마크
     */
    public static class SharedMemoryBenchmark {

        public BenchmarkResult benchmarkSharedMemorySegment(int messageCount, int messageSize)
                throws Exception {
            String segmentName = "benchmark_" + System.currentTimeMillis();

            SharedMemoryIPC.SharedMemorySegment writer =
                    new SharedMemoryIPC.SharedMemorySegment(segmentName, messageSize + 100, true);
            SharedMemoryIPC.SharedMemorySegment reader =
                    new SharedMemoryIPC.SharedMemorySegment(segmentName, messageSize + 100, false);

            List<Long> latencies = new ArrayList<>();
            byte[] data = generateRandomData(messageSize);

            long startTime = System.nanoTime();

            for (int i = 0; i < messageCount; i++) {
                long msgStart = System.nanoTime();

                writer.write(data, 0);
                byte[] received = reader.read(0, messageSize);

                long msgEnd = System.nanoTime();
                latencies.add(msgEnd - msgStart);
            }

            long totalTime = System.nanoTime() - startTime;

            writer.close();
            reader.close();

            return new BenchmarkResult("Shared Memory", messageCount, messageSize,
                    totalTime, latencies);
        }

        public BenchmarkResult benchmarkSharedMemoryQueue(int messageCount, int messageSize)
                throws Exception {
            String queueName = "queue_" + System.currentTimeMillis();

            SharedMemoryIPC.SharedMemoryQueue producer =
                    new SharedMemoryIPC.SharedMemoryQueue(queueName, 100, messageSize, true);
            SharedMemoryIPC.SharedMemoryQueue consumer =
                    new SharedMemoryIPC.SharedMemoryQueue(queueName, 100, messageSize, false);

            List<Long> latencies = Collections.synchronizedList(new ArrayList<>());
            byte[] data = generateRandomData(messageSize);

            CountDownLatch latch = new CountDownLatch(messageCount);

            // Consumer 스레드
            Thread consumerThread = new Thread(() -> {
                try {
                    for (int i = 0; i < messageCount; i++) {
                        byte[] received = consumer.dequeue();
                        latch.countDown();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            consumerThread.start();

            long startTime = System.nanoTime();

            // Producer
            for (int i = 0; i < messageCount; i++) {
                long msgStart = System.nanoTime();
                producer.enqueue(data);
                long msgEnd = System.nanoTime();
                latencies.add(msgEnd - msgStart);
            }

            latch.await();
            long totalTime = System.nanoTime() - startTime;

            producer.close();
            consumer.close();
            consumerThread.join();

            return new BenchmarkResult("Shared Memory Queue", messageCount, messageSize,
                    totalTime, latencies);
        }
    }

    /**
     * Message Queue 벤치마크
     */
    public static class MessageQueueBenchmark {

        public BenchmarkResult benchmarkSimpleQueue(int messageCount, int messageSize)
                throws Exception {
            MessageQueue.QueueManager manager = new MessageQueue.QueueManager();
            MessageQueue.SimpleQueue queue = manager.createSimpleQueue("benchmark", 10000);

            List<Long> latencies = Collections.synchronizedList(new ArrayList<>());
            byte[] data = generateRandomData(messageSize);
            String payload = new String(data, StandardCharsets.UTF_8);

            CountDownLatch latch = new CountDownLatch(messageCount);

            // Consumer 스레드
            Thread consumer = new Thread(() -> {
                try {
                    for (int i = 0; i < messageCount; i++) {
                        MessageQueue.Envelope msg = queue.receive(1, TimeUnit.SECONDS);
                        latch.countDown();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            consumer.start();

            long startTime = System.nanoTime();

            // Producer
            for (int i = 0; i < messageCount; i++) {
                long msgStart = System.nanoTime();

                MessageQueue.Envelope envelope = new MessageQueue.Envelope.Builder()
                        .sender("producer")
                        .topic("benchmark")
                        .payload(payload)
                        .build();

                queue.send(envelope);

                long msgEnd = System.nanoTime();
                latencies.add(msgEnd - msgStart);
            }

            latch.await();
            long totalTime = System.nanoTime() - startTime;

            manager.shutdown();
            consumer.join();

            return new BenchmarkResult("Message Queue", messageCount, messageSize,
                    totalTime, latencies);
        }
    }

    /**
     * 종합 벤치마크 실행
     */
    public static class BenchmarkRunner {
        private final List<BenchmarkResult> results;

        public BenchmarkRunner() {
            this.results = new ArrayList<>();
        }

        public void runAllBenchmarks(int messageCount, int messageSize) {
            System.out.println("Starting IPC Benchmarks...");
            System.out.println("Message Count: " + messageCount);
            System.out.println("Message Size: " + messageSize + " bytes\n");

            // Socket 벤치마크
            try {
                SocketBenchmark socketBench = new SocketBenchmark();
                results.add(socketBench.benchmarkTCP(messageCount, messageSize));
                results.add(socketBench.benchmarkUDP(messageCount, messageSize));
            } catch (Exception e) {
                System.err.println("Socket benchmark failed: " + e.getMessage());
            }

            // Pipe 벤치마크
            try {
                PipeBenchmark pipeBench = new PipeBenchmark();
                results.add(pipeBench.benchmarkAnonymousPipe(messageCount, messageSize));
                results.add(pipeBench.benchmarkBidirectionalPipe(messageCount, messageSize));
            } catch (Exception e) {
                System.err.println("Pipe benchmark failed: " + e.getMessage());
            }

            // Shared Memory 벤치마크
            try {
                SharedMemoryBenchmark shmBench = new SharedMemoryBenchmark();
                results.add(shmBench.benchmarkSharedMemorySegment(messageCount, messageSize));
                results.add(shmBench.benchmarkSharedMemoryQueue(messageCount, messageSize));
            } catch (Exception e) {
                System.err.println("Shared memory benchmark failed: " + e.getMessage());
            }

            // Message Queue 벤치마크
            try {
                MessageQueueBenchmark mqBench = new MessageQueueBenchmark();
                results.add(mqBench.benchmarkSimpleQueue(messageCount, messageSize));
            } catch (Exception e) {
                System.err.println("Message queue benchmark failed: " + e.getMessage());
            }
        }

        public void printResults() {
            System.out.println("\n========== BENCHMARK RESULTS ==========\n");

            for (BenchmarkResult result : results) {
                System.out.println(result);
            }

            printComparison();
        }

        private void printComparison() {
            System.out.println("\n========== PERFORMANCE COMPARISON ==========\n");

            // Throughput 순위
            System.out.println("Throughput Ranking (msg/sec):");
            results.stream()
                    .sorted((a, b) -> Double.compare(b.getThroughput(), a.getThroughput()))
                    .forEach(r -> System.out.printf("  %s: %.2f msg/sec\n",
                            r.getMethod(), r.getThroughput()));

            // Latency 순위
            System.out.println("\nAverage Latency Ranking (ms):");
            results.stream()
                    .sorted(Comparator.comparingDouble(BenchmarkResult::getAvgLatency))
                    .forEach(r -> System.out.printf("  %s: %.3f ms\n",
                            r.getMethod(), r.getAvgLatency()));
        }

        public void exportToCSV(String filename) throws IOException {
            try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
                writer.println("Method,MessageCount,MessageSize,Throughput,AvgLatency,MinLatency,MaxLatency,P50,P95,P99");

                for (BenchmarkResult result : results) {
                    writer.printf("%s,%d,%d,%.2f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f\n",
                            result.method,
                            result.messageCount,
                            result.messageSize,
                            result.throughput,
                            result.avgLatency,
                            result.minLatency,
                            result.maxLatency,
                            result.p50Latency,
                            result.p95Latency,
                            result.p99Latency
                    );
                }
            }

            System.out.println("\nResults exported to " + filename);
        }
    }

    // 유틸리티 메서드
    private static byte[] generateRandomData(int size) {
        byte[] data = new byte[size];
        new Random().nextBytes(data);
        return data;
    }

    // 메인 메서드
    public static void main(String[] args) {
        BenchmarkRunner runner = new BenchmarkRunner();

        // 다양한 크기로 벤치마크 실행
        int[] messageSizes = {64, 256, 1024, 4096, 16384};
        int messageCount = 1000;

        for (int size : messageSizes) {
            System.out.println("\n==========================================");
            runner.runAllBenchmarks(messageCount, size);
            runner.printResults();

            try {
                runner.exportToCSV("ipc_benchmark_" + size + "bytes.csv");
            } catch (IOException e) {
                System.err.println("Failed to export CSV: " + e.getMessage());
            }
        }
    }
}