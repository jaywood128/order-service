package com.streamcart.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request payload for user registration")
public record RegisterRequest(
    @Schema(description = "Unique username for the account. Must be 3-50 characters.", 
            example = "johndoe",
            minLength = 3,
            maxLength = 50)
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    String username,
    
    @Schema(description = "Email address for the account. Must be unique and valid format.",
            example = "john.doe@example.com")
    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    String email,
    
    @Schema(description = "Password for the account. Minimum 6 characters. Will be encrypted with BCrypt.",
            example = "password123",
            minLength = 6)
    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    String password,
    
    @Schema(description = "User's first name",
            example = "John")
    @NotBlank(message = "First name is required")
    String firstName,
    
    @Schema(description = "User's last name",
            example = "Doe")
    @NotBlank(message = "Last name is required")
    String lastName
) {
}

