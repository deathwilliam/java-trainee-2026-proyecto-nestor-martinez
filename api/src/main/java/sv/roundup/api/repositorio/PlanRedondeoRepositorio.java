package sv.roundup.api.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import sv.roundup.api.entidad.PlanRedondeo;

import java.util.Optional;

public interface PlanRedondeoRepositorio extends JpaRepository<PlanRedondeo, Long> {

    Optional<PlanRedondeo> findByCodigo(String codigo);
}
