package com.saa.ejb.crd.service.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Resumen de una corrida del reconciliador de devoluciones de aportes
 * ({@code DevolucionAporteService.sincronizarPagos}).
 *
 * El reconciliador es IDEMPOTENTE: correrlo N veces da el mismo resultado. Una segunda
 * corrida seguida devuelve {@code evaluadas = 0} porque las devoluciones ya cerraron su
 * ciclo y salieron del universo.
 *
 * POJO plano: getters y setters escritos a mano, sin Lombok.
 */
public class ResultadoSincronizacion {

    /** Devoluciones que entraron al universo (estado 1 o 2 con orden de pago). */
    private Integer evaluadas = 0;

    /** Devoluciones que pasaron a PAGADA porque su pago quedó confirmado. */
    private Integer marcadasPagadas = 0;

    /**
     * Devoluciones que pasaron a RECHAZADA porque su pago fue rechazado o anulado.
     * Cada una generó sus contra-movimientos positivos en CRD.APRT.
     */
    private Integer marcadasRechazadas = 0;

    /**
     * Devoluciones cuya orden de pago ya no existe en CXP. Se dejan como están y se
     * registran: es un dato para investigar, no un error que aborte la corrida.
     */
    private Integer huerfanas = 0;

    /** Devoluciones que fallaron. Una devolución con datos malos no aborta el lote. */
    private Integer conError = 0;

    /** Detalle de los errores, para el log y para la respuesta del endpoint manual. */
    private List<String> errores = new ArrayList<>();

    public ResultadoSincronizacion() {
    }

    public Integer getEvaluadas() {
        return evaluadas;
    }

    public void setEvaluadas(Integer evaluadas) {
        this.evaluadas = evaluadas;
    }

    public Integer getMarcadasPagadas() {
        return marcadasPagadas;
    }

    public void setMarcadasPagadas(Integer marcadasPagadas) {
        this.marcadasPagadas = marcadasPagadas;
    }

    public Integer getMarcadasRechazadas() {
        return marcadasRechazadas;
    }

    public void setMarcadasRechazadas(Integer marcadasRechazadas) {
        this.marcadasRechazadas = marcadasRechazadas;
    }

    public Integer getHuerfanas() {
        return huerfanas;
    }

    public void setHuerfanas(Integer huerfanas) {
        this.huerfanas = huerfanas;
    }

    public Integer getConError() {
        return conError;
    }

    public void setConError(Integer conError) {
        this.conError = conError;
    }

    public List<String> getErrores() {
        return errores;
    }

    public void setErrores(List<String> errores) {
        this.errores = errores;
    }
}
