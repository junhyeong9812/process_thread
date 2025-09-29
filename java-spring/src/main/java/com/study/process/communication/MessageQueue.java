package com.study.process.communication;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;

/**
 * 메시지 큐 시뮬레이션
 * 프로세스 간 비동기 메시지 전달을 위한 큐 구현
 */
public class MessageQueue {

    /**
     * 메시지 엔벨로프
     */
    public static class Envelope implements Serializable {
        private static final long serialVersionUID = 1L;

        private final String messageId;
        private final String correlationId;
        private final String sender;
        private final String recipient;
        private final String topic;
        private final int priority;
        private final long timestamp;
        private final long expiration;
        private final Map<String, Object> headers;
        private final Object payload;

        public static class Builder {
            private String correlationId;
            private String sender;
            private String recipient;
            private String topic = "default";
            private int priority = 5;
            private long expiration = 0;
            private Map<String, Object> headers = new HashMap<>();
            private Object payload;

            public Builder correlationId(String id) {
                this.correlationId = id;
                return this;
            }

            public Builder sender(String sender) {
                this.sender = sender;
                return this;
            }

            public Builder recipient(String recipient) {
                this.recipient = recipient;
                return this;
            }

            public Builder topic(String topic) {
                this.topic = topic;
                return this;
            }

            public Builder priority(int priority) {
                this.priority = Math.max(1, Math.min(10, priority));
                return this;
            }

            public Builder expiration(long ttlMillis) {
                this.expiration = System.currentTimeMillis() + ttlMillis;
                return this;
            }

            public Builder header(String key, Object value) {
                this.headers.put(key, value);
                return this;
            }

            public Builder payload(Object payload) {
                this.payload = payload;
                return this;
            }

            public Envelope build() {
                return new Envelope(this);
            }
        }

        private Envelope(Builder builder) {
            this.messageId = UUID.randomUUID().toString();
            this.correlationId = builder.correlationId;
            this.sender = builder.sender;
            this.recipient = builder.recipient;
            this.topic = builder.topic;
            this.priority = builder.priority;
            this.timestamp = System.currentTimeMillis();
            this.expiration = builder.expiration;
            this.headers = new HashMap<>(builder.headers);
            this.payload = builder.payload;
        }

        public boolean isExpired() {
            return expiration > 0 && System.currentTimeMillis() > expiration;
        }

        // Getters
        public String getMessageId() { return messageId; }
        public String getCorrelationId() { return correlationId; }
        public String getSender() { return sender; }
        public String getRecipient() { return recipient; }
        public String getTopic() { return topic; }
        public int getPriority() { return priority; }
        public long getTimestamp() { return timestamp; }
        public long getExpiration() { return expiration; }
        public Map<String, Object> getHeaders() { return new HashMap<>(headers); }
        public Object getPayload() { return payload; }

        @Override
        public String toString() {
            return String.format("Envelope[id=%s, topic=%s, priority=%d, sender=%s]",
                    messageId, topic, priority, sender);
        }
    }

    /**
     * 단순 메시지 큐 구현
     */
    public static class SimpleQueue {
        private final String queueName;
        private final BlockingQueue<Envelope> queue;
        private final AtomicLong messageCount;
        private final AtomicLong deliveredCount;
        private final AtomicLong expiredCount;

        public SimpleQueue(String queueName, int capacity) {
            this.queueName = queueName;
            this.queue = new LinkedBlockingQueue<>(capacity);
            this.messageCount = new AtomicLong(0);
            this.deliveredCount = new AtomicLong(0);
            this.expiredCount = new AtomicLong(0);
        }

        public boolean send(Envelope message) throws InterruptedException {
            if (message.isExpired()) {
                expiredCount.incrementAndGet();
                return false;
            }

            boolean added = queue.offer(message, 100, TimeUnit.MILLISECONDS);
            if (added) {
                messageCount.incrementAndGet();
            }
            return added;
        }

        public boolean sendNonBlocking(Envelope message) {
            if (message.isExpired()) {
                expiredCount.incrementAndGet();
                return false;
            }

            boolean added = queue.offer(message);
            if (added) {
                messageCount.incrementAndGet();
            }
            return added;
        }

