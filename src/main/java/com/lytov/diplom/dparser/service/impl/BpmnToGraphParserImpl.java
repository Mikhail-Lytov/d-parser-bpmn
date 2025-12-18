package com.lytov.diplom.dparser.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lytov.diplom.dparser.configuration.rabbit.DCoreRMQConfig;
import com.lytov.diplom.dparser.domain.enums.EdgeType;
import com.lytov.diplom.dparser.domain.enums.NodeType;
import com.lytov.diplom.dparser.external.sppr_bd.SpprBdConnector;
import com.lytov.diplom.dparser.service.api.BpmnToGraphParser;
import com.lytov.diplom.dparser.service.dto.BpmnGraph;
import com.lytov.diplom.dparser.service.dto.RequestCreateGraph;
import com.lytov.diplom.dparser.service.dto.ResultBpmnParserGraphDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BpmnToGraphParserImpl implements BpmnToGraphParser {

    private final SpprBdConnector spprBdConnector;
    private final RestTemplate restTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = DCoreRMQConfig.FROM_SPPR_CREATE_GRAPH_QUEUE)
    public void createGraphListener(@Payload RequestCreateGraph request) {
        try {
            createGraph(request.getFileId(), request.getProcessId());
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    public void createGraph(UUID fileId, UUID processId) throws FileNotFoundException, JsonProcessingException {

        String downloadUrl = spprBdConnector.getDownloadUrl(fileId);

        URI uri = UriComponentsBuilder
                .fromUriString(downloadUrl)
                .build(true)
                .toUri();

        ResponseEntity<byte[]> file = restTemplate.exchange(
                uri,
                HttpMethod.GET,
                null,
                byte[].class
        );

        String contentDisposition = file.getHeaders()
                .getFirst(HttpHeaders.CONTENT_DISPOSITION);

        File localFile = null;
        try {
            localFile = createFile(file.getBody());
        } catch (Exception e) {
            log.error(e.getMessage());
            return;
        }

        BpmnGraph bpmnGraph = this.parse(new FileInputStream(localFile));

        ResultBpmnParserGraphDto request = new ResultBpmnParserGraphDto(processId, bpmnGraph);

        Message message = MessageBuilder
                .withBody(objectMapper.writeValueAsBytes(request))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .build();

        rabbitTemplate.convertAndSend(
                DCoreRMQConfig.FROM_CORE_SECOND_PARS_RESULT_EXCHANGE,
                "",
                message

        );
    }

    @Override
    public BpmnGraph parse(InputStream bpmnXml) {
        BpmnModelInstance model = Bpmn.readModelFromStream(bpmnXml);
        BpmnGraph graph = new BpmnGraph();

        // 1) Nodes: FlowNode
        Collection<FlowNode> flowNodes = model.getModelElementsByType(FlowNode.class);
        for (FlowNode fn : flowNodes) {
            graph.addNode(new BpmnGraph.BpmnNode(fn.getId(), fn.getName(), mapFlowNodeType(fn)));
        }

        // 2) Nodes: DataObjectReference / DataStoreReference
        for (DataObjectReference dor : model.getModelElementsByType(DataObjectReference.class)) {
            graph.addNode(new BpmnGraph.BpmnNode(dor.getId(), dor.getName(), NodeType.DATA_OBJECT_REF));
        }
        for (DataStoreReference ds : model.getModelElementsByType(DataStoreReference.class)) {
            graph.addNode(new BpmnGraph.BpmnNode(ds.getId(), ds.getName(), NodeType.DATA_STORE_REF));
        }

        // 3) Edges: SequenceFlow
        for (SequenceFlow sf : model.getModelElementsByType(SequenceFlow.class)) {
            String src = idOf(sf.getSource());
            String tgt = idOf(sf.getTarget());
            ensureNodeExists(graph, sf.getSource());
            ensureNodeExists(graph, sf.getTarget());
            graph.addEdge(new BpmnGraph.BpmnEdge(sf.getId(), EdgeType.SEQUENCE_FLOW, src, tgt));
        }

        // 4) Edges: DataAssociation (DataInputAssociation/DataOutputAssociation)
        for (DataAssociation da : model.getModelElementsByType(DataAssociation.class)) {
            // У DataAssociation может быть 0..n источников и 0..1 targetRef (обычно)
            // В Camunda API:
            // - da.getSources() -> Collection<ItemAwareElement>
            // - da.getTarget()  -> ItemAwareElement
            ItemAwareElement target = da.getTarget();
            if (target == null) continue;

            ModelElementInstance instance = da.getParentElement();
            String targetId = instance != null ? idOf(instance) : idOf(target);
            ensureNodeExists(graph, target);

            for (ItemAwareElement src : da.getSources()) {
                String sourceId = idOf(src);
                ensureNodeExists(graph, src);

                String edgeId = da.getId() + ":" + sourceId + "->" + targetId;
                graph.addEdge(new BpmnGraph.BpmnEdge(edgeId, EdgeType.DATA_ASSOCIATION, sourceId, targetId));
            }
        }

        // 5) (Optional) MessageFlow
        // В BPMN messageFlow встречается в collaboration. Добавляем, если используешь.
        for (MessageFlow mf : model.getModelElementsByType(MessageFlow.class)) {
            InteractionNode src = mf.getSource();
            InteractionNode tgt = mf.getTarget();
            if (src == null || tgt == null) continue;

            ensureNodeExists(graph, (ModelElementInstance) src);
            ensureNodeExists(graph, (ModelElementInstance) tgt);

            graph.addEdge(new BpmnGraph.BpmnEdge(mf.getId(), EdgeType.MESSAGE_FLOW, idOf(src), idOf(tgt)));
        }

        return graph;
    }

    private static String idOf(ModelElementInstance e) {
        return e != null ? e.getAttributeValue("id") : null;
    }

    private static void ensureNodeExists(BpmnGraph graph, ModelElementInstance e) {
        String id = idOf(e);
        if (id == null) return;
        graph.nodes.putIfAbsent(id, new BpmnGraph.BpmnNode(id, nameOf(e), NodeType.UNKNOWN));
    }

    private static String nameOf(ModelElementInstance e) {
        // не у всех есть name; безопасно
        String name = e.getAttributeValue("name");
        return name != null ? name : "";
    }

    private static NodeType mapFlowNodeType(FlowNode fn) {
        if (fn instanceof UserTask) return NodeType.USER_TASK;
        if (fn instanceof ServiceTask) return NodeType.SERVICE_TASK;
        if (fn instanceof ScriptTask) return NodeType.SCRIPT_TASK;
        if (fn instanceof Task) return NodeType.TASK;

        if (fn instanceof StartEvent) return NodeType.START_EVENT;
        if (fn instanceof EndEvent) return NodeType.END_EVENT;
        if (fn instanceof IntermediateCatchEvent) return NodeType.INTERMEDIATE_CATCH_EVENT;
        if (fn instanceof IntermediateThrowEvent) return NodeType.INTERMEDIATE_THROW_EVENT;
        if (fn instanceof BoundaryEvent) return NodeType.BOUNDARY_EVENT;

        if (fn instanceof ExclusiveGateway) return NodeType.EXCLUSIVE_GATEWAY;
        if (fn instanceof ParallelGateway) return NodeType.PARALLEL_GATEWAY;
        if (fn instanceof InclusiveGateway) return NodeType.INCLUSIVE_GATEWAY;
        if (fn instanceof EventBasedGateway) return NodeType.EVENT_BASED_GATEWAY;

        if (fn instanceof SubProcess) return NodeType.SUB_PROCESS;
        if (fn instanceof CallActivity) return NodeType.CALL_ACTIVITY;

        return NodeType.UNKNOWN;
    }
    private File createFile(byte[] data) throws IOException {
        String tmpdir = System.getProperty("java.io.tmpdir");
        Path filepath = Paths.get(tmpdir, UUID.randomUUID().toString());
        Files.write(filepath, data);
        return filepath.toFile();
    }
}
