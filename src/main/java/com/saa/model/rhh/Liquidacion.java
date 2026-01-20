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
 * Liquidación de salida del empleado (finiquito).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "LQDC", schema = "RHH")
@NamedQueries({
    @NamedQuery(name = "LiquidacionId", query = "select e from Liquidacion e where e.codigo=:id"),
    @NamedQuery(name = "LiquidacionAll", query = "select e from Liquidacion e")
})
public class Liquidacion implements Serializable {

    /**
     * Código único de la liquidación.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic
    @Column(name = "LQDCCDGO")
    private Long codigo;

    /**
     * Empleado liquidado.
     */
    @ManyToOne
    @JoinColumn(name = "MPLDCDGO", referencedColumnName = "MPLDCDGO", nullable = false)
    private Empleado empleado;

    /**
     * Contrato liquidado.
     */
    @ManyToOne
    @JoinColumn(name = "CNTRCDGO", referencedColumnName = "CNTRCDGO", nullable = false)
    private ContratoEmpleado contratoEmpleado;

    /**
     * Fecha de salida.
     */
    @Basic
    @Column(name = "LQDCFCHS", nullable = false)
    private LocalDate fechaSalida;

    /**
     * Motivo de salida.
     */
    @Basic
    @Column(name = "LQDCMTVO")
    private String motivo;

    /**
     * Neto a pagar.
     */
    @Basic
    @Column(name = "LQDCNETO")
    private Double neto;

    /**
     * Estado de la liquidación.
     */
    @Basic
    @Column(name = "LQDCESTD")
    private String estado;

    /**
     * Fecha de registro.
     */
    @Basic
    @Column(name = "LQDCFCHR", nullable = false)
    private LocalDate fechaRegistro;

    /**
     * Usuario que registró.
     */
    @Basic
    @Column(name = "LQDCUSRR")
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

    public Empleado getEmpleado() {
        return empleado;
    }

    public void setEmpleado(Empleado empleado) {
        this.empleado = empleado;
    }

    public ContratoEmpleado getContrato() {
        return contratoEmpleado;
    }

    public void setContrato(ContratoEmpleado contrato) {
        this.contratoEmpleado = contrato;
    }

    public LocalDate getFechaSalida() {
        return fechaSalida;
    }

    public void setFechaSalida(LocalDate fechaSalida) {
        this.fechaSalida = fechaSalida;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public Double getNeto() {
        return neto;
    }

    public void setNeto(Double neto) {
        this.neto = neto;
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
