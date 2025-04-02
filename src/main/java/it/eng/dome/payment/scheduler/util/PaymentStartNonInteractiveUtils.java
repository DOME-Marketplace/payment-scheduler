package it.eng.dome.payment.scheduler.util;

import java.util.ArrayList;
import java.util.List;

import it.eng.dome.payment.scheduler.dto.BaseAttributes;
import it.eng.dome.payment.scheduler.dto.PaymentItem;
import it.eng.dome.payment.scheduler.dto.PaymentStartNonInteractive;

public class PaymentStartNonInteractiveUtils {

public static PaymentStartNonInteractive getPaymentStartNonInteractive(String paymentPreAuthorizationExternalId, String externalId, String customerId, String customerOrganizationId, String invoiceId) {
		
		BaseAttributes baseAttributes = new BaseAttributes();

		baseAttributes.setExternalId(externalId);
		baseAttributes.setCustomerId(customerId);
		baseAttributes.setCustomerOrganizationId(customerOrganizationId);
		baseAttributes.setInvoiceId(invoiceId);

		List<PaymentItem> paymentItems = new ArrayList<PaymentItem>();
		baseAttributes.setPaymentItems(paymentItems);
		
		PaymentStartNonInteractive paymentStartNonInteractive = new PaymentStartNonInteractive();
		paymentStartNonInteractive.setBaseAttributes(baseAttributes);
		paymentStartNonInteractive.setPaymentPreAuthorizationExternalId(paymentPreAuthorizationExternalId);

		return paymentStartNonInteractive;
	}
}
