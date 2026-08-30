package com.saa.ejb.crd.service.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Confirmación de un acuerdo de pago con condonación (Frente K, paso 1 de §5 del plan). El
 * operador indica cuánto se paga y cuánto se condona de CADA uno de los 5 conceptos del
 * préstamo, y AL MISMO TIEMPO los datos del cobro por la parte no condonada — el acuerdo
 * nace ya decidido, junto con su {@code CobroCredito} en CBCR (no hay aprobación de
 * condonación previa, K4 derogada el 2026-08-30).
 *
 * @see com.saa.ejb.crd.service.AcuerdoCondonacionService#registrarAcuerdo
 */
public class SolicitudRegistroAcuerdo {

    /** Préstamo del acuerdo. Debe estar EN_MORA(11) o DE_PLAZO_VENCIDO(8). Obligatorio. */
    private Long idPrestamo;

    /** Fecha de negocio del acuerdo (y del cobro). Obligatorio. */
    private LocalDate fecha;

    /** Observación del usuario. */
    private String observacion;

    /** Usuario que registra. */
    private String usuario;

    /** Las 5 líneas, una por concepto. Obligatorio, exactamente 5, sin conceptos repetidos. */
    private List<DetalleConceptoAcuerdoDTO> detalles;

    /** Cuenta bancaria donde entra la parte pagada. Obligatoria — mismo campo que {@code SolicitudRegistroCobro}. */
    private Long idCuentaBancaria;

    /** Referencia de la transferencia o depósito. */
    private String referencia;

    /** Ruta del comprobante digitalizado, ya subido con FileService. Obligatoria. */
    private String rutaRespaldo;

    public Long getIdPrestamo() {
        return idPrestamo;
    }

    public void setIdPrestamo(Long idPrestamo) {
        this.idPrestamo = idPrestamo;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
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

    public List<DetalleConceptoAcuerdoDTO> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<DetalleConceptoAcuerdoDTO> detalles) {
        this.detalles = detalles;
    }

    public Long getIdCuentaBancaria() {
        return idCuentaBancaria;
    }

    public void setIdCuentaBancaria(Long idCuentaBancaria) {
        this.idCuentaBancaria = idCuentaBancaria;
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
}
