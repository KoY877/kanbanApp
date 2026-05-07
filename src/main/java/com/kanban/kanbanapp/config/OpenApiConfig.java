package com.kanban.kanbanapp.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class OpenApiConfig {
   @Value ("${server.port:8081}")
   private String serverPort;

    @Bean 
    public OpenAPI customOpenAPI() {
        final String securitySchemaName = "bearerAuth";
        return new OpenAPI()
            .info(new Info()
                    .title("Kanban API")
                    .version("1.0")
                    .description("""
                        ## REST API for a Kanban application
                
                        ### Authentication
                        This API uses **JWT Bearer tokens** for authentication.
                        
                        **IMPORTANT - httpOnly Cookie:**
                        - The `refreshToken` is sent via an **httpOnly cookie** (Set-Cookie header)
                        - It NEVER appears in the JSON body of `/auth/login` or `/auth/register` responses
                        - The cookie is automatically sent by the browser on `/auth/refresh`
                        
                        **How to authenticate:**
                        1. Call `/auth/login` or `/auth/register`
                        2. Copy the `accessToken` from the response
                        3. Click **"Authorize"** in the top right corner
                        4. Paste the token (without "Bearer ") and confirm
                        5. All protected endpoints will now be accessible
                        
                        ### Available resources
                        - **Auth**: Register, login, token refresh, logout
                        - **Boards**: Kanban board management
                        - **Columns**: Column management (To Do, In Progress, Done...)
                        - **Tasks**: Task management
                        - **Members**: Board member management
                        - **Users**: User management
                    """)
                    .contact(new Contact()
                            .name("TodoApp Support")
                            .email("support@todoapp.com"))
                    .license(new License()
                            .name("MIT License")
                            .url("https://opensource.org/licenses/MIT")))
            .servers(List.of(
                new Server()
                    .url("http://localhost:" + serverPort)
                    .description("Serveur de développement local")))
            .components(new io.swagger.v3.oas.models.Components()
                .addSecuritySchemes(securitySchemaName, new io.swagger.v3.oas.models.security.SecurityScheme()
                    .type(io.swagger.v3.oas.models.security.SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("Entrez votre JWT token (sans 'Bearer ')")))
            .addSecurityItem(new SecurityRequirement().addList(securitySchemaName));
    }
                   
}
