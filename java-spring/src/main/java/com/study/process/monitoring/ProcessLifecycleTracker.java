package com.study.process.monitoring;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.stream.Collectors;

/**
 * 프로세스 생명주기 추적
 * 프로세스의 생성부터 종료까지 전체 생명주기를 기록하고 분석
 */
public class ProcessLifecycleTracker {

    private final Map<Long, LifecycleRecord> lifecycleRecords;
    private final List<LifecycleEventListener> listeners;
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean tracking;

    /**
     * 생명주기 기록
     */
    public static class LifecycleRecord {
        private final long pid;
        private final String processName;
        private final Instant startTime;
        private Instant endTime;
        private final List<LifecycleEvent> events;
        private LifecycleState state;
        private int restartCount;
        private long totalRuntime;
        private Integer exitCode;
        private final Map<String, Object> metadata;

        public enum LifecycleState {
            CREATED,        // 생성됨
            INITIALIZING,   // 초기화 중
            RUNNING,        // 실행 중
            PAUSED,         // 일시 정지
            STOPPING,       // 종료 중
            TERMINATED,     // 종료됨
            ERROR,          // 오류 발생
            RESTARTED       // 재시작됨
        }

        public LifecycleRecord(long pid, String processName) {
            this.pid = pid;
            this.processName = processName;
            this.startTime = Instant.now();
            this.events = new CopyOnWriteArrayList<>();
            this.state = LifecycleState.CREATED;
            this.restartCount = 0;
            this.metadata = new ConcurrentHashMap<>();

            addEvent(LifecycleEvent.Type.CREATED, "Process created");
        }

        public void addEvent(LifecycleEvent.Type type, String description) {
            LifecycleEvent event = new LifecycleEvent(type, description);
            events.add(event);
            updateState(type);
        }

        public void addEvent(LifecycleEvent.Type type, String description, Map<String, Object> data) {
            LifecycleEvent event = new LifecycleEvent(type, description, data);
            events.add(event);
            updateState(type);
        }

        private void updateState(LifecycleEvent.Type eventType) {
            switch (eventType) {
                case CREATED -> state = LifecycleState.CREATED;
                case STARTED -> state = LifecycleState.RUNNING;
                case PAUSED -> state = LifecycleState.PAUSED;
                case RESUMED -> state = LifecycleState.RUNNING;
                case STOPPED -> state = LifecycleState.STOPPING;
                case TERMINATED -> {
                    state = LifecycleState.TERMINATED;
                    endTime = Instant.now();
                    totalRuntime = Duration.between(startTime, endTime).toMillis();
                }
                case ERROR -> state = LifecycleState.ERROR;
                case RESTARTED -> {
                    state = LifecycleState.RESTARTED;
                    restartCount++;
                }
            }
        }

        // Getters
        public long getPid() { return pid; }
        public String getProcessName() { return processName; }
        public Instant getStartTime() { return startTime; }
        public Instant getEndTime() { return endTime; }
        public LifecycleState getState() { return state; }
        public int getRestartCount() { return restartCount; }
        public long getTotalRuntime() { return totalRuntime; }
        public List<LifecycleEvent> getEvents() { return new ArrayList<>(events); }
        public Integer getExitCode() { return exitCode; }
        public void setExitCode(Integer exitCode) { this.exitCode = exitCode; }

        public boolean isAlive() {
            return state == LifecycleState.RUNNING ||
                    state == LifecycleState.PAUSED ||
                    state == LifecycleState.INITIALIZING;
        }

        public Duration getUptime() {
            Instant end = endTime != null ? endTime : Instant.now();
            return Duration.between(startTime, end);
        }

        public void setMetadata(String key, Object value) {
            metadata.put(key, value);
        }

        public Object getMetadata(String key) {
            return metadata.get(key);
        }

