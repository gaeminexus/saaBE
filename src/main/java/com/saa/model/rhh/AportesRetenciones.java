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
 * Aportes o retenciones calculadas en la nómina (IESS, SRI u otros).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "PRTE", schema = "RHH")
@NamedQueries({
    @NamedQuery(name = "AportesRetenciones.findAll",
                query = "select a from AportesRetenciones a"),
    @NamedQuery(name = "AportesRetenciones.findById",
                query = "select a from AportesRetenciones a where a.codigo = :id"),
    @NamedQuery(name = "AportesRetenciones.findByNomina",
                query = "select a from AportesRetenciones a where a.nomina.codigo = :nominaId"),
    @NamedQuery(name = "AportesRetenciones.findByEntidad",
                query = "select a from AportesRetenciones a where a.entidad = :entidad")
})
public class AportesRetenciones implements Serializable {

    /**
     * Código único del aporte/retención.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic
    @Column(name = "PRTECDGO")
    private Long codigo;

    /**
     * Nómina asociada.
     */
    @ManyToOne
    @JoinColumn(name = "NMNACDGO", referencedColumnName = "NMNACDGO", nullable = false)
    private Nomina nomina;

    /**
     * Entidad (IESS, SRI u otra).
     */
    @Basic
    @Column(name = "PRTEENTD", length = 10, nullable = false)
    private String entidad;

    /**
     * Concepto del aporte/retención.
     */
    @Basic
    @Column(name = "PRTECNCP", length = 120, nullable = false)
    private String concepto;

    /**
     * Base de cálculo.
     */
    @Basic
    @Column(name = "PRTEBSEE", precision = 12, scale = 2, nullable = false)
    private Double baseCalculo;

    /**
     * Porcentaje aplicado.
     */
    @Basic
    @Column(name = "PRTEPRCN", precision = 7, scale = 4, nullable = false)
    private Double porcentaje;

    /**
     * Valor calculado.
     */
    @Basic
    @Column(name = "PRTEVLRO", precision = 12, scale = 2, nullable = false)
    private Double valor;

    /**
     * Fecha de registro.
     */
    @Basic
    @Column(name = "PRTEFCHR", nullable = false)
    private LocalDate fechaRegistro;

    /**
     * Usuario que registró.
     */
    @Basic
    @Column(name = "PRTEUSRR", length = 60)
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

    public Nomina getNomina() {
        return nomina;
    }

    public void setNomina(Nomina nomina) {
        this.nomina = nomina;
    }

    public String getEntidad() {
        return entidad;
    }

    public void setEntidad(String entidad) {
        this.entidad = entidad;
    }

    public String getConcepto() {
        return concepto;
    }

    public void setConcepto(String concepto) {
        this.concepto = concepto;
    }

    public Double getBaseCalculo() {
        return baseCalculo;
    }

    public void setBaseCalculo(Double baseCalculo) {
        this.baseCalculo = baseCalculo;
    }

    public Double getPorcentaje() {
        return porcentaje;
    }

    public void setPorcentaje(Double porcentaje) {
        this.porcentaje = porcentaje;
    }

    public Double getValor() {
        return valor;
    }

    public void setValor(Double valor) {
        this.valor = valor;
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
