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
 * Entidad que representa la tabla RPR.CG46 - G46 (Nuevos prestamos).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "CG46", schema = "RPR")
@NamedQueries({
    @NamedQuery(name = "NuevoPrestamoG46All", query = "select e from NuevoPrestamoG46 e"),
    @NamedQuery(name = "NuevoPrestamoG46Id",  query = "select e from NuevoPrestamoG46 e where e.codigo = :id")
})
public class NuevoPrestamoG46 implements Serializable {

    /** Código único del registro (Identity). */
    @Id
    @Basic
    @Column(name = "CG46CDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /** Tipo de identificación del sujeto. */
    @Basic
    @Column(name = "CG46TIDS", length = 50)
    private String tipoIdentificacion;

    /** Identificación del sujeto. */
    @Basic
    @Column(name = "CG46IDSJ", length = 50)
    private String identificacion;

    /** Número de operación. */
    @Basic
    @Column(name = "CG46NMOP", length = 50)
    private String numeroOperacion;

    /** Tipo de crédito/préstamo. */
    @Basic
    @Column(name = "CG46TDCP", length = 50)
    private String tipoCredito;

    /** Estado de la operación. */
    @Basic
    @Column(name = "CG46EDLO", length = 50)
    private String estadoOperacion;

    /** Situación de la operación. */
    @Basic
    @Column(name = "CG46SDLO", length = 50)
    private String situacionOperacion;

    /** Destino geográfico provincia. */
    @Basic
    @Column(name = "CG46DGPR", length = 100)
    private String destinoProvincia;

    /** Destino geográfico cantón. */
    @Basic
    @Column(name = "CG46DGCN", length = 100)
    private String destinoCanton;

    /** Destino geográfico parroquia. */
    @Basic
    @Column(name = "CG46DGPS", length = 100)
    private String destinoParroquia;

    /** Fecha de concesión. */
    @Basic
    @Column(name = "CG46FCCN")
    private LocalDate fechaConcesion;

    /** Fecha de vencimiento. */
    @Basic
    @Column(name = "CG46FCVN")
    private LocalDate fechaVencimiento;

    /** Valor de la operación. */
    @Basic
    @Column(name = "CG46VDLO")
    private Double valorOperacion;

    /** Tasa de interés nominal. */
    @Basic
    @Column(name = "CG46TDIN")
    private Double tasaInteresNominal;

    /** Periodicidad de pago. */
    @Basic
    @Column(name = "CG46PDDP", length = 50)
    private String periodicidadPago;

    /** Frecuencia de revisión. */
    @Basic
    @Column(name = "CG46FDRV", length = 50)
    private String frecuenciaRevision;

    /** Garantes o garantías. */
    @Basic
    @Column(name = "CG46GOGR", length = 200)
    private String garantias;

    /** FK al detalle de ejecución (RPR.EJRD) al que pertenece este registro. */
    @ManyToOne
    @JoinColumn(name = "CG46EJRD", referencedColumnName = "EJRDCDGO")
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

    public String getNumeroOperacion() { return numeroOperacion; }
    public void setNumeroOperacion(String numeroOperacion) { this.numeroOperacion = numeroOperacion; }

    public String getTipoCredito() { return tipoCredito; }
    public void setTipoCredito(String tipoCredito) { this.tipoCredito = tipoCredito; }

    public String getEstadoOperacion() { return estadoOperacion; }
    public void setEstadoOperacion(String estadoOperacion) { this.estadoOperacion = estadoOperacion; }

    public String getSituacionOperacion() { return situacionOperacion; }
    public void setSituacionOperacion(String situacionOperacion) { this.situacionOperacion = situacionOperacion; }

    public String getDestinoProvincia() { return destinoProvincia; }
    public void setDestinoProvincia(String destinoProvincia) { this.destinoProvincia = destinoProvincia; }

    public String getDestinoCanton() { return destinoCanton; }
    public void setDestinoCanton(String destinoCanton) { this.destinoCanton = destinoCanton; }

    public String getDestinoParroquia() { return destinoParroquia; }
    public void setDestinoParroquia(String destinoParroquia) { this.destinoParroquia = destinoParroquia; }

    public LocalDate getFechaConcesion() { return fechaConcesion; }
    public void setFechaConcesion(LocalDate fechaConcesion) { this.fechaConcesion = fechaConcesion; }

    public LocalDate getFechaVencimiento() { return fechaVencimiento; }
    public void setFechaVencimiento(LocalDate fechaVencimiento) { this.fechaVencimiento = fechaVencimiento; }

    public Double getValorOperacion() { return valorOperacion; }
    public void setValorOperacion(Double valorOperacion) { this.valorOperacion = valorOperacion; }

    public Double getTasaInteresNominal() { return tasaInteresNominal; }
    public void setTasaInteresNominal(Double tasaInteresNominal) { this.tasaInteresNominal = tasaInteresNominal; }

    public String getPeriodicidadPago() { return periodicidadPago; }
    public void setPeriodicidadPago(String periodicidadPago) { this.periodicidadPago = periodicidadPago; }

    public String getFrecuenciaRevision() { return frecuenciaRevision; }
    public void setFrecuenciaRevision(String frecuenciaRevision) { this.frecuenciaRevision = frecuenciaRevision; }

    public String getGarantias() { return garantias; }
    public void setGarantias(String garantias) { this.garantias = garantias; }

    public DetalleEjecucionReporte getDetalleEjecucion() { return detalleEjecucion; }
    public void setDetalleEjecucion(DetalleEjecucionReporte detalleEjecucion) { this.detalleEjecucion = detalleEjecucion; }
}