        /**
         * 이벤트 타임라인 생성
         */
        public String getTimeline() {
            StringBuilder timeline = new StringBuilder();
            timeline.append(String.format("=== Process Lifecycle: %s [PID: %d] ===\n",
                    processName, pid));

            for (LifecycleEvent event : events) {
                Duration elapsed = Duration.between(startTime, event.getTimestamp());
                timeline.append(String.format("[+%5ds] %s: %s\n",
                        elapsed.toSeconds(),
                        event.getType(),
                        event.getDescription()));
            }

            timeline.append(String.format("Total Runtime: %d seconds\n",
                    getUptime().toSeconds()));
            timeline.append(String.format("Current State: %s\n", state));

            return timeline.toString();
        }
    }

    /**
     * 생명주기 이벤트
     */
    public static class LifecycleEvent {
        public enum Type {
            CREATED,        // 생성
            STARTED,        // 시작
            PAUSED,         // 일시 정지
            RESUMED,        // 재개
            STOPPED,        // 중지
            TERMINATED,     // 종료
            ERROR,          // 오류
            RESTARTED,      // 재시작
            CPU_SPIKE,      // CPU 급증
            MEMORY_SPIKE,   // 메모리 급증
            UNRESPONSIVE,   // 응답 없음
            RECOVERED,      // 복구됨
            CUSTOM          // 사용자 정의
        }

        private final Type type;
        private final String description;
        private final Instant timestamp;
        private final Map<String, Object> data;

        public LifecycleEvent(Type type, String description) {
            this.type = type;
            this.description = description;
            this.timestamp = Instant.now();
            this.data = new HashMap<>();
        }

        public LifecycleEvent(Type type, String description, Map<String, Object> data) {
            this(type, description);
            this.data.putAll(data);
        }

        // Getters
        public Type getType() { return type; }
        public String getDescription() { return description; }
        public Instant getTimestamp() { return timestamp; }
        public Map<String, Object> getData() { return new HashMap<>(data); }
    }

    /**
     * 생명주기 이벤트 리스너
     */
    public interface LifecycleEventListener {
        void onProcessCreated(LifecycleRecord record);
        void onProcessStarted(LifecycleRecord record);
        void onProcessTerminated(LifecycleRecord record);
        void onProcessRestarted(LifecycleRecord record);
        void onProcessError(LifecycleRecord record, String error);
        void onLifecycleEvent(LifecycleRecord record, LifecycleEvent event);
    }

    /**
     * 생성자
     */
    public ProcessLifecycleTracker() {
        this.lifecycleRecords = new ConcurrentHashMap<>();
        this.listeners = new CopyOnWriteArrayList<>();
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.tracking = new AtomicBoolean(false);
    }

    /**
     * 추적 시작
     */
    public void startTracking() {
        if (tracking.getAndSet(true)) {
            return;
        }

        // 주기적으로 프로세스 상태 확인 (1초마다)
        scheduler.scheduleAtFixedRate(this::checkProcesses, 0, 1, TimeUnit.SECONDS);

        System.out.println("ProcessLifecycleTracker started");
    }

    /**
     * 추적 중지
     */
    public void stopTracking() {
        if (!tracking.getAndSet(false)) {
            return;
        }

        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }

