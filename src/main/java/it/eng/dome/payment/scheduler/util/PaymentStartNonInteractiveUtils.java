package it.eng.dome.payment.scheduler.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import it.eng.dome.payment.scheduler.dto.BaseAttributes;
import it.eng.dome.payment.scheduler.dto.PaymentItem;
import it.eng.dome.payment.scheduler.dto.PaymentStartNonInteractive;

public class PaymentStartNonInteractiveUtils {
	

	public static PaymentStartNonInteractive getPaymentStartNonInteractive(String paymentPreAuthorizationExternalId, String customerOrganizationId) {
		
		BaseAttributes baseAttributes = new BaseAttributes();

		baseAttributes.setExternalId(UUID.randomUUID().toString()); // ExternalId must be unique, can be random or sequential
		baseAttributes.setCustomerOrganizationId(customerOrganizationId);
		baseAttributes.setInvoiceId(getInvoice());

		List<PaymentItem> paymentItems = new ArrayList<PaymentItem>();
		baseAttributes.setPaymentItems(paymentItems);
		
		PaymentStartNonInteractive paymentStartNonInteractive = new PaymentStartNonInteractive();
		paymentStartNonInteractive.setBaseAttributes(baseAttributes);
		paymentStartNonInteractive.setPaymentPreAuthorizationExternalId(paymentPreAuthorizationExternalId);

		return paymentStartNonInteractive;
	}


	static Map<String, Integer> counter = new HashMap<>();

	private static String getInvoice() {
		
		// get key for HashMap
		LocalDate now = LocalDate.now();
        String key = now.format(DateTimeFormatter.ofPattern("yyyy/MM"));
        
        int value = counter.getOrDefault(key, 1);
        
        // update value in the HashMap
        counter.put(key, value + 1);
        
		return String.format("DOME-%s-%06d", key, value);
	}

}