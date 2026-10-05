package it.eng.dome.payment.scheduler.exception;

public class PaymentException extends RuntimeException {


	private static final long serialVersionUID = 396655978234692296L;

	public PaymentException(String message, Throwable cause) {
		super(message, cause);
	}
}
