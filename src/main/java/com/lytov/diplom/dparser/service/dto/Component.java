package com.lytov.diplom.dparser.service.dto;

import com.lytov.diplom.dparser.domain.enums.OperationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Component {

    private String id;
    private String name;
    private String bpmnTaskType;
    private String role;
    private String department;
    private OperationType operationType;
}
