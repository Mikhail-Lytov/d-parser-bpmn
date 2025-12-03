package com.lytov.diplom.dparser.configuration.feign.logger;

import com.lytov.diplom.dparser.configuration.feign.properties.FeignProperties;
import feign.Logger;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@ConditionalOnClass(name = {"feign.Logger", "org.springframework.cloud.openfeign.FeignLoggerFactory"})
public class SingleLineTraceFeignLoggerFactory implements org.springframework.cloud.openfeign.FeignLoggerFactory {

    private final FeignProperties feignProperties;

    @Override
    public Logger create(Class<?> type) {
        return new CustomizableFeignLogger(new FeignLogProcessor(type, feignProperties));
    }
}
