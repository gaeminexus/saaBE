package com.saa.ejb.crd.service.dto;

/**
 * Parámetros de una corrida del cierre mensual de cartera.
 *
 * El período se identifica por año y mes CERRADOS; las tres fechas del proceso
 * —corte, proceso y corte de apertura— se derivan de ahí y no se reciben: dejarlas
 * elegir abriría la puerta a un cierre de agosto fechado en marzo.
 */
public class SolicitudCierreCartera {

    /** Código de la empresa (SCP.PJRQ). Obligatorio. */
    private Long idEmpresa;

    /** Año del mes a cerrar. Obligatorio. */
    private Long anio;

    /** Mes a cerrar, 1 a 12. Obligatorio. */
    private Long mes;

    /** Usuario que ejecuta, para la auditoría y para el asiento. */
    private String usuario;

    /** IP desde la que se ejecuta, para la auditoría. */
    private String ip;

    /** Observación libre de la corrida. */
    private String observacion;

    /**
     * Salta el control de archivo Petro del mes (decisión D13). Solo tiene efecto en
     * {@code ejecutar}; la previsualización nunca bloquea.
     *
     * <p>
     * Es para el mes en que legítimamente no hubo archivo. Exige
     * {@code motivoOmisionControl}, queda registrado en la observación de la corrida y sale
     * en las advertencias y en {@code controlArchivoPetro} del resultado. <b>No hay forma
     * silenciosa de saltarlo.</b>
     * </p>
     */
    private Boolean omitirControlArchivoPetro;

    /**
     * Por qué se omite el control. <b>Obligatorio</b> si
     * {@code omitirControlArchivoPetro} es {@code true}.
     */
    private String motivoOmisionControl;

    public SolicitudCierreCartera() {
    }

    public Long getIdEmpresa() {
        return idEmpresa;
    }

    public void setIdEmpresa(Long idEmpresa) {
        this.idEmpresa = idEmpresa;
    }

    public Long getAnio() {
        return anio;
    }

    public void setAnio(Long anio) {
        this.anio = anio;
    }

    public Long getMes() {
        return mes;
    }

    public void setMes(Long mes) {
        this.mes = mes;
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

    public Boolean getOmitirControlArchivoPetro() {
        return omitirControlArchivoPetro;
    }

    public void setOmitirControlArchivoPetro(Boolean omitirControlArchivoPetro) {
        this.omitirControlArchivoPetro = omitirControlArchivoPetro;
    }

    public String getMotivoOmisionControl() {
        return motivoOmisionControl;
    }

    public void setMotivoOmisionControl(String motivoOmisionControl) {
        this.motivoOmisionControl = motivoOmisionControl;
    }
}
