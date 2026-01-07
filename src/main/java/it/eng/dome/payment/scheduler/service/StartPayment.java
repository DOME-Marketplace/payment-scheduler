package it.eng.dome.payment.scheduler.service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.eng.dome.payment.scheduler.dto.PaymentItem;
import it.eng.dome.payment.scheduler.dto.PaymentStartNonInteractive;
import it.eng.dome.payment.scheduler.model.EGPaymentResponse;
import it.eng.dome.payment.scheduler.model.EGPaymentResponse.Payout;
import it.eng.dome.payment.scheduler.model.JwtResponse;

@Component
public class StartPayment {

	private static final Logger logger = LoggerFactory.getLogger(StartPayment.class);
	
	@Autowired
	private RestClient restClient;

	@Value("${payment.payment_base_url}")
	public String paymentBaseUrl;

	@Value("${payment.payment_start_non_interactive}")
	public String paymentStartNonInteractive;


	public EGPaymentResponse paymentNonInteractive(String token, PaymentStartNonInteractive payment) {
		logger.debug("Start Non-Interactive payment");

		String url = paymentBaseUrl + paymentStartNonInteractive;
		logger.info("Payment request to URL: {}", url);

		logger.debug("Payment payload to send EG APIs: {}", payment.toJson());

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("Authorization", "Bearer " + token);
		HttpEntity<String> request = new HttpEntity<>(payment.toJson(), headers);

		try {
			JwtResponse response = restClient.post()
				    .uri(url)
				    .body(request)
				    .retrieve()
				    .body(JwtResponse.class);
			
			if (response.getResponseJwt() != null) {
				String responseJwt = response.getResponseJwt();
				logger.debug("ResponseJwt: {}", responseJwt);
				
				// decode the response
				DecodedJWT jwt = JWT.decode(responseJwt);
				logger.debug("Payload Non-Interactive: {}", jwt.getPayload());
				//logger.info("paymentExternalId: {}", jwt.getClaim("paymentExternalId").asString());
				//logger.info("paymentPreAuthorizationExternalId: {}", jwt.getClaim("paymentPreAuthorizationExternalId").asString());
				
				ObjectMapper objectMapper = new ObjectMapper();
				return objectMapper.readValue(decode(jwt.getPayload()), EGPaymentResponse.class);
	
			} else {
				logger.error("Error in ResponseJwt: {}", response.getError().toJson());
				return null;
				// TODO - remove byPassGateway and return null
				//return byPassGateway(payment.getBaseAttributes().getPaymentItems());
			}
				
		}catch(Exception e) {
			logger.error("Error: {}", e.getMessage());
			return null;
			// TODO - remove byPassGateway and return null
			//return byPassGateway(payment.getBaseAttributes().getPaymentItems());
		}
	}
	
	private String decode(String s) {
		byte[] decodedBytes = Base64.getDecoder().decode(s);
        return new String(decodedBytes, StandardCharsets.UTF_8);
	}
	
	protected EGPaymentResponse byPassGateway(List<PaymentItem> payments) {
		EGPaymentResponse eg = new EGPaymentResponse();
		eg.setPaymentExternalId(null);
		eg.setPaymentPreAuthorizationExternalId("9d4fca3b-4bfa-4dba-a09f-348b8d504e44");
		
		List<Payout> list = new ArrayList<Payout>();
		for (PaymentItem paymentItem : payments) {
			Payout payout = new Payout();
			payout.setAmount(10);
			payout.setCurrency("EUR");
			payout.setPaymentItemExternalId(paymentItem.getPaymentItemExternalId());
			payout.setState("PROCESSED");
			list.add(payout);
		}
		
		eg.setPayoutList(list);
		return eg;
	}

}
