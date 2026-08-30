package com.saa.ejb.crd.service.dto;

import java.time.LocalDateTime;

/**
 * Un movimiento individual dentro de un periodo del estado de cuenta de aportes por devengo
 * (§4.2 del plan de devengo de aportes). {@code fechaTransaccion} es la fecha de CAJA
 * (APRTFCTR): se muestra como dato de detalle, el agrupador del periodo es el periodo
 * efectivo, no esta fecha.
 *
 * @author Sistema SAA
 * @since 2026-08-27
 */
public class MovimientoEstadoCuentaDTO {

    private Long idAporte;
    private LocalDateTime fechaTransaccion;
    private Double valor;
    private Long tipoMovimiento;
    private String tipoMovimientoTexto;
    private String glosa;

    public MovimientoEstadoCuentaDTO() {
    }

    public Long getIdAporte() {
        return idAporte;
    }

    public void setIdAporte(Long idAporte) {
        this.idAporte = idAporte;
    }

    public LocalDateTime getFechaTransaccion() {
        return fechaTransaccion;
    }

    public void setFechaTransaccion(LocalDateTime fechaTransaccion) {
        this.fechaTransaccion = fechaTransaccion;
    }

    public Double getValor() {
        return valor;
    }

    public void setValor(Double valor) {
        this.valor = valor;
    }

    public Long getTipoMovimiento() {
        return tipoMovimiento;
    }

    public void setTipoMovimiento(Long tipoMovimiento) {
        this.tipoMovimiento = tipoMovimiento;
    }

    public String getTipoMovimientoTexto() {
        return tipoMovimientoTexto;
    }

    public void setTipoMovimientoTexto(String tipoMovimientoTexto) {
        this.tipoMovimientoTexto = tipoMovimientoTexto;
    }

    public String getGlosa() {
        return glosa;
    }

    public void setGlosa(String glosa) {
        this.glosa = glosa;
    }
}
