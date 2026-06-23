package com.ticketch.paymentservice.application.service;

import com.ticketch.common.exception.BusinessException;
import com.ticketch.common.exception.ErrorCode;
import com.ticketch.events.payment.PaymentCompletedEvent;
import com.ticketch.events.payment.PaymentFailedEvent;
import com.ticketch.paymentservice.application.port.in.CancelPaymentUseCase;
import com.ticketch.paymentservice.application.port.in.GetPaymentUseCase;
import com.ticketch.paymentservice.application.port.in.RequestPaymentUseCase;
import com.ticketch.paymentservice.application.port.out.LoadPaymentPort;
import com.ticketch.paymentservice.application.port.out.ProcessPaymentPort;
import com.ticketch.paymentservice.application.port.out.PublishPaymentEventPort;
import com.ticketch.paymentservice.application.port.out.SavePaymentPort;
import com.ticketch.paymentservice.application.port.out.ValidateReservationPort;
import com.ticketch.paymentservice.domain.model.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * [Application Service] 결제 핵심 비즈니스 로직 구현.
 *
 * <p>결제 프로세스: 예약 검증 → 결제 저장 → 결제 목업 처리 → 이벤트 발행
 * <ul>
 *   <li>requestPayment: 1) 예약 정보 검증 (PENDING 상태 확인) 2) 결제 정보 저장 3) 외부 PG 결제 처리 4) 성공/실패에 따라 이벤트 발행</li>
 *   <li>cancel: 결제 환불 처리 (COMPLETED 상태만 가능, 소유권 확인)</li>
 *   <li>getPayment: 결제 상세 조회 (소유권 확인)</li>
 * </ul>
 *
 * <p>모든 write 작업은 @Transactional로 관리되어 원자성을 보장한다.
 */
@Service
@RequiredArgsConstructor
public class PaymentService implements RequestPaymentUseCase, CancelPaymentUseCase, GetPaymentUseCase {

    private final ValidateReservationPort validateReservationPort;
    private final LoadPaymentPort loadPaymentPort;
    private final SavePaymentPort savePaymentPort;
    private final ProcessPaymentPort processPaymentPort;
    private final PublishPaymentEventPort publishPaymentEventPort;

    /**
     * 결제를 요청한다.
     *
     * <p>프로세스:
     * <ol>
     *   <li>예약 정보를 검증하고 PENDING 상태 확인</li>
     *   <li>결제 도메인 모델 생성 및 저장</li>
     *   <li>외부 PG(목업)에 결제 처리 요청</li>
     *   <li>결과에 따라 결제 상태 업데이트</li>
     *   <li>성공/실패 이벤트 발행</li>
     * </ol>
     *
     * @param command 결제 요청 커맨드 (userId, reservationId, amount)
     * @return 결제 결과 (paymentId, status)
     * @throws BusinessException INVALID_INPUT (예약 상태 비PENDING), RESERVATION_NOT_FOUND (예약 미존재)
     */
    @Transactional
    @Override
    public PaymentResult requestPayment(RequestPaymentCommand command) {
        // 1. 예약 정보 검증
        ValidateReservationPort.ReservationInfo info = validateReservationPort.getReservation(
                command.reservationId(),
                command.userId()
        );

        // 2. 예약 상태 확인
        if (!"PENDING".equals(info.status())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "결제 가능한 예약 상태가 아닙니다");
        }

        // 3. 결제 저장
        Payment payment = savePaymentPort.save(
                Payment.create(command.reservationId(), command.userId(), command.amount())
        );

        // 4. 외부 PG 결제 처리
        ProcessPaymentPort.ProcessResult result = processPaymentPort.process(
                command.reservationId(),
                command.amount()
        );

        // 5. 결과에 따라 상태 업데이트 및 이벤트 발행
        if (result.success()) {
            // 결제 성공
            payment.complete();
            savePaymentPort.save(payment);

            publishPaymentEventPort.publishCompleted(
                    PaymentCompletedEvent.builder()
                            .paymentId(payment.getId())
                            .reservationId(payment.getReservationId())
                            .userId(payment.getUserId())
                            .seatId(info.seatId())
                            .eventId(info.eventId())
                            .amount(payment.getAmount())
                            .paidAt(payment.getPaidAt())
                            .build()
            );
        } else {
            // 결제 실패
            payment.fail(result.failureReason());
            savePaymentPort.save(payment);

            publishPaymentEventPort.publishFailed(
                    PaymentFailedEvent.builder()
                            .paymentId(payment.getId())
                            .reservationId(payment.getReservationId())
                            .userId(payment.getUserId())
                            .seatId(info.seatId())
                            .eventId(info.eventId())
                            .reason(result.failureReason())
                            .failedAt(LocalDateTime.now())
                            .build()
            );
        }

        // 6. 결제 결과 반환
        return new PaymentResult(payment.getId(), payment.getStatus().name());
    }

    /**
     * 결제를 취소하고 환불한다.
     *
     * <p>COMPLETED 상태인 결제만 환불 가능하며, 요청자 소유권 확인 후 REFUNDED로 상태 변경.
     *
     * @param paymentId 결제 ID
     * @param userId 요청 사용자 ID (소유권 확인용)
     * @throws BusinessException PAYMENT_NOT_FOUND, ACCESS_DENIED, PAYMENT_FAILED, PAYMENT_ALREADY_REFUNDED
     */
    @Transactional
    @Override
    public void cancel(Long paymentId, Long userId) {
        Payment payment = loadPaymentPort.findById(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        payment.assertOwnedBy(userId);
        payment.refund();
        savePaymentPort.save(payment);
    }

    /**
     * 결제 정보를 조회한다.
     *
     * <p>요청자 소유권 확인 후 결제 상세 정보를 반환한다.
     *
     * @param paymentId 결제 ID
     * @param userId 요청 사용자 ID (소유권 확인용)
     * @return 결제 상세 정보 (id, reservationId, amount, status, paidAt)
     * @throws BusinessException PAYMENT_NOT_FOUND, ACCESS_DENIED
     */
    @Override
    public PaymentDetail getPayment(Long paymentId, Long userId) {
        Payment payment = loadPaymentPort.findById(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        payment.assertOwnedBy(userId);

        return new PaymentDetail(
                payment.getId(),
                payment.getReservationId(),
                payment.getAmount(),
                payment.getStatus().name(),
                payment.getPaidAt()
        );
    }
}
