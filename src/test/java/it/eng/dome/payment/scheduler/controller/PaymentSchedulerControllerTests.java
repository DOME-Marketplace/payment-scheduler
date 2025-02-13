package it.eng.dome.payment.scheduler.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentSchedulerControllerTests {

	@Test
	void contextLoads() {
	}
	
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private BuildProperties buildProperties;

    @Test
    public void shouldReturnExpectedMessage() throws Exception {

        mockMvc.perform(get("/payment/info").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.version").value(buildProperties.getVersion()))
            .andExpect(jsonPath("$.name").value(buildProperties.getName()));
    }

}
