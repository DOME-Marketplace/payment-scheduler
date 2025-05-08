package it.eng.dome.payment.scheduler.service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
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
import it.eng.dome.payment.scheduler.model.EGPaymentResponse.Payout;
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
	
	@Autowired
	private TMForumService tmforumService;


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
			
			// filtered appliedList to get only bill with isBilled = false && type recurring 
			List<AppliedCustomerBillingRate> notBilled = appliedList.stream()
					.filter(applied -> !applied.getIsBilled() && applied.getType().toLowerCase().contains("recurring"))
                    .collect(Collectors.toList());
			
			logger.info("Number of AppliedCustomerBillingRate ready for billing: {}", notBilled.size());
			
			//aggregate - payment time
			Map<String, List<AppliedCustomerBillingRate>> aggregates = new HashMap<String, List<AppliedCustomerBillingRate>>();
	
			for (AppliedCustomerBillingRate applied : notBilled) {
				//logger.debug("AppliedCustomerBillingRate payload to be billed: {}", applied.toJson());
				
				// check if exist the product
				if(applied.getProduct() != null) { 
					logger.info("AppliedCustomerBillingRate ID: {} must be billed", applied.getId());
					
					// SET keys with multiple attributes for the map<> aggregates
		        	String endDate = getEndDate(applied);
		        	String paymentPreAuthorizationExternalId = getPaymentPreAuthorizationExternalId(applied.getProduct().getId());
		        	
		        	if (paymentPreAuthorizationExternalId != null) {
		        		// key = preAuthorizationId + endDate => i.e. 9d4fca3b-4bfa-4dba-a09f-348b8d504e44|2025-05-05
			        	String key = paymentPreAuthorizationExternalId + CONCAT_KEY + endDate;

			        	// add in the ArrayList the AppliedCustomerBillingRate
			        	aggregates.computeIfAbsent(key, k -> new ArrayList<>()).add(applied);
		        	}
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
	        	
	        	if (customerOrganizationId != null) {
	        		// build the payload for EG Payment Gateway
		        	PaymentStartNonInteractive payment = getPayloadStartNonInteractive(paymentPreAuthorizationExternalId, customerOrganizationId, applied);
		        	
		        	if (executePayment(payment, applied)) {
		        		++num;
		        	}
	        	}else {
	        		logger.error("Cannot build the Payment payload. The customerOrganizationId is null");
	        	}
	        }
	        
	        logger.info("The payment process scheduled has been terminated successfully at {}", OffsetDateTime.now().format(PaymentDateUtils.formatter));
			
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
		
		//FIXME - use customerId empty - this attribute must be removed in the future
		String customerId = "0"; 
				
		PaymentStartNonInteractive payment = PaymentStartNonInteractiveUtils.getPaymentStartNonInteractive(paymentPreAuthorizationExternalId, customerId, customerOrganizationId);
		
		for (AppliedCustomerBillingRate apply : applied) {
			//logger.debug("AppliedCustomerBillingRate payload: {}", apply.toJson());
			
			// Let's suppose applyId and productId <> NULL 
			String productProviderExternalId = getProductProviderExternalId(apply.getProduct().getId());
			
			if (productProviderExternalId != null) {
			
				PaymentItem paymentItem = new PaymentItem();
	        	float amount = apply.getTaxExcludedAmount().getValue().floatValue();
	        	paymentItem.setAmount(amount);
	        	paymentItem.setCurrency("EUR");
	        	paymentItem.setProductProviderExternalId(productProviderExternalId);
	        	paymentItem.setRecurring(true);
	        	paymentItem.setPaymentItemExternalId(apply.getId());
	        	
	        	Map<String, String> attrs = new HashMap<String, String>();
	        	// attrs.put("additionalProp1", "data1"); // list of attrs if need
	        	paymentItem.setProductProviderSpecificData(attrs);
	        	payment.getBaseAttributes().addPaymentItem(paymentItem);
			
			}else {
				logger.error("Cannot build the Payment payload. The productProviderExternalId is null");
				return null;
			}
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
				
				List<Payout> payoutList = egpayment.getPayoutList();
				Map<String, AppliedCustomerBillingRate> applyMap = applied.stream().collect(Collectors.toMap(AppliedCustomerBillingRate::getId, Function.identity()));
				
				for (Payout payout : payoutList) {
					logger.info("Get payout status: {}", payout.getState());
				
					// Get the status
					Status statusEnum = Status.valueOf(payout.getState().toUpperCase());
					String applyId = payout.getPaymentItemExternalId();
					
					//TODO - To delete - workaround to get applyId - we are waiting for the paymentItemExternalId in the Payout  
					if (applyId == null && applied.size() == 1) {
						logger.debug("Workaround - we are waiting for the paymentItemExternalId in the Payout");
						logger.info("Get applyId from list because is only one!");
						applyId = applied.get(0).getId();
					}				
										
					if (applyId != null) {
						logger.info("Get applyId: {}", applyId);
					
						AppliedCustomerBillingRate appliedCustomerBillingRate = applyMap.get(applyId);
						
						logger.info("Handling payment status: {}", statusEnum.name());
						handlePaymentStatus(statusEnum, appliedCustomerBillingRate);
						
					} else {
						logger.warn("Payout: {}", payout.toJson());
						logger.warn("Cannot find the paymentItemExternalId from Payout. It's the applyId");
						return false;
					}
				}
				
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

		String paymentPreAuthorizationExternalId = null;

		if (productId != null) {

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

		logger.error("Couldn't retrieve the paymentPreAuthorizationExternalId from product {}", productId);
		return paymentPreAuthorizationExternalId;
	}
	
	/*
	 * Retrieve the ProductProviderExternalId from the relatedParty - role => ProviderType 
	 */
	private String getProductProviderExternalId(String productId) {

		if (productId != null) {

			Product product = productApis.getProduct(productId, null);
			if (product != null) {
				List<RelatedParty> parties = product.getRelatedParty();
				for (RelatedParty party : parties) {
					if (ProviderType.isValid(party.getRole())) {
						logger.debug("Retrieved productProviderExternalId: {}", party.getId());
						return party.getId(); 
					}
				}
			}

		}
		
		logger.error("Couldn't retrieve the productProviderExternalId from product {}", productId);
		return null;
	}
			
	/*
	 * Retrieve the CustomerOrganizationId from the relatedParty - role => CustomerType 
	 */
	private String getCustomerOrganizationId(String productId) {
		
		if (productId != null) {

			Product product = productApis.getProduct(productId, null);
			if (product != null) {
				List<RelatedParty> parties = product.getRelatedParty();
				for (RelatedParty party : parties) {
					if (CustomerType.isValid(party.getRole())) {
						logger.debug("Retrieved customerOrganizationId: {}", party.getId());
						return party.getId(); 
					}
				}
			}

		}
		
		logger.error("Couldn't retrieve the customerOrganizationId from product {}", productId);
		return null;
	}
	
		
	private enum Status {
	    PROCESSED,
	    FAILED,
	    PENDING
	}
	
	private void handlePaymentStatus(Status status, AppliedCustomerBillingRate applied) {
	    switch (status) {
	        case PROCESSED:
	            handleStatusProcessed(applied);
	            break;
	        case PENDING:
	            handleStatusPending(applied);
	            break;
	        case FAILED:
	            handleStatusFailed(applied);
	            break;
	    }
	}

	private void handleStatusProcessed(AppliedCustomerBillingRate applied) {
				
		if (tmforumService.updateAppliedCustomerBillingRate(applied)) { // set isBilled = true and add CustomerBill (BillRef)
			logger.info("The appliedCustomerBillingRateId {} has been updated successfully", applied.getId());
		} else {
			logger.error("Couldn't update appliedCustomerBillingRate in TMForum");
		}	
	}

	private void handleStatusPending(AppliedCustomerBillingRate applied) {
		
		if (tmforumService.setIsBilled(applied, true)) { // set isBilled = true
			logger.info("IsBilled has been updated successfully for the appliedCustomerBillingRateId {}", applied.getId());
		} else {
			logger.error("Couldn't set isBilled for the appliedCustomerBillingRate in TMForum");
		}	
	}
	
	private void handleStatusFailed(AppliedCustomerBillingRate applied) {
		logger.info("No update for the appliedCustomerBillingRateId {}", applied.getId());
	}
	
	
	private String getEndDate(AppliedCustomerBillingRate applied) {
		
		if (applied.getPeriodCoverage() != null && applied.getPeriodCoverage().getEndDateTime() != null) {
			
			OffsetDateTime endDateTime = applied.getPeriodCoverage().getEndDateTime();
			return endDateTime.toLocalDate().toString();
		}else {
			
			OffsetDateTime date = OffsetDateTime.now();
			String onlyDate = date.toLocalDate().toString();
			logger.warn("Cannot retrive the EndDateTime from PeriodCoverage. It will be set the current DateTime: {}", onlyDate);
			return onlyDate;
		}
	}
}