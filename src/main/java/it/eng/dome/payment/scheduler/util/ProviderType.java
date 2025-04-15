package it.eng.dome.payment.scheduler.util;

public enum ProviderType {
	PROVIDER, 
	SELLER;

	public static boolean isValid(String value) {
		for (ProviderType type : ProviderType.values()) {
			if (type.name().equalsIgnoreCase(value)) {
				return true;
			}
		}
		return false;
	}
}
