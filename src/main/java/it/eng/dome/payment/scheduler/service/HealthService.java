package it.eng.dome.payment.scheduler.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.eng.dome.brokerage.api.AppliedCustomerBillRateApis;
import it.eng.dome.brokerage.api.ProductInventoryApis;
import it.eng.dome.brokerage.api.fetch.FetchUtils;
import it.eng.dome.brokerage.observability.AbstractHealthService;
import it.eng.dome.brokerage.observability.health.Check;
import it.eng.dome.brokerage.observability.health.Health;
import it.eng.dome.brokerage.observability.health.HealthStatus;
import it.eng.dome.brokerage.observability.info.Info;
import it.eng.dome.payment.scheduler.task.PaymentTask;
import it.eng.dome.payment.scheduler.tmf.TmfApiFactory;

@Service
public class HealthService extends AbstractHealthService implements InitializingBean {

	private final Logger logger = LoggerFactory.getLogger(HealthService.class);
	private final static String SERVICE_NAME = "Payment Scheduler";

	@Autowired
	private TmfApiFactory tmfApiFactory;

	@Autowired
	private PaymentTask paymentTask;

	private AppliedCustomerBillRateApis appliedApis;
	private ProductInventoryApis productApis;

	@Override
	public void afterPropertiesSet() throws Exception {
		logger.info("Initializing of HealthService");
		appliedApis = new AppliedCustomerBillRateApis(tmfApiFactory.getTMF678CustomerBillApiClient());
		productApis = new ProductInventoryApis(tmfApiFactory.getTMF637ProductInventoryApiClient());
	}

	@Override
	public Info getInfo() {
		Info info = super.getInfo();
		logger.debug("Response: {}", toJson(info));

		return info;
	}

	@Override
	public Health getHealth() {
		Health health = new Health();
	    health.setDescription("Health for the " + SERVICE_NAME);

	    health.elevateStatus(HealthStatus.PASS);

		// 1: check of the TMForum APIs dependencies
		for (Check c : getTMFChecks()) {
			health.addCheck(c);
	        health.elevateStatus(c.getStatus());
		}

		// 2: check dependencies: in case of FAIL or WARN set it to WARN
		boolean onlyDependenciesFailing = health.getChecks("self", null).stream()
				.allMatch(c -> c.getStatus() == HealthStatus.PASS);
		
	    if (onlyDependenciesFailing && health.getStatus() == HealthStatus.FAIL) {
	        health.setStatus(HealthStatus.WARN);
	    }
	    
	    // 3: check self info
	    for(Check c: getChecksOnSelf()) {
	    	health.addCheck(c);
	    	health.elevateStatus(c.getStatus());
        }
	    
	    // 4: check payment gateway API
	    for(Check c: getPaymentSchedulerCheck()) {
	    	health.addCheck(c);
	    	health.elevateStatus(c.getStatus());
        }
	    
	    // 5: build human-readable notes
	    health.setNotes(buildNotes(health));
		
		logger.debug("Health response: {}", toJson(health));
		
		return health;
	}
	
	
	private List<Check> getChecksOnSelf() {
	    List<Check> out = new ArrayList<>();

	    // 1️ - Check scheduler
	    HealthStatus schedulerStatus = paymentTask.isHealthy() ? HealthStatus.PASS : HealthStatus.FAIL;
	    
	    String schedulerOutput = paymentTask.isHealthy()
	            ? "Last run OK at " + paymentTask.getLastExecutionTime()
	            : "Last run failed at " + paymentTask.getLastExecutionTime() +
	              (paymentTask.getLastErrorMessage() != null ? " | Error: " + paymentTask.getLastErrorMessage() : "");
	    
	    Check schedulerCheck = createCheck("self", "scheduler", "payment-task", schedulerStatus, schedulerOutput);
	    out.add(schedulerCheck);

	    // 2️ - Check getInfo API
	    Info info = getInfo();
	    HealthStatus infoStatus = (info != null) ? HealthStatus.PASS : HealthStatus.FAIL;
	    String infoOutput = (info != null)
	            ? SERVICE_NAME + " version: " + info.getVersion()
	            : SERVICE_NAME + " getInfo returned unexpected response";
	    Check infoCheck = createCheck("self", "get-info", "api", infoStatus, infoOutput);
	    out.add(infoCheck);

	    return out;
	}


	/**
	 * Check connectivity with TMForum API
	 */
	private List<Check> getTMFChecks() {

		List<Check> out = new ArrayList<>();

		// TMF637
		Check tmf637 = createCheck("tmf-api", "connectivity", "tmf637");

		try {
			FetchUtils.streamAll(
			    productApis::listProducts,
			    null,
			    null,
			    1
			)
			.findAny();

			tmf637.setStatus(HealthStatus.PASS);
			
		} catch (Exception e) {
			tmf637.setStatus(HealthStatus.FAIL);
			tmf637.setOutput(e.toString());
		}

		out.add(tmf637);

		// TMF678
		Check tmf678 = createCheck("tmf-api", "connectivity", "tmf678");

		try {
			FetchUtils.streamAll(
					appliedApis::listAppliedCustomerBillingRates,
			    null,
			    null,
			    1
			)
			.findAny();
			
			tmf678.setStatus(HealthStatus.PASS);
		} catch (Exception e) {
			tmf678.setStatus(HealthStatus.FAIL);
			tmf678.setOutput(e.toString());
		}

		out.add(tmf678);

		return out;
	}
	
	/**
	 * Check connectivity with Payment Gateway 
	 */
	private List<Check> getPaymentSchedulerCheck() {

        List<Check> out = new ArrayList<>();

        Check payment = createCheck("payment-gateway", "connectivity", "EG");

        try {
            //TODO add API Payment Gateway
            payment.setStatus(HealthStatus.PASS);
        }
        catch(Exception e) {
			logger.error("Error:: {}", e.getMessage());
			payment.setStatus(HealthStatus.FAIL);
			payment.setOutput(e.toString());
        }
        out.add(payment);

        return out;
    }

}
