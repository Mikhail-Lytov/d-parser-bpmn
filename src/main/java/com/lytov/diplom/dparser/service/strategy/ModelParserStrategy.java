package com.lytov.diplom.dparser.service.strategy;

import com.lytov.diplom.dparser.domain.enums.ModelType;
import com.lytov.diplom.dparser.exception.ComponentNotFound;
import com.lytov.diplom.dparser.service.api.ComponentParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModelParserStrategy {

    private final Map<ModelType, ComponentParser>  componentParsers;

    public ComponentParser getComponentParser(ModelType modelType) {
        return Optional.ofNullable(componentParsers.get(modelType))
                .orElseThrow(() -> new ComponentNotFound( modelType));
    }
}
