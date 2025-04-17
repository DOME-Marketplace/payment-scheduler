package it.eng.dome.payment.scheduler.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

@RestController
@RequestMapping("/payment")
public class PaymentNotifyController {
	
	@Value("${vc_verifier.issuer}")
	public String issuer;

	private static final Logger logger = LoggerFactory.getLogger(PaymentNotifyController.class);

	@PostMapping("/notify")
	public ResponseEntity<String> notifyPayment(@RequestHeader("Authorization") String authHeader, @RequestBody PaymentStatus paymentStatus) {

		logger.info("PaymentId: {}", paymentStatus.getPaymentId());
		logger.info("Status received: {}", paymentStatus.getStatus());
		
		String msg = "request accepted";
		
		String jwtToken = getBearerToken(authHeader);
		if (jwtToken != null) {
			DecodedJWT jwt = JWT.decode(jwtToken);
		
			logger.info("Claims - iss: {}" , jwt.getClaim("iss").asString());
			String iss = jwt.getClaim("iss").asString();
			if (issuer.equalsIgnoreCase(iss)) {
				logger.info("Token valid");
				//msg = "token valid";
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
}
