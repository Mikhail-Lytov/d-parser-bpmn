package com.lytov.diplom.dparserbpmn;

import org.springframework.boot.SpringApplication;

public class TestDParserBpmnApplication {

    public static void main(String[] args) {
        SpringApplication.from(DParserBpmnApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
