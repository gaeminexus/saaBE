package com.saa.ejb.crd.service.dto;

/**
 * Resultado de {@link com.saa.ejb.crd.service.CobroCreditoService#procesarCobro}.
 *
 * {@code procesado = false} con {@code estado = RECHAZADO} es un resultado VÁLIDO, no un
 * error: es el rechazo automático por staleness de precancelación (el monto registrado ya
 * no coincide con el préstamo al momento de procesar).
 */
public class ResultadoProcesoCobro {

    private Long idCobro;
    private Long estado;
    private boolean procesado;
    private String mensaje;
    /**
     * H42 (cobro tardío, FASE 1): suma, sobre todos los préstamos del cobro, de la mora que
     * {@code ProcesoMoraPrestamoService#recalcularMoraALaFechaDePago} eliminó al recalcular a
     * la fecha efectiva de pago — {@code 0.0} si {@code cobro.fecha} no era anterior a hoy, o
     * si el tipo de operación no tiene préstamo (REGISTRO_APORTE) o ya tiene su propio
     * staleness check (ACUERDO_CONDONACION). Insumo de la FASE 2 (reverso contable).
     */
    private double moraEliminada;

    public Long getIdCobro() {
        return idCobro;
    }

    public void setIdCobro(Long idCobro) {
        this.idCobro = idCobro;
    }

    public Long getEstado() {
        return estado;
    }

    public void setEstado(Long estado) {
        this.estado = estado;
    }

    public boolean isProcesado() {
        return procesado;
    }

    public void setProcesado(boolean procesado) {
        this.procesado = procesado;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public double getMoraEliminada() {
        return moraEliminada;
    }

    public void setMoraEliminada(double moraEliminada) {
        this.moraEliminada = moraEliminada;
    }
}
