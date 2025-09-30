package com.study.process.communication;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;

/**
 * 메모리 맵 파일을 통한 공유 메모리 IPC 구현
 * 프로세스 간 고속 데이터 공유를 위한 메커니즘
 */
public class SharedMemoryIPC {

    /**
     * 공유 메모리 세그먼트
     */
    public static class SharedMemorySegment {
        private final String segmentName;
        private final Path filePath;
        private RandomAccessFile file;
        private FileChannel channel;
        private MappedByteBuffer buffer;
        private final int size;
        private final boolean isCreator;
        private final Lock lock;

        // 헤더 구조 (16 bytes)
        private static final int HEADER_SIZE = 16;
        private static final int VERSION_OFFSET = 0;
        private static final int FLAGS_OFFSET = 4;
        private static final int LENGTH_OFFSET = 8;
        private static final int CHECKSUM_OFFSET = 12;

        public SharedMemorySegment(String segmentName, int size, boolean isCreator)
                throws IOException {
            this.segmentName = segmentName;
            this.size = size;
            this.isCreator = isCreator;
            this.lock = new ReentrantLock();

            // 공유 메모리 파일 경로 설정
            String tempDir = System.getProperty("java.io.tmpdir");
            this.filePath = Paths.get(tempDir, "shm_" + segmentName + ".dat");

            if (isCreator) {
                createSegment();
            } else {
                attachSegment();
            }
        }

        private void createSegment() throws IOException {
            // 기존 파일 삭제
            Files.deleteIfExists(filePath);

            // 새 파일 생성
            file = new RandomAccessFile(filePath.toFile(), "rw");
            file.setLength(size);

            channel = file.getChannel();
            buffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, size);

            // 헤더 초기화
            buffer.putInt(VERSION_OFFSET, 1); // 버전
            buffer.putInt(FLAGS_OFFSET, 0);   // 플래그
            buffer.putInt(LENGTH_OFFSET, 0);  // 데이터 길이
            buffer.putInt(CHECKSUM_OFFSET, 0); // 체크섬

            System.out.println("Shared memory segment created: " + segmentName +
                    " (size: " + size + " bytes)");
        }

        private void attachSegment() throws IOException {
            if (!Files.exists(filePath)) {
                throw new IOException("Shared memory segment not found: " + segmentName);
            }

            file = new RandomAccessFile(filePath.toFile(), "rw");
            channel = file.getChannel();
            buffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, file.length());

