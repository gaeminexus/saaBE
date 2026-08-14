package com.saa.ejb.crd.service.dto;

/**
 * Resultado de anular (reversar) un EventoPrestamo completo.
 *
 * Ver ESPECIFICACION-SERVICIOS-PAGO-PRESTAMOS.md §7.6.
 */
public class ResultadoAnulacion {

    private Long idEvento;
    private Long idPrestamo;

    /** PAGO_MANUAL | PAGO_APORTES | ABONO_CAPITAL | PRECANCELACION */
    private String tipoOperacion;

    /** PagoPrestamo marcados con PGPRANUL = 1 */
    private Integer pagosAnulados;

    /** Cuotas reconstruidas con recalcularCuotaDesdePagos */
    private Integer cuotasRecalculadas;

    /** Cuotas restauradas desde CRD.HDTP (solo en el reverso de ABONO_CAPITAL) */
    private Integer cuotasRestauradas;

    /** Cuotas borradas de CRD.DTPR por haber sido generadas por el evento (solo ABONO_CAPITAL) */
    private Integer cuotasEliminadas;

    /** Contra-movimientos POSITIVOS creados en CRD.APRT (solo si el evento consumió aportes) */
    private Integer movimientosAporteRevertidos;

    /** Estado del préstamo al terminar el reverso (PRSTIDST) */
    private Long estadoFinalPrestamo;

    public ResultadoAnulacion() {
    }

    public Long getIdEvento() {
        return idEvento;
    }

    public void setIdEvento(Long idEvento) {
        this.idEvento = idEvento;
    }

    public Long getIdPrestamo() {
        return idPrestamo;
    }

    public void setIdPrestamo(Long idPrestamo) {
        this.idPrestamo = idPrestamo;
    }

    public String getTipoOperacion() {
        return tipoOperacion;
    }

    public void setTipoOperacion(String tipoOperacion) {
        this.tipoOperacion = tipoOperacion;
    }

    public Integer getPagosAnulados() {
        return pagosAnulados;
    }

    public void setPagosAnulados(Integer pagosAnulados) {
        this.pagosAnulados = pagosAnulados;
    }

    public Integer getCuotasRecalculadas() {
        return cuotasRecalculadas;
    }

    public void setCuotasRecalculadas(Integer cuotasRecalculadas) {
        this.cuotasRecalculadas = cuotasRecalculadas;
    }

    public Integer getCuotasRestauradas() {
        return cuotasRestauradas;
    }

    public void setCuotasRestauradas(Integer cuotasRestauradas) {
        this.cuotasRestauradas = cuotasRestauradas;
    }

    public Integer getCuotasEliminadas() {
        return cuotasEliminadas;
    }

    public void setCuotasEliminadas(Integer cuotasEliminadas) {
        this.cuotasEliminadas = cuotasEliminadas;
    }

    public Integer getMovimientosAporteRevertidos() {
        return movimientosAporteRevertidos;
    }

    public void setMovimientosAporteRevertidos(Integer movimientosAporteRevertidos) {
        this.movimientosAporteRevertidos = movimientosAporteRevertidos;
    }

    public Long getEstadoFinalPrestamo() {
        return estadoFinalPrestamo;
    }

    public void setEstadoFinalPrestamo(Long estadoFinalPrestamo) {
        this.estadoFinalPrestamo = estadoFinalPrestamo;
    }
}
