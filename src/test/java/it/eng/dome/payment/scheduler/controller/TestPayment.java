package it.eng.dome.payment.scheduler.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import it.eng.dome.payment.scheduler.dto.PaymentItem;
import it.eng.dome.payment.scheduler.dto.PaymentStartNonInteractive;
import it.eng.dome.payment.scheduler.util.PaymentStartNonInteractiveUtils;

public class TestPayment {

	public static void main(String[] args) {
		
		String customerId = "1";
		String customerOrganizationId = "1"; 
		String invoiceId = "ab-132";
		String productProviderExternalId = "eda11ca9-cf3b-420d-8570-9d3ecf3613ac"; 
		String currency = "EUR";
		
		String paymentPreAuthorizationExternalId = "9d4fca3b-4bfa-4dba-a09f-348b8d504e44";
		
		// TODO => how to set randomExternalId
		String randomExternalId = "479c2a6d-5197-452c-ba1b-fd1393c5" + (1000 + new Random().nextInt(9000));
		PaymentStartNonInteractive paymentStartNonInteractive = PaymentStartNonInteractiveUtils.getPaymentStartNonInteractive(paymentPreAuthorizationExternalId, randomExternalId, customerId, customerOrganizationId, invoiceId);
		
		
		PaymentItem paymentItem = new PaymentItem();
    	float amount = 10;
    	paymentItem.setAmount(amount);
    	paymentItem.setCurrency(currency);
    	paymentItem.setProductProviderExternalId(productProviderExternalId);
    	paymentItem.setRecurring(true);
    	
    	Map<String, String> attrs = new HashMap<String, String>();
		// attrs.put("additionalProp1", "data1");
    	paymentItem.setProductProviderSpecificData(attrs);
    	
    	paymentStartNonInteractive.getBaseAttributes().addPaymentItem(paymentItem);

		System.out.println(paymentStartNonInteractive.toJson());
	}

}
