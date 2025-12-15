package com.saa.model.credito;

import java.io.Serializable;
import java.time.LocalDateTime;

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
 * Representa la tabla DTSP (DatosPrestamo).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "DTSP", schema = "CRD")
@NamedQueries({
    @NamedQuery(name = "DatosPrestamoAll", query = "select e from DatosPrestamo e"),
    @NamedQuery(name = "DatosPrestamoId", query = "select e from DatosPrestamo e where e.codigo = :id")
})
public class DatosPrestamo implements Serializable {

    /** Código */
    @Id
    @Basic
    @Column(name = "DTSPCDGO")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /** FK - ID Préstamo */
    @ManyToOne
    @JoinColumn(name = "PRSTCDGO", referencedColumnName = "PRSTCDGO")
    private Prestamo prestamo;

    /** Total Salario */
    @Basic
    @Column(name = "DTSPTTSL")
    private Double totalSalario;

    /** Total Egresos */
    @Basic
    @Column(name = "DTSPTTEG")
    private Double totalEgresos;

    /** Otros ingresos ROL */
    @Basic
    @Column(name = "DTSPOTIR")
    private Double otrosIngresosRol;

    /** Otros ingresos Externos */
    @Basic
    @Column(name = "DTSPOTIE")
    private Double otrosIngresosExternos;

    /** Fecha registro */
    @Basic
    @Column(name = "DTSPFCRG")
    private LocalDateTime fechaRegistro;

    /** Usuario registro */
    @Basic
    @Column(name = "DTSPUSRG", length = 50)
    private String usuarioRegistro;

    /** Estado */
    @Basic
    @Column(name = "DTSPESTD")
    private Long estado;

    // ============================
    // Getters y Setters
    // ============================

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public Prestamo getPrestamo() {
        return prestamo;
    }

    public void setPrestamo(Prestamo prestamo) {
        this.prestamo = prestamo;
    }

    public Double getTotalSalario() {
        return totalSalario;
    }

    public void setTotalSalario(Double totalSalario) {
        this.totalSalario = totalSalario;
    }

    public Double getTotalEgresos() {
        return totalEgresos;
    }

    public void setTotalEgresos(Double totalEgresos) {
        this.totalEgresos = totalEgresos;
    }

    public Double getOtrosIngresosRol() {
        return otrosIngresosRol;
    }

    public void setOtrosIngresosRol(Double otrosIngresosRol) {
        this.otrosIngresosRol = otrosIngresosRol;
    }

    public Double getOtrosIngresosExternos() {
        return otrosIngresosExternos;
    }

    public void setOtrosIngresosExternos(Double otrosIngresosExternos) {
        this.otrosIngresosExternos = otrosIngresosExternos;
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

    public Long getEstado() {
        return estado;
    }

    public void setEstado(Long estado) {
        this.estado = estado;
    }
}

