package it.eng.dome.payment.scheduler.controller;

import it.eng.dome.payment.scheduler.dto.PaymentStartNonInteractive;
import it.eng.dome.payment.scheduler.util.PaymentStartNonInteractiveUtils;

public class TestPayment {

	public static void main(String[] args) {
		
		String customerId = "1";
		String customerOrganizationId = "1"; 
		String invoiceId = "ab-132";
		int productProviderId = 1; 
		String currency = "EUR";
		
		String paymentPreAuthorizationId = "bae4cd08-1385-4e81-aa6a-260ac2954f1c";
		float taxIncludedAmount = 10;

		PaymentStartNonInteractive paymentStartNonInteractive = PaymentStartNonInteractiveUtils.getPaymentStartNonInteractive(customerId, customerOrganizationId, invoiceId, productProviderId, taxIncludedAmount, currency, paymentPreAuthorizationId);

		System.out.println(paymentStartNonInteractive.toJson());
	}

}
