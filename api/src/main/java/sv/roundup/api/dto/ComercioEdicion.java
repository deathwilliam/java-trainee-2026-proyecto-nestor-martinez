package sv.roundup.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ComercioEdicion(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 80, message = "El nombre admite como maximo 80 caracteres")
        String nombre
) {
}
