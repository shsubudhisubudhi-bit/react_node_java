package com.developer.test.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class CreateTaskRequest {

    @JsonProperty("title")
    @NotBlank(message = "title is required")
    private String title;

    @JsonProperty("status")
    @NotBlank(message = "status is required")
    private String status;

    @JsonProperty("userId")
    @NotNull(message = "userId is required")
    private Integer userId;

    public CreateTaskRequest() {
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }
}
