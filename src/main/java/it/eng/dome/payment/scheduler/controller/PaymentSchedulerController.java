package it.eng.dome.payment.scheduler.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import it.eng.dome.tmforum.tmf678.v4.JSON;
import it.eng.dome.tmforum.tmf678.v4.model.CustomerBill;


@RestController
@RequestMapping("/payment")
public class PaymentSchedulerController {
	private static final Logger logger = LoggerFactory.getLogger(PaymentSchedulerController.class);


	@RequestMapping(value = "/pay", method = RequestMethod.POST, produces =  MediaType.APPLICATION_JSON_VALUE, consumes =  MediaType.APPLICATION_JSON_VALUE)
	public String pay(@RequestBody String customerBill) throws Throwable {
		logger.info("Received request to pay:\n{}", customerBill);
		
		CustomerBill customer = JSON.getGson().fromJson(customerBill, CustomerBill.class);
		logger.info("CustomerId received: {}", customer.getId());
		
		//TODO verify payment
		
		//TODO redirect to EG APIs
		
		//TODO save in TMForum
		return "Hello";
	}
}