package com.lytov.diplom.dparser.service.api;

import com.lytov.diplom.dparser.service.dto.BpmnGraph;

import java.io.InputStream;

public interface BpmnToGraphParser {

    BpmnGraph parse(InputStream bpmnXml);
}
