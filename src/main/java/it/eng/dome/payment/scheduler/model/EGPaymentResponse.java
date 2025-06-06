package it.eng.dome.payment.scheduler.model;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class EGPaymentResponse {

	@JsonProperty("paymentExternalId")
	private String paymentExternalId;

	@JsonProperty("paymentPreAuthorizationExternalId")
	private String paymentPreAuthorizationExternalId;

	@JsonProperty("payoutList")
	private List<Payout> payoutList;


	public String getPaymentExternalId() {
		return paymentExternalId;
	}

	public void setPaymentExternalId(String paymentExternalId) {
		this.paymentExternalId = paymentExternalId;
	}

	public String getPaymentPreAuthorizationExternalId() {
		return paymentPreAuthorizationExternalId;
	}

	public void setPaymentPreAuthorizationExternalId(String paymentPreAuthorizationExternalId) {
		this.paymentPreAuthorizationExternalId = paymentPreAuthorizationExternalId;
	}

	public List<Payout> getPayoutList() {
		return payoutList;
	}

	public void setPayoutList(List<Payout> payoutList) {
		this.payoutList = payoutList;
	}

	public static class Payout {

		@JsonProperty("state")
		private String state;
		
		@JsonProperty("productProviderExternalId")
		private String productProviderExternalId;

		@JsonProperty("gatewayExternalId")
		private String gatewayExternalId;

		@JsonProperty("amount")
		private float amount;

		@JsonProperty("currency")
		private String currency;
		
		@JsonProperty("paymentMethodType")
		private String paymentMethodType;
				
		@JsonProperty("paymentItemExternalId")
		private String paymentItemExternalId;

		public String getState() {
			return state;
		}

		public void setState(String state) {
			this.state = state;
		}

		public String getProductProviderExternalId() {
			return productProviderExternalId;
		}

		public void setProductProviderExternalId(String productProviderExternalId) {
			this.productProviderExternalId = productProviderExternalId;
		}

		public String getGatewayExternalId() {
			return gatewayExternalId;
		}

		public void setGatewayExternalId(String gatewayExternalId) {
			this.gatewayExternalId = gatewayExternalId;
		}

		public float getAmount() {
			return amount;
		}

		public void setAmount(float amount) {
			this.amount = amount;
		}

		public String getCurrency() {
			return currency;
		}

		public void setCurrency(String currency) {
			this.currency = currency;
		}

		public String getPaymentMethodType() {
			return paymentMethodType;
		}

		public void setPaymentMethodType(String paymentMethodType) {
			this.paymentMethodType = paymentMethodType;
		}

		public String getPaymentItemExternalId() {
			return paymentItemExternalId;
		}

		public void setPaymentItemExternalId(String paymentItemExternalId) {
			this.paymentItemExternalId = paymentItemExternalId;
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
}
