package com.saa.model.rhh;

import java.io.Serializable;
import java.time.LocalDate;

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
 * Anexos o renovaciones asociados a un contrato.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "NXOO", schema = "RHH")
@NamedQueries({
    @NamedQuery(name = "AnexoContrato.findAll", query = "select a from AnexoContrato a"),
    @NamedQuery(name = "AnexoContrato.findById", query = "select a from AnexoContrato a where a.codigo = :id"),
    @NamedQuery(name = "AnexoContrato.findByContrato", query = "select a from AnexoContrato a where a.contratoEmpleado.codigo = :contratoId"),
    @NamedQuery(name = "AnexoContrato.findByTipo", query = "select a from AnexoContrato a where a.tipo = :tipo")
})
public class AnexoContrato implements Serializable {

    /**
     * Código único del anexo.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic
    @Column(name = "NXOOCDGO")
    private Long codigo;

    /**
     * Contrato asociado.
     */
    @ManyToOne
    @JoinColumn(name = "CNTRCDGO", referencedColumnName = "CNTRCDGO", nullable = false)
    private ContratoEmpleado contratoEmpleado;

    /**
     * Tipo de anexo (RENOVACION / ANEXO / ADENDUM).
     */
    @Basic
    @Column(name = "NXOOTPOO")
    private String tipo;

    /**
     * Fecha del anexo.
     */
    @Basic
    @Column(name = "NXOOFCHA", nullable = false)
    private LocalDate fechaAnexo;

    /**
     * Detalle del anexo.
     */
    @Basic
    @Column(name = "NXOODTLL")
    private String detalle;

    /**
     * Nuevo salario base (si aplica).
     */
    @Basic
    @Column(name = "NXOOSLRN")
    private Double nuevoSalario;

    /**
     * Nueva fecha fin (si aplica).
     */
    @Basic
    @Column(name = "NXOOFCHF")
    private LocalDate nuevaFechaFin;

    /**
     * Fecha de registro.
     */
    @Basic
    @Column(name = "NXOOFCHR", nullable = false)
    private LocalDate fechaRegistro;

    /**
     * Usuario que registró.
     */
    @Basic
    @Column(name = "NXOOUSRR")
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

    public ContratoEmpleado getContrato() {
        return contratoEmpleado;
    }

    public void setContrato(ContratoEmpleado contrato) {
        this.contratoEmpleado = contrato;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public LocalDate getFechaAnexo() {
        return fechaAnexo;
    }

    public void setFechaAnexo(LocalDate fechaAnexo) {
        this.fechaAnexo = fechaAnexo;
    }

    public String getDetalle() {
        return detalle;
    }

    public void setDetalle(String detalle) {
        this.detalle = detalle;
    }

    public Double getNuevoSalario() {
        return nuevoSalario;
    }

    public void setNuevoSalario(Double nuevoSalario) {
        this.nuevoSalario = nuevoSalario;
    }

    public LocalDate getNuevaFechaFin() {
        return nuevaFechaFin;
    }

    public void setNuevaFechaFin(LocalDate nuevaFechaFin) {
        this.nuevaFechaFin = nuevaFechaFin;
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
