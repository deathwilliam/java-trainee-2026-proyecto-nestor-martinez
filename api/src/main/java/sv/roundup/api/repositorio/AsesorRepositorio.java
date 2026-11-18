package sv.roundup.api.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import sv.roundup.api.entidad.Asesor;

import java.util.Optional;

public interface AsesorRepositorio extends JpaRepository<Asesor, Long> {
    Optional<Asesor> findByCodigoEmpleado(String codigoEmpleado);
}
