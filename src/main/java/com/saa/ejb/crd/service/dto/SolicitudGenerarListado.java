package com.saa.ejb.crd.service.dto;

import java.time.LocalDate;

/** Cuerpo de {@code POST /posg/listado} (contrato §3). */
public class SolicitudGenerarListado {

    private Long tipoSeguro;
    private LocalDate fechaCorte;
    private String usuario;
    private String observacion;
    /** S16: préstamos elegidos. Null = comportamiento anterior (todos los candidatos). */
    private java.util.List<Long> idsPrestamo;

    public Long getTipoSeguro() { return tipoSeguro; }
    public void setTipoSeguro(Long tipoSeguro) { this.tipoSeguro = tipoSeguro; }

    public LocalDate getFechaCorte() { return fechaCorte; }
    public void setFechaCorte(LocalDate fechaCorte) { this.fechaCorte = fechaCorte; }

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }

    public java.util.List<Long> getIdsPrestamo() { return idsPrestamo; }
    public void setIdsPrestamo(java.util.List<Long> idsPrestamo) { this.idsPrestamo = idsPrestamo; }
}
