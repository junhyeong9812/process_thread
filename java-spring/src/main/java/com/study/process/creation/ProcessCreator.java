package com.study.process.creation;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * ProcessBuilder를 활용한 프로세스 생성 및 관리 클래스
 * */
public class ProcessCreator {
    private String command;
    private final List<String> arguments;
    private File workingDirectory;
    private final Map<String, String> environmentVariables;

    public ProcessCreator() {
        this.arguments = new ArrayList<>();
        this.environmentVariables = new HashMap<>();
    }

    public String getCommand(){
        return command;
    }

    public void setCommand(String command){
        this.command = command;
    }

    public void addArgument(String argument){
        arguments.add(argument);
    }

    public List<String> getArguments() {
        return new ArrayList<>(arguments);
    }

    /**
     * ProcessBuilder 생성
     * */
    public ProcessBuilder createProcessBuilder() {
        List<String> fullCommand = new ArrayList<>();
        fullCommand.add(command);
        fullCommand.addAll(arguments);

        ProcessBuilder builder = new ProcessBuilder(fullCommand);

        //작업 디렉토리 설정
        if(workingDirectory != null){
            builder.directory(workingDirectory);
        }

        //환경변수 설정
        if(!environmentVariables.isEmpty()){
            builder.environment().putAll(environmentVariables);
        }

        //에러스트립을 표주 출력으로 리다이렉트
        builder.redirectErrorStream(true);

        return builder;
    }

    /**
     * 프로세스 시작
     * */
    public Process start() throws IOException {
        if(command == null) {
            throw new IllegalStateException("Command not set");
        }

        ProcessBuilder builder = createProcessBuilder();
        return builder.start();
    }

    /**
     * java 프로세스 생성
     * */
    public Process createJavaProcess(String className, String[] args)
        throws IOException {

        //java 실행 명령 구성
        String javaHome = System.getProperty("java.home");
        String javaBin = javaHome + File.separator + "bin" + File.separator + "java";

        //클래스패스
        String classpath = System.getProperty("java.class.path");

        //명령어 구성
        List<String> fullCommand = new ArrayList<>();
        fullCommand.add(javaBin);
        fullCommand.add("-cp");
        fullCommand.add(classpath);
        fullCommand.add(className);

        //프로그램 인자 추가
        if(args != null){
            fullCommand.addAll(Arrays.asList(args));
        }

        //ProcessBuilder 생성
        ProcessBuilder builder = new ProcessBuilder(fullCommand);

        if(workingDirectory != null) {
            builder.directory(workingDirectory);
        }

        if(!environmentVariables.isEmpty()){
            builder.environment().putAll(environmentVariables);
        }

        builder.redirectErrorStream(true);

        return builder.start();
    }


}
