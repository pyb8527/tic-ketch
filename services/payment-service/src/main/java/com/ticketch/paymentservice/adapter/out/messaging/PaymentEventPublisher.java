package com.ticketch.paymentservice.adapter.out.messaging;

import com.ticketch.events.payment.PaymentCompletedEvent;
import com.ticketch.events.payment.PaymentFailedEvent;
import com.ticketch.paymentservice.application.port.out.PublishPaymentEventPort;
import com.ticketch.paymentservice.config.RabbitConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ 기반 결제 이벤트 발행 어댑터.
 *
 * <p>PaymentCompletedEvent와 PaymentFailedEvent를 payment.exchange에 라우팅 키를 통해 발행한다.
 * RabbitTemplate을 사용하여 Jackson2JsonMessageConverter로 직렬화된 메시지를 전송한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventPublisher implements PublishPaymentEventPort {

	private final RabbitTemplate rabbitTemplate;

	/**
	 * 결제 완료 이벤트를 payment.exchange에 발행한다.
	 *
	 * @param event 결제 완료 이벤트
	 */
	@Override
	public void publishCompleted(PaymentCompletedEvent event) {
		rabbitTemplate.convertAndSend(RabbitConfig.PAYMENT_EXCHANGE, RabbitConfig.PAYMENT_COMPLETED_KEY, event);
		log.info("결제 완료 이벤트 발행 [paymentId={}]", event.getPaymentId());
	}

	/**
	 * 결제 실패 이벤트를 payment.exchange에 발행한다.
	 *
	 * @param event 결제 실패 이벤트
	 */
	@Override
	public void publishFailed(PaymentFailedEvent event) {
		rabbitTemplate.convertAndSend(RabbitConfig.PAYMENT_EXCHANGE, RabbitConfig.PAYMENT_FAILED_KEY, event);
		log.info("결제 실패 이벤트 발행 [paymentId={}]", event.getPaymentId());
	}
}
