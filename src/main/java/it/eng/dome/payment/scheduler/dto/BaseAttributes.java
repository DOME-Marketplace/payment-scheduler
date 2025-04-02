package it.eng.dome.payment.scheduler.dto;

import java.util.List;

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

	public void addPaymentItem(PaymentItem paymentItem) {
		this.paymentItems.add(paymentItem);
	}

	public String toJson() {
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			return objectMapper.writeValueAsString(this);
		} catch (JsonProcessingException e) {
			return null;
		}
	}

	@Override
	public String toString() {
		return "BaseAttributes [externalId=" + externalId + ", customerId=" + customerId + ", customerOrganizationId="
				+ customerOrganizationId + ", invoiceId=" + invoiceId + ", paymentItems=" + paymentItems + "]";
	}
}
