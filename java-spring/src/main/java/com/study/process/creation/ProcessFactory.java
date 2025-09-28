package com.study.process.creation;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 다양한 타입의 프로세스를 생성하는 팩토리 패턴 구현
 * 프로세스 타입별 생성 전략을 캡슐화
 */
public class ProcessFactory {

    // 프로세스 타입 정의
    public enum ProcessType {
        ECHO,           // 에코 프로세스
        COMPUTE,        // 계산 프로세스
        FILE_WRITER,    // 파일 쓰기 프로세스
        LONG_RUNNING,   // 장기 실행 프로세스
        ERROR,          // 에러 시뮬레이션 프로세스
        CUSTOM          // 커스텀 프로세스
    }

    // 프로세스 설정을 위한 Builder 패턴
    public static class ProcessConfig {
        private ProcessType type = ProcessType.ECHO;
        private Map<String, String> parameters = new HashMap<>();
        private File workingDirectory;
        private Map<String, String> environmentVariables = new HashMap<>();
        private boolean redirectErrorStream = true;
        private long timeout = 10000; // 기본 10초

        public ProcessConfig type(ProcessType type) {
            this.type = type;
            return this;
        }

        public ProcessConfig parameter(String key, String value) {
            parameters.put(key, value);
            return this;
        }

        public ProcessConfig workingDirectory(File dir) {
            this.workingDirectory = dir;
            return this;
        }

        public ProcessConfig environment(String key, String value) {
            environmentVariables.put(key, value);
            return this;
        }

        public ProcessConfig redirectErrorStream(boolean redirect) {
            this.redirectErrorStream = redirect;
            return this;
        }

        public ProcessConfig timeout(long timeout) {
            this.timeout = timeout;
            return this;
        }

        public ProcessType getType() { return type; }
        public Map<String, String> getParameters() { return parameters; }
        public File getWorkingDirectory() { return workingDirectory; }
        public Map<String, String> getEnvironmentVariables() { return environmentVariables; }
        public boolean isRedirectErrorStream() { return redirectErrorStream; }
        public long getTimeout() { return timeout; }
    }

    // 관리되는 프로세스 정보
    public static class ManagedProcess {
        private final Process process;
        private final ProcessHandle handle;
        private final ProcessConfig config;
        private final Instant createdAt;
        private final Map<String, Object> metadata;

        public ManagedProcess(Process process, ProcessConfig config) {
            this.process = process;
            this.handle = process.toHandle();
            this.config = config;
            this.createdAt = Instant.now();
            this.metadata = new HashMap<>();
            metadata.put("type", config.getType());
            metadata.put("pid", process.pid());
        }

        public Process getProcess() { return process; }
        public ProcessHandle getHandle() { return handle; }
        public ProcessConfig getConfig() { return config; }
        public Instant getCreatedAt() { return createdAt; }
        public Map<String, Object> getMetadata() { return metadata; }

        public boolean isAlive() {
            return process.isAlive();
        }

        public long getPid() {
            return process.pid();
        }

        public void addMetadata(String key, Object value) {
            metadata.put(key, value);
        }
    }

    // 생성된 프로세스 추적
    private final Map<Long, ManagedProcess> managedProcesses = new ConcurrentHashMap<>();
    private final ProcessCreator processCreator = new ProcessCreator();

    /**
     * 프로세스 생성 메인 메서드
     */
    public Process createProcess(ProcessConfig config) throws IOException {
        Process process = null;

        switch (config.getType()) {
            case ECHO:
                process = createEchoProcess(config);
                break;
            case COMPUTE:
                process = createComputeProcess(config);
                break;
            case FILE_WRITER:
                process = createFileWriterProcess(config);
                break;
            case LONG_RUNNING:
                process = createLongRunningProcess(config);
                break;
            case ERROR:
                process = createErrorProcess(config);
                break;
            case CUSTOM:
                process = createCustomProcess(config);
                break;
            default:
                throw new IllegalArgumentException("Unknown process type: " + config.getType());
        }

        // 프로세스 추적
        if (process != null) {
            ManagedProcess managed = new ManagedProcess(process, config);
            managedProcesses.put(process.pid(), managed);
        }

        return process;
    }

