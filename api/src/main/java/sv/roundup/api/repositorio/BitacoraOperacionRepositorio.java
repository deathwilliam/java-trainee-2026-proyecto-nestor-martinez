package sv.roundup.api.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import sv.roundup.api.entidad.BitacoraOperacion;
import sv.roundup.api.entidad.ResultadoOperacion;


public interface BitacoraOperacionRepositorio extends JpaRepository<BitacoraOperacion, Long> {
    long countByResultado(ResultadoOperacion resultado);
}
