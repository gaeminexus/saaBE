package com.saa.ejb.crd.service.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Body de {@code POST /rest/dsbn/detalle} — API-AUDITORIA-BANDAS.md §2. Todos los campos son
 * opcionales salvo {@code origen}/{@code idOrigen}; los arreglos son OR interno y AND entre sí.
 */
public class FiltroDetalleDistribucionBanda {

    private String origen;
    private Long idOrigen;
    private List<String> conceptos;
    private List<Long> idsBanda;
    private List<Long> idsProducto;
    private List<Long> idsTipoPrestamo;
    private List<Long> idsTipoAporte;
    private List<Long> idsEntidad;
    private List<String> cuentasContables;
    private LocalDate fechaDesde;
    private LocalDate fechaHasta;
    private Integer pagina = 0;
    private Integer tamanio = 50;
    private String ordenarPor;
    private String orden;

    public FiltroDetalleDistribucionBanda() {
    }

    public String getOrigen() {
        return origen;
    }

    public void setOrigen(String origen) {
        this.origen = origen;
    }

    public Long getIdOrigen() {
        return idOrigen;
    }

    public void setIdOrigen(Long idOrigen) {
        this.idOrigen = idOrigen;
    }

    public List<String> getConceptos() {
        return conceptos;
    }

    public void setConceptos(List<String> conceptos) {
        this.conceptos = conceptos;
    }

    public List<Long> getIdsBanda() {
        return idsBanda;
    }

    public void setIdsBanda(List<Long> idsBanda) {
        this.idsBanda = idsBanda;
    }

    public List<Long> getIdsProducto() {
        return idsProducto;
    }

    public void setIdsProducto(List<Long> idsProducto) {
        this.idsProducto = idsProducto;
    }

    public List<Long> getIdsTipoPrestamo() {
        return idsTipoPrestamo;
    }

    public void setIdsTipoPrestamo(List<Long> idsTipoPrestamo) {
        this.idsTipoPrestamo = idsTipoPrestamo;
    }

    public List<Long> getIdsTipoAporte() {
        return idsTipoAporte;
    }

    public void setIdsTipoAporte(List<Long> idsTipoAporte) {
        this.idsTipoAporte = idsTipoAporte;
    }

    public List<Long> getIdsEntidad() {
        return idsEntidad;
    }

    public void setIdsEntidad(List<Long> idsEntidad) {
        this.idsEntidad = idsEntidad;
    }

    public List<String> getCuentasContables() {
        return cuentasContables;
    }

    public void setCuentasContables(List<String> cuentasContables) {
        this.cuentasContables = cuentasContables;
    }

    public LocalDate getFechaDesde() {
        return fechaDesde;
    }

    public void setFechaDesde(LocalDate fechaDesde) {
        this.fechaDesde = fechaDesde;
    }

    public LocalDate getFechaHasta() {
        return fechaHasta;
    }

    public void setFechaHasta(LocalDate fechaHasta) {
        this.fechaHasta = fechaHasta;
    }

    public Integer getPagina() {
        return pagina;
    }

    public void setPagina(Integer pagina) {
        this.pagina = pagina;
    }

    public Integer getTamanio() {
        return tamanio;
    }

    public void setTamanio(Integer tamanio) {
        this.tamanio = tamanio;
    }

    public String getOrdenarPor() {
        return ordenarPor;
    }

    public void setOrdenarPor(String ordenarPor) {
        this.ordenarPor = ordenarPor;
    }

    public String getOrden() {
        return orden;
    }

    public void setOrden(String orden) {
        this.orden = orden;
    }
}
