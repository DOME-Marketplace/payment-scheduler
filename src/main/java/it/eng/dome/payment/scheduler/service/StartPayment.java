package it.eng.dome.payment.scheduler.service;

import java.util.ArrayList;
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.eng.dome.payment.scheduler.dto.BaseAttributes;
import it.eng.dome.payment.scheduler.dto.PaymentStartNonInteractive;
import it.eng.dome.payment.scheduler.model.JwtResponse;
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
			
			JwtResponse jwtResponse = objectMapper.readValue(response, JwtResponse.class);				
			if (jwtResponse.getResponseJwt() != null) {
				logger.error("ResponseJwt: {}", jwtResponse.getResponseJwt());
				return true;
			}else {
				logger.error("Error: {}", jwtResponse.getError().toJson());
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
		String randomExternalId = "479c2a6d-5197-452c-ba1b-fd1393c5" + (1000 + new Random().nextInt(9000));
		baseAttributes.setExternalId(randomExternalId);
		baseAttributes.setCustomerId("2");
		baseAttributes.setCustomerOrganizationId("677d1195762b774ef7334acc");
		baseAttributes.setInvoiceId("ab-132");

		PaymentItem paymentItem = baseAttributes.new PaymentItem();
		paymentItem.setProductProviderId("1");
		paymentItem.setAmount(10);
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
		paymentStartNonInteractive.setPaymentPreAuthorizationId("0e2948c6-26b7-48ce-91f4-59dcd8e4e97a");

		return paymentStartNonInteractive.toJson();
	}

}
