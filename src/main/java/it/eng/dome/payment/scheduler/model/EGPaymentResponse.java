package it.eng.dome.payment.scheduler.model;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

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
		private int gatewayExternalId;

		@JsonProperty("amount")
		private int amount;

		@JsonProperty("currency")
		private String currency;

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

		public int getGatewayExternalId() {
			return gatewayExternalId;
		}

		public void setGatewayExternalId(int gatewayExternalId) {
			this.gatewayExternalId = gatewayExternalId;
		}

		public int getAmount() {
			return amount;
		}

		public void setAmount(int amount) {
			this.amount = amount;
		}

		public String getCurrency() {
			return currency;
		}

		public void setCurrency(String currency) {
			this.currency = currency;
		}
	}
}
