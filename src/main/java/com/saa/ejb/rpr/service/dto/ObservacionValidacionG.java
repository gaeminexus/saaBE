package com.saa.ejb.rpr.service.dto;

/**
 * C39 (23-nov-2026) — una observación de la validación previa de las estructuras G40–G51 de una ejecución: lo que la
 * Superintendencia rechazaría al cargarlas (campos obligatorios vacíos, fechas de relleno, sujetos u operaciones que
 * no constan en la estructura de la que dependen). Contrato de {@code GET /rest/ejrc/validar/{codigo}}.
 * No lleva identificaciones de personas: los ejemplos son números de operación o códigos de entidad del SAA.
 */
public class ObservacionValidacionG {

    public static final String ERROR = "ERROR";
    public static final String ADVERTENCIA = "ADVERTENCIA";

    /** Estructura (G41, G46, ...). */
    private String estructura;
    /** ERROR (la Super la rechaza) o ADVERTENCIA (revisar). */
    private String nivel;
    /** Regla que no se cumple, en palabras. */
    private String regla;
    /** Registros que no la cumplen. */
    private Long cantidad;
    /** Hasta 10 ejemplos (operación o entidad), separados por coma. */
    private String ejemplos;

    public ObservacionValidacionG() {}

    public ObservacionValidacionG(String estructura, String nivel, String regla, Long cantidad, String ejemplos) {
        this.estructura = estructura;
        this.nivel = nivel;
        this.regla = regla;
        this.cantidad = cantidad;
        this.ejemplos = ejemplos;
    }

    public String getEstructura() { return estructura; }
    public void setEstructura(String estructura) { this.estructura = estructura; }
    public String getNivel() { return nivel; }
    public void setNivel(String nivel) { this.nivel = nivel; }
    public String getRegla() { return regla; }
    public void setRegla(String regla) { this.regla = regla; }
    public Long getCantidad() { return cantidad; }
    public void setCantidad(Long cantidad) { this.cantidad = cantidad; }
    public String getEjemplos() { return ejemplos; }
    public void setEjemplos(String ejemplos) { this.ejemplos = ejemplos; }
}
