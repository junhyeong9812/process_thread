package com.study.common.util;

import java.lang.management.*;
import java.util.HashMap;
import java.util.Map;

/**
 * 시스템 정보를 조회하는 유틸리티 클래스
 * OS, JVM, 하드웨어 정보를 수집합니다.
 */
public class SystemInfo {

    private static final OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
    private static final RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
    private static final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    private static final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();

    /**
     * 모든 시스템 정보를 Map으로 반환
     */
    public static Map<String, Object> getAllInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("os", getOSInfo());
        info.put("jvm", getJVMInfo());
        info.put("hardware", getHardwareInfo());
        info.put("memory", getMemoryInfo());
        info.put("thread", getThreadInfo());
        return info;
    }

    /**
     * 운영체제 정보
     */
    public static Map<String, String> getOSInfo() {
        Map<String, String> osInfo = new HashMap<>();
        osInfo.put("name", osBean.getName());
        osInfo.put("version", osBean.getVersion());
        osInfo.put("arch", osBean.getArch());
        return osInfo;
    }

    /**
     * JVM 정보
     */
    public static Map<String, String> getJVMInfo() {
        Map<String, String> jvmInfo = new HashMap<>();
        jvmInfo.put("name", runtimeBean.getVmName());
        jvmInfo.put("vendor", runtimeBean.getVmVendor());
        jvmInfo.put("version", runtimeBean.getVmVersion());
        jvmInfo.put("javaVersion", System.getProperty("java.version"));
        jvmInfo.put("javaHome", System.getProperty("java.home"));
        return jvmInfo;
    }

    /**
     * 하드웨어 정보
     */
    public static Map<String, Object> getHardwareInfo() {
        Map<String, Object> hwInfo = new HashMap<>();
        hwInfo.put("availableProcessors", Runtime.getRuntime().availableProcessors());
        hwInfo.put("systemLoadAverage", osBean.getSystemLoadAverage());

        // com.sun.management.OperatingSystemMXBean으로 캐스팅 시도
        if (osBean instanceof com.sun.management.OperatingSystemMXBean sunOsBean) {
            hwInfo.put("totalPhysicalMemory", sunOsBean.getTotalMemorySize());
            hwInfo.put("freePhysicalMemory", sunOsBean.getFreeMemorySize());
            hwInfo.put("totalSwapSpace", sunOsBean.getTotalSwapSpaceSize());
            hwInfo.put("freeSwapSpace", sunOsBean.getFreeSwapSpaceSize());
            hwInfo.put("processCpuLoad", sunOsBean.getProcessCpuLoad());
            hwInfo.put("systemCpuLoad", sunOsBean.getCpuLoad());
        }

        return hwInfo;
    }

    /**
     * 메모리 정보
     */
    public static Map<String, Object> getMemoryInfo() {
        Map<String, Object> memInfo = new HashMap<>();

        // Heap 메모리
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        Map<String, Long> heapInfo = new HashMap<>();
        heapInfo.put("init", heapUsage.getInit());
        heapInfo.put("used", heapUsage.getUsed());
        heapInfo.put("committed", heapUsage.getCommitted());
        heapInfo.put("max", heapUsage.getMax());
        memInfo.put("heap", heapInfo);

        // Non-Heap 메모리
        MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();
        Map<String, Long> nonHeapInfo = new HashMap<>();
        nonHeapInfo.put("init", nonHeapUsage.getInit());
        nonHeapInfo.put("used", nonHeapUsage.getUsed());
        nonHeapInfo.put("committed", nonHeapUsage.getCommitted());
        nonHeapInfo.put("max", nonHeapUsage.getMax());
        memInfo.put("nonHeap", nonHeapInfo);

        return memInfo;
    }

    /**
     * 스레드 정보
     */
    public static Map<String, Object> getThreadInfo() {
        Map<String, Object> threadInfo = new HashMap<>();
        threadInfo.put("threadCount", threadBean.getThreadCount());
        threadInfo.put("peakThreadCount", threadBean.getPeakThreadCount());
        threadInfo.put("totalStartedThreadCount", threadBean.getTotalStartedThreadCount());
        threadInfo.put("daemonThreadCount", threadBean.getDaemonThreadCount());
        return threadInfo;
    }

    /**
     * CPU 코어 수
     */
    public static int getAvailableProcessors() {
        return Runtime.getRuntime().availableProcessors();
    }

    /**
     * 시스템 로드 평균 (1분)
     */
    public static double getSystemLoadAverage() {
        return osBean.getSystemLoadAverage();
    }

    /**
     * JVM 가동 시간 (밀리초)
     */
    public static long getUptime() {
        return runtimeBean.getUptime();
    }

    /**
     * JVM 시작 시간 (타임스탬프)
     */
    public static long getStartTime() {
        return runtimeBean.getStartTime();
    }

    /**
     * 사용 가능한 메모리 (바이트)
     */
    public static long getFreeMemory() {
        return Runtime.getRuntime().freeMemory();
    }

    /**
     * 전체 메모리 (바이트)
     */
    public static long getTotalMemory() {
        return Runtime.getRuntime().totalMemory();
    }

    /**
     * 최대 메모리 (바이트)
     */
    public static long getMaxMemory() {
        return Runtime.getRuntime().maxMemory();
    }

    /**
     * 현재 사용 중인 메모리 (바이트)
     */
    public static long getUsedMemory() {
        return getTotalMemory() - getFreeMemory();
    }

    /**
     * 메모리 사용률 (%)
     */
    public static double getMemoryUsagePercentage() {
        long total = getTotalMemory();
        long used = getUsedMemory();
        return (double) used / total * 100.0;
    }

    /**
     * 시스템 정보를 포맷팅된 문자열로 출력
     */
    public static String getFormattedInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== System Information ===\n");

        sb.append("\n[Operating System]\n");
        Map<String, String> osInfo = getOSInfo();
        osInfo.forEach((key, value) -> sb.append(String.format("  %s: %s\n", key, value)));

        sb.append("\n[JVM]\n");
        Map<String, String> jvmInfo = getJVMInfo();
        jvmInfo.forEach((key, value) -> sb.append(String.format("  %s: %s\n", key, value)));

        sb.append("\n[Hardware]\n");
        sb.append(String.format("  CPU Cores: %d\n", getAvailableProcessors()));
        sb.append(String.format("  System Load: %.2f\n", getSystemLoadAverage()));

        sb.append("\n[Memory]\n");
        sb.append(String.format("  Used: %d MB\n", getUsedMemory() / 1024 / 1024));
        sb.append(String.format("  Free: %d MB\n", getFreeMemory() / 1024 / 1024));
        sb.append(String.format("  Total: %d MB\n", getTotalMemory() / 1024 / 1024));
        sb.append(String.format("  Max: %d MB\n", getMaxMemory() / 1024 / 1024));
        sb.append(String.format("  Usage: %.2f%%\n", getMemoryUsagePercentage()));

        sb.append("\n[Threads]\n");
        Map<String, Object> threadInfo = getThreadInfo();
        threadInfo.forEach((key, value) -> sb.append(String.format("  %s: %s\n", key, value)));

        return sb.toString();
    }

    /**
     * 테스트 메인 메서드
     */
    public static void main(String[] args) {
        System.out.println(getFormattedInfo());

        // 상세 정보 출력
        System.out.println("\n=== Detailed Hardware Info ===");
        Map<String, Object> hwInfo = getHardwareInfo();
        hwInfo.forEach((key, value) -> {
            if (value instanceof Long longValue) {
                System.out.printf("%s: %d MB\n", key, longValue / 1024 / 1024);
            } else {
                System.out.printf("%s: %s\n", key, value);
            }
        });
    }
}