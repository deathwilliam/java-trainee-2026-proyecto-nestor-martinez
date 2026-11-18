package sv.roundup.api.dto;

import sv.roundup.api.entidad.EstadoCuenta;

public record CuentaEdicion(String codigoPlan, EstadoCuenta estado) {
}
