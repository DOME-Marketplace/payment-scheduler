package it.eng.dome.payment.scheduler.service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import it.eng.dome.brokerage.api.AppliedCustomerBillRateApis;
import it.eng.dome.brokerage.api.ProductApis;
import it.eng.dome.payment.scheduler.dto.PaymentItem;
import it.eng.dome.payment.scheduler.dto.PaymentStartNonInteractive;
import it.eng.dome.payment.scheduler.model.EGPaymentResponse;
import it.eng.dome.payment.scheduler.tmf.TmfApiFactory;
import it.eng.dome.payment.scheduler.util.PaymentDateUtils;
import it.eng.dome.payment.scheduler.util.PaymentStartNonInteractiveUtils;
import it.eng.dome.tmforum.tmf637.v4.model.Characteristic;
import it.eng.dome.tmforum.tmf637.v4.model.Product;
import it.eng.dome.tmforum.tmf678.v4.ApiException;
import it.eng.dome.tmforum.tmf678.v4.model.AppliedCustomerBillingRate;
import it.eng.dome.tmforum.tmf678.v4.model.AppliedCustomerBillingRateUpdate;
import it.eng.dome.tmforum.tmf678.v4.model.BillRef;
import it.eng.dome.tmforum.tmf678.v4.model.CustomerBillCreate;

