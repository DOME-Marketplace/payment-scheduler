package it.eng.dome.payment.scheduler.model;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

public class EGPayment {

	@JsonProperty("paymentId")
	private String paymentId;

	@JsonProperty("paymentPreAuthorizationId")
	private String paymentPreAuthorizationId;

	@JsonProperty("payoutList")
	private List<Payout> payoutList;

	public String getPaymentId() {
		return paymentId;
	}

	public void setPaymentId(String paymentId) {
		this.paymentId = paymentId;
	}

	public String getPaymentPreAuthorizationId() {
		return paymentPreAuthorizationId;
	}

	public void setPaymentPreAuthorizationId(String paymentPreAuthorizationId) {
		this.paymentPreAuthorizationId = paymentPreAuthorizationId;
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

		@JsonProperty("productProviderId")
		private int productProviderId;

		@JsonProperty("gatewayId")
		private int gatewayId;

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

		public int getProductProviderId() {
			return productProviderId;
		}

		public void setProductProviderId(int productProviderId) {
			this.productProviderId = productProviderId;
		}

		public int getGatewayId() {
			return gatewayId;
		}

		public void setGatewayId(int gatewayId) {
			this.gatewayId = gatewayId;
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
