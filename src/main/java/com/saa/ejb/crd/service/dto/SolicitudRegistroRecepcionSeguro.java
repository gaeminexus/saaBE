package com.saa.ejb.crd.service.dto;

import java.time.LocalDate;

/**
 * Body de POST /rest/rvsg/registrar: crédito registra que el valor de seguro (sepelio) llegó
 * a la cuenta del fondo. Ver {@code docs/logica-negocio/crd/API-RECEPCION-VALORES-SEGURO.md}.
 */
public class SolicitudRegistroRecepcionSeguro {

    /** Partícipe fallecido a cuyo nombre queda el valor — ENTD.ENTDCDGO */
    private Long idEntidad;

    /** Tipo de aporte al que entra el valor — TPAP.TPAPCDGO. Debe estar vigente */
    private Long idTipoAporte;

    /** Cuenta bancaria del fondo donde entró el dinero — CNBC.CNBCCDGO */
    private Long idCuentaBancaria;

    /** Valor recibido; debe ser mayor a cero */
    private Double valor;

    /** Fecha de recepción del dinero; no puede ser futura */
    private LocalDate fecha;

    private String referencia;

    /** Ruta del respaldo digitalizado (no es un adjunto de CRD.ADJN) */
    private String rutaRespaldo;

    private String observacion;

    private String usuario;

    /**
     * Se ignora: la empresa contable se deriva de la cuenta bancaria, nunca de lo que mande el
     * cliente. El campo existe sólo para que un cliente que todavía lo envíe no falle.
     */
    private Long idEmpresa;

    public SolicitudRegistroRecepcionSeguro() {
    }

    public Long getIdEntidad() {
        return idEntidad;
    }

    public void setIdEntidad(Long idEntidad) {
        this.idEntidad = idEntidad;
    }

    public Long getIdTipoAporte() {
        return idTipoAporte;
    }

    public void setIdTipoAporte(Long idTipoAporte) {
        this.idTipoAporte = idTipoAporte;
    }

    public Long getIdCuentaBancaria() {
        return idCuentaBancaria;
    }

    public void setIdCuentaBancaria(Long idCuentaBancaria) {
        this.idCuentaBancaria = idCuentaBancaria;
    }

    public Double getValor() {
        return valor;
    }

    public void setValor(Double valor) {
        this.valor = valor;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public String getReferencia() {
        return referencia;
    }

    public void setReferencia(String referencia) {
        this.referencia = referencia;
    }

    public String getRutaRespaldo() {
        return rutaRespaldo;
    }

    public void setRutaRespaldo(String rutaRespaldo) {
        this.rutaRespaldo = rutaRespaldo;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public Long getIdEmpresa() {
        return idEmpresa;
    }

    public void setIdEmpresa(Long idEmpresa) {
        this.idEmpresa = idEmpresa;
    }
}
