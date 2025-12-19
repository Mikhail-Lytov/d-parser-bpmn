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

    public static final String FROM_SPPR_CREATE_GRAPH_EXCHANGE = "d-sppr.create-graph.exchange";

    public static final String FROM_SPPR_CREATE_GRAPH_QUEUE = "d-sppr.create-graph.queue";

    public static final String FROM_SPPR_MARKING_EXCHANGE = "d-sppr.marking.exchange";

    public static final String FROM_SPPR_MARKING_QUEUE = "d-sppr.marking.queue";

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

    @Bean
    public FanoutExchange fromSpprCreateGraphExchange() {
        return new FanoutExchange(FROM_SPPR_CREATE_GRAPH_EXCHANGE);
    }

    @Bean
    public Queue fromSpprCreateGraphQueue() {
        return new Queue(FROM_SPPR_CREATE_GRAPH_QUEUE);
    }

    @Bean
    public Binding fromSpprCreateGraphBinding(
            FanoutExchange fromSpprCreateGraphExchange,
            Queue fromSpprCreateGraphQueue
    ) {
        return BindingBuilder
                .bind(fromSpprCreateGraphQueue)
                .to(fromSpprCreateGraphExchange);
    }

    @Bean
    public FanoutExchange fromCoreMarkingExchange() {
        return new FanoutExchange(FROM_SPPR_MARKING_EXCHANGE);
    }

    @Bean
    public Queue fromCoreMarkingQueue() {
        return new Queue(FROM_SPPR_MARKING_QUEUE);
    }

    @Bean
    public Binding fromCoreMarkingBinding(
            FanoutExchange fromCoreMarkingExchange,
            Queue fromCoreMarkingQueue
    ) {
        return BindingBuilder
                .bind(fromCoreMarkingQueue)
                .to(fromCoreMarkingExchange);
    }
}
