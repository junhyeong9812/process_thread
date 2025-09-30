package com.study.process.management;

import com.study.process.creation.*;
import com.study.process.monitoring.*;
import java.io.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.stream.Collectors;

/**
 * 프로세스 생성/종료 관리
 * 프로세스의 전체 생명주기를 관리하는 중앙 매니저
 */
public class ProcessManager {

    private final ProcessFactory processFactory;
    private final ProcessMonitor processMonitor;
    private final ProcessLifecycleTracker lifecycleTracker;
    private final ProcessResourceTracker resourceTracker;
    private final Map<Long, ManagedProcess> managedProcesses;
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean running;

    public static class ManagedProcess {
        private final Process process;
        private final ProcessFactory.ProcessConfig config;
        private final String name;
        private final Instant startTime;
        private volatile ProcessState state;
        private volatile Integer exitCode;
        private int restartAttempts;
        private final boolean autoRestart;
        private final Map<String, Object> metadata;

        public enum ProcessState {
            STARTING, RUNNING, STOPPING, STOPPED, FAILED, RESTARTING
        }

        public ManagedProcess(Process process, ProcessFactory.ProcessConfig config,
                              String name, boolean autoRestart) {
            this.process = process;
            this.config = config;
            this.name = name;
            this.startTime = Instant.now();
            this.state = ProcessState.STARTING;
            this.autoRestart = autoRestart;
            this.restartAttempts = 0;
            this.metadata = new ConcurrentHashMap<>();
        }

        // Getters and Setters
        public Process getProcess() { return process; }
        public ProcessFactory.ProcessConfig getConfig() { return config; }
        public String getName() { return name; }
        public Instant getStartTime() { return startTime; }
        public ProcessState getState() { return state; }
        public void setState(ProcessState state) { this.state = state; }
        public Integer getExitCode() { return exitCode; }
        public void setExitCode(Integer exitCode) { this.exitCode = exitCode; }
        public int getRestartAttempts() { return restartAttempts; }
        public void incrementRestartAttempts() { this.restartAttempts++; }
        public boolean isAutoRestart() { return autoRestart; }
        public long getPid() { return process.pid(); }

        public void setMetadata(String key, Object value) {
            metadata.put(key, value);
        }

        public Object getMetadata(String key) {
            return metadata.get(key);
        }

        public boolean isAlive() {
            return process.isAlive();
        }

        public Duration getUptime() {
            return Duration.between(startTime, Instant.now());
        }
    }

    public ProcessManager() {
        this.processFactory = new ProcessFactory();
        this.processMonitor = new ProcessMonitor(1, TimeUnit.SECONDS);
        this.lifecycleTracker = new ProcessLifecycleTracker();
        this.resourceTracker = new ProcessResourceTracker(1, TimeUnit.SECONDS);
        this.managedProcesses = new ConcurrentHashMap<>();
        this.scheduler = Executors.newScheduledThreadPool(2);
        this.running = new AtomicBoolean(false);
    }

    /**
     * ProcessManager 시작
     */
    public void start() {
        if (running.getAndSet(true)) {
            return;
        }

        processMonitor.startMonitoring();
        lifecycleTracker.startTracking();
        resourceTracker.startTracking();

        // 프로세스 상태 체크 (5초마다)
        scheduler.scheduleAtFixedRate(this::checkProcesses, 0, 5, TimeUnit.SECONDS);

        // 자동 재시작 처리 (10초마다)
        scheduler.scheduleAtFixedRate(this::handleAutoRestart, 0, 10, TimeUnit.SECONDS);

        System.out.println("ProcessManager started");
    }

    /**
     * ProcessManager 종료
     */
    public void stop() {
        if (!running.getAndSet(false)) {
            return;
        }

        // 모든 프로세스 종료
        stopAllProcesses();

        processMonitor.stopMonitoring();
        lifecycleTracker.stopTracking();
        resourceTracker.stopTracking();

        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }

