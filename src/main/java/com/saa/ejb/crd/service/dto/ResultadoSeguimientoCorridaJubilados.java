package com.saa.ejb.crd.service.dto;

/**
 * Respuesta de {@code GET /rest/pgpc/corrida/{anio}/{mes}} — §4.3 de
 * API-DOS-PROCESOS-MENSUALES-JUBILADOS.md. Siempre 200, con los dos estados en 0 si el
 * período nunca se corrió: "este mes no se corrió nada" es una respuesta válida, nunca 404.
 */
public class ResultadoSeguimientoCorridaJubilados {

    private Integer anio;
    private Integer mes;
    private Long idEmpresa;

    private EstadoProcesoJubilados seguro;
    private EstadoProcesoJubilados pensiones;

    /** Calculado por el backend con la misma regla que aplica POST /seguro/generar. */
    private Boolean puedeGenerarSeguro;

    /** Calculado por el backend con la misma regla que aplica POST /pensiones/generar (incluye D2). */
    private Boolean puedeGenerarPensiones;

    /** Consecuencia visible de D4 (cada proceso usa el padrón de su propia fecha) — información, no error. */
    private Long conSeguroSinPension;
    private Long conPensionSinSeguro;

    public ResultadoSeguimientoCorridaJubilados() {
    }

    public Integer getAnio() {
        return anio;
    }

    public void setAnio(Integer anio) {
        this.anio = anio;
    }

    public Integer getMes() {
        return mes;
    }

    public void setMes(Integer mes) {
        this.mes = mes;
    }

    public Long getIdEmpresa() {
        return idEmpresa;
    }

    public void setIdEmpresa(Long idEmpresa) {
        this.idEmpresa = idEmpresa;
    }

    public EstadoProcesoJubilados getSeguro() {
        return seguro;
    }

    public void setSeguro(EstadoProcesoJubilados seguro) {
        this.seguro = seguro;
    }

    public EstadoProcesoJubilados getPensiones() {
        return pensiones;
    }

    public void setPensiones(EstadoProcesoJubilados pensiones) {
        this.pensiones = pensiones;
    }

    public Boolean getPuedeGenerarSeguro() {
        return puedeGenerarSeguro;
    }

    public void setPuedeGenerarSeguro(Boolean puedeGenerarSeguro) {
        this.puedeGenerarSeguro = puedeGenerarSeguro;
    }

    public Boolean getPuedeGenerarPensiones() {
        return puedeGenerarPensiones;
    }

    public void setPuedeGenerarPensiones(Boolean puedeGenerarPensiones) {
        this.puedeGenerarPensiones = puedeGenerarPensiones;
    }

    public Long getConSeguroSinPension() {
        return conSeguroSinPension;
    }

    public void setConSeguroSinPension(Long conSeguroSinPension) {
        this.conSeguroSinPension = conSeguroSinPension;
    }

    public Long getConPensionSinSeguro() {
        return conPensionSinSeguro;
    }

    public void setConPensionSinSeguro(Long conPensionSinSeguro) {
        this.conPensionSinSeguro = conPensionSinSeguro;
    }
}
