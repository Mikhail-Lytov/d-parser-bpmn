package com.lytov.diplom.dparser;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
public class DParserBpmnApplication {

    public static void main(String[] args) {
        SpringApplication.run(DParserBpmnApplication.class, args);
    }

}
