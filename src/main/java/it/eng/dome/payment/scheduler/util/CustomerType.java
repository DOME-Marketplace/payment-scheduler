package it.eng.dome.payment.scheduler.util;

public enum CustomerType {
	BUYER, 
	CUSTOMER;

	public static boolean isValid(String value) {
		for (CustomerType type : CustomerType.values()) {
			if (type.name().equalsIgnoreCase(value)) {
				return true;
			}
		}
		return false;
	}
}
