package com.lytov.diplom.dparser.service.impl;

import com.lytov.diplom.dparser.domain.enums.ModelType;
import com.lytov.diplom.dparser.domain.enums.OperationType;
import com.lytov.diplom.dparser.service.api.ComponentParser;
import com.lytov.diplom.dparser.service.dto.Component;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.*;
import org.springframework.stereotype.Service;

import java.io.File;
import java.lang.Process;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComponentParserImpl implements ComponentParser {
    @Override
    public List<Component> parserComponents(File bpmnFile) {
        BpmnModelInstance modelInstance = Bpmn.readModelFromFile(bpmnFile);

        Map<FlowNode, Lane> nodeToLane = buildNodeLaneMap(modelInstance);

        Map<FlowNode, String> nodeToPool = buildNodePoolMap(modelInstance);

        List<Component> components = new ArrayList<>();

        Collection<Task> tasks = modelInstance.getModelElementsByType(Task.class);
        for (Task task : tasks) {
            Component component = new Component();
            component.setId(task.getId());
            component.setName(task.getName());
            component.setBpmnTaskType(task.getElementType().getTypeName());

            Lane lane = nodeToLane.get(task);
            if (lane != null) {
                component.setRole(lane.getName());
            }

            String poolName = nodeToPool.get(task);
            component.setDepartment(poolName);

            fillRelatedData(task, component);

            OperationType opType = guessOperationType(task, component);
            component.setOperationType(opType);

            components.add(component);
        }

        return components;
    }

    @Override
    public ModelType getModelType() {
        return ModelType.BPMN;
    }

    private Map<FlowNode, Lane> buildNodeLaneMap(BpmnModelInstance modelInstance) {
        Map<FlowNode, Lane> result = new HashMap<>();
        Collection<Lane> lanes = modelInstance.getModelElementsByType(Lane.class);
        for (Lane lane : lanes) {
            for (FlowNode flowNode : lane.getFlowNodeRefs()) {
                result.put(flowNode, lane);
            }
        }
        return result;
    }

    private Map<FlowNode, String> buildNodePoolMap(BpmnModelInstance modelInstance) {
        Map<FlowNode, String> result = new HashMap<>();

        Collection<Participant> participants = modelInstance.getModelElementsByType(Participant.class);
        for (Participant participant : participants) {
            Process process = (Process) participant.getProcess();
            if (process == null) continue;
            String poolName = participant.getName();

            /*Collection<FlowElement> flowElements = process.getFlowElements();
            for (FlowElement element : flowElements) {
                if (element instanceof FlowNode flowNode) {
                    result.put(flowNode, poolName);
                }
            }*/
        }
        return result;
    }

    private void fillRelatedData(Task task, Component component) {
        // DataInputAssociation / DataOutputAssociation
        Collection<DataInputAssociation> inputs =
                task.getChildElementsByType(DataInputAssociation.class);
        for (DataInputAssociation in : inputs) {
            for (ItemAwareElement src : in.getSources()) {
                if (src.getId() != null) {
                  //  component.getRelatedData().add(src.getId());
                }
            }
        }

        Collection<DataOutputAssociation> outputs =
                task.getChildElementsByType(DataOutputAssociation.class);
        for (DataOutputAssociation out : outputs) {
            ItemAwareElement target = out.getTarget();
            if (target != null && target.getId() != null) {
                //component.getRelatedData().add(target.getId());
            }
        }
    }

    private OperationType guessOperationType(Task task, Component component) {
        String name = task.getName() != null ? task.getName().toLowerCase() : "";

        if (task instanceof UserTask) {
            if (name.contains("утверд") || name.contains("approve")) {
                return OperationType.APPROVAL;
            }
            if (name.contains("ввод") || name.contains("внести")
                    || name.contains("input")) {
                return OperationType.USER_INPUT;
            }
            return OperationType.DATA_PROCESSING;
        }

        if (task instanceof ServiceTask) {
            boolean isExternal = isExternalServiceCall((ServiceTask) task);

            if (isExternal) {
                return OperationType.EXTERNAL_CALL;
            }
            return OperationType.DATA_PROCESSING;
        }

        if (task instanceof ScriptTask) {
            return OperationType.DATA_PROCESSING;
        }

        return OperationType.OTHER;
    }

    private boolean isExternalServiceCall(ServiceTask serviceTask) {
        if (serviceTask.getExtensionElements() == null) {
            return false;
        }
        //TODO: разбирать camunda:connector / camunda:type и т.д.
        String rawXml = serviceTask.getDomElement().getTextContent();
        if (rawXml == null) return false;
        return rawXml.contains("http") || rawXml.contains("rest");
    }
}
