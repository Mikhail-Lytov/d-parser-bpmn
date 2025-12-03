package com.lytov.diplom.dparser.service.api;

import com.lytov.diplom.dparser.service.dto.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ParserProcessService {
    List<Component> parserProcess(MultipartFile file);
}
