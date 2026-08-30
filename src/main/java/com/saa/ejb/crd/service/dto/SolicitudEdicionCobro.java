package com.saa.ejb.crd.service.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Corrección de un {@link com.saa.model.crd.CobroCredito} RECHAZADO, para que crédito lo
 * corrija y lo reenvíe a contabilidad. La entidad y el tipo de operación NO se pueden
 * cambiar (si esos están mal, es más simple anular y volver a registrar); todo lo demás sí.
 *
 * @see com.saa.ejb.crd.service.CobroCreditoService#editarYReenviarCobro
 */
public class SolicitudEdicionCobro {

    private Long idCuentaBancaria;
    private String referencia;
    private String rutaRespaldo;
    private Double valor;
    private LocalDate fecha;
    private String observacion;
    private List<DetalleRegistroCobroDTO> detalles;
    private String usuario;

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

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public List<DetalleRegistroCobroDTO> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<DetalleRegistroCobroDTO> detalles) {
        this.detalles = detalles;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }
}
