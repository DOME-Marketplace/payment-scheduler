package it.eng.dome.payment.scheduler.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JwtResponse {

	@JsonProperty("responseJwt")
	private String responseJwt;

	@JsonProperty("error")
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private ErrorDetails error;

	public String getResponseJwt() {
		return responseJwt;
	}

	public void setResponseJwt(String responseJwt) {
		this.responseJwt = responseJwt;
	}

	public ErrorDetails getError() {
		return error;
	}

	public void setError(ErrorDetails error) {
		this.error = error;
	}

	public static class ErrorDetails {
		
		@JsonProperty("workflowId")
		private String workflowId;

		@JsonProperty("state")
		private String state;

		public String getWorkflowId() {
			return workflowId;
		}

		public void setWorkflowId(String workflowId) {
			this.workflowId = workflowId;
		}

		public String getState() {
			return state;
		}

		public void setState(String state) {
			this.state = state;
		}
		
		public String toJson() {
	        ObjectMapper objectMapper = new ObjectMapper();
			try {
				return objectMapper.writeValueAsString(this);
			} catch (JsonProcessingException e) {
				return null;
			}
		}
	}
}
