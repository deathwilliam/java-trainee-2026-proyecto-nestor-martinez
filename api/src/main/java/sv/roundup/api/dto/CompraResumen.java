package sv.roundup.api.dto;

import sv.roundup.api.entidad.Compra;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CompraResumen(
        String numeroComprobante,
        String comercio,
        BigDecimal monto,
        BigDecimal redondeo,
        LocalDateTime fechaHora
        ) {
    public static CompraResumen de(Compra c){
        return new CompraResumen(c.getNumeroComprobante(), c.getComercio().getNombre(), c.getMonto(),
                c.getRedondeo(), c.getFechaHora());
    }
}
