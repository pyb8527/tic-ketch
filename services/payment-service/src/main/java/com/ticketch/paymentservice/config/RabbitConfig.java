package com.ticketch.paymentservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Payment Service RabbitMQ 설정.
 *
 * <p>Payment Service는 결제 완료/실패 이벤트를 발행한다(payment.exchange).
 * payment.exchange는 reservation과 notification이 소비하므로 멱등 중복 선언을 허용한다.
 * DLX/DLQ는 데드레터를 수집한다.
 */
@Configuration
public class RabbitConfig {

    // Payment Exchange & Routing Keys (발행 전용)
    public static final String PAYMENT_EXCHANGE = "payment.exchange";
    public static final String PAYMENT_COMPLETED_KEY = "payment.completed";
    public static final String PAYMENT_FAILED_KEY = "payment.failed";

    // Dead Letter Exchange & Queue
    public static final String PAYMENT_DLX = "payment.dlx";
    public static final String PAYMENT_DLQ = "payment.dlq";

    @Bean
    public TopicExchange paymentExchange() {
        return new TopicExchange(PAYMENT_EXCHANGE);
    }

    @Bean
    public FanoutExchange paymentDlx() {
        return new FanoutExchange(PAYMENT_DLX);
    }

    @Bean
    public Queue paymentDlq() {
        return QueueBuilder.durable(PAYMENT_DLQ).build();
    }

    @Bean
    public Binding dlqBinding() {
        return BindingBuilder.bind(paymentDlq()).to(paymentDlx());
    }

    /** JSON 직렬화/역직렬화 — PaymentCompletedEvent, PaymentFailedEvent */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }
}
