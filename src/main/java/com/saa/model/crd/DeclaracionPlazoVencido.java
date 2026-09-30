package com.saa.model.crd;

import java.io.Serializable;
import java.time.LocalDate;
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
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

/**
 * Representa la tabla CRD.PLVN (DeclaracionPlazoVencido).
 *
 * Una fila por préstamo declarado en plazo vencido: guarda la FOTO de los valores del
 * memorando (Crédito) a la fecha de corte, y después la foto de la liquidación (Contabilidad).
 * Los documentos se imprimen SIEMPRE desde esta foto, nunca leyendo las cuotas en vivo — la
 * mora sigue corriendo después de declarar (D6), así que reimprimir en otra fecha daría otro
 * total con la misma fecha impresa si no se congelara acá.
 *
 * Ver {@code docs/logica-negocio/crd/API-PASE-A-PLAZO-VENCIDO.md} y
 * {@code docs/logica-negocio/crd/sql/247_DDL_DECLARACION_PLAZO_VENCIDO.sql} (55 columnas,
 * mapeadas exactamente).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "PLVN", schema = "CRD")
@SequenceGenerator(name = "SQ_PLVNCDGO", sequenceName = "CRD.SQ_PLVNCDGO", allocationSize = 1)
@NamedQueries({
    @NamedQuery(name = "DeclaracionPlazoVencidoAll", query = "select e from DeclaracionPlazoVencido e"),
    @NamedQuery(name = "DeclaracionPlazoVencidoId",  query = "select e from DeclaracionPlazoVencido e where e.codigo = :id")
})
public class DeclaracionPlazoVencido implements Serializable {

    /** 1 - Estado inicial, recién declarada por Crédito. */
    public static final long ESTADO_DECLARADA = 1L;
    /** 2 - Contabilidad ya emitió la liquidación. */
    public static final long ESTADO_LIQUIDADA = 2L;
    /** 3 - Revertida: el préstamo volvió a EN_MORA. Nunca se borra. */
    public static final long ESTADO_REVERTIDA = 3L;

    /** Código PK. */
    @Id
    @Basic
    @Column(name = "PLVNCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_PLVNCDGO")
    private Long codigo;

    /** FK - Préstamo declarado. */
    @ManyToOne
    @JoinColumn(name = "PRSTCDGO", referencedColumnName = "PRSTCDGO")
    private Prestamo prestamo;

    /** 1 DECLARADA · 2 LIQUIDADA · 3 REVERTIDA. Constantes planas de esta clase, sin rubro. */
    @Basic
    @Column(name = "PLVNESTD")
    private Long estado;

    /** PRSTIDST del préstamo ANTES de declarar. Solo registro: el reverso vuelve siempre a 11. */
    @Basic
    @Column(name = "PLVNESAN")
    private Long estadoAnterior;

    // ============================================================
    // Memorando (paso 1, Crédito)
    // ============================================================

    /** Número de memorando ingresado por el usuario (D9). Único en toda la tabla (D16). */
    @Basic
    @Column(name = "PLVNNMMM", length = 60)
    private String numeroMemorando;

    /** Fecha de corte del memorando = fecha de corte del cálculo del paso 1 (D4). */
    @Basic
    @Column(name = "PLVNFCCR")
    private LocalDate fechaCorte;

    /** PARA: nombre (D21). */
    @Basic
    @Column(name = "PLVNPRNM", length = 200)
    private String paraNombre;

    /** PARA: cargo. */
    @Basic
    @Column(name = "PLVNPRCR", length = 200)
    private String paraCargo;

    /** CC: nombre. */
    @Basic
    @Column(name = "PLVNCCNM", length = 200)
    private String ccNombre;

    /** CC: cargo. */
    @Basic
    @Column(name = "PLVNCCCR", length = 200)
    private String ccCargo;

    /**
     * Número de préstamo TAL COMO SE IMPRIMIÓ: {@code Prestamo.idAsoprep} (PRSTIDAS, número de
     * operación ASOPREP) y, si es nulo, {@code Prestamo.codigo} (PRSTCDGO). Misma convención que
     * todas las pantallas del frontend ({@code idAsoprep ?? codigo}). Se congela para que la
     * reimpresión no cambie si después se carga el idAsoprep del préstamo.
     */
    @Basic
    @Column(name = "PLVNNMPS", length = 30)
    private String numeroPrestamoImpreso;

    // ============================================================
    // Foto de identificación (no se lee en vivo: el nombre puede corregirse después)
    // ============================================================

    @Basic
    @Column(name = "PLVNNMPR", length = 300)
    private String nombreParticipe;

    @Basic
    @Column(name = "PLVNIDPR", length = 20)
    private String cedula;

    /** Tipo de crédito (nombre del producto). */
    @Basic
    @Column(name = "PLVNTPCR", length = 200)
    private String tipoCredito;

    @Basic
    @Column(name = "PLVNFCIN")
    private LocalDate fechaInicial;

    @Basic
    @Column(name = "PLVNFCFN")
    private LocalDate fechaFinal;

    /** Última fecha de cobro. Nula si el préstamo nunca recibió un pago. */
    @Basic
    @Column(name = "PLVNFCUC")
    private LocalDate fechaUltimoCobro;

    /** Inicio de la mora: vencimiento más antiguo entre las cuotas vencidas e impagas. */
    @Basic
    @Column(name = "PLVNFCIM")
    private LocalDate fechaInicioMora;

    // ============================================================
    // Foto del cuadro del memorando, a PLVNFCCR (devengado / cobrado / saldo)
    // ============================================================

    @Basic
    @Column(name = "PLVNMNTO")
    private Double monto;

    @Basic
    @Column(name = "PLVNCPCB")
    private Double capitalCobrado;

    /** Saldo capital vencido (acelerado, D11). */
    @Basic
    @Column(name = "PLVNSLCP")
    private Double saldoCapital;

    /** Interés devengado — TODAS las cuotas, vencidas o no (D11). */
    @Basic
    @Column(name = "PLVNINDV")
    private Double interesDevengado;

    @Basic
    @Column(name = "PLVNINCB")
    private Double interesCobrado;

    @Basic
    @Column(name = "PLVNSLIN")
    private Double saldoInteres;

    /** Desgravamen devengado — solo cuotas con vencimiento &le; corte (D22). */
    @Basic
    @Column(name = "PLVNDSDV")
    private Double desgravamenDevengado;

    @Basic
    @Column(name = "PLVNDSCB")
    private Double desgravamenCobrado;

    @Basic
    @Column(name = "PLVNSLDS")
    private Double saldoDesgravamen;

    /** Seguro de incendio devengado — mismo criterio que desgravamen (D22). */
    @Basic
    @Column(name = "PLVNSGDV")
    private Double seguroDevengado;

    @Basic
    @Column(name = "PLVNSGCB")
    private Double seguroCobrado;

    @Basic
    @Column(name = "PLVNSLSG")
    private Double saldoSeguro;

    /**
     * Mora devengada AL CORTE, con la fórmula pura de
     * {@code ProcesoMoraPrestamoService.calcularMoraCuota}. NUNCA el campo {@code mora}
     * persistido en {@code DTPR} (contrato §2).
     */
    @Basic
    @Column(name = "PLVNMRDV")
    private Double moraDevengada;

    @Basic
    @Column(name = "PLVNMRCB")
    private Double moraCobrada;

    @Basic
    @Column(name = "PLVNSLMR")
    private Double saldoMora;

    @Basic
    @Column(name = "PLVNTTCB")
    private Double totalCobrado;

    @Basic
    @Column(name = "PLVNTTPC")
    private Double totalPorCobrar;

    @Basic
    @Column(name = "PLVNDVMN")
    private Double dividendoMensual;

    @Basic
    @Column(name = "PLVNCTPL")
    private Long cuotasPlazo;

    @Basic
    @Column(name = "PLVNCTCB")
    private Long cuotasCobradas;

    @Basic
    @Column(name = "PLVNCTPN")
    private Long cuotasPendientes;

    /** Cuotas pendientes con vencimiento posterior al corte. */
    @Basic
    @Column(name = "PLVNCTXV")
    private Long cuotasPorVencer;

    // ============================================================
    // Auditoría del paso 1
    // ============================================================

    @Basic
    @Column(name = "PLVNUSDC", length = 50)
    private String usuarioDeclaracion;

    @Basic
    @Column(name = "PLVNFCDC")
    private LocalDateTime fechaDeclaracion;

    // ============================================================
    // Liquidación (paso 2, Contabilidad) — su propia fecha de corte y su propia foto
    // ============================================================

    @Basic
    @Column(name = "PLVNFCLQ")
    private LocalDate fechaCorteLiquidacion;

    @Basic
    @Column(name = "PLVNLQCP")
    private Double liquidacionSaldoCapital;

    @Basic
    @Column(name = "PLVNLQIN")
    private Double liquidacionInteres;

    @Basic
    @Column(name = "PLVNLQDS")
    private Double liquidacionDesgravamen;

    @Basic
    @Column(name = "PLVNLQSG")
    private Double liquidacionSeguro;

    @Basic
    @Column(name = "PLVNLQMR")
    private Double liquidacionMora;

    @Basic
    @Column(name = "PLVNLQTT")
    private Double liquidacionTotal;

    /** Cuotas impagas al corte de la liquidación (el documento las dice en número y letras). */
    @Basic
    @Column(name = "PLVNLQCI")
    private Long liquidacionCuotasImpagas;

    @Basic
    @Column(name = "PLVNUSLQ", length = 50)
    private String usuarioLiquidacion;

    @Basic
    @Column(name = "PLVNFRLQ")
    private LocalDateTime fechaLiquidacion;

    // ============================================================
    // Reverso (D14: siempre vuelve a EN_MORA)
    // ============================================================

    @Basic
    @Column(name = "PLVNUSRV", length = 50)
    private String usuarioReverso;

    @Basic
    @Column(name = "PLVNFCRV")
    private LocalDateTime fechaReverso;

    /** Motivo del reverso. Obligatorio; lo valida el backend antes de grabar. */
    @Basic
    @Column(name = "PLVNMTRV", length = 500)
    private String motivoReverso;

    // ============================================================
    // Getters y Setters
    // ============================================================

    public Long getCodigo() { return codigo; }
    public void setCodigo(Long codigo) { this.codigo = codigo; }

    public Prestamo getPrestamo() { return prestamo; }
    public void setPrestamo(Prestamo prestamo) { this.prestamo = prestamo; }

    public Long getEstado() { return estado; }
    public void setEstado(Long estado) { this.estado = estado; }

    public Long getEstadoAnterior() { return estadoAnterior; }
    public void setEstadoAnterior(Long estadoAnterior) { this.estadoAnterior = estadoAnterior; }

    public String getNumeroMemorando() { return numeroMemorando; }
    public void setNumeroMemorando(String numeroMemorando) { this.numeroMemorando = numeroMemorando; }

    public LocalDate getFechaCorte() { return fechaCorte; }
    public void setFechaCorte(LocalDate fechaCorte) { this.fechaCorte = fechaCorte; }

    public String getParaNombre() { return paraNombre; }
    public void setParaNombre(String paraNombre) { this.paraNombre = paraNombre; }

    public String getParaCargo() { return paraCargo; }
    public void setParaCargo(String paraCargo) { this.paraCargo = paraCargo; }

    public String getCcNombre() { return ccNombre; }
    public void setCcNombre(String ccNombre) { this.ccNombre = ccNombre; }

    public String getCcCargo() { return ccCargo; }
    public void setCcCargo(String ccCargo) { this.ccCargo = ccCargo; }

    public String getNumeroPrestamoImpreso() { return numeroPrestamoImpreso; }
    public void setNumeroPrestamoImpreso(String numeroPrestamoImpreso) { this.numeroPrestamoImpreso = numeroPrestamoImpreso; }

    public String getNombreParticipe() { return nombreParticipe; }
    public void setNombreParticipe(String nombreParticipe) { this.nombreParticipe = nombreParticipe; }

    public String getCedula() { return cedula; }
    public void setCedula(String cedula) { this.cedula = cedula; }

    public String getTipoCredito() { return tipoCredito; }
    public void setTipoCredito(String tipoCredito) { this.tipoCredito = tipoCredito; }

    public LocalDate getFechaInicial() { return fechaInicial; }
    public void setFechaInicial(LocalDate fechaInicial) { this.fechaInicial = fechaInicial; }

    public LocalDate getFechaFinal() { return fechaFinal; }
    public void setFechaFinal(LocalDate fechaFinal) { this.fechaFinal = fechaFinal; }

    public LocalDate getFechaUltimoCobro() { return fechaUltimoCobro; }
    public void setFechaUltimoCobro(LocalDate fechaUltimoCobro) { this.fechaUltimoCobro = fechaUltimoCobro; }

    public LocalDate getFechaInicioMora() { return fechaInicioMora; }
    public void setFechaInicioMora(LocalDate fechaInicioMora) { this.fechaInicioMora = fechaInicioMora; }

    public Double getMonto() { return monto; }
    public void setMonto(Double monto) { this.monto = monto; }

    public Double getCapitalCobrado() { return capitalCobrado; }
    public void setCapitalCobrado(Double capitalCobrado) { this.capitalCobrado = capitalCobrado; }

    public Double getSaldoCapital() { return saldoCapital; }
    public void setSaldoCapital(Double saldoCapital) { this.saldoCapital = saldoCapital; }

    public Double getInteresDevengado() { return interesDevengado; }
    public void setInteresDevengado(Double interesDevengado) { this.interesDevengado = interesDevengado; }

    public Double getInteresCobrado() { return interesCobrado; }
    public void setInteresCobrado(Double interesCobrado) { this.interesCobrado = interesCobrado; }

    public Double getSaldoInteres() { return saldoInteres; }
    public void setSaldoInteres(Double saldoInteres) { this.saldoInteres = saldoInteres; }

    public Double getDesgravamenDevengado() { return desgravamenDevengado; }
    public void setDesgravamenDevengado(Double desgravamenDevengado) { this.desgravamenDevengado = desgravamenDevengado; }

    public Double getDesgravamenCobrado() { return desgravamenCobrado; }
    public void setDesgravamenCobrado(Double desgravamenCobrado) { this.desgravamenCobrado = desgravamenCobrado; }

    public Double getSaldoDesgravamen() { return saldoDesgravamen; }
    public void setSaldoDesgravamen(Double saldoDesgravamen) { this.saldoDesgravamen = saldoDesgravamen; }

    public Double getSeguroDevengado() { return seguroDevengado; }
    public void setSeguroDevengado(Double seguroDevengado) { this.seguroDevengado = seguroDevengado; }

    public Double getSeguroCobrado() { return seguroCobrado; }
    public void setSeguroCobrado(Double seguroCobrado) { this.seguroCobrado = seguroCobrado; }

    public Double getSaldoSeguro() { return saldoSeguro; }
    public void setSaldoSeguro(Double saldoSeguro) { this.saldoSeguro = saldoSeguro; }

    public Double getMoraDevengada() { return moraDevengada; }
    public void setMoraDevengada(Double moraDevengada) { this.moraDevengada = moraDevengada; }

    public Double getMoraCobrada() { return moraCobrada; }
    public void setMoraCobrada(Double moraCobrada) { this.moraCobrada = moraCobrada; }

    public Double getSaldoMora() { return saldoMora; }
    public void setSaldoMora(Double saldoMora) { this.saldoMora = saldoMora; }

    public Double getTotalCobrado() { return totalCobrado; }
    public void setTotalCobrado(Double totalCobrado) { this.totalCobrado = totalCobrado; }

    public Double getTotalPorCobrar() { return totalPorCobrar; }
    public void setTotalPorCobrar(Double totalPorCobrar) { this.totalPorCobrar = totalPorCobrar; }

    public Double getDividendoMensual() { return dividendoMensual; }
    public void setDividendoMensual(Double dividendoMensual) { this.dividendoMensual = dividendoMensual; }

    public Long getCuotasPlazo() { return cuotasPlazo; }
    public void setCuotasPlazo(Long cuotasPlazo) { this.cuotasPlazo = cuotasPlazo; }

    public Long getCuotasCobradas() { return cuotasCobradas; }
    public void setCuotasCobradas(Long cuotasCobradas) { this.cuotasCobradas = cuotasCobradas; }

    public Long getCuotasPendientes() { return cuotasPendientes; }
    public void setCuotasPendientes(Long cuotasPendientes) { this.cuotasPendientes = cuotasPendientes; }

    public Long getCuotasPorVencer() { return cuotasPorVencer; }
    public void setCuotasPorVencer(Long cuotasPorVencer) { this.cuotasPorVencer = cuotasPorVencer; }

    public String getUsuarioDeclaracion() { return usuarioDeclaracion; }
    public void setUsuarioDeclaracion(String usuarioDeclaracion) { this.usuarioDeclaracion = usuarioDeclaracion; }

    public LocalDateTime getFechaDeclaracion() { return fechaDeclaracion; }
    public void setFechaDeclaracion(LocalDateTime fechaDeclaracion) { this.fechaDeclaracion = fechaDeclaracion; }

    public LocalDate getFechaCorteLiquidacion() { return fechaCorteLiquidacion; }
    public void setFechaCorteLiquidacion(LocalDate fechaCorteLiquidacion) { this.fechaCorteLiquidacion = fechaCorteLiquidacion; }

    public Double getLiquidacionSaldoCapital() { return liquidacionSaldoCapital; }
    public void setLiquidacionSaldoCapital(Double liquidacionSaldoCapital) { this.liquidacionSaldoCapital = liquidacionSaldoCapital; }

    public Double getLiquidacionInteres() { return liquidacionInteres; }
    public void setLiquidacionInteres(Double liquidacionInteres) { this.liquidacionInteres = liquidacionInteres; }

    public Double getLiquidacionDesgravamen() { return liquidacionDesgravamen; }
    public void setLiquidacionDesgravamen(Double liquidacionDesgravamen) { this.liquidacionDesgravamen = liquidacionDesgravamen; }

    public Double getLiquidacionSeguro() { return liquidacionSeguro; }
    public void setLiquidacionSeguro(Double liquidacionSeguro) { this.liquidacionSeguro = liquidacionSeguro; }

    public Double getLiquidacionMora() { return liquidacionMora; }
    public void setLiquidacionMora(Double liquidacionMora) { this.liquidacionMora = liquidacionMora; }

    public Double getLiquidacionTotal() { return liquidacionTotal; }
    public void setLiquidacionTotal(Double liquidacionTotal) { this.liquidacionTotal = liquidacionTotal; }

    public Long getLiquidacionCuotasImpagas() { return liquidacionCuotasImpagas; }
    public void setLiquidacionCuotasImpagas(Long liquidacionCuotasImpagas) { this.liquidacionCuotasImpagas = liquidacionCuotasImpagas; }

    public String getUsuarioLiquidacion() { return usuarioLiquidacion; }
    public void setUsuarioLiquidacion(String usuarioLiquidacion) { this.usuarioLiquidacion = usuarioLiquidacion; }

    public LocalDateTime getFechaLiquidacion() { return fechaLiquidacion; }
    public void setFechaLiquidacion(LocalDateTime fechaLiquidacion) { this.fechaLiquidacion = fechaLiquidacion; }

    public String getUsuarioReverso() { return usuarioReverso; }
    public void setUsuarioReverso(String usuarioReverso) { this.usuarioReverso = usuarioReverso; }

    public LocalDateTime getFechaReverso() { return fechaReverso; }
    public void setFechaReverso(LocalDateTime fechaReverso) { this.fechaReverso = fechaReverso; }

    public String getMotivoReverso() { return motivoReverso; }
    public void setMotivoReverso(String motivoReverso) { this.motivoReverso = motivoReverso; }
}
