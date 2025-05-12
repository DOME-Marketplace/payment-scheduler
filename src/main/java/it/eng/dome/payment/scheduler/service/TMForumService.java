package it.eng.dome.payment.scheduler.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.eng.dome.brokerage.api.AppliedCustomerBillRateApis;
import it.eng.dome.payment.scheduler.tmf.TmfApiFactory;
import it.eng.dome.tmforum.tmf678.v4.model.AppliedCustomerBillingRate;
import it.eng.dome.tmforum.tmf678.v4.model.AppliedCustomerBillingRateUpdate;
import it.eng.dome.tmforum.tmf678.v4.model.BillRef;
import it.eng.dome.tmforum.tmf678.v4.model.CustomerBillCreate;

@Service
public class TMForumService implements InitializingBean {
	
	private static final Logger logger = LoggerFactory.getLogger(TMForumService.class);
	
	@Autowired
	private TmfApiFactory tmfApiFactory;
	
	private AppliedCustomerBillRateApis appliedApis;
	
	@Override
	public void afterPropertiesSet() throws Exception {
		appliedApis = new AppliedCustomerBillRateApis(tmfApiFactory.getTMF678CustomerBillApiClient());
	}
	
	
	/**
	 * This method update the AppliedCustomerBillingRate setting isBilled attribute to true and creating a CustomerBill (BillRef)
	 * 
	 * @param appliedId
	 * @return
	 */
	public boolean addCustomerBill(String appliedId) {
		logger.info("Add the CustomerBill for appliedId: {}", appliedId);
		
		AppliedCustomerBillingRate applied = appliedApis.getAppliedCustomerBillingRate(appliedId, null);
		
		if (applied != null) {
			return updateAppliedCustomerBillingRate(applied);
		}else {
			logger.info("Cannot found the applied with id: {}", appliedId);
			return false;	
		}
		
	}
	
	
	/**
	 * This method update the AppliedCustomerBillingRate setting isBilled attribute to true and creating a CustomerBill (BillRef)
	 * 
	 * @param applied
	 * @return
	 */
	public boolean updateAppliedCustomerBillingRate(AppliedCustomerBillingRate applied) {
		logger.info("Update the AppliedCustomerBillingRate for id: {}", applied.getId());		
		
		logger.debug("Creating CustomerBill to set the BillRef in AppliedCustomerBillingRate");
		//create a new CustomerBill to set in the AppliedCustomerBillingRate (BillRef)
		CustomerBillCreate customerBill = new CustomerBillCreate();
		customerBill.setBillingAccount(applied.getBillingAccount());
		customerBill.setAmountDue(applied.getTaxIncludedAmount());
		//TODO verify if it needs other attributes
		
		String idCustomerBill = appliedApis.createCustomerBill(customerBill);
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

			return appliedApis.updateAppliedCustomerBillingRate(applied.getId(), update);
			
		} else {
			logger.error("Cannot be create the CustomerBill");
			return false;
		}
	}
	
	/**
	 * This method set isBilled attribute as parameter billed for the AppliedCustomerBillingRateId
	 * 
	 * @param appliedId
	 * @param billed
	 * @return
	 */
	public boolean setIsBilled(String appliedId, boolean billed) {

		logger.info("Set isBilled for appliedId: {}", appliedId);		
		AppliedCustomerBillingRate applied = appliedApis.getAppliedCustomerBillingRate(appliedId, null);
		return setIsBilled(applied, billed);
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

		return appliedApis.updateAppliedCustomerBillingRate(applied.getId(), update);
	}

}
