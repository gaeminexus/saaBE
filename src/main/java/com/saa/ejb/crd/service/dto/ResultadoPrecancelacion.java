package com.saa.ejb.crd.service.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Resultado de ejecutar una precancelación total.
 *
 * Ver ESPECIFICACION-SERVICIOS-PAGO-PRESTAMOS.md §7.5.
 */
public class ResultadoPrecancelacion {

    private Long idPrestamo;
    private Long idEvento;

    /** Deuda exigible efectivamente pagada (cuotas con vencimiento &lt;= fecha) */
    private Double valorExigiblePagado;

    /** Capital de las cuotas futuras, registrado en DTPRSLOT */
    private Double capitalPrecancelado;

    private Double valorTotalPrecancelacion;

    /** Cuotas futuras que pasaron a CANCELADA_ANTICIPADA (7) */
    private Integer cuotasCanceladasAnticipadas;

    /** Estado final del préstamo: 4 = CANCELADO_ANTICIPADO */
    private Long estadoFinalPrestamo;

    /** Cuota donde se acumuló el capital futuro en DTPRSLOT */
    private Long idCuotaConSaldoOtros;

    /** PGPR creado por el componente de capital futuro */
    private Long idPagoPrestamoCapitalFuturo;

    private List<MovimientoAporte> movimientosAporte = new ArrayList<>();

    public ResultadoPrecancelacion() {
    }

    public Long getIdPrestamo() {
        return idPrestamo;
    }

    public void setIdPrestamo(Long idPrestamo) {
        this.idPrestamo = idPrestamo;
    }

    public Long getIdEvento() {
        return idEvento;
    }

    public void setIdEvento(Long idEvento) {
        this.idEvento = idEvento;
    }

    public Double getValorExigiblePagado() {
        return valorExigiblePagado;
    }

    public void setValorExigiblePagado(Double valorExigiblePagado) {
        this.valorExigiblePagado = valorExigiblePagado;
    }

    public Double getCapitalPrecancelado() {
        return capitalPrecancelado;
    }

    public void setCapitalPrecancelado(Double capitalPrecancelado) {
        this.capitalPrecancelado = capitalPrecancelado;
    }

    public Double getValorTotalPrecancelacion() {
        return valorTotalPrecancelacion;
    }

    public void setValorTotalPrecancelacion(Double valorTotalPrecancelacion) {
        this.valorTotalPrecancelacion = valorTotalPrecancelacion;
    }

    public Integer getCuotasCanceladasAnticipadas() {
        return cuotasCanceladasAnticipadas;
    }

    public void setCuotasCanceladasAnticipadas(Integer cuotasCanceladasAnticipadas) {
        this.cuotasCanceladasAnticipadas = cuotasCanceladasAnticipadas;
    }

    public Long getEstadoFinalPrestamo() {
        return estadoFinalPrestamo;
    }

    public void setEstadoFinalPrestamo(Long estadoFinalPrestamo) {
        this.estadoFinalPrestamo = estadoFinalPrestamo;
    }

    public Long getIdCuotaConSaldoOtros() {
        return idCuotaConSaldoOtros;
    }

    public void setIdCuotaConSaldoOtros(Long idCuotaConSaldoOtros) {
        this.idCuotaConSaldoOtros = idCuotaConSaldoOtros;
    }

    public Long getIdPagoPrestamoCapitalFuturo() {
        return idPagoPrestamoCapitalFuturo;
    }

    public void setIdPagoPrestamoCapitalFuturo(Long idPagoPrestamoCapitalFuturo) {
        this.idPagoPrestamoCapitalFuturo = idPagoPrestamoCapitalFuturo;
    }

    public List<MovimientoAporte> getMovimientosAporte() {
        return movimientosAporte;
    }

    public void setMovimientosAporte(List<MovimientoAporte> movimientosAporte) {
        this.movimientosAporte = movimientosAporte;
    }
}