        System.out.println("ProcessLifecycleTracker stopped");
    }

    /**
     * 프로세스 추적 시작
     */
    public LifecycleRecord trackProcess(Process process, String name) {
        long pid = process.pid();
        LifecycleRecord record = new LifecycleRecord(pid, name);
        lifecycleRecords.put(pid, record);

        // 프로세스 시작 이벤트
        record.addEvent(LifecycleEvent.Type.STARTED, "Process started");
        notifyProcessCreated(record);
        notifyProcessStarted(record);

        // 프로세스 종료 감지
        process.onExit().thenAccept(p -> {
            int exitCode = p.exitValue();
            record.setExitCode(exitCode);

            String description = String.format("Process terminated with exit code: %d", exitCode);
            record.addEvent(LifecycleEvent.Type.TERMINATED, description);

            notifyProcessTerminated(record);
        });

        System.out.printf("Tracking lifecycle: [%d] %s\n", pid, name);

        return record;
    }

    public LifecycleRecord trackProcess(ProcessHandle handle, String name) {
        long pid = handle.pid();
        LifecycleRecord record = new LifecycleRecord(pid, name);
        lifecycleRecords.put(pid, record);

        record.addEvent(LifecycleEvent.Type.STARTED, "Process tracking started");
        notifyProcessStarted(record);

        // 프로세스 종료 감지
        handle.onExit().thenAccept(h -> {
            record.addEvent(LifecycleEvent.Type.TERMINATED, "Process terminated");
            notifyProcessTerminated(record);
        });

        return record;
    }

    /**
     * 프로세스 상태 확인
     */
    private void checkProcesses() {
        for (LifecycleRecord record : lifecycleRecords.values()) {
            if (record.isAlive()) {
                ProcessHandle.of(record.pid).ifPresentOrElse(
                        handle -> {
                            if (!handle.isAlive() && record.state == LifecycleRecord.LifecycleState.RUNNING) {
                                record.addEvent(LifecycleEvent.Type.TERMINATED, "Process no longer alive");
                                notifyProcessTerminated(record);
                            }
                        },
                        () -> {
                            if (record.state == LifecycleRecord.LifecycleState.RUNNING) {
                                record.addEvent(LifecycleEvent.Type.TERMINATED, "Process not found");
                                notifyProcessTerminated(record);
                            }
                        }
                );
            }
        }
    }

    /**
     * 이벤트 기록
     */
    public void recordEvent(long pid, LifecycleEvent.Type type, String description) {
        LifecycleRecord record = lifecycleRecords.get(pid);
        if (record != null) {
            LifecycleEvent event = new LifecycleEvent(type, description);
            record.addEvent(type, description);
            notifyLifecycleEvent(record, event);

            // 특별 이벤트 처리
            switch (type) {
                case RESTARTED -> notifyProcessRestarted(record);
                case ERROR -> notifyProcessError(record, description);
            }
        }
    }

    public void recordEvent(long pid, LifecycleEvent.Type type, String description,
                            Map<String, Object> data) {
        LifecycleRecord record = lifecycleRecords.get(pid);
        if (record != null) {
            LifecycleEvent event = new LifecycleEvent(type, description, data);
            record.addEvent(type, description, data);
            notifyLifecycleEvent(record, event);
        }
    }

    /**
     * 기록 조회
     */
    public LifecycleRecord getRecord(long pid) {
        return lifecycleRecords.get(pid);
    }

    public Collection<LifecycleRecord> getAllRecords() {
        return new ArrayList<>(lifecycleRecords.values());
    }

    public List<LifecycleRecord> getActiveRecords() {
        return lifecycleRecords.values().stream()
                .filter(LifecycleRecord::isAlive)
                .collect(Collectors.toList());
    }

    public List<LifecycleRecord> getTerminatedRecords() {
        return lifecycleRecords.values().stream()
                .filter(r -> r.state == LifecycleRecord.LifecycleState.TERMINATED)
                .collect(Collectors.toList());
    }

    /**
     * 이벤트 리스너 관리
     */
    public void addEventListener(LifecycleEventListener listener) {
        listeners.add(listener);
    }

    public void removeEventListener(LifecycleEventListener listener) {
        listeners.remove(listener);
    }

    // 리스너 통지 메서드들
    private void notifyProcessCreated(LifecycleRecord record) {
        listeners.forEach(l -> l.onProcessCreated(record));
    }

    private void notifyProcessStarted(LifecycleRecord record) {
        listeners.forEach(l -> l.onProcessStarted(record));
    }

    private void notifyProcessTerminated(LifecycleRecord record) {
        listeners.forEach(l -> l.onProcessTerminated(record));
    }

    private void notifyProcessRestarted(LifecycleRecord record) {
        listeners.forEach(l -> l.onProcessRestarted(record));
    }

    private void notifyProcessError(LifecycleRecord record, String error) {
        listeners.forEach(l -> l.onProcessError(record, error));
    }

    private void notifyLifecycleEvent(LifecycleRecord record, LifecycleEvent event) {
        listeners.forEach(l -> l.onLifecycleEvent(record, event));
    }

    /**
     * 통계 정보
     */
    public LifecycleStatistics getStatistics() {
        return new LifecycleStatistics(lifecycleRecords.values());
    }

    public static class LifecycleStatistics {
        private final int totalProcesses;
        private final int activeProcesses;
        private final int terminatedProcesses;
        private final int errorProcesses;
        private final double averageLifetime;
        private final int totalRestarts;
        private final Map<LifecycleRecord.LifecycleState, Integer> stateDistribution;
        private final LifecycleRecord longestRunning;
        private final LifecycleRecord mostRestarted;

        public LifecycleStatistics(Collection<LifecycleRecord> records) {
            this.totalProcesses = records.size();

            this.activeProcesses = (int) records.stream()
                    .filter(LifecycleRecord::isAlive)
                    .count();

            this.terminatedProcesses = (int) records.stream()
                    .filter(r -> r.state == LifecycleRecord.LifecycleState.TERMINATED)
                    .count();

            this.errorProcesses = (int) records.stream()
                    .filter(r -> r.state == LifecycleRecord.LifecycleState.ERROR)
                    .count();

            this.averageLifetime = records.stream()
                    .filter(r -> r.endTime != null)
                    .mapToLong(LifecycleRecord::getTotalRuntime)
                    .average()
                    .orElse(0.0);

            this.totalRestarts = records.stream()
                    .mapToInt(LifecycleRecord::getRestartCount)
                    .sum();

            this.stateDistribution = records.stream()
                    .collect(Collectors.groupingBy(
                            LifecycleRecord::getState,
                            Collectors.collectingAndThen(
                                    Collectors.counting(),
                                    Long::intValue
                            )
                    ));

            this.longestRunning = records.stream()
                    .max(Comparator.comparing(LifecycleRecord::getUptime))
                    .orElse(null);

            this.mostRestarted = records.stream()
                    .max(Comparator.comparingInt(LifecycleRecord::getRestartCount))
                    .orElse(null);
        }

        public void print() {
            System.out.println("=== Lifecycle Statistics ===");
            System.out.printf("Total: %d, Active: %d, Terminated: %d, Errors: %d\n",
                    totalProcesses, activeProcesses, terminatedProcesses, errorProcesses);
            System.out.printf("Avg Lifetime: %.2f seconds, Total Restarts: %d\n",
                    averageLifetime / 1000.0, totalRestarts);

            System.out.println("\nState Distribution:");
            stateDistribution.forEach((state, count) ->
                    System.out.printf("  %s: %d\n", state, count));

            if (longestRunning != null) {
                System.out.printf("\nLongest Running: %s (%.2f minutes)\n",
                        longestRunning.getProcessName(),
                        longestRunning.getUptime().toSeconds() / 60.0);
            }

            if (mostRestarted != null && mostRestarted.getRestartCount() > 0) {
                System.out.printf("Most Restarted: %s (%d times)\n",
                        mostRestarted.getProcessName(),
                        mostRestarted.getRestartCount());
            }
        }

        // Getters
        public int getTotalProcesses() { return totalProcesses; }
        public int getActiveProcesses() { return activeProcesses; }
        public int getTerminatedProcesses() { return terminatedProcesses; }
        public int getErrorProcesses() { return errorProcesses; }
        public double getAverageLifetime() { return averageLifetime; }
        public int getTotalRestarts() { return totalRestarts; }
        public Map<LifecycleRecord.LifecycleState, Integer> getStateDistribution() {
            return stateDistribution;
        }
    }
}