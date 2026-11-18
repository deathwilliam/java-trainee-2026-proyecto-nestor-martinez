package sv.roundup.api.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import sv.roundup.api.entidad.EstadoMeta;
import sv.roundup.api.entidad.MetaAhorro;

import java.util.List;
import java.util.Optional;

public interface MetaAhorroRepositorio extends JpaRepository<MetaAhorro, Long> {
    Optional<MetaAhorro> findByCuentaIdAndEstado(Long cuentaId, EstadoMeta estado);

    boolean existsByCuentaIdAndEstado(Long cuentaId, EstadoMeta estado);

    List<MetaAhorro> findByCuentaNumeroCuentaOrderByCreadoEnDesc(String numeroCuenta);
}
