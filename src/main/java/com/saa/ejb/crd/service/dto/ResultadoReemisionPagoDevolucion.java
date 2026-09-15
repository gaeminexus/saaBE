package com.saa.ejb.crd.service.dto;

/**
 * Resultado de POST /rest/dvap/{idDevolucion}/reemitirPago.
 *
 * POJO plano: getters y setters escritos a mano, sin Lombok.
 */
public class ResultadoReemisionPagoDevolucion {

    /** Id de la orden de pago anterior — la que se anuló, o la que ya estaba cerrada. */
    private Long idPagoAnterior;

    /** Id de la orden de pago nueva, generada POR_APROBAR en tesorería. */
    private Long idPagoNuevo;

    /**
     * {@code true} si la orden anterior ya estaba RECHAZADA o ANULADA al llegar (no hubo
     * nada que anular); {@code false} si este método la anuló.
     */
    private boolean ordenAnteriorYaEstabaCerrada;

    /** Estado resultante de la devolución, igual que responde {@code /anular}. */
    private ResultadoDevolucionAporte resultado;

    public ResultadoReemisionPagoDevolucion() {
    }

    public Long getIdPagoAnterior() {
        return idPagoAnterior;
    }

    public void setIdPagoAnterior(Long idPagoAnterior) {
        this.idPagoAnterior = idPagoAnterior;
    }

    public Long getIdPagoNuevo() {
        return idPagoNuevo;
    }

    public void setIdPagoNuevo(Long idPagoNuevo) {
        this.idPagoNuevo = idPagoNuevo;
    }

    public boolean isOrdenAnteriorYaEstabaCerrada() {
        return ordenAnteriorYaEstabaCerrada;
    }

    public void setOrdenAnteriorYaEstabaCerrada(boolean ordenAnteriorYaEstabaCerrada) {
        this.ordenAnteriorYaEstabaCerrada = ordenAnteriorYaEstabaCerrada;
    }

    public ResultadoDevolucionAporte getResultado() {
        return resultado;
    }

    public void setResultado(ResultadoDevolucionAporte resultado) {
        this.resultado = resultado;
    }
}