            System.out.println("Attached to shared memory segment: " + segmentName);
        }

        public void write(byte[] data, int offset) {
            lock.lock();
            try {
                if (offset + data.length > size - HEADER_SIZE) {
                    throw new IllegalArgumentException("Data exceeds segment size");
                }

                buffer.position(HEADER_SIZE + offset);
                buffer.put(data);

                // 데이터 길이 업데이트
                buffer.putInt(LENGTH_OFFSET, data.length);

                // 체크섬 계산 및 저장
                int checksum = calculateChecksum(data);
                buffer.putInt(CHECKSUM_OFFSET, checksum);

                // 강제 동기화
                buffer.force();
            } finally {
                lock.unlock();
            }
        }

        public byte[] read(int offset, int length) {
            lock.lock();
            try {
                if (offset + length > size - HEADER_SIZE) {
                    throw new IllegalArgumentException("Read exceeds segment size");
                }

                byte[] data = new byte[length];
                buffer.position(HEADER_SIZE + offset);
                buffer.get(data);

                return data;
            } finally {
                lock.unlock();
            }
        }

        public void writeString(String data) {
            write(data.getBytes(StandardCharsets.UTF_8), 0);
        }

        public String readString() {
            int length = buffer.getInt(LENGTH_OFFSET);
            if (length <= 0) {
                return "";
            }

            byte[] data = read(0, length);
            return new String(data, StandardCharsets.UTF_8);
        }

        private int calculateChecksum(byte[] data) {
            int checksum = 0;
            for (byte b : data) {
                checksum = (checksum << 1) ^ b;
            }
            return checksum;
        }

        public void close() throws IOException {
            // 버퍼 참조 제거
            buffer = null;

            if (channel != null) {
                channel.close();
            }
            if (file != null) {
                file.close();
            }

            // 필요시 명시적 GC 힌트 (권장하지 않지만 테스트 목적으로는 OK)
            // System.gc();

            if (isCreator) {
                Files.deleteIfExists(filePath);
            }
        }

        public String getSegmentName() { return segmentName; }
        public int getSize() { return size; }
        public boolean isCreator() { return isCreator; }
    }

    /**
     * 공유 메모리 큐 구현
     */
    public static class SharedMemoryQueue {
        private final SharedMemorySegment segment;
        private final int capacity;
        private final int itemSize;
        private final AtomicInteger head;
        private final AtomicInteger tail;
        private final AtomicInteger count;
        private final Semaphore notEmpty;
        private final Semaphore notFull;

        // 큐 메타데이터 오프셋
        private static final int META_SIZE = 16;
        private static final int HEAD_OFFSET = 0;
        private static final int TAIL_OFFSET = 4;
        private static final int COUNT_OFFSET = 8;

        public SharedMemoryQueue(String name, int capacity, int itemSize, boolean create)
                throws IOException {
            this.capacity = capacity;
            this.itemSize = itemSize;

            int totalSize = META_SIZE + (capacity * itemSize);
            this.segment = new SharedMemorySegment(name, totalSize, create);

            if (create) {
                // 메타데이터 초기화
                segment.buffer.putInt(HEAD_OFFSET, 0);
                segment.buffer.putInt(TAIL_OFFSET, 0);
                segment.buffer.putInt(COUNT_OFFSET, 0);
            }

            this.head = new AtomicInteger(segment.buffer.getInt(HEAD_OFFSET));
            this.tail = new AtomicInteger(segment.buffer.getInt(TAIL_OFFSET));
            this.count = new AtomicInteger(segment.buffer.getInt(COUNT_OFFSET));

            this.notEmpty = new Semaphore(0);
            this.notFull = new Semaphore(capacity);
        }

        public void enqueue(byte[] item) throws InterruptedException {
            if (item.length > itemSize) {
                throw new IllegalArgumentException("Item exceeds maximum size");
            }

            notFull.acquire();

            synchronized (segment) {
                int tailPos = tail.get();
                int offset = META_SIZE + (tailPos * itemSize);

                // 아이템 쓰기
                segment.buffer.position(offset);
                segment.buffer.put(item);

                // 패딩 (아이템 크기가 작은 경우)
                if (item.length < itemSize) {
                    byte[] padding = new byte[itemSize - item.length];
                    segment.buffer.put(padding);
                }

                // 메타데이터 업데이트
                tail.set((tailPos + 1) % capacity);
                count.incrementAndGet();

                segment.buffer.putInt(TAIL_OFFSET, tail.get());
                segment.buffer.putInt(COUNT_OFFSET, count.get());
                segment.buffer.force();
            }

            notEmpty.release();
        }

        public byte[] dequeue() throws InterruptedException {
            notEmpty.acquire();

            byte[] item;
            synchronized (segment) {
                int headPos = head.get();
                int offset = META_SIZE + (headPos * itemSize);

                // 아이템 읽기
                item = new byte[itemSize];
                segment.buffer.position(offset);
                segment.buffer.get(item);

                // 메타데이터 업데이트
                head.set((headPos + 1) % capacity);
                count.decrementAndGet();

                segment.buffer.putInt(HEAD_OFFSET, head.get());
                segment.buffer.putInt(COUNT_OFFSET, count.get());
                segment.buffer.force();
            }

            notFull.release();
            return item;
        }

        public boolean isEmpty() { return count.get() == 0; }
        public boolean isFull() { return count.get() == capacity; }
        public int size() { return count.get(); }

        public void close() throws IOException {
            segment.close();
        }
    }

    /**
     * 공유 메모리 맵 구현
     */
    public static class SharedMemoryMap {
        private final SharedMemorySegment segment;
        private final Map<String, Integer> indexMap;
        private final int maxEntries;
        private final int keySize;
        private final int valueSize;
        private final ReadWriteLock rwLock;

        private static final int ENTRY_COUNT_OFFSET = 0;
        private static final int METADATA_SIZE = 8;

        public SharedMemoryMap(String name, int maxEntries, int keySize, int valueSize,
                               boolean create) throws IOException {
            this.maxEntries = maxEntries;
            this.keySize = keySize;
            this.valueSize = valueSize;
            this.indexMap = new ConcurrentHashMap<>();
            this.rwLock = new ReentrantReadWriteLock();

            int entrySize = keySize + valueSize + 1; // +1 for valid flag
            int totalSize = METADATA_SIZE + (maxEntries * entrySize);

            this.segment = new SharedMemorySegment(name, totalSize, create);

            if (create) {
                segment.buffer.putInt(ENTRY_COUNT_OFFSET, 0);
            } else {
                loadIndex();
            }
        }

        private void loadIndex() {
            int entryCount = segment.buffer.getInt(ENTRY_COUNT_OFFSET);
            int entrySize = keySize + valueSize + 1;

            for (int i = 0; i < entryCount; i++) {
                int offset = METADATA_SIZE + (i * entrySize);
                segment.buffer.position(offset);

                byte valid = segment.buffer.get();
                if (valid == 1) {
                    byte[] keyBytes = new byte[keySize];
                    segment.buffer.get(keyBytes);

                    String key = new String(keyBytes, StandardCharsets.UTF_8).trim();
                    indexMap.put(key, i);
                }
            }
        }

        public void put(String key, byte[] value) {
            if (key.getBytes(StandardCharsets.UTF_8).length > keySize) {
                throw new IllegalArgumentException("Key exceeds maximum size");
            }
            if (value.length > valueSize) {
                throw new IllegalArgumentException("Value exceeds maximum size");
            }

            rwLock.writeLock().lock();
            try {
                Integer index = indexMap.get(key);

                if (index == null) {
                    // 새 엔트리 할당
                    int entryCount = segment.buffer.getInt(ENTRY_COUNT_OFFSET);
                    if (entryCount >= maxEntries) {
                        throw new IllegalStateException("Map is full");
                    }

                    index = entryCount;
                    indexMap.put(key, index);

                    segment.buffer.putInt(ENTRY_COUNT_OFFSET, entryCount + 1);
                }

                // 엔트리 쓰기
                int entrySize = keySize + valueSize + 1;
                int offset = METADATA_SIZE + (index * entrySize);

                segment.buffer.position(offset);
                segment.buffer.put((byte) 1); // valid flag

                // 키 쓰기 (패딩 포함)
                byte[] keyBytes = Arrays.copyOf(
                        key.getBytes(StandardCharsets.UTF_8),
                        keySize
                );
                segment.buffer.put(keyBytes);

                // 값 쓰기 (패딩 포함)
                byte[] valueBytes = Arrays.copyOf(value, valueSize);
                segment.buffer.put(valueBytes);

                segment.buffer.force();

            } finally {
                rwLock.writeLock().unlock();
            }
        }

        public byte[] get(String key) {
            rwLock.readLock().lock();
            try {
                Integer index = indexMap.get(key);
                if (index == null) {
                    return null;
                }

                int entrySize = keySize + valueSize + 1;
                int offset = METADATA_SIZE + (index * entrySize);

                segment.buffer.position(offset);
                byte valid = segment.buffer.get();

                if (valid != 1) {
                    return null;
                }

                // 키 건너뛰기
                segment.buffer.position(segment.buffer.position() + keySize);

                // 값 읽기
                byte[] value = new byte[valueSize];
                segment.buffer.get(value);

                return value;

            } finally {
                rwLock.readLock().unlock();
            }
        }

        public void remove(String key) {
            rwLock.writeLock().lock();
            try {
                Integer index = indexMap.remove(key);
                if (index != null) {
                    int entrySize = keySize + valueSize + 1;
                    int offset = METADATA_SIZE + (index * entrySize);

                    segment.buffer.position(offset);
                    segment.buffer.put((byte) 0); // invalid flag
                    segment.buffer.force();
                }
            } finally {
                rwLock.writeLock().unlock();
            }
        }

        public boolean containsKey(String key) {
            rwLock.readLock().lock();
            try {
                return indexMap.containsKey(key);
            } finally {
                rwLock.readLock().unlock();
            }
        }

        public int size() {
            return indexMap.size();
        }

        public void close() throws IOException {
            segment.close();
        }
    }
}