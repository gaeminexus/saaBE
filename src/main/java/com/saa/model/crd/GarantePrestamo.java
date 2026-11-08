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
 * Representa la tabla CRD.GRPR (garante del préstamo; DDL en {@code crd/sql/369}). El garante firma el pagaré junto con
 * el titular (DISENO-SOLICITUD-V1-AUTORIZACION-Y-REGLAS-OTORGAMIENTO.md, B3).
 *
 * <p>Un solo garante ACTIVO ({@code estado = 1}) por préstamo (índice único sobre los activos). Quitarlo es pasarlo a
 * {@code estado = 0}: no se borra. Si el garante es partícipe, {@code idEntidad} lo enlaza con {@code CRD.ENTD} y el
 * servicio toma de ahí el nombre y la cédula; si es un tercero, {@code idEntidad} queda nulo.</p>
 *
 * <p>{@code idPrestamo} e {@code idEntidad} van como código simple (no {@code @ManyToOne}): la entidad se serializa
 * directo a JSON y las relaciones EAGER arrastrarían el grafo completo del préstamo y del partícipe.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "GRPR", schema = "CRD")
@SequenceGenerator(name = "SQ_GRPRCDGO", sequenceName = "CRD.SQ_GRPRCDGO", allocationSize = 1)
@NamedQueries({
    @NamedQuery(name = "GarantePrestamoAll", query = "select e from GarantePrestamo e"),
    @NamedQuery(name = "GarantePrestamoId",  query = "select e from GarantePrestamo e where e.codigo = :id")
})
public class GarantePrestamo implements Serializable {

    /** Código PK. */
    @Id
    @Basic
    @Column(name = "GRPRCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_GRPRCDGO")
    private Long codigo;

    /** Préstamo garantizado (CRD.PRST). Obligatorio. */
    @Basic
    @Column(name = "PRSTCDGO")
    private Long idPrestamo;

    /** Entidad del garante si es partícipe (CRD.ENTD); nulo si es un tercero. */
    @Basic
    @Column(name = "ENTDCDGO")
    private Long idEntidad;

    /** Apellidos y nombres del garante. */
    @Basic
    @Column(name = "GRPRNMBR", length = 200)
    private String nombre;

    /** Cédula del garante. */
    @Basic
    @Column(name = "GRPRNMID", length = 20)
    private String cedula;

    /** Dirección del garante. */
    @Basic
    @Column(name = "GRPRDRCC", length = 500)
    private String direccion;

    /** Teléfono del garante. */
    @Basic
    @Column(name = "GRPRTLFN", length = 50)
    private String telefono;

    /** Correo del garante. */
    @Basic
    @Column(name = "GRPRCRRE", length = 100)
    private String correo;

    /** 1 activo, 0 anulado. */
    @Basic
    @Column(name = "GRPRESTD")
    private Long estado;

    @Basic
    @Column(name = "GRPRUSRG", length = 50)
    private String usuarioRegistro;

    @Basic
    @Column(name = "GRPRFCRG")
    private LocalDateTime fechaRegistro;

    @Basic
    @Column(name = "GRPRUSMD", length = 50)
    private String usuarioModificacion;

    @Basic
    @Column(name = "GRPRFCMD")
    private LocalDateTime fechaModificacion;

    public Long getCodigo() { return codigo; }
    public void setCodigo(Long codigo) { this.codigo = codigo; }
    public Long getIdPrestamo() { return idPrestamo; }
    public void setIdPrestamo(Long idPrestamo) { this.idPrestamo = idPrestamo; }
    public Long getIdEntidad() { return idEntidad; }
    public void setIdEntidad(Long idEntidad) { this.idEntidad = idEntidad; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getCedula() { return cedula; }
    public void setCedula(String cedula) { this.cedula = cedula; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
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
