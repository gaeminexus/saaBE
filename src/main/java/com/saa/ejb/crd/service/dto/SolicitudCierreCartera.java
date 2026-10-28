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

    // ---------------------------------------------------------------------------------
    // C15 — cierre a fecha de corte (docs/logica-negocio/crd/CIERRE-CARTERA-A-FECHA-DE-CORTE.md)
    // ---------------------------------------------------------------------------------

    /**
     * Lee la cartera como estaba al corte (H1). {@code null} = automático
     * ({@code ModoCorteCartera#esAlCorte}: sí para meses anteriores al mes pasado). En modo al
     * corte, además: ①/②/①.1 se fechan el último día del mes que cierra (H3) y ② compara el
     * corte anterior contra el corte; se agregan ⑧ provisión de cartera y ⑨ ajuste de bandas.
     */
    private Boolean estadoAlCorte;

    /** Convención de vencimiento al corte; {@code null} = propiedad {@code saa.cartera.corteInclusivo} (true). */
    private Boolean corteInclusivo;

    /**
     * H5 (D-016) — corrida de ARRANQUE sobre el mes del ancla (mayo 2025): solo ⑨ (reparto de
     * bandas de la apertura, a nivel de cuenta madre para no cambiar el B17 del ancla), ③ y ④
     * del mes siguiente, y la semilla del libro MVIC con la provisión de intereses de la apertura.
     * Sin ①, ②, ①.1, ⑥, ⑦ ni ⑧, sin control de archivo Petro.
     */
    private Boolean arranque;

    /**
     * Arranque: monto de la provisión de intereses de la apertura a sembrar en MVIC.
     * {@code null} = saldo acreedor de la cuenta de la plantilla 36 (papel 81) al corte.
     */
    private Double montoProvisionInteresesApertura;

    /** Omite el ⑧ aunque el modo sea al corte. */
    private Boolean omitirProvisionCartera;

    /** Omite el ⑨ aunque el modo sea al corte. */
    private Boolean omitirAjusteBandas;

    public SolicitudCierreCartera() {
    }

    public Boolean getEstadoAlCorte() { return estadoAlCorte; }
    public void setEstadoAlCorte(Boolean estadoAlCorte) { this.estadoAlCorte = estadoAlCorte; }
    public Boolean getCorteInclusivo() { return corteInclusivo; }
    public void setCorteInclusivo(Boolean corteInclusivo) { this.corteInclusivo = corteInclusivo; }
    public Boolean getArranque() { return arranque; }
    public void setArranque(Boolean arranque) { this.arranque = arranque; }
    public Double getMontoProvisionInteresesApertura() { return montoProvisionInteresesApertura; }
    public void setMontoProvisionInteresesApertura(Double montoProvisionInteresesApertura) {
        this.montoProvisionInteresesApertura = montoProvisionInteresesApertura;
    }
    public Boolean getOmitirProvisionCartera() { return omitirProvisionCartera; }
    public void setOmitirProvisionCartera(Boolean omitirProvisionCartera) { this.omitirProvisionCartera = omitirProvisionCartera; }
    public Boolean getOmitirAjusteBandas() { return omitirAjusteBandas; }
    public void setOmitirAjusteBandas(Boolean omitirAjusteBandas) { this.omitirAjusteBandas = omitirAjusteBandas; }

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
