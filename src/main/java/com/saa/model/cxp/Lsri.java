package com.saa.model.cxp;

import java.io.Serializable;

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
 * Pojo mapeo de tabla PGS.LSRI (módulo CXP).
 * Contiene la lista de categorías de información del SRI.
 */
@SuppressWarnings("serial")
@Entity(name = "LsriCompra")
@Table(name = "LSRI", schema = "PGS")
@SequenceGenerator(name = "SQ_LSRI_CXP", sequenceName = "PGS.SQ_LSRI", allocationSize = 1)
@NamedQueries({
    @NamedQuery(name = "LsriCompraAll", query = "select e from LsriCompra e"),
    @NamedQuery(name = "LsriCompraId",  query = "select e from LsriCompra e where e.id = :id")
})
public class Lsri implements Serializable {

    @Basic
    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_LSRI_CXP")
    private Long id;

    /** Nombre de la tabla relacionada. */
    @Basic
    @Column(name = "TABLA", length = 100)
    private String tabla;

    /** Detalle descriptivo de la categoría. */
    @Basic
    @Column(name = "DETALLE", length = 500)
    private String detalle;

    /** Estado: 1 = activo, 2 = inactivo. */
    @Basic
    @Column(name = "ESTADO")
    private Long estado;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTabla() { return tabla; }
    public void setTabla(String tabla) { this.tabla = tabla; }

    public String getDetalle() { return detalle; }
    public void setDetalle(String detalle) { this.detalle = detalle; }

    public Long getEstado() { return estado; }
    public void setEstado(Long estado) { this.estado = estado; }
}
