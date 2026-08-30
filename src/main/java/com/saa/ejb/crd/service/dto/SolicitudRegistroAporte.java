package com.saa.ejb.crd.service.dto;

import java.time.LocalDate;

/**
 * Body de POST /rest/aprt/registrarAporte: pago de aportes recibido en ventanilla.
 *
 * Registra un aporte YA PAGADO para el partícipe: sube su saldo disponible de inmediato.
 */
public class SolicitudRegistroAporte {

    /** Código de la entidad (partícipe) — ENTD.ENTDCDGO */
    private Long idEntidad;

    /** Código del tipo de aporte — TPAP.TPAPCDGO. Debe estar vigente (estado = 1) */
    private Long idTipoAporte;

    /** Valor recibido; debe ser mayor a cero */
    private Double valor;

    private String usuario;

    /** Observación libre; se concatena a la glosa del aporte */
    private String observacion;

    /** Fecha del movimiento; si es null se asume hoy. No puede ser futura */
    private LocalDate fechaTransaccion;

    /** Ruta del documento de respaldo digitalizado; se estampa en el PagoAporte generado */
    private String rutaDocumentoRespaldo;

    /**
     * Mes al que pertenece el aporte (primer día del mes). Opcional: si no se indica, se usa
     * {@code TRUNC(fechaTransaccion, 'MM')}. Fase 2 del plan de devengo de aportes.
     */
    private LocalDate periodoDevengo;

    public SolicitudRegistroAporte() {
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

    public LocalDate getFechaTransaccion() {
        return fechaTransaccion;
    }

    public void setFechaTransaccion(LocalDate fechaTransaccion) {
        this.fechaTransaccion = fechaTransaccion;
    }

    public String getRutaDocumentoRespaldo() {
        return rutaDocumentoRespaldo;
    }

    public void setRutaDocumentoRespaldo(String rutaDocumentoRespaldo) {
        this.rutaDocumentoRespaldo = rutaDocumentoRespaldo;
    }

    public LocalDate getPeriodoDevengo() {
        return periodoDevengo;
    }

    public void setPeriodoDevengo(LocalDate periodoDevengo) {
        this.periodoDevengo = periodoDevengo;
    }
}
