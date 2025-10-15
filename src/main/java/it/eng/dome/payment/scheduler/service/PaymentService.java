package it.eng.dome.payment.scheduler.service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import it.eng.dome.brokerage.api.AppliedCustomerBillRateApis;
import it.eng.dome.brokerage.api.ProductInventoryApis;
import it.eng.dome.brokerage.api.fetch.FetchUtils;
import it.eng.dome.payment.scheduler.dto.PaymentItem;
import it.eng.dome.payment.scheduler.dto.PaymentStartNonInteractive;
import it.eng.dome.payment.scheduler.model.EGPaymentResponse;
import it.eng.dome.payment.scheduler.model.EGPaymentResponse.Payout;
import it.eng.dome.payment.scheduler.util.CustomerType;
import it.eng.dome.payment.scheduler.util.PaymentDateUtils;
import it.eng.dome.payment.scheduler.util.PaymentStartNonInteractiveUtils;
import it.eng.dome.payment.scheduler.util.ProviderType;
import it.eng.dome.tmforum.tmf637.v4.ApiException;
import it.eng.dome.tmforum.tmf637.v4.model.Characteristic;
import it.eng.dome.tmforum.tmf637.v4.model.Product;
import it.eng.dome.tmforum.tmf637.v4.model.RelatedParty;
import it.eng.dome.tmforum.tmf678.v4.model.AppliedCustomerBillingRate;


