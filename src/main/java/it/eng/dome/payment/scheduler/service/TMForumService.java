package it.eng.dome.payment.scheduler.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import it.eng.dome.brokerage.api.AppliedCustomerBillRateApis;
import it.eng.dome.brokerage.api.CustomerBillApis;
import it.eng.dome.brokerage.api.ProductInventoryApis;
import it.eng.dome.brokerage.api.fetch.FetchUtils;
import it.eng.dome.payment.scheduler.config.AppConfig;
import it.eng.dome.payment.scheduler.model.EGPaymentResponse.Payout;
import it.eng.dome.payment.scheduler.util.CustomerType;
import it.eng.dome.payment.scheduler.util.ProviderType;
import it.eng.dome.tmforum.tmf637.v4.ApiException;
import it.eng.dome.tmforum.tmf637.v4.model.Characteristic;
import it.eng.dome.tmforum.tmf637.v4.model.Product;
import it.eng.dome.tmforum.tmf637.v4.model.RelatedParty;
import it.eng.dome.tmforum.tmf678.v4.model.AppliedCustomerBillingRate;
import it.eng.dome.tmforum.tmf678.v4.model.AppliedPayment;
import it.eng.dome.tmforum.tmf678.v4.model.CustomerBill;
import it.eng.dome.tmforum.tmf678.v4.model.CustomerBillUpdate;
import it.eng.dome.tmforum.tmf678.v4.model.Money;
import it.eng.dome.tmforum.tmf678.v4.model.PaymentRef;
import it.eng.dome.tmforum.tmf678.v4.model.StateValue;
import jakarta.validation.constraints.NotNull;

@Service
public class TMForumService {

    //private final AppConfig appConfig;

	private static final Logger logger = LoggerFactory.getLogger(TMForumService.class);

	private final AppliedCustomerBillRateApis appliedCustomerBillRateApis;
	private final CustomerBillApis customerBillApis;
	private final ProductInventoryApis productInventoryApis;

	public TMForumService(AppliedCustomerBillRateApis appliedCustomerBillRateApis, CustomerBillApis customerBillApis, ProductInventoryApis productInventoryApis, AppConfig appConfig) {
		this.appliedCustomerBillRateApis = appliedCustomerBillRateApis;
		this.customerBillApis = customerBillApis;
		this.productInventoryApis= productInventoryApis;
		//this.appConfig = appConfig;
	}
	
	/**
	 * Updates the specified {@link CustomerBill} with the specified {@link StateValue}
	 * @param cbId the identifier of the CustomerBill to update
	 * @param state the new state 
	 * @throws ApiException if an error occurs during the update of the CustomerBill in TMForum
	 */
	public void updateCustomerBillState(@NotNull String cbId, @NotNull StateValue state) throws it.eng.dome.tmforum.tmf678.v4.ApiException {

		CustomerBillUpdate update = new CustomerBillUpdate();
		update.setState(state);

		customerBillApis.updateCustomerBill(cbId, update);
		logger.debug("Updated CustomerBill with id: {} with state '{}' to the new state '{}'", cbId, state.getValue(), update.getState().getValue());
	}

	/**
	 * Updates the {@link CustomerBill} in the specified list with the specified {@link StateValue}
	 * @param cbs the CustomerBill(s) to update
	 * @param state the new state 
	 * @throws ApiException if an error occurs during the update of the CustomerBill in TMForum
	 */
	public void updateCustomerBillsState(@NotNull List<CustomerBill> cbs, @NotNull StateValue state) throws it.eng.dome.tmforum.tmf678.v4.ApiException {

		for (CustomerBill cb : cbs) {
			this.updateCustomerBillState(cb.getId(), state);
		}
	}
	
	public void restoreCustomerBillsState(@NotNull List<String> cbIds) throws it.eng.dome.tmforum.tmf678.v4.ApiException {
		for (String cbId:cbIds) {
			if(this.isCustomerBillPartiallyPaid(cbId)) {
				updateCustomerBillState(cbId, StateValue.PARTIALLY_PAID);
				logger.debug("Restore CB {} to state PARTIALLY_PAID", cbId);
			}
			else {
				updateCustomerBillState(cbId, StateValue.NEW);
				logger.debug("Restore CB {} to state NEW", cbId);
			}
		}
	}
	