    /**
     * ECHO 프로세스 생성
     */
    private Process createEchoProcess(ProcessConfig config) throws IOException {
        String message = config.getParameters().getOrDefault("message", "Hello from Echo Process");
        String[] args = {"ECHO", message};

        return createChildProcess(config, args);
    }

    /**
     * COMPUTE 프로세스 생성
     */
    private Process createComputeProcess(ProcessConfig config) throws IOException {
        String n = config.getParameters().getOrDefault("n", "100");
        String[] args = {"COMPUTE", n};

        return createChildProcess(config, args);
    }

    /**
     * FILE_WRITER 프로세스 생성
     */
    private Process createFileWriterProcess(ProcessConfig config) throws IOException {
        String filename = config.getParameters().getOrDefault("filename", "output.txt");
        String content = config.getParameters().getOrDefault("content", "Default content");
        String[] args = {"FILE", filename, content};

        return createChildProcess(config, args);
    }

    /**
     * LONG_RUNNING 프로세스 생성
     */
    private Process createLongRunningProcess(ProcessConfig config) throws IOException {
        String duration = config.getParameters().getOrDefault("duration", "5000");
        String[] args = {"SLEEP", duration};

        return createChildProcess(config, args);
    }

    /**
     * ERROR 프로세스 생성
     */
    private Process createErrorProcess(ProcessConfig config) throws IOException {
        String errorCode = config.getParameters().getOrDefault("errorCode", "1");
        String[] args = {"ERROR", errorCode};

        return createChildProcess(config, args);
    }

    /**
     * CUSTOM 프로세스 생성
     */
    private Process createCustomProcess(ProcessConfig config) throws IOException {
        String command = config.getParameters().get("command");
        if (command == null) {
            throw new IllegalArgumentException("Custom process requires 'command' parameter");
        }

        String argsString = config.getParameters().getOrDefault("args", "");
        String[] args = argsString.isEmpty() ? new String[0] : argsString.split(" ");

        processCreator.reset();
        processCreator.setCommand(command);
        for (String arg : args) {
            processCreator.addArgument(arg);
        }

        if (config.getWorkingDirectory() != null) {
            processCreator.setWorkingDirectory(config.getWorkingDirectory());
        }

        config.getEnvironmentVariables().forEach(processCreator::setEnvironmentVariable);

        return processCreator.start();
    }

    /**
     * ChildProcess 생성 헬퍼 메서드
     */
    private Process createChildProcess(ProcessConfig config, String[] args) throws IOException {
        processCreator.reset();

        if (config.getWorkingDirectory() != null) {
            processCreator.setWorkingDirectory(config.getWorkingDirectory());
        }

        config.getEnvironmentVariables().forEach(processCreator::setEnvironmentVariable);

        return processCreator.createJavaProcess(
                "com.study.process.creation.ChildProcess",
                args
        );
    }

    /**
     * 배치 프로세스 생성
     */
    public List<Process> createBatchProcesses(ProcessConfig config, int count)
            throws IOException {
        List<Process> processes = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            // 각 프로세스에 고유 ID 부여
            ProcessConfig individualConfig = new ProcessConfig()
                    .type(config.getType())
                    .workingDirectory(config.getWorkingDirectory())
                    .redirectErrorStream(config.isRedirectErrorStream())
                    .timeout(config.getTimeout());

            // 기존 파라미터 복사 및 인덱스 추가
            config.getParameters().forEach(individualConfig::parameter);
            individualConfig.parameter("index", String.valueOf(i));

            config.getEnvironmentVariables().forEach(individualConfig::environment);

            Process process = createProcess(individualConfig);
            processes.add(process);
        }

