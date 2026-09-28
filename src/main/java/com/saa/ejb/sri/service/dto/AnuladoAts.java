package com.saa.ejb.sri.service.dto;

import java.io.Serializable;

/**
 * Una línea de {@code <detalleAnulados>} del detalle del ATS (API-DETALLE-ATS.md §4.5).
 * Documentos que EMITIMOS nosotros y anulamos -- ver la limitación documentada en
 * {@code GeneradorAtsServiceImpl} (no distingue una anulación interna de una baja hecha en el
 * portal del SRI).
 */
public class AnuladoAts implements Serializable {

    private static final long serialVersionUID = 1L;

    private String tipoComprobante;
    private String establecimiento;
    private String puntoEmision;
    private String secuencial;
    private String autorizacion;

    public String getTipoComprobante() { return tipoComprobante; }
    public void setTipoComprobante(String tipoComprobante) { this.tipoComprobante = tipoComprobante; }

    public String getEstablecimiento() { return establecimiento; }
    public void setEstablecimiento(String establecimiento) { this.establecimiento = establecimiento; }

    public String getPuntoEmision() { return puntoEmision; }
    public void setPuntoEmision(String puntoEmision) { this.puntoEmision = puntoEmision; }

    public String getSecuencial() { return secuencial; }
    public void setSecuencial(String secuencial) { this.secuencial = secuencial; }

    public String getAutorizacion() { return autorizacion; }
    public void setAutorizacion(String autorizacion) { this.autorizacion = autorizacion; }
}
