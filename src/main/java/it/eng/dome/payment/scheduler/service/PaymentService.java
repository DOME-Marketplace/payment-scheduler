package it.eng.dome.payment.scheduler.service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import it.eng.dome.payment.scheduler.dto.PaymentDTO;
import it.eng.dome.payment.scheduler.dto.PaymentDTO.PaymentItem;
import it.eng.dome.payment.scheduler.tmf.TmfApiFactory;
import it.eng.dome.tmforum.tmf678.v4.ApiException;
import it.eng.dome.tmforum.tmf678.v4.api.AppliedCustomerBillingRateApi;
import it.eng.dome.tmforum.tmf678.v4.api.CustomerBillExtensionApi;
import it.eng.dome.tmforum.tmf678.v4.model.AppliedCustomerBillingRate;
import it.eng.dome.tmforum.tmf678.v4.model.AppliedCustomerBillingRateCreate;
import it.eng.dome.tmforum.tmf678.v4.model.AppliedCustomerBillingRateUpdate;
import it.eng.dome.tmforum.tmf678.v4.model.BillRef;
import it.eng.dome.tmforum.tmf678.v4.model.CustomerBill;
import it.eng.dome.tmforum.tmf678.v4.model.CustomerBillCreate;

@Component(value = "paymentService")
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class PaymentService implements InitializingBean {

	private final Logger logger = LoggerFactory.getLogger(PaymentService.class);
	private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy");

	@Autowired
	private TmfApiFactory tmfApiFactory;

	private AppliedCustomerBillingRateApi appliedCustomerBillingRate;
	private CustomerBillExtensionApi customerBillExtension;
	//private CustomerBillApi customerBill;

	@Override
	public void afterPropertiesSet() throws Exception {
		appliedCustomerBillingRate = new AppliedCustomerBillingRateApi(tmfApiFactory.getTMF678CustomerBillApiClient());
		customerBillExtension = new CustomerBillExtensionApi(tmfApiFactory.getTMF678CustomerBillApiClient());
		//customerBill = new CustomerBillApi(tmfApiFactory.getTMF678CustomerBillApiClient());
	}
	
	public void payments(OffsetDateTime now) {
		logger.info("Starting payments at {}", now.format(formatter));
		
		try {
			List<AppliedCustomerBillingRate> appliedList = appliedCustomerBillingRate.listAppliedCustomerBillingRate(null, null, null);
			logger.debug("Number of AppliedCustomerBillingRate found: {}", appliedList.size());
			executePayments(appliedList.toArray(new AppliedCustomerBillingRate[0]));
		} catch (ApiException e) {
			logger.debug("Error to get AppliedCustomerBillingRate - {}", e.getMessage());
		}
	}
	
	public String executePayments(AppliedCustomerBillingRate[] appliedCustomerBillingRates) {
		logger.info("Execute Payments for {} appliedCustomerBillingRate received", appliedCustomerBillingRates.length);
		int num = 0;
		
		for (AppliedCustomerBillingRate appliedCustomerBillingRate : appliedCustomerBillingRates) {
			// filter appliedCustomerBillingRates with isBilled a false -> need to be paid
			if (!appliedCustomerBillingRate.getIsBilled()) {
				logger.debug("AppliedCustomerBillingRate needs to be paid - id: {}", appliedCustomerBillingRate.getId());
				
				//TODO prepare the payload for the payment
				String payload = getPaymentPayload();
				
				if (callPaymentEG(payload)) {
					
					//TODO update AppliedCustomerBillingRate and save Payment in TMForum				
					if (updateAppliedCustomerBillingRate(appliedCustomerBillingRate)) {
						num++;
					}
					
				}else {
					logger.warn("There was a problem with the payment for appliedCustomerBillingRateId: {}", appliedCustomerBillingRate.getId());
				}
				
			}else {
				logger.debug("AppliedCustomerBillingRate already paid - id: {}", appliedCustomerBillingRate.getId());
			}
		}
		String response = "Number of payments executed: " + num;
		logger.info(response);
		return response;
	}
		
	private boolean updateAppliedCustomerBillingRate(AppliedCustomerBillingRate applied) {
		logger.info("Update to isBilled = true for AppliedCustomerBillingRateId: {}", applied.getId());
				
		AppliedCustomerBillingRateUpdate update = new AppliedCustomerBillingRateUpdate();
		update.setIsBilled(true);
				
		//create CustomerBill
		//String billingAccountId = applied.getBillingAccount().getId();
		CustomerBillCreate customerBill = new CustomerBillCreate();
		customerBill.setBillingAccount(applied.getBillingAccount());
		
		//customerBill.setRelatedParty(getRelatedParty(billingAccountId));
		customerBill.setAmountDue(applied.getTaxIncludedAmount());
		
		String idCustomerBill = saveCustomerBill(customerBill);
		if (idCustomerBill != null) {
			
			BillRef bill = new BillRef();
			bill.setId(idCustomerBill);
			bill.setHref(idCustomerBill);
			update.setBill(bill);
			
			logger.info("Payload for updateAppliedCustomerBillingRate: {}",update.toJson());
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
	
	private void getRelatedParty(String billingAccountId) {
		//???
	}
	
	/*
	public List<String> saveBill(AppliedCustomerBillingRate... bills) {
		logger.info("Saving the bill ...");
		List<String> ids = new ArrayList<String>();

		try {
			for (AppliedCustomerBillingRate bill : bills) {

				AppliedCustomerBillingRateCreate createApply = AppliedCustomerBillingRateCreate.fromJson(bill.toJson());
				AppliedCustomerBillingRate created = appliedCustomerBillingRate.createAppliedCustomerBillingRate(createApply);
				logger.info("{}AppliedCustomerBillRate saved with id: {}", created.getId());
				ids.add(created.getId());
			}
		} catch (Exception e) {
			logger.warn("AppliedCustomerBillingRate not saved!");
			logger.error("Error: {}", e.getMessage());
		}

		return ids;
	}
	*/
/*
	public CustomerBill getCustomerBill(String id) {
		logger.info("Get CustomerBill by id: {}", id);
		try {
			return customerBill.retrieveCustomerBill(id, null);
		} catch (ApiException e) {
			logger.info("CustomerBill not found with: {}", id);
			logger.error("Error: {}", e.getMessage());
			return null;
		}
	}
*/	
	

	
	
	
	private boolean callPaymentEG(String payload) {
		logger.info("Payment via EG APIs");
		
		logger.debug("Payment payload to send EG APIs: {}", payload);
		//TODO call to EG APIs
		
		return true;
	}
	
	public List<String> savePaymentsTMForum(AppliedCustomerBillingRate... bills) {
		logger.info("Saving the bill ...");
		List<String> ids = new ArrayList<String>();

		try {
			for (AppliedCustomerBillingRate bill : bills) {

//				AppliedCustomerBillingRateCreate createApply = AppliedCustomerBillingRateCreate.fromJson(bill.toJson());
//				AppliedCustomerBillingRate created = appliedCustomerBillingRate.createAppliedCustomerBillingRate(createApply);
//				logger.info("{}AppliedCustomerBillRate saved with id: {}", created.getId());
//				ids.add(created.getId());
			}
		} catch (Exception e) {
			logger.info("AppliedCustomerBillingRate not saved!");
			logger.error("Error: {}", e.getMessage());
		}

		return ids;
	}
	
	private String getPaymentPayload() {
		//TODO payload dummy
		PaymentDTO paymenDto = new PaymentDTO();
		paymenDto.setExternalId("id-ext");
		paymenDto.setCustomerId("id-customer");
		paymenDto.setCustomerOrganizationId("org");
		paymenDto.setType("ONETIME");
		paymenDto.setInvoiceId("id-invoicing");
		paymenDto.setProcessSuccessUrl("url-ok");
		paymenDto.setProcessErrorUrl("url-error");
		
		PaymentItem paymentItem = paymenDto.new PaymentItem();
		paymentItem.setProductProviderId("product-id");
		paymentItem.setAmount(10);
		paymentItem.setCurrency("currency");
		paymentItem.setRecurring("recurring");
		paymentItem.setProductProviderSpecificData("{}");
		
		List<PaymentItem> paymentItems = new ArrayList<PaymentItem>();
		paymentItems.add(paymentItem);
		
		paymenDto.setPaymentItems(paymentItems);
/*
    "externalId": str(self._order.order_id),
    "customerId": self._order.customer_id, 
    "customerOrganizationId": str(self._order.owner_organization_id),
    "type": "ONETIME", # recurring payment type is not yet implemented in the Dpas API
    "invoiceId": "invoice id", # should be the order's invoice ID
    "paymentItems": [{
        "productProviderId": "1", # should be the product provider's ID
        "amount": str(total),
        "currency": current_curr,
        "recurring" : False,
        "productProviderSpecificData": {}
    }],
    "processSuccessUrl": return_url,
    "processErrorUrl": cancel_url		
 */
		return paymenDto.toJson();
	}
}
