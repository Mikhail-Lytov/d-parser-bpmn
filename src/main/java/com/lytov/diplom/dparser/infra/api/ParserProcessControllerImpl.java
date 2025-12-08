package com.lytov.diplom.dparser.infra.api;

import com.lytov.diplom.dparser.service.api.ParserProcessService;
import com.lytov.diplom.dparser.service.dto.Component;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ParserProcessControllerImpl implements ParserProcessController {

    private final ParserProcessService service;
    @Override
    public ResponseEntity<List<Component>> parse(UUID fileId) {
        return ResponseEntity.ok(service.parserProcess(fileId));
    }
}
