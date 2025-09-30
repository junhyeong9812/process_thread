package com.study.process.communication;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 파이프 기반 프로세스 간 통신 구현
 * Named Pipe와 Anonymous Pipe를 통한 프로세스 간 데이터 교환
 */
public class PipeIPC {

    /**
     * Anonymous Pipe 구현 (같은 JVM 내 프로세스 간 통신)
     */
    public static class AnonymousPipe {
        private final PipedOutputStream outputStream;
        private final PipedInputStream inputStream;
        private final BufferedWriter writer;
        private final BufferedReader reader;
        private final String pipeId;

        public AnonymousPipe() throws IOException {
            this.pipeId = UUID.randomUUID().toString();
            this.outputStream = new PipedOutputStream();
            this.inputStream = new PipedInputStream(outputStream);
            this.writer = new BufferedWriter(
                    new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)
            );
            this.reader = new BufferedReader(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8)
            );
        }

        public void write(String data) throws IOException {
            writer.write(data);
            writer.newLine();
            writer.flush();
        }

        public String readLine() throws IOException {
            return reader.readLine();
        }

        public String readLine(long timeout, TimeUnit unit) throws IOException {
            Future<String> future = CompletableFuture.supplyAsync(() -> {
                try {
                    return reader.readLine();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            try {
                return future.get(timeout, unit);
            } catch (InterruptedException | ExecutionException e) {
                throw new IOException("Read interrupted", e);
            } catch (TimeoutException e) {
                future.cancel(true);
                return null;
            }
        }

        public void close() throws IOException {
            writer.close();
            reader.close();
            outputStream.close();
            inputStream.close();
        }

        public String getPipeId() { return pipeId; }

        public PipedOutputStream getOutputStream() { return outputStream; }
        public PipedInputStream getInputStream() { return inputStream; }
    }

    /**
     * Named Pipe 구현 (파일 시스템 기반)
     */
    public static class NamedPipe {
        private final String pipeName;
        private final Path pipePath;
        private RandomAccessFile pipeFile;
        private final BlockingQueue<String> messageQueue;
        private Thread readerThread;
        private Thread writerThread;
        private final AtomicBoolean isOpen;
        private final boolean isServer;

        // Windows Named Pipe 경로: \\.\pipe\pipename
        // Unix/Linux FIFO 경로: /tmp/pipename

        public NamedPipe(String pipeName, boolean isServer) {
            this.pipeName = pipeName;
            this.isServer = isServer;
            this.isOpen = new AtomicBoolean(false);
            this.messageQueue = new LinkedBlockingQueue<>();

            // OS별 파이프 경로 설정
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                // Windows는 실제 Named Pipe 대신 파일로 시뮬레이션
                this.pipePath = Paths.get(System.getProperty("java.io.tmpdir"),
                        "pipe_" + pipeName);
            } else {
                // Unix/Linux FIFO
                this.pipePath = Paths.get("/tmp", "pipe_" + pipeName);
            }
        }

        public void create() throws IOException {
            if (isOpen.get()) {
                throw new IllegalStateException("Pipe already open");
            }

            if (isServer) {
                // 서버 모드: 파이프 파일 생성
                Files.deleteIfExists(pipePath);
                Files.createFile(pipePath);
                System.out.println("Named pipe created: " + pipePath);
            } else {
                // 클라이언트 모드: 파이프 파일 존재 확인
                if (!Files.exists(pipePath)) {
                    throw new IOException("Pipe not found: " + pipePath);
                }
            }

            pipeFile = new RandomAccessFile(pipePath.toFile(), "rw");
            isOpen.set(true);

            // 리더 스레드 시작
            if (!isServer) {
                startReader();
            }

            // 라이터 스레드 시작
            if (isServer) {
                startWriter();
            }
        }

        private void startReader() {
            readerThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new FileReader(pipePath.toFile()))) {

                    String line;
                    while (isOpen.get() && (line = reader.readLine()) != null) {
                        messageQueue.offer(line);
                    }
                } catch (IOException e) {
                    if (isOpen.get()) {
                        System.err.println("Reader error: " + e.getMessage());
                    }
                }
            });
            readerThread.setName("NamedPipe-Reader-" + pipeName);
            readerThread.start();
        }

        private void startWriter() {
            writerThread = new Thread(() -> {
                try (BufferedWriter writer = new BufferedWriter(
                        new FileWriter(pipePath.toFile(), true))) {

                    while (isOpen.get()) {
                        try {
                            String message = messageQueue.poll(100, TimeUnit.MILLISECONDS);
                            if (message != null) {
                                writer.write(message);
                                writer.newLine();
                                writer.flush();
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                } catch (IOException e) {
                    if (isOpen.get()) {
                        System.err.println("Writer error: " + e.getMessage());
                    }
                }
            });
            writerThread.setName("NamedPipe-Writer-" + pipeName);
            writerThread.start();
        }

        public void write(String data) throws IOException {
            if (!isOpen.get()) {
                throw new IOException("Pipe is closed");
            }

            if (isServer) {
                messageQueue.offer(data);
            } else {
                // 클라이언트 직접 쓰기
                synchronized (pipeFile) {
                    pipeFile.seek(pipeFile.length());
                    pipeFile.writeBytes(data + System.lineSeparator());
                }
            }
        }

        public String read(long timeout, TimeUnit unit) throws InterruptedException {
            if (!isOpen.get()) {
                throw new IllegalStateException("Pipe is closed");
            }

            return messageQueue.poll(timeout, unit);
        }

        public void close() throws IOException {
            isOpen.set(false);

            if (readerThread != null) {
                readerThread.interrupt();
                try {
                    readerThread.join(1000);
                } catch (InterruptedException ignored) {}
            }

            if (writerThread != null) {
                writerThread.interrupt();
                try {
                    writerThread.join(1000);
                } catch (InterruptedException ignored) {}
            }

            if (pipeFile != null) {
                pipeFile.close();
            }

            if (isServer) {
                Files.deleteIfExists(pipePath);
            }

            System.out.println("Named pipe closed: " + pipeName);
        }

        public boolean isOpen() { return isOpen.get(); }
        public String getPipeName() { return pipeName; }
        public Path getPipePath() { return pipePath; }
    }

    /**
     * 양방향 파이프 구현
     */
    public static class BidirectionalPipe {
        private final AnonymousPipe pipe1; // A -> B
        private final AnonymousPipe pipe2; // B -> A
        private final String pipeId;

        public BidirectionalPipe() throws IOException {
            this.pipeId = UUID.randomUUID().toString();
            this.pipe1 = new AnonymousPipe();
            this.pipe2 = new AnonymousPipe();
        }

        public void writeToA(String data) throws IOException {
            pipe2.write(data);
        }

        public void writeToB(String data) throws IOException {
            pipe1.write(data);
        }

        public String readFromA() throws IOException {
            return pipe1.readLine();
        }

        public String readFromB() throws IOException {
            return pipe2.readLine();
        }

        public void close() throws IOException {
            pipe1.close();
            pipe2.close();
        }

        public String getPipeId() { return pipeId; }

        /**
         * 엔드포인트 A 획득
         */
        public PipeEndpoint getEndpointA() {
            return new PipeEndpoint() {
                @Override
                public void write(String data) throws IOException {
                    writeToB(data);
                }

                @Override
                public String read() throws IOException {
                    return readFromB();
                }

                @Override
                public void close() throws IOException {
                    pipe1.close();
                    pipe2.close();
                }
            };
        }

        /**
         * 엔드포인트 B 획득
         */
        public PipeEndpoint getEndpointB() {
            return new PipeEndpoint() {
                @Override
                public void write(String data) throws IOException {
                    writeToA(data);
                }

                @Override
                public String read() throws IOException {
                    return readFromA();
                }

                @Override
                public void close() throws IOException {
                    pipe1.close();
                    pipe2.close();
                }
            };
        }
    }

    /**
     * 파이프 엔드포인트 인터페이스
     */
    public interface PipeEndpoint {
        void write(String data) throws IOException;
        String read() throws IOException;
        void close() throws IOException;
    }

    /**
     * 파이프 풀 관리
     */
    public static class PipePool {
        private final Queue<AnonymousPipe> availablePipes;
        private final Set<AnonymousPipe> inUsePipes;
        private final int maxSize;

        public PipePool(int maxSize) {
            this.maxSize = maxSize;
            this.availablePipes = new ConcurrentLinkedQueue<>();
            this.inUsePipes = Collections.synchronizedSet(new HashSet<>());
        }

        public AnonymousPipe acquire() throws IOException {
            AnonymousPipe pipe = availablePipes.poll();

            if (pipe == null) {
                if (inUsePipes.size() >= maxSize) {
                    throw new IOException("Pipe pool exhausted");
                }
                pipe = new AnonymousPipe();
            }

            inUsePipes.add(pipe);
            return pipe;
        }

        public void release(AnonymousPipe pipe) {
            if (inUsePipes.remove(pipe)) {
                availablePipes.offer(pipe);
            }
        }

        public void closeAll() throws IOException {
            IOException lastException = null;

            for (AnonymousPipe pipe : availablePipes) {
                try {
                    pipe.close();
                } catch (IOException e) {
                    lastException = e;
                }
            }

            for (AnonymousPipe pipe : inUsePipes) {
                try {
                    pipe.close();
                } catch (IOException e) {
                    lastException = e;
                }
            }

            availablePipes.clear();
            inUsePipes.clear();

            if (lastException != null) {
                throw lastException;
            }
        }

        public int getAvailableCount() { return availablePipes.size(); }
        public int getInUseCount() { return inUsePipes.size(); }
    }

    /**
     * 프로세스 간 파이프 브릿지
     */
    public static class ProcessPipeBridge {
        private final Process sourceProcess;
        private final Process targetProcess;
        private Thread bridgeThread;
        private final AtomicBoolean running;

        public ProcessPipeBridge(Process sourceProcess, Process targetProcess) {
            this.sourceProcess = sourceProcess;
            this.targetProcess = targetProcess;
            this.running = new AtomicBoolean(false);
        }

        public void start() {
            if (running.get()) {
                throw new IllegalStateException("Bridge already running");
            }

            running.set(true);

            bridgeThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(sourceProcess.getInputStream()));
                     BufferedWriter writer = new BufferedWriter(
                             new OutputStreamWriter(targetProcess.getOutputStream()))) {

                    String line;
                    while (running.get() && (line = reader.readLine()) != null) {
                        writer.write(line);
                        writer.newLine();
                        writer.flush();
                    }

                } catch (IOException e) {
                    System.err.println("Bridge error: " + e.getMessage());
                }
            });

            bridgeThread.setName("ProcessPipeBridge");
            bridgeThread.start();
        }

        public void stop() {
            running.set(false);

            if (bridgeThread != null) {
                bridgeThread.interrupt();
                try {
                    bridgeThread.join(1000);
                } catch (InterruptedException ignored) {}
            }
        }

        public boolean isRunning() { return running.get(); }
    }
}