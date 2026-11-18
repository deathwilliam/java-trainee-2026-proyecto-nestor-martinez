package sv.roundup.api.servicio;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sv.roundup.api.entidad.*;
import sv.roundup.api.excepcion.OperacionRechazadaException;
import sv.roundup.api.excepcion.SaldoInsuficienteException;
import sv.roundup.api.repositorio.*;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/*
 * Sesion 35: las REGLAS de la compra, probadas SIN base de datos y SIN Spring.
 *
 * Comprueba el caso bueno y cada regla que revisa CompraServicio (R1, R2, R3, R4, R6 y "la cuenta
 * y el comercio tienen que existir"), en milisegundos. Los repositorios son de MENTIRA (mocks) y
 * cada prueba les dice que responder con when(...).thenReturn(...).
 *
 * Que NO comprueba: el ROLLBACK. Aqui no hay Spring, asi que @Transactional no hace nada. Eso lo
 * sigue comprobando CompraServicioTest (@SpringBootTest, con la base de verdad).
 *
 *   @ExtendWith(MockitoExtension.class)  -> JUnit le pide a Mockito que llene los @Mock y el
 *                                           @InjectMocks antes de cada prueba
 *   @Mock         -> un objeto de mentira: no hace nada y devuelve vacios (null, Optional.empty())
 *   @InjectMocks  -> crea el CompraServicio DE VERDAD y le pasa los @Mock por el constructor
 */
@ExtendWith(MockitoExtension.class)
class CompraReglasTest {

    // Uno por cada cosa que recibe el constructor de CompraServicio
    @Mock private CuentaRepositorio cuentas;
    @Mock private ComercioRepositorio comercios;
    @Mock private UsuarioRepositorio usuarios;
    @Mock private CompraRepositorio compras;
    @Mock private BitacoraOperacionRepositorio bitacora;
    @Mock private MetaAhorroRepositorio metas;
    @Mock private BitacoraServicio bitacoraServicio;

    @InjectMocks private CompraServicio compraServicio;

    // ---------------------------------------------------------------- datos en memoria, con new

    // Plan DOBLE: multiplicador 2, tope de $50.00 al mes (como en import.sql)
    private final PlanRedondeo doble = new PlanRedondeo("DOBLE", "Doble", 2, new BigDecimal("50.00"));

    private final Comercio superSelectos = new Comercio("SUPER-SELECTOS", "Super Selectos");
    private final Usuario ana = new Usuario("ana", "hash", "ana@roundup.sv");

    // La cuenta RU-000123 es de Ana: cuenta -> cliente -> usuario "ana" (lo revisa cuenta.esDe)
    private Cuenta cuenta(String saldo) {
        Cliente clienteAna = new Cliente("01234567-8", "Ana", "Martinez", "ana.martinez@correo.com", "7000-0001");
        clienteAna.setUsuario(ana);
        return new Cuenta("RU-000123", clienteAna, null, doble, new BigDecimal(saldo));   // nace ACTIVA
    }

    // Prepara lo que el servicio busca, en el orden en que lo busca: cuenta, comercio y usuario
    private void prepararCuentaComercioYUsuario(Cuenta cuenta) {
        when(cuentas.buscarParaCompra("RU-000123")).thenReturn(Optional.of(cuenta));
        when(comercios.findByCodigo("SUPER-SELECTOS")).thenReturn(Optional.of(superSelectos));
        when(usuarios.findByUsername("ana")).thenReturn(Optional.of(ana));
    }

