package com.graduate.novel.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Novel Backend API")
                        .version("1.0.0")
                        .description("""
                                ## Novel Reading Platform API
                                
                                Complete REST API for managing novels, chapters, users, and more.
                                
                                ### Features:
                                - 📚 Story and Chapter Management
                                - 🔐 JWT Authentication
                                - ⭐ Favorites and Ratings
                                - 💬 Comments
                                - 📊 Reading History
                                - 🤖 AI-powered Recommendations
                                - 🕷️ Web Crawling for content
                                
                                ### Authentication:
                                Most endpoints require JWT authentication. 
                                1. Login via `/api/auth/login`
                                2. Use the returned `accessToken` in the Authorization header
                                3. Format: `Bearer {token}`
                                """)
                        .contact(new Contact()
                                .name("Novel Backend Team")
                                .email("support@novel-backend.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local Development Server"),
                        new Server()
                                .url("https://api.novel-backend.com")
                                .description("Production Server")))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication",
                                new SecurityScheme()
                                        .name("Bearer Authentication")
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter JWT token obtained from login")));
    }
}

