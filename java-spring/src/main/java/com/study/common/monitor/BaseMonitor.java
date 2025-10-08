package com.study.common.monitor;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 모니터링 기본 추상 클래스
 * 모든 모니터의 공통 기능을 정의합니다.
 */
public abstract class BaseMonitor {

    protected final String name;
    protected final AtomicBoolean isRunning;
    protected final AtomicLong startTime;
    protected final AtomicLong stopTime;
    protected volatile Thread monitorThread;

    /**
     * 생성자
     * @param name 모니터 이름
     */
    protected BaseMonitor(String name) {
        this.name = name;
        this.isRunning = new AtomicBoolean(false);
        this.startTime = new AtomicLong(0);
        this.stopTime = new AtomicLong(0);
    }

    /**
     * 모니터링 시작
     */
    public void start() {
        if (isRunning.compareAndSet(false, true)) {
            startTime.set(System.currentTimeMillis());
            stopTime.set(0);
            onStart();
            System.out.printf("[%s] Monitoring started%n", name);
        } else {
            System.out.printf("[%s] Already running%n", name);
        }
    }

    /**
     * 모니터링 중지
     */
    public void stop() {
        if (isRunning.compareAndSet(true, false)) {
            stopTime.set(System.currentTimeMillis());
            onStop();

            if (monitorThread != null) {
                monitorThread.interrupt();
            }

            System.out.printf("[%s] Monitoring stopped (Duration: %d ms)%n",
                    name, getDuration());
        } else {
            System.out.printf("[%s] Not running%n", name);
        }
    }

    /**
     * 모니터링 재시작
     */
    public void restart() {
        stop();
        try {
            Thread.sleep(100); // 잠시 대기
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        start();
    }

    /**
     * 모니터링 실행 여부 확인
     */
    public boolean isRunning() {
        return isRunning.get();
    }

    /**
     * 모니터 이름 조회
     */
    public String getName() {
        return name;
    }

    /**
     * 시작 시간 조회 (밀리초)
     */
    public long getStartTime() {
        return startTime.get();
    }

    /**
     * 중지 시간 조회 (밀리초)
     */
    public long getStopTime() {
        return stopTime.get();
    }

    /**
     * 실행 시간 조회 (밀리초)
     */
    public long getDuration() {
        long start = startTime.get();
        if (start == 0) {
            return 0;
        }

        long end = stopTime.get();
        if (end == 0) {
            // 아직 실행 중
            return System.currentTimeMillis() - start;
        }

        return end - start;
    }

    /**
     * 실행 시간 조회 (초)
     */
    public double getDurationSeconds() {
        return getDuration() / 1000.0;
    }

    /**
     * 상태 정보 출력
     */
    public void printStatus() {
        System.out.println(getStatusString());
    }

    /**
     * 상태 정보를 문자열로 반환
     */
    public String getStatusString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("[%s] Status:%n", name));
        sb.append(String.format("  Running: %s%n", isRunning()));
        sb.append(String.format("  Duration: %.2f seconds%n", getDurationSeconds()));

        if (isRunning()) {
            sb.append(String.format("  Started at: %d%n", startTime.get()));
        } else if (stopTime.get() > 0) {
            sb.append(String.format("  Stopped at: %d%n", stopTime.get()));
        }

