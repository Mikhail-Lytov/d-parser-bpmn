package com.lytov.diplom.dparser.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequestCreateGraph {
    private UUID processId;
    private UUID fileId;
}
