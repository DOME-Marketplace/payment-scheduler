package it.eng.dome.payment.scheduler.controller;

import java.time.OffsetDateTime;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import it.eng.dome.payment.scheduler.service.PaymentService;
import it.eng.dome.payment.scheduler.util.PaymentDateUtils;
import it.eng.dome.tmforum.tmf678.v4.JSON;
import it.eng.dome.tmforum.tmf678.v4.model.AppliedCustomerBillingRate;

@RestController
@RequestMapping("/payment")
public class PaymentSchedulerController {

	private static final Logger logger = LoggerFactory.getLogger(PaymentSchedulerController.class);

	@Autowired
	protected PaymentService paymentService;

	@RequestMapping(value = "/pay", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> pay(@RequestBody String applied) throws Throwable {
		logger.info("Received request with appliedCustomerBillingRates");

		AppliedCustomerBillingRate[] appliedCustomerBillingRates = JSON.getGson().fromJson(applied,	AppliedCustomerBillingRate[].class);
		logger.info("Number of AppliedCustomerBillingRates received: {}", appliedCustomerBillingRates.length);

		String response = paymentService.executePayments(Arrays.asList(appliedCustomerBillingRates));
		return new ResponseEntity<String>(response, HttpStatus.OK);
	}

	@RequestMapping(value = "/start", method = RequestMethod.POST)
	public ResponseEntity<String> startScheduler() throws Throwable {

		logger.info("Start the scheduler task via REST APIs for payments");

		String response = "Starting the payments at " + OffsetDateTime.now().format(PaymentDateUtils.formatter);
		paymentService.payments();
		return new ResponseEntity<String>(response, HttpStatus.OK);
	}

}