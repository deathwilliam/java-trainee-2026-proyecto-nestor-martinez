package sv.roundup.api.dto;

import sv.roundup.api.entidad.Comercio;

public record ComercioRespuesta(
        String codigo,
        String nombre,
        boolean activo
) {
    public static ComercioRespuesta de(Comercio c){
        return new ComercioRespuesta(c.getCodigo(), c.getNombre(), c.isActivo());
    }
}