@Component(value = "paymentService")
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class PaymentService {

	private final Logger logger = LoggerFactory.getLogger(PaymentService.class);
	private final static String CONCAT_KEY = "|";
	
	private final ProductInventoryApis productInventoryApis;
	private final AppliedCustomerBillRateApis appliedCustomerBillRateApis;
	
	@Autowired
	private StartPayment payment;

	@Autowired
	private VCVerifier vcverifier;
	
	@Autowired
	private TMForumService tmforumService;
	
	
	public PaymentService(ProductInventoryApis productInventoryApis, AppliedCustomerBillRateApis appliedCustomerBillRateApis) {
		this.productInventoryApis = productInventoryApis;
		this.appliedCustomerBillRateApis = appliedCustomerBillRateApis;
	}

	
	/**
	 * Main method called by PaymentScheduler service (in the PaymentTask class)
	 */
	public String payments() {
		logger.info("Starting payments at {}", OffsetDateTime.now().format(PaymentDateUtils.formatter));
		
		// add filter for AppliedCustomerBillingRate 
		Map<String, String> filter = new HashMap<String, String>();
		filter.put("isBilled", "false"); // isBilled = false
		//filter.put("rateType", "recurring"); // type = recurring		

//		List<AppliedCustomerBillingRate> appliedList = appliedApis.getAllAppliedCustomerBillingRates(null, filter);
		
		//TODO to improve - just fixing the error!!!!
		List<AppliedCustomerBillingRate> appliedList = FetchUtils.streamAll(
			appliedCustomerBillRateApis::listAppliedCustomerBillingRates,   // method reference
	        null,                                     		// fields
	        filter,               							// filter
	        100                                       		// pageSize
		).toList(); 
		
		return payments(appliedList);
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
			logger.debug("Number of applied ready for billing: {}", appliedList.size());
			
			//aggregate - payment time
			Map<String, List<AppliedCustomerBillingRate>> aggregates = new HashMap<String, List<AppliedCustomerBillingRate>>();
			List<String> noComplaintBills = new ArrayList<String>();
	
			for (AppliedCustomerBillingRate applied : appliedList) {
				//logger.debug("AppliedCustomerBillingRate payload to be billed: {}", applied.toJson());
								
				// check if exist the product
				if(applied.getProduct() != null) {
					logger.info("AppliedId {} must be billed", applied.getId());
					
					// SET keys with multiple attributes for the map<> aggregates
		        	String endDate = getEndDate(applied);
		        	String paymentPreAuthorizationExternalId = getPaymentPreAuthorizationExternalId(applied.getProduct().getId());
		        	
		        	if (paymentPreAuthorizationExternalId != null) {
		        		// key = preAuthorizationId + endDate => i.e. 9d4fca3b-4bfa-4dba-a09f-348b8d504e44|2025-05-05
			        	String key = paymentPreAuthorizationExternalId + CONCAT_KEY + endDate;

			        	// add in the ArrayList the AppliedCustomerBillingRate
			        	aggregates.computeIfAbsent(key, k -> new ArrayList<>()).add(applied);
		        	} else {
		        		noComplaintBills.add(applied.getId());
		        		logger.warn("Couldn't found the paymentPreAuthorizationExternalId attribute from ProductCharacteristic");
		        	}
				} else {	
					noComplaintBills.add(applied.getId());
					 // product attribute is required to get the paymentPreAuthorizationExternal from ProductCharacteristic
					logger.warn("Cannot found the product attribute for appliedId: {}", applied.getId());
					logger.warn("Product attribute is required to get the paymentPreAuthorizationExternal from ProductCharacteristic");
					
					logger.warn("The applied {} cannot be update", applied.getId());
				}
			}
		
			// payment
			logger.debug("Size of list applied aggregates to pay: {}", aggregates.size());
			int count = 0;
	        for (Entry<String, List<AppliedCustomerBillingRate>> entry : aggregates.entrySet()) {
	        	
	        	List<AppliedCustomerBillingRate> applied = entry.getValue();
	        	logger.debug("List applied aggregates[{}] - contains num of applied: {}", ++count, applied.size());
	        	
	        	String key = entry.getKey();
	        		        	
	        	// retrieve the paymentPreAuthorizationExternalId from key
	        	String paymentPreAuthorizationExternalId = key.substring(0, key.indexOf(CONCAT_KEY));
	        	
	        	String customerOrganizationId = getCustomerOrganizationId(applied.get(0).getProduct().getId());
	        	
	        	if (customerOrganizationId != null) {
	        		// build the payload for EG Payment Gateway
		        	PaymentStartNonInteractive payment = getPayloadStartNonInteractive(paymentPreAuthorizationExternalId, customerOrganizationId, applied);
		        	
		        	if (executePayment(payment, applied)) {
		        		num = num + applied.size();
		        	}
	        	}else {
	        		logger.error("Cannot build the Payment payload. The customerOrganizationId is null");
	        	}
	        }
	        
			// report not complaint bills - applied not billed yet
	        if (!noComplaintBills.isEmpty()) {
	        	logger.info("Number of non-complaint applied: {}", noComplaintBills.size());
	        	logger.info("The following applied cannot be billed: {}", String.join(", ", noComplaintBills));
	        }
			
	        logger.info("The payment process scheduled has been terminated at {}", OffsetDateTime.now().format(PaymentDateUtils.formatter));
			
		}else {
			logger.warn("The list of AppliedCustomerBillingRate to be billed is empty");
		}
		
		String response = "Number of payments executed: " + num;
		logger.info(response);
		return response;
	}
	
	/*
	 * Create the payload for StartNonInteractive call
	 */
	private PaymentStartNonInteractive getPayloadStartNonInteractive(String paymentPreAuthorizationExternalId, String customerOrganizationId, List<AppliedCustomerBillingRate> applied) {
				
		PaymentStartNonInteractive payment = PaymentStartNonInteractiveUtils.getPaymentStartNonInteractive(paymentPreAuthorizationExternalId, customerOrganizationId);
		
		for (AppliedCustomerBillingRate apply : applied) {
			//logger.debug("AppliedCustomerBillingRate payload: {}", apply.toJson());
			
			// Let's suppose applyId and productId <> NULL 
			String productProviderExternalId = getProductProviderExternalId(apply.getProduct().getId());
			
			float amount = 0;
			if (apply.getTaxExcludedAmount().getValue() != null) {
				amount = apply.getTaxIncludedAmount().getValue().floatValue();
			}
			
			if (productProviderExternalId != null) {
			
				PaymentItem paymentItem = new PaymentItem();	        	
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
				
				String paymentExternalId=egpayment.getPaymentExternalId();
				logger.debug("PaymentExternalId: {}", paymentExternalId);
				
				List<Payout> payoutList = egpayment.getPayoutList();
				Map<String, AppliedCustomerBillingRate> applyMap = applied.stream().collect(Collectors.toMap(AppliedCustomerBillingRate::getId, Function.identity()));
				
				logger.info("Updating {} applied in the PayoutList", payoutList.size());
				
				for (Payout payout : payoutList) {
					logger.info("Get payout status: {}", payout.getState());
				
					// Get the status
					Status statusEnum = Status.valueOf(payout.getState().toUpperCase());
					String applyId = payout.getPaymentItemExternalId();
										
					if (applyId != null) {
						logger.info("Get applyId: {}", applyId);
					
						AppliedCustomerBillingRate appliedCustomerBillingRate = applyMap.get(applyId);
						
						logger.info("Handling payment status: {}", statusEnum.name());
						handlePaymentStatus(statusEnum, appliedCustomerBillingRate, paymentExternalId);
						
					} else {
						//TODO - Manage if cannot find the paymentItemExternalId -> Payment state = PROCESSED => how it must update the appliedCustomerBillingRate
												
						logger.warn("Payout: {}", payout.toJson());
						logger.warn("Payout state: {}", statusEnum);
						
						// cannot manage the appliedCustomerBillingRate. No applyId (paymentItemExternalId) provided
						logger.error("Cannot find the paymentItemExternalId from Payout. The paymentItemExternalId is the applyId");
					}
				}
				
				return true;
			}else {
				logger.error("Error: EG Payment Gateway couldn't pay the applied: {}", applied.stream().map(AppliedCustomerBillingRate::getId).collect(Collectors.joining(",")));
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
		
		final String PAYMENT_PRE_AUTHORIZATION = "paymentPreAuthorization";

		if (productId != null) {

			try {
				Product product = productInventoryApis.getProduct(productId, null);
				if (product != null) {
					List<Characteristic> prodChars = product.getProductCharacteristic();

					if (prodChars != null && !prodChars.isEmpty()) {
						for (Characteristic c : prodChars) {
							if (c.getName().trim().startsWith(PAYMENT_PRE_AUTHORIZATION)) {		
								logger.info("Found the attribute {} in the ProductCharacteristic", c.getName());
								if (c.getValue() != null) {								
									logger.info("Found the {} value: {}", c.getName(), c.getValue().toString());
									return c.getValue().toString();
								} else {
									logger.error("The {} is null from product {}", c.getName(), productId);
									return null;
								}
							}
						}
					}				
				}	
			} catch (ApiException e) {
				logger.error("Error: {}", e.getMessage());
				return null;
			}
						
		}

		logger.error("Couldn't retrieve the paymentPreAuthorizationExternalId from product {}", productId);
		return null;
	}
	
	/*
	 * Retrieve the ProductProviderExternalId from the relatedParty - role => ProviderType 
	 */
	private String getProductProviderExternalId(String productId) {

		if (productId != null) {
			
			try {
				Product product = productInventoryApis.getProduct(productId, null);			
				if (product != null) {
					List<RelatedParty> parties = product.getRelatedParty();
					for (RelatedParty party : parties) {
						if (ProviderType.isValid(party.getRole())) {
							logger.debug("Retrieved productProviderExternalId: {}", party.getId());
							return party.getId(); 
						}
					}
				}
			} catch (ApiException e) {
				logger.error("Error: {}", e.getMessage());
				return null;
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
			try {
				Product product = productInventoryApis.getProduct(productId, null);
				if (product != null) {
					List<RelatedParty> parties = product.getRelatedParty();
					for (RelatedParty party : parties) {
						if (CustomerType.isValid(party.getRole())) {
							logger.debug("Retrieved customerOrganizationId: {}", party.getId());
							return party.getId(); 
						}
					}
				}
			} catch (ApiException e) {
				logger.error("Error: {}", e.getMessage());
				return null;
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
	
	private void handlePaymentStatus(Status status, AppliedCustomerBillingRate applied, String paymentExternalId) {
	    switch (status) {
	        case PROCESSED:
	            handleStatusProcessed(applied, paymentExternalId);
	            break;
	        case PENDING:
	            handleStatusPending(applied);
	            break;
	        case FAILED:
	            handleStatusFailed(applied);
	            break;
	    }
	}

	private void handleStatusProcessed(AppliedCustomerBillingRate applied, String paymentExternalId) {
				
		if (tmforumService.updateAppliedCustomerBillingRate(applied, paymentExternalId)) { // set isBilled = true and add CustomerBill (BillRef)
			logger.info("The appliedCustomerBillingRateId {} with type {} has been updated successfully", applied.getId(), applied.getType());
		} else {
			logger.error("Couldn't update appliedCustomerBillingRate {} in TMForum", applied.getId());
		}	
	}

	private void handleStatusPending(AppliedCustomerBillingRate applied) {
		
		if (tmforumService.setIsBilled(applied, true)) { // set isBilled = true
			logger.info("IsBilled has been updated successfully for the appliedCustomerBillingRateId {}", applied.getId());
		} else {
			logger.error("Couldn't set isBilled for the appliedCustomerBillingRate {} in TMForum", applied.getId());
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