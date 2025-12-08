package com.lytov.diplom.dparser.infra.api;

import com.lytov.diplom.dparser.service.dto.Component;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.UUID;

@Tag(name = "Парсинг процессов")
@RequestMapping("/api/v0/parsing-process")
public interface ParserProcessController {

    @PostMapping("/{id}")
    ResponseEntity<List<Component>> parse(@PathVariable("id") UUID id);
}
