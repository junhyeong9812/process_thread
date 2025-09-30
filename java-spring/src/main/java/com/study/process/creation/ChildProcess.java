package com.study.process.creation;
// 패키지 선언: 이 클래스가 속한 패키지 경로를 지정
// Java의 패키지는 관련 클래스들을 그룹화하고 네임스페이스를 제공

import java.io.FileWriter;
// 파일에 문자 데이터를 쓰기 위한 클래스 임포트
// FileWriter는 문자 스트림을 파일에 쓰는 편리한 클래스

import java.io.IOException;
// 입출력 작업 중 발생할 수 있는 예외 클래스
// 파일 작업 시 발생 가능한 오류 처리를 위해 필요

import java.nio.file.Files;
// NIO.2 파일 시스템 API의 유틸리티 클래스
// 파일 작업을 위한 정적 메서드들을 제공 (현재 코드에서는 미사용)

import java.nio.file.Path;
// 파일 시스템의 경로를 나타내는 인터페이스
// 플랫폼 독립적인 파일 경로 표현 (현재 코드에서는 미사용)

import java.nio.file.Paths;
// Path 인스턴스를 생성하는 유틸리티 클래스
// 문자열로부터 Path 객체 생성 (현재 코드에서는 미사용)

/**
 * 자식 프로세스로 실행될 독립적인 JAVA 애플리케이션
 * 다양한 모드로 실행 가능하여 프로세스 테스트에 활용
 */
// Javadoc 주석: 클래스의 목적과 용도를 문서화
// 이 클래스는 ProcessBuilder로 실행될 별도의 JVM 프로세스가 됨

public class ChildProcess {
    // public: 다른 패키지에서도 접근 가능
    // class: 클래스 선언 키워드
    // ChildProcess: 클래스명 - 자식 프로세스 역할을 명확히 표현

    public static void main(String[] args){
        // public: JVM이 외부에서 이 메서드를 호출할 수 있어야 함
        // static: 객체 생성 없이 JVM이 직접 호출 가능해야 함
        // void: 반환값 없음 (프로세스 종료 코드는 System.exit()로 설정)
        // main: JVM이 프로그램 시작점으로 인식하는 특별한 메서드명
        // String[] args: 커맨드라인 인자들을 배열로 받음

        //PID 획득
        long pid = ProcessHandle.current().pid();
        // ProcessHandle.current(): 현재 실행 중인 프로세스의 핸들 획득
        // .pid(): 프로세스 ID를 long 타입으로 반환
        // 운영체제가 각 프로세스에 할당한 고유 식별자

        System.out.println("Child process started");
        // 표준 출력에 시작 메시지 출력
        // 부모 프로세스가 자식의 출력을 캡처할 수 있음

        System.out.println("PID: "+pid);
        // 프로세스 ID 출력
        // 디버깅과 프로세스 추적에 유용

        if(args.length == 0){
            // 커맨드라인 인자가 없는 경우 체크
            // args.length: 배열의 크기 (전달된 인자 개수)

            System.out.println("Performing default task");
            // 기본 작업 수행 메시지 출력
            // 인자 없이 실행되었을 때의 기본 동작

        } else {
            // 인자가 1개 이상 전달된 경우

            //모드별 실행
            String mode = args[0];
            // 첫 번째 인자를 모드로 사용
            // 프로세스의 동작 방식을 결정하는 키워드

            excuteMode(mode,args);
            // 모드에 따른 작업 실행 메서드 호출
            // 전체 args 배열을 전달하여 추가 인자 활용 가능
        }

        System.out.println("Child process completed");
        // 프로세스 종료 메시지
        // 정상 종료를 확인할 수 있는 마커
    }

    private static void excuteMode(String mode,String[] args){
        // private: 클래스 내부에서만 사용
        // static: main에서 직접 호출하기 위해 정적 메서드로 선언
        // mode: 실행 모드 문자열
        // args: 전체 커맨드라인 인자 배열

        switch (mode){
            // Java 7부터 String을 switch문에 사용 가능
            // 모드별로 다른 동작을 분기

            case "ECHO":
                // 에코 모드: 전달받은 메시지를 그대로 출력
                executeEcho(args);
                break;
            // break: 다음 case로 진행하지 않고 switch 종료

            case "COMPUTE":
                // 계산 모드: 1부터 n까지의 합 계산
                executeCompute(args);
                break;

            case "FILE":
                // 파일 모드: 파일 생성 및 내용 쓰기
                executeFile(args);
                break;

            case "SLEEP":
                // 슬립 모드: 지정된 시간동안 대기
                executeSleep(args);
                break;

            case "ERROR":
                // 에러 모드: 특정 종료 코드로 프로세스 종료
                executeError(args);
                break;

            default:
                // 알 수 없는 모드 처리
                System.out.println("Unknown mode: " + mode);
                break;
        }
    }

