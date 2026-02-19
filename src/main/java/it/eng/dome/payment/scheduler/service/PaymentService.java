package it.eng.dome.payment.scheduler.service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import it.eng.dome.brokerage.api.AppliedCustomerBillRateApis;
import it.eng.dome.brokerage.api.CustomerBillApis;
import it.eng.dome.brokerage.api.ProductInventoryApis;
import it.eng.dome.brokerage.api.fetch.FetchUtils;
import it.eng.dome.payment.scheduler.dto.PaymentItem;
import it.eng.dome.payment.scheduler.dto.PaymentStartNonInteractive;
import it.eng.dome.payment.scheduler.model.EGPaymentResponse;
import it.eng.dome.payment.scheduler.model.EGPaymentResponse.Payout;
import it.eng.dome.payment.scheduler.util.PaymentDateUtils;
import it.eng.dome.payment.scheduler.util.PaymentStartNonInteractiveUtils;
import it.eng.dome.tmforum.tmf678.v4.model.CustomerBill;
import it.eng.dome.tmforum.tmf678.v4.model.StateValue;
import jakarta.validation.constraints.NotNull;


@Component(value = "paymentService")
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class PaymentService {

	private final Logger logger = LoggerFactory.getLogger(PaymentService.class);
	private final CustomerBillApis customerBillApis;
	
	@Autowired
	private StartPayment payment;

	@Autowired
	private VCVerifier vcverifier;
	
	@Autowired
	private TMForumService tmforumService;
	
	
	public PaymentService(ProductInventoryApis productInventoryApis, CustomerBillApis customerBillApis, AppliedCustomerBillRateApis appliedCustomerBillRateApis) {
		this.customerBillApis = customerBillApis;
	}

	
	/**
	 * Main method called by PaymentScheduler service (in the PaymentTask class)
	 */
	public String payments() {
		logger.info("Starting payments at {}", OffsetDateTime.now().format(PaymentDateUtils.formatter));
		
		// get CustomerBill(s) with state NEW or PARTIALLY_PAID
		List<CustomerBill> cbList = Stream.concat(
		        fetchByState(StateValue.NEW).stream(),
		        fetchByState(StateValue.PARTIALLY_PAID).stream()
		).toList();
		
		//payments(cbList);
		return payments(cbList);
	}
	
	/**
	 * Execute payments by using the aggregate feature from a list of bill
	 * 
	 * @param customerBills
	 * @return String - provide the number of payments 
	 */
	public String payments(List<CustomerBill> customerBills) {
		int num = 0;
		List<String> noComplaintBills = new ArrayList<String>();
		
		if (customerBills != null && !customerBills.isEmpty()) {
			logger.debug("Number of CustomerBill ready for billing: {}", customerBills.size());
			
			//aggregate CB by same paymentPreAuthorizationExternalId - payment time
			Map<String, List<CustomerBill>> aggregates = new HashMap<String, List<CustomerBill>>();
			
			for (CustomerBill cb : customerBills) {	
				// Retrieve the Product id associated to the CB through their ACBRs
				String productId=tmforumService.getProductIdOfCB(cb.getId());
				
				if(productId==null || productId.isBlank()) {
					logger.warn("Cannot found the Product ID associated to the CB: {}", cb.getId());
					logger.warn("Product attribute is required to get the paymentPreAuthorizationExternal from ProductCharacteristic");
					logger.warn("The CB {} cannot be payed skipped", cb.getId());
					noComplaintBills.add(cb.getId());
					continue;
				}
				
				logger.debug("Product ID {} asssociated to CB with ID {}", productId,cb.getId());
					
		        String paymentPreAuthorizationExternalId =tmforumService.getPaymentPreAuthorizationExternalId(productId);
		        
		        if(paymentPreAuthorizationExternalId==null || paymentPreAuthorizationExternalId.isBlank()) {
		        	logger.warn("Couldn't found the paymentPreAuthorizationExternalId attribute from ProductCharacteristic");
		        	logger.warn("The CB {} cannot be payed skipped", cb.getId());
		        	noComplaintBills.add(cb.getId());
		        	continue;
		        }
		        	
			     aggregates.computeIfAbsent(paymentPreAuthorizationExternalId, k -> new ArrayList<>()).add(cb);
			}
		
			// payment
			logger.debug("Size of aggregates CB to pay: {}", aggregates.size());
	        for (Entry<String, List<CustomerBill>> entry : aggregates.entrySet()) {
	        	
	        	List<CustomerBill> cbs = entry.getValue();
	        	logger.debug("Entry with paymentPreAuthorizationExternalId '{}' contains num of CBs: {}", entry.getKey(), cbs.size());
	        	
	        	PaymentStartNonInteractive payment = getPayloadStartNonInteractive(entry.getKey(),cbs);
	        	
	        	if(payment==null) {
	        		List<String> customerBillIds = customerBills.stream()
	        		        .map(CustomerBill::getId)
	        		        .collect(Collectors.toList());
	        		String idsForLog = customerBillIds.stream()
	        		        .collect(Collectors.joining(", ", "[", "]"));
	        	
	        		logger.error("Error generating Payment payload for entry with paymentPreAuthorizationExternalId {} and CBs {}", entry.getKey(),idsForLog);
	        		noComplaintBills.addAll(customerBillIds);
	        		continue;
	        	}
	        		
				if(executePayment(payment, cbs)) {
					logger.info("Payment executed for CBs {}", cbs.stream().map(CustomerBill::getId).collect(Collectors.joining(", ", "[", "]")));
					num = num + cbs.size();
				}
	        }
	        
			// report not complaint bills - cb not billed yet
	        if (!noComplaintBills.isEmpty()) {
	        	logger.debug("Number of non-complaint CBs: {}", noComplaintBills.size());
	        	logger.info("The following CBs cannot be billed: {}", String.join(", ", noComplaintBills));
	        }
			
	        logger.info("The payment process scheduled has been terminated at {}", OffsetDateTime.now().format(PaymentDateUtils.formatter));
			
		}
		
		String response = "Number of payments executed: " + num;
		return response;
	}
	
	/*
	 * Create the payload for StartNonInteractive call
	 */
	private PaymentStartNonInteractive getPayloadStartNonInteractive(String paymentPreAuthorizationExternalId, List<CustomerBill> cbs) {
				
		String productId= tmforumService.getProductIdOfCB(cbs.get(0).getId());
		String customerOrganizationId= tmforumService.getCustomerOrganizationId(productId);
		
		if(customerOrganizationId==null || customerOrganizationId.isBlank()) {
			logger.error("Cannot build the Payment payload. The customerOrganizationId is null for Product {} associated to CustomerBill {}", productId, cbs.get(0).getId());
			return null;
		}
		
		PaymentStartNonInteractive payment = PaymentStartNonInteractiveUtils.getPaymentStartNonInteractive(paymentPreAuthorizationExternalId, customerOrganizationId);
		
		for (CustomerBill cb : cbs) {
			String productProviderExternalId = tmforumService.getProductProviderExternalId(productId);
			
			if(productProviderExternalId==null || productProviderExternalId.isBlank()) {
				logger.error("Cannot build the Payment payload. The productProviderExternalId is null for Product {} associated to CustomerBill {}", productId, cbs.get(0).getId());
				return null;
			}
			
			// Generate PaymentItem
			PaymentItem paymentItem = new PaymentItem();	        	
        	paymentItem.setAmount(cb.getTaxIncludedAmount().getValue());
        	paymentItem.setCurrency(cb.getTaxIncludedAmount().getUnit());
        	paymentItem.setProductProviderExternalId(productProviderExternalId);
        	paymentItem.setRecurring(true);
        	paymentItem.setPaymentItemExternalId(cb.getId());
        	
        	Map<String, String> attrs = new HashMap<String, String>();
        	// attrs.put("additionalProp1", "data1"); // list of attrs if need
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
	 * @throws it.eng.dome.tmforum.tmf678.v4.ApiException 
	 */
	private boolean executePayment(PaymentStartNonInteractive paymentStartNonInteractive, List<CustomerBill> cbs){
		
		String token = vcverifier.getVCVerifierToken();
		if (token != null) {
			
			try {
				// Update State of CBs to "sent"
				tmforumService.updateCustomerBillsState(cbs, StateValue.SENT);
				EGPaymentResponse egpayment = payment.paymentNonInteractive(token, paymentStartNonInteractive);
				   
				if (egpayment != null) {
					
					String paymentExternalId=egpayment.getPaymentExternalId();
					//logger.debug("PaymentExternalId: {}", paymentExternalId);
					
					List<Payout> payoutList = egpayment.getPayoutList();
					//Map<String, CustomerBill> cbMap = cbs.stream().collect(Collectors.toMap(CustomerBill::getId, Function.identity()));
					
					logger.debug("Updating {} CB in the PayoutList for payment transaction with id {}", payoutList.size(), paymentExternalId);
					
					for (Payout payout : payoutList) {
						handlePaymentPayout(payout, paymentExternalId);
					}
					
					return true;
				}else {
					List<String> customerBillIds = cbs.stream()
	        		        .map(CustomerBill::getId)
	        		        .collect(Collectors.toList());
					logger.error("Error: EG Payment Gateway couldn't pay the CustomerBill: {}", customerBillIds.stream().collect(Collectors.joining(",")));
					tmforumService.restoreCustomerBillsState(customerBillIds);
					return false;
				}
			}catch (it.eng.dome.tmforum.tmf678.v4.ApiException e){
				logger.error("Error executing payment: {}",e.getMessage());
				return false;
			}
			
		} else {
			logger.error("Error to get the Token from VC Verfier Server");
			return false;
		}
	}
	
		
	private enum Status {
	    PROCESSED,
	    FAILED,
	    PENDING
	}
	
	
	private void handlePaymentPayout(@NotNull Payout payout, @NotNull String paymentExternalId) throws it.eng.dome.tmforum.tmf678.v4.ApiException{
		
		// Get the status
		Status statusEnum = Status.valueOf(payout.getState().toUpperCase());
		// Get the status
		String cbId = payout.getPaymentItemExternalId();
		logger.info("Handling payment status {} for CustomeBill {}",statusEnum, cbId);
		
	    switch (statusEnum) {
	        case PROCESSED:
	            handleStatusProcessed(payout, paymentExternalId);
	            break;
	        case PENDING:
	            handleStatusPending(cbId);
	            break;
	        case FAILED:
	            handleStatusFailed(cbId);
	            break;
	    }
	}
	
	private void handleStatusProcessed(Payout payout, String paymentExternalId) throws it.eng.dome.tmforum.tmf678.v4.ApiException {
		
		tmforumService.updatePaidCustomerBill(payout,paymentExternalId);
		
		logger.info("The CustomeBill {} with status {} has been updated successfully", payout.getPaymentItemExternalId(),payout.getState());
	}

	private void handleStatusPending(String cbId) {
		logger.info("Status Pending: no update for the CustomerBill {}", cbId);
	}
	
	private void handleStatusFailed(String cbId) throws it.eng.dome.tmforum.tmf678.v4.ApiException {
		logger.info("Status Failed: the state of CustomerBill {} is restored to new", cbId);
		if(tmforumService.isCustomerBillPartiallyPaid(cbId))
			tmforumService.updateCustomerBillState(cbId, StateValue.PARTIALLY_PAID);
		else
			tmforumService.updateCustomerBillState(cbId, StateValue.NEW);
	}
	
	private List<CustomerBill> fetchByState(StateValue state) {
		logger.debug("Fetching CustomerBill(s) by state {}",state.getValue());
	    Map<String, String> filter = Map.of("state", state.getValue()); 
	    
	    List<CustomerBill> filteredList=FetchUtils.streamAll(customerBillApis::listCustomerBills, null, filter, 100).toList();
	    logger.debug("Number of retrieved CustomerBill(s): {}",filteredList.size());
	    
	    return filteredList;
	}
}