@Component(value = "paymentService")
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class PaymentService implements InitializingBean {

	private final Logger logger = LoggerFactory.getLogger(PaymentService.class);
	private final static String CONCAT_KEY = "|";
	
	@Autowired
	private TmfApiFactory tmfApiFactory;
	
	@Autowired
	private StartPayment payment;

	@Autowired
	private VCVerifier vcverifier;


	private AppliedCustomerBillRateApis appliedApis;
	private ProductApis productApis;
	
	@Override
	public void afterPropertiesSet() throws Exception {
		appliedApis = new AppliedCustomerBillRateApis(tmfApiFactory.getTMF678CustomerBillApiClient());
		productApis = new ProductApis(tmfApiFactory.getTMF637ProductInventoryApiClient());
	}
	
	/**
	 * Main method called by PaymentScheduler service (in the PaymentTask class)
	 * 
	 * @throws ApiException
	 */
	public void payments() throws ApiException {
		logger.info("Starting payments at {}", OffsetDateTime.now().format(PaymentDateUtils.formatter));

		List<AppliedCustomerBillingRate> appliedList = appliedApis.getAllAppliedCustomerBillingRates(null);
		payments(appliedList);
	}
	
	/**
	 * Execute payments by using the aggregate feature from a list of bill
	 * 
	 * @param appliedList
	 * @return String - provide the number of payments 
	 */
	public String payments(List<AppliedCustomerBillingRate> appliedList) {
		int num = 0;
		
		if (appliedList != null && !appliedList.isEmpty()) {
			logger.debug("Total number of AppliedCustomerBillingRate found: {}", appliedList.size());
			
			// filtered appliedList isBilled
			List<AppliedCustomerBillingRate> notBilled = appliedList.stream()
					.filter(applied -> !applied.getIsBilled())
                    .collect(Collectors.toList());
			
			logger.debug("Number of AppliedCustomerBillingRate ready for billing: {}", notBilled.size());
			
			//aggregate - payment time
			Map<String, List<AppliedCustomerBillingRate>> aggregates = new HashMap<String, List<AppliedCustomerBillingRate>>();
			
			for (AppliedCustomerBillingRate applied : notBilled) {
				logger.debug("AppliedCustomerBillingRate payload: {}", applied.toJson());
				
				if(applied.getProduct()!= null) {
					logger.debug("AppliedCustomerBillingRate ID: {} must be billed", applied.getId());
					
					OffsetDateTime endDateTime = applied.getPeriodCoverage().getEndDateTime();
					
					// SET keys with multiple attributes for the map<> aggregate
		        	String endDate = getEndDate(endDateTime);
		        	String paymentPreAuthorizationExternalId = getPaymentPreAuthorizationExternalId(applied.getProduct().getId());
		        			        	
		        	String key = paymentPreAuthorizationExternalId + CONCAT_KEY + endDate;
		        	
		        	aggregates.computeIfAbsent(key, k -> new ArrayList<>()).add(applied);
				}
			}
			
			// payment
			logger.debug("Number of payment to pay: {}", aggregates.size());
	        for (Entry<String, List<AppliedCustomerBillingRate>> entry : aggregates.entrySet()) {
	        	
	        	List<AppliedCustomerBillingRate> applied = entry.getValue();
	        	String key = entry.getKey();
	        	
	        	String paymentPreAuthorizationExternalId = key.substring(0, key.indexOf(CONCAT_KEY));
	        	// build the payload for Payment Gateway 
	        	PaymentStartNonInteractive payment = getPayloadStartNonInteractive(paymentPreAuthorizationExternalId, applied);
	        	
	        	if (executePayment(payment, applied)) {
	        		++num;
	        	}
	        	
	        }
			
			/*for (AppliedCustomerBillingRate applied : notBilled) {
				logger.debug("AppliedCustomerBillingRate payload: {}", applied.toJson());
				
				if(applied.getProduct()!= null) {
					
					logger.debug("AppliedCustomerBillingRate ID: {} must be billed", applied.getId());
					
					OffsetDateTime endDateTime = applied.getPeriodCoverage().getEndDateTime();		        	
		        	
		        	// SET keys with multiple attributes for the map<> aggregate
		        	String endDate = getEndDate(endDateTime);
		        	String paymentPreAuthorizationExternalId = getPaymentPreAuthorizationExternalId(applied.getProduct().getId());
		        	String customerOrganizationId = getCustomerOrganizationId(applied.getBillingAccount().getId());
					
		        	String key = paymentPreAuthorizationExternalId + CONCAT_KEY + endDate;
		        	logger.debug("key created: {}", key);
		        	
		        	if (!payments.containsKey(key)) {
		        		// use this customerId
		        		String customerId = "1"; 
		        		String invoiceId = "inv-123" + (1000 + new Random().nextInt(9000));
		        		
		        		// TODO => how to set randomExternalId
		        		String randomExternalId = "479c2a6d-5197-452c-ba1b-fd1393c5" + (1000 + new Random().nextInt(9000));
		        		
		        		PaymentStartNonInteractive payment = PaymentStartNonInteractiveUtils.getPaymentStartNonInteractive(paymentPreAuthorizationExternalId, randomExternalId, customerId, customerOrganizationId, invoiceId);
		        		logger.debug("Create new payload");
		        		payments.put(key, payment);
		            }
		        	
		        	PaymentItem paymentItem = new PaymentItem();
		        	float amount = applied.getTaxExcludedAmount().getValue().floatValue();
		        	paymentItem.setAmount(amount);
		        	paymentItem.setCurrency("EUR");
		        	paymentItem.setProductProviderExternalId("eda11ca9-cf3b-420d-8570-9d3ecf3613ac");
		        	paymentItem.setRecurring(true);
		        	
		        	Map<String, String> attrs = new HashMap<String, String>();
		    		// attrs.put("additionalProp1", "data1");
		        	paymentItem.setProductProviderSpecificData(attrs);
		        	payments.get(key).getBaseAttributes().addPaymentItem(paymentItem); 
				}
			}*/
			
			// payment
			/*
			logger.debug("Number of payment to pay: {}", payments.size());
	        for (Entry<String, PaymentStartNonInteractive> entry : payments.entrySet()) {
	        	logger.debug("Payment payload: {}", entry.getValue().toJson());
	        	
	        	if (executePayment(entry.getValue())) {
					++num;
				}
	        }
			*/
			
		}else {
			logger.warn("List of AppliedCustomerBillingRate is empty");
		}
		
		String response = "Number of payments executed: " + num;
		logger.info(response);
		return response;
	}
	
	private PaymentStartNonInteractive getPayloadStartNonInteractive(String paymentPreAuthorizationExternalId, List<AppliedCustomerBillingRate> applied) {
		
		// use this customerId
		String customerId = "1"; 
		String invoiceId = "inv-123" + (1000 + new Random().nextInt(9000));
		
		// TODO => how to set randomExternalId
		String randomExternalId = "479c2a6d-5197-452c-ba1b-fd1393c5" + (1000 + new Random().nextInt(9000));
		String customerOrganizationId = getCustomerOrganizationId("id");
		
		PaymentStartNonInteractive payment = PaymentStartNonInteractiveUtils.getPaymentStartNonInteractive(paymentPreAuthorizationExternalId, randomExternalId, customerId, customerOrganizationId, invoiceId);
		
		for (AppliedCustomerBillingRate apply : applied) {
			logger.debug("AppliedCustomerBillingRate payload: {}", apply.toJson());
			
			PaymentItem paymentItem = new PaymentItem();
        	float amount = apply.getTaxExcludedAmount().getValue().floatValue();
        	paymentItem.setAmount(amount);
        	paymentItem.setCurrency("EUR");
        	paymentItem.setProductProviderExternalId("eda11ca9-cf3b-420d-8570-9d3ecf3613ac");
        	paymentItem.setRecurring(true);
        	
        	Map<String, String> attrs = new HashMap<String, String>();
    		// attrs.put("additionalProp1", "data1");
        	paymentItem.setProductProviderSpecificData(attrs);
        	payment.getBaseAttributes().addPaymentItem(paymentItem); 
		}       
		
		return payment;
	}
	
	private boolean executePayment(PaymentStartNonInteractive paymentStartNonInteractive, List<AppliedCustomerBillingRate> applied) {
		
		String token = vcverifier.getVCVerifierToken();
		if (token != null) {
			EGPaymentResponse egpayment = payment.paymentNonInteractive(token, paymentStartNonInteractive);
			   
			if (egpayment != null) {
				logger.debug("PaymentExternalId: {}", egpayment.getPaymentExternalId());
				
				//update AppliedCustomerBillingRates and save Payment in TMForum				
				for (AppliedCustomerBillingRate appliedCustomerBillingRate : applied) {

					if (updateAppliedCustomerBillingRate(appliedCustomerBillingRate)) {
						logger.info("The appliedCustomerBillingRateId {} has been updated", appliedCustomerBillingRate.getId());
						//return true;
					} else {
						logger.warn("Cannot saving/updating data in TM Forum");
						//TODO => probably it must be foreseen the roll-back procedure!
						return false;
					}
				}
				
				logger.info("The overall payment process has been terminated with a successful");
				return true;
			}else {
				logger.error("Error in the EG Payment response");
				return false;
			}
		} else {
			logger.warn("Token cannot be null");
			return false;
		}
	}
	
	/**
	 * 
	 * @param appliedCustomerBillingRate
	 * @param taxIncludedAmount
	 * @return boolean - if the process has been completed successfully or not (include the saving/updating data in TM Forum)
	 */
	/*private boolean executePayments(AppliedCustomerBillingRate appliedCustomerBillingRate, float taxIncludedAmount) {
		
		if ((appliedCustomerBillingRate != null) && (!appliedCustomerBillingRate.getIsBilled())) {
			
			String token = vcverifier.getVCVerifierToken();
			//logger.debug("Token: {}", token);

			if (token != null) {


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
					PaymentStartNonInteractive paymentStartNonInteractive = PaymentStartNonInteractiveUtils.getPaymentStartNonInteractive(paymentPreAuthorizationExternalId, randomExternalId, customerId, customerOrganizationId, invoiceId);
					
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
				
			} else {
				logger.warn("Token cannot be null");
				return false;
			}
		}
		return false;
	}
	*/
	
	/*
	 * Retrieve the paymentPreAuthorizationExternalId from the productCharacteristic 
	 */
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

			Product product = productApis.getProduct(productId, null);

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

		return paymentPreAuthorizationExternalId;
	}
			
	/*
	 * Retrieve the CustomerOrganizationId from the relatedParty -> role=Customer 
	 */
	private String getCustomerOrganizationId(String billingAccountId) {
		logger.info("Start getting CustomerOrganizationId ...");
		String customerOrganizationId = "1";
		
		if (billingAccountId != null) {
			// get CustomerOrganizationId from relatedParty with the role=Customer
			return customerOrganizationId;
		}
		return customerOrganizationId;
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
		
		String idCustomerBill = appliedApis.createCustomerBill(customerBill);
		if (idCustomerBill != null) {
			// create BillRef		
			logger.info("Preparing the BillRef");
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

			return appliedApis.updateAppliedCustomerBillingRate(applied.getId(), update);
			/*
			try {
				AppliedCustomerBillingRate billUpdate = appliedCustomerBillingRate.updateAppliedCustomerBillingRate(applied.getId(), update);
				logger.info("Update AppliedCustomerBillingRate with id: {}", billUpdate.getId());
				return true;
			} catch (ApiException e) {
				logger.error("Error: ", e.getMessage());
				return false;
			}*/
		}else {
			logger.warn("CustomerBill cannot be null");
			return false;
		}
	}
	/*
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
	}*/
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
	
	*/

	private String getEndDate(OffsetDateTime date) {
		String onlyDate = date.toLocalDate().toString();
		return onlyDate;
	}
}