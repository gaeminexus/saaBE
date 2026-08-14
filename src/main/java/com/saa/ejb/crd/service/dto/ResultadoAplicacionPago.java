package com.saa.ejb.crd.service.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Resultado de aplicar un pago a un préstamo: cuánto entró, cuánto se aplicó,
 * qué cuotas se afectaron y en qué estado quedó el préstamo.
 *
 * Ver ESPECIFICACION-SERVICIOS-PAGO-PRESTAMOS.md §5.1.
 */
public class ResultadoAplicacionPago {

    private Long idPrestamo;
    private Long idEvento;

    /** Valor entregado por el usuario */
    private double valorRecibido;

    /** Valor efectivamente imputado a cuotas */
    private double valorAplicado;

    /** Valor que no encontró cuota donde aplicarse */
    private double excedenteNoAplicado;

    /** true si esta operación dejó el préstamo en CANCELADO (3) */
    private boolean prestamoCancelado;

    /** Estado del préstamo al terminar la operación (PRSTIDST) */
    private Long estadoFinalPrestamo;

    private List<DetalleAplicacionCuota> cuotasAfectadas = new ArrayList<>();

    public ResultadoAplicacionPago() {
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

    public double getValorRecibido() {
        return valorRecibido;
    }

    public void setValorRecibido(double valorRecibido) {
        this.valorRecibido = valorRecibido;
    }

    public double getValorAplicado() {
        return valorAplicado;
    }

    public void setValorAplicado(double valorAplicado) {
        this.valorAplicado = valorAplicado;
    }

    public double getExcedenteNoAplicado() {
        return excedenteNoAplicado;
    }

    public void setExcedenteNoAplicado(double excedenteNoAplicado) {
        this.excedenteNoAplicado = excedenteNoAplicado;
    }

    public boolean isPrestamoCancelado() {
        return prestamoCancelado;
    }

    public void setPrestamoCancelado(boolean prestamoCancelado) {
        this.prestamoCancelado = prestamoCancelado;
    }

    public Long getEstadoFinalPrestamo() {
        return estadoFinalPrestamo;
    }

    public void setEstadoFinalPrestamo(Long estadoFinalPrestamo) {
        this.estadoFinalPrestamo = estadoFinalPrestamo;
    }

    public List<DetalleAplicacionCuota> getCuotasAfectadas() {
        return cuotasAfectadas;
    }

    public void setCuotasAfectadas(List<DetalleAplicacionCuota> cuotasAfectadas) {
        this.cuotasAfectadas = cuotasAfectadas;
    }
}
