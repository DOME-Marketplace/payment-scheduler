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
import it.eng.dome.payment.scheduler.model.EGPaymentResponse;
import it.eng.dome.payment.scheduler.tmf.TmfApiFactory;
import it.eng.dome.payment.scheduler.util.PaymentDateUtils;
import it.eng.dome.payment.scheduler.util.PaymentStartNonInteractiveUtils;
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
				
				logger.info("Verify AppliedCustomerBillingRateId: {}", applied.getId());
				logger.debug("AppliedCustomerBillingRate payload: {}", applied.toJson());
				
				if(!applied.getIsBilled()) {
					logger.debug("The acbr with ID: {} must be billed", applied.getId());
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
			//logger.debug("Token: {}", token);

			//if (token != null) {


				// TODO -> must be retrieve the paymentPreAuthorizationId from productCharatheristic ????
				String paymentPreAuthorizationId = getPaymentPreAuthorizationExternalId(appliedCustomerBillingRate.getProduct().getId());
				logger.info("PaymentPreAuthorizationExternalId used: {}", paymentPreAuthorizationId);
				
				if (paymentPreAuthorizationId != null) {
					
					// TODO: set the list of params - default values for testing
					String customerId = "1";
					String customerOrganizationId = "1"; 
					String invoiceId = "ab-132";
					String productProviderExternalId = "eda11ca9-cf3b-420d-8570-9d3ecf3613ac"; 
					String currency = "EUR";

					// Get payload PaymentStartNonInteractive
					PaymentStartNonInteractive paymentStartNonInteractive = PaymentStartNonInteractiveUtils.getPaymentStartNonInteractive(customerId, customerOrganizationId, invoiceId, productProviderExternalId, taxIncludedAmount, currency, paymentPreAuthorizationId);
					
					// TODO - please take care of this comment
					// these lines provide 2 actions: payment (paymentNonInteractive) + saving data (updateAppliedCustomerBillingRate)
					// these 2 actions must be an atomic task
					EGPaymentResponse egpayment = payment.paymentNonInteractive(token, paymentStartNonInteractive);
																			   
					if (egpayment != null) {
						logger.debug("PaymentExternalId: {}", egpayment.getPaymentExternalId());
						
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
	
	
	private String getPaymentPreAuthorizationExternalId(String productId) {
		logger.info("Start getting PreAuthorizationExternalId ...");
		
		// default -> became paymentPreAuthorizationExternalId
		String paymentPreAuthorizationExternalId = "9d4fca3b-4bfa-4dba-a09f-348b8d504e44";

		if (productId == null) {
			// TODO Exception
			logger.error("The productId is null..");
		}else {

			// getProduct
			logger.debug("ProductId is {}", productId);
			try {
				if (productInventory != null) {
					
					Product product = productInventory.retrieveProduct(productId, null);
		
					if (product != null) {
						logger.info("Product: {}", product.toJson());
						
						List<Characteristic> prodChars = product.getProductCharacteristic();

						// TODO Manage exception
						if (prodChars != null && !prodChars.isEmpty()) {
							for (Characteristic c : prodChars) {
								if (c.getName().trim().equalsIgnoreCase("paymentPreAuthorizationExternalId")) {
									paymentPreAuthorizationExternalId = c.getValue().toString();
									logger.info("Found the paymentPreAuthorizationId: {}", paymentPreAuthorizationExternalId);
									break;
								}
							}
						}
						
					}
				}
			} catch (it.eng.dome.tmforum.tmf637.v4.ApiException e) {
				// TODO Auto-generated catch block
				logger.error("Error {}", e.getMessage());
				return paymentPreAuthorizationExternalId;
			}
		}

		return paymentPreAuthorizationExternalId;
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