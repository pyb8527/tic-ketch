package com.ticketch.reservationservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
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
 * Reservation Service RabbitMQ 설정.
 *
 * <p>Reservation Service는 결제 완료/실패 이벤트를 소비하고(payment.* queues),
 * 좌석 보유/해제 이벤트를 발행한다(seat.exchange).
 * seat exchange와 queues는 event-service가 소유하므로 여기서는 발행만 수행한다.
 */
@Configuration
public class RabbitConfig {

    // Seat Exchange & Routing Keys (발행 전용)
    public static final String SEAT_EXCHANGE = "seat.exchange";
    public static final String SEAT_HELD_KEY = "seat.held";
    public static final String SEAT_RELEASED_KEY = "seat.released";

    // Payment Exchange & Routing Keys (소비 대상)
    public static final String PAYMENT_EXCHANGE = "payment.exchange";
    public static final String PAYMENT_COMPLETED_KEY = "payment.completed";
    public static final String PAYMENT_FAILED_KEY = "payment.failed";

    // Payment Queues (이 서비스가 소비)
    public static final String PAYMENT_COMPLETED_Q = "payment.completed.reservation.queue";
    public static final String PAYMENT_FAILED_Q = "payment.failed.reservation.queue";

    @Bean
    public TopicExchange seatExchange() {
        return new TopicExchange(SEAT_EXCHANGE);
    }

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
        return BindingBuilder.bind(paymentCompletedQueue())
                .to(paymentExchange())
                .with(PAYMENT_COMPLETED_KEY);
    }

    @Bean
    public Binding paymentFailedBinding() {
        return BindingBuilder.bind(paymentFailedQueue())
                .to(paymentExchange())
                .with(PAYMENT_FAILED_KEY);
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
