package com.flightapp.authservice.dto.request;
import lombok.Data;

@Data
public class SignupRequest {
    private String username;
    private String password;
    // optional, only used when creating admin
    private String adminSecret;
}
