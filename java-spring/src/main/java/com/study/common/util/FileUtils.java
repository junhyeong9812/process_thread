package com.study.common.util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 파일 작업 유틸리티 클래스
 * 파일 읽기/쓰기, 디렉토리 관리 등을 제공합니다.
 */
public class FileUtils {

    /**
     * 파일 읽기 (전체 내용을 String으로)
     */
    public static String readFile(String filePath) throws IOException {
        return Files.readString(Path.of(filePath), StandardCharsets.UTF_8);
    }

    /**
     * 파일 읽기 (Path 객체 사용)
     */
    public static String readFile(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    /**
     * 파일 읽기 (줄 단위로 List 반환)
     */
    public static List<String> readLines(String filePath) throws IOException {
        return Files.readAllLines(Path.of(filePath), StandardCharsets.UTF_8);
    }

    /**
     * 파일 읽기 (줄 단위로 List 반환, Path 사용)
     */
    public static List<String> readLines(Path path) throws IOException {
        return Files.readAllLines(path, StandardCharsets.UTF_8);
    }

    /**
     * 파일 쓰기 (내용 덮어쓰기)
     */
    public static void writeFile(String filePath, String content) throws IOException {
        Files.writeString(Path.of(filePath), content, StandardCharsets.UTF_8);
    }

    /**
     * 파일 쓰기 (Path 객체 사용)
     */
    public static void writeFile(Path path, String content) throws IOException {
        Files.writeString(path, content, StandardCharsets.UTF_8);
    }

    /**
     * 파일 쓰기 (줄 단위로)
     */
    public static void writeLines(String filePath, List<String> lines) throws IOException {
        Files.write(Path.of(filePath), lines, StandardCharsets.UTF_8);
    }

    /**
     * 파일에 내용 추가
     */
    public static void appendFile(String filePath, String content) throws IOException {
        Files.writeString(
                Path.of(filePath),
                content,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
        );
    }

    /**
     * 파일에 줄 추가
     */
    public static void appendLine(String filePath, String line) throws IOException {
        appendFile(filePath, line + System.lineSeparator());
    }

    /**
     * 파일 복사
     */
    public static void copyFile(String source, String destination) throws IOException {
        Files.copy(
                Path.of(source),
                Path.of(destination),
                StandardCopyOption.REPLACE_EXISTING
        );
    }

    /**
     * 파일 복사 (Path 객체 사용)
     */
    public static void copyFile(Path source, Path destination) throws IOException {
        Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * 파일 이동
     */
    public static void moveFile(String source, String destination) throws IOException {
        Files.move(
                Path.of(source),
                Path.of(destination),
                StandardCopyOption.REPLACE_EXISTING
        );
    }

    /**
     * 파일 삭제
     */
    public static boolean deleteFile(String filePath) throws IOException {
        return Files.deleteIfExists(Path.of(filePath));
    }

    /**
     * 파일 삭제 (Path 객체 사용)
     */
    public static boolean deleteFile(Path path) throws IOException {
        return Files.deleteIfExists(path);
    }

    /**
     * 파일 존재 여부 확인
     */
    public static boolean exists(String filePath) {
        return Files.exists(Path.of(filePath));
    }

    /**
     * 파일 존재 여부 확인 (Path 사용)
     */
    public static boolean exists(Path path) {
        return Files.exists(path);
    }

    /**
     * 디렉토리 생성
     */
    public static void createDirectory(String dirPath) throws IOException {
        Files.createDirectories(Path.of(dirPath));
    }

    /**
     * 디렉토리 생성 (Path 사용)
     */
    public static void createDirectory(Path path) throws IOException {
        Files.createDirectories(path);
    }

    /**
     * 디렉토리 삭제 (재귀적으로)
     */
    public static void deleteDirectory(String dirPath) throws IOException {
        Path path = Path.of(dirPath);
        if (Files.exists(path)) {
            try (Stream<Path> walk = Files.walk(path)) {
                walk.sorted((a, b) -> b.compareTo(a)) // 역순 정렬 (하위부터 삭제)
                        .forEach(p -> {
                            try {
                                Files.delete(p);
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                        });
            }
        }
    }

    /**
     * 디렉토리 내 파일 목록 조회
     */
    public static List<Path> listFiles(String dirPath) throws IOException {
        try (Stream<Path> stream = Files.list(Path.of(dirPath))) {
            return stream.filter(Files::isRegularFile)
                    .collect(Collectors.toList());
        }
    }

    /**
     * 디렉토리 내 모든 파일 목록 조회 (재귀적으로)
     */
    public static List<Path> listFilesRecursively(String dirPath) throws IOException {
        try (Stream<Path> stream = Files.walk(Path.of(dirPath))) {
            return stream.filter(Files::isRegularFile)
                    .collect(Collectors.toList());
        }
    }

    /**
     * 파일 크기 조회 (바이트)
     */
    public static long getFileSize(String filePath) throws IOException {
        return Files.size(Path.of(filePath));
    }

    /**
     * 파일 크기 조회 (읽기 쉬운 형식)
     */
    public static String getFileSizeFormatted(String filePath) throws IOException {
        long bytes = getFileSize(filePath);
        return formatFileSize(bytes);
    }

    /**
     * 바이트를 읽기 쉬운 크기로 변환
     */
    public static String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }

    /**
     * 파일 확장자 추출
     */
    public static String getExtension(String filePath) {
        int lastDot = filePath.lastIndexOf('.');
        if (lastDot > 0 && lastDot < filePath.length() - 1) {
            return filePath.substring(lastDot + 1);
        }
        return "";
    }

    /**
     * 파일명 (확장자 제외) 추출
     */
    public static String getNameWithoutExtension(String filePath) {
        String name = Path.of(filePath).getFileName().toString();
        int lastDot = name.lastIndexOf('.');
        if (lastDot > 0) {
            return name.substring(0, lastDot);
        }
        return name;
    }

    /**
     * 임시 파일 생성
     */
    public static Path createTempFile(String prefix, String suffix) throws IOException {
        return Files.createTempFile(prefix, suffix);
    }

    /**
     * 임시 디렉토리 생성
     */
    public static Path createTempDirectory(String prefix) throws IOException {
        return Files.createTempDirectory(prefix);
    }

    /**
     * 바이너리 파일 읽기
     */
    public static byte[] readBytes(String filePath) throws IOException {
        return Files.readAllBytes(Path.of(filePath));
    }

    /**
     * 바이너리 파일 쓰기
     */
    public static void writeBytes(String filePath, byte[] bytes) throws IOException {
        Files.write(Path.of(filePath), bytes);
    }

    /**
     * 파일이 디렉토리인지 확인
     */
    public static boolean isDirectory(String path) {
        return Files.isDirectory(Path.of(path));
    }

    /**
     * 파일이 일반 파일인지 확인
     */
    public static boolean isRegularFile(String path) {
        return Files.isRegularFile(Path.of(path));
    }

    /**
     * 파일 수정 시간 조회 (밀리초)
     */
    public static long getLastModifiedTime(String filePath) throws IOException {
        return Files.getLastModifiedTime(Path.of(filePath)).toMillis();
    }

    /**
     * CSV 파일 읽기 (간단한 구현)
     */
    public static List<String[]> readCSV(String filePath) throws IOException {
        List<String[]> records = new ArrayList<>();
        List<String> lines = readLines(filePath);
        for (String line : lines) {
            records.add(line.split(","));
        }
        return records;
    }

    /**
     * CSV 파일 쓰기 (간단한 구현)
     */
    public static void writeCSV(String filePath, List<String[]> records) throws IOException {
        List<String> lines = records.stream()
                .map(record -> String.join(",", record))
                .collect(Collectors.toList());
        writeLines(filePath, lines);
    }

    /**
     * 파일 내용을 스트림으로 읽기
     */
    public static Stream<String> lines(String filePath) throws IOException {
        return Files.lines(Path.of(filePath), StandardCharsets.UTF_8);
    }

    /**
     * 안전한 파일 읽기 (예외 처리 포함)
     */
    public static String readFileSafe(String filePath, String defaultValue) {
        try {
            return readFile(filePath);
        } catch (IOException e) {
            return defaultValue;
        }
    }

    /**
     * 안전한 파일 쓰기 (예외 처리 포함)
     */
    public static boolean writeFileSafe(String filePath, String content) {
        try {
            writeFile(filePath, content);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * 파일 정보 클래스
     */
    public static class FileInfo {
        private final String path;
        private final String name;
        private final String extension;
        private final long size;
        private final long lastModified;
        private final boolean isDirectory;

        public FileInfo(Path filePath) throws IOException {
            this.path = filePath.toString();
            this.name = filePath.getFileName().toString();
            this.extension = FileUtils.getExtension(this.path);
            this.size = Files.isRegularFile(filePath) ? Files.size(filePath) : 0;
            this.lastModified = Files.getLastModifiedTime(filePath).toMillis();
            this.isDirectory = Files.isDirectory(filePath);
        }

        public String getPath() { return path; }
        public String getName() { return name; }
        public String getExtension() { return extension; }
        public long getSize() { return size; }
        public String getSizeFormatted() { return formatFileSize(size); }
        public long getLastModified() { return lastModified; }
        public boolean isDirectory() { return isDirectory; }

        @Override
        public String toString() {
            return String.format("FileInfo{name='%s', size=%s, isDir=%s}",
                    name, getSizeFormatted(), isDirectory);
        }
    }

    /**
     * 파일 정보 조회
     */
    public static FileInfo getFileInfo(String filePath) throws IOException {
        return new FileInfo(Path.of(filePath));
    }

    /**
     * 테스트 메인 메서드
     */
    public static void main(String[] args) {
        System.out.println("=== FileUtils 테스트 ===\n");

        try {
            // 1. 임시 디렉토리 생성
            Path tempDir = createTempDirectory("fileutils-test");
            System.out.println("1. 임시 디렉토리 생성: " + tempDir);

            // 2. 파일 쓰기
            String testFile = tempDir.resolve("test.txt").toString();
            writeFile(testFile, "Hello, FileUtils!\nThis is a test file.");
            System.out.println("\n2. 파일 생성: " + testFile);

            // 3. 파일 읽기
            String content = readFile(testFile);
            System.out.println("\n3. 파일 내용:");
            System.out.println(content);

            // 4. 줄 단위 읽기
            List<String> lines = readLines(testFile);
            System.out.println("\n4. 줄 단위 읽기:");
            lines.forEach(line -> System.out.println("  - " + line));

            // 5. 파일에 추가
            appendLine(testFile, "Appended line!");
            System.out.println("\n5. 줄 추가 후:");
            System.out.println(readFile(testFile));

            // 6. 파일 정보
            FileInfo info = getFileInfo(testFile);
            System.out.println("\n6. 파일 정보:");
            System.out.println(info);
            System.out.println("  확장자: " + info.getExtension());

            // 7. 파일 복사
            String copiedFile = tempDir.resolve("test-copy.txt").toString();
            copyFile(testFile, copiedFile);
            System.out.println("\n7. 파일 복사: " + copiedFile);

            // 8. 디렉토리 내 파일 목록
            System.out.println("\n8. 디렉토리 내 파일 목록:");
            listFiles(tempDir.toString()).forEach(System.out::println);

            // 9. CSV 테스트
            String csvFile = tempDir.resolve("test.csv").toString();
            List<String[]> csvData = List.of(
                    new String[]{"Name", "Age", "City"},
                    new String[]{"Alice", "30", "Seoul"},
                    new String[]{"Bob", "25", "Busan"}
            );
            writeCSV(csvFile, csvData);
            System.out.println("\n9. CSV 파일 생성 및 읽기:");
            readCSV(csvFile).forEach(row ->
                    System.out.println("  " + String.join(" | ", row))
            );

            // 10. 정리
            System.out.println("\n10. 임시 파일 정리:");
            deleteDirectory(tempDir.toString());
            System.out.println("정리 완료!");

        } catch (IOException e) {
            System.err.println("오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }
}