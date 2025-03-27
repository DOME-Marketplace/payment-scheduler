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

import it.eng.dome.payment.scheduler.dto.PaymentStartNonInteractive;
import it.eng.dome.payment.scheduler.model.EGPayment;
import it.eng.dome.payment.scheduler.tmf.TmfApiFactory;
import it.eng.dome.payment.scheduler.util.PaymentDateUtils;
import it.eng.dome.tmforum.tmf637.v4.api.ProductApi;
import it.eng.dome.tmforum.tmf637.v4.model.Characteristic;
import it.eng.dome.tmforum.tmf637.v4.model.Product;
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
	//private final static String PREFIX_KEY = "aggregate-period-";
	
	@Autowired
	private TmfApiFactory tmfApiFactory;

	private AppliedCustomerBillingRateApi appliedCustomerBillingRate;
	private CustomerBillExtensionApi customerBillExtension;
	private ProductApi productInventory;
	
	@Autowired
	private StartPayment payment;

	@Autowired
	private VCVerifier vcverifier;
	
	@Override
	public void afterPropertiesSet() throws Exception {
		it.eng.dome.tmforum.tmf678.v4.ApiClient apiClientTMF678 = tmfApiFactory.getTMF678CustomerBillApiClient();
		appliedCustomerBillingRate = new AppliedCustomerBillingRateApi(apiClientTMF678);
		customerBillExtension = new CustomerBillExtensionApi(apiClientTMF678);	
		
		it.eng.dome.tmforum.tmf637.v4.ApiClient apiClientTMF637 = tmfApiFactory.getTMF637ProductInventoryApiClient();
		productInventory = new ProductApi(apiClientTMF637);	
	}
	
	/**
	 * Main method called by PaymentScheduler service (in the PaymentTask class)
	 * 
	 * @throws ApiException
	 */
	public void payments() throws ApiException {
		logger.info("Starting payments at {}", OffsetDateTime.now().format(PaymentDateUtils.formatter));
		
		List<AppliedCustomerBillingRate> appliedList = appliedCustomerBillingRate.listAppliedCustomerBillingRate(null, null, 1000);
		executePayments(appliedList);
	}
	
	/**
	 * Execute payments by using the aggregate feature from a list of bill
	 * 
	 * @param appliedList
	 * @return String - provide the number of payments 
	 */
	public String executePayments(List<AppliedCustomerBillingRate> appliedList) {
		int num = 0;
		
		if (appliedList != null && !appliedList.isEmpty()) {
			logger.debug("Number of AppliedCustomerBillingRate found: {}", appliedList.size());
			
			for (AppliedCustomerBillingRate applied : appliedList) {
				
				logger.debug("Verify AppliedCustomerBillingRateId: {}", applied.getId());
				
				logger.debug("Check if IsBilled: {}", applied.getIsBilled());
				
				logger.debug("AppliedCustomerBillingRate payload: {}", applied.toJson());
				
				if(!applied.getIsBilled()) {
					logger.debug("The acbr with ID: {} must be billed",applied.getId());
					executePayments(applied, applied.getTaxIncludedAmount().getValue());
				}
			}
			
			/*
			// apply aggregate feature
			Map<String, List<AppliedCustomerBillingRate>> aggregates = aggregate(appliedList);

			if (!aggregates.isEmpty()) {
				logger.debug("Number of AppliedCustomerBillingRate aggregates: {}", aggregates.size());
								
				for (Map.Entry<String, List<AppliedCustomerBillingRate>> entry : aggregates.entrySet()) {
					
					String key = entry.getKey();
					List<AppliedCustomerBillingRate> applied = entry.getValue();
					logger.debug("Number of applied aggregate: {} - for {}", applied.size(), key);

					float taxIncludedAmount = 0;
					AppliedCustomerBillingRate appliedCustomerBillingRate = null;
					for (AppliedCustomerBillingRate apply : applied) {
						taxIncludedAmount = +apply.getTaxIncludedAmount().getValue();
						if (appliedCustomerBillingRate == null) {
							appliedCustomerBillingRate = apply;
						}
					}
					
					executePayments(appliedCustomerBillingRate, taxIncludedAmount);
					num++;
				}				
				
			} else {
				logger.warn("List of AppliedCustomerBillingRate aggregate is empty");
			}
			*/
		}else {
			logger.warn("List of AppliedCustomerBillingRate is empty");
		}
		
		String response = "Number of payments executed: " + num;
		logger.info(response);
		return response;
	}
	
	/**
	 * 
	 * @param appliedCustomerBillingRate
	 * @param taxIncludedAmount
	 * @return boolean - if the process has been completed successfully or not (include the saving/updating data in TM Forum)
	 */
	private boolean executePayments(AppliedCustomerBillingRate appliedCustomerBillingRate, float taxIncludedAmount) {
		
		if ((appliedCustomerBillingRate != null) && (!appliedCustomerBillingRate.getIsBilled())) {
			
			String token = vcverifier.getVCVerifierToken();
			logger.info("TEST TOKEN: {}", token);

			//if (token != null) {


				// TODO -> must be retrieve the paymentPreAuthorizationId from productCharatheristic ????
				String paymentPreAuthorizationId = getPaymentPreAuthorizationId(appliedCustomerBillingRate.getProduct().getId());
				logger.info("PaymentPreAuthorizationId: {}", paymentPreAuthorizationId);
				
				if (paymentPreAuthorizationId != null) {
					
					// TODO: set the list of params - default values for testing
					String customerId = "1";
					String customerOrganizationId = "1"; 
					String invoiceId = "ab-132";
					int productProviderId = 1; 
					String currency = "EUR";

					// Get payload PaymentStartNonInteractive
					PaymentStartNonInteractive paymentStartNonInteractive = payment.getPaymentStartNonInteractive(customerId, customerOrganizationId, invoiceId, productProviderId, taxIncludedAmount, currency, paymentPreAuthorizationId);
					
					// TODO - please take care of this comment
					// these lines provide 2 actions: payment (paymentNonInteractive) + saving data (updateAppliedCustomerBillingRate)
					// these 2 actions must be an atomic task
					EGPayment egpayment = payment.paymentNonInteractive(token, paymentStartNonInteractive);
																			   
					if (egpayment != null) {
						logger.debug("PaymentId: {}", egpayment.getPaymentId());
						
						//update AppliedCustomerBillingRate and save Payment in TMForum				
						if (updateAppliedCustomerBillingRate(appliedCustomerBillingRate)) {
							logger.info("The overall payment process has been terminated with a successful");
							return true;
						} else {
							logger.warn("Cannot saving/updating data in TM Forum");
							//TODO => probably it must be foreseen the roll-back procedure!
							return false;
						}
					}
				}
				
//			} else {
//				logger.warn("Token cannot be null");
//				return false;
//			}
		}
		return false;
	}
	
	
	private String getPaymentPreAuthorizationId(String productId) {
		logger.info("Start getting preauthorizationId...");
		
		// default
		String paymentPreAuthorizationId = "bae4cd08-1385-4e81-aa6a-260ac2954f1c";

		if (productId == null) {
			// TODO Exception
			logger.error("The productId is null..");
		}

		// getProduct
		try {
			Product product = productInventory.retrieveProduct(productId, null);

			List<Characteristic> prodChars = product.getProductCharacteristic();
			// TODO Manage exception
			if (prodChars != null && !prodChars.isEmpty()) {
				for (Characteristic c : prodChars) {
					if (c.getName().trim().equalsIgnoreCase("paymentPreAuthorizationId")) {
						paymentPreAuthorizationId = c.getValue().toString();
						break;
					}
				}
			}

		} catch (it.eng.dome.tmforum.tmf637.v4.ApiException e) {
			// TODO Auto-generated catch block
			logger.error("Error {}", e.getMessage());
		}

		return paymentPreAuthorizationId;
	}
			
	/**
	 * 
	 * @param applied
	 * @return
	 */
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
	/*
	private Map<String, List<AppliedCustomerBillingRate>> aggregate(List<AppliedCustomerBillingRate> appliedList) {
		logger.info("Apply aggregate feature");
		
		Map<String, List<AppliedCustomerBillingRate>> aggregates = new HashMap<>();
		
		for (AppliedCustomerBillingRate appliedCustomerBillingRate : appliedList) {
			if (!appliedCustomerBillingRate.getIsBilled()) { // not billed
				
				// grouping on the same endDateTime
				OffsetDateTime endDateTime = appliedCustomerBillingRate.getPeriodCoverage().getEndDateTime();
				
				// key example => aggregate-period-2025-12-31
				String keyPeriodEndDate = PREFIX_KEY + getEndDate(endDateTime);							
				aggregates.computeIfAbsent(keyPeriodEndDate, k -> new ArrayList<>()).add(appliedCustomerBillingRate);
			}
		}

		return aggregates;
	}
	
	
	private String getEndDate(OffsetDateTime date) {
		String onlyDate = date.toLocalDate().toString();
		logger.info("Only Date: {}", onlyDate);
		return onlyDate;
	}*/
}