package sv.roundup.api.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sv.roundup.api.entidad.Usuario;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepositorio extends JpaRepository<Usuario, Long> {
    Optional<Usuario>findByUsername(String username);

    @Query("select ur.rol.nombre from UsuarioRol ur " +
            "where ur.usuario.id = :usuarioId and ur.rol.activo = true")
    List<String> rolesDe(@Param("usuarioId") Long usuarioId);
}
