package it.eng.dome.payment.scheduler.dto;

import java.util.Map;

public class PaymentItem {

	private String productProviderExternalId;
	private float amount;
	private String currency;
	private boolean recurring;
	private Map<String, String> productProviderSpecificData;

	public String getProductProviderExternalId() {
		return productProviderExternalId;
	}

	public void setProductProviderExternalId(String productProviderExternalId) {
		this.productProviderExternalId = productProviderExternalId;
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

	public boolean getRecurring() {
		return recurring;
	}

	public void setRecurring(boolean recurring) {
		this.recurring = recurring;
	}

	public Map<String, String> getProductProviderSpecificData() {
		return productProviderSpecificData;
	}

	public void setProductProviderSpecificData(Map<String, String> productProviderSpecificData) {
		this.productProviderSpecificData = productProviderSpecificData;
	}

	@Override
	public String toString() {
		return "PaymentItem [productProviderExternalId=" + productProviderExternalId + ", amount=" + amount
				+ ", currency=" + currency + ", recurring=" + recurring + ", productProviderSpecificData="
				+ productProviderSpecificData + "]";
	}
}