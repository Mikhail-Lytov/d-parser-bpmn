package com.lytov.diplom.dparser.service.dto;

import com.lytov.diplom.dparser.domain.enums.EdgeType;
import com.lytov.diplom.dparser.domain.enums.NodeType;

import java.util.*;

public class BpmnGraph {
    public final Map<String, BpmnNode> nodes = new LinkedHashMap<>();
    public final List<BpmnEdge> edges = new ArrayList<>();

    // adjacency для быстрых правил
    public final Map<String, List<BpmnEdge>> out = new HashMap<>();
    public final Map<String, List<BpmnEdge>> in = new HashMap<>();

    public void addNode(BpmnNode n) {
        nodes.putIfAbsent(n.id(), n);
    }

    public void addEdge(BpmnEdge e) {
        edges.add(e);
        out.computeIfAbsent(e.sourceId(), k -> new ArrayList<>()).add(e);
        in.computeIfAbsent(e.targetId(), k -> new ArrayList<>()).add(e);
    }

    public record BpmnNode(String id, String name, NodeType type) {}

    public record BpmnEdge(String id, EdgeType type, String sourceId, String targetId) {}

}
