package it.eng.dome.payment.scheduler.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import it.eng.dome.payment.scheduler.tmf.TmfApiFactory;
import it.eng.dome.tmforum.tmf678.v4.api.AppliedCustomerBillingRateApi;
import it.eng.dome.tmforum.tmf678.v4.model.AppliedCustomerBillingRate;


@Component(value = "applied")
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class AppliedCustomerBillRate implements InitializingBean {
	
	private final Logger logger = LoggerFactory.getLogger(AppliedCustomerBillRate.class);
	private final int LIMIT = 10;

	@Autowired
	private TmfApiFactory tmfApiFactory;

	private AppliedCustomerBillingRateApi appliedCustomerBillingRate;
	
	@Override
	public void afterPropertiesSet() throws Exception {
		it.eng.dome.tmforum.tmf678.v4.ApiClient apiClientTMF678 = tmfApiFactory.getTMF678CustomerBillApiClient();
		appliedCustomerBillingRate = new AppliedCustomerBillingRateApi(apiClientTMF678);		
	}
	
	
	public List<AppliedCustomerBillingRate> getAllAppliedCustomerBillingRates() {
		logger.info("Get all AppliedCustomerBillingRates");
		List<AppliedCustomerBillingRate> all = new ArrayList<AppliedCustomerBillingRate>();
		getAllApplied(all, 0);
		Collections.reverse(all); //reverse order
		logger.info("Number of AppliedCustomerBillingRates: {}", all.size());
		return all;
	}
	
	private void getAllApplied(List<AppliedCustomerBillingRate> list, int start) {
		int offset = start * LIMIT;

		try {
			List<AppliedCustomerBillingRate> appliedList = appliedCustomerBillingRate.listAppliedCustomerBillingRate(null, offset, LIMIT);
			if (!appliedList.isEmpty()) {
				Collections.reverse(appliedList); //reverse order
				getAllApplied(list, start + 1);
				list.addAll(appliedList);
			}else {
				return;
			}
		} catch (Exception e) {
			return;
		}		
	}
}
