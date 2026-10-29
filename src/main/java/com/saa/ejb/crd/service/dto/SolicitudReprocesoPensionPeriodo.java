package com.saa.ejb.crd.service.dto;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Cuerpo de {@code POST /pgpc/reprocesarPeriodo} (C20, pensión complementaria de un mes pasado).
 * Acepta el lote del expediente tal cual (sus campos descriptivos se ignoran) más
 * {@code idEmpresa}, {@code anio}, {@code mes} y {@code usuario}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SolicitudReprocesoPensionPeriodo {

    private Long idEmpresa;
    private Integer anio;
    private Integer mes;
    private String usuario;

    /**
     * Usuario del sistema ({@code SCP.PJRQ.PJRQCDGO}) que registra las órdenes en CxP. Opcional:
     * sin él se busca por {@link #usuario} como nombre de usuario del sistema.
     */
    private Long idUsuario;

    /** Cuenta bancaria por defecto de los pagos con {@code fechaPagoBanco} (p. ej. 418). */
    private Long idCuentaBancariaPago;

    /** Referencia por defecto de esos pagos (p. ej. "Ru-858739"). */
    private String referenciaPago;

    /**
     * Jubilados del rol. Vacía o ausente: modo padrón (VPPC activa y traslado de jubilación hasta
     * el fin del mes; cruce por lo exigible al corte; sin pago confirmado).
     */
    private List<FilaReprocesoPension> filas = new ArrayList<>();

    public Long getIdEmpresa() {
        return idEmpresa;
    }

    public void setIdEmpresa(Long idEmpresa) {
        this.idEmpresa = idEmpresa;
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

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public Long getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Long idUsuario) {
        this.idUsuario = idUsuario;
    }

    public Long getIdCuentaBancariaPago() {
        return idCuentaBancariaPago;
    }

    public void setIdCuentaBancariaPago(Long idCuentaBancariaPago) {
        this.idCuentaBancariaPago = idCuentaBancariaPago;
    }

    public String getReferenciaPago() {
        return referenciaPago;
    }

    public void setReferenciaPago(String referenciaPago) {
        this.referenciaPago = referenciaPago;
    }

    public List<FilaReprocesoPension> getFilas() {
        return filas;
    }

    public void setFilas(List<FilaReprocesoPension> filas) {
        this.filas = filas;
    }
}