        public Envelope receive(long timeout, TimeUnit unit) throws InterruptedException {
            Envelope message = queue.poll(timeout, unit);

            while (message != null && message.isExpired()) {
                expiredCount.incrementAndGet();
                message = queue.poll(timeout, unit);
            }

            if (message != null) {
                deliveredCount.incrementAndGet();
            }

            return message;
        }

        public Envelope peek() {
            Envelope message = queue.peek();

            while (message != null && message.isExpired()) {
                queue.poll();
                expiredCount.incrementAndGet();
                message = queue.peek();
            }

            return message;
        }

        public void clear() {
            queue.clear();
        }

        public int size() { return queue.size(); }
        public boolean isEmpty() { return queue.isEmpty(); }
        public long getMessageCount() { return messageCount.get(); }
        public long getDeliveredCount() { return deliveredCount.get(); }
        public long getExpiredCount() { return expiredCount.get(); }
        public String getQueueName() { return queueName; }
    }

    /**
     * 우선순위 메시지 큐
     */
    public static class PriorityQueue {
        private final String queueName;
        private final PriorityBlockingQueue<Envelope> queue;
        private final AtomicLong messageCount;
        private final AtomicLong deliveredCount;

        public PriorityQueue(String queueName) {
            this.queueName = queueName;
            this.queue = new PriorityBlockingQueue<>(100,
                    Comparator.comparingInt(Envelope::getPriority).reversed()
                            .thenComparing(Envelope::getTimestamp));
            this.messageCount = new AtomicLong(0);
            this.deliveredCount = new AtomicLong(0);
        }

        public void send(Envelope message) {
            if (!message.isExpired()) {
                queue.offer(message);
                messageCount.incrementAndGet();
            }
        }

        public Envelope receive(long timeout, TimeUnit unit) throws InterruptedException {
            Envelope message = queue.poll(timeout, unit);

            while (message != null && message.isExpired()) {
                message = queue.poll(timeout, unit);
            }

            if (message != null) {
                deliveredCount.incrementAndGet();
            }

            return message;
        }

        public int size() { return queue.size(); }
        public String getQueueName() { return queueName; }
        public long getMessageCount() { return messageCount.get(); }
        public long getDeliveredCount() { return deliveredCount.get(); }
    }

    /**
     * 토픽 기반 메시지 큐 (Pub/Sub)
     */
    public static class TopicQueue {
        private final String topicName;
        private final Map<String, BlockingQueue<Envelope>> subscriberQueues;
        private final Set<String> subscribers;
        private final ReadWriteLock lock;
        private final AtomicLong publishedCount;
        private final Map<String, AtomicLong> deliveredCounts;

        public TopicQueue(String topicName) {
            this.topicName = topicName;
            this.subscriberQueues = new ConcurrentHashMap<>();
            this.subscribers = ConcurrentHashMap.newKeySet();
            this.lock = new ReentrantReadWriteLock();
            this.publishedCount = new AtomicLong(0);
            this.deliveredCounts = new ConcurrentHashMap<>();
        }

        public void subscribe(String subscriberId) {
            lock.writeLock().lock();
            try {
                if (subscribers.add(subscriberId)) {
                    subscriberQueues.put(subscriberId,
                            new LinkedBlockingQueue<>(1000));
                    deliveredCounts.put(subscriberId, new AtomicLong(0));
                    System.out.printf("Subscriber '%s' joined topic '%s'\n",
                            subscriberId, topicName);
                }
            } finally {
                lock.writeLock().unlock();
            }
        }

        public void unsubscribe(String subscriberId) {
            lock.writeLock().lock();
            try {
                subscribers.remove(subscriberId);
                subscriberQueues.remove(subscriberId);
                deliveredCounts.remove(subscriberId);
                System.out.printf("Subscriber '%s' left topic '%s'\n",
                        subscriberId, topicName);
            } finally {
                lock.writeLock().unlock();
            }
        }

