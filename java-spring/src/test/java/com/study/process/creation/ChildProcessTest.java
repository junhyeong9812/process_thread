package com.study.process.creation;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

class ChildProcessTest {

    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @BeforeEach
    void setUp(){
        System.setOut(new PrintStream(outputStream));
    }

    @AfterEach
    void tearDown(){
        System.setOut(originalOut);
    }

    @Test
    @DisplayName("1. main 메서드가 존재한다.")
    void mainMethodExists(){
        //컴파일만 되면 통과
        ChildProcess.main(new String[]{});
    }

    @Test
    @DisplayName("2. 시작 메시지를 출력한다.")
    void shouldPrintStartMessage(){
        //when
        ChildProcess.main(new String[] {});

        //then
        String output  = outputStream.toString();
        assertThat(output).contains("Child process started");
    }

    @Test
    @DisplayName("3. PID를 출력한다.")
    void shouldPrintPid(){
        //when
        ChildProcess.main(new String[]{});

        //then
        String output = outputStream.toString();
        assertThat(output).contains("PID: ");
    }

    @Test
    @DisplayName("4. 완료 메시지를 출력한다.")
    void shouldPrintCompletionMessage(){
        //when
        ChildProcess.main(new String[]{});

        //then
        String output = outputStream.toString();
        assertThat(output).contains("Child process completed");
    }

    @Test
    @DisplayName("5. 인자 없을 때 기본 작업을 수행한다.")
    void shouldPerformDefaultTaskWithNoArgs(){
        //when
        ChildProcess.main(new String[]{});

        //then
        String output = outputStream.toString();
        assertThat(output).contains("Performing default task");
    }

    @Test
    @DisplayName("6. ECHO 모드를 인식한다.")
    void shouldRecognizeEchoMode(){
        //given
        String[] args ={"ECHO"};

        //when
        ChildProcess.main(args);

        //then
        String output = outputStream.toString();
        assertThat(output).contains("ECHO:");
    }

    @Test
    @DisplayName("7. ECHO 모드에서 메시지를 출력한다.")
    void shouldEchoMessage(){
        //given
        String[] args = {"ECHO","Hello"};

        //when
        ChildProcess.main(args);

        //then
        String output = outputStream.toString();
        assertThat(output).contains("ECHO: Hello");
    }

    @Test
    @DisplayName("8. ECHO 모드에서 여러 단어를 출력한다.")
    void shouldEchoMultipleWords(){
        //given
        String[] args = {"ECHO","Hello","World"};

        //when
        ChildProcess.main(args);

        //then
        String output = outputStream.toString();
        assertThat(output).contains("ECHO: Hello World");
    }

    @Test
    @DisplayName("9. COMPUTE 모드를 인식한다.")
    void shouldRecognizeComputeMode(){
        //given
        String[] args = {"COMPUTE","10"};

        //when
        ChildProcess.main(args);

        //then
        String output = outputStream.toString();
        assertThat(output).contains("Computing sum");
    }
}