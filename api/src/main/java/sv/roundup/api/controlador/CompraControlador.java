package sv.roundup.api.controlador;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import sv.roundup.api.dto.CompraRespuesta;
import sv.roundup.api.dto.CompraSolicitud;
import sv.roundup.api.entidad.Compra;
import sv.roundup.api.servicio.CompraServicio;


@Tag(name = "Compras", description = "Procesa compras con redondeo")
@RestController
@RequestMapping("/api/compras")
public class CompraControlador {
    private final CompraServicio compraServicio;

    public CompraControlador(CompraServicio compraServicio) {
        this.compraServicio = compraServicio;
    }

    @Operation(summary = "Procesar una compra", description = "Cobra monto + redondeo a meta activa, 201 = EXITO, 400 = si una regla dice no y bitacora = FALLIDO. Quien compra sale del token, no del JSON")
    @PreAuthorize("hasRole('CLIENTE')")
    @PostMapping
    public ResponseEntity<CompraRespuesta> procesar(@Valid @RequestBody CompraSolicitud s, @AuthenticationPrincipal Jwt token){
        Compra compra = compraServicio.procesar(s.numeroCuenta(), s.codigoComercio(), s.monto(), token.getSubject());
        return ResponseEntity.status(HttpStatus.CREATED).body(CompraRespuesta.de(compra));
    }

}
