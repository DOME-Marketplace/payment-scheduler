package it.eng.dome.payment.scheduler.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;

import it.eng.dome.tmforum.tmf678.v4.model.AppliedCustomerBillingRate;


public class TestApplied {

	private static final int LIMIT = 4;
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
			e.printStackTrace();
		}

        System.out.println(applied.size());
//        for (AppliedCustomerBillingRate appliedCustomerBillingRate : applied) {
//			System.out.println(appliedCustomerBillingRate.getDescription());
//		}
       
        List<AppliedCustomerBillingRate> subList = getPaginatedList(applied, 4, 2);
        System.out.println(">> " + subList.size());
        
        for (AppliedCustomerBillingRate appliedCustomerBillingRate : subList) {
			System.out.println(appliedCustomerBillingRate.getDescription());
		}
        System.out.println();
        TestApplied ta = new TestApplied();
        List<AppliedCustomerBillingRate> all = ta.getAllAppliedCustomerBillingRate();
        
        System.out.println("all items: " + all.size());
        for (AppliedCustomerBillingRate appliedCustomerBillingRate : all) {
			System.out.println(appliedCustomerBillingRate.getDescription());
		}
	}
	
	private static String getJson() {
		String file = "src/test/resources/appliedcustomerbillingrate.json";
		try {
			return new String(Files.readAllBytes(Paths.get(file)));
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	private static <T> List<AppliedCustomerBillingRate> getPaginatedList(List<AppliedCustomerBillingRate> lista, int offset, int limit) {
        if (offset < 0 || limit <= 0) {
            throw new IllegalArgumentException("Offset deve essere >= 0 e limit > 0");
        }

        if (offset >= lista.size()) {
            return new ArrayList<>(); // Se l'offset è fuori dai limiti, restituisce una lista vuota
        }

        int end = Math.min(offset + limit, lista.size()); // Evita IndexOutOfBoundsException
        return lista.subList(offset, end);
    }
	
	private List<AppliedCustomerBillingRate> getAllAppliedCustomerBillingRate() {
		List<AppliedCustomerBillingRate> all = new ArrayList<AppliedCustomerBillingRate>();
		getAllApplied(all, 0);
		//Collections.reverse(all);
		return all;
	}
	
	private void getAllApplied(List<AppliedCustomerBillingRate> list, int start) {
		int offset = start * LIMIT;

		try {
			List<AppliedCustomerBillingRate> appliedList = getPaginatedList(applied, offset, LIMIT); //appliedCustomerBillingRate.listAppliedCustomerBillingRate(null, offset, limit);
			if (!appliedList.isEmpty()) {
				//order invers
				//Collections.reverse(appliedList);
				list.addAll(appliedList);
				getAllApplied(list, start + 1);
				//list.addAll(appliedList);
			}else {
				return;
			}
		} catch (Exception e) {
			return;
		}		
	}
}
