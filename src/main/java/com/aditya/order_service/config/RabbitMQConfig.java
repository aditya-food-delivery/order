package com.aditya.order_service.config;


import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "order.exchange";

    // Queues
    public static final String PAYMENT_INITIATED_QUEUE = "payment.initiated.queue";
    public static final String PAYMENT_COMPLETED_QUEUE = "payment.completed.queue";
    public static final String PAYMENT_FAILED_QUEUE = "payment.failed.queue";

    // Routing Keys
    public static final String PAYMENT_INITIATED_KEY = "payment.initiated";
    public static final String PAYMENT_COMPLETED_KEY = "payment.completed";
    public static final String PAYMENT_FAILED_KEY = "payment.failed";

    // Exchange
    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE);
    }

    // Queues
    @Bean
    public Queue paymentInitiatedQueue() {
        return QueueBuilder.durable(PAYMENT_INITIATED_QUEUE).build();
    }

    @Bean
    public Queue paymentCompletedQueue() {
        return QueueBuilder.durable(PAYMENT_COMPLETED_QUEUE).build();
    }

    @Bean
    public Queue paymentFailedQueue() {
        return QueueBuilder.durable(PAYMENT_FAILED_QUEUE).build();
    }

    // Bindings
    @Bean
    public Binding initiatedBinding() {
        return BindingBuilder.bind(paymentInitiatedQueue())
                .to(exchange())
                .with(PAYMENT_INITIATED_KEY);
    }

    @Bean
    public Binding completedBinding() {
        return BindingBuilder.bind(paymentCompletedQueue())
                .to(exchange())
                .with(PAYMENT_COMPLETED_KEY);
    }

    @Bean
    public Binding failedBinding() {
        return BindingBuilder.bind(paymentFailedQueue())
                .to(exchange())
                .with(PAYMENT_FAILED_KEY);
    }
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}