package sv.roundup.api.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import sv.roundup.api.entidad.Comercio;

import java.util.Optional;

public interface ComercioRepositorio extends JpaRepository<Comercio, Long>{
    Optional<Comercio> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);
}
