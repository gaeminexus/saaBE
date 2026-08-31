package com.saa.ejb.crd.service.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Resultado de registrar o anular una devolución de aportes.
 *
 * POJO plano: getters y setters escritos a mano, sin Lombok.
 */
public class ResultadoDevolucionAporte {

    /** DVAPCDGO de la devolución. */
    private Long idDevolucion;

    private Long idEntidad;

    /**
     * Orden de pago generada en CXP (PGS.PGTR.PGTRCDGO). Null si todavía no se generó o si
     * la devolución se anuló antes.
     */
    private Long idPagoProgramado;

    /** Estado de la devolución. Ver {@link com.saa.rubros.EstadoDevolucionAporte}. */
    private Long estado;

    /** Nombre legible del estado, para la pantalla. */
    private String estadoTexto;

    private Double valorTotal;

    private LocalDate fecha;

    /** Código del asiento contable del pago (lo genera CXP), cuando la devolución ya está pagada. */
    private Long numeroAsiento;

    /**
     * Código del asiento de RECLASIFICACIÓN (lo genera CRD al registrar) — opción C,
     * 2026-08-31. Null si la contabilidad de CRD estaba apagada al registrar, o en
     * devoluciones anteriores a esa fecha; no es un error.
     */
    private Long numeroAsientoReclasificacion;

    /** Fecha en que el banco confirmó el pago. */
    private LocalDate fechaPago;

    private List<DetalleResultadoDevolucion> detalle = new ArrayList<>();

    public ResultadoDevolucionAporte() {
    }

    public Long getIdDevolucion() {
        return idDevolucion;
    }

    public void setIdDevolucion(Long idDevolucion) {
        this.idDevolucion = idDevolucion;
    }

    public Long getIdEntidad() {
        return idEntidad;
    }

    public void setIdEntidad(Long idEntidad) {
        this.idEntidad = idEntidad;
    }

    public Long getIdPagoProgramado() {
        return idPagoProgramado;
    }

    public void setIdPagoProgramado(Long idPagoProgramado) {
        this.idPagoProgramado = idPagoProgramado;
    }

    public Long getEstado() {
        return estado;
    }

    public void setEstado(Long estado) {
        this.estado = estado;
    }

    public String getEstadoTexto() {
        return estadoTexto;
    }

    public void setEstadoTexto(String estadoTexto) {
        this.estadoTexto = estadoTexto;
    }

    public Double getValorTotal() {
        return valorTotal;
    }

    public void setValorTotal(Double valorTotal) {
        this.valorTotal = valorTotal;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public Long getNumeroAsiento() {
        return numeroAsiento;
    }

    public void setNumeroAsiento(Long numeroAsiento) {
        this.numeroAsiento = numeroAsiento;
    }

    public Long getNumeroAsientoReclasificacion() {
        return numeroAsientoReclasificacion;
    }

    public void setNumeroAsientoReclasificacion(Long numeroAsientoReclasificacion) {
        this.numeroAsientoReclasificacion = numeroAsientoReclasificacion;
    }

    public LocalDate getFechaPago() {
        return fechaPago;
    }

    public void setFechaPago(LocalDate fechaPago) {
        this.fechaPago = fechaPago;
    }

    public List<DetalleResultadoDevolucion> getDetalle() {
        return detalle;
    }

    public void setDetalle(List<DetalleResultadoDevolucion> detalle) {
        this.detalle = detalle;
    }
}
