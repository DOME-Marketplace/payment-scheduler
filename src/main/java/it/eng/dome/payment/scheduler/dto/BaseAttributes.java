package it.eng.dome.payment.scheduler.dto;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class BaseAttributes {

	private String externalId;
	private String customerId;
	private String customerOrganizationId;
	private String invoiceId;
	private List<PaymentItem> paymentItems;

	public String getExternalId() {
		return externalId;
	}

	public void setExternalId(String externalId) {
		this.externalId = externalId;
	}

	public String getCustomerId() {
		return customerId;
	}

	public void setCustomerId(String customerId) {
		this.customerId = customerId;
	}

	public String getCustomerOrganizationId() {
		return customerOrganizationId;
	}

	public void setCustomerOrganizationId(String customerOrganizationId) {
		this.customerOrganizationId = customerOrganizationId;
	}

	public String getInvoiceId() {
		return invoiceId;
	}

	public void setInvoiceId(String invoiceId) {
		this.invoiceId = invoiceId;
	}

	public List<PaymentItem> getPaymentItems() {
		return paymentItems;
	}

	public void setPaymentItems(List<PaymentItem> paymentItems) {
		this.paymentItems = paymentItems;
	}
	
	public String toJson() {
        ObjectMapper objectMapper = new ObjectMapper();
		try {
			return objectMapper.writeValueAsString(this);
		} catch (JsonProcessingException e) {
			return null;
		}
	}

	public class PaymentItem {

		private int productProviderId;
		private float amount;
		private String currency;
		private boolean recurring;
		private Map<String, String> productProviderSpecificData;

		public int getProductProviderId() {
			return productProviderId;
		}

		public void setProductProviderId(int productProviderId) {
			this.productProviderId = productProviderId;
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
	}
	
}
