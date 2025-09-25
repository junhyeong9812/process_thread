package com.study.process.creation;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.*;


class ProcessCreatorTest {
    private ProcessCreator creator;

    @BeforeEach
    void setUp() {
        creator = new ProcessCreator();
    }

    @Test
    @DisplayName("1. ProccessCreator 객체를 생성할 수 있다.")
    void shouldCreateProcessCreator() {
        assertThat(creator).isNotNull();
    }

    @Test
    @DisplayName("2. 명령어를 설정할 수 있다.")
    void shouldSetCommand(){
        //when
        creator.setCommand("echo");

        //then
        assertThat(creator.getCommand()).isEqualTo("echo");
    }

    @Test
    @DisplayName("3. 인자를 추가할 수 있다.")
    void shouldAddArgument(){
        //given
        creator.setCommand("echo");

        //when
        creator.addArgument("hello");

        //then
        assertThat(creator.getArguments()).containsExactly("hello");
    }

    @Test
    @DisplayName("4. 여러 인자를 추가할 수 있다.")
    void shouldAddMultipleArguments() {
        //given
        creator.setCommand("echo");

        //when
        creator.addArgument("hello");
        creator.addArgument("world");

        //then
        assertThat(creator.getArguments()).containsExactly("hello","world");
    }

    @Test
    @DisplayName("5.ProcessBuilder를 생성할 수 있다.")
    void shouldCreateProcessBuilder() {
        //given
        creator.setCommand("echo");

        //when
        ProcessBuilder builder = creator.createProcessBuilder();

        //then
        assertThat(builder).isNotNull();
        assertThat(builder.command()).containsExactly("echo");
    }

    @Test
    @DisplayName("6. ProcessBuilder에 인자가 포함된다.")
    void shouldIncludeArgumentsInProcessBuilder(){
        //given
        creator.setCommand("echo");
        creator.addArgument("hello");

        //when
        ProcessBuilder builder = creator.createProcessBuilder();

        //then
        assertThat(builder.command()).containsExactly("echo","hello");
    }

    @Test
    @DisplayName("7. 프로세스를 시작할 수 있다.")
    void shouldStartProcess() throws IOException {
        //given
//        creator.setCommand("cmd");
//        creator.addArgument("/c");
//        creator.addArgument("echo");
//        creator.addArgument("test");
        //위 명령어는 너무 빨리 죽어서 ping을 통한 프로세스 생존 확인
        creator.setCommand("ping");
        creator.addArgument("127.0.0.1");
        creator.addArgument("-n");
        creator.addArgument("2");

        //when
        Process process = creator.start();

        //then
        assertThat(process).isNotNull();
        assertThat(process.isAlive()).isTrue();

        //cleanup
        process.destroyForcibly();
    }

    @Test
    @DisplayName("8. java 프로세스를 생성할 수 있다.")
    void shouldCreateJavaProcess() throws IOException {
        //when
        Process process = creator.createJavaProcess(
            "com.study.process.creation.ChildProcess",
            new  String[]{}
        );

        //then
        assertThat(process).isNotNull();
        assertThat(process.isAlive()).isTrue();

        //cleanup
        process.destroyForcibly();
    }
}