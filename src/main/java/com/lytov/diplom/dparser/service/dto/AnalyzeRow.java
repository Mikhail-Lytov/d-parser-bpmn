package com.lytov.diplom.dparser.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalyzeRow {
    private String bpmnType;
    private String refId;
    private String riskId;
}
