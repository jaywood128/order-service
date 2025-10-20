package com.streamcart.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request payload for user authentication")
public record LoginRequest(
    @Schema(description = "Username of the account",
            example = "mscott")
    @NotBlank(message = "Username is required")
    String username,
    
    @Schema(description = "Password for the account",
            example = "worldsbestboss")
    @NotBlank(message = "Password is required")
    String password
) {
}

