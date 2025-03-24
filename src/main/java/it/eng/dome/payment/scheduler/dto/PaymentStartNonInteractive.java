package it.eng.dome.payment.scheduler.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PaymentStartNonInteractive {

	private String paymentPreAuthorizationId;
	private BaseAttributes baseAttributes; 
	
	public String getPaymentPreAuthorizationId() {
		return paymentPreAuthorizationId;
	}
	
	public void setPaymentPreAuthorizationId(String paymentPreAuthorizationId) {
		this.paymentPreAuthorizationId = paymentPreAuthorizationId;
	}
	public BaseAttributes getBaseAttributes() {
		return baseAttributes;
	}
	
	public void setBaseAttributes(BaseAttributes baseAttributes) {
		this.baseAttributes = baseAttributes;
	}
	
	public String toJson() {
        ObjectMapper objectMapper = new ObjectMapper();
		try {
			return objectMapper.writeValueAsString(this);
		} catch (JsonProcessingException e) {
			return null;
		}
	}
}
