package com.saa.ejb.crd.service.dto;

import java.time.LocalDate;

/**
 * Body de POST /rest/prst/pagarCuota.
 *
 * Ver ESPECIFICACION-SERVICIOS-PAGO-PRESTAMOS.md §5.1 y §8.
 */
public class SolicitudPagoCuota {

    private Long idPrestamo;
    private Double valor;
    private String usuario;
    private String observacion;

    /** Fecha de negocio del pago; si es null se asume hoy */
    private LocalDate fechaPago;

    /** Ruta del documento de respaldo digitalizado; se estampa en los PagoPrestamo generados */
    private String rutaDocumentoRespaldo;

    public SolicitudPagoCuota() {
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

    public LocalDate getFechaPago() {
        return fechaPago;
    }

    public void setFechaPago(LocalDate fechaPago) {
        this.fechaPago = fechaPago;
    }

    public String getRutaDocumentoRespaldo() {
        return rutaDocumentoRespaldo;
    }

    public void setRutaDocumentoRespaldo(String rutaDocumentoRespaldo) {
        this.rutaDocumentoRespaldo = rutaDocumentoRespaldo;
    }
}