        public void publish(Envelope message) {
            if (message.isExpired()) {
                return;
            }

            lock.readLock().lock();
            try {
                for (BlockingQueue<Envelope> queue : subscriberQueues.values()) {
                    queue.offer(message); // Non-blocking
                }
                publishedCount.incrementAndGet();
            } finally {
                lock.readLock().unlock();
            }
        }

        public Envelope receive(String subscriberId, long timeout, TimeUnit unit)
                throws InterruptedException {
            BlockingQueue<Envelope> queue = subscriberQueues.get(subscriberId);
            if (queue == null) {
                return null;
            }

            Envelope message = queue.poll(timeout, unit);

            while (message != null && message.isExpired()) {
                message = queue.poll(timeout, unit);
            }

            if (message != null) {
                deliveredCounts.get(subscriberId).incrementAndGet();
            }

            return message;
        }

        public int getSubscriberCount() { return subscribers.size(); }
        public String getTopicName() { return topicName; }
        public long getPublishedCount() { return publishedCount.get(); }
        public long getDeliveredCount(String subscriberId) {
            AtomicLong count = deliveredCounts.get(subscriberId);
            return count != null ? count.get() : 0;
        }
    }

    /**
     * 메시지 큐 매니저
     */
    public static class QueueManager {
        private final Map<String, SimpleQueue> simpleQueues;
        private final Map<String, PriorityQueue> priorityQueues;
        private final Map<String, TopicQueue> topicQueues;
        private final ScheduledExecutorService cleanupExecutor;
        private volatile boolean running;

        public QueueManager() {
            this.simpleQueues = new ConcurrentHashMap<>();
            this.priorityQueues = new ConcurrentHashMap<>();
            this.topicQueues = new ConcurrentHashMap<>();
            this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor();
            this.running = true;

            // 만료된 메시지 정리 (30초마다)
            cleanupExecutor.scheduleAtFixedRate(
                    this::cleanupExpiredMessages,
                    0, 30, TimeUnit.SECONDS
            );
        }

        public SimpleQueue createSimpleQueue(String name, int capacity) {
            return simpleQueues.computeIfAbsent(name,
                    k -> {
                        System.out.printf("Created simple queue: %s (capacity=%d)\n", k, capacity);
                        return new SimpleQueue(k, capacity);
                    });
        }

        public PriorityQueue createPriorityQueue(String name) {
            return priorityQueues.computeIfAbsent(name, k -> {
                System.out.printf("Created priority queue: %s\n", k);
                return new PriorityQueue(k);
            });
        }

        public TopicQueue createTopicQueue(String name) {
            return topicQueues.computeIfAbsent(name, k -> {
                System.out.printf("Created topic queue: %s\n", k);
                return new TopicQueue(k);
            });
        }

        public SimpleQueue getSimpleQueue(String name) {
            return simpleQueues.get(name);
        }

        public PriorityQueue getPriorityQueue(String name) {
            return priorityQueues.get(name);
        }

        public TopicQueue getTopicQueue(String name) {
            return topicQueues.get(name);
        }

        public void deleteQueue(String name) {
            simpleQueues.remove(name);
            priorityQueues.remove(name);
            topicQueues.remove(name);
            System.out.printf("Deleted queue: %s\n", name);
        }

