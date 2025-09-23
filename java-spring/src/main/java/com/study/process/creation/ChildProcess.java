package com.study.process.creation;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 자식 프로세스로 실행될 독립적인 JAVA 애플리케이션
 * 다양한 모드로 실행 가능하여 프로세스 테스트에 활용
 * */
public class ChildProcess {
    public static void main(String[] args){
        //PID 획득
        long pid = ProcessHandle.current().pid();
        System.out.println("Child process started");
        System.out.println("PID: "+pid);
        if(args.length == 0){
            System.out.println("Performing default task");
        } else {
            //모드별 실행
            String mode = args[0];
            excuteMode(mode,args);
        }
        System.out.println("Child process completed");
    }

    private static void excuteMode(String mode,String[] args){
        switch (mode){
            case "ECHO":
                executeEcho(args);
                break;
            case "COMPUTE":
                executeCompute(args);
                break;
            case "FILE":
                executeFile(args);
                break;
            case "SLEEP":
                executeSleep(args);
                break;
            case "ERROR":
                executeError(args);
                break;
            default:
                System.out.println("Unknown mode: " + mode);
                break;


        }
    }
    private static void executeEcho(String[] args){
        StringBuilder message = new StringBuilder("ECHO:");

        if (args.length > 1) {
            for (int i = 1; i < args.length; i++) {
                message.append(" ").append(args[i]);
            }
        }

        System.out.println(message.toString());
    }

    private static void executeCompute(String[] args){
        if (args.length < 2) {
            System.out.println("Computing sum from 1 to 0");
            System.out.println("Result: 0");
            return;
        }

        try {
            int n = Integer.parseInt(args[1]);
            System.out.println("Computing sum from 1 to " + n);

            long sum = 0;
            for (int i = 1; i <= n; i++) {
                sum += i;
            }

            System.out.println("Result: " + sum);
        } catch (NumberFormatException e) {
            System.out.println("Invalid number: " + args[1]);
        }

    }

    private static void executeFile(String[] args){
        if (args.length < 3) {
            System.out.println("FILE mode requires filename and content");
            return;
        }

        String filename = args[1];
        StringBuilder content = new StringBuilder();

        for (int i = 2; i < args.length; i++) {
            if (i > 2) content.append(" ");
            content.append(args[i]);
        }

        try (FileWriter writer = new FileWriter(filename)) {
            writer.write(content.toString());
            System.out.println("File written: " + filename);
        } catch (IOException e) {
            System.err.println("Error writing file: " + e.getMessage());
        }

    }

    private static void executeSleep(String[] args){
        if (args.length < 2) {
            System.out.println("SLEEP mode requires duration in milliseconds");
            return;
        }

        try {
            int millis = Integer.parseInt(args[1]);
            System.out.println("Sleeping for " + millis + " ms");
            Thread.sleep(millis);
            System.out.println("Woke up after " + millis + " ms");
        } catch (NumberFormatException e) {
            System.out.println("Invalid number: " + args[1]);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Sleep interrupted");
        }

    }

    private static void executeError(String[] args){
        if (args.length < 2) {
            System.out.println("ERROR mode requires error code");
            return;
        }

        try {
            int errorCode = Integer.parseInt(args[1]);
            System.out.println("Simulating error with code: " + errorCode);
            System.exit(errorCode);
        } catch (NumberFormatException e) {
            System.out.println("Invalid error code: " + args[1]);
        }
    }

}

