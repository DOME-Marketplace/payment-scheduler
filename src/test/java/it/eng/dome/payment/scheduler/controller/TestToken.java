package it.eng.dome.payment.scheduler.controller;

import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import it.eng.dome.payment.scheduler.loader.LearCredentialMachineLoader;
import it.eng.dome.payment.scheduler.loader.PrivateKeyLoader;
import it.eng.dome.payment.scheduler.service.M2MTokenService;
import it.eng.dome.payment.scheduler.service.VCVerifier;

public class TestToken {
	
	public static void main(String[] args) {
		//mainSBX(args);
		mainPRD(args);
	}
	
	
	public static void mainPRD(String[] args) {
		LearCredentialMachineLoader learCredentialMachineLoader = new LearCredentialMachineLoader();
		
		PrivateKeyLoader privateKey = new PrivateKeyLoader();
		M2MTokenService m2m = new M2MTokenService(privateKey);

		//usage of reflection to set the 'externalDomain' attribute instead of to set via constructor o getter and setter 
		org.springframework.test.util.ReflectionTestUtils.setField(m2m, "externalDomain", "https://verifier.dome-marketplace.eu");
				
		String client_assertion =  m2m.getAssertion(learCredentialMachineLoader.getLearCredentialMachine()).get("client_assertion");
		System.out.println("Get client_assertion: " + client_assertion);
		
		RestClient restClient = RestClient.create();
		VCVerifier verifier = new VCVerifier(restClient, learCredentialMachineLoader);
		
		//usage of reflection to set attributes in VCVerifier
		org.springframework.test.util.ReflectionTestUtils.setField(verifier, "endpoint", "https://verifier.dome-marketplace.eu/oidc/token");
		org.springframework.test.util.ReflectionTestUtils.setField(verifier, "m2mTokenService", m2m);
		
		try {			
			System.out.println("Get token: " + verifier.getVCVerifierToken());
		} catch (HttpClientErrorException e) {	
			System.out.println("\n\n********* Exception *********");
			System.out.println("Status: " + e.getStatusCode());
			System.out.println("Body: " + e.getResponseBodyAsString());
		}
	}
	
	public static void mainSBX(String[] args) {
		LearCredentialMachineLoader learCredentialMachineLoader = new LearCredentialMachineLoader();
		
		PrivateKeyLoader privateKey = new PrivateKeyLoader();
		System.out.println("private key: " + privateKey.getPrivateKey());
		M2MTokenService m2m = new M2MTokenService(privateKey);
	
		
		//usage of reflection to set the 'externalDomain' attribute instead of to set via constructor o getter and setter 
		org.springframework.test.util.ReflectionTestUtils.setField(m2m, "externalDomain", "https://verifier.dome-marketplace-sbx.org");
				
		//String client_assertion =  m2m.getAssertion(learCredentialMachineLoader.getLearCredentialMachine()).get("client_assertion");
		//System.out.println("Get client_assertion: " + client_assertion);
		
		RestClient restClient = RestClient.create();
		VCVerifier verifier = new VCVerifier(restClient, learCredentialMachineLoader);
		
		//usage of reflection to set attributes in VCVerifier
		org.springframework.test.util.ReflectionTestUtils.setField(verifier, "endpoint", "https://verifier.dome-marketplace-sbx.org/oidc/token");
		//org.springframework.test.util.ReflectionTestUtils.setField(verifier, "endpoint", "https://verifier.dome-marketplace-sbx.org/verifier/oidc/token");
		org.springframework.test.util.ReflectionTestUtils.setField(verifier, "m2mTokenService", m2m);
		
		try {			
			System.out.println("Get token: " + verifier.getVCVerifierToken());
		} catch (HttpClientErrorException e) {	
			System.out.println("\n\n********* Exception *********");
			System.out.println("Status: " + e.getStatusCode());
			System.out.println("Body: " + e.getResponseBodyAsString());
		}		
	}
	
	
	
	public static void mainDEV(String[] args) {
		LearCredentialMachineLoader learCredentialMachineLoader = new LearCredentialMachineLoader();
		
		PrivateKeyLoader privateKey = new PrivateKeyLoader();
		M2MTokenService m2m = new M2MTokenService(privateKey);
		
		//usage of reflection to set the 'externalDomain' attribute instead of to set via constructor o getter and setter 
		org.springframework.test.util.ReflectionTestUtils.setField(m2m, "externalDomain", "https://verifier.dome-marketplace-dev2.org");
				
		String client_assertion =  m2m.getAssertion(learCredentialMachineLoader.getLearCredentialMachine()).get("client_assertion");
		System.out.println("Get client_assertion: " + client_assertion);
		
		RestClient restClient = RestClient.create();
		VCVerifier verifier = new VCVerifier(restClient, learCredentialMachineLoader);
		
		//usage of reflection to set attributes in VCVerifier
		org.springframework.test.util.ReflectionTestUtils.setField(verifier, "endpoint", "https://verifier.dome-marketplace-dev2.org/oidc/token");
		org.springframework.test.util.ReflectionTestUtils.setField(verifier, "m2mTokenService", m2m);
		System.out.println("Get token: " + verifier.getVCVerifierToken());
		
	}
	
}
