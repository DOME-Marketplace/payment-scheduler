package it.eng.dome.payment.scheduler.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import it.eng.dome.payment.scheduler.model.TokenResponse;

@Component
public class VCVerifier {

	private static final Logger logger = LoggerFactory.getLogger(StartPayment.class);
	private final RestTemplate restTemplate;

	@Value("${vc_verifier.endpoint}")
	public String endpoint;

	@Value("${vc_verifier.client_id}")
	public String client_id;
	
	@Value("${vc_verifier.client_assertion_type}")
	public String client_assertion_type;

	@Value("${vc_verifier.client_assertion}")
	public String client_assertion;

	public VCVerifier(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}

	public String getVCVerifierToken() {
		logger.info("Get token from VC Verifier at URL: {}", endpoint);

		// prepare the header
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		// prepare the body in x-www-form-urlencoded format
		MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");
        body.add("client_id", client_id);
        body.add("client_assertion_type", client_assertion_type);
        body.add("client_assertion", client_assertion);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
		ResponseEntity<TokenResponse> response = restTemplate.exchange(endpoint, HttpMethod.POST, request, TokenResponse.class);
		
		if (response.getBody() != null) {
			String accessToken = response.getBody().getAccess_token();
			if (accessToken != null) {
				logger.info("Access Token retrieved: {}", accessToken);
				return accessToken;
			} else {
				logger.error("Access Token = null for the request {}", endpoint);
				return null;
			}
		} else {
			logger.error("Body null for the POST request to {}", endpoint);
			return null;
		}
	}
}
