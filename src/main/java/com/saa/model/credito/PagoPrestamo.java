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
 * Representa la tabla PGPR (PagoPrestamo).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "PGPR", schema = "CRD")
@NamedQueries({
    @NamedQuery(name = "PagoPrestamoAll", query = "select e from PagoPrestamo e"),
    @NamedQuery(name = "PagoPrestamoId", query = "select e from PagoPrestamo e where e.codigo = :id")
})
public class PagoPrestamo implements Serializable {

    /** Código */
    @Id
    @Basic
    @Column(name = "PGPRCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;
    
    /** FK - Código Prestamo*/
    @ManyToOne
    @JoinColumn(name = "PRSTCDGO", referencedColumnName = "PRSTCDGO")
    private Prestamo prestamo;
    
    /** FK - Código Detalle Prestamo*/
    @ManyToOne
    @JoinColumn(name = "DTPRCDGO", referencedColumnName = "DTPRCDGO")
    private DetallePrestamo detallePrestamo;

    /** Fecha */
    @Basic
    @Column(name = "PGPRFCHA")
    private LocalDateTime fecha;

    /** Valor */
    @Basic
    @Column(name = "PGPRVLRR")
    private Double valor;

    /** Número de cuota */
    @Basic
    @Column(name = "PGPRNMCT")
    private Double numeroCuota;

    /** Capital Pagado */
    @Basic
    @Column(name = "PGPRCPPG")
    private Double capitalPagado;

    /** Interés Pagado */
    @Basic
    @Column(name = "PGPRINPG")
    private Double interesPagado;

    /** Mora pagada */
    @Basic
    @Column(name = "PGPRMRPG")
    private Double moraPagada;

    /** Interés vencido pagado */
    @Basic
    @Column(name = "PGPRINVP")
    private Double interesVencidoPagado;

    /** Desgravamen */
    @Basic
    @Column(name = "PGPRDSGR")
    private Double desgravamen;

    /** Saldo otros */
    @Basic
    @Column(name = "PGPRSLOT")
    private Double saldoOtros;

    /** Observación */
    @Basic
    @Column(name = "PGPROBSR", length = 2000)
    private String observacion;

    /** Tipo */
    @Basic
    @Column(name = "PGPRTPOO", length = 50)
    private String tipo;

    /** Estado */
    @Basic
    @Column(name = "PGPRESTD")
    private Long estado;

    /** Fecha registro */
    @Basic
    @Column(name = "PGPRFCRG")
    private LocalDateTime fechaRegistro;

    /** Usuario registro */
    @Basic
    @Column(name = "PGPRUSRG", length = 200)
    private String usuarioRegistro;

    /** ID Estado */
    @Basic
    @Column(name = "PGPRIDST", nullable = false)
    private Long idEstado;

    // ============================================================
    // Getters y Setters
    // ============================================================

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public Prestamo getPrestamo() {
        return prestamo;
    }

    public void setPrestamo(Prestamo prestamo) {
        this.prestamo = prestamo;
    }

    public DetallePrestamo getDetallePrestamo() {
        return detallePrestamo;
    }

    public void setDetallePrestamo(DetallePrestamo detallePrestamo) {
        this.detallePrestamo = detallePrestamo;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public Double getValor() {
        return valor;
    }

    public void setValor(Double valor) {
        this.valor = valor;
    }

    public Double getNumeroCuota() {
        return numeroCuota;
    }

    public void setNumeroCuota(Double numeroCuota) {
        this.numeroCuota = numeroCuota;
    }

    public Double getCapitalPagado() {
        return capitalPagado;
    }

    public void setCapitalPagado(Double capitalPagado) {
        this.capitalPagado = capitalPagado;
    }

    public Double getInteresPagado() {
        return interesPagado;
    }

    public void setInteresPagado(Double interesPagado) {
        this.interesPagado = interesPagado;
    }

    public Double getMoraPagada() {
        return moraPagada;
    }

    public void setMoraPagada(Double moraPagada) {
        this.moraPagada = moraPagada;
    }

    public Double getInteresVencidoPagado() {
        return interesVencidoPagado;
    }

    public void setInteresVencidoPagado(Double interesVencidoPagado) {
        this.interesVencidoPagado = interesVencidoPagado;
    }

    public Double getDesgravamen() {
        return desgravamen;
    }

    public void setDesgravamen(Double desgravamen) {
        this.desgravamen = desgravamen;
    }

    public Double getSaldoOtros() {
        return saldoOtros;
    }

    public void setSaldoOtros(Double saldoOtros) {
        this.saldoOtros = saldoOtros;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public Long getEstado() {
        return estado;
    }

    public void setEstado(Long estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public String getUsuarioRegistro() {
        return usuarioRegistro;
    }

    public void setUsuarioRegistro(String usuarioRegistro) {
        this.usuarioRegistro = usuarioRegistro;
    }

    public Long getIdEstado() {
        return idEstado;
    }

    public void setIdEstado(Long idEstado) {
        this.idEstado = idEstado;
    }
}

