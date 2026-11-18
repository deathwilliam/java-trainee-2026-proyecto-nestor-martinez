package sv.roundup.api.entidad;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "bitacora_operacion")
public class BitacoraOperacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cuenta_id", nullable = false)
    private Cuenta cuenta;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_operacion", length = 20, nullable = false)
    private TipoOperacion tipoOperacion;

    @Enumerated(EnumType.STRING)
    @Column(length = 10, nullable = false)
    private ResultadoOperacion resultado;

    @Column(length = 255, nullable = false)
    private String detalle;

    @Column(name = "fecha_hora", nullable = false, updatable = false)
    private LocalDateTime fechaHora = LocalDateTime.now();

    public BitacoraOperacion(Usuario usuario, Cuenta cuenta, TipoOperacion tipoOperacion, ResultadoOperacion resultado, String detalle) {
        this.usuario = usuario;
        this.cuenta = cuenta;
        this.tipoOperacion = tipoOperacion;
        this.resultado = resultado;
        this.detalle = detalle;
    }

    protected BitacoraOperacion(){}

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public Cuenta getCuenta() {
        return cuenta;
    }

    public TipoOperacion getTipoOperacion() {
        return tipoOperacion;
    }

    public ResultadoOperacion getResultado() {
        return resultado;
    }

    public String getDetalle() {
        return detalle;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }
}
