package it.eng.dome.payment.scheduler.dto;

import java.io.Serializable;

public class PaymentStatus implements Serializable {

	private static final long serialVersionUID = -1088443960466271769L;
	private String paymentId;
    private String status;  // "paid", "inprogress", "failed"
    
	public String getPaymentId() {
		return paymentId;
	}
	public void setPaymentId(String paymentId) {
		this.paymentId = paymentId;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
       
}
