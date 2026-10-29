package com.saa.ejb.crd.service.dto;

/**
 * Cuerpo de los endpoints de aprobar/rechazar/reenviar un {@link com.saa.model.crd.CobroCredito}.
 * {@code motivo} solo aplica al rechazo.
 */
public class SolicitudAprobacionCobro {

    private String usuario;

    private String motivo;

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    /**
     * Solo {@code /cbcr/{id}/procesar}: modo re-proceso de un mes pasado (D-019 en el motor de
     * cobros, 2026-10-28). Con {@code true}, y solo si la fecha de pago y la de afectación del cobro
     * caen en un mes anterior al mes en curso, cada préstamo del cobro se evalúa vigente al corte
     * de ese mes (se reabre si hoy está cancelado). Sin el campo, o en {@code false}, el proceso es
     * el de siempre. Ver docs/logica-negocio/crd/API-REPROCESO-COBRO-FECHA-PASADA.md.
     */
    private Boolean reproceso;

    public Boolean getReproceso() {
        return reproceso;
    }

    public void setReproceso(Boolean reproceso) {
        this.reproceso = reproceso;
    }
}
