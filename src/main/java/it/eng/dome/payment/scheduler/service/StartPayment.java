package it.eng.dome.payment.scheduler.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.eng.dome.payment.scheduler.dto.BaseAttributes;
import it.eng.dome.payment.scheduler.dto.PaymentStartNonInteractive;
import it.eng.dome.payment.scheduler.model.JwtResponse;
import it.eng.dome.payment.scheduler.model.PaymentResponse;
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

		// TODO prepare the payload for the payment
		String payment = getPaymentStartNonInteractive();

		String url = paymentBaseUrl + paymentStartNonInteractive;
		logger.info("Payment request to URL: {}", url);

		logger.debug("Payment payload to send EG APIs: {}", payment);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> request = new HttpEntity<>(payment, headers);

		String response = restTemplate.postForObject(url, request, String.class);
		

		ObjectMapper objectMapper = new ObjectMapper();
		try {
			if (response.contains("responseJwt")) {				
				JwtResponse jwtResponse = objectMapper.readValue(response, JwtResponse.class);
				logger.error("ResponseJwt: {}", jwtResponse.getResponseJwt());
				logger.error("Error: {}", jwtResponse.getError().toJson());
				return false;
			} else if (response.contains("paymentId")) {
				PaymentResponse paymentResponse = objectMapper.readValue(response, PaymentResponse.class);
				logger.debug("PaymentId: {}", paymentResponse.getPaymentId());
				logger.debug("PaymentPreAuthorizationId: {}", paymentResponse.getPaymentPreAuthorizationId());				
				return true;
			} else {
				logger.error("Response: {} ", response);
				return false;
			}
		} catch (JsonMappingException e) {
			logger.error("Error: {} ", e.getMessage());
			return false;
		} catch (JsonProcessingException e) {
			logger.error("Error: {} ", e.getMessage());
			return false;
		}
	}

	public void payment() {
		logger.debug("Start payment");

		logger.info(restTemplate.toString());
	}

	private String getPaymentStartNonInteractive() {
		// TODO payload dummy
		BaseAttributes baseAttributes = new BaseAttributes();
		baseAttributes.setExternalId("urn:ngsi-ld:product-order:50df4527-ac1d-4b6b-a73d-4760dd533b67");
		baseAttributes.setCustomerId("83916709-c00e-4d2a-8379-32ce4ec5ebfe");
		baseAttributes.setCustomerOrganizationId("urn:ngsi-ld:organization:f2ad85a5-9edf-497c-b343-f08899084ebb");
		baseAttributes.setInvoiceId("ab-132");

		PaymentItem paymentItem = baseAttributes.new PaymentItem();
		paymentItem.setProductProviderId("1");
		paymentItem.setAmount(1);
		paymentItem.setCurrency("EUR");
		paymentItem.setRecurring(true);

		Map<String, String> attrs = new HashMap<String, String>();
		// attrs.put("additionalProp1", "data1");
		paymentItem.setProductProviderSpecificData(attrs);

		List<PaymentItem> paymentItems = new ArrayList<PaymentItem>();
		paymentItems.add(paymentItem);

		baseAttributes.setPaymentItems(paymentItems);

		PaymentStartNonInteractive paymentStartNonInteractive = new PaymentStartNonInteractive();
		paymentStartNonInteractive.setBaseAttributes(baseAttributes);
		paymentStartNonInteractive.setPaymentPreAuthorizationId("8a98d502-fc0b-472f-a69a-670b4f550b50");

		return paymentStartNonInteractive.toJson();
	}

}
