package com.saa.model.rhh;

import java.io.Serializable;
import java.time.LocalDate;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

/**
 * Registro maestro de empleados.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "MPLD", schema = "RHH")
@NamedQueries({
    @NamedQuery(name = "EmpleadoId", query = "select e from Empleado e where e.codigo=:id"),
    @NamedQuery(name = "EmpleadoAll", query = "select e from Empleado e")
})
public class Empleado implements Serializable {

    /**
     * Código único del empleado.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic
    @Column(name = "MPLDCDGO")
    private Long codigo;

    /**
     * Identificación (cédula/RUC/pasaporte).
     */
    @Basic
    @Column(name = "MPLDIDNT")
    private String identificacion;

    /**
     * Apellidos.
     */
    @Basic
    @Column(name = "MPLDAPLL")
    private String apellidos;

    /**
     * Nombres.
     */
    @Basic
    @Column(name = "MPLDNMBR")
    private String nombres;

    /**
     * Fecha de nacimiento.
     */
    @Basic
    @Column(name = "MPLDFCHN")
    private LocalDate fechaNacimiento;

    /**
     * Correo electrónico.
     */
    @Basic
    @Column(name = "MPLDEMAI")
    private String email;

    /**
     * Teléfono.
     */
    @Basic
    @Column(name = "MPLDTLFN")
    private String telefono;

    /**
     * Dirección.
     */
    @Basic
    @Column(name = "MPLDDRCC")
    private String direccion;

    /**
     * Estado del empleado (A=Activo, I=Inactivo).
     */
    @Basic
    @Column(name = "MPLDESTD")
    private String estado;

    /**
     * Fecha de registro.
     */
    @Basic
    @Column(name = "MPLDFCHR")
    private LocalDate fechaRegistro;

    /**
     * Usuario que registró.
     */
    @Basic
    @Column(name = "MPLDUSRR")
    private String usuarioRegistro;

    // =============================
    // Getters y Setters
    // =============================

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public String getIdentificacion() {
        return identificacion;
    }

    public void setIdentificacion(String identificacion) {
        this.identificacion = identificacion;
    }

    public String getApellidos() {
        return apellidos;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }

    public String getNombres() {
        return nombres;
    }

    public void setNombres(String nombres) {
        this.nombres = nombres;
    }

    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }

    public void setFechaNacimiento(LocalDate fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public LocalDate getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDate fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public String getUsuarioRegistro() {
        return usuarioRegistro;
    }

    public void setUsuarioRegistro(String usuarioRegistro) {
        this.usuarioRegistro = usuarioRegistro;
    }
}
