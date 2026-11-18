package sv.roundup.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ComercioSolicitud(
        @NotBlank(message = "El codigo es obligatorio")
        @Size(max = 20, message = "El codigo contiene como maximo 20 caracteres")
        String codigo,
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 80, message = "El nombre contiene como maximo 80 caracteres")
        String nombre
) {
}
