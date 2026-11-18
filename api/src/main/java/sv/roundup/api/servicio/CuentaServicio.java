package sv.roundup.api.servicio;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.roundup.api.entidad.*;
import sv.roundup.api.excepcion.OperacionRechazadaException;
import sv.roundup.api.repositorio.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class CuentaServicio {
    private final CuentaRepositorio cuentaRepositorio;
    private final CompraRepositorio compraRepositorio;
    private final MetaAhorroRepositorio metaAhorroRepositorio;
    private final ClienteRepositorio clienteRepositorio;
    private final AsesorRepositorio asesorRepositorio;
    private final PlanRedondeoRepositorio planRedondeoRepositorio;

    public CuentaServicio(CuentaRepositorio cuentaRepositorio,
                          CompraRepositorio compraRepositorio,
                          MetaAhorroRepositorio metaAhorroRepositorio,
                          ClienteRepositorio clienteRepositorio,
                          AsesorRepositorio asesorRepositorio,
                          PlanRedondeoRepositorio planRedondeoRepositorio) {
        this.cuentaRepositorio = cuentaRepositorio;
        this.compraRepositorio = compraRepositorio;
        this.metaAhorroRepositorio = metaAhorroRepositorio;
        this.clienteRepositorio = clienteRepositorio;
        this.asesorRepositorio = asesorRepositorio;
        this.planRedondeoRepositorio = planRedondeoRepositorio;
    }

    public Optional<Cuenta> porNumero(String numeroCuenta){
        return cuentaRepositorio.buscarConDetalle(numeroCuenta);
    }

    public boolean existe(String numeroCuenta){return cuentaRepositorio.existsByNumeroCuenta(numeroCuenta);}

    public List<Compra> comprasDe(String numeroCuenta){return compraRepositorio.deLaCuenta(numeroCuenta);}

    public List<MetaAhorro> metasDe(String numeroCuenta){return metaAhorroRepositorio.findByCuentaNumeroCuentaOrderByCreadoEnDesc(numeroCuenta);}


    public List<Cuenta> listar(){ return cuentaRepositorio.listarConDetalle(); }

    @Transactional
    public Cuenta abrir(String dui, String codigoAsesor, String codigoPlan, BigDecimal saldoApertura){
        Cliente cliente = clienteRepositorio.findByDui(dui)
                .orElseThrow(() -> new OperacionRechazadaException("No existe un cliente con DUI " + dui));
        Asesor asesor = asesorRepositorio.findByCodigoEmpleado(codigoAsesor)
                .orElseThrow(() -> new OperacionRechazadaException("No existe el asesor " + codigoAsesor));
        PlanRedondeo plan = planActivo(codigoPlan);

        String numero = String.format("RU-%06d", cuentaRepositorio.siguienteNumero());
        return cuentaRepositorio.save(new Cuenta(numero, cliente, asesor, plan, saldoApertura));
    }

    @Transactional
    public boolean editar(String numeroCuenta, String codigoPlan, EstadoCuenta estado){
        Optional<Cuenta> encontrada = cuentaRepositorio.buscarParaCompra(numeroCuenta);
        if (encontrada.isEmpty()){
            return false;
        }
        Cuenta cuenta = encontrada.get();

        if (cuenta.getEstado() == EstadoCuenta.CERRADA){
            throw new OperacionRechazadaException("La cuenta " + numeroCuenta + " esta CERRADA: ya no se modifica");
        }
        if (estado == EstadoCuenta.CERRADA){
            throw new OperacionRechazadaException("Para cerrar la cuenta use DELETE /api/cuentas/" + numeroCuenta);
        }
        if (codigoPlan != null){
            cuenta.setPlanRedondeo(planActivo(codigoPlan));
        }
        if (estado != null){
            cuenta.setEstado(estado);
        }
        return true;
    }

    @Transactional
    public boolean cerrar(String numeroCuenta){
        Optional<Cuenta> cuenta = cuentaRepositorio.buscarParaCompra(numeroCuenta);
        cuenta.ifPresent(c -> c.setEstado(EstadoCuenta.CERRADA));
        return cuenta.isPresent();
    }

    @Transactional
    public Optional<MetaAhorro> crearMeta(String numeroCuenta, String nombre, BigDecimal montoObjetivo, String username)
    {
        Optional<Cuenta> encontrada = cuentaRepositorio.findByNumeroCuenta(numeroCuenta);
        if(encontrada.isEmpty()){
            return Optional.empty();
        }

        Cuenta cuenta = encontrada.get();

        if (!cuenta.esDe(username)){
            throw new OperacionRechazadaException("La cuenta " + numeroCuenta + " no es de " + username);
        }

        if (cuenta.getEstado() != EstadoCuenta.ACTIVA){
            throw new OperacionRechazadaException("La cuenta " + numeroCuenta + " esta " + cuenta.getEstado());
        }
        metaAhorroRepositorio.findByCuentaIdAndEstado(cuenta.getId(), EstadoMeta.ACTIVA)
                .ifPresent(m -> {
                    throw new OperacionRechazadaException("Ya tienes una meta ACTIVA: " + m.getNombre());
                });

        return Optional.of(metaAhorroRepositorio.save(new MetaAhorro(cuenta, nombre, montoObjetivo)));
    }

    private PlanRedondeo planActivo(String codigoPlan) {
        PlanRedondeo plan = planRedondeoRepositorio.findByCodigo(codigoPlan)
                .orElseThrow(() -> new OperacionRechazadaException("No existe el plan " + codigoPlan));
        if (!plan.isActivo()) {
            throw new OperacionRechazadaException("El plan " + codigoPlan + " no esta activo");
        }
        return plan;
    }
}
