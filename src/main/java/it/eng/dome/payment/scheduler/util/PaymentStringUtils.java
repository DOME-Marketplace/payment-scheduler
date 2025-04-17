package it.eng.dome.payment.scheduler.util;

public class PaymentStringUtils {
	
	/**
	 * Remove urn:ngsi-ld:applied-customer-billing-rate prefix from string
	 */
	public static String removeAppliedPrefix(String s) {
		if (s != null) {
			return s.replaceFirst("^urn:ngsi-ld:applied-customer-billing-rate:?", "");
		}
		return s;
	}
	
	public static String addAppliedPrefix(String s) {
		if (s != null) {
			return "urn:ngsi-ld:applied-customer-billing-rate:" + s;
		}
		return s;
	}

	/**
	 * Remove urn:ngsi-ld:organization prefix from string
	 */
	public static String removeOrganizationPrefix(String s) {
		if (s != null) {
			return s.replaceFirst("^urn:ngsi-ld:organization:?", "");
		}
		return s;
	}
	 
}
