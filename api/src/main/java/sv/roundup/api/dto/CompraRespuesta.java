package sv.roundup.api.dto;

import sv.roundup.api.entidad.Compra;
import sv.roundup.api.entidad.EstadoMeta;
import sv.roundup.api.entidad.MetaAhorro;

import java.math.BigDecimal;

public record CompraRespuesta(
        String numeroComprobante,
        String numeroCuenta,
        String comercio,
        BigDecimal monto,
        BigDecimal redondeo,
        BigDecimal totalCobrado,
        BigDecimal saldoNuevo,
        String meta,
        BigDecimal avanceMeta,
        Boolean metaCompletada) {

    public static CompraRespuesta de(Compra c){
        MetaAhorro m = c.getMeta();
        BigDecimal avance = null;

        if (m != null){
            avance = m.avance();
        }
        return new CompraRespuesta(c.getNumeroComprobante(), c.getCuenta().getNumeroCuenta(),
                c.getComercio().getNombre(), c.getMonto(), c.getRedondeo(),
                c.getMonto().add(c.getRedondeo()), c.getCuenta().getSaldo(),
                m == null ? null : m.getNombre(), avance,
                m == null ? null : m.getEstado() == EstadoMeta.COMPLETADA);
    }
}
