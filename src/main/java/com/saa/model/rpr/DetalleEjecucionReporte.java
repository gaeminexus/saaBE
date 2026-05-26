package com.saa.model.rpr;

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
 * Entidad que representa la tabla RPR.EJRD - Detalle de Ejecución por Reporte G.
 * Una fila por cada reporte G (G40-G51) dentro de una ejecución mensual.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "EJRD", schema = "RPR")
@NamedQueries({
    @NamedQuery(name = "DetalleEjecucionReporteAll", query = "select e from DetalleEjecucionReporte e"),
    @NamedQuery(name = "DetalleEjecucionReporteId",  query = "select e from DetalleEjecucionReporte e where e.codigo = :id")
})
public class DetalleEjecucionReporte implements Serializable {

    /** Código único del registro (Identity). */
    @Id
    @Basic
    @Column(name = "EJRDCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /** Cabecera de ejecución a la que pertenece este detalle. */
    @ManyToOne
    @JoinColumn(name = "EJRCCDGO", referencedColumnName = "EJRCCDGO")
    private EjecucionReporte ejecucionReporte;

    /**
     * Tipo de reporte generado.
     * Valores: G40, G41, G42, G43, G44, G45, G46, G47, G48, G49, G50, G51.
     */
    @Basic
    @Column(name = "EJRDTPRP", length = 10)
    private String tipoReporte;

    /**
     * Estado del reporte.
     * 1 = OK, 2 = Con novedades, 3 = Pendiente.
     */
    @Basic
    @Column(name = "EJRDESTD")
    private Long estado;

    /** Fecha en que se generó este reporte G específico. */
    @Basic
    @Column(name = "EJRDFCGN")
    private LocalDate fechaGeneracion;

    /** Cantidad de registros generados para este reporte. */
    @Basic
    @Column(name = "EJRDCNRG")
    private Long cantidadRegistros;

    /** Descripción de novedades o errores encontrados al generar el reporte. */
    @Basic
    @Column(name = "EJRDNVDD", length = 1000)
    private String novedades;

    /**
     * Detalle original que este registro está corrigiendo.
     * Null si es ejecución inicial.
     */
    @ManyToOne
    @JoinColumn(name = "EJRDEJRO", referencedColumnName = "EJRDCDGO")
    private DetalleEjecucionReporte detalleOriginal;

    // ============================================================
    // Getters y Setters
    // ============================================================

    public Long getCodigo() { return codigo; }
    public void setCodigo(Long codigo) { this.codigo = codigo; }

    public EjecucionReporte getEjecucionReporte() { return ejecucionReporte; }
    public void setEjecucionReporte(EjecucionReporte ejecucionReporte) { this.ejecucionReporte = ejecucionReporte; }

    public String getTipoReporte() { return tipoReporte; }
    public void setTipoReporte(String tipoReporte) { this.tipoReporte = tipoReporte; }

    public Long getEstado() { return estado; }
    public void setEstado(Long estado) { this.estado = estado; }

    public LocalDate getFechaGeneracion() { return fechaGeneracion; }
    public void setFechaGeneracion(LocalDate fechaGeneracion) { this.fechaGeneracion = fechaGeneracion; }

    public Long getCantidadRegistros() { return cantidadRegistros; }
    public void setCantidadRegistros(Long cantidadRegistros) { this.cantidadRegistros = cantidadRegistros; }

    public String getNovedades() { return novedades; }
    public void setNovedades(String novedades) { this.novedades = novedades; }

    public DetalleEjecucionReporte getDetalleOriginal() { return detalleOriginal; }
    public void setDetalleOriginal(DetalleEjecucionReporte detalleOriginal) { this.detalleOriginal = detalleOriginal; }
}