	public boolean isCustomerBillPartiallyPaid(@NotNull String cbId) throws it.eng.dome.tmforum.tmf678.v4.ApiException {
		CustomerBill cb=customerBillApis.getCustomerBill(cbId, null);
		
		// If the total amount to paid is major to the remaining amount the CB has been partially paid 
		if(cb.getTaxIncludedAmount().getValue()>cb.getRemainingAmount().getValue())
			return true;
		else
			return false;
		
	}
	
	/**
	 * Updates the CustomerBill associated to the specified {@link Payout}
	 * 
	 * @param payout the {@link Payout} with indication of the paid CustomerBill
	 * @param paymentExternalId the identifier of the payment transaction
	 * @throws it.eng.dome.tmforum.tmf678.v4.ApiException If an error occurs during the update of the CustomerBill
	 */
	public void updatePaidCustomerBill(@NotNull Payout payout, @NotNull String paymentExternalId) throws it.eng.dome.tmforum.tmf678.v4.ApiException{
		
		CustomerBill cb=customerBillApis.getCustomerBill(payout.getPaymentItemExternalId(), null);

		float amountPaid= payout.getAmount();
		
		if(amountPaid<=0) {
			logger.warn("The amount paied for CustomerBill {} in the payment transaction {} is minor or equals to zero",cb.getId(), paymentExternalId);
			
			if(this.isCustomerBillPartiallyPaid(cb.getId()))
				this.updateCustomerBillState(cb.getId(), StateValue.PARTIALLY_PAID);
			else
				this.updateCustomerBillState(cb.getId(), StateValue.NEW);
			
		}else {
			CustomerBillUpdate update = new CustomerBillUpdate();
			
			// Update admountDue
			float updatedAmountDue =
			        BigDecimal.valueOf(cb.getAmountDue().getValue())
			                .subtract(BigDecimal.valueOf(amountPaid))
			                .floatValue();
			Money amountDueMoney=new Money();
			amountDueMoney.setValue(updatedAmountDue);
			amountDueMoney.setUnit(cb.getAmountDue().getUnit());
			update.setAmountDue(amountDueMoney);
			
			// Update remainingAmount
			float updatedRemainingAmount =
			        BigDecimal.valueOf(cb.getRemainingAmount().getValue())
			                .subtract(BigDecimal.valueOf(amountPaid))
			                .floatValue();
			Money remainingAmountMoney=new Money();
			remainingAmountMoney.setValue(updatedRemainingAmount);
			remainingAmountMoney.setUnit(cb.getRemainingAmount().getUnit());
			update.setRemainingAmount(remainingAmountMoney);
			
			// Update AppliedPayment
			List<AppliedPayment> appliedPayments;
			if(cb.getAppliedPayment()==null)
				appliedPayments=new ArrayList<AppliedPayment>();
			else {
				appliedPayments=cb.getAppliedPayment();
			}
			
			AppliedPayment payment=new AppliedPayment();
			PaymentRef paymentRef=new PaymentRef();
			paymentRef.setId(paymentExternalId);
			payment.setPayment(paymentRef);
			Money appliedAmount=new Money();
			appliedAmount.setValue(amountPaid);
			appliedAmount.setUnit(payout.getCurrency());
			
			payment.setAppliedAmount(appliedAmount);
			appliedPayments.add(payment);
			update.setAppliedPayment(appliedPayments);
			
			if(updatedRemainingAmount>0)
				update.setState(StateValue.PARTIALLY_PAID);
			else
				update.setState(StateValue.SETTLED);
			
			customerBillApis.updateCustomerBill(cb.getId(), update);
			logger.debug("CB with id {} has been updated successful to state {}",update.getState().getValue());

		}
	}
	
