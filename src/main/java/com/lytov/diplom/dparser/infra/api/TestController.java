package com.lytov.diplom.dparser.infra.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.lytov.diplom.dparser.service.impl.BpmnToGraphParserImpl;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.FileNotFoundException;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class TestController {
    private final BpmnToGraphParserImpl bpmnToGraphParserImpl;

    @PostMapping("/test")
    public void test(@RequestBody Test test) throws FileNotFoundException, JsonProcessingException {
        bpmnToGraphParserImpl.createGraph(test.getFileId(), test.getProcessId());
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Test {
        private UUID fileId;
        private UUID processId;
    }
}
