package it.eng.dome.payment.scheduler.controller;

import it.eng.dome.payment.scheduler.loader.LearCredentialMachineLoader;
import it.eng.dome.payment.scheduler.loader.PrivateKeyLoader;
import it.eng.dome.payment.scheduler.service.M2MTokenService;

public class TestToken {
	
	
	public static void main(String[] args) {
		LearCredentialMachineLoader learCredentialMachineLoader = new LearCredentialMachineLoader();
		
		PrivateKeyLoader privateKey = new PrivateKeyLoader();
		M2MTokenService m2m = new M2MTokenService(privateKey);
		
		//usage of reflection to set the 'externalDomain' attribute instead of to set via constructor o getter and setter 
		org.springframework.test.util.ReflectionTestUtils.setField(m2m, "externalDomain", "https://verifier.dome-marketplace-sbx.org");
		System.out.println(m2m.getAssertion(learCredentialMachineLoader.getLearCredentialMachine()).get("client_assertion"));
		
	}
}
