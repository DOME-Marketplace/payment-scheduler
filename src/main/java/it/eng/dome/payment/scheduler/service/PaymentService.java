package it.eng.dome.payment.scheduler.service;

import java.time.OffsetDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import it.eng.dome.payment.scheduler.model.EGPayment;
import it.eng.dome.payment.scheduler.tmf.TmfApiFactory;
import it.eng.dome.payment.scheduler.util.PaymentDateUtils;
import it.eng.dome.tmforum.tmf678.v4.ApiClient;
import it.eng.dome.tmforum.tmf678.v4.ApiException;
import it.eng.dome.tmforum.tmf678.v4.api.AppliedCustomerBillingRateApi;
import it.eng.dome.tmforum.tmf678.v4.api.CustomerBillExtensionApi;
import it.eng.dome.tmforum.tmf678.v4.model.AppliedCustomerBillingRate;
import it.eng.dome.tmforum.tmf678.v4.model.AppliedCustomerBillingRateUpdate;
import it.eng.dome.tmforum.tmf678.v4.model.BillRef;
import it.eng.dome.tmforum.tmf678.v4.model.CustomerBill;
import it.eng.dome.tmforum.tmf678.v4.model.CustomerBillCreate;

@Component(value = "paymentService")
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class PaymentService implements InitializingBean {

	private final Logger logger = LoggerFactory.getLogger(PaymentService.class);
	
	@Autowired
	private TmfApiFactory tmfApiFactory;

	private AppliedCustomerBillingRateApi appliedCustomerBillingRate;
	private CustomerBillExtensionApi customerBillExtension;
	
	@Autowired
	private StartPayment payment;
	
	@Autowired
	private VCVerifier vcverifier;

	@Override
	public void afterPropertiesSet() throws Exception {
		ApiClient apiClientTMF678 = tmfApiFactory.getTMF678CustomerBillApiClient();
		appliedCustomerBillingRate = new AppliedCustomerBillingRateApi(apiClientTMF678);
		customerBillExtension = new CustomerBillExtensionApi(apiClientTMF678);
	}
	
	
	public void payments() throws ApiException {
		logger.info("Starting payments at {}", OffsetDateTime.now().format(PaymentDateUtils.formatter));
		
		logger.info("Token: {}", vcverifier.getVCVerifierToken());
		
		/*List<AppliedCustomerBillingRate> appliedList = appliedCustomerBillingRate.listAppliedCustomerBillingRate(null, null, null);
		logger.debug("Number of AppliedCustomerBillingRate found: {}", appliedList.size());
		
		List<AppliedCustomerBillingRate> applied = aggregate(appliedList);
		
		if (applied != null) {
			logger.debug("Number of AppliedCustomerBillingRate aggregates found: {}", applied.size());
			executePayments(applied.toArray(new AppliedCustomerBillingRate[0]));
		}else {
			logger.warn("List of AppliedCustomerBillingRate cannot be null");
		}*/
	}
	
	public String executePayments(AppliedCustomerBillingRate... appliedCustomerBillingRates) {
		logger.info("Execute Payments for {} appliedCustomerBillingRate received", appliedCustomerBillingRates.length);
		int num = 0;
		
		for (AppliedCustomerBillingRate appliedCustomerBillingRate : appliedCustomerBillingRates) {
			
			// filter appliedCustomerBillingRates with isBilled a false -> need to be paid
			if (!appliedCustomerBillingRate.getIsBilled()) {
				logger.debug("Bill {} needs to be paid", appliedCustomerBillingRate.getId());
				
				String token = vcverifier.getVCVerifierToken();
				
				if (token != null) {
					logger.debug("Token: {}", token);
					EGPayment egpayment = payment.paymentNonInteractive(token);
					if (egpayment != null) {
						logger.debug("PaymentId: {}", egpayment.getPaymentId());
						
						//TODO update AppliedCustomerBillingRate and save Payment in TMForum				
						if (updateAppliedCustomerBillingRate(appliedCustomerBillingRate)) {
							num++;
						}
						
					}else {
						logger.warn("There was a problem with the payment for bill: {}", appliedCustomerBillingRate.getId());
					}
				}
				
			}else {
				logger.debug("Bill {} already paid", appliedCustomerBillingRate.getId());
			}
		}
		String response = "Number of payments executed: " + num;
		logger.info(response);
		return response;
	}
		
	private boolean updateAppliedCustomerBillingRate(AppliedCustomerBillingRate applied) {
		logger.info("Update the AppliedCustomerBillingRate for id: {}", applied.getId());		
		
		logger.debug("Creating CustomerBill to set the BillRef in AppliedCustomerBillingRate");
		//create a new CustomerBill to set in the AppliedCustomerBillingRate (BillRef)
		CustomerBillCreate customerBill = new CustomerBillCreate();
		customerBill.setBillingAccount(applied.getBillingAccount());
		customerBill.setAmountDue(applied.getTaxIncludedAmount());
		//TODO verify if it needs other attributes
		
		String idCustomerBill = saveCustomerBill(customerBill);
		if (idCustomerBill != null) {
			logger.info("Created the CustomerBill with id: {}", idCustomerBill);
						
			BillRef bill = new BillRef();
			bill.setId(idCustomerBill);
			bill.setHref(idCustomerBill);
			logger.info("Set id {} in the BillRef", bill.getId());
			
			// create AppliedCustomerBillingRateUpdate object to update the AppliedCustomerBillingRate
			logger.debug("Creating an AppliedCustomerBillingRateUpdate object to update the AppliedCustomerBillingRate with id: {}", applied.getId());	
			AppliedCustomerBillingRateUpdate update = new AppliedCustomerBillingRateUpdate();
			update.setIsBilled(true);
			update.setBill(bill);
			logger.debug("Payload of AppliedCustomerBillingRateUpdate: {}", applied.toJson());	

			try {
				AppliedCustomerBillingRate billUpdate = appliedCustomerBillingRate.updateAppliedCustomerBillingRate(applied.getId(), update);
				logger.info("Update AppliedCustomerBillingRate with id: {}", billUpdate.getId());
				return true;
			} catch (ApiException e) {
				logger.error("Error: ", e.getMessage());
				return false;
			}
		}else {
			logger.warn("CustomerBill cannot be null");
			return false;
		}
	}
	
	private String saveCustomerBill(CustomerBillCreate customerBillCreate) {
		logger.info("Saving the customerBill ...");
		try {
			CustomerBill customerBill = customerBillExtension.createCustomerBill(customerBillCreate);
			logger.info("CustomerBill saved with id: {}", customerBill.getId());
			return customerBill.getId();
		} catch (ApiException e) {
			logger.info("CustomerBill not saved: {}", customerBillCreate.toString());
			logger.error("Error: {}", e.getMessage());
			return null;
		}
	}
	
	private List<AppliedCustomerBillingRate> aggregate(List<AppliedCustomerBillingRate> appliedList) {
		//TODO implement aggregation feature ...
		return appliedList;
	}
}
