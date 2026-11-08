package com.saa.ejb.crd.service.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Estado de la aprobación por niveles de un préstamo ({@code GET /prst/estadoAprobacion/{id}}), B4. {@code sinNiveles} =
 * no hay regla activa en toda la tabla (se aprueba como hoy) o no se pudo resolver una regla (ver {@code mensaje}).
 */
public class EstadoAprobacionPrestamo {

    /** La regla con la que se aprueba. */
    public static class ReglaResumen {
        private Long id;
        private String nombre;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getNombre() { return nombre; }
        public void setNombre(String nombre) { this.nombre = nombre; }
    }

    /** Un nivel con su estado. */
    public static class NivelEstado {
        private Long idNivel;
        private Long orden;
        private String nombre;
        private List<String> usuarios = new ArrayList<>();
        private String aprobadoPor;
        private LocalDateTime fecha;
        /** 1 aprobado, 2 negado; nulo = pendiente. */
        private Long decision;

        public Long getIdNivel() { return idNivel; }
        public void setIdNivel(Long idNivel) { this.idNivel = idNivel; }
        public Long getOrden() { return orden; }
        public void setOrden(Long orden) { this.orden = orden; }
        public String getNombre() { return nombre; }
        public void setNombre(String nombre) { this.nombre = nombre; }
        public List<String> getUsuarios() { return usuarios; }
        public void setUsuarios(List<String> usuarios) { this.usuarios = usuarios; }
        public String getAprobadoPor() { return aprobadoPor; }
        public void setAprobadoPor(String aprobadoPor) { this.aprobadoPor = aprobadoPor; }
        public LocalDateTime getFecha() { return fecha; }
        public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
        public Long getDecision() { return decision; }
        public void setDecision(Long decision) { this.decision = decision; }
    }

    private ReglaResumen regla;
    private boolean sinNiveles;
    private List<NivelEstado> niveles = new ArrayList<>();
    /** Orden del nivel que falta aprobar; nulo si ya están todos o no hay niveles. */
    private Long nivelPendiente;
    /** Total de niveles activos de la regla. */
    private int totalNiveles;
    /** {@code true} si el usuario de la consulta ({@code idUsuario}) puede aprobar hoy el nivel pendiente. */
    private boolean puedeAprobar;
    private String mensaje;

    public ReglaResumen getRegla() { return regla; }
    public void setRegla(ReglaResumen regla) { this.regla = regla; }
    public boolean isSinNiveles() { return sinNiveles; }
    public void setSinNiveles(boolean sinNiveles) { this.sinNiveles = sinNiveles; }
    public List<NivelEstado> getNiveles() { return niveles; }
    public void setNiveles(List<NivelEstado> niveles) { this.niveles = niveles; }
    public Long getNivelPendiente() { return nivelPendiente; }
    public void setNivelPendiente(Long nivelPendiente) { this.nivelPendiente = nivelPendiente; }
    public int getTotalNiveles() { return totalNiveles; }
    public void setTotalNiveles(int totalNiveles) { this.totalNiveles = totalNiveles; }
    public boolean isPuedeAprobar() { return puedeAprobar; }
    public void setPuedeAprobar(boolean puedeAprobar) { this.puedeAprobar = puedeAprobar; }
    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
}
