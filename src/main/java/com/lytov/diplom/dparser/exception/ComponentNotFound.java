package com.lytov.diplom.dparser.exception;

import com.lytov.diplom.dparser.domain.enums.ModelType;

public class ComponentNotFound extends RuntimeException {
    public ComponentNotFound(ModelType modelType) {
        super("component parser not found for model type: " + modelType);
    }
}
