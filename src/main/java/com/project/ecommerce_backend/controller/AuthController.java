package com.project.ecommerce_backend.controller;


import com.project.ecommerce_backend.dto.JwtResponse;
import com.project.ecommerce_backend.dto.LoginRequest;
import com.project.ecommerce_backend.dto.SignupRequest;
import com.project.ecommerce_backend.entity.User;
import com.project.ecommerce_backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody
                                      SignupRequest request){
        User user = authService.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body("User registered successfully");

    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody
                                   LoginRequest request) {
        JwtResponse response = authService.loginUser(request);
        return ResponseEntity.ok(response);
    }
}
