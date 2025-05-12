package it.eng.dome.payment.scheduler.controller;

import java.util.List;

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
	public ResponseEntity<String> notifyPayment(@RequestHeader("Authorization") String authHeader, @RequestBody PaymentStatus paymentStatus) {
		
		String msg = "request accepted";
		
		String jwtToken = getBearerToken(authHeader);
		if (jwtToken != null) {
			DecodedJWT jwt = JWT.decode(jwtToken);
		
			logger.info("Claims - iss: {}" , jwt.getClaim("iss").asString());
			String iss = jwt.getClaim("iss").asString();
			if (issuer.equalsIgnoreCase(iss)) {
				logger.info("Token valid");
				//msg = "token valid";
				logger.info("State: {}", paymentStatus.getState());
				logger.info("paymentItemExternalIds received: {}", paymentStatus.getPaymentItemExternalIds());
				
				Status statusEnum = Status.valueOf(paymentStatus.getState().toUpperCase());
				List<String> appliedIds = paymentStatus.getPaymentItemExternalIds();
				
				logger.info("Handling {} payment status: {}", statusEnum.name(), appliedIds.size());
				handlePaymentStatus(statusEnum, appliedIds);
				
			}else {
				logger.warn("Token not valid");
				msg = "token not valid";
			}
		}else {
			logger.debug("Cannot retrieve the token from header: {}", authHeader);
			msg = "No header to get the token";
		}

		return new ResponseEntity<String>(msg, HttpStatus.OK);
	}
	
	private String getBearerToken(String authHeader) {       
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
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
			
			if (tmforumService.addCustomerBill(id)) { // set isBilled = true and add CustomerBill (BillRef)
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
