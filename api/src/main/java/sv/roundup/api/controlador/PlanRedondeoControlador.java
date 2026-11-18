package sv.roundup.api.controlador;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sv.roundup.api.dto.PlanRespuesta;
import sv.roundup.api.servicio.PlanRedondeoServicio;

import java.util.List;

@Tag(name = "Planes", description = "Catalogo de planes de redondeo (x1, x2, x3) con su tope mensual")
@RestController
@RequestMapping("/api/planes")
public class PlanRedondeoControlador {
    private final PlanRedondeoServicio planServicio;

    public PlanRedondeoControlador(PlanRedondeoServicio planServicio){
        this.planServicio = planServicio;
    }

    @Operation(summary = "Listar planes", description = "Los planes de redondeo con su multiplicador y su tope mensual")
    @GetMapping
    public ResponseEntity<List<PlanRespuesta>> listar(){
        return ResponseEntity.ok(planServicio.listar().stream().map(PlanRespuesta::de).toList());
    }
}
