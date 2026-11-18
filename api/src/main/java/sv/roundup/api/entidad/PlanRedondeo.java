package sv.roundup.api.entidad;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "plan_redondeo")
public class PlanRedondeo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 20, nullable = false, unique = true)
    private String codigo;

    @Column(length = 60, nullable = false)
    private String nombre;

    @Column(nullable = false)
    private int multiplicador;

    @Column(name = "tope_mensual", precision = 12, scale = 2, nullable = false)
    private BigDecimal topeMensual;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();

    protected PlanRedondeo(){

    }

    public PlanRedondeo(String codigo, String nombre, int multiplicador, BigDecimal topeMensual) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.multiplicador = multiplicador;
        this.topeMensual = topeMensual;
    }

    public Long getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public int getMultiplicador() {
        return multiplicador;
    }

    public BigDecimal getTopeMensual() {
        return topeMensual;
    }

    public boolean isActivo() {
        return activo;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setMultiplicador(int multiplicador) {
        this.multiplicador = multiplicador;
    }

    public void setTopeMensual(BigDecimal topeMensual) {
        this.topeMensual = topeMensual;
    }
}
