package it.eng.dome.payment.scheduler.tmf;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import it.eng.dome.brokerage.billing.utils.UrlPathUtils;

@Component(value = "tmfApiFactory")
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
public final class TmfApiFactory implements InitializingBean {

	private static final Logger logger = LoggerFactory.getLogger(TmfApiFactory.class);
	private static final String TMF_ENDPOINT_CONCAT_PATH = "-";

	@Value("${tmforumapi.tmf_endpoint}")
	public String tmfEndpoint;

	@Value("${tmforumapi.tmf_envoy}")
	public boolean tmfEnvoy;

	@Value("${tmforumapi.tmf_namespace}")
	public String tmfNamespace;

	@Value("${tmforumapi.tmf_postfix}")
	public String tmfPostfix;

	@Value("${tmforumapi.tmf_port}")
	public String tmfPort;

	@Value( "${tmforumapi.tmf678_billing_path}" )
	private String tmf678CustomerBillPath;
	
	@Value( "${tmforumapi.tmf637_inventory_path}" )
	private String tmf637ProductInventoryPath;
	
	
	private it.eng.dome.tmforum.tmf678.v4.ApiClient apiClientTmf678;	
	private it.eng.dome.tmforum.tmf637.v4.ApiClient apiClientTmf637;
	
	
	public it.eng.dome.tmforum.tmf678.v4.ApiClient getTMF678CustomerBillApiClient() {
		if (apiClientTmf678 == null) { 
			apiClientTmf678 = it.eng.dome.tmforum.tmf678.v4.Configuration.getDefaultApiClient();
			
			String basePath = tmfEndpoint;
			if (!tmfEnvoy) { // no envoy specific path
				basePath += TMF_ENDPOINT_CONCAT_PATH + "customer-bill-management" + "." + tmfNamespace + "." + tmfPostfix + ":" + tmfPort;
			}
			
			apiClientTmf678.setBasePath(basePath + "/" + tmf678CustomerBillPath);
			logger.debug("Invoke Customer Billing API at endpoint: " + apiClientTmf678.getBasePath());
		}
		return apiClientTmf678;
	}

	
	public it.eng.dome.tmforum.tmf637.v4.ApiClient getTMF637ProductInventoryApiClient() {	
		if (apiClientTmf637 == null) {
			apiClientTmf637  = it.eng.dome.tmforum.tmf637.v4.Configuration.getDefaultApiClient();
			
			String basePath = tmfEndpoint;
			if (!tmfEnvoy) { // no envoy specific path
				basePath += TMF_ENDPOINT_CONCAT_PATH + "product-inventory" + "." + tmfNamespace + "." + tmfPostfix + ":" + tmfPort;
			}
			
			apiClientTmf637.setBasePath(basePath + "/" + tmf637ProductInventoryPath);
			logger.debug("Invoke Catalog API at endpoint: " + apiClientTmf637.getBasePath());
		}
		
		return apiClientTmf637;
	}
	
	
	@Override
	public void afterPropertiesSet() throws Exception {
		logger.info("Payment Scheduler is using the following TMForum endpoint prefix: " + tmfEndpoint);
		if (tmfEnvoy) {
			logger.info("You set the apiProxy for TMForum endpoint. No tmf_port {} can be applied", tmfPort);
		} else {
			logger.info("No apiProxy set for TMForum APIs. You have to access on specific software via paths at tmf_port {}", tmfPort);
		}

		Assert.state(!StringUtils.isBlank(tmfEndpoint),	"Payment Scheduler not properly configured. tmf_endpoint property has no value.");
		Assert.state(!StringUtils.isBlank(tmf637ProductInventoryPath), "Payment Scheduler not properly configured. The tmf637_inventory_path property has no value.");
		Assert.state(!StringUtils.isBlank(tmf678CustomerBillPath), "Payment Scheduler not properly configured. The tmf678_billing_path property has no value.");

		if (tmfEndpoint.endsWith("/")) {
			tmfEndpoint = UrlPathUtils.removeFinalSlash(tmfEndpoint);
		}

		if (tmf678CustomerBillPath.startsWith("/")) {
			tmf678CustomerBillPath = UrlPathUtils.removeInitialSlash(tmf678CustomerBillPath);
		}
		
		if (tmf637ProductInventoryPath.startsWith("/")) {
			tmf637ProductInventoryPath = UrlPathUtils.removeInitialSlash(tmf637ProductInventoryPath);
		}

	}
	
}
