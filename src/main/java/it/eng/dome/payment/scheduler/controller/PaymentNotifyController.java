package it.eng.dome.payment.scheduler.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
		
		String jwtToken = authHeader.replace("Bearer ", "");
		DecodedJWT jwt = JWT.decode(jwtToken);
		//logger.info("Payload: {}" , jwt.getPayload());
		//logger.info("Header: {}" , jwt.getHeader());
		//logger.info("Claims: {}" , jwt.getClaims());
		//logger.info("Claims - sub: {}" , jwt.getClaim("sub").asString());
		
		logger.info("Claims - iss: {}" , jwt.getClaim("iss").asString());
		String iss = jwt.getClaim("iss").asString();
		if (issuer.equalsIgnoreCase(iss)) {
			logger.info("Token valid");
		}else {
			logger.warn("Token not valid");
		}
		
		return ResponseEntity.ok("Sent payment status: " + paymentStatus.getStatus() + " for paymentId: " + paymentStatus.getPaymentId());
	}
}
