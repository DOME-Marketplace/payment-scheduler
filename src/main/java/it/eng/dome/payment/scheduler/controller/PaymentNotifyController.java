package it.eng.dome.payment.scheduler.controller;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
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
						List<String> appliedIds = paymentStatus.getPaymentItemExternalIds();
						String paymentExternalId = paymentStatus.getPaymentExternalId();
						
						logger.info("Handling {} payment status with {} applied and paymentExternalId='{}'", statusEnum.name(), appliedIds.size(), paymentExternalId);
						List<String> appliedNotUpdated = handlePaymentStatus(statusEnum, appliedIds, paymentExternalId);
	
						if (!appliedNotUpdated.isEmpty()) {
							String msg = "The following " + appliedIds.size() + " applied cannot be updated: " + String.join(", ", appliedNotUpdated);
							logger.error(msg);
							return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(Map.of("error", msg));
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
	
	private List<String> handlePaymentStatus(Status status, List<String> applied, String paymentExternalId) {
	    switch (status) {
	        case SUCCEEDED:
	            return handleStatusSucceeded(applied, paymentExternalId);
	        case FAILED:
	            return handleStatusFailed(applied);
	    }
	    
		return new ArrayList<String>();
	}
	
	private List<String> handleStatusSucceeded(List<String> applied, String paymentExternalId) {
		
		// return the appliedIds that cannot be found (not updated)
		List<String> appliedIdsNotSucceeded = new ArrayList<String>();
		
		for (String id : applied) {
			
			if (tmforumService.addCustomerBill(id,paymentExternalId)) { 
				// set isBilled = true and add CustomerBill (BillRef) to applied
				logger.info("The appliedCustomerBillingRateId {} has been updated successfully", id);
			} else {
				// add appliedId in the list
				appliedIdsNotSucceeded.add(id);
				logger.error("Couldn't update appliedCustomerBillingRate {} in TMForum", id);
			}	
		}
		
		return appliedIdsNotSucceeded;
	}
	
	private List<String> handleStatusFailed(List<String> applied) {
		
		// return the appliedIds that cannot be found (not updated)
		List<String> appliedIdsNotBilled = new ArrayList<String>();

		for (String id : applied) {
			
			if (tmforumService.setIsBilled(id, false)) { // set isBilled = false
				logger.info("IsBilled has been updated successfully for the appliedCustomerBillingRateId {}", id);
			} else {
				// add appliedId in the list
				appliedIdsNotBilled.add(id);
				logger.error("Couldn't set isBilled for the appliedCustomerBillingRate {} in TMForum", id);
			}	
		}
		
		return appliedIdsNotBilled;
	}
}
