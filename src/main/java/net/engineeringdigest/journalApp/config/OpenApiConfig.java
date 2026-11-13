package net.engineeringdigest.journalApp.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.bson.types.ObjectId;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// API docs at /swagger-ui.html, with an "Authorize" button that takes the access token.
@Configuration
public class OpenApiConfig {

    static {
        // Entry ids travel as plain 24-character strings; without this the docs describe ObjectId's internals.
        SpringDocUtils.getConfig().replaceWithClass(ObjectId.class, String.class);
    }

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info().title("Journal App API").version("v1"))
                .components(new Components().addSecuritySchemes("bearer",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearer"));
    }
}
