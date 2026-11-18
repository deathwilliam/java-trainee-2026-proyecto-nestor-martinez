package sv.roundup.api.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CompraSolicitud(
        @NotBlank(message = "el numero de cuenta es obligatorio") String numeroCuenta,
        @NotBlank(message = "el codigo de comercio es obligatorio") String codigoComercio,
        @NotNull(message = "el monto es obligatorio")
        @Positive(message = "el monto debe ser mayor a $0.00")
        @Digits(integer = 10, fraction = 2, message = "El monto admite como mucho 2 decimales")
        BigDecimal monto) {
}
