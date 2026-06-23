package com.ticketch.notificationservice.adapter.in.messaging;

import com.ticketch.events.payment.PaymentCompletedEvent;
import com.ticketch.events.payment.PaymentFailedEvent;
import com.ticketch.notificationservice.application.port.in.SendNotificationUseCase;
import com.ticketch.notificationservice.application.port.in.SendNotificationUseCase.SendNotificationCommand;
import com.ticketch.notificationservice.config.RabbitConfig;
import com.ticketch.notificationservice.domain.model.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ 결제 이벤트 컨슈머.
 *
 * <p>Payment Service에서 발행한 PaymentCompletedEvent, PaymentFailedEvent를 구독하여
 * SendNotificationUseCase에 위임한다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentNotificationConsumer {

    private final SendNotificationUseCase sendNotificationUseCase;

    /**
     * 결제 완료 이벤트 핸들러.
     *
     * @param event PaymentCompletedEvent
     */
    @RabbitListener(queues = RabbitConfig.PAYMENT_COMPLETED_Q)
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        log.info(
                "💳 [결제 완료] userId={} reservationId={} amount={}",
                event.getUserId(),
                event.getReservationId(),
                event.getAmount());
        sendNotificationUseCase.send(
                new SendNotificationCommand(
                        event.getUserId(),
                        NotificationType.PAYMENT_COMPLETED,
                        event.getReservationId(),
                        event.getAmount()));
    }

    /**
     * 결제 실패 이벤트 핸들러.
     *
     * @param event PaymentFailedEvent
     */
    @RabbitListener(queues = RabbitConfig.PAYMENT_FAILED_Q)
    public void onPaymentFailed(PaymentFailedEvent event) {
        log.info(
                "❌ [결제 실패] userId={} reservationId={} reason={}",
                event.getUserId(),
                event.getReservationId(),
                event.getReason());
        sendNotificationUseCase.send(
                new SendNotificationCommand(
                        event.getUserId(),
                        NotificationType.PAYMENT_FAILED,
                        event.getReservationId(),
                        null));
    }
}
