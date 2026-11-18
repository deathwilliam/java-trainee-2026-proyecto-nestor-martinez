package sv.roundup.api.dto;

import sv.roundup.api.entidad.PlanRedondeo;

import java.math.BigDecimal;

public record PlanRespuesta(
        String codigo,
        String nombre,
        int multiplicador,
        BigDecimal topeMensual,
        boolean activo
) {
    public static PlanRespuesta de(PlanRedondeo p){
        return new PlanRespuesta(p.getCodigo(), p.getNombre(), p.getMultiplicador(),
                p.getTopeMensual(), p.isActivo());
    }
}
