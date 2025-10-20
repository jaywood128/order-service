package com.streamcart.order.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI orderServiceOpenAPI() {
        // Define JWT Bearer token security scheme
        final String securitySchemeName = "bearerAuth";
        
        return new OpenAPI()
                .info(new Info()
                        .title("StreamCart Order Service API")
                        .description("Event-driven microservice for processing orders in StreamCart e-commerce platform. " +
                                "Handles order creation, order retrieval, and publishes order.created events to Kafka. " +
                                "Uses JWT authentication for secure access.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("StreamCart Team")
                                .email("support@streamcart.com")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8081")
                                .description("Local development server"),
                        new Server()
                                .url("http://localhost:8081")
                                .description("Docker development environment")))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter JWT token obtained from /api/auth/login or /api/auth/register")));
    }
}

