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
    private Double capital;

    /** Interés */
    @Basic
    @Column(name = "DTPRINTR", precision = 10, scale = 2)
    private Double interes;

    /** Mora */
    @Basic
    @Column(name = "DTPRMRAA", precision = 10, scale = 2)
    private Double mora;

    /** Interés Vencido */
    @Basic
    @Column(name = "DTPRINVN", precision = 10, scale = 2)
    private Double interesVencido;

    /** Saldo capital */
    @Basic
    @Column(name = "DTPRSLCP", precision = 10, scale = 2)
    private Double saldoCapital;

    /** Saldo Interes */
    @Basic
    @Column(name = "DTPRSLIN", precision = 10, scale = 2)
    private Double saldoInteres;

    /** Saldo mora */
    @Basic
    @Column(name = "DTPRSLMR", precision = 10, scale = 2)
    private Double saldoMora;

    /** Saldo Interes Vencido */
    @Basic
    @Column(name = "DTPRSLIV", precision = 10, scale = 2)
    private Double saldoInteresVencido;

    /** Fecha pagado */
    @Basic
    @Column(name = "DTPRFCPG")
    private Date fechaPagado;

    /** Abono */
    @Basic
    @Column(name = "DTPRABNO", precision = 10, scale = 2)
    private Double abono;

    /** Capital Pagado */
    @Basic
    @Column(name = "DTPRCPPG", precision = 10, scale = 2)
    private Double capitalPagado;

    /** Interés Pagado */
    @Basic
    @Column(name = "DTPRINPG", precision = 10, scale = 2)
    private Double interesPagado;

    /** Desgravamen */
    @Basic
    @Column(name = "DTPRDSGR", precision = 10, scale = 2)
    private Double desgravamen;

    /** Cuota */
    @Basic
    @Column(name = "DTPRCTAA", precision = 10, scale = 2)
    private Double cuota;

    /** Saldo */
    @Basic
    @Column(name = "DTPRSLDO", precision = 10, scale = 2)
    private Double saldo;

    /** Saldo Otros */
    @Basic
    @Column(name = "DTPRSLOT", precision = 10, scale = 2)
    private Double saldoOtros;

    /** Desgravamen Firmado */
    @Basic
    @Column(name = "DTPRDSFR", precision = 10, scale = 2)
    private Double desgravamenFirmado;

    /** Desgravamen Diferido */
    @Basic
    @Column(name = "DTPRDSDF", precision = 10, scale = 2)
    private Double desgravamenDiferido;

    /** Desgravamen original */
    @Basic
    @Column(name = "DTPRDSOR", precision = 10, scale = 2)
    private Double desgravamenOriginal;

    /** Valor diferido */
    @Basic
    @Column(name = "DTPRVLDF", precision = 10, scale = 2)
    private Double valorDiferido;

    /** Total */
    @Basic
    @Column(name = "DTPRTTLL", precision = 10, scale = 2)
    private Double total;

    /** Mora pagado */
    @Basic
    @Column(name = "DTPRMRPG", precision = 10, scale = 2)
    private Double moraPagado;

    /** Desgravamen Pagado */
    @Basic
    @Column(name = "DTPRDSPG", precision = 10, scale = 2)
    private Double desgravamenPagado;

    /** Interés vendido pagado */
    @Basic
    @Column(name = "DTPRINVP", precision = 10, scale = 2)
    private Double interesVendidoPagado;

    /** Mora calculada */
    @Basic
    @Column(name = "DTPRMRCL", precision = 10, scale = 2)
    private Double moraCalculada;

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
    private Double otrosSeguros;

    /** Total con seguro */
    @Basic
    @Column(name = "DTPRTTCS", precision = 10, scale = 2)
    private Double totalConSeguro;

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

    public Double getCapital() {
        return capital;
    }

    public void setCapital(Double capital) {
        this.capital = capital;
    }

    public Double getInteres() {
        return interes;
    }

    public void setInteres(Double interes) {
        this.interes = interes;
    }

    public Double getMora() {
        return mora;
    }

    public void setMora(Double mora) {
        this.mora = mora;
    }

    public Double getInteresVencido() {
        return interesVencido;
    }

    public void setInteresVencido(Double interesVencido) {
        this.interesVencido = interesVencido;
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

    public Double getSaldoMora() {
        return saldoMora;
    }

    public void setSaldoMora(Double saldoMora) {
        this.saldoMora = saldoMora;
    }

    public Double getSaldoInteresVencido() {
        return saldoInteresVencido;
    }

    public void setSaldoInteresVencido(Double saldoInteresVencido) {
        this.saldoInteresVencido = saldoInteresVencido;
    }

    public Date getFechaPagado() {
        return fechaPagado;
    }

    public void setFechaPagado(Date fechaPagado) {
        this.fechaPagado = fechaPagado;
    }

    public Double getAbono() {
        return abono;
    }

    public void setAbono(Double abono) {
        this.abono = abono;
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

    public Double getDesgravamen() {
        return desgravamen;
    }

    public void setDesgravamen(Double desgravamen) {
        this.desgravamen = desgravamen;
    }

    public Double getCuota() {
        return cuota;
    }

    public void setCuota(Double cuota) {
        this.cuota = cuota;
    }

    public Double getSaldo() {
        return saldo;
    }

    public void setSaldo(Double saldo) {
        this.saldo = saldo;
    }

    public Double getSaldoOtros() {
        return saldoOtros;
    }

    public void setSaldoOtros(Double saldoOtros) {
        this.saldoOtros = saldoOtros;
    }

    public Double getDesgravamenFirmado() {
        return desgravamenFirmado;
    }

    public void setDesgravamenFirmado(Double desgravamenFirmado) {
        this.desgravamenFirmado = desgravamenFirmado;
    }

    public Double getDesgravamenDiferido() {
        return desgravamenDiferido;
    }

    public void setDesgravamenDiferido(Double desgravamenDiferido) {
        this.desgravamenDiferido = desgravamenDiferido;
    }

    public Double getDesgravamenOriginal() {
        return desgravamenOriginal;
    }

    public void setDesgravamenOriginal(Double desgravamenOriginal) {
        this.desgravamenOriginal = desgravamenOriginal;
    }

    public Double getValorDiferido() {
        return valorDiferido;
    }

    public void setValorDiferido(Double valorDiferido) {
        this.valorDiferido = valorDiferido;
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    public Double getMoraPagado() {
        return moraPagado;
    }

    public void setMoraPagado(Double moraPagado) {
        this.moraPagado = moraPagado;
    }

    public Double getDesgravamenPagado() {
        return desgravamenPagado;
    }

    public void setDesgravamenPagado(Double desgravamenPagado) {
        this.desgravamenPagado = desgravamenPagado;
    }

    public Double getInteresVendidoPagado() {
        return interesVendidoPagado;
    }

    public void setInteresVendidoPagado(Double interesVendidoPagado) {
        this.interesVendidoPagado = interesVendidoPagado;
    }

    public Double getMoraCalculada() {
        return moraCalculada;
    }

    public void setMoraCalculada(Double moraCalculada) {
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

    public Double getOtrosSeguros() {
        return otrosSeguros;
    }

    public void setOtrosSeguros(Double otrosSeguros) {
        this.otrosSeguros = otrosSeguros;
    }

    public Double getTotalConSeguro() {
        return totalConSeguro;
    }

    public void setTotalConSeguro(Double totalConSeguro) {
        this.totalConSeguro = totalConSeguro;
    }
}
