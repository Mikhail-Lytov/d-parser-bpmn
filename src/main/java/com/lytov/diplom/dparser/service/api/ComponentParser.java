package com.lytov.diplom.dparser.service.api;

import com.lytov.diplom.dparser.domain.enums.ModelType;
import com.lytov.diplom.dparser.service.dto.Component;

import java.io.File;
import java.util.List;

public interface ComponentParser {

    List<Component> parserComponents(File file);

    ModelType getModelType();
}
