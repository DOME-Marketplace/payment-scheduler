package it.eng.dome.payment.scheduler.controller;

import it.eng.dome.payment.scheduler.loader.LearCredentialMachineLoader;
import it.eng.dome.payment.scheduler.loader.PrivateKeyLoader;
import it.eng.dome.payment.scheduler.service.M2MTokenService;

public class TestToken {
	
	
	public static void main(String[] args) {
		LearCredentialMachineLoader learCredentialMachineLoader = new LearCredentialMachineLoader();
		
		PrivateKeyLoader privateKey = new PrivateKeyLoader();
		M2MTokenService m2m = new M2MTokenService(privateKey);
		System.out.println(m2m.getAssertion(learCredentialMachineLoader.getLearCredentialMachine()).get("client_assertion"));
		
	}
}
