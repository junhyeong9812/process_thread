package com.study.process.creation;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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



}
