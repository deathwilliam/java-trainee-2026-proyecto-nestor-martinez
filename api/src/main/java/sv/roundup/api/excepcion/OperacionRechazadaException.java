package sv.roundup.api.excepcion;

/*
 * Una regla de negocio dijo que no: la operacion no se hace.
 *
 * Extiende RuntimeException A PROPOSITO: @Transactional deshace la transaccion
 * (ROLLBACK) cuando el metodo sale con una RuntimeException. Con una excepcion
 * chequeada (extends Exception), Spring haria COMMIT de lo que alcanzo a hacer.
 */
public class OperacionRechazadaException extends RuntimeException {

    public OperacionRechazadaException(String mensaje) {
        super(mensaje);
    }
}
