package com.study.process.creation;

import java.util.ArrayList;
import java.util.List;

/**
 * ProcessBuilder를 활용한 프로세스 생성 및 관리 클래스
 * */
public class ProcessCreator {
    private String command;
    private final List<String> arguments;

    public ProcessCreator() {
        this.arguments = new ArrayList<>();
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



}
