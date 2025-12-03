package com.lytov.diplom.dparser.domain.enums;

import com.lytov.diplom.dparser.exception.ModalNotFound;

import java.util.Arrays;

public enum ModelType {
    BPMN("bpmn");

    private final String ext;

    ModelType(String ext) {
        this.ext = ext;
    }

    public static ModelType modalByExt(String ext) {
        return Arrays.stream(ModelType.values())
                .filter(e -> e.ext.equals(ext))
                .findFirst()
                .orElseThrow(() -> new ModalNotFound("modal not found by ext: " + ext));
    }
}
