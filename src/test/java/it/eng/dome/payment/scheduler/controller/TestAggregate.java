package it.eng.dome.payment.scheduler.controller;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.eng.dome.tmforum.tmf678.v4.model.AppliedCustomerBillingRate;
import it.eng.dome.tmforum.tmf678.v4.model.TimePeriod;

public class TestAggregate {
	
	private final static String PREFIX_KEY = "aggregate-period-";

	public static void main(String[] args) {
		TestAggregate tg = new TestAggregate();
		
		Map<String, List<AppliedCustomerBillingRate>> aggregates = tg.testAggregate();

		System.out.println("Size aggregate: " + aggregates.size());
		
		for (Map.Entry<String, List<AppliedCustomerBillingRate>> entry : aggregates.entrySet()) {
			
			String key = entry.getKey();
			System.out.println("Key: " + key);
			System.out.println("Num items: " + entry.getValue().size());
			List<AppliedCustomerBillingRate> applied = entry.getValue();
			for (AppliedCustomerBillingRate appliedCustomerBillingRate : applied) {
				System.out.println("ID: " + appliedCustomerBillingRate.getId());
			}
			System.out.println("");
		}
	}
	
	private List<AppliedCustomerBillingRate> getAppliedCustomerBillingRates() {
		
		List<AppliedCustomerBillingRate> applyList = new ArrayList<AppliedCustomerBillingRate>();
		
		// 1
		AppliedCustomerBillingRate apply = new AppliedCustomerBillingRate();
		apply.setId("app-1");
		TimePeriod tp = new TimePeriod();
		tp.setStartDateTime(OffsetDateTime.parse("2019-12-01T00:00:00.000Z"));
		tp.setEndDateTime(OffsetDateTime.parse("2019-12-31T23:59:59.999Z"));
		apply.setPeriodCoverage(tp);
		applyList.add(apply);
		
		// 2
		apply = new AppliedCustomerBillingRate();
		apply.setId("app-2");
		tp = new TimePeriod();
		tp.setStartDateTime(OffsetDateTime.parse("2019-12-11T00:00:00.000Z"));
		tp.setEndDateTime(OffsetDateTime.parse("2019-12-31T11:59:59.999Z"));
		apply.setPeriodCoverage(tp);
		applyList.add(apply);
		
		// 3
		apply = new AppliedCustomerBillingRate();
		apply.setId("app-3");
		tp = new TimePeriod();
		tp.setStartDateTime(OffsetDateTime.parse("2021-12-11T00:00:00.000Z"));
		tp.setEndDateTime(OffsetDateTime.parse("2022-12-11T10:10:40.000Z"));
		apply.setPeriodCoverage(tp);
		applyList.add(apply);
		
		// 4
		apply = new AppliedCustomerBillingRate();
		apply.setId("app-4");
		tp = new TimePeriod();
		tp.setStartDateTime(OffsetDateTime.parse("2021-12-11T00:00:00.000Z"));
		tp.setEndDateTime(OffsetDateTime.parse("2019-12-31T10:40:10.000Z"));
		apply.setPeriodCoverage(tp);
		applyList.add(apply);

		// 5
		apply = new AppliedCustomerBillingRate();
		apply.setId("app-5");
		tp = new TimePeriod();
		tp.setStartDateTime(OffsetDateTime.parse("2025-12-11T00:00:00.000Z"));
		tp.setEndDateTime(OffsetDateTime.parse("2025-12-31T10:20:40.000Z"));
		apply.setPeriodCoverage(tp);
		applyList.add(apply);
		
		return applyList;
	}
	
	private Map<String, List<AppliedCustomerBillingRate>> testAggregate() {
		
		List<AppliedCustomerBillingRate> appliedList = getAppliedCustomerBillingRates();
		
		Map<String, List<AppliedCustomerBillingRate>> aggregates = new HashMap<>();
		
		for (AppliedCustomerBillingRate appliedCustomerBillingRate : appliedList) {
			
			OffsetDateTime endDateTime = appliedCustomerBillingRate.getPeriodCoverage().getEndDateTime();
			System.out.println("ACBR: " + appliedCustomerBillingRate.getId() + " -> " + endDateTime);
			String keyPeriodEndDate = PREFIX_KEY + getEndDate(endDateTime);
						
			aggregates.computeIfAbsent(keyPeriodEndDate, k -> new ArrayList<>()).add(appliedCustomerBillingRate);
		}
		
		return aggregates;
	}
	
	
	private String getEndDate(OffsetDateTime date) {
		String onlyDate = date.toLocalDate().toString();
		return onlyDate;
	}

}
