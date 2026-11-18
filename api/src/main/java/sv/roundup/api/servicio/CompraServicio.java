package sv.roundup.api.servicio;

import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.roundup.api.entidad.*;
import sv.roundup.api.excepcion.OperacionRechazadaException;
import sv.roundup.api.excepcion.SaldoInsuficienteException;
import sv.roundup.api.repositorio.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.slf4j.Logger;

@Service
public class CompraServicio {
    private static final Logger log = LoggerFactory.getLogger(CompraServicio.class);

    private final CuentaRepositorio cuentas;
    private final ComercioRepositorio comercios;
    private final UsuarioRepositorio usuarios;
    private final CompraRepositorio compras;
    private final BitacoraOperacionRepositorio bitacora;
    private final MetaAhorroRepositorio metas;
    private final BitacoraServicio bitacoraServicio;

    public CompraServicio(CuentaRepositorio cuentas, ComercioRepositorio comercios, UsuarioRepositorio usuarios, CompraRepositorio compras, BitacoraOperacionRepositorio bitacora, MetaAhorroRepositorio metas, BitacoraServicio bitacoraServicio) {
        this.cuentas = cuentas;
        this.comercios = comercios;
        this.usuarios = usuarios;
        this.compras = compras;
        this.bitacora = bitacora;
        this.metas = metas;
        this.bitacoraServicio = bitacoraServicio;
    }

    @Transactional
    public Compra procesar(String numeroCuenta, String codigoComercio, BigDecimal monto,
                           String username){
        try {
            Compra compra = ejecutar(numeroCuenta, codigoComercio, monto, username);
            log.info("Compra realizada: {} por ${} en {} (cuenta {}, usuario {}), redondeo ${}",
                    compra.getNumeroComprobante(), monto, codigoComercio, numeroCuenta, username, compra.getRedondeo());
            return compra;
        }catch (RuntimeException e){
            bitacoraServicio.registrarFallida(numeroCuenta, username, "Compra por $" + monto + " en " + codigoComercio + ": " +
                    e.getMessage());

            log.warn("Compra rechazada: ${} en {} (cuenta {}, usuario {}): {}",
                    monto, codigoComercio, numeroCuenta, username, e.getMessage());
            throw e;
        }
    }


    private Compra ejecutar(String numeroCuenta, String codigoComercio, BigDecimal monto, String username){
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0){
            throw new OperacionRechazadaException("El monto debe de ser mayor a 0");
        }

        Cuenta cuenta = cuentas.buscarParaCompra(numeroCuenta)
                .orElseThrow(() -> new OperacionRechazadaException("No existe la cuenta " + numeroCuenta));

        if (!cuenta.esDe(username)){
            throw new OperacionRechazadaException("La cuenta " + numeroCuenta + " no es de " + username);
        }

        if (cuenta.getEstado() != EstadoCuenta.ACTIVA){
            throw new OperacionRechazadaException("La cuenta " + numeroCuenta + " esta " + cuenta.getEstado());
        }

        Comercio comercio = comercios.findByCodigo(codigoComercio)
                .orElseThrow(() -> new OperacionRechazadaException("No existe el comercio " + codigoComercio));

        if (!comercio.isActivo()){
            throw new OperacionRechazadaException("El comercio " + codigoComercio + " no esta activo");
        }

        Usuario usuario = usuarios.findByUsername(username)
                .orElseThrow(() -> new OperacionRechazadaException("El usuario " + username + " no existe"));

        MetaAhorro meta = metas.findByCuentaIdAndEstado(cuenta.getId(), EstadoMeta.ACTIVA).orElse(null);

        BigDecimal redondeo = BigDecimal.ZERO;
        if (meta != null){
            BigDecimal siguienteDolar = monto.setScale(0, RoundingMode.CEILING);
            BigDecimal multiplicador = BigDecimal.valueOf(cuenta.getPlanRedondeo().getMultiplicador());
            redondeo = siguienteDolar.subtract(monto).multiply(multiplicador);

            LocalDateTime desde = LocalDate.now().withDayOfMonth(1).atStartOfDay();
            LocalDateTime hasta = desde.plusMonths(1);
            BigDecimal llevaEnElMes = compras.redondeoDelPeriodo(cuenta.getId(), desde, hasta);
            BigDecimal disponible = cuenta.getPlanRedondeo().getTopeMensual().subtract(llevaEnElMes).max(BigDecimal.ZERO);
            redondeo = redondeo.min(disponible);
        }

        BigDecimal total = monto.add(redondeo);
        if (cuenta.getSaldo().compareTo(total) < 0){
            throw new SaldoInsuficienteException(numeroCuenta, total, cuenta.getSaldo());
        }

        String numeroComprobante = String.format("C-%06d", compras.siguienteComprobante());
        Compra compra = compras.save(new Compra(numeroComprobante, cuenta, comercio, meta, monto, redondeo));

        cuenta.setSaldo(cuenta.getSaldo().subtract(total));

        if (meta != null && redondeo.compareTo(BigDecimal.ZERO) > 0){
            meta.setMontoAhorrado(meta.getMontoAhorrado().add(redondeo));
            if (meta.getMontoAhorrado().compareTo(meta.getMontoObjetivo()) >= 0){
                meta.setEstado(EstadoMeta.COMPLETADA);
            }
        }

        bitacora.save(new BitacoraOperacion(usuario, cuenta, TipoOperacion.COMPRA, ResultadoOperacion.EXITOSO, "Compra " + numeroComprobante + " por $" + monto + " en " + codigoComercio + ", redondeo $" + redondeo));

        return compra;
    }
}
