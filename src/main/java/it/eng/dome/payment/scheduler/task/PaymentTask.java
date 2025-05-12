package it.eng.dome.payment.scheduler.task;

import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import it.eng.dome.payment.scheduler.service.PaymentService;
import it.eng.dome.payment.scheduler.util.PaymentDateUtils;


@Component
@EnableScheduling
public class PaymentTask {
	
	private static final Logger logger = LoggerFactory.getLogger(PaymentTask.class);

	@Autowired
	protected PaymentService paymentService;

	@Scheduled(cron = "${scheduling.cron}")
	public void paymentCycleTask() {
		logger.info("Scheduling the payment cycle process at {}", PaymentDateUtils.dateformat.format(new Date()));

		paymentService.payments();
	}

}
