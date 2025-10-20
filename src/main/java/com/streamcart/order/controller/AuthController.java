package com.streamcart.order.controller;

import com.streamcart.order.dto.AuthResponse;
import com.streamcart.order.dto.LoginRequest;
import com.streamcart.order.dto.RegisterRequest;
import com.streamcart.order.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "User registration and authentication endpoints. No JWT token required for these operations.")
public class AuthController {
    
    private final AuthService authService;
    
    @Operation(
            summary = "Register a new user",
            description = "Creates a new user account with encrypted password and returns a JWT token for immediate authentication. " +
                    "Username and email must be unique. Password is encrypted using BCrypt before storage."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "User registered successfully. JWT token included in response.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request: validation errors, duplicate username, or duplicate email"
            )
    })
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registration request received for username: {}", request.username());
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @Operation(
            summary = "Login with existing user credentials",
            description = "Authenticates user with username and password. Returns a JWT token valid for 15 minutes. " +
                    "Use the returned token in the Authorization header (Bearer <token>) for protected endpoints."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Login successful. JWT token included in response.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Invalid credentials: username not found or password incorrect"
            )
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login request received for username: {}", request.username());
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}

