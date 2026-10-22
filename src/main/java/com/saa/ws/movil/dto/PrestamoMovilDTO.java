package com.saa.ws.movil.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Préstamo recortado para {@code /movil/prestamos/...}. A propósito NO serializa la entidad
 * {@code Prestamo} (§4.2 del contrato): esa entidad arrastra {@code Entidad} completa (datos
 * personales del partícipe) dentro de cada préstamo por el {@code @ManyToOne}.
 *
 * <p>⚠️ {@link #idEstado} es el estado vigente (columna {@code PRSTIDST}) — NO
 * {@code estadoPrestamo} ({@code ESPSCDGO}, la FK al catálogo). Ver la trampa documentada en
 * {@code CLAUDE.md} sobre {@code CRD.PRST}.</p>
 *
 * <p>Fechas como texto (ISO local, sin zona), no como arreglo Jackson — regla §4.3 del
 * contrato.</p>
 */
public class PrestamoMovilDTO {

    private Long codigo;
    private Long idAsoprep;
    private Long idProducto;
    private String nombreProducto;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime fecha;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime fechaInicio;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime fechaFin;

    private Long plazo;
    private Double montoSolicitado;
    private Double valorCuota;
    private Double tasa;
    private Double totalPagado;
    private Double saldoCapital;
    private Double saldoInteres;
    private Double saldoPorVencer;
    private Double saldoVencido;
    private Double saldoTotal;
    private Double moraCalculada;
    private Long diasVencido;
    private Long idEstado;

    /** TPPRCDGO del tipo del producto del prestamo; null si falta. */
    private Long idTipoPrestamo;

    /** Nombre del tipo de prestamo. */
    private String nombreTipoPrestamo;

    /** true si idTipoPrestamo == 1 (QUIROGRAFARIO): la garantia de los aportes solo cubre esos productos. */
    private Boolean cubiertoPorAportes;

    /** Vencimiento minimo de las cuotas validas (estado != ANULADA). null si no hay cuotas. */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime fechaPrimeraCuota;

    /** Vencimiento maximo de las cuotas validas (estado != ANULADA). */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime fechaUltimaCuota;

    /** Misma regla que participe-dash del SAA (obtenerSaldoDesdeUltimaPagada). Ver CONTRATO-INTRANET-MOVIL.md. */
    private Double saldoCapitalPendiente;

    /** montoSolicitado - saldoCapitalPendiente, a 2 decimales; null si alguno es null. */
    private Double capitalPagado;

    /** Total de la cuota PAGADA/CANCELADA_ANTICIPADA de mayor numeroCuota; null si no hay. */
    private Double valorUltimaCuotaPagada;

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public Long getIdAsoprep() {
        return idAsoprep;
    }

    public void setIdAsoprep(Long idAsoprep) {
        this.idAsoprep = idAsoprep;
    }

    public Long getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(Long idProducto) {
        this.idProducto = idProducto;
    }

    public String getNombreProducto() {
        return nombreProducto;
    }

    public void setNombreProducto(String nombreProducto) {
        this.nombreProducto = nombreProducto;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public LocalDateTime getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDateTime fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDateTime getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDateTime fechaFin) {
        this.fechaFin = fechaFin;
    }

    public Long getPlazo() {
        return plazo;
    }

    public void setPlazo(Long plazo) {
        this.plazo = plazo;
    }

    public Double getMontoSolicitado() {
        return montoSolicitado;
    }

    public void setMontoSolicitado(Double montoSolicitado) {
        this.montoSolicitado = montoSolicitado;
    }

    public Double getValorCuota() {
        return valorCuota;
    }

    public void setValorCuota(Double valorCuota) {
        this.valorCuota = valorCuota;
    }

    public Double getTasa() {
        return tasa;
    }

    public void setTasa(Double tasa) {
        this.tasa = tasa;
    }

    public Double getTotalPagado() {
        return totalPagado;
    }

    public void setTotalPagado(Double totalPagado) {
        this.totalPagado = totalPagado;
    }

    public Double getSaldoCapital() {
        return saldoCapital;
    }

    public void setSaldoCapital(Double saldoCapital) {
        this.saldoCapital = saldoCapital;
    }

    public Double getSaldoInteres() {
        return saldoInteres;
    }

    public void setSaldoInteres(Double saldoInteres) {
        this.saldoInteres = saldoInteres;
    }

    public Double getSaldoPorVencer() {
        return saldoPorVencer;
    }

    public void setSaldoPorVencer(Double saldoPorVencer) {
        this.saldoPorVencer = saldoPorVencer;
    }

    public Double getSaldoVencido() {
        return saldoVencido;
    }

    public void setSaldoVencido(Double saldoVencido) {
        this.saldoVencido = saldoVencido;
    }

    public Double getSaldoTotal() {
        return saldoTotal;
    }

    public void setSaldoTotal(Double saldoTotal) {
        this.saldoTotal = saldoTotal;
    }

    public Double getMoraCalculada() {
        return moraCalculada;
    }

    public void setMoraCalculada(Double moraCalculada) {
        this.moraCalculada = moraCalculada;
    }

    public Long getDiasVencido() {
        return diasVencido;
    }

    public void setDiasVencido(Long diasVencido) {
        this.diasVencido = diasVencido;
    }

    public Long getIdEstado() {
        return idEstado;
    }

    public void setIdEstado(Long idEstado) {
        this.idEstado = idEstado;
    }

    public Long getIdTipoPrestamo() {
        return idTipoPrestamo;
    }

    public void setIdTipoPrestamo(Long idTipoPrestamo) {
        this.idTipoPrestamo = idTipoPrestamo;
    }

    public String getNombreTipoPrestamo() {
        return nombreTipoPrestamo;
    }

    public void setNombreTipoPrestamo(String nombreTipoPrestamo) {
        this.nombreTipoPrestamo = nombreTipoPrestamo;
    }

    public Boolean getCubiertoPorAportes() {
        return cubiertoPorAportes;
    }

    public void setCubiertoPorAportes(Boolean cubiertoPorAportes) {
        this.cubiertoPorAportes = cubiertoPorAportes;
    }

    public LocalDateTime getFechaPrimeraCuota() {
        return fechaPrimeraCuota;
    }

    public void setFechaPrimeraCuota(LocalDateTime fechaPrimeraCuota) {
        this.fechaPrimeraCuota = fechaPrimeraCuota;
    }

    public LocalDateTime getFechaUltimaCuota() {
        return fechaUltimaCuota;
    }

    public void setFechaUltimaCuota(LocalDateTime fechaUltimaCuota) {
        this.fechaUltimaCuota = fechaUltimaCuota;
    }

    public Double getSaldoCapitalPendiente() {
        return saldoCapitalPendiente;
    }

    public void setSaldoCapitalPendiente(Double saldoCapitalPendiente) {
        this.saldoCapitalPendiente = saldoCapitalPendiente;
    }

    public Double getCapitalPagado() {
        return capitalPagado;
    }

    public void setCapitalPagado(Double capitalPagado) {
        this.capitalPagado = capitalPagado;
    }

    public Double getValorUltimaCuotaPagada() {
        return valorUltimaCuotaPagada;
    }

    public void setValorUltimaCuotaPagada(Double valorUltimaCuotaPagada) {
        this.valorUltimaCuotaPagada = valorUltimaCuotaPagada;
    }
}
