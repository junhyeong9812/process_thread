package com.study.process.creation;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * 병렬 프로세스 생성 및 관리
 * 다수의 프로세스를 동시에 생성하고 관리하는 기능 제공
 */
public class ProcessSpawner {

    private final ProcessFactory processFactory;
    private final ExecutorService executor;
    private final Map<Long, ProcessWrapper> managedProcesses;
    private final ScheduledExecutorService scheduler;
    private volatile boolean isRunning;

    /**
     * 프로세스 래퍼 클래스
     */
    public static class ProcessWrapper {
        private final Process process;
        private final long startTime;
        private final ProcessFactory.ProcessConfig config;
        private volatile ProcessStatus status;
        private volatile Integer exitCode;
        private volatile String output;

        public enum ProcessStatus {
            STARTING, RUNNING, COMPLETED, FAILED, TERMINATED
        }

        public ProcessWrapper(Process process, ProcessFactory.ProcessConfig config) {
            this.process = process;
            this.config = config;
            this.startTime = System.currentTimeMillis();
            this.status = ProcessStatus.STARTING;
        }

        public Process getProcess() { return process; }
        public long getStartTime() { return startTime; }
        public ProcessFactory.ProcessConfig getConfig() { return config; }
        public ProcessStatus getStatus() { return status; }
        public Integer getExitCode() { return exitCode; }
        public String getOutput() { return output; }
        public long getPid() { return process.pid(); }

        public void setStatus(ProcessStatus status) { this.status = status; }
        public void setExitCode(Integer exitCode) { this.exitCode = exitCode; }
        public void setOutput(String output) { this.output = output; }

        public boolean isAlive() {
            return process.isAlive();
        }

        public long getUptime() {
            return System.currentTimeMillis() - startTime;
        }
    }

    /**
     * 스폰 결과 클래스
     */
    public static class SpawnResult {
        private final List<ProcessWrapper> successfulProcesses;
        private final List<Exception> failures;
        private final long totalTime;

        public SpawnResult(List<ProcessWrapper> successfulProcesses,
                           List<Exception> failures,
                           long totalTime) {
            this.successfulProcesses = successfulProcesses;
            this.failures = failures;
            this.totalTime = totalTime;
        }

        public List<ProcessWrapper> getSuccessfulProcesses() {
            return successfulProcesses;
        }

        public List<Exception> getFailures() {
            return failures;
        }

        public long getTotalTime() {
            return totalTime;
        }

        public int getSuccessCount() {
            return successfulProcesses.size();
        }

        public int getFailureCount() {
            return failures.size();
        }

        public boolean hasFailures() {
            return !failures.isEmpty();
        }

        @Override
        public String toString() {
            return String.format("SpawnResult[success=%d, failures=%d, time=%dms]",
                    getSuccessCount(), getFailureCount(), totalTime);
        }
    }

    public ProcessSpawner() {
        this(Runtime.getRuntime().availableProcessors());
    }

    public ProcessSpawner(int threadPoolSize) {
        this.processFactory = new ProcessFactory();
        this.executor = Executors.newFixedThreadPool(threadPoolSize);
        this.managedProcesses = new ConcurrentHashMap<>();
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.isRunning = true;

        // 프로세스 상태 모니터링 시작
        startMonitoring();
    }

    /**
     * 동시에 여러 프로세스 생성
     */
    public SpawnResult spawnProcesses(List<ProcessFactory.ProcessConfig> configs) {
        long startTime = System.currentTimeMillis();
        List<ProcessWrapper> successfulProcesses = new CopyOnWriteArrayList<>();
        List<Exception> failures = new CopyOnWriteArrayList<>();

        List<CompletableFuture<ProcessWrapper>> futures = configs.stream()
                .map(config -> CompletableFuture.supplyAsync(() -> {
                    try {
                        Process process = processFactory.createProcess(config);
                        ProcessWrapper wrapper = new ProcessWrapper(process, config);
                        wrapper.setStatus(ProcessWrapper.ProcessStatus.RUNNING);
                        managedProcesses.put(wrapper.getPid(), wrapper);
                        successfulProcesses.add(wrapper);
                        return wrapper;
                    } catch (Exception e) {
                        failures.add(e);
                        return null;
                    }
                }, executor))
                .collect(Collectors.toList());

        // 모든 프로세스 생성 완료 대기
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        long totalTime = System.currentTimeMillis() - startTime;
        return new SpawnResult(successfulProcesses, failures, totalTime);
    }

