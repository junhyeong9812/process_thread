package com.study.process.creation;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * ProcessBuilder를 활용한 프로세스 생성 및 관리 클래스
 * TDD 방식으로 점진적으로 구현
 */
public class ProcessCreator {

    private String command;
    private final List<String> arguments;
    private File workingDirectory;
    private final Map<String, String> environmentVariables;

    public ProcessCreator() {
        this.arguments = new ArrayList<>();
        this.environmentVariables = new HashMap<>();
    }

    // ====== 기본 명령어 관리 ======

    public void setCommand(String command) {
        this.command = command;
    }

    public String getCommand() {
        return command;
    }

    public void addArgument(String argument) {
        arguments.add(argument);
    }

    public List<String> getArguments() {
        return new ArrayList<>(arguments);
    }

    // ====== ProcessBuilder 생성 ======

    public ProcessBuilder createProcessBuilder() {
        List<String> fullCommand = new ArrayList<>();
        fullCommand.add(command);
        fullCommand.addAll(arguments);

        ProcessBuilder builder = new ProcessBuilder(fullCommand);

        // 작업 디렉토리 설정
        if (workingDirectory != null) {
            builder.directory(workingDirectory);
        }

        // 환경 변수 설정
        if (!environmentVariables.isEmpty()) {
            builder.environment().putAll(environmentVariables);
        }

        // 에러 스트림을 표준 출력으로 리다이렉트
        builder.redirectErrorStream(true);

        return builder;
    }

    // ====== 프로세스 시작 ======

    public Process start() throws IOException {
        if (command == null) {
            throw new IllegalStateException("Command not set");
        }

        ProcessBuilder builder = createProcessBuilder();
        return builder.start();
    }

    // ====== Java 프로세스 생성 ======

    public Process createJavaProcess(String className, String[] args)
            throws IOException {

        // Java 실행 명령 구성
        String javaHome = System.getProperty("java.home");
        String javaBin = javaHome + File.separator + "bin" + File.separator + "java";

        // 클래스패스
        String classpath = System.getProperty("java.class.path");

        // 명령어 구성
        List<String> fullCommand = new ArrayList<>();
        fullCommand.add(javaBin);
        fullCommand.add("-cp");
        fullCommand.add(classpath);
        fullCommand.add(className);

        // 프로그램 인자 추가
        if (args != null) {
            fullCommand.addAll(Arrays.asList(args));
        }

        // ProcessBuilder 생성
        ProcessBuilder builder = new ProcessBuilder(fullCommand);

        if (workingDirectory != null) {
            builder.directory(workingDirectory);
        }

        if (!environmentVariables.isEmpty()) {
            builder.environment().putAll(environmentVariables);
        }

        builder.redirectErrorStream(true);

        return builder.start();
    }

    // ====== 프로세스 출력 읽기 ======

    public String readOutput(Process process) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {

            return reader.lines()
                    .collect(Collectors.joining(System.lineSeparator()));
        }
    }

    // ====== 프로세스 제어 ======

    public boolean waitFor(Process process, long timeout, TimeUnit unit)
            throws InterruptedException {
        return process.waitFor(timeout, unit);
    }

    public int getExitCode(Process process) {
        if (!process.isAlive()) {
            return process.exitValue();
        }
        throw new IllegalStateException("Process is still running");
    }

    public void destroy(Process process) {
        process.destroyForcibly();
        try {
            process.waitFor(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ====== 작업 디렉토리 관리 ======

    public void setWorkingDirectory(File directory) {
        this.workingDirectory = directory;
    }

    public File getWorkingDirectory() {
        return workingDirectory;
    }

    // ====== 환경 변수 관리 ======

    public void setEnvironmentVariable(String key, String value) {
        environmentVariables.put(key, value);
    }

    public Map<String, String> getEnvironmentVariables() {
        return new HashMap<>(environmentVariables);
    }

    // ====== 프로세스 정보 ======

    public long getPid(Process process) {
        return process.pid();
    }

    public ProcessHandle.Info getProcessInfo(Process process) {
        return process.toHandle().info();
    }

    // ====== 유틸리티 메서드 ======

    public void clearArguments() {
        arguments.clear();
    }

    public void clearEnvironmentVariables() {
        environmentVariables.clear();
    }

    public void reset() {
        command = null;
        arguments.clear();
        workingDirectory = null;
        environmentVariables.clear();
    }
}