package it.eng.dome.payment.scheduler.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import it.eng.dome.payment.scheduler.request.PaymentLogLevel;
import it.eng.dome.payment.scheduler.util.Constants;

@RestController
@RequestMapping("/configuration")
public class ConfigurationController {
	
	private static final Path LOG_LEVEL_FILE = Path.of(Constants.LOG_LEVEL_PATH + File.separator + Constants.LOG_LEVEL_FILENAME);
	
	private static final Logger logger = LoggerFactory.getLogger(ConfigurationController.class);
	
	private final String LOG_LEVEL_INFO = "{\"logLevel\":\"DEBUG\"}";


    @Autowired
    private LoggingSystem loggingSystem;
    
    @RequestMapping(value = "/loglevel", method = RequestMethod.POST, produces = MediaType.TEXT_PLAIN_VALUE)
    public String setLogLevel(@RequestBody PaymentLogLevel request) throws IOException {
        try {
            LogLevel logLevel = LogLevel.valueOf(request.getLevel().toUpperCase());
            loggingSystem.setLogLevel(Constants.LOGGER_PACKAGE_PATH, logLevel);
            
            Files.createDirectories(LOG_LEVEL_FILE.getParent());
            
            Files.writeString(LOG_LEVEL_FILE, "{\"logLevel\":\"" + logLevel.name() + "\"}");
            
            String msg_level = "Log level for '" + Constants.LOGGER_PACKAGE_PATH + "' set to " + logLevel;
            logger.info(msg_level);

            return msg_level;
        } catch (IllegalArgumentException e) {
        	logger.error("Invalid log level: {}", request.getLevel());
            return "Invalid log level: " + request.getLevel();
        }
    }
    
    @RequestMapping(value = "/loglevel", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(responses = { @ApiResponse(content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = @ExampleObject(value = LOG_LEVEL_INFO))) })
    public String getLogLevel() throws IOException {
    	LogLevel logLevel = loggingSystem.getLoggerConfiguration(Constants.LOGGER_PACKAGE_PATH).getEffectiveLevel();
    	logger.info("Get log level: {}", logLevel);
    	
    	return "{\"logLevel\": \"" + logLevel.name() + "\"}";
    }
}
