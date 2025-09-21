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
        } else {
            if(args[0].equals("ECHO")) {
                if (args.length > 1) {
                    StringBuilder message = new StringBuilder();
                    for (int i = 1; i < args.length; i++) {
                        if (i > 1) message.append(" ");
                        message.append(args[i]);
                    }
                    System.out.println("ECHO: " + message);
                }
            }else if(args[0].equals("COMPUTE")){
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
            }
        }
        System.out.println("Child process completed");
    }
}
