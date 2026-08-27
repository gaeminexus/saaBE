package com.saa.model.tsr;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.saa.model.cnt.Asiento;

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
 * Entity CierreCajaChica.
 * Cierre / arqueo de caja chica: saldo libros vs saldo físico. Tabla: TSR.CRCH.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "CRCH", schema = "TSR")
@NamedQueries({
    @NamedQuery(name = "CierreCajaChicaAll", query = "select e from CierreCajaChica e"),
    @NamedQuery(name = "CierreCajaChicaId", query = "select e from CierreCajaChica e where e.codigo = :id")
})
public class CierreCajaChica implements Serializable {

    @Id
    @Basic
    @Column(name = "CRCHCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /** Caja chica que se cierra. FK a TSR.CJCH. */
    @ManyToOne
    @JoinColumn(name = "CJCHCDGO", referencedColumnName = "CJCHCDGO")
    private CajaChica cajaChica;

    /** Fecha del cierre. */
    @Basic
    @Column(name = "CRCHFCHA")
    private LocalDate fecha;

    /** Inicio del periodo cerrado. */
    @Basic
    @Column(name = "CRCHFCIN")
    private LocalDate fechaInicio;

    /** Fin del periodo cerrado. */
    @Basic
    @Column(name = "CRCHFCFN")
    private LocalDate fechaFin;

    /** Saldo inicial del periodo. */
    @Basic
    @Column(name = "CRCHSLIN")
    private Double saldoInicial;

    /** Total gastos del periodo. */
    @Basic
    @Column(name = "CRCHTGST")
    private Double totalGastos;

    /** Total reposiciones y aperturas del periodo. */
    @Basic
    @Column(name = "CRCHTRPS")
    private Double totalReposiciones;

    /** Total ajustes del periodo (positivos menos negativos). */
    @Basic
    @Column(name = "CRCHTAJS")
    private Double totalAjustes;

    /** Saldo según libros al cierre. */
    @Basic
    @Column(name = "CRCHSLDO")
    private Double saldoLibros;

    /** Saldo físico contado. */
    @Basic
    @Column(name = "CRCHSLFS")
    private Double saldoFisico;

    /** Diferencia = saldo físico - saldo libros. */
    @Basic
    @Column(name = "CRCHDFRN")
    private Double diferencia;

    /** Observaciones del arqueo. */
    @Basic
    @Column(name = "CRCHOBSR", length = 2000)
    private String observacion;

    /** Estado (rubro 233): 1=Borrador, 2=Cerrado, 3=Anulado. */
    @Basic
    @Column(name = "CRCHESTD")
    private Long estado;

    /** Asiento de ajuste por diferencia (CNT.ASNT), opcional. */
    @ManyToOne
    @JoinColumn(name = "ASNTCDGO", referencedColumnName = "ASNTCDGO")
    private Asiento asiento;

    /** Fecha de registro. */
    @Basic
    @Column(name = "CRCHFCRG")
    private LocalDateTime fechaRegistro;

    /** Usuario que registra. */
    @Basic
    @Column(name = "CRCHUSAR")
    private Long usuario;

    // ── Getters y Setters ────────────────────────────────────────────────────

    public Long getCodigo() { return codigo; }
    public void setCodigo(Long codigo) { this.codigo = codigo; }

    public CajaChica getCajaChica() { return cajaChica; }
    public void setCajaChica(CajaChica cajaChica) { this.cajaChica = cajaChica; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }

    public LocalDate getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }

    public Double getSaldoInicial() { return saldoInicial; }
    public void setSaldoInicial(Double saldoInicial) { this.saldoInicial = saldoInicial; }

    public Double getTotalGastos() { return totalGastos; }
    public void setTotalGastos(Double totalGastos) { this.totalGastos = totalGastos; }

    public Double getTotalReposiciones() { return totalReposiciones; }
    public void setTotalReposiciones(Double totalReposiciones) { this.totalReposiciones = totalReposiciones; }

    public Double getTotalAjustes() { return totalAjustes; }
    public void setTotalAjustes(Double totalAjustes) { this.totalAjustes = totalAjustes; }

    public Double getSaldoLibros() { return saldoLibros; }
    public void setSaldoLibros(Double saldoLibros) { this.saldoLibros = saldoLibros; }

    public Double getSaldoFisico() { return saldoFisico; }
    public void setSaldoFisico(Double saldoFisico) { this.saldoFisico = saldoFisico; }

    public Double getDiferencia() { return diferencia; }
    public void setDiferencia(Double diferencia) { this.diferencia = diferencia; }

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }

    public Long getEstado() { return estado; }
    public void setEstado(Long estado) { this.estado = estado; }

    public Asiento getAsiento() { return asiento; }
    public void setAsiento(Asiento asiento) { this.asiento = asiento; }

    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }

    public Long getUsuario() { return usuario; }
    public void setUsuario(Long usuario) { this.usuario = usuario; }
}
