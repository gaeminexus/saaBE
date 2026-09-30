package com.saa.ejb.crd.service.dto;

import java.time.LocalDate;
import java.util.List;

/** Cuerpo de {@code POST /rest/plvn/declarar} (§5 del contrato). */
public class SolicitudDeclararPlazoVencido {

    private LocalDate fechaCorte;
    private String usuario;
    private String paraNombre;
    private String paraCargo;
    private String ccNombre;
    private String ccCargo;
    private List<ItemDeclararPlazoVencido> prestamos;

    public LocalDate getFechaCorte() { return fechaCorte; }
    public void setFechaCorte(LocalDate fechaCorte) { this.fechaCorte = fechaCorte; }

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    public String getParaNombre() { return paraNombre; }
    public void setParaNombre(String paraNombre) { this.paraNombre = paraNombre; }

    public String getParaCargo() { return paraCargo; }
    public void setParaCargo(String paraCargo) { this.paraCargo = paraCargo; }

    public String getCcNombre() { return ccNombre; }
    public void setCcNombre(String ccNombre) { this.ccNombre = ccNombre; }

    public String getCcCargo() { return ccCargo; }
    public void setCcCargo(String ccCargo) { this.ccCargo = ccCargo; }

    public List<ItemDeclararPlazoVencido> getPrestamos() { return prestamos; }
    public void setPrestamos(List<ItemDeclararPlazoVencido> prestamos) { this.prestamos = prestamos; }
}
