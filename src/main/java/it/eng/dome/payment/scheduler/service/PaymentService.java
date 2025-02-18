package it.eng.dome.payment.scheduler.service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import it.eng.dome.payment.scheduler.tmf.TmfApiFactory;
import it.eng.dome.tmforum.tmf678.v4.ApiException;
import it.eng.dome.tmforum.tmf678.v4.api.AppliedCustomerBillingRateApi;
import it.eng.dome.tmforum.tmf678.v4.model.AppliedCustomerBillingRate;
import it.eng.dome.tmforum.tmf678.v4.model.AppliedCustomerBillingRateCreate;

@Component(value = "paymentService")
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class PaymentService implements InitializingBean {

	private final Logger logger = LoggerFactory.getLogger(PaymentService.class);
	private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy");

	@Autowired
	private TmfApiFactory tmfApiFactory;

	private AppliedCustomerBillingRateApi appliedCustomerBillingRate;

	@Override
	public void afterPropertiesSet() throws Exception {
		appliedCustomerBillingRate = new AppliedCustomerBillingRateApi(tmfApiFactory.getTMF678CustomerBillApiClient());
	}

	public List<String> saveBill(AppliedCustomerBillingRate[] bills) {
		logger.info("Saving the bill ...");
		List<String> ids = new ArrayList<String>();

		try {
			for (AppliedCustomerBillingRate bill : bills) {

				AppliedCustomerBillingRateCreate createApply = AppliedCustomerBillingRateCreate.fromJson(bill.toJson());
				AppliedCustomerBillingRate created = appliedCustomerBillingRate.createAppliedCustomerBillingRate(createApply);
				logger.info("{}AppliedCustomerBillRate saved with id: {}", created.getId());
				ids.add(created.getId());
			}
		} catch (Exception e) {
			logger.info("AppliedCustomerBillingRate not saved!");
			logger.error("Error: {}", e.getMessage());
		}

		return ids;
	}
	
	public void payments(OffsetDateTime now) {
		logger.info("Starting payments at {}", now.format(formatter));
		
		//TODO GET AppliedCustomerBillingRate
		try {
			List<AppliedCustomerBillingRate> appliedList = appliedCustomerBillingRate.listAppliedCustomerBillingRate(null, null, null);
			logger.debug("Number of AppliedCustomerBillingRate found: {}", appliedList.size());
			for (AppliedCustomerBillingRate appliedCustomerBillingRate : appliedList) {
				logger.debug("appliedCustomerBillingRateId: {}", appliedCustomerBillingRate.getId());
			}
		} catch (ApiException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
