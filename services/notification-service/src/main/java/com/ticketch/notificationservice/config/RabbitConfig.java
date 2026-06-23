package com.ticketch.notificationservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Notification Service RabbitMQ 설정.
 *
 * <p>notification 전용 큐로 payment.exchange의 결제 이벤트를 소비. (Reservation 큐와 별개)
 * Exchange/Queue는 이미 존재해도 Spring AMQP가 멱등하게 선언한다.
 */
@Configuration
public class RabbitConfig {

    public static final String PAYMENT_EXCHANGE       = "payment.exchange";
    public static final String PAYMENT_COMPLETED_KEY  = "payment.completed";
    public static final String PAYMENT_FAILED_KEY     = "payment.failed";
    public static final String PAYMENT_COMPLETED_Q    = "payment.completed.notification.queue";
    public static final String PAYMENT_FAILED_Q       = "payment.failed.notification.queue";

    @Bean
    public TopicExchange paymentExchange() {
        return new TopicExchange(PAYMENT_EXCHANGE);
    }

    @Bean
    public Queue paymentCompletedQueue() {
        return QueueBuilder.durable(PAYMENT_COMPLETED_Q).build();
    }

    @Bean
    public Queue paymentFailedQueue() {
        return QueueBuilder.durable(PAYMENT_FAILED_Q).build();
    }

    @Bean
    public Binding paymentCompletedBinding() {
        return BindingBuilder.bind(paymentCompletedQueue()).to(paymentExchange()).with(PAYMENT_COMPLETED_KEY);
    }

    @Bean
    public Binding paymentFailedBinding() {
        return BindingBuilder.bind(paymentFailedQueue()).to(paymentExchange()).with(PAYMENT_FAILED_KEY);
    }

    /** JSON 직렬화 — PaymentCompletedEvent/PaymentFailedEvent 역직렬화 */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
