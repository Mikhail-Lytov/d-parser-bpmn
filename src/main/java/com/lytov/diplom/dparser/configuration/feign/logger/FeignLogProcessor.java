package com.lytov.diplom.dparser.configuration.feign.logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lytov.diplom.dparser.configuration.feign.properties.FeignProperties;
import feign.Request;
import feign.Response;
import feign.Util;
import lombok.SneakyThrows;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import static feign.Util.decodeOrDefault;
import static feign.Util.valuesOrEmpty;

@ConditionalOnClass(name = {"com.fasterxml.jackson.dataformat.xml.XmlMapper", "feign.Logger"})
public class FeignLogProcessor {

    private final ObjectMapper jsonMapper = new ObjectMapper();
    private final FeignProperties feignProperties;
    private final Logger log;

    public FeignLogProcessor(Class<?> className, FeignProperties feignProperties) {
        this.log = LoggerFactory.getLogger(className.getCanonicalName());
        this.feignProperties = feignProperties;
    }

    public void log(String configKey, String format, Object... args) {
        log.trace(format(configKey, format, args));
    }

    @SneakyThrows
    protected void logRequest(final String configKey, final feign.Logger.Level logLevel, final Request request) {
        List<String> logs = new ArrayList<>();

        logs.add(format(configKey, "\n---> %s %s HTTP/1.1", request.httpMethod().name(), request.url()));

        if (logLevel.ordinal() >= feign.Logger.Level.HEADERS.ordinal()) {
            logs.add("Headers:");

            for (String field : request.headers().keySet()) {
                for (String value : valuesOrEmpty(request.headers(), field)) {
                    logs.add(String.format("\t%s: %s", field, value));
                }
            }

            logs.add("Body:");

            int bodyLength = 0;
            if (request.body() != null) {
                bodyLength = request.body().length;
                if (logLevel.ordinal() >= feign.Logger.Level.FULL.ordinal()) {
                    String bodyText = getPrettyBody(
                            request.headers().get("Content-Type").stream().findFirst().orElse(""),
                            request.body(),
                            request.charset()
                    );

                    logs.add(String.format("%s", bodyText));
                }
            }

            logs.add(String.format("---> END HTTP (%s-byte body)", bodyLength));
        }
        log.trace(String.join("\n", logs));
    }

    protected Response logAndRebufferResponse(
            final String configKey,
            final feign.Logger.Level logLevel,
            final Response response,
            final long elapsedTime
    ) throws IOException {
        List<String> logs = new ArrayList<>();

        String reason = response.reason() != null && logLevel.compareTo(feign.Logger.Level.NONE) > 0 ?
                " " + response.reason() : "";

        int status = response.status();

        logs.add(format(
                configKey,
                "\n<--- %s %s HTTP/1.1 %s%s (%sms)",
                response.request().httpMethod().name(),
                response.request().url(),
                status,
                reason,
                elapsedTime
        ));

        if (logLevel.ordinal() >= feign.Logger.Level.HEADERS.ordinal()) {
            logs.add("Headers:");

            for (String field : response.headers().keySet()) {
                for (String value : valuesOrEmpty(response.headers(), field)) {
                    logs.add(String.format("\t%s: %s", field, value));
                }
            }

            logs.add("Body:");

            boolean displayBody = feignProperties.getExcludeUrls().stream().noneMatch(
                    pr -> response.request().url().matches(pr)
            );
            int bodyLength = 0;
            if (response.body() != null
                    && !(status == HttpStatus.NO_CONTENT.value() || status == HttpStatus.RESET_CONTENT.value())
                    && (displayBody || status != HttpStatus.OK.value())
            ) {
                // HTTP 204 No Content "...response MUST NOT include a message-body"
                // HTTP 205 Reset Content "...response MUST NOT include an entity"
                byte[] bodyData = Util.toByteArray(response.body().asInputStream());
                bodyLength = bodyData.length;

                if (logLevel.ordinal() >= feign.Logger.Level.FULL.ordinal() && bodyLength > 0) {
                    String bodyText = getPrettyBody(
                            response.headers().getOrDefault("Content-Type", new ArrayList<>())
                                    .stream()
                                    .findFirst()
                                    .orElse(""),
                            bodyData,
                            response.charset()
                    );

                    logs.add(String.format("%s", bodyText, "Binary data"));
                }

                logs.add(String.format("<--- END HTTP (%s-byte body)", bodyLength));

                log.debug(String.join("\n", logs));

                return response.toBuilder().body(bodyData).build();
            } else if (!displayBody) {
                logs.add("<--- END HTTP (body is hidden)");
            } else {
                logs.add(String.format("<--- END HTTP (%s-byte body)", bodyLength));
            }
        }

        log.trace(String.join("\n", logs));

        return response;
    }

    private String getPrettyBody(String contentType, byte[] bodyData, Charset charset) throws IOException {
        if (Strings.isBlank(contentType)) {
            return String.format("%s", decodeOrDefault(bodyData, charset, "Binary data"));
        }

        MediaType mediaType = MediaType.valueOf(contentType);

        if (mediaType.equalsTypeAndSubtype(MediaType.APPLICATION_JSON)) {
            return jsonMapper.readTree(bodyData).toPrettyString();
        } else {
            return String.format("%s", decodeOrDefault(bodyData, charset, "Binary data"));
        }
    }

    private String format(final String configKey, final String format, final Object... args) {
        return String.format(methodTag(configKey) + format, args);
    }

    private String methodTag(String configKey) {
        return '[' + configKey.substring(0, configKey.indexOf(40)) + "] ";
    }
}
