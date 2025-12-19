package com.lytov.diplom.dparser.service.impl;

import com.lytov.diplom.dparser.external.sppr_bd.SpprBdConnector;
import com.lytov.diplom.dparser.service.dto.AnalyzeRequest;
import com.lytov.diplom.dparser.service.dto.AnalyzeRow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnDiagram;
import org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnEdge;
import org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnPlane;
import org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnShape;
import org.camunda.bpm.model.bpmn.instance.dc.Bounds;
import org.camunda.bpm.model.bpmn.instance.di.Waypoint;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.File;
import java.io.IOException;
import org.camunda.bpm.model.bpmn.instance.Process;

import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarkingService {

    /**
     * Счётчик для генерации уникальных id.
     * Ранее по ошибке использовался org.yaml.snakeyaml.nodes.Tag.SEQ,
     * из-за чего id получались некорректными (и могли ломать сериализацию).
     */
    private static final AtomicLong SEQ = new AtomicLong(0);

    private final SpprBdConnector spprBdConnector;
    private final RestTemplate restTemplate;

    /**
     * Старый метод оставлен для совместимости.
     * Он создаёт размеченный BPMN во временном файле и пишет путь в лог.
     */
    public void marking(AnalyzeRequest request) {
        Path saved = markingToTempFile(request);
        log.info("Marked BPMN saved to: {} ({} bytes)", saved, safeSize(saved));
    }

    /**
     * Основной метод разметки: возвращает путь к созданному временному .bpmn файлу.
     * Это устраняет типовую причину "0 KB" (когда вызывающий код создаёт файл сам,
     * но MarkingService в него ничего не записывает).
     */
    public Path markingToTempFile(AnalyzeRequest request) {
        String downloadUrl = spprBdConnector.getDownloadUrl(request.getFileId());

        URI uri = UriComponentsBuilder
                .fromUriString(downloadUrl)
                .build(true)             // <- ВАЖНО: true = не кодировать заново
                .toUri();

        ResponseEntity<byte[]> file = restTemplate.exchange(
                uri,
                HttpMethod.GET,
                null,
                byte[].class
        );

        byte[] sourceBytes = file.getBody();
        if (sourceBytes == null || sourceBytes.length == 0) {
            throw new IllegalStateException("Downloaded BPMN is empty for fileId=" + request.getFileId());
        }

        File localFile;
        try {
            localFile = createFile(sourceBytes);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create local temp BPMN file", e);
        }

        BpmnModelInstance model = Bpmn.readModelFromFile(localFile);

        BpmnPlane plane = ensurePlane(model);
        Map<String, BpmnShape> shapeByElementId = indexShapes(plane);
        Map<String, BpmnEdge> edgeByElementId = indexEdges(plane);

        for (AnalyzeRow row : request.getRows()) {
            if ("NODE".equalsIgnoreCase(row.getBpmnType())) {
                markNode(model, plane, shapeByElementId, edgeByElementId, row.getRefId(), row.getRiskId());
            } else if ("EDGE".equalsIgnoreCase(row.getBpmnType())) {
                markEdge(model, plane, shapeByElementId, edgeByElementId, row.getRefId(), row.getRiskId());
            }
        }

        try {
            Path savePath = Paths.get("/Users/mihaillytov/Desktop/Диплом/реализация/backend/d-sppr-parent",
                    UUID.randomUUID() + ".bpmn");
            try (OutputStream out = Files.newOutputStream(savePath)) {
                Bpmn.writeModelToStream(out, model);
            }
            return savePath;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to write marked BPMN", e);
        }
    }

    /** Удобный вариант, если нужно вернуть файл в HTTP-ответе как байты. */
    public byte[] markingToBytes(AnalyzeRequest request) {
        Path p = markingToTempFile(request);
        try {
            return Files.readAllBytes(p);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read marked BPMN bytes", e);
        }
    }

    private static void markNode(
            BpmnModelInstance model,
            BpmnPlane plane,
            Map<String, BpmnShape> shapeByElementId,
            Map<String, BpmnEdge> edgeByElementId,
            String elementId,
            String riskId
    ) {
        ModelElementInstance el = model.getModelElementById(elementId);
        if (el == null) return;

        if (el instanceof Activity act) {
            BoundaryEvent boundary = addBoundaryMarker(model, act, riskId);

            // DI: boundary рядом с activity
            BpmnShape hostShape = shapeByElementId.get(act.getId());
            Bounds hostBounds = boundsOrFallback(model, hostShape, 100, 100, 140, 90);

            Bounds boundaryBounds = createBounds(model,
                    hostBounds.getX() + hostBounds.getWidth() - 10,
                    hostBounds.getY() - 10,
                    36, 36
            );

            BpmnShape boundaryShape = createShape(model, plane, boundary, boundaryBounds);
            shapeByElementId.put(boundary.getId(), boundaryShape);

        } else if (el instanceof BaseElement be) {
            // Для Event/Gateway/etc. — аннотация
            addAnnotationNearElement(model, plane, shapeByElementId, edgeByElementId, be, riskId);
        }
    }

    private static BoundaryEvent addBoundaryMarker(BpmnModelInstance model, Activity host, String riskId) {
        ModelElementInstance container = findOwningProcessOrSubProcess(host);

        BoundaryEvent be = model.newInstance(BoundaryEvent.class);
        be.setId("RiskBoundary_" + host.getId() + "_" + shortId(riskId) + "_" + SEQ.incrementAndGet());
        be.setName("Риск " + shortId(riskId));
        be.setAttachedTo(host);
        be.setCancelActivity(false);

        // Просто сохраняем riskId в documentation (универсально)
        Documentation doc = model.newInstance(Documentation.class);
        doc.setTextContent("RISK_ID=" + riskId);
        be.getDocumentations().add(doc);

        container.addChildElement(be);
        return be;
    }

    // -------------------- MARK EDGE --------------------

    private static void markEdge(
            BpmnModelInstance model,
            BpmnPlane plane,
            Map<String, BpmnShape> shapeByElementId,
            Map<String, BpmnEdge> edgeByElementId,
            String refId,
            String riskId
    ) {
        // refId иногда приходит составным: "EdgeId:Source->Target".
        // Для поиска элемента в модели нам нужен реальный id (обычно до двоеточия).
        String elementId = normalizeElementId(refId);

        // 1) пробуем найти по нормализованному id
        ModelElementInstance el = model.getModelElementById(elementId);

        BaseElement attachTo = null;

        if (el instanceof BaseElement be) {
            attachTo = pickAttachPointForEdge(be);
        }

        // 2) если refId составной: "...->Target" — пробуем прикрепить к target
        if (attachTo == null) {
            String targetId = parseTargetId(refId);
            if (targetId != null) {
                ModelElementInstance t = model.getModelElementById(targetId);
                if (t instanceof BaseElement tbe) attachTo = tbe;
            }
        }

        // 3) fallback: если есть source до "->" — пробуем прикрепить к source
        if (attachTo == null) {
            String sourceId = parseSourceId(refId);
            if (sourceId != null) {
                ModelElementInstance s = model.getModelElementById(sourceId);
                if (s instanceof BaseElement sbe) attachTo = sbe;
            }
        }

        if (attachTo != null) {
            addAnnotationNearElement(model, plane, shapeByElementId, edgeByElementId, attachTo, riskId + " (edge)");
        }
    }

    private static BaseElement pickAttachPointForEdge(BaseElement edgeElement) {
        if (edgeElement instanceof SequenceFlow sf) {
            FlowNode target = sf.getTarget();
            if (target != null) return target;
            FlowNode source = sf.getSource();
            if (source != null) return source;
        }
        if (edgeElement instanceof org.camunda.bpm.model.bpmn.instance.Association a) {
            BaseElement tgt = (BaseElement) a.getTarget();
            if (tgt != null) return tgt;
            BaseElement src = (BaseElement) a.getSource();
            if (src != null) return src;
        }

        // DataInputAssociation/DataOutputAssociation (DataAssociation) часто "внутри" Activity:
        // крепим к ближайшему Activity вверх по parent цепочке
        ModelElementInstance cur = edgeElement;
        while (cur != null) {
            if (cur instanceof Activity act) return act;
            cur = cur.getParentElement();
        }

        return null;
    }

    // -------------------- ANNOTATION + ASSOCIATION + DI --------------------

    private static void addAnnotationNearElement(
            BpmnModelInstance model,
            BpmnPlane plane,
            Map<String, BpmnShape> shapeByElementId,
            Map<String, BpmnEdge> edgeByElementId,
            BaseElement target,
            String riskText
    ) {
        ModelElementInstance container = findOwningProcessOrSubProcess(target);

        // 1) TextAnnotation
        TextAnnotation ta = model.newInstance(TextAnnotation.class);
        ta.setId("RiskNote_" + target.getId() + "_" + SEQ.incrementAndGet());

        Text txt = model.newInstance(Text.class);
        txt.setTextContent("Риск: " + riskText);
        ta.setText(txt);

        container.addChildElement(ta);

        // 2) Association (target -> annotation)
        org.camunda.bpm.model.bpmn.instance.Association assoc =
                model.newInstance(org.camunda.bpm.model.bpmn.instance.Association.class);
        assoc.setId("RiskAssoc_" + target.getId() + "_" + SEQ.incrementAndGet());
        assoc.setSource(target);
        assoc.setTarget(ta);

        container.addChildElement(assoc);

        // 3) DI placement
        BpmnShape targetShape = shapeByElementId.get(target.getId());
        Bounds targetBounds = boundsOrFallback(model, targetShape, 100, 100, 140, 90);

        Bounds noteBounds = createBounds(model,
                targetBounds.getX() + targetBounds.getWidth() + 30,
                targetBounds.getY(),
                240, 70
        );

        BpmnShape noteShape = createShape(model, plane, ta, noteBounds);
        shapeByElementId.put(ta.getId(), noteShape);

        BpmnEdge assocEdge = createEdge(model, plane, assoc,
                centerX(targetBounds), centerY(targetBounds),
                centerX(noteBounds), centerY(noteBounds)
        );
        edgeByElementId.put(assoc.getId(), assocEdge);
    }

    // -------------------- CONTAINER (NO FlowElementsContainer) --------------------

    /**
     * Находит ближайший Process или SubProcess — это корректный контейнер для добавления FlowElements
     * (BoundaryEvent/TextAnnotation/Association).
     */
    private static ModelElementInstance findOwningProcessOrSubProcess(ModelElementInstance element) {
        ModelElementInstance cur = element;
        while (cur != null) {
            if (cur instanceof SubProcess) return cur;
            if (cur instanceof Process) return cur;
            cur = cur.getParentElement();
        }
        throw new IllegalStateException("No Process/SubProcess found for element id=" + safeId(element));
    }

    private static String safeId(ModelElementInstance e) {
        if (e == null) return "null";
        if (e instanceof BaseElement be) return be.getId();
        return e.getElementType().getTypeName();
    }

    // -------------------- DI HELPERS --------------------

    private static BpmnPlane ensurePlane(BpmnModelInstance model) {
        Collection<BpmnDiagram> diagrams = model.getModelElementsByType(BpmnDiagram.class);
        BpmnDiagram diagram;

        if (diagrams.isEmpty()) {
            diagram = model.newInstance(BpmnDiagram.class);
            diagram.setId("BpmnDiagram_" + SEQ.incrementAndGet());
            model.getDefinitions().addChildElement(diagram);
        } else {
            diagram = diagrams.iterator().next();
        }

        BpmnPlane plane = diagram.getBpmnPlane();
        if (plane == null) {
            plane = model.newInstance(BpmnPlane.class);
            plane.setId("BpmnPlane_" + SEQ.incrementAndGet());

            Process p = firstProcess(model);
            if (p != null) plane.setBpmnElement(p);

            diagram.setBpmnPlane(plane);
        }

        return plane;
    }

    private static Process firstProcess(BpmnModelInstance model) {
        Collection<Process> processes = model.getModelElementsByType(Process.class);
        return processes.isEmpty() ? null : processes.iterator().next();
    }

    private static Map<String, BpmnShape> indexShapes(BpmnPlane plane) {
        Map<String, BpmnShape> map = new HashMap<>();
        for (BpmnShape s : plane.getChildElementsByType(BpmnShape.class)) {
            BaseElement be = s.getBpmnElement();
            if (be != null && be.getId() != null) map.put(be.getId(), s);
        }
        return map;
    }

    private static Map<String, BpmnEdge> indexEdges(BpmnPlane plane) {
        Map<String, BpmnEdge> map = new HashMap<>();
        for (BpmnEdge e : plane.getChildElementsByType(BpmnEdge.class)) {
            BaseElement be = e.getBpmnElement();
            if (be != null && be.getId() != null) map.put(be.getId(), e);
        }
        return map;
    }

    private static BpmnShape createShape(BpmnModelInstance model, BpmnPlane plane, BaseElement element, Bounds bounds) {
        BpmnShape shape = model.newInstance(BpmnShape.class);
        shape.setId("DI_Shape_" + element.getId() + "_" + SEQ.incrementAndGet());
        shape.setBpmnElement(element);
        shape.setBounds(bounds);
        plane.addChildElement(shape);
        return shape;
    }

    private static BpmnEdge createEdge(
            BpmnModelInstance model, BpmnPlane plane, BaseElement element,
            double x1, double y1, double x2, double y2
    ) {
        BpmnEdge edge = model.newInstance(BpmnEdge.class);
        edge.setId("DI_Edge_" + element.getId() + "_" + SEQ.incrementAndGet());
        edge.setBpmnElement(element);

        Waypoint w1 = model.newInstance(Waypoint.class);
        w1.setX(x1);
        w1.setY(y1);

        Waypoint w2 = model.newInstance(Waypoint.class);
        w2.setX(x2);
        w2.setY(y2);

        edge.getWaypoints().add(w1);
        edge.getWaypoints().add(w2);

        plane.addChildElement(edge);
        return edge;
    }

    private static Bounds createBounds(BpmnModelInstance model, double x, double y, double w, double h) {
        Bounds b = model.newInstance(Bounds.class);
        b.setX(x);
        b.setY(y);
        b.setWidth(w);
        b.setHeight(h);
        return b;
    }

    private static Bounds boundsOrFallback(BpmnModelInstance model, BpmnShape shape, double fx, double fy, double fw, double fh) {
        if (shape != null && shape.getBounds() != null) return shape.getBounds();
        return createBounds(model, fx, fy, fw, fh);
    }

    private static double centerX(Bounds b) { return b.getX() + b.getWidth() / 2.0; }
    private static double centerY(Bounds b) { return b.getY() + b.getHeight() / 2.0; }

    // -------------------- PARSING HELPERS --------------------

    /**
     * refId иногда приходит в "расширенном" виде:
     *   "ElementId:Source->Target"
     * Для поиска по model.getModelElementById нужен именно ElementId.
     */
    private static String normalizeElementId(String refId) {
        if (refId == null) return null;
        int colon = refId.indexOf(':');
        String id = (colon >= 0) ? refId.substring(0, colon) : refId;
        return id.trim();
    }

    /** refId вида "DataInputAssociation_X:Source->Target" */
    private static String parseTargetId(String compositeId) {
        if (compositeId == null) return null;
        int idx = compositeId.lastIndexOf("->");
        if (idx < 0) return null;
        return compositeId.substring(idx + 2).trim();
    }

    /** refId вида "DataInputAssociation_X:Source->Target" */
    private static String parseSourceId(String compositeId) {
        if (compositeId == null) return null;
        int colon = compositeId.indexOf(':');
        int arrow = compositeId.lastIndexOf("->");
        if (arrow < 0) return null;

        int start = (colon >= 0) ? colon + 1 : 0;
        String mid = compositeId.substring(start, arrow).trim();

        // mid может содержать "DataObjectReference_..", но иногда там ещё что-то — возвращаем как есть
        return mid.isEmpty() ? null : mid;
    }

    private static String shortId(String uuid) {
        if (uuid == null) return "NA";
        return uuid.length() <= 8 ? uuid : uuid.substring(0, 8);
    }

    private static long safeSize(Path path) {
        try {
            return Files.size(path);
        } catch (Exception e) {
            return -1L;
        }
    }
    private File createFile(byte[] data) throws IOException {
        String tmpdir = System.getProperty("java.io.tmpdir");
        Path filepath = Paths.get(tmpdir, UUID.randomUUID().toString());
        Files.write(filepath, data);
        return filepath.toFile();
    }
}
