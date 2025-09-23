package com.study.process.creation;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

class ChildProcessTest {

    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errerStream = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    @BeforeEach
    void setUp(){
        System.setOut(new PrintStream(outputStream));
        System.setErr(new PrintStream(errerStream));
    }

    @AfterEach
    void tearDown(){
        System.setOut(originalOut);
        System.setErr(originalErr);
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

    @Test
    @DisplayName("10.COMPUTE 모드에서 1부터 N까지 합을 계산한다.")
    void ShouldComputeSum(){
        //given
        String[] args ={"COMPUTE","10"};

        //when
        ChildProcess.main(args);

        //then
        String output = outputStream.toString();
        assertThat(output).contains("Result: 55");
    }

    @Test
    @DisplayName("11. COMPUTE 모드에서 잘못된 숫자를 처리한다.")
    void shouldHandleInvalidNumberInCompute(){
        //given
        String[] args = {"COMPUTE", "invalid"};

        //when
        ChildProcess.main(args);

        //then
        String output = outputStream.toString();
        String error = errerStream.toString();
        assertThat(output+error).contains("Invalid number");
    }

    @Test
    @DisplayName("12. FILE 모드를 인식한다.")
    void shouldRecognizeFileMode(@TempDir Path tempDir){
        //given
        Path file = tempDir.resolve("text.txt");
        String[] args = {"FILE",file.toString(),"content"};

        //when
        ChildProcess.main(args);

        //then
        assertThat(file).exists();
    }

    @Test
    @DisplayName("13. FILE 모드에서 파일에 내용을 쓴다.")
    void shouldWriteContentToFile(@TempDir Path tempDir) throws IOException{
        //given
        Path file = tempDir.resolve("test.txt");
        String[] args = {"FILE",file.toString(),"test","content"};

        //when
        ChildProcess.main(args);

        //then
        String content = Files.readString(file);
        assertThat(content).isEqualTo("test content");
    }

    @Test
    @DisplayName("14. SLEEP 모드를 인식한다.")
    void shouldRecognizeSleepMode(){
        //given
        String[] args = {"SLEEP","10"};

        //when
        ChildProcess.main(args);

        //then
        String output = outputStream.toString();
        assertThat(output).contains("Sleeping for 10 ms");
    }

    @Test
    @DisplayName("15. SLEEP모드에서 실제로 대기한다.")
    void shouldActuallySleep(){
        //given
        String[] args = {"SLEEP","50"};

        //when
        long startTime =System.currentTimeMillis();
        ChildProcess.main(args);
        long endTime = System.currentTimeMillis();

        //then
        assertThat(endTime - startTime).isGreaterThanOrEqualTo(50);
    }

    @Test
    @DisplayName("16. ERROR 모드를 인식한다.")
    void shouldRecognizeErrorMode(){
        //given
        String[] args ={"ERROR","42"};

        //when
        ChildProcess.main(args);

        //then
        String output = outputStream.toString();
        assertThat(output).contains("Simulating error with code: 42");
    }


}