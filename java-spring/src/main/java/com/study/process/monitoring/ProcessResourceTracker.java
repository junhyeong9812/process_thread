package com.study.process.monitoring;

import java.lang.management.*;
import com.sun.management.OperatingSystemMXBean;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * CPU, 메모리 사용량 추적
 * 프로세스별 리소스 사용량을 실시간으로 모니터링
 */
public class ProcessResourceTracker {

    private final Map<Long, ResourceMetrics> processMetrics;
    private final ScheduledExecutorService scheduler;
    private final OperatingSystemMXBean osBean;
    private final MemoryMXBean memoryBean;
    private final AtomicBoolean tracking;
    private final long samplingInterval;

    public static class ResourceMetrics {
        private final long pid;
        private final Deque<ResourceSample> samples;
        private final int maxSamples;

        // 현재 메트릭
        private double cpuUsage;
        private long memoryUsage;
        private long virtualMemory;
        private int threadCount;
        private int fileDescriptorCount;

        // 통계
        private double avgCpuUsage;
        private double maxCpuUsage;
        private long avgMemoryUsage;
        private long maxMemoryUsage;

        public ResourceMetrics(long pid, int maxSamples) {
            this.pid = pid;
            this.samples = new LinkedList<>();
            this.maxSamples = maxSamples;
        }

        public void addSample(ResourceSample sample) {
            samples.offer(sample);
            if (samples.size() > maxSamples) {
                samples.poll();
            }

            updateStatistics();
        }

        private void updateStatistics() {
            if (samples.isEmpty()) return;

            avgCpuUsage = samples.stream()
                    .mapToDouble(s -> s.cpuUsage)
                    .average()
                    .orElse(0.0);

            maxCpuUsage = samples.stream()
                    .mapToDouble(s -> s.cpuUsage)
                    .max()
                    .orElse(0.0);

            avgMemoryUsage = (long) samples.stream()
                    .mapToLong(s -> s.memoryUsage)
                    .average()
                    .orElse(0.0);

            maxMemoryUsage = samples.stream()
                    .mapToLong(s -> s.memoryUsage)
                    .max()
                    .orElse(0L);
        }

        public double getCpuUsage() { return cpuUsage; }
        public long getMemoryUsage() { return memoryUsage; }
        public double getAvgCpuUsage() { return avgCpuUsage; }
        public double getMaxCpuUsage() { return maxCpuUsage; }
        public long getAvgMemoryUsage() { return avgMemoryUsage; }
        public long getMaxMemoryUsage() { return maxMemoryUsage; }
        public List<ResourceSample> getSamples() { return new ArrayList<>(samples); }
    }

    public static class ResourceSample {
        private final long timestamp;
        private final double cpuUsage;
        private final long memoryUsage;
        private final long virtualMemory;
        private final int threadCount;

        public ResourceSample(double cpuUsage, long memoryUsage, long virtualMemory, int threadCount) {
            this.timestamp = System.currentTimeMillis();
            this.cpuUsage = cpuUsage;
            this.memoryUsage = memoryUsage;
            this.virtualMemory = virtualMemory;
            this.threadCount = threadCount;
        }

        public long getTimestamp() { return timestamp; }
        public double getCpuUsage() { return cpuUsage; }
        public long getMemoryUsage() { return memoryUsage; }
        public long getVirtualMemory() { return virtualMemory; }
        public int getThreadCount() { return threadCount; }
    }

    public ProcessResourceTracker(long samplingInterval, TimeUnit unit) {
        this.processMetrics = new ConcurrentHashMap<>();
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        this.memoryBean = ManagementFactory.getMemoryMXBean();
        this.tracking = new AtomicBoolean(false);
        this.samplingInterval = unit.toMillis(samplingInterval);
    }

    public void startTracking() {
        if (tracking.getAndSet(true)) {
            return;
        }

        scheduler.scheduleAtFixedRate(this::collectMetrics, 0, samplingInterval, TimeUnit.MILLISECONDS);
    }

    public void stopTracking() {
        tracking.set(false);
        scheduler.shutdown();
    }

    public void trackProcess(long pid) {
        processMetrics.computeIfAbsent(pid, k -> new ResourceMetrics(k, 100));
    }

    public void untrackProcess(long pid) {
        processMetrics.remove(pid);
    }

    private void collectMetrics() {
        for (Map.Entry<Long, ResourceMetrics> entry : processMetrics.entrySet()) {
            long pid = entry.getKey();
            ResourceMetrics metrics = entry.getValue();

            ProcessHandle.of(pid).ifPresent(handle -> {
                if (handle.isAlive()) {
                    ResourceSample sample = collectProcessMetrics(handle);
                    metrics.addSample(sample);

                    // 현재 메트릭 업데이트
                    metrics.cpuUsage = sample.cpuUsage;
                    metrics.memoryUsage = sample.memoryUsage;
                    metrics.virtualMemory = sample.virtualMemory;
                    metrics.threadCount = sample.threadCount;
                }
            });
        }
    }

    private ResourceSample collectProcessMetrics(ProcessHandle handle) {
        ProcessHandle.Info info = handle.info();

        // CPU 사용률 계산
        double cpuUsage = 0.0;
        if (info.totalCpuDuration().isPresent()) {
            cpuUsage = info.totalCpuDuration().get().toMillis() / 1000.0;
        }

        // 메모리 사용량 (JVM 프로세스인 경우)
        long memoryUsage = 0;
        long virtualMemory = 0;
        if (handle.pid() == ProcessHandle.current().pid()) {
            MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
            MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();
            memoryUsage = heapUsage.getUsed() + nonHeapUsage.getUsed();
            virtualMemory = heapUsage.getCommitted() + nonHeapUsage.getCommitted();
        }

        // 스레드 수
        int threadCount = handle.descendants().mapToInt(p -> 1).sum() + 1;

        return new ResourceSample(cpuUsage, memoryUsage, virtualMemory, threadCount);
    }

    public ResourceMetrics getMetrics(long pid) {
        return processMetrics.get(pid);
    }

    public Map<Long, ResourceMetrics> getAllMetrics() {
        return new HashMap<>(processMetrics);
    }

    public SystemResourceInfo getSystemResourceInfo() {
        return new SystemResourceInfo();
    }

    public class SystemResourceInfo {
        private final double systemCpuLoad;
        private final double processCpuLoad;
        private final long totalMemory;
        private final long freeMemory;
        private final long usedMemory;
        private final int availableProcessors;

        public SystemResourceInfo() {
            this.systemCpuLoad = osBean.getSystemCpuLoad() * 100;
            this.processCpuLoad = osBean.getProcessCpuLoad() * 100;
            this.totalMemory = osBean.getTotalPhysicalMemorySize();
            this.freeMemory = osBean.getFreePhysicalMemorySize();
            this.usedMemory = totalMemory - freeMemory;
            this.availableProcessors = osBean.getAvailableProcessors();
        }

        @Override
        public String toString() {
            return String.format("System: CPU=%.2f%%, Process CPU=%.2f%%, Memory=%d/%d MB, Cores=%d",
                    systemCpuLoad, processCpuLoad, usedMemory / 1024 / 1024,
                    totalMemory / 1024 / 1024, availableProcessors);
        }
    }
}