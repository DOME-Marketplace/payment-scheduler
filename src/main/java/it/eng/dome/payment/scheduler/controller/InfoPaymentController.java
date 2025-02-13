package it.eng.dome.payment.scheduler.controller;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import it.eng.dome.brokerage.billing.utils.DateUtils;

@RestController
@RequestMapping("/payment")
public class InfoPaymentController {

	private static final Logger log = LoggerFactory.getLogger(InfoPaymentController.class);

    @Autowired
    private BuildProperties buildProperties;

	@RequestMapping(value = "/info", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(responses = { @ApiResponse(content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = @ExampleObject(value = INFO))) })
    public Map<String, String> getInfo() {
        log.info("Request getInfo");
        Map<String, String> map = new HashMap<String, String>();
        map.put("version", buildProperties.getVersion());
        map.put("name", buildProperties.getName());
        map.put("release_time", DateUtils.getFormatterTimestamp(buildProperties.getTime()));
        log.debug(map.toString());
        return map;
    }
	
    private final String INFO = "{\"name\":\"Payment Scheduler\", \"version\":\"0.0.1\", \"release_time\":\"14-02-2025 17:31:56\"}";
}
