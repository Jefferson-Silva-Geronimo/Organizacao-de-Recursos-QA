package com.organizacao_de_recursos.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearer-jwt";

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info().title("Organização de Recursos").version("v1")
                        .description("API de alocação de salas, professores e materiais sem conflitos de horário."))
                .addSecurityItem(new SecurityRequirement().addList(BEARER))
                .components(new Components().addSecuritySchemes(BEARER,
                        new SecurityScheme().name(BEARER).type(SecurityScheme.Type.HTTP)
                                .scheme("bearer").bearerFormat("JWT")));
    }
}
