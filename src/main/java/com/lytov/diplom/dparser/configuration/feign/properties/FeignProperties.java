package com.lytov.diplom.dparser.configuration.feign.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "library.feign")
public class FeignProperties {

    private List<String> excludeUrls = new ArrayList<>();

}
