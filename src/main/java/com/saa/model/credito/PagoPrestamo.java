package com.saa.model.credito;

import java.io.Serializable;
import java.util.Date;

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
    private Date fecha;

    /** Valor */
    @Basic
    @Column(name = "PGPRVLRR")
    private Long valor;

    /** Número de cuota */
    @Basic
    @Column(name = "PGPRNMCT")
    private Long numeroCuota;

    /** Capital Pagado */
    @Basic
    @Column(name = "PGPRCPPG")
    private Long capitalPagado;

    /** Interés Pagado */
    @Basic
    @Column(name = "PGPRINPG")
    private Long interesPagado;

    /** Mora pagada */
    @Basic
    @Column(name = "PGPRMRPG")
    private Long moraPagada;

    /** Interés vencido pagado */
    @Basic
    @Column(name = "PGPRINVP")
    private Long interesVencidoPagado;

    /** Desgravamen */
    @Basic
    @Column(name = "PGPRDSGR")
    private Long desgravamen;

    /** Saldo otros */
    @Basic
    @Column(name = "PGPRSLOT")
    private Long saldoOtros;

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
    private Date fechaRegistro;

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

    public Date getFecha() {
        return fecha;
    }

    public void setFecha(Date fecha) {
        this.fecha = fecha;
    }

    public Long getValor() {
        return valor;
    }

    public void setValor(Long valor) {
        this.valor = valor;
    }

    public Long getNumeroCuota() {
        return numeroCuota;
    }

    public void setNumeroCuota(Long numeroCuota) {
        this.numeroCuota = numeroCuota;
    }

    public Long getCapitalPagado() {
        return capitalPagado;
    }

    public void setCapitalPagado(Long capitalPagado) {
        this.capitalPagado = capitalPagado;
    }

    public Long getInteresPagado() {
        return interesPagado;
    }

    public void setInteresPagado(Long interesPagado) {
        this.interesPagado = interesPagado;
    }

    public Long getMoraPagada() {
        return moraPagada;
    }

    public void setMoraPagada(Long moraPagada) {
        this.moraPagada = moraPagada;
    }

    public Long getInteresVencidoPagado() {
        return interesVencidoPagado;
    }

    public void setInteresVencidoPagado(Long interesVencidoPagado) {
        this.interesVencidoPagado = interesVencidoPagado;
    }

    public Long getDesgravamen() {
        return desgravamen;
    }

    public void setDesgravamen(Long desgravamen) {
        this.desgravamen = desgravamen;
    }

    public Long getSaldoOtros() {
        return saldoOtros;
    }

    public void setSaldoOtros(Long saldoOtros) {
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

    public Date getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(Date fechaRegistro) {
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

