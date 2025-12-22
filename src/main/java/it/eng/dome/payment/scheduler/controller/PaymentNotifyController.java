package it.eng.dome.payment.scheduler.controller;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;

import it.eng.dome.payment.scheduler.dto.PaymentStatus;
import it.eng.dome.payment.scheduler.service.TMForumService;
import it.eng.dome.tmforum.tmf678.v4.ApiException;

@RestController
@RequestMapping("/payment")
public class PaymentNotifyController {
	
	@Value("${vc_verifier.issuer}")
	public String issuer;
	
	@Autowired
	private TMForumService tmforumService;

	private static final Logger logger = LoggerFactory.getLogger(PaymentNotifyController.class);

	@PostMapping("/notify")
	public ResponseEntity<Object> notifyPayment(@RequestHeader("Authorization") String authHeader, @RequestBody PaymentStatus paymentStatus) {

		String jwtToken = getBearerToken(authHeader);

		if (jwtToken != null) {
			try {
				DecodedJWT jwt = JWT.decode(jwtToken);
	
				long expired = jwt.getClaim("exp").asLong();			
				// verify if token is not expired
				if (!isTokenExpired(expired)) { 
					
					String iss = jwt.getClaim("iss").asString();
					// verify if iss is complaint
					if (issuer.equalsIgnoreCase(iss)) {
						
						Status statusEnum = Status.valueOf(paymentStatus.getState().toUpperCase());
						List<String> cbIds = paymentStatus.getPaymentItemExternalIds();
						String paymentExternalId = paymentStatus.getPaymentExternalId();
						
						logger.info("Handling {} payment status with {} CustomerBills and paymentExternalId='{}'", statusEnum.name(), cbIds.size(), paymentExternalId);
						
						try {
							handlePaymentStatus(statusEnum, cbIds, paymentExternalId);
						}catch (ApiException e){
							String msg = "Error handling PaymentStatus "+statusEnum.name()+" for CbIds "+String.join(", ", cbIds)+
									" and paymentExternalId "+paymentExternalId;
							logger.error(msg);
							return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", msg));
						}
						
					} else {
						logger.error("The token provided in the header is not valid");
						return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Token is not valid"));
					}
				} else {
					logger.error("Token is expired");
					return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Token is expired"));
				}			
			}catch(Exception e) {
				logger.error("Error: {}", e.getMessage());
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
			}
		} else {
			logger.error("Couldn't find a correct token from the header");
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Token not found in the Header"));
		}

		return ResponseEntity.noContent().build();
	}
	
	private String getBearerToken(String authHeader) {   
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).trim();
        }
        return null;
    }
	
	
	private boolean isTokenExpired(long exp) {
		
        Instant expInstant = Instant.ofEpochSecond(exp);
        Instant now = Instant.now();
        logger.debug("Token time expires: {} - Current time server: {}", expInstant, now);
        
        Duration offset = Duration.between(now, expInstant);
        logger.debug("Difference in minutes: {}", offset.toMinutes());
 		
        if (now.isAfter(expInstant)) {
        	// token expired
		    return true;
        } else {
        	// token is still valid"
        	logger.debug("Token has {} minutes remaining until it expires", offset.toMinutes());
 		    return false;
 		}
	}
	
	private enum Status {
		SUCCEEDED,
	    FAILED
	}
	
	private void handlePaymentStatus(Status status, List<String> cbIds, String paymentExternalId) throws ApiException {
	    switch (status) {
	        case SUCCEEDED:
	            handleStatusSucceeded(cbIds, paymentExternalId);
	        case FAILED:
	            handleStatusFailed(cbIds);
	    }
	    
	}
	
	private void handleStatusSucceeded(List<String> cbIds, String paymentExternalId) throws ApiException {
		logger.info("Handling Successful payment for CBs with ids {}", String.join(", ", cbIds));
		tmforumService.updatePaymentSuccessfulNotification(cbIds, paymentExternalId);
		
	}
	
	private void handleStatusFailed(List<String> cbIds) throws ApiException {
		
		logger.info("Handling Failed payment for CBs with ids {}", String.join(", ", cbIds));
		tmforumService.restoreCustomerBillsState(cbIds);
	}
}
