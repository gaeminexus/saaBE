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
 * Entidad que representa la tabla RPR.CG43 - G43 (Partícipes cesantes).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "CG43", schema = "RPR")
@NamedQueries({
    @NamedQuery(name = "ParticipeCesanteG43All", query = "select e from ParticipeCesanteG43 e"),
    @NamedQuery(name = "ParticipeCesanteG43Id",  query = "select e from ParticipeCesanteG43 e where e.codigo = :id")
})
public class ParticipeCesanteG43 implements Serializable {

    /** Código único del registro (Identity). */
    @Id
    @Basic
    @Column(name = "CG43CDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /** Tipo de identificación del partícipe. */
    @Basic
    @Column(name = "CG43TIDP", length = 50)
    private String tipoIdentificacion;

    /** Identificación del partícipe. */
    @Basic
    @Column(name = "CG43IDPR", length = 50)
    private String identificacion;

    /** Fecha del término de la relación laboral. */
    @Basic
    @Column(name = "CG43FTRL")
    private LocalDate fechaTerminoRelacionLaboral;

    /** Número de imposiciones acumuladas personales. */
    @Basic
    @Column(name = "CG43NIAP")
    private Long numeroImposicionesPersonales;

    /** Número de imposiciones acumuladas patronales. */
    @Basic
    @Column(name = "CG43NIAT")
    private Long numeroImposicionesPatronales;

    /** Fecha de liquidación. */
    @Basic
    @Column(name = "CG43FCLQ")
    private LocalDate fechaLiquidacion;

    /** Saldo de la cuenta individual. */
    @Basic
    @Column(name = "CG43SDCI")
    private Double saldoCuentaIndividual;

    /** Valores compensados al partícipe. */
    @Basic
    @Column(name = "CG43VCAP")
    private Double valoresCompensados;

    /** Valores pagados al partícipe. */
    @Basic
    @Column(name = "CG43VPAP")
    private Double valoresPagados;

    /** FK al detalle de ejecución (RPR.EJRD) al que pertenece este registro. */
    @ManyToOne
    @JoinColumn(name = "CG43EJRD", referencedColumnName = "EJRDCDGO")
    private DetalleEjecucionReporte detalleEjecucion;

    // ============================================================
    // Getters y Setters
    // ============================================================

    public Long getCodigo() { return codigo; }
    public void setCodigo(Long codigo) { this.codigo = codigo; }

    public String getTipoIdentificacion() { return tipoIdentificacion; }
    public void setTipoIdentificacion(String tipoIdentificacion) { this.tipoIdentificacion = tipoIdentificacion; }

    public String getIdentificacion() { return identificacion; }
    public void setIdentificacion(String identificacion) { this.identificacion = identificacion; }

    public LocalDate getFechaTerminoRelacionLaboral() { return fechaTerminoRelacionLaboral; }
    public void setFechaTerminoRelacionLaboral(LocalDate fechaTerminoRelacionLaboral) { this.fechaTerminoRelacionLaboral = fechaTerminoRelacionLaboral; }

    public Long getNumeroImposicionesPersonales() { return numeroImposicionesPersonales; }
    public void setNumeroImposicionesPersonales(Long numeroImposicionesPersonales) { this.numeroImposicionesPersonales = numeroImposicionesPersonales; }

    public Long getNumeroImposicionesPatronales() { return numeroImposicionesPatronales; }
    public void setNumeroImposicionesPatronales(Long numeroImposicionesPatronales) { this.numeroImposicionesPatronales = numeroImposicionesPatronales; }

    public LocalDate getFechaLiquidacion() { return fechaLiquidacion; }
    public void setFechaLiquidacion(LocalDate fechaLiquidacion) { this.fechaLiquidacion = fechaLiquidacion; }

    public Double getSaldoCuentaIndividual() { return saldoCuentaIndividual; }
    public void setSaldoCuentaIndividual(Double saldoCuentaIndividual) { this.saldoCuentaIndividual = saldoCuentaIndividual; }

    public Double getValoresCompensados() { return valoresCompensados; }
    public void setValoresCompensados(Double valoresCompensados) { this.valoresCompensados = valoresCompensados; }

    public Double getValoresPagados() { return valoresPagados; }
    public void setValoresPagados(Double valoresPagados) { this.valoresPagados = valoresPagados; }

    public DetalleEjecucionReporte getDetalleEjecucion() { return detalleEjecucion; }
    public void setDetalleEjecucion(DetalleEjecucionReporte detalleEjecucion) { this.detalleEjecucion = detalleEjecucion; }
}