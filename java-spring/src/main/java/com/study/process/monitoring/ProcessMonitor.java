package com.study.process.monitoring;

import java.lang.management.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.stream.Collectors;

/**
 * 프로세스 상태 모니터링
 * 실시간으로 프로세스 정보 수집 및 분석
 */
public class ProcessMonitor {

    private final Map<Long, ProcessInfo> monitoredProcesses;
    private final ScheduledExecutorService scheduler;
    private final List<ProcessEventListener> listeners;
    private final AtomicBoolean monitoring;
    private final long refreshInterval;
    private final TimeUnit refreshUnit;

    /**
     * 프로세스 정보
     */
    public static class ProcessInfo {
        private final long pid;
        private final String command;
        private final Instant startTime;
        private ProcessHandle handle;
        private ProcessHandle.Info info;
        private ProcessState state;
        private long cpuTime;
        private long lastCpuTime;
        private double cpuUsage;
        private final List<ProcessSnapshot> history;
        private final Map<String, Object> metadata;

        public enum ProcessState {
            STARTING,   // 시작 중
            RUNNING,    // 실행 중
            SUSPENDED,  // 일시 중지
            TERMINATED, // 종료됨
            ZOMBIE      // 좀비 프로세스
        }

        public ProcessInfo(ProcessHandle handle) {
            this.handle = handle;
            this.pid = handle.pid();
            this.info = handle.info();
            this.command = info.command().orElse("Unknown");
            this.startTime = info.startInstant().orElse(Instant.now());
            this.state = ProcessState.STARTING;
            this.history = new CopyOnWriteArrayList<>();
            this.metadata = new ConcurrentHashMap<>();
            updateInfo();
        }

        public void updateInfo() {
            if (!handle.isAlive()) {
                state = ProcessState.TERMINATED;
                return;
            }

            info = handle.info();
            state = ProcessState.RUNNING;

            // CPU 사용률 계산
            long currentCpuTime = info.totalCpuDuration()
                    .map(Duration::toMillis)
                    .orElse(0L);

            if (lastCpuTime > 0) {
                // CPU 사용률 = (현재 CPU 시간 - 이전 CPU 시간) / 경과 시간 * 100
                long cpuDiff = currentCpuTime - lastCpuTime;
                cpuUsage = cpuDiff; // 실제로는 경과 시간으로 나누어야 함
            }

            lastCpuTime = currentCpuTime;
            cpuTime = currentCpuTime;

            // 스냅샷 저장 (최대 100개)
            history.add(new ProcessSnapshot(this));
            if (history.size() > 100) {
                history.remove(0);
            }
        }

        // Getters
        public long getPid() { return pid; }
        public String getCommand() { return command; }
        public Instant getStartTime() { return startTime; }
        public ProcessState getState() { return state; }
        public double getCpuUsage() { return cpuUsage; }
        public long getCpuTime() { return cpuTime; }
        public List<ProcessSnapshot> getHistory() { return new ArrayList<>(history); }
        public ProcessHandle getHandle() { return handle; }
        public ProcessHandle.Info getInfo() { return info; }

        public Duration getUptime() {
            return Duration.between(startTime, Instant.now());
        }

        public void setMetadata(String key, Object value) {
            metadata.put(key, value);
        }

        public Object getMetadata(String key) {
            return metadata.get(key);
        }
    }

    /**
     * 프로세스 스냅샷
     */
    public static class ProcessSnapshot {
        private final Instant timestamp;
        private final ProcessInfo.ProcessState state;
        private final double cpuUsage;
        private final long cpuTime;

        public ProcessSnapshot(ProcessInfo info) {
            this.timestamp = Instant.now();
            this.state = info.state;
            this.cpuUsage = info.cpuUsage;
            this.cpuTime = info.cpuTime;
        }

        public Instant getTimestamp() { return timestamp; }
        public ProcessInfo.ProcessState getState() { return state; }
        public double getCpuUsage() { return cpuUsage; }
        public long getCpuTime() { return cpuTime; }
    }

