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
 * Periodo de nómina (año/mes y rango de fechas).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "PRDN", schema = "RHH")
@NamedQueries({
    @NamedQuery(name = "PeriodoNomina.findAll", query = "select p from PeriodoNomina p"),
    @NamedQuery(name = "PeriodoNomina.findById", query = "select p from PeriodoNomina p where p.codigo = :id"),
    @NamedQuery(name = "PeriodoNomina.findAbiertos", query = "select p from PeriodoNomina p where p.estado = 'ABIERTO'"),
    @NamedQuery(name = "PeriodoNomina.findByAnioMes",
                query = "select p from PeriodoNomina p where p.anio = :anio and p.mes = :mes")
})
public class PeriodoNomina implements Serializable {

    /**
     * Código único del periodo.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic
    @Column(name = "PRDNCDGO")
    private Long codigo;

    /**
     * Año del periodo.
     */
    @Basic
    @Column(name = "PRDNANOO")
    private Integer anio;

    /**
     * Mes del periodo (1-12).
     */
    @Basic
    @Column(name = "PRDNMSEE")
    private Integer mes;

    /**
     * Fecha de inicio.
     */
    @Basic
    @Column(name = "PRDNFCHI")
    private LocalDate fechaInicio;

    /**
     * Fecha de fin.
     */
    @Basic
    @Column(name = "PRDNFCHF")
    private LocalDate fechaFin;

    /**
     * Estado del periodo (ABIERTO / CALCULADO / CERRADO).
     */
    @Basic
    @Column(name = "PRDNESTD")
    private String estado;

    /**
     * Fecha de registro.
     */
    @Basic
    @Column(name = "PRDNFCHR")
    private LocalDate fechaRegistro;

    /**
     * Usuario que registró.
     */
    @Basic
    @Column(name = "PRDNUSRR")
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

    public Integer getAnio() {
        return anio;
    }

    public void setAnio(Integer anio) {
        this.anio = anio;
    }

    public Integer getMes() {
        return mes;
    }

    public void setMes(Integer mes) {
        this.mes = mes;
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
