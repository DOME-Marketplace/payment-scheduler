package it.eng.dome.payment.scheduler.task;

import java.time.OffsetDateTime;

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

	private volatile OffsetDateTime lastExecutionTime;
	private volatile boolean lastExecutionSuccess = true;
	private volatile String lastErrorMessage = null;

	@Scheduled(cron = "${scheduling.cron}")
	public void paymentCycleTask() {
		
		OffsetDateTime now = OffsetDateTime.now();

		logger.info("Scheduling the payment cycle process at {}", now.format(PaymentDateUtils.formatter));

		try {
			paymentService.payments();
			lastExecutionSuccess = true;
			lastErrorMessage = null;
			
		} catch (Exception e) {
			lastExecutionSuccess = false;
			lastErrorMessage = e.getMessage();
			logger.error("Error during scheduled payment cycle", e);
		
		} finally {
			lastExecutionTime = now;
		}
	}
	
	public boolean isHealthy() {
        return lastExecutionSuccess;
    }
	
    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

    public OffsetDateTime getLastExecutionTime() {
        return lastExecutionTime;
    }

}
