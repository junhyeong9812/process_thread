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
}