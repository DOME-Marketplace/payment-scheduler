package it.eng.dome.payment.scheduler.service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import it.eng.dome.brokerage.api.AppliedCustomerBillRateApis;
import it.eng.dome.brokerage.api.CustomerBillApis;
import it.eng.dome.tmforum.tmf678.v4.ApiException;
import it.eng.dome.tmforum.tmf678.v4.model.AppliedCustomerBillingRate;
import it.eng.dome.tmforum.tmf678.v4.model.AppliedCustomerBillingRateUpdate;
import it.eng.dome.tmforum.tmf678.v4.model.AppliedPayment;
import it.eng.dome.tmforum.tmf678.v4.model.BillRef;
import it.eng.dome.tmforum.tmf678.v4.model.CustomerBillCreate;
import it.eng.dome.tmforum.tmf678.v4.model.Money;
import it.eng.dome.tmforum.tmf678.v4.model.PaymentRef;
import it.eng.dome.tmforum.tmf678.v4.model.RelatedParty;
import it.eng.dome.tmforum.tmf678.v4.model.StateValue;

@Service
public class TMForumService {
	
	private static final Logger logger = LoggerFactory.getLogger(TMForumService.class);
	
	private final AppliedCustomerBillRateApis appliedCustomerBillRateApis;
	private final CustomerBillApis customerBillApis;
	
	public TMForumService(AppliedCustomerBillRateApis appliedCustomerBillRateApis, CustomerBillApis customerBillApis) {
		this.appliedCustomerBillRateApis = appliedCustomerBillRateApis;
		this.customerBillApis = customerBillApis;		
	}
	

	/**
	 * This method update the AppliedCustomerBillingRate setting isBilled attribute to true and creating a CustomerBill (BillRef)
	 * 
	 * @param appliedId
	 * @return
	 */
	public boolean addCustomerBill(String appliedId, String paymentExternalId) {
		logger.info("Add the CustomerBill for appliedId: {}", appliedId);

		try {
			AppliedCustomerBillingRate applied = appliedCustomerBillRateApis.getAppliedCustomerBillingRate(appliedId, null);
			
			if (applied != null) {
				return updateAppliedCustomerBillingRate(applied, paymentExternalId);
			}else {
				logger.info("Cannot found the applied with id: {} to add the CustomerBill", appliedId);
				return false;	
			}

		} catch (ApiException e) {
			logger.error("Error: {}", e.getMessage());
			return false;
		}
	}
	
	
	/**
	 * This method update the AppliedCustomerBillingRate setting isBilled attribute to true and creating a CustomerBill (BillRef)
	 * 
	 * @param applied
	 * @return
	 */
	public boolean updateAppliedCustomerBillingRate(AppliedCustomerBillingRate applied, String paymentExternalId) {
		logger.info("Update the AppliedCustomerBillingRate for id: {}", applied.getId());		
		
		logger.debug("Creating CustomerBill to set the BillRef in AppliedCustomerBillingRate");
		//create a new CustomerBill to set in the AppliedCustomerBillingRate (BillRef)
		CustomerBillCreate customerBill = new CustomerBillCreate();
		customerBill.setBillingAccount(applied.getBillingAccount());
		customerBill.setBillDate(OffsetDateTime.now());
		customerBill.setBillingPeriod(applied.getPeriodCoverage());
		customerBill.setState(StateValue.SETTLED);	
		customerBill.setTaxExcludedAmount(applied.getTaxExcludedAmount());
		customerBill.setTaxIncludedAmount(applied.getTaxIncludedAmount());
		
		// Set customerBill.amountDue
		// Assumption
		// When the CustomerBill has been created the bill has been successfully paid (all the amount due)
		// In the current implementation a CustomerBill is created for each ACBR
		// The amountDue is set to "0" (i.e., all the amount due has been paid)
		//
		Money amountDue=new Money();
		amountDue.setUnit("EUR");
		amountDue.setValue(0f);
		customerBill.setAmountDue(amountDue);
		
		// Set customerBill.appliedPayment
		// Assumption
		// The list of the appliedPayment is valorized with an aplliedPayment
		// The amount of the payment is set to the taxIncluededAmount
		// The reference to the payment is set to the paymentExternalId
		List<AppliedPayment> appliedPayments=new ArrayList<AppliedPayment>();
		AppliedPayment appliedPayment=new AppliedPayment();
		appliedPayment.setAppliedAmount(applied.getTaxIncludedAmount());
		PaymentRef paymentRef=new PaymentRef();
		paymentRef.setId(paymentExternalId);
		appliedPayment.setPayment(paymentRef);
		appliedPayments.add(appliedPayment);
		customerBill.setAppliedPayment(appliedPayments);
		
		//check on RelatedParty if is null
		List<RelatedParty> parties = new ArrayList<RelatedParty>();
		if (applied.getRelatedParty() != null) {
			logger.warn("Get num of RelatedParty from applied: {}", applied.getRelatedParty().size());
			try {
				parties = appliedCustomerBillRateApis.getAppliedCustomerBillingRate(applied.getId(), null).getRelatedParty();
			} catch (ApiException e) {
				logger.error("Cannot be found the RelatedParty for appliedId: {}", applied.getId());
				//return false;
			}
		}
		
		//FIXME - applied list cannot provide all parties, but just one!!!
		customerBill.setRelatedParty(/*applied.getRelatedParty()*/parties);

		try {
			String idCustomerBill = customerBillApis.createCustomerBill(customerBill);
		
			if (idCustomerBill != null) {
				// Creating the BillRef		
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
				appliedCustomerBillRateApis.updateAppliedCustomerBillingRate(applied.getId(), update);
				return true;
			} 
		} catch (ApiException e) {
			logger.error("CustomerBill cannot be create");
			return false;
		}
		return false;
	}
	
