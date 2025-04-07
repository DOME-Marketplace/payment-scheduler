package it.eng.dome.payment.scheduler.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import it.eng.dome.payment.scheduler.request.SetLogLevel;
import it.eng.dome.payment.scheduler.util.Constants;

@RestController
@RequestMapping("/configuration")
public class ConfigurationController {
	
	private static final String LOG_LEVEL_FILENAME = "log-level.json";

	private static final Path LOG_LEVEL_FILE = Path.of(Constants.LOG_LEVEL_PATH + File.separator + LOG_LEVEL_FILENAME);


    @Autowired
    private LoggingSystem loggingSystem;
    
    @PostMapping
    public String setLogLevel(@RequestBody SetLogLevel request) throws IOException {
        try {
            LogLevel logLevel = LogLevel.valueOf(request.getLevel().toUpperCase());
            loggingSystem.setLogLevel(Constants.LOGGER_PACKAGE_PATH, logLevel);
            
            Files.createDirectories(LOG_LEVEL_FILE.getParent());
            Files.writeString(LOG_LEVEL_FILE, "{\"logLevel\":\"" + logLevel.name() + "\"}");
            
            return "Log level for '" + Constants.LOGGER_PACKAGE_PATH + "' set to " + logLevel;
        } catch (IllegalArgumentException e) {
            return "Invalid log level: " + request.getLevel();
        }
    }
}
