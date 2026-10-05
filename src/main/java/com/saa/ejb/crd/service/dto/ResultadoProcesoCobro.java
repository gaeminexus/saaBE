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
    /**
     * Provisión de intereses reversada (interés + mora), ÍTEM 4 del frente de provisión de
     * intereses ({@code docs/logica-negocio/crd/DISENO-PROVISION-INTERESES-Y-FECHA-AFECTACION.md}
     * §5) — {@code 0.0} hasta que ese ítem esté implementado, o si el cobro no tenía nada
     * provisionado que reversar.
     */
    private double provisionReversada;
    /** Asiento D 149905 / H 470510 del reverso de provisión (§5); {@code null} si no hubo. */
    private Long idAsientoReversoProvision;
    /** Asiento separado del cobro tardío (§6.2/§7bis: exceso de mora, devengo y bandas); {@code null} si no hubo. */
    private Long idAsientoCobroTardio;

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

    public double getProvisionReversada() {
        return provisionReversada;
    }

    public void setProvisionReversada(double provisionReversada) {
        this.provisionReversada = provisionReversada;
    }

    public Long getIdAsientoReversoProvision() {
        return idAsientoReversoProvision;
    }

    public void setIdAsientoReversoProvision(Long idAsientoReversoProvision) {
        this.idAsientoReversoProvision = idAsientoReversoProvision;
    }

    public Long getIdAsientoCobroTardio() {
        return idAsientoCobroTardio;
    }

    public void setIdAsientoCobroTardio(Long idAsientoCobroTardio) {
        this.idAsientoCobroTardio = idAsientoCobroTardio;
    }
}
