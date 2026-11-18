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
import sv.roundup.api.dto.*;
import sv.roundup.api.entidad.Cuenta;
import sv.roundup.api.servicio.CuentaServicio;

import java.util.List;

@Tag(name = "Cuentas", description = "Consulta de una cuenta: datos, compras y metas")
@RestController
@RequestMapping("/api/cuentas")
public class CuentaControlador {

    private final CuentaServicio cuentaServicio;

    public CuentaControlador(CuentaServicio cuentaServicio) {
        this.cuentaServicio = cuentaServicio;
    }

    @Operation(summary = "Obtener cuenta", description = "Obtiene la cuenta usando el numero de cuenta")
    @GetMapping("/{numero}")
    public ResponseEntity<CuentaRespuesta> cuentasDe(@PathVariable String numero){
        return cuentaServicio.porNumero(numero)
                .map(CuentaRespuesta::de)
                .map(ResponseEntity::ok)
                .orElse(ManejadorErrores.noEncontrado("No existe la cuenta " + numero));
    }

    @Operation(summary = "Obtener compras", description = "Obtiene las compras usando el numero de cuenta")
    @GetMapping("/{numero}/compras")
    public ResponseEntity<List<CompraResumen>> comprasDe(@PathVariable String numero){
        if (!cuentaServicio.existe(numero)){
            return ManejadorErrores.noEncontrado("No existe la cuenta " + numero);
        }

        List<CompraResumen> lista = cuentaServicio.comprasDe(numero).stream()
                .map(CompraResumen::de)
                .toList();
        return ResponseEntity.ok(lista);
    }

    @Operation(summary = "Obtener metas", description = "Obtiene las metas usando el numero de cuenta")
    @GetMapping("/{numero}/metas")
    public ResponseEntity<List<MetaRespuesta>> metasDe(@PathVariable String numero){
        if (!cuentaServicio.existe(numero)){
            return ManejadorErrores.noEncontrado("No existe la cuenta " + numero);
        }

        List<MetaRespuesta> lista = cuentaServicio.metasDe(numero).stream()
                .map(MetaRespuesta::de)
                .toList();

        return ResponseEntity.ok(lista);
    }

    @Operation(summary = "Listar cuentas", description = "Todas las cuentas con su cliente, plan, estado y saldo")
    @GetMapping
    public ResponseEntity<List<CuentaRespuesta>> listar(){
        return ResponseEntity.ok(cuentaServicio.listar().stream().map(CuentaRespuesta::de).toList());
    }

    @Operation(summary = "Abrir cuenta", description = "Abre una cuenta ACTIVA con su saldo de apertura y su plan. 201, o 400 si el cliente, el asesor o el plan no existen")
    @PostMapping
    public ResponseEntity<CuentaRespuesta> abrir(@Valid @RequestBody CuentaSolicitud s){
        Cuenta cuenta = cuentaServicio.abrir(s.duiCliente(), s.codigoAsesor(), s.codigoPlan(), s.saldoApertura());
        return ResponseEntity.status(HttpStatus.CREATED).body(CuentaRespuesta.de(cuenta));
    }

    @Operation(summary = "Editar cuenta", description = "Cambia el plan o el estado (ACTIVA o BLOQUEADA). 404 si no existe, 400 si esta CERRADA")
    @PatchMapping("/{numero}")
    public ResponseEntity<CuentaRespuesta> editar(@PathVariable String numero, @Valid @RequestBody CuentaEdicion e){
        if (!cuentaServicio.editar(numero, e.codigoPlan(), e.estado())){
            return ManejadorErrores.noEncontrado("No existe la cuenta " + numero);
        }
        // Se vuelve a leer con join fetch: la cuenta bloqueada trae al cliente como "sobre cerrado"
        return cuentaServicio.porNumero(numero)
                .map(CuentaRespuesta::de)
                .map(ResponseEntity::ok)
                .orElse(ManejadorErrores.noEncontrado("No existe la cuenta " + numero));
    }

    @Operation(summary = "Cerrar cuenta", description = "La marca CERRADA (no se borra: tiene compras). 204, o 404 si no existe")
    @PreAuthorize("hasRole('ASESOR')")
    @DeleteMapping("/{numero}")
    public ResponseEntity<Void> cerrar(@PathVariable String numero){
        if (cuentaServicio.cerrar(numero)){
            return ResponseEntity.noContent().build();
        }
        return ManejadorErrores.noEncontrado("No existe la cuenta " + numero);
    }

    @Operation(summary = "Crear meta", description = "Crea una meta ACTIVA para la cuenta. 400 si ya tiene una ACTIVA (R5) o si la cuenta no esta ACTIVA; 404 si no existe")
    @PreAuthorize("hasRole('CLIENTE')")
    @PostMapping("/{numero}/metas")
    public ResponseEntity<MetaRespuesta> crearMeta(@PathVariable String numero, @Valid @RequestBody MetaSolicitud s, @AuthenticationPrincipal Jwt token) {
        return cuentaServicio.crearMeta(numero, s.nombre(), s.montoObjetivo(), token.getSubject())
                .map(MetaRespuesta::de)
                .map(m -> ResponseEntity.status(HttpStatus.CREATED).body(m))
                .orElse(ManejadorErrores.noEncontrado("No existe la cuenta " + numero));
    }
}
