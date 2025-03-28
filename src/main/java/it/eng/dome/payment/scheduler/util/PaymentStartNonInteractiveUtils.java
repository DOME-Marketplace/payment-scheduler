package it.eng.dome.payment.scheduler.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import it.eng.dome.payment.scheduler.dto.BaseAttributes;
import it.eng.dome.payment.scheduler.dto.PaymentStartNonInteractive;
import it.eng.dome.payment.scheduler.dto.BaseAttributes.PaymentItem;

public class PaymentStartNonInteractiveUtils {

public static PaymentStartNonInteractive getPaymentStartNonInteractive(String customerId, String customerOrganizationId, String invoiceId, int productProviderId, float amount, String currency, String paymentPreAuthorizationId) {
		
		BaseAttributes baseAttributes = new BaseAttributes();
		// TODO => how to set randomExternalId
		String randomExternalId = "479c2a6d-5197-452c-ba1b-fd1393c5" + (1000 + new Random().nextInt(9000));
		baseAttributes.setExternalId(randomExternalId);
		baseAttributes.setCustomerId(customerId);
		baseAttributes.setCustomerOrganizationId(customerOrganizationId);
		baseAttributes.setInvoiceId(invoiceId);

		PaymentItem paymentItem = baseAttributes.new PaymentItem();
		paymentItem.setProductProviderId(productProviderId);
		paymentItem.setAmount(amount);
		paymentItem.setCurrency(currency);
		
		// TODO => verify if it can be set TRUE
		paymentItem.setRecurring(true);

		Map<String, String> attrs = new HashMap<String, String>();
		// attrs.put("additionalProp1", "data1");
		paymentItem.setProductProviderSpecificData(attrs);

		List<PaymentItem> paymentItems = new ArrayList<PaymentItem>();
		paymentItems.add(paymentItem);

		baseAttributes.setPaymentItems(paymentItems);

		PaymentStartNonInteractive paymentStartNonInteractive = new PaymentStartNonInteractive();
		paymentStartNonInteractive.setBaseAttributes(baseAttributes);
		paymentStartNonInteractive.setPaymentPreAuthorizationId(paymentPreAuthorizationId);

		return paymentStartNonInteractive;
	}
}
