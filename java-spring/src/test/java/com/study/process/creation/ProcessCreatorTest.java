package com.study.process.creation;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
}