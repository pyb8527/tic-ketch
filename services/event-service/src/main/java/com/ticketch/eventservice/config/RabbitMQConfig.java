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

    @Bean
    public TopicExchange seatExchange() {
        return new TopicExchange(SEAT_EXCHANGE);
    }

    @Bean
    public Queue seatReleasedQueue() {
        return QueueBuilder.durable(SEAT_RELEASED_Q).build();
    }

    @Bean
    public Binding seatReleasedBinding() {
        return BindingBuilder.bind(seatReleasedQueue()).to(seatExchange()).with(SEAT_RELEASED_KEY);
    }

    /** JSON 직렬화 — SeatReleasedEvent 역직렬화 */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
