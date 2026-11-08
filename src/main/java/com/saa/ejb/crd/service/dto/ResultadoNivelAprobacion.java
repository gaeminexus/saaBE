package com.saa.ejb.crd.service.dto;

/**
 * Lo que dejó {@code NivelAprobacionService.registrarAprobacion}: si hay niveles, cuál se aprobó, cuántos son y si era el
 * último. Sin niveles configurados ({@code conNiveles = false}) el préstamo se aprueba como hoy.
 */
public class ResultadoNivelAprobacion {

    private boolean conNiveles;
    private boolean esUltimo;
    private Long ordenAprobado;
    private int totalNiveles;
    private String nombreNivel;
    private String siguienteNivel;
    private Long idRegla;
    private String usuario;

    public boolean isConNiveles() { return conNiveles; }
    public void setConNiveles(boolean conNiveles) { this.conNiveles = conNiveles; }
    public boolean isEsUltimo() { return esUltimo; }
    public void setEsUltimo(boolean esUltimo) { this.esUltimo = esUltimo; }
    public Long getOrdenAprobado() { return ordenAprobado; }
    public void setOrdenAprobado(Long ordenAprobado) { this.ordenAprobado = ordenAprobado; }
    public int getTotalNiveles() { return totalNiveles; }
    public void setTotalNiveles(int totalNiveles) { this.totalNiveles = totalNiveles; }
    public String getNombreNivel() { return nombreNivel; }
    public void setNombreNivel(String nombreNivel) { this.nombreNivel = nombreNivel; }
    public String getSiguienteNivel() { return siguienteNivel; }
    public void setSiguienteNivel(String siguienteNivel) { this.siguienteNivel = siguienteNivel; }
    public Long getIdRegla() { return idRegla; }
    public void setIdRegla(Long idRegla) { this.idRegla = idRegla; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
}
