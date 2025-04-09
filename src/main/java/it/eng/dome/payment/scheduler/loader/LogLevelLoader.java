package it.eng.dome.payment.scheduler.loader;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.eng.dome.payment.scheduler.util.Constants;
import jakarta.annotation.PostConstruct;

@Component
public class LogLevelLoader {

    private static final Path LOG_LEVEL_FILE = Path.of(Constants.LOG_LEVEL_PATH + File.separator + Constants.LOG_LEVEL_FILENAME);
    
    private static final Logger logger = LoggerFactory.getLogger(LogLevelLoader.class);
    
    @Autowired
    private LoggingSystem loggingSystem;

    @PostConstruct
    public void initLogLevel() {
        if (Files.exists(LOG_LEVEL_FILE)) {
        	ObjectMapper objectMapper = new ObjectMapper();
            try {
                String content = Files.readString(LOG_LEVEL_FILE);
                JsonNode jsonNode = objectMapper.readTree(content);                
                String levelStr = jsonNode.get("logLevel").asText();
                LogLevel level = LogLevel.valueOf(levelStr.toUpperCase());
                
                loggingSystem.setLogLevel(Constants.LOGGER_PACKAGE_PATH, level);
                logger.info("Log level loaded from file: {}", level);
            } catch (Exception e) {
            	logger.error("Failed to load log level: {}", e.getMessage());
            }
        }
    }
}
