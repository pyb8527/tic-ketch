package com.ticketch.eventservice.config;

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
 * Event Service RabbitMQ 설정.
 *
 * <p>seat.exchange의 seat.released 이벤트를 소비하여 좌석 상태를 복원한다.
 * Exchange/Queue는 이미 존재해도 Spring AMQP가 멱등하게 선언한다.
 */
@Configuration
public class RabbitMQConfig {

    public static final String SEAT_EXCHANGE     = "seat.exchange";
    public static final String SEAT_RELEASED_KEY = "seat.released";
    public static final String SEAT_RELEASED_Q   = "seat.released.queue";
    public static final String SEAT_HELD_KEY     = "seat.held";
    public static final String SEAT_HELD_Q       = "seat.held.queue";

    // 결제 완료 시 좌석을 SOLD로 변경하기 위해 payment.exchange도 구독한다.
    public static final String PAYMENT_EXCHANGE      = "payment.exchange";
    public static final String PAYMENT_COMPLETED_KEY = "payment.completed";
    public static final String PAYMENT_COMPLETED_Q   = "payment.completed.event.queue";

    @Bean
    public TopicExchange seatExchange() {
        return new TopicExchange(SEAT_EXCHANGE);
    }

    @Bean
    public TopicExchange paymentExchange() {
        return new TopicExchange(PAYMENT_EXCHANGE);
    }

    // ── seat.released → AVAILABLE ──────────────────────────────
    @Bean
    public Queue seatReleasedQueue() {
        return QueueBuilder.durable(SEAT_RELEASED_Q).build();
    }

    @Bean
    public Binding seatReleasedBinding() {
        return BindingBuilder.bind(seatReleasedQueue()).to(seatExchange()).with(SEAT_RELEASED_KEY);
    }

    // ── seat.held → HELD ───────────────────────────────────────
    @Bean
    public Queue seatHeldQueue() {
        return QueueBuilder.durable(SEAT_HELD_Q).build();
    }

    @Bean
    public Binding seatHeldBinding() {
        return BindingBuilder.bind(seatHeldQueue()).to(seatExchange()).with(SEAT_HELD_KEY);
    }

    // ── payment.completed → SOLD ───────────────────────────────
    @Bean
    public Queue paymentCompletedEventQueue() {
        return QueueBuilder.durable(PAYMENT_COMPLETED_Q).build();
    }

    @Bean
    public Binding paymentCompletedEventBinding() {
        return BindingBuilder.bind(paymentCompletedEventQueue()).to(paymentExchange()).with(PAYMENT_COMPLETED_KEY);
    }

    /** JSON 직렬화 — Seat / Payment 이벤트 역직렬화 */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