        private void cleanupExpiredMessages() {
            if (!running) return;

            // 간단한 큐 정리
            simpleQueues.values().forEach(queue -> {
                while (queue.peek() != null && queue.peek().isExpired()) {
                    try {
                        queue.receive(0, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
        }

        public void shutdown() {
            running = false;
            cleanupExecutor.shutdown();

            try {
                if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    cleanupExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                cleanupExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }

            System.out.println("QueueManager shutdown");
        }

        public QueueStatistics getStatistics() {
            return new QueueStatistics(simpleQueues, priorityQueues, topicQueues);
        }

        public static class QueueStatistics {
            private final int simpleQueueCount;
            private final int priorityQueueCount;
            private final int topicQueueCount;
            private final long totalMessages;
            private final long totalDelivered;

            public QueueStatistics(Map<String, SimpleQueue> simple,
                                   Map<String, PriorityQueue> priority,
                                   Map<String, TopicQueue> topic) {
                this.simpleQueueCount = simple.size();
                this.priorityQueueCount = priority.size();
                this.topicQueueCount = topic.size();

                this.totalMessages = simple.values().stream()
                        .mapToLong(SimpleQueue::size)
                        .sum() + priority.values().stream()
                        .mapToLong(PriorityQueue::size)
                        .sum();

                this.totalDelivered = simple.values().stream()
                        .mapToLong(SimpleQueue::getDeliveredCount)
                        .sum() + priority.values().stream()
                        .mapToLong(PriorityQueue::getDeliveredCount)
                        .sum();
            }

            @Override
            public String toString() {
                return String.format(
                        "Queues[simple=%d, priority=%d, topic=%d], Messages[current=%d, delivered=%d]",
                        simpleQueueCount, priorityQueueCount, topicQueueCount,
                        totalMessages, totalDelivered
                );
            }

            // Getters
            public int getSimpleQueueCount() { return simpleQueueCount; }
            public int getPriorityQueueCount() { return priorityQueueCount; }
            public int getTopicQueueCount() { return topicQueueCount; }
            public long getTotalMessages() { return totalMessages; }
            public long getTotalDelivered() { return totalDelivered; }
        }
    }

    /**
     * 파일 기반 영속적 큐
     */
    public static class PersistentQueue {
        private final String queueName;
        private final File queueFile;
        private final BlockingQueue<Envelope> memoryQueue;
        private final ReentrantLock fileLock;
        private final AtomicBoolean persistenceEnabled;

        public PersistentQueue(String queueName, String basePath) throws IOException {
            this.queueName = queueName;
            this.queueFile = new File(basePath, queueName + ".queue");
            this.memoryQueue = new LinkedBlockingQueue<>();
            this.fileLock = new ReentrantLock();
            this.persistenceEnabled = new AtomicBoolean(true);

            // 디렉토리 생성
            File dir = new File(basePath);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // 기존 메시지 로드
            loadFromFile();
        }

        private void loadFromFile() throws IOException {
            if (!queueFile.exists()) {
                return;
            }

            fileLock.lock();
            try (ObjectInputStream in = new ObjectInputStream(
                    new FileInputStream(queueFile))) {

                int loadedCount = 0;
                while (true) {
                    try {
                        Envelope message = (Envelope) in.readObject();
                        if (!message.isExpired()) {
                            memoryQueue.offer(message);
                            loadedCount++;
                        }
                    } catch (EOFException e) {
                        break;
                    } catch (ClassNotFoundException e) {
                        System.err.println("Failed to deserialize message: " + e);
                    }
                }

                System.out.printf("Loaded %d messages from persistent queue: %s\n",
                        loadedCount, queueName);

            } finally {
                fileLock.unlock();
            }
        }

        public void send(Envelope message) throws IOException, InterruptedException {
            if (message.isExpired()) {
                return;
            }

            memoryQueue.put(message);

            if (persistenceEnabled.get()) {
                persistToFile(message);
            }
        }

        private void persistToFile(Envelope message) throws IOException {
            fileLock.lock();
            try (ObjectOutputStream out = new ObjectOutputStream(
                    new FileOutputStream(queueFile, true))) {
                out.writeObject(message);
            } finally {
                fileLock.unlock();
            }
        }

        public Envelope receive(long timeout, TimeUnit unit) throws InterruptedException {
            Envelope message = memoryQueue.poll(timeout, unit);

            if (message != null && persistenceEnabled.get()) {
                // 받은 메시지 제거하기 위해 전체 재작성 필요
                try {
                    flush();
                } catch (IOException e) {
                    System.err.println("Failed to update persistent queue: " + e);
                }
            }

            return message;
        }

        public void setPersistenceEnabled(boolean enabled) {
            persistenceEnabled.set(enabled);
        }

        public void flush() throws IOException {
            fileLock.lock();
            try (ObjectOutputStream out = new ObjectOutputStream(
                    new FileOutputStream(queueFile))) {

                for (Envelope message : memoryQueue) {
                    out.writeObject(message);
                }

            } finally {
                fileLock.unlock();
            }
        }

        public int size() { return memoryQueue.size(); }
        public String getQueueName() { return queueName; }
    }
}