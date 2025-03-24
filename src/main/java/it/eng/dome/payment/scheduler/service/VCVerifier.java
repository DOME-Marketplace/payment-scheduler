package it.eng.dome.payment.scheduler.service;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

import it.eng.dome.payment.scheduler.config.LearCredentialMachineLoader;
import it.eng.dome.payment.scheduler.model.TokenResponse;
import it.eng.dome.payment.scheduler.util.M2MTokenUtils;

@Component
public class VCVerifier {

	private static final Logger logger = LoggerFactory.getLogger(StartPayment.class);
	private final RestTemplate restTemplate;
	private LearCredentialMachineLoader learCredentialMachine;

	@Value("${vc_verifier.endpoint}")
	public String endpoint;
	
	@Autowired
	private M2MTokenService m2mTokenService;

	
	public VCVerifier(RestTemplate restTemplate, LearCredentialMachineLoader learCredentialMachine) {
		this.restTemplate = restTemplate;
		this.learCredentialMachine = learCredentialMachine;
	}
	
	public String getVCVerifierToken() {
		logger.info("Get token from VC Verifier at URL: {}", endpoint);
		
		String learCredential = learCredentialMachine.getLearCredentialMachine();
		Map<String, String> map = m2mTokenService.getAssertion(learCredential);
				
		if (map != null && !map.isEmpty()) {
			
			String client_assertion = map.get(M2MTokenUtils.CLIENT_ASSERTION);
			String client_assertion_type = M2MTokenUtils.CLIENT_ASSERTION_TYPE;
			String client_id = map.get(M2MTokenUtils.CLIENT_ID);
			
			logger.debug("client_id: {}", client_id);
			logger.debug("client_assertion: {}", client_assertion);
			
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
					logger.error("Cannot found the access_token attribute from the response {}", response.getBody());
					return "TOKEN_NULL";
				}
			} else {
				logger.error("Response Body cannot be null making a POST call to {}", endpoint);
				return "BODY_NULL";
			}
		}
		return "NULL";
	}
}
