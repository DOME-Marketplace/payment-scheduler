package it.eng.dome.payment.scheduler.controller;

import java.time.OffsetDateTime;
import java.util.List;

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
import it.eng.dome.tmforum.tmf678.v4.model.CustomerBill;

@RestController
@RequestMapping("/payment")
public class PaymentSchedulerController {

	private static final Logger logger = LoggerFactory.getLogger(PaymentSchedulerController.class);

	@Autowired
	protected PaymentService paymentService;

	
	@RequestMapping(value = "/pay", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> pay(@RequestBody List<CustomerBill> cbs) throws Throwable {
		logger.info("Received request with CustomerBills");

		logger.info("Number of CustomerBills received: {}", cbs.size());

		String response = paymentService.payments(cbs);
		return new ResponseEntity<String>(response, HttpStatus.OK);
	}

	@RequestMapping(value = "/start", method = RequestMethod.POST)
	public ResponseEntity<String> startScheduler() throws Throwable {

		logger.info("Start the scheduler task via REST APIs for payments at: {}", OffsetDateTime.now().format(PaymentDateUtils.formatter));

		String response = paymentService.payments();
		return new ResponseEntity<String>(response, HttpStatus.OK);
	}

}