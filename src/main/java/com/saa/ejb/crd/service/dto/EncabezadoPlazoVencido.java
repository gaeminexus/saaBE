package com.saa.ejb.crd.service.dto;

/**
 * PARA/CC de la última declaración grabada ({@code GET /rest/plvn/ultimoEncabezado}, D21).
 * Los cuatro campos vienen en null si nunca se declaró nada (200, no 404).
 */
public class EncabezadoPlazoVencido {

    private String paraNombre;
    private String paraCargo;
    private String ccNombre;
    private String ccCargo;

    public String getParaNombre() { return paraNombre; }
    public void setParaNombre(String paraNombre) { this.paraNombre = paraNombre; }

    public String getParaCargo() { return paraCargo; }
    public void setParaCargo(String paraCargo) { this.paraCargo = paraCargo; }

    public String getCcNombre() { return ccNombre; }
    public void setCcNombre(String ccNombre) { this.ccNombre = ccNombre; }

    public String getCcCargo() { return ccCargo; }
    public void setCcCargo(String ccCargo) { this.ccCargo = ccCargo; }
}
