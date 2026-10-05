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
 * Representa la tabla CRD.MVIC (MovimientoInteresCuota): el libro de movimientos de intereses
 * (ordinario, mora) y de clasificación de capital por cuota — qué provisionó, devengó y
 * clasificó el cierre de cartera, y lo que cada cobro reversó de eso. Una fila por movimiento,
 * NUNCA se actualiza ni se borra (salvo {@code anulado}, cuando se reversa la CORRIDA entera).
 *
 * Ver {@code docs/logica-negocio/crd/DISENO-PROVISION-INTERESES-Y-FECHA-AFECTACION.md} §3 y
 * {@code docs/logica-negocio/crd/sql/309_DDL_FECHA_AFECTACION_Y_LIBRO_INTERESES.sql} (18
 * columnas, mapeadas exactamente).
 *
 * <b>Saldo provisionado de una cuota y componente:</b> Σ tipo 1 + Σ tipo 4 − Σ tipo 2 − Σ tipo 3
 * (solo filas con {@code anulado = 0}).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "MVIC", schema = "CRD")
@SequenceGenerator(name = "SQ_MVICCDGO", sequenceName = "CRD.SQ_MVICCDGO", allocationSize = 1)
@NamedQueries({
    @NamedQuery(name = "MovimientoInteresCuotaAll", query = "select e from MovimientoInteresCuota e"),
    @NamedQuery(name = "MovimientoInteresCuotaId",  query = "select e from MovimientoInteresCuota e where e.codigo = :id")
})
public class MovimientoInteresCuota implements Serializable {

    /** Código PK. */
    @Id
    @Basic
    @Column(name = "MVICCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_MVICCDGO")
    private Long codigo;

    /** FK - préstamo. */
    @ManyToOne
    @JoinColumn(name = "PRSTCDGO", referencedColumnName = "PRSTCDGO")
    private Prestamo prestamo;

    /**
     * DTPRCDGO de la cuota, SIN relación ni FK a propósito (H82: el abono a capital y el
     * reverso de operaciones BORRAN cuotas de DTPR) — mismo criterio que {@code CuotaSeguro.idCuota}
     * y {@code DetallePlazoVencido.idCuota}.
     */
    @Basic
    @Column(name = "DTPRCDGO")
    private Long idCuota;

    /** 1-9 — {@code com.saa.rubros.TipoMovimientoInteresCuota}. */
    @Basic
    @Column(name = "MVICTPMV")
    private Long tipoMovimiento;

    /** 1 INTERÉS · 2 MORA · 3 CAPITAL — {@code com.saa.rubros.ComponenteMovimientoInteresCuota}. */
    @Basic
    @Column(name = "MVICCMPN")
    private Long componente;

    /** Valor del movimiento, SIEMPRE positivo; el signo lo da {@code tipoMovimiento}. */
    @Basic
    @Column(name = "MVICVLOR")
    private Double valor;

    /** Fecha contable del movimiento. */
    @Basic
    @Column(name = "MVICFCCT")
    private LocalDate fechaContable;

    /** FK - corrida del cierre que lo generó (tipos 1, 5 y 7 únicamente). */
    @ManyToOne
    @JoinColumn(name = "CRCTCDGO", referencedColumnName = "CRCTCDGO")
    private CorridaCierreCartera corrida;

    /** FK - pago que lo originó (tipos 2, 3, 4, 8 y 9). */
    @ManyToOne
    @JoinColumn(name = "PGPRCDGO", referencedColumnName = "PGPRCDGO")
    private PagoPrestamo pago;

    /** Origen: CIERRE, CBCR, PETRO, CRUCE, PRECANCELACION, CONDONACION. */
    @Basic
    @Column(name = "MVICORGN", length = 30)
    private String origen;

    /** Id del origen (cobro, carga Petro, evento, acuerdo). */
    @Basic
    @Column(name = "MVICIDOR")
    private Long idOrigen;

    /** Asiento contable que lo registró. Sin FK: otro esquema (CNT). */
    @Basic
    @Column(name = "ASNTCDGO")
    private Long asiento;

    /** FK - para el tipo 4 (RE_PROVISION): el movimiento tipo 2 que deshace. */
    @ManyToOne
    @JoinColumn(name = "MVICREVR", referencedColumnName = "MVICCDGO")
    private MovimientoInteresCuota movimientoReversado;

    /** Componente CAPITAL: tipo de cartera — 1 POR VENCER · 2 VENCIDO. */
    @Basic
    @Column(name = "MVICTPCR")
    private Long tipoCartera;

    /** Componente CAPITAL: banda (CRD.BNDP). Sin relación — solo el id, informativo. */
    @Basic
    @Column(name = "BNDPCDGO")
    private Long idBanda;

    /** 0 vigente · 1 anulado (se reversó la corrida o la operación). Nunca se borra. */
    @Basic
    @Column(name = "MVICANUL")
    private Long anulado;

    @Basic
    @Column(name = "MVICUSRG", length = 50)
    private String usuarioRegistro;

    @Basic
    @Column(name = "MVICFCRG")
    private LocalDateTime fechaRegistro;

    // ============================================================
    // Getters y Setters
    // ============================================================

    public Long getCodigo() { return codigo; }
    public void setCodigo(Long codigo) { this.codigo = codigo; }

    public Prestamo getPrestamo() { return prestamo; }
    public void setPrestamo(Prestamo prestamo) { this.prestamo = prestamo; }

    public Long getIdCuota() { return idCuota; }
    public void setIdCuota(Long idCuota) { this.idCuota = idCuota; }

    public Long getTipoMovimiento() { return tipoMovimiento; }
    public void setTipoMovimiento(Long tipoMovimiento) { this.tipoMovimiento = tipoMovimiento; }

    public Long getComponente() { return componente; }
    public void setComponente(Long componente) { this.componente = componente; }

    public Double getValor() { return valor; }
    public void setValor(Double valor) { this.valor = valor; }

    public LocalDate getFechaContable() { return fechaContable; }
    public void setFechaContable(LocalDate fechaContable) { this.fechaContable = fechaContable; }

    public CorridaCierreCartera getCorrida() { return corrida; }
    public void setCorrida(CorridaCierreCartera corrida) { this.corrida = corrida; }

    public PagoPrestamo getPago() { return pago; }
    public void setPago(PagoPrestamo pago) { this.pago = pago; }

    public String getOrigen() { return origen; }
    public void setOrigen(String origen) { this.origen = origen; }

    public Long getIdOrigen() { return idOrigen; }
    public void setIdOrigen(Long idOrigen) { this.idOrigen = idOrigen; }

    public Long getAsiento() { return asiento; }
    public void setAsiento(Long asiento) { this.asiento = asiento; }

    public MovimientoInteresCuota getMovimientoReversado() { return movimientoReversado; }
    public void setMovimientoReversado(MovimientoInteresCuota movimientoReversado) { this.movimientoReversado = movimientoReversado; }

    public Long getTipoCartera() { return tipoCartera; }
    public void setTipoCartera(Long tipoCartera) { this.tipoCartera = tipoCartera; }

    public Long getIdBanda() { return idBanda; }
    public void setIdBanda(Long idBanda) { this.idBanda = idBanda; }

    public Long getAnulado() { return anulado; }
    public void setAnulado(Long anulado) { this.anulado = anulado; }

    public String getUsuarioRegistro() { return usuarioRegistro; }
    public void setUsuarioRegistro(String usuarioRegistro) { this.usuarioRegistro = usuarioRegistro; }

    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }
}
