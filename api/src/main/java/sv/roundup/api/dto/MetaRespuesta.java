package sv.roundup.api.dto;

import sv.roundup.api.entidad.EstadoMeta;
import sv.roundup.api.entidad.MetaAhorro;

import java.math.BigDecimal;

public record MetaRespuesta(
        String nombre,
        BigDecimal montoObjetivo,
        BigDecimal montoAhorrado,
        BigDecimal avance,
        EstadoMeta estado
) {
    public static MetaRespuesta de(MetaAhorro m){
        return new MetaRespuesta(m.getNombre(), m.getMontoObjetivo(), m.getMontoAhorrado(),
                m.avance(),m.getEstado());
    }
}
