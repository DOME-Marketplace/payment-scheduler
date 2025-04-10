package it.eng.dome.payment.scheduler.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import it.eng.dome.payment.scheduler.dto.PaymentStatus;
import it.eng.dome.payment.scheduler.entity.Payment;
import it.eng.dome.payment.scheduler.repository.PaymentRepository;
import it.eng.dome.payment.scheduler.service.PaymentNotificationSender;

@RestController
@RequestMapping("/payment")
public class PaymentController {
	
	private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);
	
	@Autowired
    private PaymentRepository paymentRepository;
	
	@Autowired
    private PaymentNotificationSender paymentNotificationSender;

    @PostMapping("/create")
    public ResponseEntity<String> createPayment(@RequestBody Payment payment) {

    	logger.info("PaymentId: {}", payment.getPaymentId());
    	
        //payment.setStatus(payment.getStatus());
        //payment.setBilled(false);
        paymentRepository.save(payment);
        
        logger.info("Saved paymentId test: {}", payment.getPaymentId());
        return ResponseEntity.ok("Payment created with ID: " + payment.getPaymentId());
    }

    @PostMapping("/notify")
    public ResponseEntity<String> notifyPayment(@RequestBody PaymentStatus paymentStatus) {

    	logger.info("PaymentId: {}", paymentStatus.getPaymentId());
    	logger.info("Status received: {}", paymentStatus.getStatus());

    	paymentNotificationSender.sendPaymentStatus(paymentStatus.getPaymentId(), paymentStatus.getStatus());
        return ResponseEntity.ok("Sent payment status: " + paymentStatus.getStatus() + " for paymentId: " + paymentStatus.getPaymentId());
    }
}
