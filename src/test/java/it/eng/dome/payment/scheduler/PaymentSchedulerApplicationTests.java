package it.eng.dome.payment.scheduler;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class PaymentSchedulerApplicationTests {

	@Autowired
	private PaymentSchedulerApplication paymentSchedulerApplication;

	@Test
	void contextLoads() {
		// to ensure that controller is getting created inside the application context
		assertNotNull(paymentSchedulerApplication);
	}

}