    /**
     * 프로세스 이벤트 리스너
     */
    public interface ProcessEventListener {
        void onProcessStarted(ProcessInfo info);
        void onProcessTerminated(ProcessInfo info);
        void onProcessStateChanged(ProcessInfo info, ProcessInfo.ProcessState oldState,
                                   ProcessInfo.ProcessState newState);
        void onHighCpuUsage(ProcessInfo info, double usage);
        void onHighMemoryUsage(ProcessInfo info, long memory);
    }

    /**
     * 생성자
     */
    public ProcessMonitor(long refreshInterval, TimeUnit unit) {
        this.monitoredProcesses = new ConcurrentHashMap<>();
        this.scheduler = Executors.newScheduledThreadPool(2);
        this.listeners = new CopyOnWriteArrayList<>();
        this.monitoring = new AtomicBoolean(false);
        this.refreshInterval = refreshInterval;
        this.refreshUnit = unit;
    }

    /**
     * 모니터링 시작
     */
    public void startMonitoring() {
        if (monitoring.getAndSet(true)) {
            return;
        }

        // 프로세스 업데이트 작업
        scheduler.scheduleAtFixedRate(
                this::updateProcesses,
                0, refreshInterval, refreshUnit
        );

        // 이벤트 체크 작업
        scheduler.scheduleAtFixedRate(
                this::checkProcessEvents,
                0, refreshInterval * 2, refreshUnit
        );

        System.out.println("ProcessMonitor started");
    }

