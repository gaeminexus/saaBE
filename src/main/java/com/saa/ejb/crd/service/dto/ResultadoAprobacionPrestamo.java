package com.saa.ejb.crd.service.dto;

import com.saa.model.crd.Prestamo;

/**
 * Resultado de {@code PrestamoService.aprobarConNiveles}. {@code aprobacionCompleta = false} = se aprobó un nivel que no
 * es el último: el préstamo sigue en GENERADO (1) y {@code mensaje} dice «Nivel N de M aprobado; falta: nombre».
 * {@code aprobacionCompleta = true}: corrió el aprobar completo (idAsoprep, orden de desembolso, asiento, VIGENTE).
 */
public class ResultadoAprobacionPrestamo {

    private Prestamo prestamo;
    private boolean aprobacionCompleta;
    private Long nivelAprobado;
    private int totalNiveles;
    private String siguienteNivel;
    private String mensaje;

    public Prestamo getPrestamo() { return prestamo; }
    public void setPrestamo(Prestamo prestamo) { this.prestamo = prestamo; }
    public boolean isAprobacionCompleta() { return aprobacionCompleta; }
    public void setAprobacionCompleta(boolean aprobacionCompleta) { this.aprobacionCompleta = aprobacionCompleta; }
    public Long getNivelAprobado() { return nivelAprobado; }
    public void setNivelAprobado(Long nivelAprobado) { this.nivelAprobado = nivelAprobado; }
    public int getTotalNiveles() { return totalNiveles; }
    public void setTotalNiveles(int totalNiveles) { this.totalNiveles = totalNiveles; }
    public String getSiguienteNivel() { return siguienteNivel; }
    public void setSiguienteNivel(String siguienteNivel) { this.siguienteNivel = siguienteNivel; }
    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
}
