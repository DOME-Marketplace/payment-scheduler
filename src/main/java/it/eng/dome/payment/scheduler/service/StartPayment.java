package it.eng.dome.payment.scheduler.service;

import java.util.Base64;

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
	
	private String decode(String s) {
		byte[] decodedBytes = Base64.getDecoder().decode(s);
        return new String(decodedBytes);
	}

}
