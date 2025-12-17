package com.lytov.diplom.dparser.configuration.rabbit;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DCoreRMQConfig {
    public static final String FROM_CORE_SECOND_PARS_RESULT_EXCHANGE = "d-core.second-pars-result.exchange";
    public static final String FROM_CORE_SECOND_PARS_RESULT_QUEUE = "d-core.second-pars-result.queue";

    @Bean
    public FanoutExchange fromCoreSecondParsResultExchange() {
        return new FanoutExchange(FROM_CORE_SECOND_PARS_RESULT_EXCHANGE);
    }

    @Bean
    public Queue fromCoreSecondParsResultQueue() {
        return new Queue(FROM_CORE_SECOND_PARS_RESULT_QUEUE);
    }

    @Bean
    public Binding fromCoreSecondParsResultBinding(
            FanoutExchange fromCoreSecondParsResultExchange,
            Queue fromCoreSecondParsResultQueue
    ) {
        return BindingBuilder
                .bind(fromCoreSecondParsResultQueue)
                .to(fromCoreSecondParsResultExchange);
    }
}
