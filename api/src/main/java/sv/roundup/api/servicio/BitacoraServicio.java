package sv.roundup.api.servicio;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import sv.roundup.api.entidad.BitacoraOperacion;
import sv.roundup.api.entidad.Cuenta;
import sv.roundup.api.entidad.ResultadoOperacion;
import sv.roundup.api.entidad.TipoOperacion;
import sv.roundup.api.entidad.Usuario;
import sv.roundup.api.repositorio.BitacoraOperacionRepositorio;
import sv.roundup.api.repositorio.CuentaRepositorio;
import sv.roundup.api.repositorio.UsuarioRepositorio;

import java.util.Optional;

/*
 * Quien escribe el intento FALLIDO en la bitacora. Es una clase APARTE de CompraServicio, y eso
 * es lo que hace funcionar el REQUIRES_NEW: @Transactional lo cumple el proxy de Spring, y una
 * llamada desde OTRA clase pasa por el proxy; una llamada this.metodo() de la misma clase, no.
 */
@Service
public class BitacoraServicio {

    private final BitacoraOperacionRepositorio bitacora;
    private final CuentaRepositorio cuentas;
    private final UsuarioRepositorio usuarios;

    public BitacoraServicio(BitacoraOperacionRepositorio bitacora, CuentaRepositorio cuentas,
                            UsuarioRepositorio usuarios) {
        this.bitacora = bitacora;
        this.cuentas = cuentas;
        this.usuarios = usuarios;
    }

    // REQUIRES_NEW: pausa la transaccion de la compra, abre una NUEVA solo para esto, hace su
    // INSERT y su COMMIT, y vuelve. La compra hace su ROLLBACK, pero la FALLIDO ya quedo.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarFallida(String numeroCuenta, String username, String detalle) {
        Optional<Cuenta> cuenta = cuentas.findByNumeroCuenta(numeroCuenta);
        Optional<Usuario> usuario = usuarios.findByUsername(username);

        // usuario_id y cuenta_id son NOT NULL: si alguno no existe, no hay a que apuntar
        if (cuenta.isEmpty() || usuario.isEmpty()) {
            return;
        }
        bitacora.save(new BitacoraOperacion(usuario.get(), cuenta.get(), TipoOperacion.COMPRA,
                ResultadoOperacion.FALLIDO, detalle));
    }
}