package com.study.process.creation;

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
                if (args.length > 1) {
                    StringBuilder message = new StringBuilder();
                    for (int i = 1; i < args.length; i++) {
                        if (i > 1) message.append(" ");
                        message.append(args[i]);
                    }
                    System.out.println("ECHO: " + message);
                }
                break;
            case "COMPUTE":
                if(args.length>1){
                    try{
                        int n = Integer.parseInt(args[1]);
                        System.out.println("Computing sum from 1 to " +n);

                        long sum = 0;
                        for(int i = 1;i<=n;i++){
                            sum += i;
                        }
                        System.out.println("Result: "+sum);
                    }catch (NumberFormatException e) {
                        System.err.println("Invalid number: " + args[1]);
                    }
                }
                break;
            case "FILE":
                if(args.length < 3){
                    System.out.println("FILE mode requires filename and content");
                    return;
                }

                String filename = args[1];
                StringBuilder content = new StringBuilder();
                for(int i=2;i<args.length;i++){
                    if(i>2) content.append(" ");
                    content.append(args[i]);
                }
                try {
                    Path filePath = Paths.get(filename);
                    Files.writeString(filePath,content);
                    System.out.println("File created"+ filename);
                }catch (IOException e){
                    System.err.println("Failed to create file :" +e.getMessage());
                }
                break;

            case "SLEEP":
                if(args.length < 2 ){
                    System.out.println("SLEEP mode requires duration in milliseconds");
                    return;
                }

                try{
                    int mills = Integer.parseInt(args[1]);
                    System.out.println("Sleeping for "+mills+" ms");
                    Thread.sleep(mills);
                    System.out.println("Woke up after "+mills+" ms");
                }catch (InterruptedException e){
                    Thread.currentThread().interrupt();
                    System.out.println("Sleep interrupted");
                }
        }
    }
}
