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
			DecodedJWT jwt = JWT.decode(jwtToken);

			long expired = jwt.getClaim("exp").asLong();
			
			if (!isTokenExpired(expired)) { // verify if token is not expired
				
				String iss = jwt.getClaim("iss").asString();
				if (issuer.equalsIgnoreCase(iss)) {
					
					Status statusEnum = Status.valueOf(paymentStatus.getState().toUpperCase());
					List<String> appliedIds = paymentStatus.getPaymentItemExternalIds();
					
					logger.info("Handling {} payment status with {} applied", statusEnum.name(), appliedIds.size());
					handlePaymentStatus(statusEnum, appliedIds);
					
				} else {
					logger.error("The token provided in the header is not valid");
					return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("Error", "Token is not valid"));
				}
			} else {
				logger.error("Token is expired");
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("Error", "Token is expired"));
			}			
			
		} else {
			logger.error("Cannot retrieve the token from header: {}", authHeader);
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("Error", "Token not found in the Header"));
		}

		return ResponseEntity.noContent().build();
	}
	
	private String getBearerToken(String authHeader) {       
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
	
	
	private boolean isTokenExpired(long exp) {
		/*
		Instant expiration = Instant.ofEpochSecond(exp);
		Instant now = Instant.now();
		
		Instant expirationAfter = expiration.plus(Duration.ofMinutes(5));
		*/
		
		// adjustment of time
		
        Instant expInstant = Instant.ofEpochSecond(exp);
        logger.debug("Token time expires: {}", expInstant);
        Instant now = Instant.now();
        logger.debug("Current time server: {}", now);

        Duration offset = Duration.between(now, expInstant);
        logger.debug("Difference minutes: {}", offset.toMinutes());
 		
        if (now.isAfter(expInstant)) {
        	logger.info("Token expired at: {}", expInstant);
		    return true;
        } else {
        	logger.info("Token is still valid");
 		    return false;
 		}
	}
	
	private enum Status {
		SUCCEEDED,
	    FAILED
	}
	
	private void handlePaymentStatus(Status status, List<String> applied) {
	    switch (status) {
	        case SUCCEEDED:
	            handleStatusSucceeded(applied);
	            break;
	        case FAILED:
	            handleStatusFailed(applied);
	            break;
	    }
	}
	
	private void handleStatusSucceeded(List<String> applied) {
		
		for (String id : applied) {
			
			if (tmforumService.addCustomerBill(id)) { 
				// set isBilled = true and add CustomerBill (BillRef) to applied
				logger.info("The appliedCustomerBillingRateId {} has been updated successfully", id);
			} else {
				logger.error("Couldn't update appliedCustomerBillingRate {} in TMForum", id);
			}	
		}
	}
	
	private void handleStatusFailed(List<String> applied) {

		for (String id : applied) {
			
			if (tmforumService.setIsBilled(id, false)) { // set isBilled = false
				logger.info("IsBilled has been updated successfully for the appliedCustomerBillingRateId {}", id);
			} else {
				logger.error("Couldn't set isBilled for the appliedCustomerBillingRate {} in TMForum", id);
			}	
		}
	}
}
