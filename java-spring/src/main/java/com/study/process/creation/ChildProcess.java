package com.study.process.creation;

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
        }
        System.out.println("Child process completed");
    }
}
