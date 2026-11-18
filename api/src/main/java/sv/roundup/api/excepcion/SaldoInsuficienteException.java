package sv.roundup.api.excepcion;

import java.math.BigDecimal;

/*
 * R1: la cuenta no alcanza para pagar el monto mas el redondeo.
 * Es hija de OperacionRechazadaException: quien atrapa a la madre, atrapa tambien a esta.
 * Guarda lo que hacia falta y lo que habia, para que quien la atrape pueda mostrarlo.
 */
public class SaldoInsuficienteException extends OperacionRechazadaException {

    private final BigDecimal necesita;
    private final BigDecimal saldo;

    public SaldoInsuficienteException(String numeroCuenta, BigDecimal necesita, BigDecimal saldo) {
        super("Saldo insuficiente: la compra necesita $" + necesita + " y la cuenta " + numeroCuenta
                + " tiene $" + saldo);
        this.necesita = necesita;
        this.saldo = saldo;
    }

    public BigDecimal getNecesita() {
        return necesita;
    }

    public BigDecimal getSaldo() {
        return saldo;
    }
}