        return processes;
    }

    /**
     * 프로세스 체인 생성 (순차 실행)
     */
    public Process createProcessChain(List<ProcessConfig> configs)
            throws IOException, InterruptedException {
        Process lastProcess = null;

        for (ProcessConfig config : configs) {
            if (lastProcess != null) {
                // 이전 프로세스가 완료될 때까지 대기
                lastProcess.waitFor();

                // 이전 프로세스의 종료 코드가 0이 아니면 중단
                if (lastProcess.exitValue() != 0) {
                    throw new RuntimeException("Process chain interrupted. Exit code: "
                            + lastProcess.exitValue());
                }
            }

            lastProcess = createProcess(config);
        }

        return lastProcess;
    }

    /**
     * 활성 프로세스 조회
     */
    public Map<Long, Process> getActiveProcesses() {
        // 죽은 프로세스 제거
        managedProcesses.entrySet().removeIf(entry -> !entry.getValue().isAlive());

        // Process 객체만 반환
        Map<Long, Process> processes = new HashMap<>();
        managedProcesses.forEach((pid, managed) -> {
            if (managed.isAlive()) {
                processes.put(pid, managed.getProcess());
            }
        });
        return processes;
    }

    /**
     * 관리되는 프로세스 조회
     */
    public Map<Long, ManagedProcess> getManagedProcesses() {
        managedProcesses.entrySet().removeIf(entry -> !entry.getValue().isAlive());
        return new HashMap<>(managedProcesses);
    }

    /**
     * 특정 프로세스 조회
     */
    public Process getProcess(long pid) {
        ManagedProcess managed = managedProcesses.get(pid);
        return managed != null ? managed.getProcess() : null;
    }

    /**
     * 특정 관리 프로세스 조회
     */
    public ManagedProcess getManagedProcess(long pid) {
        return managedProcesses.get(pid);
    }

    /**
     * 프로세스 핸들 조회
     */
    public ProcessHandle getProcessHandle(long pid) {
        ManagedProcess managed = managedProcesses.get(pid);
        return managed != null ? managed.getHandle() : null;
    }

    /**
     * 모든 활성 프로세스 종료
     */
    public void destroyAllProcesses() {
        managedProcesses.values().forEach(managed -> {
            Process process = managed.getProcess();
            if (process.isAlive()) {
                process.destroyForcibly();
            }
        });
        managedProcesses.clear();
    }

    /**
     * 특정 프로세스 종료
     */
    public boolean destroyProcess(long pid) {
        ManagedProcess managed = managedProcesses.get(pid);
        if (managed != null) {
            Process process = managed.getProcess();
            if (process.isAlive()) {
                process.destroyForcibly();
                managedProcesses.remove(pid);
                return true;
            }
        }
        return false;
    }

    /**
     * 프로세스 통계 정보
     */
    public ProcessStatistics getStatistics() {
        return new ProcessStatistics(managedProcesses);
    }

    /**
     * 프로세스 통계 클래스
     */
    public static class ProcessStatistics {
        private final int totalActive;
        private final Map<ProcessType, Integer> typeCount;
        private final Map<String, Integer> userCount;

        public ProcessStatistics(Map<Long, ManagedProcess> managed) {
            this.totalActive = (int) managed.values().stream()
                    .filter(ManagedProcess::isAlive)
                    .count();

            this.typeCount = new HashMap<>();
            this.userCount = new HashMap<>();

            managed.values().forEach(m -> {
                if (m.isAlive()) {
                    // 타입별 카운트
                    ProcessType type = m.getConfig().getType();
                    typeCount.merge(type, 1, Integer::sum);

                    // 사용자별 카운트
                    String user = m.getHandle().info().user().orElse("Unknown");
                    userCount.merge(user, 1, Integer::sum);
                }
            });
        }

        public int getTotalActive() { return totalActive; }
        public Map<ProcessType, Integer> getTypeCount() { return typeCount; }
        public Map<String, Integer> getUserCount() { return userCount; }

        @Override
        public String toString() {
            return String.format("Active: %d, Types: %s, Users: %s",
                    totalActive, typeCount, userCount);
        }
    }
}