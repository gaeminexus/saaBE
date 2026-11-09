package com.saa.ejb.crd.service.dto;

/**
 * Respuesta de {@code GET /rgap/simular}: qué regla aplicaría a un tipo, monto y plazo, con sus niveles. Si no hay ninguna
 * regla activa en toda la tabla ({@code sinReglasActivas}), el préstamo se aprueba como hoy, en un paso. Si la
 * configuración es ambigua o falta una regla, {@code regla} es nulo y {@code mensaje} dice por qué.
 */
public class SimulacionReglaAprobacion {

    private boolean sinReglasActivas;
    private ReglaAprobacionCompleta regla;
    private String mensaje;
    /** Operación con la que se simuló: 1 NUEVO, 2 NOVACION, 3 REESTRUCTURACION. */
    private Long operacion;

    public Long getOperacion() { return operacion; }
    public void setOperacion(Long operacion) { this.operacion = operacion; }

    public boolean isSinReglasActivas() { return sinReglasActivas; }
    public void setSinReglasActivas(boolean sinReglasActivas) { this.sinReglasActivas = sinReglasActivas; }
    public ReglaAprobacionCompleta getRegla() { return regla; }
    public void setRegla(ReglaAprobacionCompleta regla) { this.regla = regla; }
    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
}
