package sv.roundup.api.repositorio;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sv.roundup.api.entidad.Cuenta;

import java.util.List;
import java.util.Optional;

public interface CuentaRepositorio extends JpaRepository<Cuenta, Long>{
    Optional<Cuenta> findByNumeroCuenta(String numeroCuenta);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Cuenta c where c.numeroCuenta = :numeroCuenta")
    Optional<Cuenta> buscarParaCompra(@Param("numeroCuenta") String numeroCuenta);

    @Query("select c from Cuenta c join fetch c.cliente join fetch c.planRedondeo" +
            " where c.numeroCuenta= :numeroCuenta")
    Optional<Cuenta> buscarConDetalle(@Param("numeroCuenta") String numeroCuenta);

    boolean existsByNumeroCuenta(String numeroCuenta);

    @Query("select c from Cuenta c join fetch c.cliente join fetch " +
            "c.planRedondeo order by c.numeroCuenta")
    List<Cuenta> listarConDetalle();

    @Query(value = "select nextval('cuenta_numero_seq')", nativeQuery = true)
    Long siguienteNumero();
}
