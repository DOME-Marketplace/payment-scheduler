package it.eng.dome.payment.scheduler.controller;

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
		float taxIncludedAmount = 10;

		PaymentStartNonInteractive paymentStartNonInteractive = PaymentStartNonInteractiveUtils.getPaymentStartNonInteractive(customerId, customerOrganizationId, invoiceId, productProviderExternalId, taxIncludedAmount, currency, paymentPreAuthorizationExternalId);

		System.out.println(paymentStartNonInteractive.toJson());
	}

}
