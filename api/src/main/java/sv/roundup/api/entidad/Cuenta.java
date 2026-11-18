package sv.roundup.api.entidad;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cuenta")
public class Cuenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_cuenta", length = 12, nullable = false, unique = true)
    private String numeroCuenta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asesor_id", nullable = false)
    private Asesor asesor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_redondeo_id", nullable = false)
    private PlanRedondeo planRedondeo;

    @Enumerated(EnumType.STRING)
    @Column(length = 10, nullable = false)
    private EstadoCuenta estado;

    @Column(name = "saldo_apertura", precision = 12, scale = 2, nullable = false)
    private BigDecimal saldoApertura;

    @Column(precision = 12, scale = 2,nullable = false)
    private BigDecimal saldo;

    @Column(name = "fecha_apertura", nullable = false, updatable = false)
    private LocalDateTime fechaApertura = LocalDateTime.now();

    public Cuenta(String numeroCuenta, Cliente cliente, Asesor asesor, PlanRedondeo planRedondeo, BigDecimal saldoApertura) {
        this.numeroCuenta = numeroCuenta;
        this.cliente = cliente;
        this.asesor = asesor;
        this.planRedondeo = planRedondeo;
        this.saldoApertura = saldoApertura;
        this.saldo = saldoApertura;
        this.estado = EstadoCuenta.ACTIVA;
    }

    protected Cuenta(){}

    public Long getId() {
        return id;
    }

    public String getNumeroCuenta() {
        return numeroCuenta;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public Asesor getAsesor() {
        return asesor;
    }

    public PlanRedondeo getPlanRedondeo() {
        return planRedondeo;
    }

    public EstadoCuenta getEstado() {
        return estado;
    }

    public BigDecimal getSaldoApertura() {
        return saldoApertura;
    }

    public BigDecimal getSaldo() {
        return saldo;
    }

    public LocalDateTime getFechaApertura() {
        return fechaApertura;
    }

    public void setPlanRedondeo(PlanRedondeo planRedondeo) {
        this.planRedondeo = planRedondeo;
    }

    public void setEstado(EstadoCuenta estado) {
        this.estado = estado;
    }

    public void setSaldo(BigDecimal saldo) {
        this.saldo = saldo;
    }

    //Verfica que el usuario sea cliente, caso contrario no puede operar como CLIENTE
    public boolean esDe(String username){
        return cliente != null && cliente.getUsuario()!= null
                && cliente.getUsuario().getUsername().equals(username);
    }
}
