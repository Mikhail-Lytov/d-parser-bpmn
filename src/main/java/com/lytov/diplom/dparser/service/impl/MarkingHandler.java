package com.lytov.diplom.dparser.service.impl;

import com.lytov.diplom.dparser.configuration.rabbit.DCoreRMQConfig;
import com.lytov.diplom.dparser.service.dto.AnalyzeRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarkingHandler {

    private final MarkingService markingService;

    @RabbitListener(queues = DCoreRMQConfig.FROM_SPPR_MARKING_QUEUE)
    public void handlerMarking(@Payload AnalyzeRequest request) {
        try {
            markingService.marking(request);
        } catch (Exception e) {
            log.error("Error marking, {}", e.getMessage());
        }
    }
}
