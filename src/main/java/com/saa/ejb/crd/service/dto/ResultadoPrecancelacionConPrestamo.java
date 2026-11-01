package com.saa.ejb.crd.service.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * C16 (D-021) — resultado de la precancelación de un préstamo pagada con el dinero de un préstamo
 * nuevo del mismo partícipe (desembolso neto).
 */
public class ResultadoPrecancelacionConPrestamo {

    private Long idPrestamoCancelado;
    private Long idPrestamoNuevo;
    private Long idEvento;
    private Long idAsiento;
    private Double capital;
    private Double interes;
    private Double seguro;
    private Double total;
    /** Parte del capital que no estaba en la cuota del mes (PGPR con PGPRSLOT). */
    private Double capitalFuturo;
    private List<Long> idsPagoPrestamo = new ArrayList<>();
    /** Cuotas futuras pendientes que pasaron a CANCELADA_ANTICIPADA (7). */
    private Integer cuotasCanceladas;
    /** Cuotas futuras ya saldadas sin pago (migración) a las que se les fechó DTPRFCPG. */
    private Integer cuotasFechadas;
    private String tabla;
    /** true si ya estaba registrada (re-envío del lote): no se tocó nada. */
    private boolean yaExistia;

    public Long getIdPrestamoCancelado() { return idPrestamoCancelado; }
    public void setIdPrestamoCancelado(Long v) { this.idPrestamoCancelado = v; }
    public Long getIdPrestamoNuevo() { return idPrestamoNuevo; }
    public void setIdPrestamoNuevo(Long v) { this.idPrestamoNuevo = v; }
    public Long getIdEvento() { return idEvento; }
    public void setIdEvento(Long v) { this.idEvento = v; }
    public Long getIdAsiento() { return idAsiento; }
    public void setIdAsiento(Long v) { this.idAsiento = v; }
    public Double getCapital() { return capital; }
    public void setCapital(Double v) { this.capital = v; }
    public Double getInteres() { return interes; }
    public void setInteres(Double v) { this.interes = v; }
    public Double getSeguro() { return seguro; }
    public void setSeguro(Double v) { this.seguro = v; }
    public Double getTotal() { return total; }
    public void setTotal(Double v) { this.total = v; }
    public Double getCapitalFuturo() { return capitalFuturo; }
    public void setCapitalFuturo(Double v) { this.capitalFuturo = v; }
    public List<Long> getIdsPagoPrestamo() { return idsPagoPrestamo; }
    public void setIdsPagoPrestamo(List<Long> v) { this.idsPagoPrestamo = v; }
    public Integer getCuotasCanceladas() { return cuotasCanceladas; }
    public void setCuotasCanceladas(Integer v) { this.cuotasCanceladas = v; }
    public Integer getCuotasFechadas() { return cuotasFechadas; }
    public void setCuotasFechadas(Integer v) { this.cuotasFechadas = v; }
    public String getTabla() { return tabla; }
    public void setTabla(String v) { this.tabla = v; }
    public boolean isYaExistia() { return yaExistia; }
    public void setYaExistia(boolean v) { this.yaExistia = v; }
}
