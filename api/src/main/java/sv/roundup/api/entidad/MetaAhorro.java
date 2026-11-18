package sv.roundup.api.entidad;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Table(name = "meta_ahorro")
public class MetaAhorro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cuenta_id", nullable = false)
    private Cuenta cuenta;

    @Column(length = 60, nullable = false)
    private String nombre;

    @Column(name = "monto_objetivo", precision = 12, scale = 2, nullable = false)
    private BigDecimal montoObjetivo;

    @Column(name = "monto_ahorrado", precision = 12, scale = 2, nullable = false)
    private BigDecimal montoAhorrado;

    @Enumerated(EnumType.STRING)
    @Column(length = 12, nullable = false)
    private EstadoMeta estado;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();

    public MetaAhorro(Cuenta cuenta,String nombre, BigDecimal montoObjetivo) {
        this.cuenta = cuenta;
        this.nombre = nombre;
        this.montoObjetivo = montoObjetivo;
        this.estado = EstadoMeta.ACTIVA;
        this.montoAhorrado = BigDecimal.ZERO;
    }

    protected MetaAhorro(){}

    public Long getId() {
        return id;
    }

    public Cuenta getCuenta() {
        return cuenta;
    }

    public String getNombre() {
        return nombre;
    }

    public BigDecimal getMontoObjetivo() {
        return montoObjetivo;
    }

    public BigDecimal getMontoAhorrado() {
        return montoAhorrado;
    }

    public EstadoMeta getEstado() {
        return estado;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }

    public void setMontoAhorrado(BigDecimal montoAhorrado) {
        this.montoAhorrado = montoAhorrado;
    }

    public void setEstado(EstadoMeta estado) {
        this.estado = estado;
    }

    public BigDecimal avance(){
        return montoAhorrado.multiply(new BigDecimal("100")).divide(montoObjetivo, 2, RoundingMode.HALF_UP);
    }
}
