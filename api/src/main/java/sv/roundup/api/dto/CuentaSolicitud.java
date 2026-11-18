package sv.roundup.api.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CuentaSolicitud(
        @NotBlank(message = "El DUI del cliente es obligatorio")
        @Pattern(regexp = "\\d{8}-\\d", message = "El DUI tiene el formato 00000000-0")
        String duiCliente,
        @NotBlank(message = "El codigo del asesor es obligatorio")
        String codigoAsesor,
        @NotBlank(message = "El codigo del plan es obligatorio")
        String codigoPlan,
        @NotNull(message = "El saldo de apertura es obligatorio")
        @PositiveOrZero(message = "El saldo de apertura no puede ser negativo")
        @Digits(integer = 10, fraction = 2, message = "El saldo de apertura admite como mucho 2 decimales")
        BigDecimal saldoApertura
) {
}
