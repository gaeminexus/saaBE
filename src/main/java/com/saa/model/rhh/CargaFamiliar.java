package com.saa.model.rhh;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.saa.basico.util.EntidadAuditableFechaHora;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

/**
 * Dependiente del empleado. Determina el tope de gastos personales para la rebaja del impuesto a la renta y el reparto de utilidades.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "CRGF", schema = "RHH")
@NamedQueries({
    @NamedQuery(name = "CargaFamiliarId", query = "select e from CargaFamiliar e where e.codigo=:id"),
    @NamedQuery(name = "CargaFamiliarAll", query = "select e from CargaFamiliar e")
})
public class CargaFamiliar implements Serializable, EntidadAuditableFechaHora {

    /**
     * Codigo unico de la carga familiar.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic
    @Column(name = "CRGFCDGO")
    private Long codigo;

    /**
     * Empleado del que depende la carga.
     */
    @ManyToOne
    @JoinColumn(name = "MPLDCDGO", referencedColumnName = "MPLDCDGO")
    private Empleado empleado;

    /**
     * Parentesco: detalle del rubro RHH_PARENTESCO_CARGA.
     */
    @Basic
    @Column(name = "CRGFPRNT")
    private Long parentesco;

    /**
     * Identificacion del dependiente.
     */
    @Basic
    @Column(name = "CRGFIDNT", length = 20)
    private String identificacion;

    /**
     * Apellidos del dependiente.
     */
    @Basic
    @Column(name = "CRGFAPLL", length = 100)
    private String apellidos;

    /**
     * Nombres del dependiente.
     */
    @Basic
    @Column(name = "CRGFNMBR", length = 100)
    private String nombres;

    /**
     * Fecha de nacimiento del dependiente.
     */
    @Basic
    @Column(name = "CRGFFCHN")
    private LocalDate fechaNacimiento;

    /**
     * El dependiente tiene discapacidad reconocida (S/N).
     */
    @Basic
    @Column(name = "CRGFDSCP", length = 1)
    private String discapacidad;

    /**
     * Porcentaje de discapacidad del dependiente.
     */
    @Basic
    @Column(name = "CRGFPRDS")
    private Double porcentajeDiscapacidad;

    /**
     * Califica como carga para la rebaja de gastos personales del IR (S/N).
     */
    @Basic
    @Column(name = "CRGFIRRB", length = 1)
    private String calificaIr;

    /**
     * Califica para el reparto de utilidades (S/N).
     */
    @Basic
    @Column(name = "CRGFUTIL", length = 1)
    private String calificaUtilidades;

    /**
     * Depende economicamente del empleado (S/N).
     */
    @Basic
    @Column(name = "CRGFDPEC", length = 1)
    private String dependeEconomicamente;

    /**
     * Fecha desde la que rige como carga.
     */
    @Basic
    @Column(name = "CRGFFCIN")
    private LocalDate fechaInicio;

    /**
     * Fecha hasta la que rige como carga.
     */
    @Basic
    @Column(name = "CRGFFCFN")
    private LocalDate fechaFin;

    /**
     * Estado del registro.
     */
    @Basic
    @Column(name = "CRGFESTD")
    private Long estado;

    /**
     * Fecha de registro.
     */
    @Basic
    @Column(name = "CRGFFCHR")
    private LocalDateTime fechaRegistro;

    /**
     * Usuario que registro.
     */
    @Basic
    @Column(name = "CRGFUSRR", length = 60)
    private String usuarioRegistro;

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public Empleado getEmpleado() {
        return empleado;
    }

    public void setEmpleado(Empleado empleado) {
        this.empleado = empleado;
    }

    public Long getParentesco() {
        return parentesco;
    }

    public void setParentesco(Long parentesco) {
        this.parentesco = parentesco;
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

    public String getDiscapacidad() {
        return discapacidad;
    }

    public void setDiscapacidad(String discapacidad) {
        this.discapacidad = discapacidad;
    }

    public Double getPorcentajeDiscapacidad() {
        return porcentajeDiscapacidad;
    }

    public void setPorcentajeDiscapacidad(Double porcentajeDiscapacidad) {
        this.porcentajeDiscapacidad = porcentajeDiscapacidad;
    }

    public String getCalificaIr() {
        return calificaIr;
    }

    public void setCalificaIr(String calificaIr) {
        this.calificaIr = calificaIr;
    }

    public String getCalificaUtilidades() {
        return calificaUtilidades;
    }

    public void setCalificaUtilidades(String calificaUtilidades) {
        this.calificaUtilidades = calificaUtilidades;
    }

    public String getDependeEconomicamente() {
        return dependeEconomicamente;
    }

    public void setDependeEconomicamente(String dependeEconomicamente) {
        this.dependeEconomicamente = dependeEconomicamente;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDate fechaFin) {
        this.fechaFin = fechaFin;
    }

    public Long getEstado() {
        return estado;
    }

    public void setEstado(Long estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public String getUsuarioRegistro() {
        return usuarioRegistro;
    }

    public void setUsuarioRegistro(String usuarioRegistro) {
        this.usuarioRegistro = usuarioRegistro;
    }
}
