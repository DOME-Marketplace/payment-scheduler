package it.eng.dome.payment.scheduler.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PrivateKeyLoader {

	private static final String PRIVATE_KEY_PATH = ".private-key";
	private static final String ENV_VAR_NAME = "PRIVATE_KEY_BASE64"; 
	
	private static final Logger logger = LoggerFactory.getLogger(PrivateKeyLoader.class);
	
	private String privateKey;

	public PrivateKeyLoader() {
		try {
			
			privateKey = System.getenv(ENV_VAR_NAME);
			
			if (privateKey == null || privateKey.isBlank()) {				
				privateKey = new String(Files.readAllBytes(Paths.get(PRIVATE_KEY_PATH)), StandardCharsets.UTF_8).trim();
				logger.debug("Loaded PrivateKey from file: {}", PRIVATE_KEY_PATH);
			}else {
				logger.debug("Loaded PrivateKey via ENV_VAR: {}", ENV_VAR_NAME);
			}
			
		} catch (IOException e) {
			logger.error("Error loading privateKey: {}", e.getMessage());
		}
	}

	public String getPrivateKey() {
		return privateKey;
	}

}
