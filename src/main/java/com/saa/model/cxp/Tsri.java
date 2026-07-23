package com.saa.model.cxp;

import java.io.Serializable;

import com.saa.model.cnt.PlanCuenta;

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
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

/**
 * Pojo mapeo de tabla PGS.TSRI (módulo CXP).
 * Contiene los tipos de información del SRI relacionados con una categoría.
 */
@SuppressWarnings("serial")
@Entity(name = "TsriCompra")
@Table(name = "TSRI", schema = "PGS")
@SequenceGenerator(name = "SQ_TSRI_CXP", sequenceName = "PGS.SQ_TSRI", allocationSize = 1)
@NamedQueries({
    @NamedQuery(name = "TsriCompraAll", query = "select e from TsriCompra e"),
    @NamedQuery(name = "TsriCompraId",  query = "select e from TsriCompra e where e.id = :id")
})
public class Tsri implements Serializable {

    @Basic
    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_TSRI_CXP")
    private Long id;

    /** Relación con la categoría LSRI (CXP). */
    @ManyToOne
    @JoinColumn(name = "LSRI", referencedColumnName = "TABLA")
    private Lsri lsri;

    /** Código del tipo de información. */
    @Basic
    @Column(name = "CODIGO", length = 500)
    private String codigo;

    /** Detalle descriptivo del tipo. */
    @Basic
    @Column(name = "DETALLE", length = 1000)
    private String detalle;

    /** Porcentaje asociado. */
    @Basic
    @Column(name = "PORCENTAJE")
    private Double porcentaje;

    /** Valor asociado. */
    @Basic
    @Column(name = "VALOR")
    private Double valor;

    /** Texto descriptivo adicional. */
    @Basic
    @Column(name = "TEXTO", length = 1000)
    private String texto;

    /** Estado: 1 = activo, 2 = inactivo. */
    @Basic
    @Column(name = "ESTADO")
    private Long estado;

    /** Plan de cuenta relacionado. */
    @ManyToOne
    @JoinColumn(name = "PLNNCDGO", referencedColumnName = "PLNNCDGO")
    private PlanCuenta planCuenta;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Lsri getLsri() { return lsri; }
    public void setLsri(Lsri lsri) { this.lsri = lsri; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getDetalle() { return detalle; }
    public void setDetalle(String detalle) { this.detalle = detalle; }

    public Double getPorcentaje() { return porcentaje; }
    public void setPorcentaje(Double porcentaje) { this.porcentaje = porcentaje; }

    public Double getValor() { return valor; }
    public void setValor(Double valor) { this.valor = valor; }

    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }

    public Long getEstado() { return estado; }
    public void setEstado(Long estado) { this.estado = estado; }

    public PlanCuenta getPlanCuenta() { return planCuenta; }
    public void setPlanCuenta(PlanCuenta planCuenta) { this.planCuenta = planCuenta; }
}
