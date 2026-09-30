package com.saa.ejb.crd.service.dto;

import java.time.LocalDate;

/** Cuerpo de {@code POST /rest/plvn/{id}/liquidar} (§6 del contrato). */
public class SolicitudLiquidarPlazoVencido {

    private LocalDate fechaCorte;
    private String usuario;

    public LocalDate getFechaCorte() { return fechaCorte; }
    public void setFechaCorte(LocalDate fechaCorte) { this.fechaCorte = fechaCorte; }

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
}
