package com.saa.model.crd;

import java.io.Serializable;
import java.time.LocalDateTime;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

/**
 * Representa la tabla CRD.NVAP (NivelAprobacionPrestamo); DDL en {@code crd/sql/370}.
 * Es la nivel de aprobación de una regla (RGAP); se aprueban en orden 1..n. El número de niveles de una regla es cuántos NVAP activos tiene.
 * Las llaves foráneas van como código simple (no {@code @ManyToOne}): la entidad se serializa directo a JSON y los EAGER
 * arrastrarían el grafo completo. Diseño: DISENO-SOLICITUD-V1-AUTORIZACION-Y-REGLAS-OTORGAMIENTO.md, B4.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "NVAP", schema = "CRD")
@SequenceGenerator(name = "SQ_NVAPCDGO", sequenceName = "CRD.SQ_NVAPCDGO", allocationSize = 1)
@NamedQueries({
    @NamedQuery(name = "NivelAprobacionPrestamoAll", query = "select e from NivelAprobacionPrestamo e"),
    @NamedQuery(name = "NivelAprobacionPrestamoId",  query = "select e from NivelAprobacionPrestamo e where e.codigo = :id")
})
public class NivelAprobacionPrestamo implements Serializable {

    /** Código PK. */
    @Id
    @Basic
    @Column(name = "NVAPCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_NVAPCDGO")
    private Long codigo;

    /** Regla (CRD.RGAP). */
    @Basic
    @Column(name = "RGAPCDGO")
    private Long idRegla;

    /** Orden del nivel dentro de la regla, desde 1. */
    @Basic
    @Column(name = "NVAPORDN")
    private Long orden;

    /** Nombre del nivel (Jefe de Crédito, Gerencia, CAD…). */
    @Basic
    @Column(name = "NVAPNMBR")
    private String nombre;

    /** 1 activo, 0 inactivo. */
    @Basic
    @Column(name = "NVAPESTD")
    private Long estado;

    /** Usuario que registró. */
    @Basic
    @Column(name = "NVAPUSRG")
    private String usuarioRegistro;

    /** Fecha de registro. */
    @Basic
    @Column(name = "NVAPFCRG")
    private LocalDateTime fechaRegistro;

    /** Usuario que modificó. */
    @Basic
    @Column(name = "NVAPUSMD")
    private String usuarioModificacion;

    /** Fecha de modificación. */
    @Basic
    @Column(name = "NVAPFCMD")
    private LocalDateTime fechaModificacion;

    public Long getCodigo() { return codigo; }
    public void setCodigo(Long codigo) { this.codigo = codigo; }
    public Long getIdRegla() { return idRegla; }
    public void setIdRegla(Long idRegla) { this.idRegla = idRegla; }
    public Long getOrden() { return orden; }
    public void setOrden(Long orden) { this.orden = orden; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public Long getEstado() { return estado; }
    public void setEstado(Long estado) { this.estado = estado; }
    public String getUsuarioRegistro() { return usuarioRegistro; }
    public void setUsuarioRegistro(String usuarioRegistro) { this.usuarioRegistro = usuarioRegistro; }
    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }
    public String getUsuarioModificacion() { return usuarioModificacion; }
    public void setUsuarioModificacion(String usuarioModificacion) { this.usuarioModificacion = usuarioModificacion; }
    public LocalDateTime getFechaModificacion() { return fechaModificacion; }
    public void setFechaModificacion(LocalDateTime fechaModificacion) { this.fechaModificacion = fechaModificacion; }
}
