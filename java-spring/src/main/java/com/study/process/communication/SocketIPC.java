package com.study.process.communication;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 소켓 기반 프로세스 간 통신 구현
 * TCP/UDP 소켓을 사용하여 프로세스 간 메시지 교환
 */
public class SocketIPC {

    /**
     * 메시지 클래스
     */
    public static class Message implements Serializable {
        private static final long serialVersionUID = 1L;

        private final String id;
        private final String type;
        private final String content;
        private final Map<String, String> headers;
        private final long timestamp;

        public Message(String type, String content) {
            this.id = UUID.randomUUID().toString();
            this.type = type;
            this.content = content;
            this.headers = new HashMap<>();
            this.timestamp = System.currentTimeMillis();
        }

        public Message(String type, String content, Map<String, String> headers) {
            this(type, content);
            this.headers.putAll(headers);
        }

        public String getId() { return id; }
        public String getType() { return type; }
        public String getContent() { return content; }
        public Map<String, String> getHeaders() { return headers; }
        public long getTimestamp() { return timestamp; }

        @Override
        public String toString() {
            return String.format("Message[id=%s, type=%s, content=%s]",
                    id, type, content);
        }
    }

    /**
     * TCP 서버 구현
     */
    public static class TCPServer {
        private final int port;
        private ServerSocket serverSocket;
        private final ExecutorService executor;
        private final BlockingQueue<Message> receivedMessages;
        private final Map<String, Socket> clients;
        private final AtomicBoolean running;
        private Thread acceptThread;

        public TCPServer(int port) {
            this.port = port;
            this.executor = Executors.newCachedThreadPool();
            this.receivedMessages = new LinkedBlockingQueue<>();
            this.clients = new ConcurrentHashMap<>();
            this.running = new AtomicBoolean(false);
        }

        public void start() throws IOException {
            if (running.get()) {
                throw new IllegalStateException("Server already running");
            }

            serverSocket = new ServerSocket(port);
            running.set(true);

            // Accept 스레드 시작
            acceptThread = new Thread(this::acceptConnections);
            acceptThread.setName("TCP-Server-Accept-" + port);
            acceptThread.start();

            System.out.println("TCP Server started on port " + port);
        }

        private void acceptConnections() {
            while (running.get()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    String clientId = clientSocket.getInetAddress().getHostAddress() +
                            ":" + clientSocket.getPort();
                    clients.put(clientId, clientSocket);

                    // 클라이언트 처리 스레드
                    executor.submit(() -> handleClient(clientId, clientSocket));

                    System.out.println("Client connected: " + clientId);

                } catch (IOException e) {
                    if (running.get()) {
                        System.err.println("Accept error: " + e.getMessage());
                    }
                }
            }
        }

        private void handleClient(String clientId, Socket socket) {
            try (ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {

                while (running.get() && !socket.isClosed()) {
                    try {
                        Message message = (Message) in.readObject();
                        receivedMessages.offer(message);

                        // Echo response
                        Message response = new Message(
                                "RESPONSE",
                                "Received: " + message.getContent()
                        );
                        out.writeObject(response);
                        out.flush();

                    } catch (EOFException e) {
                        break; // 클라이언트 연결 종료
                    } catch (ClassNotFoundException e) {
                        System.err.println("Invalid message from " + clientId);
                    }
                }
            } catch (IOException e) {
                System.err.println("Client error " + clientId + ": " + e.getMessage());
            } finally {
                clients.remove(clientId);
                try {
                    socket.close();
                } catch (IOException ignored) {}
                System.out.println("Client disconnected: " + clientId);
            }
        }

        public void broadcast(Message message) {
            clients.values().parallelStream().forEach(socket -> {
                try {
                    ObjectOutputStream out = new ObjectOutputStream(
                            socket.getOutputStream()
                    );
                    out.writeObject(message);
                    out.flush();
                } catch (IOException e) {
                    System.err.println("Broadcast error: " + e.getMessage());
                }
            });
        }

        public Message receive(long timeout, TimeUnit unit)
                throws InterruptedException {
            return receivedMessages.poll(timeout, unit);
        }

        public void stop() {
            running.set(false);

            // 모든 클라이언트 연결 종료
            clients.values().forEach(socket -> {
                try {
                    socket.close();
                } catch (IOException ignored) {}
            });

            try {
                if (serverSocket != null && !serverSocket.isClosed()) {
                    serverSocket.close();
                }
            } catch (IOException ignored) {}

            executor.shutdown();

            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }

            System.out.println("TCP Server stopped");
        }

