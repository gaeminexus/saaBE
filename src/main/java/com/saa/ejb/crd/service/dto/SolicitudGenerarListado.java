package com.saa.ejb.crd.service.dto;

import java.time.LocalDate;

/** Cuerpo de {@code POST /posg/listado} (contrato §3). */
public class SolicitudGenerarListado {

    private Long tipoSeguro;
    private LocalDate fechaCorte;
    private String usuario;
    private String observacion;

    public Long getTipoSeguro() { return tipoSeguro; }
    public void setTipoSeguro(Long tipoSeguro) { this.tipoSeguro = tipoSeguro; }

    public LocalDate getFechaCorte() { return fechaCorte; }
    public void setFechaCorte(LocalDate fechaCorte) { this.fechaCorte = fechaCorte; }

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
}
