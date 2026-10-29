package com.saa.ejb.cnt.service.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * C17 — Estado de cierre de un mes para una empresa: contable (CNT.PRDO) y de cartera (CRD.CRCT).
 *
 * <p>Lo consume el generador de balances / estados financieros para marcar el reporte como DEFINITIVO
 * ({@code estado = CERRADO}) o PRELIMINAR ({@code estado = ABIERTO}), y lo expone
 * {@code GET /prdo/estado/{idEmpresa}/{anio}/{mes}}. docs/logica-negocio/cnt/CIERRES-BLOQUEANTES.md §5.</p>
 */
@SuppressWarnings("serial")
public class EstadoPeriodo implements Serializable {

    public static final String ABIERTO = "ABIERTO";
    public static final String CERRADO = "CERRADO";

    private Long idEmpresa;
    private Long anio;
    private Long mes;

    /** ABIERTO / CERRADO (cierre contable). Es el que decide DEFINITIVO / PRELIMINAR. */
    private String estado;
    /** true si el período contable existe en CNT.PRDO. */
    private boolean existePeriodo;
    private Long idPeriodo;
    /** Estado de mayorización (rubro 22: 1 activo, 2 mayorizado, 3 desmayorizado). */
    private Long estadoMayorizacion;
    private LocalDateTime fechaCierre;
    private String usuarioCierre;

    /** ABIERTO / CERRADO (cierre de cartera: corrida EJECUTADA vigente en CRD.CRCT). */
    private String estadoCartera;
    private Long idCorridaCartera;
    private LocalDateTime fechaCierreCartera;
    private String usuarioCierreCartera;

    /** true si los estados financieros del mes salen como DEFINITIVOS. */
    public boolean isDefinitivo() {
        return CERRADO.equals(estado);
    }

    /** Leyenda sugerida para el reporte. */
    public String getLeyenda() {
        return isDefinitivo() ? "DEFINITIVO" : "PRELIMINAR - PERIODO CONTABLE ABIERTO";
    }

    public Long getIdEmpresa() { return idEmpresa; }
    public void setIdEmpresa(Long idEmpresa) { this.idEmpresa = idEmpresa; }
    public Long getAnio() { return anio; }
    public void setAnio(Long anio) { this.anio = anio; }
    public Long getMes() { return mes; }
    public void setMes(Long mes) { this.mes = mes; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public boolean isExistePeriodo() { return existePeriodo; }
    public void setExistePeriodo(boolean existePeriodo) { this.existePeriodo = existePeriodo; }
    public Long getIdPeriodo() { return idPeriodo; }
    public void setIdPeriodo(Long idPeriodo) { this.idPeriodo = idPeriodo; }
    public Long getEstadoMayorizacion() { return estadoMayorizacion; }
    public void setEstadoMayorizacion(Long estadoMayorizacion) { this.estadoMayorizacion = estadoMayorizacion; }
    public LocalDateTime getFechaCierre() { return fechaCierre; }
    public void setFechaCierre(LocalDateTime fechaCierre) { this.fechaCierre = fechaCierre; }
    public String getUsuarioCierre() { return usuarioCierre; }
    public void setUsuarioCierre(String usuarioCierre) { this.usuarioCierre = usuarioCierre; }
    public String getEstadoCartera() { return estadoCartera; }
    public void setEstadoCartera(String estadoCartera) { this.estadoCartera = estadoCartera; }
    public Long getIdCorridaCartera() { return idCorridaCartera; }
    public void setIdCorridaCartera(Long idCorridaCartera) { this.idCorridaCartera = idCorridaCartera; }
    public LocalDateTime getFechaCierreCartera() { return fechaCierreCartera; }
    public void setFechaCierreCartera(LocalDateTime fechaCierreCartera) { this.fechaCierreCartera = fechaCierreCartera; }
    public String getUsuarioCierreCartera() { return usuarioCierreCartera; }
    public void setUsuarioCierreCartera(String usuarioCierreCartera) { this.usuarioCierreCartera = usuarioCierreCartera; }
}
