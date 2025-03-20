package it.eng.dome.payment.scheduler.service;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.eng.dome.payment.scheduler.dto.BaseAttributes;
import it.eng.dome.payment.scheduler.dto.BaseAttributes.PaymentItem;
import it.eng.dome.payment.scheduler.dto.PaymentStartNonInteractive;
import it.eng.dome.payment.scheduler.model.EGPayment;
import it.eng.dome.payment.scheduler.model.JwtResponse;

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

	public EGPayment paymentNonInteractive(String token, PaymentStartNonInteractive payment) {
		logger.debug("Start Non-Interactive payment");

		String url = paymentBaseUrl + paymentStartNonInteractive;
		logger.info("Payment request to URL: {}", url);

		logger.debug("Payment payload to send EG APIs: {}", payment.toJson());

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("Authorization", "Bearer " + token);
		HttpEntity<String> request = new HttpEntity<>(payment.toJson(), headers);

		JwtResponse response = restTemplate.postForObject(url, request, JwtResponse.class);
		if (response.getResponseJwt() != null) {
			String responseJwt = response.getResponseJwt();
			logger.debug("ResponseJwt: {}", responseJwt);
			try {
				ObjectMapper objectMapper = new ObjectMapper();
				DecodedJWT jwt = JWT.decode(responseJwt);
				logger.debug("Payload: {}", jwt.getPayload());
				logger.info("paymentId: {}", jwt.getClaim("paymentId").asString());
				logger.info("paymentPreAuthorizationId: {}", jwt.getClaim("paymentPreAuthorizationId").asString());
				
				return objectMapper.readValue(decode(jwt.getPayload()), EGPayment.class);
			} catch (Exception e) {
				logger.error(e.getMessage());
				return null;
	        }
		}else {
			logger.error("Error: {}", response.getError().toJson());
			return null;
		}
	}


	public PaymentStartNonInteractive getPaymentStartNonInteractive(String customerId, String customerOrganizationId, String invoiceId, int productProviderId, float amount, String currency, String paymentPreAuthorizationId) {
		
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
		//TODO retrieve paymentPreAuthorizationId from product
		paymentStartNonInteractive.setPaymentPreAuthorizationId(paymentPreAuthorizationId);

		return paymentStartNonInteractive;
	}
	
	private String decode(String s) {
		byte[] decodedBytes = Base64.getDecoder().decode(s);
        return new String(decodedBytes);
	}

}