    /**
     * 모니터링 중지
     */
    public void stopMonitoring() {
        if (!monitoring.getAndSet(false)) {
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

        System.out.println("ProcessMonitor stopped");
    }

    /**
     * 프로세스 추가
     */
    public void addProcess(ProcessHandle handle) {
        ProcessInfo info = new ProcessInfo(handle);
        monitoredProcesses.put(handle.pid(), info);
        notifyProcessStarted(info);

        System.out.printf("Monitoring process: [%d] %s\n", info.getPid(), info.getCommand());
    }

    public void addProcess(long pid) {
        ProcessHandle.of(pid).ifPresent(this::addProcess);
    }

    public void addProcess(Process process) {
        addProcess(process.toHandle());
    }

    /**
     * 프로세스 제거
     */
    public void removeProcess(long pid) {
        ProcessInfo info = monitoredProcesses.remove(pid);
        if (info != null) {
            notifyProcessTerminated(info);
            System.out.printf("Stopped monitoring: [%d] %s\n", pid, info.getCommand());
        }
    }

    /**
     * 프로세스 정보 업데이트
     */
    private void updateProcesses() {
        for (ProcessInfo info : monitoredProcesses.values()) {
            ProcessInfo.ProcessState oldState = info.state;
            info.updateInfo();

            // 상태 변경 감지
            if (oldState != info.state) {
                notifyStateChanged(info, oldState, info.state);
            }

            // 높은 CPU 사용률 감지 (80% 이상)
            if (info.cpuUsage > 80) {
                notifyHighCpuUsage(info, info.cpuUsage);
            }

            // 종료된 프로세스 제거
            if (info.state == ProcessInfo.ProcessState.TERMINATED) {
                removeProcess(info.pid);
            }
        }
    }

    /**
     * 프로세스 이벤트 체크
     */
    private void checkProcessEvents() {
        // 종료된 프로세스 감지
        monitoredProcesses.entrySet().removeIf(entry -> {
            ProcessInfo info = entry.getValue();
            if (!info.handle.isAlive()) {
                notifyProcessTerminated(info);
                return true;
            }
            return false;
        });
    }

    /**
     * 프로세스 정보 조회
     */
    public ProcessInfo getProcessInfo(long pid) {
        return monitoredProcesses.get(pid);
    }

    public Collection<ProcessInfo> getAllProcesses() {
        return new ArrayList<>(monitoredProcesses.values());
    }

    public List<ProcessInfo> getRunningProcesses() {
        return monitoredProcesses.values().stream()
                .filter(p -> p.state == ProcessInfo.ProcessState.RUNNING)
                .collect(Collectors.toList());
    }

    public List<ProcessInfo> getTerminatedProcesses() {
        return monitoredProcesses.values().stream()
                .filter(p -> p.state == ProcessInfo.ProcessState.TERMINATED)
                .collect(Collectors.toList());
    }

    /**
     * 이벤트 리스너 관리
     */
    public void addEventListener(ProcessEventListener listener) {
        listeners.add(listener);
    }

    public void removeEventListener(ProcessEventListener listener) {
        listeners.remove(listener);
    }

    // 이벤트 알림 메서드들
    private void notifyProcessStarted(ProcessInfo info) {
        listeners.forEach(l -> l.onProcessStarted(info));
    }

    private void notifyProcessTerminated(ProcessInfo info) {
        listeners.forEach(l -> l.onProcessTerminated(info));
    }

    private void notifyStateChanged(ProcessInfo info, ProcessInfo.ProcessState oldState,
                                    ProcessInfo.ProcessState newState) {
        listeners.forEach(l -> l.onProcessStateChanged(info, oldState, newState));
    }

    private void notifyHighCpuUsage(ProcessInfo info, double usage) {
        listeners.forEach(l -> l.onHighCpuUsage(info, usage));
    }

    /**
     * 통계 정보
     */
    public MonitorStatistics getStatistics() {
        return new MonitorStatistics(monitoredProcesses.values());
    }

    public static class MonitorStatistics {
        private final int totalProcesses;
        private final int runningProcesses;
        private final int terminatedProcesses;
        private final double averageCpuUsage;
        private final long totalCpuTime;
        private final Duration averageUptime;
        private final Map<ProcessInfo.ProcessState, Integer> stateDistribution;

        public MonitorStatistics(Collection<ProcessInfo> processes) {
            this.totalProcesses = processes.size();

            this.runningProcesses = (int) processes.stream()
                    .filter(p -> p.state == ProcessInfo.ProcessState.RUNNING)
                    .count();

            this.terminatedProcesses = (int) processes.stream()
                    .filter(p -> p.state == ProcessInfo.ProcessState.TERMINATED)
                    .count();

            this.averageCpuUsage = processes.stream()
                    .mapToDouble(ProcessInfo::getCpuUsage)
                    .average()
                    .orElse(0.0);

            this.totalCpuTime = processes.stream()
                    .mapToLong(ProcessInfo::getCpuTime)
                    .sum();

            // 평균 실행 시간
            double avgSeconds = processes.stream()
                    .map(ProcessInfo::getUptime)
                    .mapToLong(Duration::toSeconds)
                    .average()
                    .orElse(0.0);
            this.averageUptime = Duration.ofSeconds((long) avgSeconds);

            // 상태 분포
            this.stateDistribution = processes.stream()
                    .collect(Collectors.groupingBy(
                            ProcessInfo::getState,
                            Collectors.collectingAndThen(
                                    Collectors.counting(),
                                    Long::intValue
                            )
                    ));
        }

        public void print() {
            System.out.println("=== Process Monitor Statistics ===");
            System.out.printf("Total: %d, Running: %d, Terminated: %d\n",
                    totalProcesses, runningProcesses, terminatedProcesses);
            System.out.printf("Avg CPU Usage: %.2f%%, Total CPU Time: %d ms\n",
                    averageCpuUsage, totalCpuTime);
            System.out.printf("Avg Uptime: %d seconds\n", averageUptime.toSeconds());

            System.out.println("State Distribution:");
            stateDistribution.forEach((state, count) ->
                    System.out.printf("  %s: %d\n", state, count));
        }

        // Getters
        public int getTotalProcesses() { return totalProcesses; }
        public int getRunningProcesses() { return runningProcesses; }
        public int getTerminatedProcesses() { return terminatedProcesses; }
        public double getAverageCpuUsage() { return averageCpuUsage; }
        public long getTotalCpuTime() { return totalCpuTime; }
        public Duration getAverageUptime() { return averageUptime; }
        public Map<ProcessInfo.ProcessState, Integer> getStateDistribution() {
            return stateDistribution;
        }
    }
}