	/**
	 * FIXME
	 * Updates the specified paid {@link CustomerBill} after receiving a SUCCESSFUL notification from the Payment Service
	 * TO FIX: At the moment the Payment Service doesn't provide in the notification the amount paid, therefore we assume that partial payments are not allowed
	 * We consider that all the amount due has been paid.   
	 * 
	 * @param cbIds the identifier of the {@link CustomerBill} to update
	 * @param paymentExternalId the identifier of the payment transaction
	 * @throws it.eng.dome.tmforum.tmf678.v4.ApiException If an error occurs during the update of the CustomerBill
	 */
	public void updatePaymentSuccessfulNotification(@NotNull List<String> cbIds, @NotNull String paymentExternalId) throws it.eng.dome.tmforum.tmf678.v4.ApiException{
		
		for(String cbId: cbIds) {
			CustomerBill cb=customerBillApis.getCustomerBill(cbId, null);
			
			// We assume if a successful notification is received al the amountDue has been paid (no partial payments)
			float amountPaid= cb.getAmountDue().getValue();
			
			CustomerBillUpdate update = new CustomerBillUpdate();
				
			// Update admountDue to 0
			float updatedAmountDue = 0f;

			Money amountDueMoney=new Money();
			amountDueMoney.setValue(updatedAmountDue);
			amountDueMoney.setUnit(cb.getAmountDue().getUnit());
			update.setAmountDue(amountDueMoney);
				
			// Update remainingAmount to 0
			float updatedRemainingAmount = 0f;

			Money remainingAmountMoney=new Money();
			remainingAmountMoney.setValue(updatedRemainingAmount);
			remainingAmountMoney.setUnit(cb.getRemainingAmount().getUnit());
			update.setRemainingAmount(remainingAmountMoney);
				
			// Update AppliedPayment
			List<AppliedPayment> appliedPayments;
			if(cb.getAppliedPayment()==null)
				appliedPayments=new ArrayList<AppliedPayment>();
			else {
				appliedPayments=cb.getAppliedPayment();
			}
			
			AppliedPayment payment=new AppliedPayment();
			PaymentRef paymentRef=new PaymentRef();
			paymentRef.setId(paymentExternalId);
			payment.setPayment(paymentRef);
			Money appliedAmount=new Money();
			appliedAmount.setValue(amountPaid);
			appliedAmount.setUnit(cb.getAmountDue().getUnit());
			
			payment.setAppliedAmount(appliedAmount);
			appliedPayments.add(payment);
			update.setAppliedPayment(appliedPayments);
			

			update.setState(StateValue.SETTLED);
			
			customerBillApis.updateCustomerBill(cb.getId(), update);
			logger.debug("CB with id {} has been updated successful to state {}",update.getState().getValue());

		}
	}
	
	/**
	 * Retrieves the identifier of the {@link Product} associated to the specified {@link CustomerBill} through its {@link AppliedCustomerBillingRate}
	 * 
	 * @param cbId the identifier of the CustomerBill
	 * @return the identifier of the Product associated to the CustomeBill, null if not found
	 */
	public String getProductIdOfCB(@NotNull String cbId) {
		
		 Map<String, String> filter = new HashMap<>();
        filter.put("bill.id", cbId);
        
        AppliedCustomerBillingRate acbr =
       		    FetchUtils.streamAll(
       		    		appliedCustomerBillRateApis::listAppliedCustomerBillingRates,    // method reference
       	                 null,     // fields
       	                 filter,   // filter
       	                100       // pageSize
       	         ).findFirst()
       		      .orElse(null);
        
        if(acbr==null) {
       	 logger.info("No ACBR found for Customer Bill with id {}: ", cbId);
       	 return null;
        }else {
       	 if(acbr.getProduct()!=null) {
       		 return acbr.getProduct().getId();
       	 }else {
       		 logger.info("No ProductRef present in the ACBR with id {}: ", acbr.getId());
           	 return null;
       	 }
        }
	}
	

	/**
	 * Retrieves the identifier of the Organization associated to the specified Product's identifier with role Buyer
	 * @param productId the identifier of the {@link Product}
	 * @return the identifier of the Organization with role Buyer, null if not found
	 */
	public String getCustomerOrganizationId(@NotNull String productId){
		
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
	
	/**
	 * Retrieves the paymentPreAuthorizationExternalId from the productCharacteristic of the specified Product's identifier
	 * @param productId the identifier of the {@link Product}
	 * @return the paymentPreAuthorizationExternalId of the specified Product, null if not found
	 */
	public String getPaymentPreAuthorizationExternalId(@NotNull String productId) {
		
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
	
	/**
	 * Retrieves the identifier of the Organization associated to the specified Product's identifier with role Seller
	 * @param productId the identifier of the {@link Product}
	 * @return the identifier of the Organization with role Seller, null if not found
	 */
	public String getProductProviderExternalId(@NotNull String productId) {

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

}
