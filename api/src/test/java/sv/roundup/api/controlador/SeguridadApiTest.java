package sv.roundup.api.controlador;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * Sesion 38: lo que probabamos a mano en Swagger, comprobado solo cada vez que se corre mvnw test.
 *
 * Revisa la SEGURIDAD de la API: quien entra, quien no, y que el usuario de la bitacora sale del
 * token. CompraReglasTest revisa las reglas de la compra; esta, las dos puertas:
 *   puerta 1 = la cadena de filtros de ConfiguracionSeguridad (reglas por ruta)
 *   puerta 2 = @PreAuthorize en cada metodo
 *
 * MockMvc: hace peticiones HTTP de mentira a la aplicacion, sin abrir un puerto, pero pasando por TODO:
 * la cadena de filtros, @PreAuthorize, @Valid, el controlador, el servicio y la base roundup_test.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:5432/roundup_test",
        "spring.jpa.hibernate.ddl-auto=create",
        "jwt.clave=clave-solo-para-las-pruebas-automaticas-de-roundup"
})
@AutoConfigureMockMvc
class SeguridadApiTest {

    // Una compra chica en la cuenta de ana (import.sql): RU-000123 tiene saldo y la meta Laptop ACTIVA
    private static final String COMPRA = """
            { "numeroCuenta": "RU-000123", "codigoComercio": "SUPER-SELECTOS", "monto": 1.50 }""";

    @Autowired
    private MockMvc mvc;

    // Para leer la bitacora directo de la base: RoundUp no tiene un endpoint que la muestre
    @Autowired
    private JdbcTemplate jdbc;

    // Entra de verdad, por el login, y devuelve el token. Asi tambien se prueba el login.
    private String tokenDe(String usuario, String clave) throws Exception {
        String json = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuario\":\"%s\",\"clave\":\"%s\"}".formatted(usuario, clave)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.split("\"token\":\"")[1].split("\"")[0];
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    // ---------------------------------------------------------------- 401: no se quien eres

    @Test
    void sinTokenDa401ConProblemDetail() throws Exception {
        mvc.perform(get("/api/cuentas"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("No autenticado"));
    }

    @Test
    void unTokenFalsoDa401() throws Exception {
        mvc.perform(get("/api/cuentas").header("Authorization", bearer("esto.no.sirve")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("No autenticado"));
    }

    @Test
    void laClaveMalaDa401YNoEntregaToken() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuario\":\"ana\",\"clave\":\"mala\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Credenciales incorrectas"))
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    // ---------------------------------------------------------------- 403: se quien eres, pero no puedes

    @Test
    void elAsesorLeePeroNoCompra() throws Exception {
        String asesor = tokenDe("mlopez", "Asesor2026");

        mvc.perform(get("/api/cuentas").header("Authorization", bearer(asesor)))
                .andExpect(status().isOk());

        mvc.perform(post("/api/compras").header("Authorization", bearer(asesor))
                        .contentType(MediaType.APPLICATION_JSON).content(COMPRA))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Sin permiso"));
    }

    @Test
    void elClienteCompraYLosDatosMalosDan400() throws Exception {
        String cliente = tokenDe("ana", "Cliente2026");

        mvc.perform(post("/api/compras").header("Authorization", bearer(cliente))
                        .contentType(MediaType.APPLICATION_JSON).content(COMPRA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numeroCuenta").value("RU-000123"));

        // Sin monto: pasa las dos puertas y lo detiene @Valid
        mvc.perform(post("/api/compras").header("Authorization", bearer(cliente))
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"numeroCuenta\": \"RU-000123\" }"))
                .andExpect(status().isBadRequest());
    }

    // LA prueba de la sesion 38: el JSON dice "luis", el token dice "ana". Gana el token.
    @Test
    void elUsuarioDeLaBitacoraSaleDelTokenNoDelJson() throws Exception {
        String ana = tokenDe("ana", "Cliente2026");
        String conOtroUsuario = """
                { "numeroCuenta": "RU-000123", "codigoComercio": "SUPER-SELECTOS", "monto": 1.50, "username": "luis" }""";

        mvc.perform(post("/api/compras").header("Authorization", bearer(ana))
                        .contentType(MediaType.APPLICATION_JSON).content(conOtroUsuario))
                .andExpect(status().isCreated());

        // La fila mas nueva de la bitacora, con el username de quien quedo registrado
        String quien = jdbc.queryForObject("""
                select u.username from bitacora_operacion b join usuario u on u.id = b.usuario_id
                order by b.id desc limit 1""", String.class);
        assertEquals("ana", quien);
    }

    // El CLIENTE solo opera SUS cuentas: RU-000124 es de Luis. Ana pasa las dos puertas (es CLIENTE),
    // pero el servicio revisa que la cuenta sea de quien dice el token
    @Test
    void elClienteNoOperaCuentasAjenas() throws Exception {
        String ana = tokenDe("ana", "Cliente2026");

        mvc.perform(post("/api/compras").header("Authorization", bearer(ana))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"numeroCuenta\": \"RU-000124\", \"codigoComercio\": \"SUPER-SELECTOS\", \"monto\": 1.00 }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("La cuenta RU-000124 no es de ana"));

        mvc.perform(post("/api/cuentas/RU-000124/metas").header("Authorization", bearer(ana))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"nombre\": \"Moto\", \"montoObjetivo\": 500.00 }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("La cuenta RU-000124 no es de ana"));
    }

    @Test
    void soloElAsesorCierraCuentas() throws Exception {
        mvc.perform(delete("/api/cuentas/RU-999999").header("Authorization", bearer(tokenDe("ana", "Cliente2026"))))
                .andExpect(status().isForbidden());

        // El asesor pasa las dos puertas; la cuenta no existe, asi que lo detiene el controlador (404)
        mvc.perform(delete("/api/cuentas/RU-999999").header("Authorization", bearer(tokenDe("mlopez", "Asesor2026"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("No encontrado"));
    }

    @Test
    void soloElAdminDesactivaComercios() throws Exception {
        mvc.perform(delete("/api/comercios/NO-EXISTE").header("Authorization", bearer(tokenDe("mlopez", "Asesor2026"))))
                .andExpect(status().isForbidden());

        mvc.perform(delete("/api/comercios/NO-EXISTE").header("Authorization", bearer(tokenDe("admin", "Admin2026"))))
                .andExpect(status().isNotFound());
    }
}