    // Prepara lo que el servicio necesita para grabar: el redondeo del mes, el comprobante y el save
    private void prepararGrabacion(String redondeoDelMes) {
        when(compras.redondeoDelPeriodo(any(), any(), any())).thenReturn(new BigDecimal(redondeoDelMes));
        when(compras.siguienteComprobante()).thenReturn(4L);
        // save(...) de un mock devuelve null: "devolver el mismo objeto que recibio"
        when(compras.save(any(Compra.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Compra comprar(String monto) {
        return compraServicio.procesar("RU-000123", "SUPER-SELECTOS", new BigDecimal(monto), "ana");
    }

    // ---------------------------------------------------------------- el caso bueno

    @Test
    void compraBuenaCobraElRedondeoYLoAbonaALaMeta() {
        Cuenta cuenta = cuenta("500.00");
        MetaAhorro laptop = new MetaAhorro(cuenta, "Laptop", new BigDecimal("300.00"));
        prepararCuentaComercioYUsuario(cuenta);
        when(metas.findByCuentaIdAndEstado(cuenta.getId(), EstadoMeta.ACTIVA)).thenReturn(Optional.of(laptop));
        prepararGrabacion("0.00");

        Compra compra = comprar("4.30");

        // R3: $4.30 en plan DOBLE -> (5.00 - 4.30) x 2 = $1.40
        assertEquals(new BigDecimal("1.40"), compra.getRedondeo());
        assertEquals("C-000004", compra.getNumeroComprobante());
        // R10: el saldo baja monto + redondeo = $5.70
        assertEquals(new BigDecimal("494.30"), cuenta.getSaldo());
        // R11: la meta sube lo mismo que el redondeo
        assertEquals(new BigDecimal("1.40"), laptop.getMontoAhorrado());
        assertEquals(EstadoMeta.ACTIVA, laptop.getEstado());

        // ArgumentCaptor: "atrapa" lo que el servicio le paso a save(), para revisarlo
        ArgumentCaptor<BitacoraOperacion> fila = ArgumentCaptor.forClass(BitacoraOperacion.class);
        verify(bitacora).save(fila.capture());
        assertEquals(ResultadoOperacion.EXITOSO, fila.getValue().getResultado());
        assertEquals(ana, fila.getValue().getUsuario());
        verify(bitacoraServicio, never()).registrarFallida(anyString(), anyString(), anyString());
    }

    // ---------------------------------------------------------------- el monto: corta antes de ir a la base

    @Test
    void montoCeroSeRechazaSinBuscarLaCuenta() {
        OperacionRechazadaException e = assertThrows(OperacionRechazadaException.class, () -> comprar("0.00"));

        assertEquals("El monto debe de ser mayor a 0", e.getMessage());
        verifyNoInteractions(cuentas, comercios, usuarios, compras, bitacora, metas);
        // El intento fallido SI se anota, en la bitacora aparte (REQUIRES_NEW)
        verify(bitacoraServicio).registrarFallida(eq("RU-000123"), eq("ana"), anyString());
    }

    // ---------------------------------------------------------------- lo que tiene que existir

    @Test
    void cuentaQueNoExisteSeRechaza() {
        when(cuentas.buscarParaCompra("RU-999999")).thenReturn(Optional.empty());

        OperacionRechazadaException e = assertThrows(OperacionRechazadaException.class,
                () -> compraServicio.procesar("RU-999999", "SUPER-SELECTOS", new BigDecimal("4.30"), "ana"));

        assertEquals("No existe la cuenta RU-999999", e.getMessage());
        verify(compras, never()).save(any(Compra.class));
    }

    // ---------------------------------------------------------------- el CLIENTE solo compra desde SUS cuentas

    @Test
    void unaCuentaAjenaSeRechazaAntesDeMirarSuEstado() {
        Cuenta cuentaDeAna = cuenta("500.00");
        when(cuentas.buscarParaCompra("RU-000123")).thenReturn(Optional.of(cuentaDeAna));

        // Luis (su username sale del token) intenta comprar con la cuenta de Ana
        OperacionRechazadaException e = assertThrows(OperacionRechazadaException.class,
                () -> compraServicio.procesar("RU-000123", "SUPER-SELECTOS", new BigDecimal("4.30"), "luis"));

        assertEquals("La cuenta RU-000123 no es de luis", e.getMessage());
        assertEquals(new BigDecimal("500.00"), cuentaDeAna.getSaldo());   // no se toco
        verifyNoInteractions(comercios, usuarios, compras, bitacora, metas);
        verify(bitacoraServicio).registrarFallida(eq("RU-000123"), eq("luis"), anyString());
    }

    @Test
    void comercioQueNoExisteSeRechaza() {
        when(cuentas.buscarParaCompra("RU-000123")).thenReturn(Optional.of(cuenta("500.00")));
        when(comercios.findByCodigo("NO-EXISTE")).thenReturn(Optional.empty());

        OperacionRechazadaException e = assertThrows(OperacionRechazadaException.class,
                () -> compraServicio.procesar("RU-000123", "NO-EXISTE", new BigDecimal("4.30"), "ana"));

        assertEquals("No existe el comercio NO-EXISTE", e.getMessage());
        verify(compras, never()).save(any(Compra.class));
    }

    // ---------------------------------------------------------------- R4: cuenta ACTIVA y comercio activo

    @Test
    void r4CuentaBloqueadaSeRechazaYNoCobraNada() {
        Cuenta cuenta = cuenta("500.00");
        cuenta.setEstado(EstadoCuenta.BLOQUEADA);
        when(cuentas.buscarParaCompra("RU-000123")).thenReturn(Optional.of(cuenta));

        OperacionRechazadaException e = assertThrows(OperacionRechazadaException.class, () -> comprar("4.30"));

        assertEquals("La cuenta RU-000123 esta BLOQUEADA", e.getMessage());
        assertEquals(new BigDecimal("500.00"), cuenta.getSaldo());     // no se toco
        verify(compras, never()).save(any(Compra.class));
        verify(bitacoraServicio).registrarFallida(eq("RU-000123"), eq("ana"), anyString());
    }

    @Test
    void r4ComercioInactivoSeRechaza() {
        superSelectos.setActivo(false);
        when(cuentas.buscarParaCompra("RU-000123")).thenReturn(Optional.of(cuenta("500.00")));
        when(comercios.findByCodigo("SUPER-SELECTOS")).thenReturn(Optional.of(superSelectos));

        OperacionRechazadaException e = assertThrows(OperacionRechazadaException.class, () -> comprar("4.30"));

        assertEquals("El comercio SUPER-SELECTOS no esta activo", e.getMessage());
        verify(compras, never()).save(any(Compra.class));
    }

    // ---------------------------------------------------------------- R1, los dos lados

    @Test
    void r1SaldoQueNoAlcanzaParaMontoMasRedondeoSeRechaza() {
        // $4.30 + $1.40 de redondeo = $5.70, y la cuenta solo tiene $5.69
        Cuenta cuenta = cuenta("5.69");
        prepararCuentaComercioYUsuario(cuenta);
        when(metas.findByCuentaIdAndEstado(cuenta.getId(), EstadoMeta.ACTIVA))
                .thenReturn(Optional.of(new MetaAhorro(cuenta, "Laptop", new BigDecimal("300.00"))));
        when(compras.redondeoDelPeriodo(any(), any(), any())).thenReturn(BigDecimal.ZERO);

        // SaldoInsuficienteException es hija de OperacionRechazadaException: se pide la hija
        SaldoInsuficienteException e = assertThrows(SaldoInsuficienteException.class, () -> comprar("4.30"));

        assertEquals(new BigDecimal("5.70"), e.getNecesita());
        assertEquals(new BigDecimal("5.69"), cuenta.getSaldo());       // no se toco
        verify(compras, never()).save(any(Compra.class));
        verify(bitacora, never()).save(any(BitacoraOperacion.class));
    }

    @Test
    void r1SaldoJustoParaMontoMasRedondeoSiPasa() {
        Cuenta cuenta = cuenta("5.70");
        prepararCuentaComercioYUsuario(cuenta);
        when(metas.findByCuentaIdAndEstado(cuenta.getId(), EstadoMeta.ACTIVA))
                .thenReturn(Optional.of(new MetaAhorro(cuenta, "Laptop", new BigDecimal("300.00"))));
        prepararGrabacion("0.00");

        comprar("4.30");

        assertEquals(new BigDecimal("0.00"), cuenta.getSaldo());       // quedo en cero, no en negativo
    }

    // ---------------------------------------------------------------- R2: el tope mensual recorta, no rechaza

    @Test
    void r2ElRedondeoSeRecortaALoQueFaltaParaElTope() {
        Cuenta cuenta = cuenta("500.00");
        MetaAhorro laptop = new MetaAhorro(cuenta, "Laptop", new BigDecimal("300.00"));
        prepararCuentaComercioYUsuario(cuenta);
        when(metas.findByCuentaIdAndEstado(cuenta.getId(), EstadoMeta.ACTIVA)).thenReturn(Optional.of(laptop));
        prepararGrabacion("49.50");                                     // tope $50.00: faltan $0.50

        Compra compra = comprar("4.30");

        assertEquals(new BigDecimal("0.50"), compra.getRedondeo());     // no $1.40
        assertEquals(new BigDecimal("495.20"), cuenta.getSaldo());      // 500 - 4.30 - 0.50
        assertEquals(new BigDecimal("0.50"), laptop.getMontoAhorrado());
    }

    // ---------------------------------------------------------------- R3: monto exacto

    @Test
    void r3MontoExactoNoTieneRedondeo() {
        Cuenta cuenta = cuenta("500.00");
        MetaAhorro laptop = new MetaAhorro(cuenta, "Laptop", new BigDecimal("300.00"));
        prepararCuentaComercioYUsuario(cuenta);
        when(metas.findByCuentaIdAndEstado(cuenta.getId(), EstadoMeta.ACTIVA)).thenReturn(Optional.of(laptop));
        prepararGrabacion("0.00");

        Compra compra = comprar("5.00");

        assertEquals(0, compra.getRedondeo().compareTo(BigDecimal.ZERO));
        assertEquals(new BigDecimal("495.00"), cuenta.getSaldo());
        assertEquals(BigDecimal.ZERO, laptop.getMontoAhorrado());       // la meta no cambio
    }

    // ---------------------------------------------------------------- R6: la meta

    @Test
    void r6SinMetaActivaSeCobraSinRedondeo() {
        Cuenta cuenta = cuenta("500.00");
        prepararCuentaComercioYUsuario(cuenta);
        when(metas.findByCuentaIdAndEstado(cuenta.getId(), EstadoMeta.ACTIVA)).thenReturn(Optional.empty());
        when(compras.siguienteComprobante()).thenReturn(4L);
        when(compras.save(any(Compra.class))).thenAnswer(inv -> inv.getArgument(0));

        Compra compra = comprar("4.30");

        assertEquals(0, compra.getRedondeo().compareTo(BigDecimal.ZERO));
        assertNull(compra.getMeta());
        assertEquals(new BigDecimal("495.70"), cuenta.getSaldo());      // solo el monto
    }

    @Test
    void r6LaMetaQueLlegaAlObjetivoQuedaCompletada() {
        Cuenta cuenta = cuenta("500.00");
        MetaAhorro casiLista = new MetaAhorro(cuenta, "Audifonos", new BigDecimal("10.00"));
        casiLista.setMontoAhorrado(new BigDecimal("9.00"));
        prepararCuentaComercioYUsuario(cuenta);
        when(metas.findByCuentaIdAndEstado(cuenta.getId(), EstadoMeta.ACTIVA)).thenReturn(Optional.of(casiLista));
        prepararGrabacion("0.00");

        comprar("4.30");

        assertEquals(new BigDecimal("10.40"), casiLista.getMontoAhorrado());   // 9.00 + 1.40
        assertEquals(EstadoMeta.COMPLETADA, casiLista.getEstado());
    }
}
