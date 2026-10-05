package it.eng.dome.payment.scheduler.service;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import it.eng.dome.payment.scheduler.loader.LearCredentialMachineLoader;
import it.eng.dome.payment.scheduler.model.TokenResponse;
import it.eng.dome.payment.scheduler.util.M2MTokenUtils;

@Component
public class VCVerifier {

	private static final Logger logger = LoggerFactory.getLogger(VCVerifier.class);
	private LearCredentialMachineLoader learCredentialMachine;
	
	private RestClient restClient;

	@Value("${vc_verifier.endpoint}")
	public String endpoint;
	
	@Autowired
	private M2MTokenService m2mTokenService;
	
	public VCVerifier(RestClient restClient, LearCredentialMachineLoader learCredentialMachine) {
		this.restClient = restClient;
		this.learCredentialMachine = learCredentialMachine;
	}
	
	public String getVCVerifierToken() {
		logger.info("Getting the token from VC Verifier at the URL: {}", endpoint);
		
		String learCredential = learCredentialMachine.getLearCredentialMachine();
		//logger.debug("LEAR Credential: {}", learCredential);
		
		Map<String, String> map = m2mTokenService.getAssertion(learCredential);
		
		if (map == null || map.isEmpty()) {
			throw new IllegalStateException("Unable to generate assertion from LEAR Credential");
		}
			
		String client_assertion = map.get(M2MTokenUtils.CLIENT_ASSERTION);
		String client_assertion_type = M2MTokenUtils.CLIENT_ASSERTION_TYPE;
		String client_id = map.get(M2MTokenUtils.CLIENT_ID);
		
		//logger.debug("client_id: {}", client_id);
		//logger.debug("client_assertion: {}", client_assertion);
		
		// prepare the header
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		// prepare the body in x-www-form-urlencoded format
		MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");
        body.add("client_id", client_id);
        body.add("client_assertion_type", client_assertion_type);
        body.add("client_assertion", client_assertion);
        try {
	        ResponseEntity<TokenResponse> response = restClient.post()
			        .uri(endpoint)
			        .accept(MediaType.APPLICATION_JSON)
			        .headers(h -> h.addAll(headers))
			        .body(body)
			        .retrieve()
			        .toEntity(TokenResponse.class);
	        
	        if (response.getBody() == null) {
	        	throw new IllegalStateException("Response Body cannot be null making a POST call to " + endpoint);
	        }
	        
	        String accessToken = response.getBody().getAccess_token();
	        
	        if (accessToken == null || accessToken.isBlank()) {
	        	throw new IllegalStateException("The accessToken not present in VC Verifier response");
	        }
	        	
        	logger.info("Access Token retrieved successfully");
        	return accessToken;

        } catch (HttpClientErrorException e) {
        	logger.error("Cannot perform the request to the Verifier. Status={}", e.getStatusCode(), e);
        	throw e;
//       	} catch (Exception ex) {
//       		logger.error("Unexpected error while retrieving token for client_id={}", client_id,	ex);
//       		throw new IllegalStateException("Error retrieving token from VC Verifier", ex);
       	}
	}
}
