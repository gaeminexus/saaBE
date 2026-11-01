package com.saa.ejb.crd.service.dto;

/**
 * Body de {@code POST /rest/asgn/confirmarRecepcion/{idCarga}} — CONTRATO CONGELADO, ver
 * {@code docs/logica-negocio/crd/API-COBRO-PETRO-DOS-PASOS.md} §2.2.
 *
 * <p>{@code paraReproceso} (opcional, por defecto false — cuadre Superintendencia C4): permite
 * confirmar la recepción de una carga YA PROCESADA (estado 3) que nunca tuvo el paso 1 (las
 * cargas jun-2025 → jul-2026 aplicadas con el proceso viejo), como antesala de
 * {@code POST /rest/crar/reprocesar/{idCarga}}. Sella {@code CRARFCAC} y genera el asiento
 * TRANSITORIO igual que siempre, pero NO cambia el estado de la carga (sigue en 3: el reproceso
 * es quien la devuelve a 2 y la vuelve a aplicar). Ver REPROCESO-CARGA-PETRO.md §6.</p>
 */
public class SolicitudConfirmarRecepcion {

    private String usuario;
    private String ip;
    private String observacion;
    private Boolean paraReproceso;

    public Boolean getParaReproceso() {
        return paraReproceso;
    }

    public void setParaReproceso(Boolean paraReproceso) {
        this.paraReproceso = paraReproceso;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }
}
