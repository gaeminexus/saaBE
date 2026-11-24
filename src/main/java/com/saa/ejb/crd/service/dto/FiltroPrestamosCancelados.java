package com.saa.ejb.crd.service.dto;

import java.io.Serializable;
import java.util.List;

/** Cuerpo de POST /rest/prcn/consultar y /rest/prcn/pdf (contrato §2.1). */
public class FiltroPrestamosCancelados implements Serializable {

    private static final long serialVersionUID = 1L;

    private String fechaDesde;
    private String fechaHasta;
    private Long idProducto;
    private Long idFilial;
    private List<Long> estados;
    private List<String> formas;
    private String usuario;

    public String getFechaDesde() { return fechaDesde; }
    public void setFechaDesde(String fechaDesde) { this.fechaDesde = fechaDesde; }
    public String getFechaHasta() { return fechaHasta; }
    public void setFechaHasta(String fechaHasta) { this.fechaHasta = fechaHasta; }
    public Long getIdProducto() { return idProducto; }
    public void setIdProducto(Long idProducto) { this.idProducto = idProducto; }
    public Long getIdFilial() { return idFilial; }
    public void setIdFilial(Long idFilial) { this.idFilial = idFilial; }
    public List<Long> getEstados() { return estados; }
    public void setEstados(List<Long> estados) { this.estados = estados; }
    public List<String> getFormas() { return formas; }
    public void setFormas(List<String> formas) { this.formas = formas; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
}
