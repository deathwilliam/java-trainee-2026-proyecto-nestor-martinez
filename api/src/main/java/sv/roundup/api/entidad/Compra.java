package sv.roundup.api.entidad;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "compra")
public class Compra {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_comprobante", length = 20, nullable = false, unique = true)
    private String numeroComprobante;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cuenta_id", nullable = false)
    private Cuenta cuenta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comercio_id",  nullable = false)
    private Comercio comercio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meta_id")
    private MetaAhorro meta;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal monto;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal redondeo;

    @Column(name = "fecha_hora", nullable = false, updatable = false)
    private LocalDateTime fechaHora = LocalDateTime.now();

    public Compra(String numeroComprobante, Cuenta cuenta, Comercio comercio, MetaAhorro meta, BigDecimal monto, BigDecimal redondeo) {
        this.numeroComprobante = numeroComprobante;
        this.cuenta = cuenta;
        this.comercio = comercio;
        this.meta = meta;
        this.monto = monto;
        this.redondeo = redondeo;
    }

    protected Compra(){}

    public Long getId() {
        return id;
    }

    public String getNumeroComprobante() {
        return numeroComprobante;
    }

    public Cuenta getCuenta() {
        return cuenta;
    }

    public Comercio getComercio() {
        return comercio;
    }

    public MetaAhorro getMeta() {
        return meta;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public BigDecimal getRedondeo() {
        return redondeo;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }
}
