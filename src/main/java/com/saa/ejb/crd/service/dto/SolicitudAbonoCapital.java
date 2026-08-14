package com.saa.ejb.crd.service.dto;

import java.time.LocalDate;

/**
 * Body de POST /rest/prst/abonarCapital.
 *
 * Ver ESPECIFICACION-SERVICIOS-PAGO-PRESTAMOS.md §5.1 y §7.3.
 */
public class SolicitudAbonoCapital {

    private Long idPrestamo;
    private Double valor;

    /** 1 = mantiene el valor de cuota y reduce el plazo; 2 = mantiene el plazo y reduce la cuota */
    private Integer modalidad;

    private String usuario;
    private String observacion;

    /** Fecha de negocio de la operación; si es null se asume hoy */
    private LocalDate fecha;

    /** Ruta del documento de respaldo digitalizado; se estampa en el PagoPrestamo del abono */
    private String rutaDocumentoRespaldo;

    public SolicitudAbonoCapital() {
    }

    public Long getIdPrestamo() {
        return idPrestamo;
    }

    public void setIdPrestamo(Long idPrestamo) {
        this.idPrestamo = idPrestamo;
    }

    public Double getValor() {
        return valor;
    }

    public void setValor(Double valor) {
        this.valor = valor;
    }

    public Integer getModalidad() {
        return modalidad;
    }

    public void setModalidad(Integer modalidad) {
        this.modalidad = modalidad;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public String getRutaDocumentoRespaldo() {
        return rutaDocumentoRespaldo;
    }

    public void setRutaDocumentoRespaldo(String rutaDocumentoRespaldo) {
        this.rutaDocumentoRespaldo = rutaDocumentoRespaldo;
    }
}
