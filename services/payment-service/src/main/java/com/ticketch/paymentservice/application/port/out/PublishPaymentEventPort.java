package com.ticketch.paymentservice.application.port.out;

import com.ticketch.events.payment.PaymentCompletedEvent;
import com.ticketch.events.payment.PaymentFailedEvent;

/**
 * 결제 이벤트 발행 Output Port (RabbitMQ payment.exchange).
 * 결제 완료/실패 이벤트를 메시지 브로커에 발행한다.
 */
public interface PublishPaymentEventPort {
	/**
	 * 결제 완료 이벤트를 발행한다.
	 *
	 * @param event 결제 완료 이벤트
	 */
	void publishCompleted(PaymentCompletedEvent event);

	/**
	 * 결제 실패 이벤트를 발행한다.
	 *
	 * @param event 결제 실패 이벤트
	 */
	void publishFailed(PaymentFailedEvent event);
}
