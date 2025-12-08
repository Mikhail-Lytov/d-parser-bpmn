package com.lytov.diplom.dparser.service.impl;

import com.lytov.diplom.dparser.domain.enums.ModelType;
import com.lytov.diplom.dparser.external.sppr_bd.SpprBdConnector;
import com.lytov.diplom.dparser.service.api.ComponentParser;
import com.lytov.diplom.dparser.service.api.ParserProcessService;
import com.lytov.diplom.dparser.service.dto.Component;
import com.lytov.diplom.dparser.service.strategy.ModelParserStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
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

    private final SpprBdConnector spprBdConnector;
    private final RestTemplate restTemplate;

    @Override
    public List<Component> parserProcess(UUID fileId) {

        String downloadUrl = spprBdConnector.getDownloadUrl(fileId);

        URI uri = UriComponentsBuilder
                .fromUriString(downloadUrl)
                .build(true)             // <- ВАЖНО: true = не кодировать заново
                .toUri();

        ResponseEntity<byte[]> file =restTemplate.exchange(
                uri,
                HttpMethod.GET,
                null,
                byte[].class
        );

        String contentDisposition = file.getHeaders()
                .getFirst(HttpHeaders.CONTENT_DISPOSITION);

        File localFile = null;
        try {
            localFile = createFile(file.getBody());
        } catch (Exception e) {
            log.error(e.getMessage());
            return null;
        }
        try {
            String ext = FilenameUtils.getExtension(contentDisposition); //TODO: пар
            ComponentParser parser = parserStrategy.getComponentParser(ModelType.modalByExt(ext));
            return parser.parserComponents(localFile);
        } finally {
            Optional.of(localFile).ifPresent(File::delete);
        }
    }

    private File createFile(byte[] data) throws IOException {
        String tmpdir = System.getProperty("java.io.tmpdir");
        Path filepath = Paths.get(tmpdir, UUID.randomUUID().toString());
        Files.write(filepath, data);
        return filepath.toFile();
    }
}
