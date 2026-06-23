package com.ticketch.paymentservice.adapter.out.mock;

import com.ticketch.paymentservice.application.port.out.ProcessPaymentPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Random;

/**
 * 목업 PG 어댑터.
 * 90% 성공 / 10% 실패 시뮬레이션 (실제 PG 미연동).
 */
@Slf4j
@Component
public class MockPgAdapter implements ProcessPaymentPort {

	private final Random random = new Random();

	/**
	 * 결제를 처리한다 (목업).
	 *
	 * @param reservationId 예약 ID
	 * @param amount 결제액
	 * @return 결제 처리 결과
	 */
	@Override
	public ProcessResult process(Long reservationId, Integer amount) {
		if (random.nextInt(100) < 90) {
			log.info("결제 성공 - reservationId: {}, amount: {}", reservationId, amount);
			return new ProcessResult(true, null);
		}
		log.warn("결제 실패 - reservationId: {}, amount: {} (10% 실패 시뮬레이션)", reservationId, amount);
		return new ProcessResult(false, "목업 PG 결제 거절 (10% 실패 시뮬레이션)");
	}
}
