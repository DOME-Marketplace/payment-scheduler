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
import it.eng.dome.payment.scheduler.util.CustomerType;
import it.eng.dome.payment.scheduler.util.PaymentDateUtils;
import it.eng.dome.payment.scheduler.util.PaymentStartNonInteractiveUtils;
import it.eng.dome.payment.scheduler.util.ProviderType;
import it.eng.dome.tmforum.tmf637.v4.model.Characteristic;
import it.eng.dome.tmforum.tmf637.v4.model.Product;
import it.eng.dome.tmforum.tmf637.v4.model.RelatedParty;
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
			
			// filtered appliedList isBilled -> filter only applied not billed
			List<AppliedCustomerBillingRate> notBilled = appliedList.stream()
					.filter(applied -> !applied.getIsBilled())
                    .collect(Collectors.toList());
			
			logger.info("Number of AppliedCustomerBillingRate ready for billing: {}", notBilled.size());
			
			//aggregate - payment time
			Map<String, List<AppliedCustomerBillingRate>> aggregates = new HashMap<String, List<AppliedCustomerBillingRate>>();
			// aggregate process
			for (AppliedCustomerBillingRate applied : notBilled) {
				//logger.debug("AppliedCustomerBillingRate payload to be billed: {}", applied.toJson());
				
				// check if exist the product
				if(applied.getProduct() != null) { 
					logger.info("AppliedCustomerBillingRate ID: {} must be billed", applied.getId());
					
					OffsetDateTime endDateTime = applied.getPeriodCoverage().getEndDateTime();
					
					// SET keys with multiple attributes for the map<> aggregates
		        	String endDate = getEndDate(endDateTime);
		        	String paymentPreAuthorizationExternalId = getPaymentPreAuthorizationExternalId(applied.getProduct().getId());
		        			        	
		        	// key = preAuthorizationId + endDate
		        	String key = paymentPreAuthorizationExternalId + CONCAT_KEY + endDate;
		        	
		        	// add in the ArrayList the AppliedCustomerBillingRate
		        	aggregates.computeIfAbsent(key, k -> new ArrayList<>()).add(applied);
				}
			}
			
			// payment
			logger.debug("Number of aggregates applied to pay: {}", aggregates.size());
	        for (Entry<String, List<AppliedCustomerBillingRate>> entry : aggregates.entrySet()) {
	        	
	        	List<AppliedCustomerBillingRate> applied = entry.getValue();
	        	logger.debug("Num of applied in the aggregate: {}", applied.size());
	        	String key = entry.getKey();
	        	
	        	// retrieve the paymentPreAuthorizationExternalId from key
	        	String paymentPreAuthorizationExternalId = key.substring(0, key.indexOf(CONCAT_KEY));
	        	
	        	String customerOrganizationId = getCustomerOrganizationId(applied.get(0).getProduct().getId());
	        	
	        	// build the payload for EG Payment Gateway
	        	//TODO - verify if it must be added new attribute in the getPayloadStartNonInteractive method
	        	PaymentStartNonInteractive payment = getPayloadStartNonInteractive(paymentPreAuthorizationExternalId, customerOrganizationId, applied);
	        	
	        	if (executePayment(payment, applied)) {
	        		++num;
	        	}
	        	
	        }
			
		}else {
			logger.warn("List of AppliedCustomerBillingRate is empty");
		}
		
		String response = "Number of payments executed: " + num;
		logger.info(response);
		return response;
	}
	
	/*
	 * Create the payload for StartNonInteractive call
	 */
	private PaymentStartNonInteractive getPayloadStartNonInteractive(String paymentPreAuthorizationExternalId, String customerOrganizationId, List<AppliedCustomerBillingRate> applied) {
		
		// use this customerId
		String customerId = "1"; 
		String invoiceId = "inv-123" + (1000 + new Random().nextInt(9000));
		
		// TODO => how to set randomExternalId (it cannot be the same) 
		String randomExternalId = "479c2a6d-5197-452c-ba1b-fd1393c5" + (1000 + new Random().nextInt(9000));
		//String customerOrganizationId = getCustomerOrganizationId(applied.get(0).getProduct().getId());
				
		PaymentStartNonInteractive payment = PaymentStartNonInteractiveUtils.getPaymentStartNonInteractive(paymentPreAuthorizationExternalId, randomExternalId, customerId, customerOrganizationId, invoiceId);
		
		for (AppliedCustomerBillingRate apply : applied) {
			//logger.debug("AppliedCustomerBillingRate payload: {}", apply.toJson());
			
			PaymentItem paymentItem = new PaymentItem();
        	float amount = apply.getTaxExcludedAmount().getValue().floatValue();
        	paymentItem.setAmount(amount);
        	paymentItem.setCurrency("EUR");
        	paymentItem.setProductProviderExternalId(getProductProviderExternalId(apply.getProduct().getId()));
        	paymentItem.setRecurring(true);
        	
        	Map<String, String> attrs = new HashMap<String, String>();
    		// attrs.put("additionalProp1", "data1");
        	paymentItem.setProductProviderSpecificData(attrs);
        	payment.getBaseAttributes().addPaymentItem(paymentItem); 
		}       
		
		return payment;
	}
	
	/**
	 * 
	 * @param paymentStartNonInteractive
	 * @param applied
	 * @return boolean - if the process has been completed successfully or not (include the saving/updating data in TM Forum)
	 */
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
				logger.error("Error in the EG Payment Server");
				return false;
			}
		} else {
			logger.error("Error to get the Token from VC Verfier Server");
			return false;
		}
	}
	
	
	/*
	 * Retrieve the paymentPreAuthorizationExternalId from the productCharacteristic 
	 */
	private String getPaymentPreAuthorizationExternalId(String productId) {
		
		//FIXME set default value
		String paymentPreAuthorizationExternalId = "9d4fca3b-4bfa-4dba-a09f-348b8d504e44";

		if (productId == null) {
			// TODO Exception
			logger.error("The productId is null..");
		} else {

			Product product = productApis.getProduct(productId, null);
			if (product != null) {
				List<Characteristic> prodChars = product.getProductCharacteristic();

				// TODO Manage exception
				if (prodChars != null && !prodChars.isEmpty()) {
					for (Characteristic c : prodChars) {
						if (c.getName().trim().equalsIgnoreCase("paymentPreAuthorizationExternalId")) {
							paymentPreAuthorizationExternalId = c.getValue().toString();
							logger.info("Found the paymentPreAuthorizationId: {}", paymentPreAuthorizationExternalId);
							return paymentPreAuthorizationExternalId;
						}
					}
				}				
			}				
		}

		return paymentPreAuthorizationExternalId;
	}
	
	/*
	 * Retrieve the ProductProviderExternalId from the relatedParty - role => ProviderType 
	 */
	private String getProductProviderExternalId(String productId) {

		String productProviderExternalId = "eda11ca9-cf3b-420d-8570-9d3ecf3613ac";

		if (productId == null) {
			logger.error("The productId is null..");
		} else {

			Product product = productApis.getProduct(productId, null);
			if (product != null) {
				List<RelatedParty> parties = product.getRelatedParty();
				for (RelatedParty party : parties) {
					if (ProviderType.isValid(party.getRole())) {
						logger.debug("Retrieved productProviderExternalId: {}", party.getId());
						return party.getId().replaceFirst("^urn:ngsi-ld:organization:?", ""); 
					}
				}
			}

		}
		return productProviderExternalId;
	}
			
	/*
	 * Retrieve the CustomerOrganizationId from the relatedParty - role => CustomerType 
	 */
	private String getCustomerOrganizationId(String productId) {
		
		String customerOrganizationId = "1";
		
		if (productId == null) {
			logger.error("The productId is null..");
		} else {

			Product product = productApis.getProduct(productId, null);
			if (product != null) {
				List<RelatedParty> parties = product.getRelatedParty();
				for (RelatedParty party : parties) {
					if (CustomerType.isValid(party.getRole())) {
						logger.debug("Retrieved customerOrganizationId: {}", party.getId());
						return party.getId().replaceFirst("^urn:ngsi-ld:organization:?", ""); 
					}
				}
			}

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