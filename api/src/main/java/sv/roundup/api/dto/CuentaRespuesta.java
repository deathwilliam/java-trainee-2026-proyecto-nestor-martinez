package sv.roundup.api.dto;

import sv.roundup.api.entidad.Cuenta;
import sv.roundup.api.entidad.EstadoCuenta;

import java.math.BigDecimal;

public record CuentaRespuesta(
        String numeroCuenta,
        String cliente,
        String plan,
        EstadoCuenta estado,
        BigDecimal saldo) {

    public static CuentaRespuesta de(Cuenta c){
        return new CuentaRespuesta(c.getNumeroCuenta(),
                c.getCliente().getNombres() + " " + c.getCliente().getApellidos(),
                c.getPlanRedondeo().getCodigo(),
                c.getEstado(), c.getSaldo());
    }
}
