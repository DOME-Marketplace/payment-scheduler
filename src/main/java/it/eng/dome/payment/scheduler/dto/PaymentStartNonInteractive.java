package it.eng.dome.payment.scheduler.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PaymentStartNonInteractive {

	private String paymentPreAuthorizationExternalId;
	private BaseAttributes baseAttributes; 
	

	public String getPaymentPreAuthorizationExternalId() {
		return paymentPreAuthorizationExternalId;
	}

	public void setPaymentPreAuthorizationExternalId(String paymentPreAuthorizationExternalId) {
		this.paymentPreAuthorizationExternalId = paymentPreAuthorizationExternalId;
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
