package sv.roundup.api.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sv.roundup.api.entidad.Compra;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface CompraRepositorio extends JpaRepository<Compra, Long> {

    @Query("select coalesce(sum(c.redondeo), 0) from Compra c " +
            "where c.cuenta.id = :cuentaId and c.fechaHora >= :desde and c.fechaHora < :hasta")
    BigDecimal redondeoDelPeriodo(@Param("cuentaId") Long cuentaId,
                                  @Param("desde")LocalDateTime desde,
                                  @Param("hasta") LocalDateTime hasta);

    @Query(value = "select nextval('compra_comprobante_seq')", nativeQuery = true)
    Long siguienteComprobante();

    @Query("select c from Compra c join fetch c.comercio where c.cuenta.numeroCuenta" +
            " = :numeroCuenta order by c.fechaHora desc")
    List<Compra> deLaCuenta(@Param("numeroCuenta") String numeroCuenta);
}
