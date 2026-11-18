package sv.roundup.api.servicio;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import sv.roundup.api.entidad.*;
import sv.roundup.api.excepcion.SaldoInsuficienteException;
import sv.roundup.api.repositorio.BitacoraOperacionRepositorio;
import sv.roundup.api.repositorio.CompraRepositorio;
import sv.roundup.api.repositorio.CuentaRepositorio;
import sv.roundup.api.repositorio.MetaAhorroRepositorio;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

/*
 * La prueba que pide la Etapa 2: si procesar una compra falla A LA MITAD, no queda nada.
 *
 * @SpringBootTest: arranca la aplicacion entera contra una base de verdad, pero APARTE
 * (roundup_test) con ddl-auto=create: cada corrida la borra y la llena con import.sql.
 *
 * La clase NO lleva @Transactional: tiene que mirar la base DESPUES del rollback,
 * desde afuera de la transaccion del servicio.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:5432/roundup_test",
        "spring.jpa.hibernate.ddl-auto=create",
        "jwt.clave=clave-solo-para-las-pruebas-automaticas-de-roundup"
})
class CompraServicioTest {

    @Autowired
    private CompraServicio compraServicio;

    @Autowired
    private CuentaRepositorio cuentas;

    @Autowired
    private MetaAhorroRepositorio metas;

    @Autowired
    private CompraRepositorio compras;

    // @MockitoSpyBean: el repositorio de verdad, "espiado". Hace lo de siempre,
    // salvo lo que la prueba le diga que haga distinto: aqui, fallar al grabar la bitacora.
    @MockitoSpyBean
    private BitacoraOperacionRepositorio bitacora;

    @Test
    void siFallaALaMitadNoQuedaNada() {
        BigDecimal saldoAntes = saldo("RU-000123");
        BigDecimal ahorradoAntes = ahorradoEnLaMeta("RU-000123");
        long comprasAntes = compras.count();
        long bitacoraAntes = bitacora.count();

        // La bitacora es el cambio 4, el ULTIMO: cuando falla, la compra ya se grabo,
        // el saldo ya bajo y la meta ya subio. Es la caida a la mitad.
        doThrow(new IllegalStateException("Falla simulada al grabar la bitacora"))
                .when(bitacora).save(any(BitacoraOperacion.class));

        assertThrows(IllegalStateException.class,
                () -> compraServicio.procesar("RU-000123", "SUPER-SELECTOS", new BigDecimal("4.30"), "ana"));

        // Nada quedo: ni el saldo rebajado, ni el redondeo en la meta, ni la compra suelta
        assertEquals(0, saldoAntes.compareTo(saldo("RU-000123")), "el saldo de la cuenta cambio");
        assertEquals(0, ahorradoAntes.compareTo(ahorradoEnLaMeta("RU-000123")), "el monto_ahorrado de la meta cambio");
        assertEquals(comprasAntes, compras.count(), "quedo una compra suelta");
        assertEquals(bitacoraAntes, bitacora.count(), "quedo una fila de bitacora");
    }

    @Test
    void siUnaReglaDiceQueNoNoSeTocaNada() {
        BigDecimal saldoAntes = saldo("RU-000124");
        long comprasAntes = compras.count();

        // RU-000124 tiene $3.20: R1 la rechaza (4.20 + 0.80 de redondeo = 5.00) antes de cambiar nada
        assertThrows(SaldoInsuficienteException.class,
                () -> compraServicio.procesar("RU-000124", "SUPER-SELECTOS", new BigDecimal("4.20"), "luis"));

        assertEquals(0, saldoAntes.compareTo(saldo("RU-000124")));
        assertEquals(comprasAntes, compras.count());
    }

    @Test
    void siTodoSaleBienQuedanTodasLasPartes() {
        BigDecimal saldoAntes = saldo("RU-000123");
        BigDecimal ahorradoAntes = ahorradoEnLaMeta("RU-000123");
        long comprasAntes = compras.count();
        long bitacoraAntes = bitacora.count();

        // R3: $4.30 en plan DOBLE -> (5.00 - 4.30) x 2 = 1.40
        Compra compra = compraServicio.procesar("RU-000123", "SUPER-SELECTOS", new BigDecimal("4.30"), "ana");

        assertEquals(0, new BigDecimal("1.40").compareTo(compra.getRedondeo()), "el redondeo no es el de R3");
        assertEquals(0, saldoAntes.subtract(new BigDecimal("5.70")).compareTo(saldo("RU-000123")), "R10: el saldo no bajo 5.70");
        assertEquals(0, ahorradoAntes.add(new BigDecimal("1.40")).compareTo(ahorradoEnLaMeta("RU-000123")), "R11: la meta no subio 1.40");
        assertEquals(comprasAntes + 1, compras.count(), "falta la compra");
        assertEquals(bitacoraAntes + 1, bitacora.count(), "falta la bitacora");
    }

    // compareTo y no equals: con BigDecimal, 1.4 y 1.40 no son "equals"
    private BigDecimal saldo(String numeroCuenta) {
        return cuentas.findByNumeroCuenta(numeroCuenta).orElseThrow().getSaldo();
    }

    private BigDecimal ahorradoEnLaMeta(String numeroCuenta) {
        Cuenta cuenta = cuentas.findByNumeroCuenta(numeroCuenta).orElseThrow();
        return metas.findByCuentaIdAndEstado(cuenta.getId(), EstadoMeta.ACTIVA).orElseThrow().getMontoAhorrado();
    }

    @Test
    void siUnaReglaDiceQueNoQuedaElFallo(){
        long fallidosAntes = bitacora.countByResultado(ResultadoOperacion.FALLIDO);
        long exitosoAntes = bitacora.countByResultado(ResultadoOperacion.EXITOSO);
        BigDecimal saldoAntes = saldo("RU-000124");
        long comprasAntes = compras.count();

        assertThrows(SaldoInsuficienteException.class,
                () -> compraServicio.procesar("RU-000124", "SUPER-SELECTOS", new BigDecimal("4.20"), "luis"));

        assertEquals(fallidosAntes + 1, bitacora.countByResultado(ResultadoOperacion.FALLIDO),
                "el intento fallido no quedo en la bitacora");

        assertEquals(exitosoAntes, bitacora.countByResultado(ResultadoOperacion.EXITOSO),
                "quedo un EXITOSO de una compra rechazada");

        assertEquals(0, saldoAntes.compareTo(saldo("RU-000124")), "el saldo cambio");
        assertEquals(comprasAntes, compras.count(), "quedo una compra suelta");
    }


}