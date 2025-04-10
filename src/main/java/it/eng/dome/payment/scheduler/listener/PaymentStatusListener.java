package it.eng.dome.payment.scheduler.listener;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.eng.dome.payment.scheduler.dto.PaymentStatus;
import it.eng.dome.payment.scheduler.entity.Payment;
import it.eng.dome.payment.scheduler.repository.PaymentRepository;

@Service
public class PaymentStatusListener {
	
	private static final Logger logger = LoggerFactory.getLogger(PaymentStatusListener.class);

	@Autowired
    private PaymentRepository paymentRepository;

    @RabbitListener(queues = "paymentStatusQueue")
    public void receivePaymentStatus(PaymentStatus paymentStatus) {
    	logger.info("Received PaymentStatus Listener ...");
    	
    	List<Payment> payments = paymentRepository.findAll();
    	logger.info("Number payments in wait: {}", payments.size());
    	for (Payment payment : payments) {
			logger.info(">> PaymentId: {}", payment.getPaymentId());
		}
    	

        Payment payment = paymentRepository.findByPaymentId(paymentStatus.getPaymentId());
                
        if (payment != null) {
        	logger.info("Get Payment by ID: {} from repository", payment.getPaymentId());
        	
        	logger.info("Action - Payment Status: {}", paymentStatus.getStatus());
            
            if ("paid".equals(paymentStatus.getStatus())) {
                paymentRepository.delete(payment);
                logger.info("Delete PaymentId {} with status {} from repository", paymentStatus.getPaymentId(), paymentStatus.getStatus());
            
            }else if ("failed".equals(paymentStatus.getStatus())) {
            	paymentRepository.delete(payment);
            	logger.info("Delete PaymentId {} with status {} from repository", paymentStatus.getPaymentId(), paymentStatus.getStatus());
            
            }else {
            	logger.info("No action in repository");
            }            
        }else {
        	logger.warn("PaymentId {} not found in the repository", paymentStatus.getPaymentId());
        }
    }
}
