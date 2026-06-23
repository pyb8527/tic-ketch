package com.ticketch.paymentservice.application.service;

import com.ticketch.common.exception.BusinessException;
import com.ticketch.events.payment.PaymentCompletedEvent;
import com.ticketch.events.payment.PaymentFailedEvent;
import com.ticketch.paymentservice.application.port.in.RequestPaymentUseCase;
import com.ticketch.paymentservice.application.port.in.RequestPaymentUseCase.PaymentResult;
import com.ticketch.paymentservice.application.port.out.LoadPaymentPort;
import com.ticketch.paymentservice.application.port.out.ProcessPaymentPort;
import com.ticketch.paymentservice.application.port.out.PublishPaymentEventPort;
import com.ticketch.paymentservice.application.port.out.SavePaymentPort;
import com.ticketch.paymentservice.application.port.out.ValidateReservationPort;
import com.ticketch.paymentservice.domain.model.Payment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * [Unit Test] PaymentService 순수 Mockito 단위 테스트.
 * Spring 컨텍스트·DB·실인프라 불필요.
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceTest.class);

    @Mock ValidateReservationPort validateReservationPort;
    @Mock LoadPaymentPort loadPaymentPort;
    @Mock SavePaymentPort savePaymentPort;
    @Mock ProcessPaymentPort processPaymentPort;
    @Mock PublishPaymentEventPort publishPaymentEventPort;

    @InjectMocks PaymentService paymentService;

    // ── 공통 save stub 헬퍼: 첫 저장(id=null)에만 id=1L 부여, 이후(id!=null)는 그대로 반환 ──
    private void stubSaveWithId() {
        when(savePaymentPort.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            if (p.getId() != null) return p;
            return Payment.builder()
                    .id(1L)
                    .reservationId(p.getReservationId())
                    .userId(p.getUserId())
                    .amount(p.getAmount())
                    .status(p.getStatus())
                    .failureReason(p.getFailureReason())
                    .paidAt(p.getPaidAt())
                    .createdAt(p.getCreatedAt())
                    .build();
        });
    }

    @Test
    @DisplayName("결제 성공 시 PaymentCompletedEvent 발행")
    void 결제_성공시_PaymentCompletedEvent_발행() {
        // given
        log.info("결제 성공 시나리오 — publishCompleted 호출 검증");
        stubSaveWithId();

        when(validateReservationPort.getReservation(anyLong(), anyLong()))
                .thenReturn(new ValidateReservationPort.ReservationInfo(10L, 1L, "PENDING"));
        when(processPaymentPort.process(anyLong(), any()))
                .thenReturn(new ProcessPaymentPort.ProcessResult(true, null));

        // when
        PaymentResult result = paymentService.requestPayment(
                new RequestPaymentUseCase.RequestPaymentCommand(100L, 5L, 50000));

        // then
        log.info("결제 결과 status={}", result.status());
        verify(publishPaymentEventPort).publishCompleted(any(PaymentCompletedEvent.class));
        verify(publishPaymentEventPort, never()).publishFailed(any(PaymentFailedEvent.class));
        assertThat(result.status()).isEqualTo("COMPLETED");
    }

    @Test
    @DisplayName("결제 실패 시 PaymentFailedEvent 발행")
    void 결제_실패시_PaymentFailedEvent_발행() {
        // given
        log.info("결제 실패 시나리오 — publishFailed 호출 검증");
        stubSaveWithId();

        when(validateReservationPort.getReservation(anyLong(), anyLong()))
                .thenReturn(new ValidateReservationPort.ReservationInfo(10L, 1L, "PENDING"));
        when(processPaymentPort.process(anyLong(), any()))
                .thenReturn(new ProcessPaymentPort.ProcessResult(false, "거절"));

        // when
        PaymentResult result = paymentService.requestPayment(
                new RequestPaymentUseCase.RequestPaymentCommand(100L, 5L, 50000));

        // then
        log.info("결제 결과 status={}", result.status());
        verify(publishPaymentEventPort).publishFailed(any(PaymentFailedEvent.class));
        verify(publishPaymentEventPort, never()).publishCompleted(any(PaymentCompletedEvent.class));
        assertThat(result.status()).isEqualTo("FAILED");
    }

    @Test
    @DisplayName("예약 상태가 PENDING이 아니면 BusinessException 발생")
    void 예약상태가_PENDING이_아니면_예외() {
        // given
        log.info("예약 상태 CONFIRMED — BusinessException 발생 검증");
        when(validateReservationPort.getReservation(anyLong(), anyLong()))
                .thenReturn(new ValidateReservationPort.ReservationInfo(10L, 1L, "CONFIRMED"));

        // when / then
        assertThatThrownBy(() -> paymentService.requestPayment(
                new RequestPaymentUseCase.RequestPaymentCommand(100L, 5L, 50000)))
                .isInstanceOf(BusinessException.class);

        log.info("BusinessException 정상 발생 확인 — 이벤트 발행 없음 검증");
        verify(publishPaymentEventPort, never()).publishCompleted(any());
        verify(publishPaymentEventPort, never()).publishFailed(any());
    }
}
