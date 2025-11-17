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
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

/**
 * Representa la tabla DTPR (DetallePrestamo).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "DTPR", schema = "CRD")
@SequenceGenerator(name = "SQ_DTPRCDGO", sequenceName = "CRD.SQ_DTPRCDGO", allocationSize = 1)
@NamedQueries({
    @NamedQuery(name = "DetallePrestamoAll", query = "select e from DetallePrestamo e"),
    @NamedQuery(name = "DetallePrestamoId", query = "select e from DetallePrestamo e where e.codigo = :id")
})
public class DetallePrestamo implements Serializable {

    /** Código del detalle */
    @Id
    @Basic
    @Column(name = "DTPRCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_DTPRCDGO")
    private Long codigo;

    /** FK - Código Filial */
    @ManyToOne
    @JoinColumn(name = "PRSTCDGO", referencedColumnName = "PRSTCDGO")
    private Prestamo prestamo;

    /** Número de cuota */
    @Basic
    @Column(name = "DTPRNMCT")
    private Long numeroCuota;

    /** Fecha vencimiento */
    @Basic
    @Column(name = "DTPRFCVN")
    private Date fechaVencimiento;

    /** Capital */
    @Basic
    @Column(name = "DTPRCPTL", precision = 10, scale = 2)
    private Long capital;

    /** Interés */
    @Basic
    @Column(name = "DTPRINTR", precision = 10, scale = 2)
    private Long interes;

    /** Mora */
    @Basic
    @Column(name = "DTPRMRAA", precision = 10, scale = 2)
    private Long mora;

    /** Interés Vencido */
    @Basic
    @Column(name = "DTPRINVN", precision = 10, scale = 2)
    private Long interesVencido;

    /** Saldo capital */
    @Basic
    @Column(name = "DTPRSLCP", precision = 10, scale = 2)
    private Long saldoCapital;

    /** Saldo Interes */
    @Basic
    @Column(name = "DTPRSLIN", precision = 10, scale = 2)
    private Long saldoInteres;

    /** Saldo mora */
    @Basic
    @Column(name = "DTPRSLMR", precision = 10, scale = 2)
    private Long saldoMora;

    /** Saldo Interes Vencido */
    @Basic
    @Column(name = "DTPRSLIV", precision = 10, scale = 2)
    private Long saldoInteresVencido;

    /** Fecha pagado */
    @Basic
    @Column(name = "DTPRFCPG")
    private Date fechaPagado;

    /** Abono */
    @Basic
    @Column(name = "DTPRABNO", precision = 10, scale = 2)
    private Long abono;

    /** Capital Pagado */
    @Basic
    @Column(name = "DTPRCPPG", precision = 10, scale = 2)
    private Long capitalPagado;

    /** Interés Pagado */
    @Basic
    @Column(name = "DTPRINPG", precision = 10, scale = 2)
    private Long interesPagado;

    /** Desgravamen */
    @Basic
    @Column(name = "DTPRDSGR", precision = 10, scale = 2)
    private Long desgravamen;

    /** Cuota */
    @Basic
    @Column(name = "DTPRCTAA", precision = 10, scale = 2)
    private Long cuota;

    /** Saldo */
    @Basic
    @Column(name = "DTPRSLDO", precision = 10, scale = 2)
    private Long saldo;

    /** Saldo Otros */
    @Basic
    @Column(name = "DTPRSLOT", precision = 10, scale = 2)
    private Long saldoOtros;

    /** Desgravamen Firmado */
    @Basic
    @Column(name = "DTPRDSFR", precision = 10, scale = 2)
    private Long desgravamenFirmado;

    /** Desgravamen Diferido */
    @Basic
    @Column(name = "DTPRDSDF", precision = 10, scale = 2)
    private Long desgravamenDiferido;

    /** Desgravamen original */
    @Basic
    @Column(name = "DTPRDSOR", precision = 10, scale = 2)
    private Long desgravamenOriginal;

    /** Valor diferido */
    @Basic
    @Column(name = "DTPRVLDF", precision = 10, scale = 2)
    private Long valorDiferido;

    /** Total */
    @Basic
    @Column(name = "DTPRTTLL", precision = 10, scale = 2)
    private Long total;

    /** Mora pagado */
    @Basic
    @Column(name = "DTPRMRPG", precision = 10, scale = 2)
    private Long moraPagado;

    /** Desgravamen Pagado */
    @Basic
    @Column(name = "DTPRDSPG", precision = 10, scale = 2)
    private Long desgravamenPagado;

    /** Interés vendido pagado */
    @Basic
    @Column(name = "DTPRINVP", precision = 10, scale = 2)
    private Long interesVendidoPagado;

    /** Mora calculada */
    @Basic
    @Column(name = "DTPRMRCL", precision = 10, scale = 2)
    private Long moraCalculada;

    /** Días mora */
    @Basic
    @Column(name = "DTPRDSMR", precision = 10, scale = 2)
    private Long diasMora;

    /** Estado */
    @Basic
    @Column(name = "DTPRESTD")
    private Long estado;

    /** Fecha registro */
    @Basic
    @Column(name = "DTPRFCRG")
    private Date fechaRegistro;

    /** Usuario registro */
    @Basic
    @Column(name = "DTPRUSRG", length = 200)
    private String usuarioRegistro;

    /** ID Estado */
    @Basic
    @Column(name = "DTPRIDST")
    private Long idEstado;

    /** Código externo */
    @Basic
    @Column(name = "DTPRCDEX")
    private Long codigoExterno;

    /** Otros seguros */
    @Basic
    @Column(name = "DTPROTSG", precision = 10, scale = 2)
    private Long otrosSeguros;

    /** Total con seguro */
    @Basic
    @Column(name = "DTPRTTCS", precision = 10, scale = 2)
    private Long totalConSeguro;

    // ============================================================
    // Getters y Setters
    // ============================================================

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public Prestamo getPrestamoId() {
        return prestamo;
    }

    public void setPrestamoId(Prestamo prestamo) {
        this.prestamo = prestamo;
    }

    public Long getNumeroCuota() {
        return numeroCuota;
    }

    public void setNumeroCuota(Long numeroCuota) {
        this.numeroCuota = numeroCuota;
    }

    public Date getFechaVencimiento() {
        return fechaVencimiento;
    }

    public void setFechaVencimiento(Date fechaVencimiento) {
        this.fechaVencimiento = fechaVencimiento;
    }

    public Long getCapital() {
        return capital;
    }

    public void setCapital(Long capital) {
        this.capital = capital;
    }

    public Long getInteres() {
        return interes;
    }

    public void setInteres(Long interes) {
        this.interes = interes;
    }

    public Long getMora() {
        return mora;
    }

    public void setMora(Long mora) {
        this.mora = mora;
    }

    public Long getInteresVencido() {
        return interesVencido;
    }

    public void setInteresVencido(Long interesVencido) {
        this.interesVencido = interesVencido;
    }

    public Long getSaldoCapital() {
        return saldoCapital;
    }

    public void setSaldoCapital(Long saldoCapital) {
        this.saldoCapital = saldoCapital;
    }

    public Long getSaldoInteres() {
        return saldoInteres;
    }

    public void setSaldoInteres(Long saldoInteres) {
        this.saldoInteres = saldoInteres;
    }

    public Long getSaldoMora() {
        return saldoMora;
    }

    public void setSaldoMora(Long saldoMora) {
        this.saldoMora = saldoMora;
    }

    public Long getSaldoInteresVencido() {
        return saldoInteresVencido;
    }

    public void setSaldoInteresVencido(Long saldoInteresVencido) {
        this.saldoInteresVencido = saldoInteresVencido;
    }

    public Date getFechaPagado() {
        return fechaPagado;
    }

    public void setFechaPagado(Date fechaPagado) {
        this.fechaPagado = fechaPagado;
    }

    public Long getAbono() {
        return abono;
    }

    public void setAbono(Long abono) {
        this.abono = abono;
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

    public Long getDesgravamen() {
        return desgravamen;
    }

    public void setDesgravamen(Long desgravamen) {
        this.desgravamen = desgravamen;
    }

    public Long getCuota() {
        return cuota;
    }

    public void setCuota(Long cuota) {
        this.cuota = cuota;
    }

    public Long getSaldo() {
        return saldo;
    }

    public void setSaldo(Long saldo) {
        this.saldo = saldo;
    }

    public Long getSaldoOtros() {
        return saldoOtros;
    }

    public void setSaldoOtros(Long saldoOtros) {
        this.saldoOtros = saldoOtros;
    }

    public Long getDesgravamenFirmado() {
        return desgravamenFirmado;
    }

    public void setDesgravamenFirmado(Long desgravamenFirmado) {
        this.desgravamenFirmado = desgravamenFirmado;
    }

    public Long getDesgravamenDiferido() {
        return desgravamenDiferido;
    }

    public void setDesgravamenDiferido(Long desgravamenDiferido) {
        this.desgravamenDiferido = desgravamenDiferido;
    }

    public Long getDesgravamenOriginal() {
        return desgravamenOriginal;
    }

    public void setDesgravamenOriginal(Long desgravamenOriginal) {
        this.desgravamenOriginal = desgravamenOriginal;
    }

    public Long getValorDiferido() {
        return valorDiferido;
    }

    public void setValorDiferido(Long valorDiferido) {
        this.valorDiferido = valorDiferido;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public Long getMoraPagado() {
        return moraPagado;
    }

    public void setMoraPagado(Long moraPagado) {
        this.moraPagado = moraPagado;
    }

    public Long getDesgravamenPagado() {
        return desgravamenPagado;
    }

    public void setDesgravamenPagado(Long desgravamenPagado) {
        this.desgravamenPagado = desgravamenPagado;
    }

    public Long getInteresVendidoPagado() {
        return interesVendidoPagado;
    }

    public void setInteresVendidoPagado(Long interesVendidoPagado) {
        this.interesVendidoPagado = interesVendidoPagado;
    }

    public Long getMoraCalculada() {
        return moraCalculada;
    }

    public void setMoraCalculada(Long moraCalculada) {
        this.moraCalculada = moraCalculada;
    }

    public Long getDiasMora() {
        return diasMora;
    }

    public void setDiasMora(Long diasMora) {
        this.diasMora = diasMora;
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

    public Long getCodigoExterno() {
        return codigoExterno;
    }

    public void setCodigoExterno(Long codigoExterno) {
        this.codigoExterno = codigoExterno;
    }

    public Long getOtrosSeguros() {
        return otrosSeguros;
    }

    public void setOtrosSeguros(Long otrosSeguros) {
        this.otrosSeguros = otrosSeguros;
    }

    public Long getTotalConSeguro() {
        return totalConSeguro;
    }

    public void setTotalConSeguro(Long totalConSeguro) {
        this.totalConSeguro = totalConSeguro;
    }
}
