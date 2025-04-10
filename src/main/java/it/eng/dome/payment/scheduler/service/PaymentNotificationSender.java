package it.eng.dome.payment.scheduler.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.eng.dome.payment.scheduler.dto.PaymentStatus;

@Component
public class PaymentNotificationSender {
	
	private static final Logger logger = LoggerFactory.getLogger(PaymentNotificationSender.class);

	@Autowired
	private AmqpTemplate amqpTemplate;

	public void sendPaymentStatus(String paymentId, String status) {
		PaymentStatus paymentStatus = new PaymentStatus();
		paymentStatus.setPaymentId(paymentId);
		paymentStatus.setStatus(status);
		
		logger.info("Send status: {}", paymentStatus.getStatus());

		amqpTemplate.convertAndSend("paymentExchange", "payment.status", paymentStatus);
	}
}
