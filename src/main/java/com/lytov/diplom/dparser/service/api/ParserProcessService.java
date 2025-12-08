package com.lytov.diplom.dparser.service.api;

import com.lytov.diplom.dparser.service.dto.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface ParserProcessService {
    List<Component> parserProcess(UUID fileId);
}