	/**
	 * This method set isBilled attribute as parameter billed for the AppliedCustomerBillingRateId
	 * 
	 * @param appliedId
	 * @param billed
	 * @return
	 */
	public boolean setIsBilled(String appliedId, boolean billed) {

		logger.info("Set isBilled = {} for appliedId: {}", billed, appliedId);		
		
		try {
			AppliedCustomerBillingRate applied = appliedCustomerBillRateApis.getAppliedCustomerBillingRate(appliedId, null);
	
			if (applied != null) {
				return setIsBilled(applied, billed);
			}else {
				logger.info("Cannot found the applied with id: {} to set isBilled attribute", appliedId);
				return false;	
			}
		} catch (ApiException e) {
			logger.error("Error: {}", e.getMessage());
			return false;
		}
	}
	
	
	/**
	 * This method set isBilled attribute as parameter billed in the AppliedCustomerBillingRate applied
	 * 
	 * @param applied
	 * @param billed
	 * @return
	 */
	public boolean setIsBilled(AppliedCustomerBillingRate applied, boolean billed) {
		logger.info("Setting isBilled to {}", billed);		
			
		// create AppliedCustomerBillingRateUpdate object to update the AppliedCustomerBillingRate
		logger.debug("Creating an AppliedCustomerBillingRateUpdate object to set isBilled for the AppliedCustomerBillingRate with id: {}", applied.getId());	
		AppliedCustomerBillingRateUpdate update = new AppliedCustomerBillingRateUpdate();
		update.setIsBilled(billed);

		if (!billed) { // if isBilled = false -> need to reset the BillingAccount
			logger.debug("Required to reset the BillingAccount for AppliedCustomerBillingRateUpdate if isBilled = {}", billed);
			update.setBillingAccount(applied.getBillingAccount());
		}
				
		logger.debug("Payload of AppliedCustomerBillingRateUpdate: {}", applied.toJson());	

		try {
			appliedCustomerBillRateApis.updateAppliedCustomerBillingRate(applied.getId(), update);
			return true;
		} catch (ApiException e) {
			logger.error("Error: {}", e.getMessage());
			return false;
		}
	}

}
