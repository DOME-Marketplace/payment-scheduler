package it.eng.dome.payment.scheduler.controller;

import java.util.HashMap;
import java.util.Map;

import it.eng.dome.payment.scheduler.dto.PaymentItem;
import it.eng.dome.payment.scheduler.dto.PaymentStartNonInteractive;
import it.eng.dome.payment.scheduler.util.PaymentStartNonInteractiveUtils;

public class TestPayment {

	public static void main(String[] args) {
		
		String customerOrganizationId = "1"; 
		String productProviderExternalId = "eda11ca9-cf3b-420d-8570-9d3ecf3613ac"; 
		String currency = "EUR";
		
		String paymentPreAuthorizationExternalId = "9d4fca3b-4bfa-4dba-a09f-348b8d504e44";
		
		PaymentStartNonInteractive paymentStartNonInteractive = PaymentStartNonInteractiveUtils.getPaymentStartNonInteractive(paymentPreAuthorizationExternalId, customerOrganizationId);
		
		
		PaymentItem paymentItem = new PaymentItem();
    	float amount = 10;
    	paymentItem.setAmount(amount);
    	paymentItem.setCurrency(currency);
    	paymentItem.setProductProviderExternalId(productProviderExternalId);
    	paymentItem.setRecurring(true);
    	paymentItem.setPaymentItemExternalId("id-applied");
    	
    	Map<String, String> attrs = new HashMap<String, String>();
		// attrs.put("additionalProp1", "data1");
    	paymentItem.setProductProviderSpecificData(attrs);
    	
    	paymentStartNonInteractive.getBaseAttributes().addPaymentItem(paymentItem);

		System.out.println(paymentStartNonInteractive.toJson());
	}

}
