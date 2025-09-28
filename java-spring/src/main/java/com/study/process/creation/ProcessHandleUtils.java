package com.study.process.creation;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.stream.*;

/**
 * ProcessHandle 유틸리티 클래스
 * ProcessFactory와 함께 사용되는 헬퍼 기능 제공
 */
public class ProcessHandleUtils {

    /**
     * ProcessHandle 래퍼 - ProcessFactory 내부에서 사용
     */
    public static class EnhancedHandle {
        private final ProcessHandle handle;
        private final Process process;
        private final Map<String, Object> metadata;
        private final Instant creationTime;

        public EnhancedHandle(Process process) {
            this.process = process;
            this.handle = process.toHandle();
            this.metadata = new ConcurrentHashMap<>();
            this.creationTime = Instant.now();
        }

        public EnhancedHandle(ProcessHandle handle) {
            this.process = null;
            this.handle = handle;
            this.metadata = new ConcurrentHashMap<>();
            this.creationTime = Instant.now();
        }

        public long pid() { return handle.pid(); }
        public boolean isAlive() { return handle.isAlive(); }

        public String getCommand() {
            return handle.info().command().orElse("Unknown");
        }

        public Duration getCpuTime() {
            return handle.info().totalCpuDuration().orElse(Duration.ZERO);
        }

        public void setMetadata(String key, Object value) {
            metadata.put(key, value);
        }

        public Object getMetadata(String key) {
            return metadata.get(key);
        }

        public Process getProcess() {
            return process;
        }

        public ProcessHandle getHandle() {
            return handle;
        }
    }

    /**
     * 프로세스 검색 유틸리티
     */
    public static class ProcessFinder {

        public static List<ProcessHandle> findByCommand(String command) {
            return ProcessHandle.allProcesses()
                    .filter(h -> h.info().command()
                            .map(cmd -> cmd.contains(command))
                            .orElse(false))
                    .collect(Collectors.toList());
        }

        public static List<ProcessHandle> findByUser(String user) {
            return ProcessHandle.allProcesses()
                    .filter(h -> user.equals(h.info().user().orElse("")))
                    .collect(Collectors.toList());
        }

        public static Optional<ProcessHandle> findByPid(long pid) {
            return ProcessHandle.of(pid);
        }

        public static List<ProcessHandle> getChildren(long parentPid) {
            return ProcessHandle.of(parentPid)
                    .map(parent -> parent.children().collect(Collectors.toList()))
                    .orElse(Collections.emptyList());
        }
    }

    /**
     * 프로세스 비교기
     */
    public static class Comparators {
        public static Comparator<ProcessHandle> byPid() {
            return Comparator.comparingLong(ProcessHandle::pid);
        }

        public static Comparator<ProcessHandle> byCpuTime() {
            return Comparator.comparing(h ->
                    h.info().totalCpuDuration().orElse(Duration.ZERO));
        }

        public static Comparator<ProcessHandle> byStartTime() {
            return Comparator.comparing(h ->
                    h.info().startInstant().orElse(Instant.EPOCH));
        }
    }

    /**
     * 프로세스 필터
     */
    public static class Filters {
        public static Predicate<ProcessHandle> isAlive() {
            return ProcessHandle::isAlive;
        }

        public static Predicate<ProcessHandle> hasCommand(String cmd) {
            return h -> h.info().command()
                    .map(c -> c.contains(cmd))
                    .orElse(false);
        }

        public static Predicate<ProcessHandle> cpuTimeGreaterThan(Duration duration) {
            return h -> h.info().totalCpuDuration()
                    .map(cpu -> cpu.compareTo(duration) > 0)
                    .orElse(false);
        }

        public static Predicate<ProcessHandle> startedAfter(Instant time) {
            return h -> h.info().startInstant()
                    .map(start -> start.isAfter(time))
                    .orElse(false);
        }
    }

    /**
     * 프로세스 정보 수집
     */
    public static class ProcessInfo {
        private final long pid;
        private final String command;
        private final String user;
        private final Duration cpuTime;
        private final Instant startTime;
        private final boolean alive;

        public ProcessInfo(ProcessHandle handle) {
            ProcessHandle.Info info = handle.info();
            this.pid = handle.pid();
            this.command = info.command().orElse("Unknown");
            this.user = info.user().orElse("Unknown");
            this.cpuTime = info.totalCpuDuration().orElse(Duration.ZERO);
            this.startTime = info.startInstant().orElse(Instant.EPOCH);
            this.alive = handle.isAlive();
        }

        // Getters
        public long getPid() { return pid; }
        public String getCommand() { return command; }
        public String getUser() { return user; }
        public Duration getCpuTime() { return cpuTime; }
        public Instant getStartTime() { return startTime; }
        public boolean isAlive() { return alive; }

        @Override
        public String toString() {
            return String.format("Process[pid=%d, cmd=%s, user=%s, cpu=%dms]",
                    pid, command, user, cpuTime.toMillis());
        }
    }

    /**
     * 프로세스 작업 유틸리티
     */
    public static void killProcess(long pid) {
        ProcessHandle.of(pid).ifPresent(ProcessHandle::destroyForcibly);
    }

    public static void killProcessTree(long pid) {
        ProcessHandle.of(pid).ifPresent(parent -> {
            parent.descendants().forEach(ProcessHandle::destroyForcibly);
            parent.destroyForcibly();
        });
    }

    public static boolean waitForProcess(long pid, long timeout, TimeUnit unit)
            throws InterruptedException {
        Optional<ProcessHandle> handle = ProcessHandle.of(pid);
        if (handle.isEmpty()) {
            return true;
        }

        try {
            return handle.get().onExit().get(timeout, unit) != null;
        } catch (ExecutionException | TimeoutException e) {
            return false;
        }
    }

    /**
     * 프로세스 트리 간단 출력
     */
    public static void printProcessTree(ProcessHandle root, int indent) {
        String indentStr = " ".repeat(indent);
        ProcessHandle.Info info = root.info();

        System.out.printf("%s[%d] %s\n",
                indentStr,
                root.pid(),
                info.command().orElse("Unknown"));

        root.children().forEach(child -> printProcessTree(child, indent + 2));
    }
}