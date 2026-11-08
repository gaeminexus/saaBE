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
 * Representa la tabla CRD.UNAP (UsuarioNivelAprobacion); DDL en {@code crd/sql/370}.
 * Es la usuario habilitado para aprobar un nivel (NVAP); basta uno de los del nivel. El usuario es SCP.PJRQ (sin FK: otro esquema).
 * Las llaves foráneas van como código simple (no {@code @ManyToOne}): la entidad se serializa directo a JSON y los EAGER
 * arrastrarían el grafo completo. Diseño: DISENO-SOLICITUD-V1-AUTORIZACION-Y-REGLAS-OTORGAMIENTO.md, B4.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "UNAP", schema = "CRD")
@SequenceGenerator(name = "SQ_UNAPCDGO", sequenceName = "CRD.SQ_UNAPCDGO", allocationSize = 1)
@NamedQueries({
    @NamedQuery(name = "UsuarioNivelAprobacionAll", query = "select e from UsuarioNivelAprobacion e"),
    @NamedQuery(name = "UsuarioNivelAprobacionId",  query = "select e from UsuarioNivelAprobacion e where e.codigo = :id")
})
public class UsuarioNivelAprobacion implements Serializable {

    /** Código PK. */
    @Id
    @Basic
    @Column(name = "UNAPCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_UNAPCDGO")
    private Long codigo;

    /** Nivel (CRD.NVAP). */
    @Basic
    @Column(name = "NVAPCDGO")
    private Long idNivel;

    /** Usuario (SCP.PJRQ.PJRQCDGO). */
    @Basic
    @Column(name = "UNAPPJRQ")
    private Long idUsuario;

    /** 1 activo, 0 inactivo. */
    @Basic
    @Column(name = "UNAPESTD")
    private Long estado;

    /** Usuario que registró. */
    @Basic
    @Column(name = "UNAPUSRG")
    private String usuarioRegistro;

    /** Fecha de registro. */
    @Basic
    @Column(name = "UNAPFCRG")
    private LocalDateTime fechaRegistro;

    /** Usuario que modificó. */
    @Basic
    @Column(name = "UNAPUSMD")
    private String usuarioModificacion;

    /** Fecha de modificación. */
    @Basic
    @Column(name = "UNAPFCMD")
    private LocalDateTime fechaModificacion;

    public Long getCodigo() { return codigo; }
    public void setCodigo(Long codigo) { this.codigo = codigo; }
    public Long getIdNivel() { return idNivel; }
    public void setIdNivel(Long idNivel) { this.idNivel = idNivel; }
    public Long getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Long idUsuario) { this.idUsuario = idUsuario; }
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
