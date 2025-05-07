package it.eng.dome.payment.scheduler.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONException;

import it.eng.dome.payment.scheduler.dto.PaymentItem;
import it.eng.dome.payment.scheduler.dto.PaymentStartNonInteractive;
import it.eng.dome.payment.scheduler.util.PaymentStartNonInteractiveUtils;
import it.eng.dome.tmforum.tmf678.v4.model.AppliedCustomerBillingRate;


public class TestAggregatePaymentTime {

	static List<AppliedCustomerBillingRate> applied = new ArrayList<AppliedCustomerBillingRate>();
	
	public static void main(String[] args) throws IOException {
		
		String json = getJson();
		//System.out.println("Payload:\n" + json);

		
        try {
			JSONArray array = new JSONArray(json);
			for (int i = 0; i < array.length(); i++) {
	            AppliedCustomerBillingRate apply = AppliedCustomerBillingRate.fromJson(array.getString(i));
	            applied.add(apply);
	        }
			
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        System.out.println("Size Applied: " + applied.size());
     // filtered appliedList isBilled
		List<AppliedCustomerBillingRate> notBilled = applied.stream()
				.filter(applied -> !applied.getIsBilled())
                .collect(Collectors.toList());

		System.out.println("Size Applied is not billed: " + notBilled.size());

        int count = 0;
        
        Map<String, PaymentStartNonInteractive> payments = new HashMap<String, PaymentStartNonInteractive>();
        
        for (AppliedCustomerBillingRate appliedCustomerBillingRate : notBilled) {
        	OffsetDateTime endDateTime = appliedCustomerBillingRate.getPeriodCoverage().getEndDateTime();
        	System.out.println(++count + " " + appliedCustomerBillingRate.getId() + " - " + getEndDate(endDateTime));
        	
        	// set keys with multiple attributes
        	String endDate = getEndDate(endDateTime);
        	String paymentPreAuthorizationExternalId = "9d4fca3b-4bfa-4dba-a09f-348b8d504e44"; //appliedCustomerBillingRate.getProduct().getId();
        	String customerOrganizationId = "1"; //appliedCustomerBillingRate.getBillingAccount().getId();

        	
        	String key = paymentPreAuthorizationExternalId + "-" + customerOrganizationId + "-" + endDate;
        	System.out.println("key -> " + key);
        	
        	if (!payments.containsKey(key)) {
        		// use this customerId
        		String customerId = "1"; 
        		
        		PaymentStartNonInteractive payment = PaymentStartNonInteractiveUtils.getPaymentStartNonInteractive(paymentPreAuthorizationExternalId, customerId, customerOrganizationId);
        		System.out.println("create payload");
        		payments.put(key, payment);
            }
        	
        	PaymentItem paymentItem = new PaymentItem();
        	float amount = appliedCustomerBillingRate.getTaxExcludedAmount().getValue().floatValue();
        	paymentItem.setAmount(amount);
        	paymentItem.setCurrency("EUR");
        	paymentItem.setProductProviderExternalId("eda11ca9-cf3b-420d-8570-9d3ecf3613ac");
        	paymentItem.setRecurring(true);
        	paymentItem.setPaymentItemExternalId(appliedCustomerBillingRate.getId());
        	
        	Map<String, String> attrs = new HashMap<String, String>();
    		// attrs.put("additionalProp1", "data1");
        	paymentItem.setProductProviderSpecificData(attrs);
        	payments.get(key).getBaseAttributes().addPaymentItem(paymentItem); 

        }

        
        System.out.println(payments.size());
        for (Entry<String, PaymentStartNonInteractive> entry : payments.entrySet()) {
        	System.out.println(entry.getValue().toJson());
        }
	}
	
	
	
	private static String getEndDate(OffsetDateTime date) {
		String onlyDate = date.toLocalDate().toString();
		return onlyDate;
	}
	
	private static String getJson() {
		String file = "src/test/resources/appliedcustomerbillingrate.json";
		try {
			return new String(Files.readAllBytes(Paths.get(file)));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

}
