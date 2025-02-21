package it.eng.dome.payment.scheduler.service;

import java.time.OffsetDateTime;
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
import it.eng.dome.payment.scheduler.util.PaymentDateUtils;
import it.eng.dome.tmforum.tmf678.v4.ApiClient;
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
	
	@Autowired
	private TmfApiFactory tmfApiFactory;

	private AppliedCustomerBillingRateApi appliedCustomerBillingRate;
	private CustomerBillExtensionApi customerBillExtension;

	@Override
	public void afterPropertiesSet() throws Exception {
		ApiClient apiClientTMF678 = tmfApiFactory.getTMF678CustomerBillApiClient();
		appliedCustomerBillingRate = new AppliedCustomerBillingRateApi(apiClientTMF678);
		customerBillExtension = new CustomerBillExtensionApi(apiClientTMF678);
	}
	
	public void payments() {
		logger.info("Starting payments at {}", OffsetDateTime.now().format(PaymentDateUtils.formatter));
		
		try {
			List<AppliedCustomerBillingRate> appliedList = appliedCustomerBillingRate.listAppliedCustomerBillingRate(null, null, null);
			logger.debug("Number of AppliedCustomerBillingRate found: {}", appliedList.size());
			executePayments(appliedList.toArray(new AppliedCustomerBillingRate[0]));
		} catch (ApiException e) {
			logger.debug("Error to get AppliedCustomerBillingRate - {}", e.getMessage());
		}
	}
	
	public String executePayments(AppliedCustomerBillingRate... appliedCustomerBillingRates) {
		logger.info("Execute Payments for {} appliedCustomerBillingRate received", appliedCustomerBillingRates.length);
		int num = 0;
		
		for (AppliedCustomerBillingRate appliedCustomerBillingRate : appliedCustomerBillingRates) {
			
			// filter appliedCustomerBillingRates with isBilled a false -> need to be paid
			if (!appliedCustomerBillingRate.getIsBilled()) {
				logger.debug("Bill {} needs to be paid", appliedCustomerBillingRate.getId());
				
				//TODO prepare the payload for the payment
				String payload = getPaymentPayload();
				
				if (callPaymentEG(payload)) {
					
					//TODO update AppliedCustomerBillingRate and save Payment in TMForum				
					if (updateAppliedCustomerBillingRate(appliedCustomerBillingRate)) {
						num++;
					}
					
				}else {
					logger.warn("There was a problem with the payment for bill: {}", appliedCustomerBillingRate.getId());
				}
				
			}else {
				logger.debug("Bill {} already paid", appliedCustomerBillingRate.getId());
			}
		}
		String response = "Number of payments executed: " + num;
		logger.info(response);
		return response;
	}
		
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
	

	
	
	private boolean callPaymentEG(String payload) {
		logger.info("Payment via EG APIs");
		
		logger.debug("Payment payload to send EG APIs: {}", payload);
		//TODO call to EG APIs
		
		return true;
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
