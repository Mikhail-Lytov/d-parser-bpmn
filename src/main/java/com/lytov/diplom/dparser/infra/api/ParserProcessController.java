package com.lytov.diplom.dparser.infra.api;

import com.lytov.diplom.dparser.service.dto.Component;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Парсинг процессов")
@RequestMapping("/api/v0/parsing-process")
public interface ParserProcessController {

    @PostMapping(
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    ResponseEntity<List<Component>> parse(@RequestParam("file") MultipartFile file);
}
