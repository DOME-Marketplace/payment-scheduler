package it.eng.dome.payment.scheduler.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LearCredentialMachineLoader {

	private static final String LEAR_CREDENTIAL_PATH = ".lear-credential-machine";
	private static final String ENV_VAR_NAME = "LEAR_CREDENTIAL_BASE64"; 
	
	private static final Logger logger = LoggerFactory.getLogger(PrivateKeyLoader.class);
	
	private String learCredentialMachine;

	public LearCredentialMachineLoader() {
		try {
			
			learCredentialMachine = System.getenv(ENV_VAR_NAME);
			
			if (learCredentialMachine == null || learCredentialMachine.isBlank()) {				
				learCredentialMachine = new String(Files.readAllBytes(Paths.get(LEAR_CREDENTIAL_PATH)), StandardCharsets.UTF_8).trim();
				logger.debug("Loaded LearCredentialMachine from file: {}", LEAR_CREDENTIAL_PATH);
			}else {
				logger.debug("Loaded LearCredentialMachine via ENV_VAR: {}", ENV_VAR_NAME);
			}
			
		} catch (IOException e) {
			logger.error("Error loading LearCredentialMachine: {}", e.getMessage());
		}
	}

	public String getLearCredentialMachine() {
		return learCredentialMachine;
	}
}
