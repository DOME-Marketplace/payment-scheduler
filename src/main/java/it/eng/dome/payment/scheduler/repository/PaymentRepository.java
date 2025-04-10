package it.eng.dome.payment.scheduler.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import it.eng.dome.payment.scheduler.entity.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

	Payment findByPaymentId(String paymentId);

}