        public boolean isRunning() { return running.get(); }
        public int getClientCount() { return clients.size(); }
        public BlockingQueue<Message> getReceivedMessages() { return receivedMessages; }
    }

    /**
     * TCP 클라이언트 구현
     */
    public static class TCPClient {
        private final String host;
        private final int port;
        private Socket socket;
        private ObjectOutputStream out;
        private ObjectInputStream in;
        private final BlockingQueue<Message> receivedMessages;
        private Thread receiveThread;
        private final AtomicBoolean connected;

        public TCPClient(String host, int port) {
            this.host = host;
            this.port = port;
            this.receivedMessages = new LinkedBlockingQueue<>();
            this.connected = new AtomicBoolean(false);
        }

        public void connect() throws IOException {
            if (connected.get()) {
                throw new IllegalStateException("Already connected");
            }

            socket = new Socket(host, port);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            connected.set(true);

            // 수신 스레드 시작
            receiveThread = new Thread(this::receiveMessages);
            receiveThread.setName("TCP-Client-Receive");
            receiveThread.start();

            System.out.println("Connected to " + host + ":" + port);
        }

        private void receiveMessages() {
            while (connected.get()) {
                try {
                    Message message = (Message) in.readObject();
                    receivedMessages.offer(message);
                } catch (EOFException e) {
                    break; // 서버 연결 종료
                } catch (Exception e) {
                    if (connected.get()) {
                        System.err.println("Receive error: " + e.getMessage());
                    }
                    break;
                }
            }
            connected.set(false);
        }

        public void send(Message message) throws IOException {
            if (!connected.get()) {
                throw new IOException("Not connected");
            }

            out.writeObject(message);
            out.flush();
        }

        public Message receive(long timeout, TimeUnit unit)
                throws InterruptedException {
            return receivedMessages.poll(timeout, unit);
        }

        public void disconnect() {
            connected.set(false);

            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException ignored) {}

            if (receiveThread != null) {
                try {
                    receiveThread.join(1000);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }

            System.out.println("Disconnected from " + host + ":" + port);
        }

        public boolean isConnected() { return connected.get(); }
    }

    /**
     * UDP 통신 구현
     */
    public static class UDPCommunicator {
        private final int port;
        private DatagramSocket socket;
        private final BlockingQueue<DatagramMessage> receivedMessages;
        private Thread receiveThread;
        private final AtomicBoolean running;
        private final int bufferSize = 65536; // 64KB

        public static class DatagramMessage {
            private final String content;
            private final InetAddress address;
            private final int port;

            public DatagramMessage(String content, InetAddress address, int port) {
                this.content = content;
                this.address = address;
                this.port = port;
            }

            public String getContent() { return content; }
            public InetAddress getAddress() { return address; }
            public int getPort() { return port; }
        }

        public UDPCommunicator(int port) {
            this.port = port;
            this.receivedMessages = new LinkedBlockingQueue<>();
            this.running = new AtomicBoolean(false);
        }

        public void start() throws SocketException {
            if (running.get()) {
                throw new IllegalStateException("Already running");
            }

            socket = new DatagramSocket(port);
            running.set(true);

            // 수신 스레드 시작
            receiveThread = new Thread(this::receiveLoop);
            receiveThread.setName("UDP-Receiver-" + port);
            receiveThread.start();

            System.out.println("UDP Communicator started on port " + port);
        }

        private void receiveLoop() {
            byte[] buffer = new byte[bufferSize];

            while (running.get()) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);

                    String content = new String(
                            packet.getData(),
                            0,
                            packet.getLength(),
                            StandardCharsets.UTF_8
                    );

                    DatagramMessage message = new DatagramMessage(
                            content,
                            packet.getAddress(),
                            packet.getPort()
                    );

                    receivedMessages.offer(message);

                } catch (IOException e) {
                    if (running.get()) {
                        System.err.println("UDP receive error: " + e.getMessage());
                    }
                }
            }
        }

        public void send(String content, String host, int targetPort)
                throws IOException {
            if (!running.get()) {
                throw new IOException("Not running");
            }

            byte[] data = content.getBytes(StandardCharsets.UTF_8);
            InetAddress address = InetAddress.getByName(host);
            DatagramPacket packet = new DatagramPacket(
                    data,
                    data.length,
                    address,
                    targetPort
            );

            socket.send(packet);
        }

        public void broadcast(String content, int targetPort) throws IOException {
            send(content, "255.255.255.255", targetPort);
        }

        public DatagramMessage receive(long timeout, TimeUnit unit)
                throws InterruptedException {
            return receivedMessages.poll(timeout, unit);
        }

        public void stop() {
            running.set(false);

            if (socket != null && !socket.isClosed()) {
                socket.close();
            }

            if (receiveThread != null) {
                try {
                    receiveThread.join(1000);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }

            System.out.println("UDP Communicator stopped");
        }

        public boolean isRunning() { return running.get(); }
        public int getPort() { return port; }
    }

    /**
     * 멀티캐스트 통신 구현
     */
    public static class MulticastCommunicator {
        private final String groupAddress;
        private final int port;
        private MulticastSocket socket;
        private InetAddress group;
        private final BlockingQueue<String> receivedMessages;
        private Thread receiveThread;
        private final AtomicBoolean running;

        public MulticastCommunicator(String groupAddress, int port) {
            this.groupAddress = groupAddress;
            this.port = port;
            this.receivedMessages = new LinkedBlockingQueue<>();
            this.running = new AtomicBoolean(false);
        }

        public void start() throws IOException {
            if (running.get()) {
                throw new IllegalStateException("Already running");
            }

            socket = new MulticastSocket(port);
            group = InetAddress.getByName(groupAddress);

            // 멀티캐스트 그룹 참여
            socket.joinGroup(new InetSocketAddress(group, port),
                    NetworkInterface.getByIndex(0));

            running.set(true);

            // 수신 스레드 시작
            receiveThread = new Thread(this::receiveLoop);
            receiveThread.setName("Multicast-Receiver");
            receiveThread.start();

            System.out.println("Multicast communicator joined " + groupAddress + ":" + port);
        }

        private void receiveLoop() {
            byte[] buffer = new byte[1024];

            while (running.get()) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);

                    String message = new String(
                            packet.getData(),
                            0,
                            packet.getLength(),
                            StandardCharsets.UTF_8
                    );

                    receivedMessages.offer(message);

                } catch (IOException e) {
                    if (running.get()) {
                        System.err.println("Multicast receive error: " + e.getMessage());
                    }
                }
            }
        }

        public void send(String message) throws IOException {
            if (!running.get()) {
                throw new IOException("Not running");
            }

            byte[] data = message.getBytes(StandardCharsets.UTF_8);
            DatagramPacket packet = new DatagramPacket(data, data.length, group, port);
            socket.send(packet);
        }

        public String receive(long timeout, TimeUnit unit)
                throws InterruptedException {
            return receivedMessages.poll(timeout, unit);
        }

        public void stop() throws IOException {
            running.set(false);

            if (socket != null) {
                socket.leaveGroup(new InetSocketAddress(group, port),
                        NetworkInterface.getByIndex(0));
                socket.close();
            }

            if (receiveThread != null) {
                try {
                    receiveThread.join(1000);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }

            System.out.println("Left multicast group");
        }

        public boolean isRunning() { return running.get(); }
    }
}