    private static void executeEcho(String[] args){
        // ECHO 모드 구현: 메시지를 받아서 출력

        StringBuilder message = new StringBuilder("ECHO:");
        // StringBuilder: 문자열을 효율적으로 연결
        // String 연결보다 메모리 효율적 (불변 객체 생성 방지)
        // "ECHO:" 프리픽스로 시작

        if (args.length > 1) {
            // args[0]은 모드이므로, args[1]부터가 실제 메시지

            for (int i = 1; i < args.length; i++) {
                // 두 번째 인자부터 순회

                message.append(" ").append(args[i]);
                // 공백으로 구분하여 각 인자를 연결
                // append는 StringBuilder 자신을 반환 (메서드 체이닝)
            }
        }

        System.out.println(message.toString());
        // StringBuilder를 String으로 변환하여 출력
        // toString(): StringBuilder의 내용을 String 객체로 변환
    }

    private static void executeCompute(String[] args){
        // COMPUTE 모드: 1부터 n까지의 합 계산

        if (args.length < 2) {
            // args[1]에 n 값이 필요
            System.out.println("Computing sum from 1 to 0");
            System.out.println("Result: 0");
            return;
            // 인자 부족시 기본값 0으로 처리하고 조기 종료
        }

        try {
            int n = Integer.parseInt(args[1]);
            // 문자열을 정수로 변환
            // NumberFormatException 발생 가능

            System.out.println("Computing sum from 1 to " + n);

            long sum = 0;
            // long 타입 사용: int 범위 초과 가능성 대비
            // n이 크면 합이 int 최대값(2^31-1) 초과 가능

            for (int i = 1; i <= n; i++) {
                // 1부터 n까지 반복
                sum += i;
                // 누적 합 계산 (가우스 공식 대신 단순 반복 사용)
            }

            System.out.println("Result: " + sum);

        } catch (NumberFormatException e) {
            // parseInt 실패시 예외 처리
            System.out.println("Invalid number: " + args[1]);
        }
    }

    private static void executeFile(String[] args){
        // FILE 모드: 파일 생성 및 쓰기

        if (args.length < 3) {
            // 최소 args[1]=파일명, args[2]=내용 필요
            System.out.println("FILE mode requires filename and content");
            return;
        }

        String filename = args[1];
        // 첫 번째 추가 인자를 파일명으로 사용

        StringBuilder content = new StringBuilder();
        // 여러 인자를 하나의 내용으로 결합

        for (int i = 2; i < args.length; i++) {
            // args[2]부터 파일 내용

            if (i > 2) content.append(" ");
            // 첫 단어가 아니면 공백 추가 (단어 구분)

            content.append(args[i]);
            // 각 인자를 내용에 추가
        }

        try (FileWriter writer = new FileWriter(filename)) {
            // try-with-resources: 자동 리소스 관리
            // FileWriter가 AutoCloseable이므로 자동으로 close() 호출
            // 예외 발생시에도 안전하게 파일 닫힘

            writer.write(content.toString());
            // 파일에 내용 쓰기

            System.out.println("File written: " + filename);

        } catch (IOException e) {
            // 파일 작업 실패시 (권한 없음, 디스크 풀 등)
            System.err.println("Error writing file: " + e.getMessage());
            // System.err: 표준 에러 스트림 사용
        }
    }

    private static void executeSleep(String[] args){
        // SLEEP 모드: 지정 시간동안 프로세스 일시 정지

        if (args.length < 2) {
            System.out.println("SLEEP mode requires duration in milliseconds");
            return;
        }

        try {
            int millis = Integer.parseInt(args[1]);
            // 밀리초 단위 대기 시간 파싱

            System.out.println("Sleeping for " + millis + " ms");

            Thread.sleep(millis);
            // 현재 스레드를 지정 시간동안 일시 정지
            // InterruptedException 발생 가능

            System.out.println("Woke up after " + millis + " ms");

        } catch (NumberFormatException e) {
            System.out.println("Invalid number: " + args[1]);

        } catch (InterruptedException e) {
            // 다른 스레드가 interrupt() 호출시 발생

            Thread.currentThread().interrupt();
            // 인터럽트 상태 복원 (Best Practice)
            // 상위 코드가 인터럽트를 감지할 수 있도록 함

            System.out.println("Sleep interrupted");
        }
    }

    private static void executeError(String[] args){
        // ERROR 모드: 특정 종료 코드로 프로세스 종료

        if (args.length < 2) {
            System.out.println("ERROR mode requires error code");
            return;
        }

        try {
            int errorCode = Integer.parseInt(args[1]);
            // 종료 코드 파싱

            System.out.println("Simulating error with code: " + errorCode);

            System.exit(errorCode);
            // JVM을 지정된 종료 코드로 즉시 종료
            // 0: 정상 종료, 0이 아닌 값: 오류 종료
            // 부모 프로세스가 이 코드를 받아 처리 가능

        } catch (NumberFormatException e) {
            System.out.println("Invalid error code: " + args[1]);
        }
    }
}