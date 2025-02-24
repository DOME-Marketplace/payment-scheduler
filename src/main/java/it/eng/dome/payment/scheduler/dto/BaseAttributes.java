package it.eng.dome.payment.scheduler.dto;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class BaseAttributes {

	private String externalId;
	private String customerId;
	private String customerOrganizationId;
	private String type;
	private String invoiceId;
	private List<PaymentItem> paymentItems;
//	private String processSuccessUrl;
//	private String processErrorUrl;

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

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
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
/*
	public String getProcessSuccessUrl() {
		return processSuccessUrl;
	}

	public void setProcessSuccessUrl(String processSuccessUrl) {
		this.processSuccessUrl = processSuccessUrl;
	}

	public String getProcessErrorUrl() {
		return processErrorUrl;
	}

	public void setProcessErrorUrl(String processErrorUrl) {
		this.processErrorUrl = processErrorUrl;
	}
*/	
	public String toJson() {
        ObjectMapper objectMapper = new ObjectMapper();
		try {
			return objectMapper.writeValueAsString(this);
		} catch (JsonProcessingException e) {
			return null;
		}
	}

	public class PaymentItem {

		private String productProviderId;
		private float amount;
		private String currency;
		private String recurring;
		private String productProviderSpecificData;

		public String getProductProviderId() {
			return productProviderId;
		}

		public void setProductProviderId(String productProviderId) {
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

		public String getRecurring() {
			return recurring;
		}

		public void setRecurring(String recurring) {
			this.recurring = recurring;
		}

		public String getProductProviderSpecificData() {
			return productProviderSpecificData;
		}

		public void setProductProviderSpecificData(String productProviderSpecificData) {
			this.productProviderSpecificData = productProviderSpecificData;
		}
	}
}
