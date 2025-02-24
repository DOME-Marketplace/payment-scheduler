package it.eng.dome.payment.scheduler.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import it.eng.dome.payment.scheduler.dto.BaseAttributes;
import it.eng.dome.payment.scheduler.dto.PaymentStartNonInteractive;
import it.eng.dome.payment.scheduler.exception.ControllerExceptionHandler;
import it.eng.dome.payment.scheduler.dto.BaseAttributes.PaymentItem;

@Component
public class StartPayment {

	private static final Logger logger = LoggerFactory.getLogger(StartPayment.class);
	private final RestTemplate restTemplate;
		
    @Value("${payment.payment_base_url}")
    public String paymentBaseUrl;
    
    @Value("${payment.payment_start_non_interactive}")
    public String paymentStartNonInteractive;    
    
	
	public StartPayment(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}
	
	public boolean paymentNonInteractive() {
		logger.debug("Start Non-Interactive payment");
		
		//TODO prepare the payload for the payment
		String payment = getPaymentStartNonInteractive();
		
		String url = paymentBaseUrl + paymentStartNonInteractive;
		logger.info("Payment request to URL: {}", url);
		
		logger.debug("Payment payload to send EG APIs: {}", payment);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> request = new HttpEntity<>(payment, headers);

		String response = restTemplate.postForObject(url, request, String.class);
		logger.info("Response: {} ", response);

		
		return false;
	}

	public void payment() {
		logger.debug("Start payment");
		
		logger.info(restTemplate.toString());
	}
	
	
	private String getPaymentStartNonInteractive() {
		//TODO payload dummy
		BaseAttributes baseAttributes = new BaseAttributes();
		baseAttributes.setExternalId("id-ext-1");
		baseAttributes.setCustomerId("id-customer-1");
		baseAttributes.setCustomerOrganizationId("urn:ngsi-ld:organization:f2ad85a5-9edf-497c-b343-f08899084ebb");
		baseAttributes.setType("ONETIME");
		baseAttributes.setInvoiceId("id-invoicing-1");

		
		PaymentItem paymentItem = baseAttributes.new PaymentItem();
		paymentItem.setProductProviderId("product-id-1");
		paymentItem.setAmount(1);
		paymentItem.setCurrency("EUR");
		paymentItem.setRecurring("recurring");
		paymentItem.setProductProviderSpecificData("{}");
		
		List<PaymentItem> paymentItems = new ArrayList<PaymentItem>();
		paymentItems.add(paymentItem);
		
		baseAttributes.setPaymentItems(paymentItems);
		
		PaymentStartNonInteractive paymentStartNonInteractive = new PaymentStartNonInteractive();
		paymentStartNonInteractive.setBaseAttributes(baseAttributes);
		paymentStartNonInteractive.setPaymentPreAuthorizationId("1bf099fd-7d57-426a-b420-6ced7d2404e8");
/*
    "externalId": str(self._order.order_id),
    "customerId": self._order.customer_id, 
    "customerOrganizationId": str(self._order.owner_organization_id),
    "type": "ONETIME", # recurring payment type is not yet implemented in the Dpas API
    "invoiceId": "invoice id", # should be the order's invoice ID
    "paymentItems": [{
        "productProviderId": "1", # should be the product provider's ID
        "amount": str(total),
        "currency": current_curr,
        "recurring" : False,
        "productProviderSpecificData": {}
    }],
    "processSuccessUrl": return_url,
    "processErrorUrl": cancel_url		
 */
		return paymentStartNonInteractive.toJson();
	}
	
/**
 
{
   "paymentId":"0fa4c855-5793-47bf-92fd-2cd1cc6cb6ae",
   "paymentPreAuthorizationId":"1bf099fd-7d57-426a-b420-6ced7d2404e8",
   "payoutList":[
      {
         "state":"PAID_BY_CUSTOMER",
         "productProviderId":1,
         "gatewayId":1,
         "amount":22.0000000000000000,
         "currency":"EUR"
      }
   ]
}	 
	  
 */
}
