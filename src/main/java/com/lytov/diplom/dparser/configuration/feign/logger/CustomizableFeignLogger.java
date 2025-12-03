package com.lytov.diplom.dparser.configuration.feign.logger;

import feign.Logger;
import feign.Request;
import feign.Response;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;

import java.io.IOException;

@ConditionalOnClass(name = {"feign.Logger"})
public class CustomizableFeignLogger extends Logger {

    private final FeignLogProcessor feignLogProcessor;

    public CustomizableFeignLogger(FeignLogProcessor feignLogProcessor) {
        this.feignLogProcessor = feignLogProcessor;
    }

    @Override
    protected void log(String s, String s1, Object... objects) {
        feignLogProcessor.log(s, s1, objects);
    }

    @Override
    protected void logRequest(String configKey, Level logLevel, Request request) {
        this.feignLogProcessor.logRequest(configKey, logLevel, request);
    }

    @Override
    protected Response logAndRebufferResponse(String configKey, Level logLevel, Response response, long elapsedTime) throws IOException {
        return this.feignLogProcessor.logAndRebufferResponse(configKey, logLevel, response, elapsedTime);
    }
}