    /**
     * 동일한 설정으로 N개의 프로세스 생성
     */
    public SpawnResult spawnIdenticalProcesses(ProcessFactory.ProcessConfig baseConfig,
                                               int count) {
        List<ProcessFactory.ProcessConfig> configs = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            ProcessFactory.ProcessConfig config = new ProcessFactory.ProcessConfig()
                    .type(baseConfig.getType())
                    .workingDirectory(baseConfig.getWorkingDirectory())
                    .redirectErrorStream(baseConfig.isRedirectErrorStream())
                    .timeout(baseConfig.getTimeout());

            // 기본 파라미터 복사 및 인덱스 추가
            baseConfig.getParameters().forEach(config::parameter);
            config.parameter("spawn_index", String.valueOf(i));
            config.parameter("spawn_id", UUID.randomUUID().toString());

            baseConfig.getEnvironmentVariables().forEach(config::environment);
            configs.add(config);
        }

        return spawnProcesses(configs);
    }

    /**
     * 프로세스 파이프라인 생성 (출력을 다음 프로세스의 입력으로 연결)
     */
    public ProcessWrapper createPipeline(List<ProcessFactory.ProcessConfig> configs)
            throws IOException, InterruptedException {
        if (configs.isEmpty()) {
            throw new IllegalArgumentException("Pipeline requires at least one process");
        }

        ProcessWrapper lastWrapper = null;

        for (int i = 0; i < configs.size(); i++) {
            ProcessFactory.ProcessConfig config = configs.get(i);

            // 이전 프로세스가 있으면 완료 대기
            if (lastWrapper != null) {
                lastWrapper.getProcess().waitFor();

                // 이전 프로세스의 출력을 현재 프로세스의 입력으로 설정
                String previousOutput = readProcessOutput(lastWrapper.getProcess());
                config.parameter("input", previousOutput);
            }

            Process process = processFactory.createProcess(config);
            lastWrapper = new ProcessWrapper(process, config);
            lastWrapper.setStatus(ProcessWrapper.ProcessStatus.RUNNING);
            managedProcesses.put(lastWrapper.getPid(), lastWrapper);
        }

        return lastWrapper;
    }

    /**
     * 프로세스 그룹 생성 및 관리
     */
    public String createProcessGroup(List<ProcessFactory.ProcessConfig> configs) {
        String groupId = UUID.randomUUID().toString();

        configs.forEach(config -> {
            config.environment("PROCESS_GROUP_ID", groupId);
        });

        SpawnResult result = spawnProcesses(configs);

        // 그룹 ID로 프로세스 태깅
        result.getSuccessfulProcesses().forEach(wrapper -> {
            wrapper.getConfig().parameter("group_id", groupId);
        });

        return groupId;
    }

    /**
     * 그룹별 프로세스 조회
     */
    public List<ProcessWrapper> getProcessesByGroup(String groupId) {
        return managedProcesses.values().stream()
                .filter(wrapper -> groupId.equals(
                        wrapper.getConfig().getParameters().get("group_id")))
                .collect(Collectors.toList());
    }

    /**
     * 프로세스 상태 모니터링
     */
    private void startMonitoring() {
        scheduler.scheduleAtFixedRate(() -> {
            if (!isRunning) return;

            managedProcesses.values().forEach(wrapper -> {
                if (!wrapper.isAlive() &&
                        wrapper.getStatus() == ProcessWrapper.ProcessStatus.RUNNING) {

                    try {
                        int exitCode = wrapper.getProcess().exitValue();
                        wrapper.setExitCode(exitCode);
                        wrapper.setStatus(exitCode == 0 ?
                                ProcessWrapper.ProcessStatus.COMPLETED :
                                ProcessWrapper.ProcessStatus.FAILED);

                        // 프로세스 출력 저장
                        String output = readProcessOutput(wrapper.getProcess());
                        wrapper.setOutput(output);

                    } catch (Exception e) {
                        wrapper.setStatus(ProcessWrapper.ProcessStatus.FAILED);
                    }
                }
            });

            // 완료된 프로세스 정리 (옵션)
            cleanupCompletedProcesses();

        }, 0, 1, TimeUnit.SECONDS);
    }

    /**
     * 완료된 프로세스 정리
     */
    private void cleanupCompletedProcesses() {
        long cutoffTime = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(5);

        managedProcesses.entrySet().removeIf(entry -> {
            ProcessWrapper wrapper = entry.getValue();
            return !wrapper.isAlive() && wrapper.getStartTime() < cutoffTime;
        });
    }

    /**
     * 프로세스 출력 읽기
     */
    private String readProcessOutput(Process process) {
        try {
            ProcessCreator creator = new ProcessCreator();
            return creator.readOutput(process);
        } catch (IOException e) {
            return "Error reading output: " + e.getMessage();
        }
    }

    /**
     * 특정 프로세스 종료
     */
    public boolean terminateProcess(long pid) {
        ProcessWrapper wrapper = managedProcesses.get(pid);
        if (wrapper != null && wrapper.isAlive()) {
            wrapper.getProcess().destroyForcibly();
            wrapper.setStatus(ProcessWrapper.ProcessStatus.TERMINATED);
            return true;
        }
        return false;
    }

    /**
     * 모든 프로세스 종료
     */
    public void terminateAll() {
        managedProcesses.values().forEach(wrapper -> {
            if (wrapper.isAlive()) {
                wrapper.getProcess().destroyForcibly();
                wrapper.setStatus(ProcessWrapper.ProcessStatus.TERMINATED);
            }
        });
    }

    /**
     * 그룹별 프로세스 종료
     */
    public void terminateGroup(String groupId) {
        getProcessesByGroup(groupId).forEach(wrapper -> {
            if (wrapper.isAlive()) {
                wrapper.getProcess().destroyForcibly();
                wrapper.setStatus(ProcessWrapper.ProcessStatus.TERMINATED);
            }
        });
    }

    /**
     * 프로세스 재시작
     */
    public ProcessWrapper restartProcess(long pid) throws IOException {
        ProcessWrapper oldWrapper = managedProcesses.get(pid);
        if (oldWrapper == null) {
            throw new IllegalArgumentException("Process not found: " + pid);
        }

        // 기존 프로세스 종료
        if (oldWrapper.isAlive()) {
            oldWrapper.getProcess().destroyForcibly();
        }

        // 새 프로세스 생성
        Process newProcess = processFactory.createProcess(oldWrapper.getConfig());
        ProcessWrapper newWrapper = new ProcessWrapper(newProcess, oldWrapper.getConfig());
        newWrapper.setStatus(ProcessWrapper.ProcessStatus.RUNNING);

        // 관리 맵 업데이트
        managedProcesses.remove(pid);
        managedProcesses.put(newWrapper.getPid(), newWrapper);

        return newWrapper;
    }

    /**
     * 프로세스 통계
     */
    public SpawnerStatistics getStatistics() {
        return new SpawnerStatistics(managedProcesses.values());
    }

    /**
     * 통계 클래스
     */
    public static class SpawnerStatistics {
        private final int total;
        private final int running;
        private final int completed;
        private final int failed;
        private final int terminated;
        private final double averageUptime;

        public SpawnerStatistics(Collection<ProcessWrapper> processes) {
            this.total = processes.size();

            Map<ProcessWrapper.ProcessStatus, Long> statusCount = processes.stream()
                    .collect(Collectors.groupingBy(
                            ProcessWrapper::getStatus,
                            Collectors.counting()
                    ));

            this.running = statusCount.getOrDefault(
                    ProcessWrapper.ProcessStatus.RUNNING, 0L).intValue();
            this.completed = statusCount.getOrDefault(
                    ProcessWrapper.ProcessStatus.COMPLETED, 0L).intValue();
            this.failed = statusCount.getOrDefault(
                    ProcessWrapper.ProcessStatus.FAILED, 0L).intValue();
            this.terminated = statusCount.getOrDefault(
                    ProcessWrapper.ProcessStatus.TERMINATED, 0L).intValue();

            this.averageUptime = processes.stream()
                    .mapToLong(ProcessWrapper::getUptime)
                    .average()
                    .orElse(0.0);
        }

        public int getTotal() { return total; }
        public int getRunning() { return running; }
        public int getCompleted() { return completed; }
        public int getFailed() { return failed; }
        public int getTerminated() { return terminated; }
        public double getAverageUptime() { return averageUptime; }

        @Override
        public String toString() {
            return String.format("Total: %d, Running: %d, Completed: %d, Failed: %d, " +
                            "Terminated: %d, Avg Uptime: %.2fms",
                    total, running, completed, failed, terminated, averageUptime);
        }
    }

    /**
     * 리소스 정리
     */
    public void shutdown() {
        isRunning = false;
        terminateAll();
        executor.shutdown();
        scheduler.shutdown();

        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}