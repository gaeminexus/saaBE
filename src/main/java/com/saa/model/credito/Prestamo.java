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
 * Representa la tabla PRST (Prestamo).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "PRST", schema = "CRD")
@NamedQueries({
    @NamedQuery(name = "PrestamoAll", query = "select e from Prestamo e"),
    @NamedQuery(name = "PrestamoId", query = "select e from Prestamo e where e.codigo = :id")
})
public class Prestamo implements Serializable {

    /**
     * Código.
     */
    @Id
    @Basic
    @Column(name = "PRSTCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    @Basic
    @Column(name = "PRSTIDAS")
    private Long idSistema;
    
    @ManyToOne
    @JoinColumn(name = "ENTDCDGO", referencedColumnName = "ENTDCDGO")
    private Entidad entidad;

    @ManyToOne
    @JoinColumn(name = "PRDCCDGO", referencedColumnName = "PRDCCDGO")
    private Producto producto;

    @Basic
    @Column(name = "PRSTTPAM")
    private Long tipoAmortizacion;

    @Basic
    @Column(name = "PRSTAMRT", length = 50)
    private String amortizacion;

    @Basic
    @Column(name = "PRSTFCHA")
    private LocalDateTime fecha;

    @Basic
    @Column(name = "PRSTFCIN")
    private LocalDateTime fechaInicio;

    @Basic
    @Column(name = "PRSTFCFN")
    private LocalDateTime fechaFin;

    @Basic
    @Column(name = "PRSTINNM")
    private Double interesNominal;

    @Basic
    @Column(name = "PRSTMNSL")
    private Double montoSolicitado;

    @Basic
    @Column(name = "PRSTVLCT")
    private Double valorCuota;

    @Basic
    @Column(name = "PRSTPLZO")
    private Long plazo;

    @Basic
    @Column(name = "PRSTMNLD")
    private Double montoLiquidacion;
    
    /** FK - Código Filial */
    @ManyToOne
    @JoinColumn(name = "FLLLCDGO", referencedColumnName = "FLLLCDGO")
    private Filial filial;

    @Basic
    @Column(name = "ESPSCDGO")
    private Long estadoPrestamo;

    @Basic
    @Column(name = "PRSTTSAA")
    private Double tasa;

    @Basic
    @Column(name = "PRSTTTPG")
    private Double totalPagado;

    @Basic
    @Column(name = "PRSTTTCP")
    private Double totalCapital;

    @Basic
    @Column(name = "PRSTTTIN")
    private Double totalInteres;

    @Basic
    @Column(name = "PRSTTTMR")
    private Double totalMora;

    @Basic
    @Column(name = "PRSTTTIV")
    private Double totalInteresVencido;

    @Basic
    @Column(name = "PRSTTTSG")
    private Double totalSeguros;

    @Basic
    @Column(name = "PRSTTTPR")
    private Double totalPrestamo;

    @Basic
    @Column(name = "PRSTSLXV")
    private Double saldoPorVencer;

    @Basic
    @Column(name = "PRSTSLVN")
    private Double saldoVencido;

    @Basic
    @Column(name = "PRSTSLTT")
    private Double saldoTotal;

    @Basic
    @Column(name = "PRSTFCRG")
    private LocalDateTime fechaRegistro;

    @Basic
    @Column(name = "PRSTUSRG", length = 2000)
    private String usuarioRegistro;

    @Basic
    @Column(name = "PRSTFCMD")
    private LocalDateTime fechaModificacion;

    @Basic
    @Column(name = "PRSTUSMD", length = 500)
    private String usuarioModificacion;

    @Basic
    @Column(name = "PRSTOBSR", length = 2000)
    private String observacion;

    @Basic
    @Column(name = "MTVPCDGO")
    private String motivoPrestamo;

    @Basic
    @Column(name = "PRSTESOP")
    private Long estadoOperacion;

    @Basic
    @Column(name = "PRSTTSNM")
    private Double tasaNominal;

    @Basic
    @Column(name = "PRSTTSEF")
    private Double tasaEfectiva;

    @Basic
    @Column(name = "PRSTESNV")
    private Long esNovacion;

    @Basic
    @Column(name = "PRSTRPRC")
    private Long reprocesado;

    @Basic
    @Column(name = "PRSTRSTR")
    private Long reestructurado;

    @Basic
    @Column(name = "PRSTRFNN")
    private Long refinanciado;

    @Basic
    @Column(name = "PRSTSLCP")
    private Double saldoCapital;

    @Basic
    @Column(name = "PRSTSLOT")
    private Double saldoOtros;

    @Basic
    @Column(name = "PRSTSLIN")
    private Double saldoInteres;

    @Basic
    @Column(name = "PRSTMRCL")
    private Double moraCalculada;

    @Basic
    @Column(name = "PRSTDSVN")
    private Long diasVencido;

    @Basic
    @Column(name = "PRSTMNNV")
    private Double montoNovacion;

    @Basic
    @Column(name = "PRSTINVR")
    private Double interesVariable;

    @Basic
    @Column(name = "PRSTUSAP", length = 50)
    private String usuarioAprobacion;

    @Basic
    @Column(name = "PRSTFCAP")
    private LocalDateTime fechaAprobacion;

    @Basic
    @Column(name = "PRSTFCAD")
    private LocalDateTime fechaAdjudicacion;

    @Basic
    @Column(name = "PRSTUSRC", length = 200)
    private String usuarioRechazo;

    @Basic
    @Column(name = "PRSTFCRC")
    private LocalDateTime fechaRechazo;

    @Basic
    @Column(name = "PRSTUSLG", length = 200)
    private String usuarioLegalizacion;

    @Basic
    @Column(name = "PRSTFCLG")
    private LocalDateTime fechaLegalizacion;

    @Basic
    @Column(name = "PRSTUSAC", length = 200)
    private String usuarioAcreditacion;

    @Basic
    @Column(name = "PRSTFCAC")
    private LocalDateTime fechaAcreditacion;

    @Basic
    @Column(name = "PRSTAJAP")
    private Long ajusteAportes;

    @Basic
    @Column(name = "PRSTMSCB")
    private Long mesesACobrar;

    @Basic
    @Column(name = "PRSTIDST")
    private Long estado;

    @Basic
    @Column(name = "PRSTFRTT")
    private Long firmadoTitular;

    // ============================================================
    // Getters y Setters
    // ============================================================

    public Long getCodigo() { return codigo; }
    public void setCodigo(Long codigo) { this.codigo = codigo; }

    public Long getIdSistema() { return idSistema; }
    public void setIdSistema(Long idSistema) { this.idSistema = idSistema; }

    public Entidad getEntidad() { return entidad; }
    public void setEntidad(Entidad entidad) { this.entidad = entidad; }

    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }

    public Long getTipoAmortizacion() { return tipoAmortizacion; }
    public void setTipoAmortizacion(Long tipoAmortizacion) { this.tipoAmortizacion = tipoAmortizacion; }

    public String getAmortizacion() { return amortizacion; }
    public void setAmortizacion(String amortizacion) { this.amortizacion = amortizacion; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    public LocalDateTime getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDateTime fechaInicio) { this.fechaInicio = fechaInicio; }

    public LocalDateTime getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDateTime fechaFin) { this.fechaFin = fechaFin; }

    public Double getInteresNominal() { return interesNominal; }
    public void setInteresNominal(Double interesNominal) { this.interesNominal = interesNominal; }

    public Double getMontoSolicitado() { return montoSolicitado; }
    public void setMontoSolicitado(Double montoSolicitado) { this.montoSolicitado = montoSolicitado; }

    public Double getValorCuota() { return valorCuota; }
    public void setValorCuota(Double valorCuota) { this.valorCuota = valorCuota; }

    public Long getPlazo() { return plazo; }
    public void setPlazo(Long plazo) { this.plazo = plazo; }

    public Double getMontoLiquidacion() { return montoLiquidacion; }
    public void setMontoLiquidacion(Double montoLiquidacion) { this.montoLiquidacion = montoLiquidacion; }

    public Filial getFilial() { return filial; }
    public void setFilial(Filial filial) { this.filial = filial; }

    public Long getEstadoPrestamo() { return estadoPrestamo; }
    public void setEstadoPrestamo(Long estadoPrestamo) { this.estadoPrestamo = estadoPrestamo; }

    public Double getTasa() { return tasa; }
    public void setTasa(Double tasa) { this.tasa = tasa; }

    public Double getTotalPagado() { return totalPagado; }
    public void setTotalPagado(Double totalPagado) { this.totalPagado = totalPagado; }

    public Double getTotalCapital() { return totalCapital; }
    public void setTotalCapital(Double totalCapital) { this.totalCapital = totalCapital; }

    public Double getTotalInteres() { return totalInteres; }
    public void setTotalInteres(Double totalInteres) { this.totalInteres = totalInteres; }

    public Double getTotalMora() { return totalMora; }
    public void setTotalMora(Double totalMora) { this.totalMora = totalMora; }

    public Double getTotalInteresVencido() { return totalInteresVencido; }
    public void setTotalInteresVencido(Double totalInteresVencido) { this.totalInteresVencido = totalInteresVencido; }

    public Double getTotalSeguros() { return totalSeguros; }
    public void setTotalSeguros(Double totalSeguros) { this.totalSeguros = totalSeguros; }

    public Double getTotalPrestamo() { return totalPrestamo; }
    public void setTotalPrestamo(Double totalPrestamo) { this.totalPrestamo = totalPrestamo; }

    public Double getSaldoPorVencer() { return saldoPorVencer; }
    public void setSaldoPorVencer(Double saldoPorVencer) { this.saldoPorVencer = saldoPorVencer; }

    public Double getSaldoVencido() { return saldoVencido; }
    public void setSaldoVencido(Double saldoVencido) { this.saldoVencido = saldoVencido; }

    public Double getSaldoTotal() { return saldoTotal; }
    public void setSaldoTotal(Double saldoTotal) { this.saldoTotal = saldoTotal; }

    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }

    public String getUsuarioRegistro() { return usuarioRegistro; }
    public void setUsuarioRegistro(String usuarioRegistro) { this.usuarioRegistro = usuarioRegistro; }

    public LocalDateTime getFechaModificacion() { return fechaModificacion; }
    public void setFechaModificacion(LocalDateTime fechaModificacion) { this.fechaModificacion = fechaModificacion; }

    public String getUsuarioModificacion() { return usuarioModificacion; }
    public void setUsuarioModificacion(String usuarioModificacion) { this.usuarioModificacion = usuarioModificacion; }

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }

    public String getMotivoPrestamo() { return motivoPrestamo; }
    public void setMotivoPrestamo(String motivoPrestamo) { this.motivoPrestamo = motivoPrestamo; }

    public Long getEstadoOperacion() { return estadoOperacion; }
    public void setEstadoOperacion(Long estadoOperacion) { this.estadoOperacion = estadoOperacion; }

    public Double getTasaNominal() { return tasaNominal; }
    public void setTasaNominal(Double tasaNominal) { this.tasaNominal = tasaNominal; }

    public Double getTasaEfectiva() { return tasaEfectiva; }
    public void setTasaEfectiva(Double tasaEfectiva) { this.tasaEfectiva = tasaEfectiva; }

    public Long getEsNovacion() { return esNovacion; }
    public void setEsNovacion(Long esNovacion) { this.esNovacion = esNovacion; }

    public Long getReprocesado() { return reprocesado; }
    public void setReprocesado(Long reprocesado) { this.reprocesado = reprocesado; }

    public Long getReestructurado() { return reestructurado; }
    public void setReestructurado(Long reestructurado) { this.reestructurado = reestructurado; }

    public Long getRefinanciado() { return refinanciado; }
    public void setRefinanciado(Long refinanciado) { this.refinanciado = refinanciado; }

    public Double getSaldoCapital() { return saldoCapital; }
    public void setSaldoCapital(Double saldoCapital) { this.saldoCapital = saldoCapital; }

    public Double getSaldoOtros() { return saldoOtros; }
    public void setSaldoOtros(Double saldoOtros) { this.saldoOtros = saldoOtros; }

    public Double getSaldoInteres() { return saldoInteres; }
    public void setSaldoInteres(Double saldoInteres) { this.saldoInteres = saldoInteres; }

    public Double getMoraCalculada() { return moraCalculada; }
    public void setMoraCalculada(Double moraCalculada) { this.moraCalculada = moraCalculada; }

    public Long getDiasVencido() { return diasVencido; }
    public void setDiasVencido(Long diasVencido) { this.diasVencido = diasVencido; }

    public Double getMontoNovacion() { return montoNovacion; }
    public void setMontoNovacion(Double montoNovacion) { this.montoNovacion = montoNovacion; }

    public Double getInteresVariable() { return interesVariable; }
    public void setInteresVariable(Double interesVariable) { this.interesVariable = interesVariable; }

    public String getUsuarioAprobacion() { return usuarioAprobacion; }
    public void setUsuarioAprobacion(String usuarioAprobacion) { this.usuarioAprobacion = usuarioAprobacion; }

    public LocalDateTime getFechaAprobacion() { return fechaAprobacion; }
    public void setFechaAprobacion(LocalDateTime fechaAprobacion) { this.fechaAprobacion = fechaAprobacion; }

    public LocalDateTime getFechaAdjudicacion() { return fechaAdjudicacion; }
    public void setFechaAdjudicacion(LocalDateTime fechaAdjudicacion) { this.fechaAdjudicacion = fechaAdjudicacion; }

    public String getUsuarioRechazo() { return usuarioRechazo; }
    public void setUsuarioRechazo(String usuarioRechazo) { this.usuarioRechazo = usuarioRechazo; }

    public LocalDateTime getFechaRechazo() { return fechaRechazo; }
    public void setFechaRechazo(LocalDateTime fechaRechazo) { this.fechaRechazo = fechaRechazo; }

    public String getUsuarioLegalizacion() { return usuarioLegalizacion; }
    public void setUsuarioLegalizacion(String usuarioLegalizacion) { this.usuarioLegalizacion = usuarioLegalizacion; }

    public LocalDateTime getFechaLegalizacion() { return fechaLegalizacion; }
    public void setFechaLegalizacion(LocalDateTime fechaLegalizacion) { this.fechaLegalizacion = fechaLegalizacion; }

    public String getUsuarioAcreditacion() { return usuarioAcreditacion; }
    public void setUsuarioAcreditacion(String usuarioAcreditacion) { this.usuarioAcreditacion = usuarioAcreditacion; }

    public LocalDateTime getFechaAcreditacion() { return fechaAcreditacion; }
    public void setFechaAcreditacion(LocalDateTime fechaAcreditacion) { this.fechaAcreditacion = fechaAcreditacion; }

    public Long getAjusteAportes() { return ajusteAportes; }
    public void setAjusteAportes(Long ajusteAportes) { this.ajusteAportes = ajusteAportes; }

    public Long getMesesACobrar() { return mesesACobrar; }
    public void setMesesACobrar(Long mesesACobrar) { this.mesesACobrar = mesesACobrar; }

    public Long getEstado() { return estado; }
    public void setEstado(Long estado) { this.estado = estado; }

    public Long getFirmadoTitular() { return firmadoTitular; }
    public void setFirmadoTitular(Long firmadoTitular) { this.firmadoTitular = firmadoTitular; }

}
