package com.developer.test.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

public class CreateUserRequest {

    @JsonProperty("name")
    @NotBlank(message = "name is required")
    private String name;

    @JsonProperty("email")
    @NotBlank(message = "email is required")
    @Email(message = "email must be a valid email address")
    private String email;

    @JsonProperty("role")
    @NotBlank(message = "role is required")
    private String role;

    public CreateUserRequest() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
