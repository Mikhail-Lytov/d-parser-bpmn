package com.lytov.diplom.dparser.service.impl;

import com.lytov.diplom.dparser.domain.enums.ModelType;
import com.lytov.diplom.dparser.service.api.ComponentParser;
import com.lytov.diplom.dparser.service.api.ParserProcessService;
import com.lytov.diplom.dparser.service.dto.Component;
import com.lytov.diplom.dparser.service.strategy.ModelParserStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParserProcessServiceImpl implements ParserProcessService {

    private final ModelParserStrategy parserStrategy;

    @Override
    public List<Component> parserProcess(MultipartFile file) {

        File localFile = null;
        try {
            localFile = multipartFileToFile(file);
        }catch (Exception e){
            log.error("Error parsing file {}",file.getOriginalFilename());
            return null;
        }
        try {
            String ext = FilenameUtils.getExtension(localFile.getName()); //TODO: пар
            ComponentParser parser = parserStrategy.getComponentParser(ModelType.modalByExt(ext));
            return parser.parserComponents(localFile);
        } finally {
            Optional.of(localFile).ifPresent(File::delete);
        }
    }

    private File multipartFileToFile(MultipartFile multipart) throws IOException {
        String tmpdir = System.getProperty("java.io.tmpdir");
        Path filepath = Paths.get(tmpdir, UUID.randomUUID() + Objects.requireNonNull(multipart.getOriginalFilename()));
        multipart.transferTo(filepath);
        return filepath.toFile();
    }
}
