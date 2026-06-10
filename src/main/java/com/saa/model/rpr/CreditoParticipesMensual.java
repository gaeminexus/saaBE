package com.saa.model.rpr;

import java.io.Serializable;

import com.saa.model.crd.Entidad;
import com.saa.model.crd.TipoAporte;

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
 * Entidad que representa la tabla RPR.CPRM - CPRM (Crédito Partícipes Mensual).
 * Similar al G42 pero para uso interno del departamento de cartera.
 *
 * A diferencia del G42 (que tiene un registro por entidad con columnas por tipo de aporte),
 * esta tabla tiene UN REGISTRO POR CADA COMBINACIÓN entidad + tipo de aporte,
 * con el total acumulado de aportes de esa entidad para ese tipo.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "CPRM", schema = "RPR")
@NamedQueries({
    @NamedQuery(name = "CreditoParticipesMensualAll", query = "select e from CreditoParticipesMensual e"),
    @NamedQuery(name = "CreditoParticipesMensualId",  query = "select e from CreditoParticipesMensual e where e.codigo = :id")
})
public class CreditoParticipesMensual implements Serializable {

    /** Código único del registro (Identity). */
    @Id
    @Basic
    @Column(name = "CPRMCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /** Tipo de identificación del partícipe. */
    @Basic
    @Column(name = "CPRMTIDP", length = 50)
    private String tipoIdentificacion;

    /** Identificación del partícipe. */
    @Basic
    @Column(name = "CPRMIDPR", length = 50)
    private String identificacion;

    /** FK al tipo de aporte (CRD.TPAP) — nombre del tipo de aporte en el reporte. */
    @ManyToOne
    @JoinColumn(name = "TPAPCDGO", referencedColumnName = "TPAPCDGO")
    private TipoAporte tipoAporte;

    /**
     * Total acumulado de aportes de esta entidad para este tipo de aporte
     * (sumatoria de APRT.APRTVLRR hasta la fecha de corte).
     */
    @Basic
    @Column(name = "CPRMTTAL")
    private Double total;

    /** FK al control de ejecución (RPR.EJCC) al que pertenece este registro. */
    @ManyToOne
    @JoinColumn(name = "CPRMEJCC", referencedColumnName = "EJCCCDGO")
    private EjecucionReporteCartera ejecucionReporte;

    /** FK a la entidad (CRD.ENTD) — permite búsqueda rápida por entidad. */
    @ManyToOne
    @JoinColumn(name = "ENTDCDGO", referencedColumnName = "ENTDCDGO")
    private Entidad entidad;

    /** Nombre del estado de la entidad (Nuevo, Activo, Cesante, Jubilado, etc.). */
    @Basic
    @Column(name = "CPRMSTEN", length = 50)
    private String nombreEstado;

    // ============================================================
    // Getters y Setters
    // ============================================================

    public Long getCodigo() { return codigo; }
    public void setCodigo(Long codigo) { this.codigo = codigo; }

    public String getTipoIdentificacion() { return tipoIdentificacion; }
    public void setTipoIdentificacion(String tipoIdentificacion) { this.tipoIdentificacion = tipoIdentificacion; }

    public String getIdentificacion() { return identificacion; }
    public void setIdentificacion(String identificacion) { this.identificacion = identificacion; }

    public TipoAporte getTipoAporte() { return tipoAporte; }
    public void setTipoAporte(TipoAporte tipoAporte) { this.tipoAporte = tipoAporte; }

    public Double getTotal() { return total; }
    public void setTotal(Double total) { this.total = total; }

    public EjecucionReporteCartera getEjecucionReporte() { return ejecucionReporte; }
    public void setEjecucionReporte(EjecucionReporteCartera ejecucionReporte) { this.ejecucionReporte = ejecucionReporte; }

    public Entidad getEntidad() { return entidad; }
    public void setEntidad(Entidad entidad) { this.entidad = entidad; }

    public String getNombreEstado() { return nombreEstado; }
    public void setNombreEstado(String nombreEstado) { this.nombreEstado = nombreEstado; }
}