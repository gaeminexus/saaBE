package com.saa.ejb.crd.service.dto;

/**
 * Respuesta de {@code GET /rest/prst/compromisoGarante/{idGarante}?monto=}: aviso (no bloquea nada) de si pagar con los
 * aportes del garante lo deja por debajo de lo que tiene comprometido en sus propios préstamos. Ver
 * DISENO-PAGO-CON-APORTES-DEL-GARANTE.md §G5.
 */
public class ResultadoCompromisoGarante {

    /** Σ del saldo disponible de los tipos de aporte del garante (la misma fuente que {@code saldosPorEntidad}). */
    private double saldoAportes;

    /** Capital pendiente de los préstamos vigentes PQ/PE del garante ({@code saldoCapitalCupo} del otorgamiento). */
    private double capitalComprometido;

    /** {@code saldoAportes - monto}. */
    private double saldoDespues;

    /** {@code saldoDespues < capitalComprometido}. */
    private boolean quedaDescubierto;

    /** Texto listo para mostrar; {@code null} cuando no queda descubierto. */
    private String mensaje;

    public ResultadoCompromisoGarante() {
    }

    public double getSaldoAportes() {
        return saldoAportes;
    }

    public void setSaldoAportes(double saldoAportes) {
        this.saldoAportes = saldoAportes;
    }

    public double getCapitalComprometido() {
        return capitalComprometido;
    }

    public void setCapitalComprometido(double capitalComprometido) {
        this.capitalComprometido = capitalComprometido;
    }

    public double getSaldoDespues() {
        return saldoDespues;
    }

    public void setSaldoDespues(double saldoDespues) {
        this.saldoDespues = saldoDespues;
    }

    public boolean isQuedaDescubierto() {
        return quedaDescubierto;
    }

    public void setQuedaDescubierto(boolean quedaDescubierto) {
        this.quedaDescubierto = quedaDescubierto;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }
}
