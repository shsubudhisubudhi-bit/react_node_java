package com.developer.test.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    @JsonProperty("error")
    private String error;

    @JsonProperty("details")
    private Map<String, String> details;

    @JsonProperty("timestamp")
    private String timestamp;

    public ErrorResponse() {
    }

    public ErrorResponse(String error) {
        this(error, null);
    }

    public ErrorResponse(String error, Map<String, String> details) {
        this.error = error;
        this.details = details;
        this.timestamp = Instant.now().toString();
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public Map<String, String> getDetails() {
        return details;
    }

    public void setDetails(Map<String, String> details) {
        this.details = details;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
