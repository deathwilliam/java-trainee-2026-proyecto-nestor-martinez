package sv.roundup.api.controlador;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfiguracionOpenApi {

    private static final String ESQUEMA = "bearer-jwt";

    @Bean
    public OpenAPI documentacion(){
        return new OpenAPI()
                .info(
                        new Info()
                                .title("API de RoundUp")
                                .version("1.0")
                                .description("Ahorro por redondeo de compras: cuentas, comercios, " +
                                        "compras y metas. Proyecto integrador del Programa Digital " +
                                        "Trainee 2026."))
                .components(new Components().addSecuritySchemes(ESQUEMA, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA));

    }
}
