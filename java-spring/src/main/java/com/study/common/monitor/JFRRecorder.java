package com.study.common.monitor;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Java Flight Recorder 래퍼 클래스
 * JFR을 사용한 상세 프로파일링 기능을 제공합니다.
 */
public class JFRRecorder {

    private Recording recording;
    private final String name;
    private Path outputPath;
    private boolean isRecording;

    /**
     * 생성자
     */
    public JFRRecorder(String name) {
        this.name = name;
        this.isRecording = false;
    }

    /**
     * 기본 설정으로 녹화 시작
     */
    public void startRecording() {
        startRecording(null, null);
    }

    /**
     * 녹화 시작
     * @param maxAge 최대 보관 시간 (null이면 무제한)
     * @param maxSize 최대 파일 크기 (null이면 무제한)
     */
    public void startRecording(Duration maxAge, Long maxSize) {
        if (isRecording) {
            System.out.println("[JFR] Recording is already running");
            return;
        }

        try {
            recording = new Recording();
            recording.setName(name);

            // 설정
            if (maxAge != null) {
                recording.setMaxAge(maxAge);
            }
            if (maxSize != null) {
                recording.setMaxSize(maxSize);
            }

            // 프로파일 설정 (default 또는 profile)
            // recording.setSettings(Map.of("profile", "profile")); // 상세 프로파일링

            recording.start();
            isRecording = true;

            System.out.printf("[JFR] Recording started: %s%n", name);

        } catch (Exception e) {
            System.err.println("[JFR] Failed to start recording: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 녹화 중지 및 파일 저장
     */
    public Path stopRecording() {
        return stopRecording(null);
    }

    /**
     * 녹화 중지 및 파일 저장
     * @param outputPath 출력 파일 경로 (null이면 자동 생성)
     */
    public Path stopRecording(Path outputPath) {
        if (!isRecording || recording == null) {
            System.out.println("[JFR] No active recording");
            return null;
        }

        try {
            // 출력 경로 설정
            if (outputPath == null) {
                String timestamp = Instant.now().toString().replace(":", "-");
                String fileName = String.format("jfr-%s-%s.jfr", name, timestamp);
                outputPath = Paths.get(fileName);
            }

            // 녹화 중지 및 저장
            recording.dump(outputPath);
            recording.close();

            this.outputPath = outputPath;
            isRecording = false;

            System.out.printf("[JFR] Recording stopped and saved to: %s%n", outputPath);
            System.out.printf("[JFR] File size: %d bytes%n", Files.size(outputPath));

            return outputPath;

        } catch (IOException e) {
            System.err.println("[JFR] Failed to stop recording: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 녹화 일시 중지
     */
    public void pauseRecording() {
        if (recording != null && isRecording) {
            // JFR은 일시중지 기능이 없으므로 stop 후 다시 start 필요
            System.out.println("[JFR] Pause not directly supported. Use stop and start instead.");
        }
    }

    /**
     * 녹화 중인지 확인
     */
    public boolean isRecording() {
        return isRecording;
    }

    /**
     * 현재 녹화 객체 조회
     */
    public Recording getRecording() {
        return recording;
    }

    /**
     * 마지막 저장된 파일 경로
     */
    public Path getOutputPath() {
        return outputPath;
    }

    /**
     * JFR 파일 분석
     */
    public RecordingAnalysis analyzeRecording(Path jfrFile) {
        if (!Files.exists(jfrFile)) {
            throw new IllegalArgumentException("JFR file does not exist: " + jfrFile);
        }

        RecordingAnalysis analysis = new RecordingAnalysis();

        try (RecordingFile recordingFile = new RecordingFile(jfrFile)) {
            System.out.printf("[JFR] Analyzing recording: %s%n", jfrFile);

            while (recordingFile.hasMoreEvents()) {
                RecordedEvent event = recordingFile.readEvent();
                analysis.addEvent(event);
            }

            System.out.printf("[JFR] Analysis complete. Total events: %d%n",
                    analysis.getTotalEvents());

        } catch (IOException e) {
            System.err.println("[JFR] Failed to analyze recording: " + e.getMessage());
            e.printStackTrace();
        }

        return analysis;
    }

    /**
     * 녹화 분석 결과 클래스
     */
    public static class RecordingAnalysis {
        private final Map<String, Integer> eventCounts;
        private final List<RecordedEvent> allEvents;
        private int totalEvents;

        public RecordingAnalysis() {
            this.eventCounts = new HashMap<>();
            this.allEvents = new ArrayList<>();
            this.totalEvents = 0;
        }

        void addEvent(RecordedEvent event) {
            String eventType = event.getEventType().getName();
            eventCounts.merge(eventType, 1, Integer::sum);
            allEvents.add(event);
            totalEvents++;
        }

        public int getTotalEvents() {
            return totalEvents;
        }

        public Map<String, Integer> getEventCounts() {
            return new HashMap<>(eventCounts);
        }

        public List<RecordedEvent> getAllEvents() {
            return new ArrayList<>(allEvents);
        }

        public List<RecordedEvent> getEventsByType(String eventType) {
            List<RecordedEvent> filtered = new ArrayList<>();
            for (RecordedEvent event : allEvents) {
                if (event.getEventType().getName().equals(eventType)) {
                    filtered.add(event);
                }
            }
            return filtered;
        }

        public String getSummary() {
            StringBuilder sb = new StringBuilder();
            sb.append("=== JFR Recording Analysis ===%n");
            sb.append(String.format("Total Events: %d%n%n", totalEvents));

            sb.append("Event Type Distribution:%n");

            // 이벤트 타입별 정렬
            List<Map.Entry<String, Integer>> sortedEntries = new ArrayList<>(eventCounts.entrySet());
            sortedEntries.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));

            int displayCount = Math.min(20, sortedEntries.size());
            for (int i = 0; i < displayCount; i++) {
                Map.Entry<String, Integer> entry = sortedEntries.get(i);
                double percentage = (double) entry.getValue() / totalEvents * 100;
                sb.append(String.format("  %-50s : %6d (%.2f%%)%n",
                        entry.getKey(), entry.getValue(), percentage));
            }

            if (sortedEntries.size() > displayCount) {
                sb.append(String.format("  ... and %d more event types%n",
                        sortedEntries.size() - displayCount));
            }

            return sb.toString();
        }

        public void printSummary() {
            System.out.println(getSummary());
        }
    }

    /**
     * 간단한 프로파일링 유틸리티
     */
    public static class QuickProfiler {
        private final JFRRecorder recorder;

        public QuickProfiler(String name) {
            this.recorder = new JFRRecorder(name);
        }

        /**
         * 코드 블록 프로파일링
         */
        public <T> T profile(String taskName, java.util.function.Supplier<T> task) {
            recorder.startRecording();

            try {
                return task.get();
            } finally {
                Path jfrFile = recorder.stopRecording();
                System.out.printf("[Profile] %s completed. Recording saved to: %s%n",
                        taskName, jfrFile);
            }
        }

        /**
         * 코드 블록 프로파일링 (반환값 없음)
         */
        public void profile(String taskName, Runnable task) {
            recorder.startRecording();

            try {
                task.run();
            } finally {
                Path jfrFile = recorder.stopRecording();
                System.out.printf("[Profile] %s completed. Recording saved to: %s%n",
                        taskName, jfrFile);
            }
        }
    }

    /**
     * JFR 사용 가능 여부 확인
     */
    public static boolean isJFRAvailable() {
        try {
            Class.forName("jdk.jfr.Recording");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * 정리
     */
    public void cleanup() {
        if (isRecording && recording != null) {
            stopRecording();
        }
    }

    /**
     * 테스트 메인 메서드
     */
    public static void main(String[] args) {
        System.out.println("=== JFRRecorder 테스트 ===\n");

        // JFR 사용 가능 여부 확인
        if (!isJFRAvailable()) {
            System.err.println("JFR is not available in this JVM");
            return;
        }

        JFRRecorder recorder = new JFRRecorder("TestRecording");

        try {
            // 1. 녹화 시작
            System.out.println("1. JFR 녹화 시작");
            recorder.startRecording(Duration.ofMinutes(5), 100 * 1024 * 1024L); // 5분, 100MB

            // 2. 일부 작업 수행
            System.out.println("\n2. 작업 수행 중 (5초)...");
            for (int i = 0; i < 5; i++) {
                // CPU 작업
                long sum = 0;
                for (int j = 0; j < 10_000_000; j++) {
                    sum += j;
                }

                // 메모리 할당
                List<byte[]> memory = new ArrayList<>();
                for (int j = 0; j < 10; j++) {
                    memory.add(new byte[1024 * 1024]); // 1MB
                }

                // GC 유도
                System.gc();

                Thread.sleep(1000);
                System.out.printf("  작업 진행: %d/5%n", i + 1);
            }

            // 3. 녹화 중지 및 저장
            System.out.println("\n3. JFR 녹화 중지 및 저장");
            Path jfrFile = recorder.stopRecording();

            if (jfrFile != null && Files.exists(jfrFile)) {
                // 4. 녹화 파일 분석
                System.out.println("\n4. JFR 파일 분석");
                RecordingAnalysis analysis = recorder.analyzeRecording(jfrFile);
                analysis.printSummary();

                // 5. 파일 삭제 (테스트 후 정리)
                System.out.println("\n5. 테스트 파일 정리");
                Files.deleteIfExists(jfrFile);
                System.out.println("정리 완료");
            }

            // 6. QuickProfiler 테스트
            System.out.println("\n6. QuickProfiler 테스트");
            QuickProfiler profiler = new QuickProfiler("QuickTest");

            profiler.profile("SimpleTask", () -> {
                long sum = 0;
                for (int i = 0; i < 100_000_000; i++) {
                    sum += i;
                }
                System.out.println("Task completed with sum: " + sum);
            });

        } catch (Exception e) {
            System.err.println("테스트 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        } finally {
            recorder.cleanup();
        }

        System.out.println("\n테스트 완료!");
        System.out.println("\n참고: JFR 파일은 VisualVM, JDK Mission Control 등으로 분석할 수 있습니다.");
    }
}