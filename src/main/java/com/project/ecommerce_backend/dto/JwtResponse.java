package com.project.ecommerce_backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JwtResponse {
    private String token;
    private String type = "Bearer";
    private String username;
    private String email;

    public JwtResponse(String token,
                       String username,
                       String email) {
        this.token = token;
        this.username = username;
        this.email = email;
    }
}
