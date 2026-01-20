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
 * Detalle de rubros calculados en la liquidación.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "TMLQ", schema = "RHH")
@NamedQueries({
    @NamedQuery(name = "DetalleLiquidacionId", query = "select e from DetalleLiquidacion e where e.codigo=:id"),
    @NamedQuery(name = "DetalleLiquidacionAll", query = "select e from DetalleLiquidacion e")
    
})
public class DetalleLiquidacion implements Serializable {

    /**
     * Código único del detalle.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic
    @Column(name = "TMLQCDGO")
    private Long codigo;

    /**
     * Liquidación asociada.
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "LQDCCDGO", nullable = false)
    private Liquidacion liquidacion;

    /**
     * Valor del rubro.
     */
    @Basic
    @Column(name = "TMLQVLRO")
    private Double valor;

    /**
     * Descripción del rubro.
     */
    @Basic
    @Column(name = "TMLQDSCR")
    private String descripcion;

    /**
     * Fecha de registro.
     */
    @Basic
    @Column(name = "TMLQFCHR", nullable = false)
    private LocalDate fechaRegistro;

    /**
     * Usuario que registró.
     */
    @Basic
    @Column(name = "TMLQUSRR")
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

    public Liquidacion getLiquidacion() {
        return liquidacion;
    }

    public void setLiquidacion(Liquidacion liquidacion) {
        this.liquidacion = liquidacion;
    }


    public Double getValor() {
        return valor;
    }

    public void setValor(Double valor) {
        this.valor = valor;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
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
