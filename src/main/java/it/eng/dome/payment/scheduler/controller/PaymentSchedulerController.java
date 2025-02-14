package it.eng.dome.payment.scheduler.controller;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import it.eng.dome.payment.scheduler.dto.StartRequestDTO;
import it.eng.dome.payment.scheduler.service.PaymentService;
import it.eng.dome.tmforum.tmf678.v4.JSON;
import it.eng.dome.tmforum.tmf678.v4.model.AppliedCustomerBillingRate;


@RestController
@RequestMapping("/payment")
public class PaymentSchedulerController {
	
	private static final Logger logger = LoggerFactory.getLogger(PaymentSchedulerController.class);

	@Autowired
	protected PaymentService paymentService;

	@RequestMapping(value = "/pay", method = RequestMethod.POST, produces =  MediaType.APPLICATION_JSON_VALUE, consumes =  MediaType.APPLICATION_JSON_VALUE)
	public String pay(@RequestBody String applied) throws Throwable {
		logger.info("Received request with appliedCustomerBillingRates");
		
		AppliedCustomerBillingRate[] appliedCustomerBillingRates = JSON.getGson().fromJson(applied, AppliedCustomerBillingRate[].class);
		logger.info("Number of AppliedCustomerBillingRates received: {}", appliedCustomerBillingRates.length);
				
		//TODO verify payment
		//filter appliedCustomerBillingRates
		List<AppliedCustomerBillingRate> appliedList = new ArrayList<>();
		for (AppliedCustomerBillingRate appliedCustomerBillingRate : appliedCustomerBillingRates) {
			if (verifyBill(appliedCustomerBillingRate)) {
				appliedList.add(appliedCustomerBillingRate);
			}
		}
		logger.debug("Number of appliedCustomerBillingRate to pay and save: {}", appliedList.size());
		//TODO redirect to EG APIs
		// call payment 
		
		//TODO save in TMForum
		List<String> ids = paymentService.saveBill(appliedList.toArray(new AppliedCustomerBillingRate[0]));
		logger.info("Number of AppliedCustomerBillingRate saved: {}",ids.size());
		logger.debug("AppliedCustomerBillingRate ids saved: {}", ids);
		return "Hello";
	}
	
	@RequestMapping(value = "/start", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public Map<String, String> startScheduler(@RequestBody StartRequestDTO datetime) throws Throwable {

		Map<String, String> response = new HashMap<String, String>();
		OffsetDateTime now = OffsetDateTime.now();
		try {
			String dt = datetime.getDatetime().toString();
			logger.debug("Set datetime manually at {}", dt);
			now = OffsetDateTime.parse(dt);
		} catch (Exception e) {
			logger.warn("Cannot recognize the datetime attribute! Please use the YYYY-MM-DDTHH:mm:ss format");
			response.put("msg", "Cannot recognize the datetime attribute! Please use the YYYY-MM-DDTHH:mm:ss format");
			response.put("err", e.getMessage());
		}

		logger.info("Start the scheduler task via REST APIs for payments");

		response.put("response", "Starting the payments from datetime: " + now);
		paymentService.payments(now);
		return response;
	}
	
	
	private boolean verifyBill(AppliedCustomerBillingRate applied) {
		logger.info("Verify if AppliedCustomerBillingRateId {} needs to be billed", applied.getId());
		return false; //!applied.getIsBilled().booleanValue();
	}

}