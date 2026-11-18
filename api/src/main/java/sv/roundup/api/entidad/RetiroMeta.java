package sv.roundup.api.entidad;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "retiro_meta")
public class RetiroMeta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_comprobante", length = 20, unique = true, nullable = false)
    private String numeroComprobante;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meta_id", unique = true, nullable = false)
    private MetaAhorro meta;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal monto;

    @Column(name = "fecha_hora", nullable = false, updatable = false)
    private LocalDateTime fechaHora = LocalDateTime.now();

    public RetiroMeta(String numeroComprobante, MetaAhorro meta, BigDecimal monto) {
        this.numeroComprobante = numeroComprobante;
        this.meta = meta;
        this.monto = monto;
    }

    protected RetiroMeta(){}

    public Long getId() {
        return id;
    }

    public String getNumeroComprobante() {
        return numeroComprobante;
    }

    public MetaAhorro getMeta() {
        return meta;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

}

