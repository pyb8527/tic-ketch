package com.ticketch.reservationservice.adapter.in.messaging;

import com.ticketch.events.payment.PaymentCompletedEvent;
import com.ticketch.events.payment.PaymentFailedEvent;
import com.ticketch.events.reservation.SeatReleasedEvent;
import com.ticketch.reservationservice.application.port.in.CancelReservationUseCase;
import com.ticketch.reservationservice.application.port.in.ConfirmReservationUseCase;
import com.ticketch.reservationservice.config.RabbitConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * [In Adapter] 결제 이벤트 RabbitMQ 컨슈머.
 *
 * <p>Payment Service에서 발행한 결제 완료/실패 이벤트를 수신하여
 * 예약의 상태를 변경하거나 취소하는 업스트림 액터 역할을 수행한다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {

    private final ConfirmReservationUseCase confirmReservationUseCase;
    private final CancelReservationUseCase cancelReservationUseCase;

    /**
     * 결제 완료 이벤트를 처리한다.
     *
     * <p>예약 상태를 PENDING → CONFIRMED로 변경한다.
     *
     * @param event 결제 완료 이벤트
     */
    @RabbitListener(queues = RabbitConfig.PAYMENT_COMPLETED_Q)
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        log.info("결제 완료 이벤트 수신: reservationId={}, paymentId={}", event.getReservationId(), event.getPaymentId());
        confirmReservationUseCase.confirm(event.getReservationId());
    }

    /**
     * 결제 실패 이벤트를 처리한다.
     *
     * <p>예약을 취소하고 좌석을 해제하며, 사유를 PAYMENT_FAILED로 기록한다.
     *
     * @param event 결제 실패 이벤트
     */
    @RabbitListener(queues = RabbitConfig.PAYMENT_FAILED_Q)
    public void onPaymentFailed(PaymentFailedEvent event) {
        log.info("결제 실패 이벤트 수신: reservationId={}, paymentId={}, reason={}", event.getReservationId(), event.getPaymentId(), event.getReason());
        cancelReservationUseCase.cancel(
            new CancelReservationUseCase.CancelCommand(
                event.getReservationId(),
                null,
                SeatReleasedEvent.ReleaseReason.PAYMENT_FAILED
            )
        );
    }
}