        System.out.println("ProcessManager stopped");
    }

    /**
     * 프로세스 생성
     */
    public ManagedProcess createProcess(String name, ProcessFactory.ProcessConfig config,
                                        boolean autoRestart) throws IOException {
        // ProcessFactory를 통해 프로세스 생성
        Process process = processFactory.createProcess(config);

        // ManagedProcess 생성
        ManagedProcess managedProcess = new ManagedProcess(process, config, name, autoRestart);
        managedProcess.setState(ManagedProcess.ProcessState.RUNNING);

        // 관리 맵에 추가
        managedProcesses.put(process.pid(), managedProcess);

        // 모니터링 시작
        processMonitor.addProcess(process.toHandle());
        lifecycleTracker.trackProcess(process, name);
        resourceTracker.trackProcess(process.pid());

        // 프로세스 종료 콜백 등록
        process.onExit().thenAccept(p -> handleProcessExit(managedProcess));

        System.out.printf("Process created: %s [PID: %d]\n", name, process.pid());

        return managedProcess;
    }

    /**
     * 간편한 프로세스 생성 (기본 설정)
     */
    public ManagedProcess createSimpleProcess(String name, ProcessFactory.ProcessType type)
            throws IOException {
        ProcessFactory.ProcessConfig config = new ProcessFactory.ProcessConfig()
                .type(type);
        return createProcess(name, config, false);
    }

    /**
     * 프로세스 종료
     */
    public void stopProcess(long pid) {
        stopProcess(pid, 10, TimeUnit.SECONDS);
    }

    public void stopProcess(long pid, long timeout, TimeUnit unit) {
        ManagedProcess managed = managedProcesses.get(pid);
        if (managed == null) {
            System.out.println("Process not found: " + pid);
            return;
        }

        managed.setState(ManagedProcess.ProcessState.STOPPING);
        Process process = managed.getProcess();

        try {
            // 정상 종료 시도
            process.destroy();

            boolean terminated = process.waitFor(timeout, unit);
            if (!terminated) {
                // 강제 종료
                System.out.printf("Force killing process %s [PID: %d]\n",
                        managed.getName(), pid);
                process.destroyForcibly();
                process.waitFor(5, TimeUnit.SECONDS);
            }

            managed.setState(ManagedProcess.ProcessState.STOPPED);
            managed.setExitCode(process.exitValue());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
        }

        // 모니터링 중지
        processMonitor.removeProcess(pid);
        resourceTracker.untrackProcess(pid);

        // 관리 맵에서 제거
        managedProcesses.remove(pid);

        System.out.printf("Process stopped: %s [PID: %d, Exit: %d]\n",
                managed.getName(), pid, managed.getExitCode());
    }

    /**
     * 모든 프로세스 종료
     */
    public void stopAllProcesses() {
        List<Long> pids = new ArrayList<>(managedProcesses.keySet());
        for (Long pid : pids) {
            stopProcess(pid);
        }
    }

    /**
     * 프로세스 재시작
     */
    public ManagedProcess restartProcess(long pid) throws IOException {
        ManagedProcess managed = managedProcesses.get(pid);
        if (managed == null) {
            throw new IllegalArgumentException("Process not found: " + pid);
        }

        String newName = managed.getName() + "_restart_" + (managed.getRestartAttempts() + 1);
        managed.setState(ManagedProcess.ProcessState.RESTARTING);
        managed.incrementRestartAttempts();

        System.out.printf("Restarting process: %s -> %s\n", managed.getName(), newName);

        // 기존 프로세스 종료
        stopProcess(pid);

        // 새 프로세스 생성
        return createProcess(newName, managed.getConfig(), managed.isAutoRestart());
    }

    /**
     * 프로세스 종료 핸들러
     */
    private void handleProcessExit(ManagedProcess managed) {
        managed.setExitCode(managed.getProcess().exitValue());

        if (managed.getExitCode() != 0) {
            managed.setState(ManagedProcess.ProcessState.FAILED);
            System.out.printf("Process failed: %s [PID: %d, Exit: %d]\n",
                    managed.getName(), managed.getPid(), managed.getExitCode());
        } else {
            managed.setState(ManagedProcess.ProcessState.STOPPED);
            System.out.printf("Process completed: %s [PID: %d]\n",
                    managed.getName(), managed.getPid());
        }

        // 라이프사이클 이벤트 기록
        lifecycleTracker.recordEvent(
                managed.getPid(),
                ProcessLifecycleTracker.LifecycleEvent.Type.TERMINATED,
                "Process exited with code: " + managed.getExitCode()
        );
    }

    /**
     * 프로세스 상태 체크
     */
    private void checkProcesses() {
        for (ManagedProcess managed : managedProcesses.values()) {
            if (managed.getState() == ManagedProcess.ProcessState.RUNNING) {
                if (!managed.isAlive()) {
                    handleProcessExit(managed);
                }
            }
        }
    }

    /**
     * 자동 재시작 처리
     */
    private void handleAutoRestart() {
        List<ManagedProcess> toRestart = new ArrayList<>();

        for (ManagedProcess managed : managedProcesses.values()) {
            if (managed.isAutoRestart() &&
                    managed.getState() == ManagedProcess.ProcessState.FAILED &&
                    managed.getRestartAttempts() < 3) {
                toRestart.add(managed);
            }
        }

        for (ManagedProcess managed : toRestart) {
            try {
                System.out.printf("Auto-restarting process: %s (attempt %d/3)\n",
                        managed.getName(), managed.getRestartAttempts() + 1);
                restartProcess(managed.getPid());
            } catch (IOException e) {
                System.err.printf("Failed to auto-restart process %s: %s\n",
                        managed.getName(), e.getMessage());
            }
        }
    }

    /**
     * 프로세스 조회
     */
    public ManagedProcess getProcess(long pid) {
        return managedProcesses.get(pid);
    }

    public ManagedProcess getProcessByName(String name) {
        return managedProcesses.values().stream()
                .filter(p -> p.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    public Collection<ManagedProcess> getAllProcesses() {
        return new ArrayList<>(managedProcesses.values());
    }

    public List<ManagedProcess> getRunningProcesses() {
        return managedProcesses.values().stream()
                .filter(p -> p.getState() == ManagedProcess.ProcessState.RUNNING)
                .toList();
    }

    public List<ManagedProcess> getFailedProcesses() {
        return managedProcesses.values().stream()
                .filter(p -> p.getState() == ManagedProcess.ProcessState.FAILED)
                .toList();
    }

    /**
     * 통계 정보
     */
    public ProcessManagerStats getStatistics() {
        return new ProcessManagerStats(managedProcesses.values());
    }

    public static class ProcessManagerStats {
        private final int totalProcesses;
        private final int runningProcesses;
        private final int failedProcesses;
        private final int stoppedProcesses;
        private final int totalRestarts;

        public ProcessManagerStats(Collection<ManagedProcess> processes) {
            this.totalProcesses = processes.size();

            Map<ManagedProcess.ProcessState, Long> stateCounts = processes.stream()
                    .collect(Collectors.groupingBy(
                            ManagedProcess::getState,
                            Collectors.counting()
                    ));

            this.runningProcesses = stateCounts.getOrDefault(
                    ManagedProcess.ProcessState.RUNNING, 0L).intValue();
            this.failedProcesses = stateCounts.getOrDefault(
                    ManagedProcess.ProcessState.FAILED, 0L).intValue();
            this.stoppedProcesses = stateCounts.getOrDefault(
                    ManagedProcess.ProcessState.STOPPED, 0L).intValue();

            this.totalRestarts = processes.stream()
                    .mapToInt(ManagedProcess::getRestartAttempts)
                    .sum();
        }

        public void print() {
            System.out.println("=== ProcessManager Statistics ===");
            System.out.printf("Total: %d, Running: %d, Failed: %d, Stopped: %d\n",
                    totalProcesses, runningProcesses, failedProcesses, stoppedProcesses);
            System.out.printf("Total Restarts: %d\n", totalRestarts);
        }

        // Getters
        public int getTotalProcesses() { return totalProcesses; }
        public int getRunningProcesses() { return runningProcesses; }
        public int getFailedProcesses() { return failedProcesses; }
        public int getStoppedProcesses() { return stoppedProcesses; }
        public int getTotalRestarts() { return totalRestarts; }
    }
}