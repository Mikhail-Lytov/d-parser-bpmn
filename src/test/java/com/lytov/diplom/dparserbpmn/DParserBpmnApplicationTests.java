package com.lytov.diplom.dparserbpmn;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class DParserBpmnApplicationTests {

    @Test
    void contextLoads() {
    }

}
