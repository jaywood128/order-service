package com.streamcart.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response payload containing JWT token and user information after successful registration or login")
public record AuthResponse(
    @Schema(description = "JWT token for authentication. Valid for 15 minutes. Include in Authorization header as 'Bearer {token}'",
            example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJqb2huZG9lIiwiaWF0IjoxNjk5...")
    String token,
    
    @Schema(description = "Username of the authenticated user",
            example = "johndoe")
    String username,
    
    @Schema(description = "Email address of the authenticated user",
            example = "john.doe@example.com")
    String email,
    
    @Schema(description = "Success message",
            example = "User registered successfully")
    String message
) {
}