        return sb.toString();
    }

    /**
     * 주기적 모니터링 시작 (백그라운드 스레드)
     * @param intervalMillis 모니터링 주기 (밀리초)
     */
    protected void startPeriodicMonitoring(long intervalMillis) {
        if (monitorThread != null && monitorThread.isAlive()) {
            System.out.printf("[%s] Periodic monitoring already running%n", name);
            return;
        }

        monitorThread = new Thread(() -> {
            System.out.printf("[%s] Periodic monitoring started (interval: %d ms)%n",
                    name, intervalMillis);

            while (isRunning() && !Thread.currentThread().isInterrupted()) {
                try {
                    collectMetrics();
                    Thread.sleep(intervalMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            System.out.printf("[%s] Periodic monitoring stopped%n", name);
        });

        monitorThread.setName(name + "-MonitorThread");
        monitorThread.setDaemon(true);
        monitorThread.start();
    }

    /**
     * 현재 시간 (밀리초)
     */
    protected long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    /**
     * 현재 시간 (나노초)
     */
    protected long nanoTime() {
        return System.nanoTime();
    }

    /**
     * 로그 출력
     */
    protected void log(String message) {
        System.out.printf("[%s] %s%n", name, message);
    }

    /**
     * 로그 출력 (포맷팅)
     */
    protected void logf(String format, Object... args) {
        System.out.printf("[%s] " + format + "%n", name, args);
    }

    // ========== 추상 메서드 (하위 클래스에서 구현) ==========

    /**
     * 모니터링 시작 시 호출
     * 하위 클래스에서 초기화 로직 구현
     */
    protected abstract void onStart();

    /**
     * 모니터링 중지 시 호출
     * 하위 클래스에서 정리 로직 구현
     */
    protected abstract void onStop();

    /**
     * 메트릭 수집
     * 주기적으로 호출되며, 하위 클래스에서 수집 로직 구현
     */
    protected abstract void collectMetrics();

    /**
     * 수집된 데이터 조회
     * 하위 클래스에서 데이터 반환 로직 구현
     */
    public abstract Object getCollectedData();

    /**
     * 수집된 데이터 초기화
     * 하위 클래스에서 초기화 로직 구현
     */
    public abstract void clearData();

    /**
     * 리소스 정리
     * 모니터 종료 시 호출
     */
    public void cleanup() {
        if (isRunning()) {
            stop();
        }

        if (monitorThread != null && monitorThread.isAlive()) {
            monitorThread.interrupt();
            try {
                monitorThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Shutdown Hook 등록
     * JVM 종료 시 자동으로 모니터 정리
     */
    protected void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.printf("[%s] Shutdown hook triggered%n", name);
            cleanup();
        }));
    }

    /**
     * 간단한 테스트를 위한 예제 구현
     */
    public static class SimpleMonitor extends BaseMonitor {
        private int collectCount = 0;

        public SimpleMonitor(String name) {
            super(name);
        }

        @Override
        protected void onStart() {
            log("SimpleMonitor started");
            collectCount = 0;
            startPeriodicMonitoring(1000); // 1초마다 수집
        }

        @Override
        protected void onStop() {
            log("SimpleMonitor stopped");
        }

        @Override
        protected void collectMetrics() {
            collectCount++;
            logf("Collecting metrics... (count: %d)", collectCount);
        }

        @Override
        public Object getCollectedData() {
            return collectCount;
        }

        @Override
        public void clearData() {
            collectCount = 0;
        }
    }

    /**
     * 테스트 메인 메서드
     */
    public static void main(String[] args) {
        System.out.println("=== BaseMonitor 테스트 ===\n");

        SimpleMonitor monitor = new SimpleMonitor("TestMonitor");

        // 1. 시작
        System.out.println("1. 모니터 시작:");
        monitor.start();

        try {
            // 2. 3초 동안 실행
            System.out.println("\n2. 3초 대기...");
            Thread.sleep(3000);

            // 3. 상태 확인
            System.out.println("\n3. 상태 확인:");
            monitor.printStatus();
            System.out.println("수집 횟수: " + monitor.getCollectedData());

            // 4. 재시작
            System.out.println("\n4. 재시작:");
            monitor.restart();

            // 5. 2초 더 실행
            System.out.println("\n5. 2초 더 대기...");
            Thread.sleep(2000);

            // 6. 중지
            System.out.println("\n6. 모니터 중지:");
            monitor.stop();

            // 7. 최종 상태
            System.out.println("\n7. 최종 상태:");
            monitor.printStatus();
            System.out.println("총 수집 횟수: " + monitor.getCollectedData());

            // 8. 정리
            System.out.println("\n8. 정리:");
            monitor.cleanup();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("테스트 중단됨");
        }

        System.out.println("\n테스트 완료!");
    }
}