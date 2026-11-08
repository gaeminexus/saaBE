package com.saa.ejb.crd.service.dto;

import java.util.ArrayList;
import java.util.List;

import com.saa.model.crd.ReglaAprobacionPrestamo;

/** Una regla de aprobación con sus niveles activos (en orden) y los usuarios activos de cada nivel (B4). */
public class ReglaAprobacionCompleta {

    /** Un usuario habilitado para aprobar un nivel. */
    public static class UsuarioNivel {
        private Long codigo;
        private Long idUsuario;
        private String nombre;

        public Long getCodigo() { return codigo; }
        public void setCodigo(Long codigo) { this.codigo = codigo; }
        public Long getIdUsuario() { return idUsuario; }
        public void setIdUsuario(Long idUsuario) { this.idUsuario = idUsuario; }
        public String getNombre() { return nombre; }
        public void setNombre(String nombre) { this.nombre = nombre; }
    }

    /** Un nivel con sus usuarios. */
    public static class Nivel {
        private Long codigo;
        private Long orden;
        private String nombre;
        private List<UsuarioNivel> usuarios = new ArrayList<>();

        public Long getCodigo() { return codigo; }
        public void setCodigo(Long codigo) { this.codigo = codigo; }
        public Long getOrden() { return orden; }
        public void setOrden(Long orden) { this.orden = orden; }
        public String getNombre() { return nombre; }
        public void setNombre(String nombre) { this.nombre = nombre; }
        public List<UsuarioNivel> getUsuarios() { return usuarios; }
        public void setUsuarios(List<UsuarioNivel> usuarios) { this.usuarios = usuarios; }
    }

    private ReglaAprobacionPrestamo regla;
    private List<Nivel> niveles = new ArrayList<>();

    public ReglaAprobacionPrestamo getRegla() { return regla; }
    public void setRegla(ReglaAprobacionPrestamo regla) { this.regla = regla; }
    public List<Nivel> getNiveles() { return niveles; }
    public void setNiveles(List<Nivel> niveles) { this.niveles = niveles; }
}
