package com.lytov.diplom.dparser.configuration.feign.strategy;

import com.lytov.diplom.dparser.domain.enums.ModelType;
import com.lytov.diplom.dparser.service.api.ComponentParser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
public class ModelParserConfiguration {

    @Bean
    Map<ModelType, ComponentParser> parserComponents(final List<ComponentParser> componentParsers) {
        return componentParsers.stream().collect(
                Collectors.toMap(
                        ComponentParser::getModelType,
                        componentParser -> componentParser
                )
        );
    }
}
