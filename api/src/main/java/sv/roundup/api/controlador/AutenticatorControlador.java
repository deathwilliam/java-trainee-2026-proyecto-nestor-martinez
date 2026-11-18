package sv.roundup.api.controlador;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import sv.roundup.api.dto.LoginSolicitud;
import sv.roundup.api.dto.TokenRespuesta;
import sv.roundup.api.servicio.AutenticacionServicio;

import java.util.Map;

@Tag(name = "Autenticacion", description = "Login: usuario y clave a cambio de un token JWT")
@RestController
@RequestMapping("/api/auth")
public class AutenticatorControlador {

    private final AutenticacionServicio autenticacionServicio;

    public AutenticatorControlador(AutenticacionServicio autenticacionServicio) {
        this.autenticacionServicio = autenticacionServicio;
    }

    @Operation(summary = "Iniciar sesion", description = "Devuelve un token JWT que vence en 1 hora. 401 si el usuario o la clave estan mal")
    @PostMapping("/login")
    public ResponseEntity<TokenRespuesta> login(@Valid @RequestBody LoginSolicitud s){
        return ResponseEntity.ok(autenticacionServicio.login(s.usuario(), s.clave()));
    }

    @Operation(summary = "Quien soy", description = "Lee el token y devuelve el usuario y sus roles")
    @GetMapping("/yo")
    public ResponseEntity<Map<String, Object>> yo(@AuthenticationPrincipal Jwt token){
        return ResponseEntity.ok(Map.of(
                "usuario", token.getSubject(),
                "roles", token.getClaimAsStringList("roles")
        ));
    }
}
