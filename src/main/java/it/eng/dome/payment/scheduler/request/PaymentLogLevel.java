package it.eng.dome.payment.scheduler.request;

import io.swagger.v3.oas.annotations.media.Schema;

public class PaymentLogLevel {

	@Schema(description = "Set the log level (DEBUG, INFO, WARN, ERROR, ect.)", example = "DEBUG", defaultValue = "DEBUG")
	private String level;

	public String getLevel() {
		return level;
	}

	public void setLevel(String level) {
		this.level = level;
	}
}
