package it.eng.dome.payment.scheduler.dto;

import java.util.List;

public class PaymentStatus {
	
	private String paymentExternalId;
	private String state;
	private List<String> paymentItemExternalIds;
		
	public String getPaymentExternalId() {
		return paymentExternalId;
	}
	public void setPaymentExternalId(String paymentExternalId) {
		this.paymentExternalId = paymentExternalId;
	}
	public String getState() {
		return state;
	}
	public void setState(String state) {
		this.state = state;
	}
	public List<String> getPaymentItemExternalIds() {
		return paymentItemExternalIds;
	}
	public void setPaymentItemExternalIds(List<String> paymentItemExternalIds) {
		this.paymentItemExternalIds = paymentItemExternalIds;
	}

}
