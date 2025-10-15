package it.eng.dome.payment.scheduler.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import it.eng.dome.brokerage.api.AppliedCustomerBillRateApis;
import it.eng.dome.brokerage.api.CustomerBillApis;
import it.eng.dome.brokerage.api.ProductInventoryApis;
import it.eng.dome.payment.scheduler.tmf.TmfApiFactory;


@Configuration
public class TmfApiConfig {
	
	private final Logger logger = LoggerFactory.getLogger(TmfApiConfig.class);
	
	@Autowired
	private TmfApiFactory tmfApiFactory;
	
	@Bean
    public ProductInventoryApis productInventoryApis() {
		logger.info("Initializing of ProductInventoryApis");
		
		return new ProductInventoryApis(tmfApiFactory.getTMF637ProductInventoryApiClient());
	}

	@Bean
    public AppliedCustomerBillRateApis appliedCustomerBillRateApis() {
		logger.info("Initializing of AppliedCustomerBillRateApis");
		
		return new AppliedCustomerBillRateApis(tmfApiFactory.getTMF678CustomerBillApiClient());
	}

	@Bean
    public CustomerBillApis customerBillApis() {
		logger.info("Initializing of CustomerBillApis");
		
		return new CustomerBillApis(tmfApiFactory.getTMF678CustomerBillApiClient());
	}
}
