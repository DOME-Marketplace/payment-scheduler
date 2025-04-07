package it.eng.dome.payment.scheduler.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import it.eng.dome.payment.scheduler.config.SetLogLevel;

@RestController
@RequestMapping("/configuration")
public class ConfigurationController {
	
	private final LoggingSystem loggingSystem;

    private static final String LOGGER_NAME = "it.eng.dome";
    
    @Autowired
    public ConfigurationController(LoggingSystem loggingSystem) {
        this.loggingSystem = loggingSystem;
    }
    
    @PostMapping
    public String setLogLevel(@RequestBody SetLogLevel request) {
        try {
            LogLevel logLevel = LogLevel.valueOf(request.getLevel().toUpperCase());
            loggingSystem.setLogLevel(LOGGER_NAME, logLevel);
            return "Log level for '" + LOGGER_NAME + "' set to " + logLevel;
        } catch (IllegalArgumentException e) {
            return "Invalid log level: " + request.getLevel();
        }
    }
}
