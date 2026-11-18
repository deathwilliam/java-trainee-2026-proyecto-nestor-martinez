package sv.roundup.api.controlador;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sv.roundup.api.dto.ComercioEdicion;
import sv.roundup.api.dto.ComercioRespuesta;
import sv.roundup.api.dto.ComercioSolicitud;
import sv.roundup.api.entidad.Comercio;
import sv.roundup.api.servicio.ComercioServicio;

import java.util.List;

@Tag(name = "Comercio", description = "Catalogos de comercios")
@RestController
@RequestMapping("/api/comercios")
public class ComercioControlador {
    private final ComercioServicio comercioServicio;

    public ComercioControlador(ComercioServicio comercioServicio){
        this.comercioServicio = comercioServicio;
    }

    @Operation(summary = "Listar comercios", description = "Todos los comercios, activos e inactivos, con su codigo y nombre")
    @GetMapping
    public ResponseEntity<List<ComercioRespuesta>> listar(){
        List<ComercioRespuesta> lista = comercioServicio.listar().stream()
                .map(ComercioRespuesta::de)
                .toList();

        return ResponseEntity.ok(lista);
    }

    @Operation(summary = "Buscar comercio", description = "Busca un comercio por su codigo. 200 si existe, 404 si no")
    @GetMapping("/{codigo}")
    public ResponseEntity<ComercioRespuesta> buscar(@PathVariable String codigo){
        return comercioServicio.porCodigo(codigo)
                .map(ComercioRespuesta::de)
                .map(ResponseEntity::ok)
                .orElse(ManejadorErrores.noEncontrado("No existe el comercio " + codigo));
    }

    @Operation(summary = "Crear comercio", description = "Registra un comercio ACTIVO. 201, o 400 si ya existe uno con ese codigo")
    @PostMapping
    public ResponseEntity<ComercioRespuesta> crear(@Valid @RequestBody ComercioSolicitud s){
        Comercio comercio = comercioServicio.crear(s.codigo(), s.nombre());
        return ResponseEntity.status(HttpStatus.CREATED).body(ComercioRespuesta.de(comercio));
    }

    @Operation(summary = "Editar comercio", description = "Cambio el nombre del negocio, 404 si el negocio no existe")
    @PatchMapping("/{codigo}")
    public ResponseEntity<ComercioRespuesta> editar(@PathVariable String codigo, @Valid @RequestBody ComercioEdicion e){
        return comercioServicio.editar(codigo, e.nombre())
                .map(ComercioRespuesta::de)
                .map(ResponseEntity::ok)
                .orElse(ManejadorErrores.noEncontrado("No existe el comercio "+ codigo));
    }

    @Operation(summary = "Desactivar comercio", description = "Lo marca inactivo (no se borra: tiene compras). 204 si se hizo, 404 si no existe")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{codigo}")
    public ResponseEntity<Void> desactivar(@PathVariable String codigo) {
        if (comercioServicio.desactivar(codigo)) {
            return ResponseEntity.noContent().build();
        }
        return ManejadorErrores.noEncontrado("No existe el comercio " + codigo);
    }
}
