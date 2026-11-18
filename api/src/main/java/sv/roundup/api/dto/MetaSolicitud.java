package sv.roundup.api.dto;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;


public record MetaSolicitud(
        @NotBlank(message = "El nombre de la meta es obligatorio")
        @Size(max = 60, message = "El nombre admite como maximo 60 caracteres")
        String nombre,
        @NotNull(message = "El monto objetivo es obligatorio")
        @Positive(message = "El monto objetivo debe ser mayor a $0.00")
        @Digits(integer = 10, fraction = 2, message = "El monto objetivo admite como mucho 2 decimales")
        BigDecimal montoObjetivo
) {
}
