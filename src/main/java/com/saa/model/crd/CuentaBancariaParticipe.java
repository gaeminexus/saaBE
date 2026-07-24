package com.saa.model.crd;

import java.io.Serializable;

import com.saa.model.tsr.BancoExterno;

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
 * Representa la tabla CNBP (CuentaBancariaParticipe).
 * Almacena las cuentas bancarias de una entidad (partícipe).
 * Relación 1:N con ENTD.
 * El tipo de banco proviene de TSR.BEXT (BancoExterno).
 * El tipo de cuenta (CNBPTPCN) almacena el codigoAlterno del DetalleRubro
 * correspondiente al rubro de tipo de cuenta bancaria.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "CNBP", schema = "CRD")
@NamedQueries({
    @NamedQuery(name = "CuentaBancariaParticipeAll", query = "select e from CuentaBancariaParticipe e"),
    @NamedQuery(name = "CuentaBancariaParticipeId",  query = "select e from CuentaBancariaParticipe e where e.codigo = :id")
})
public class CuentaBancariaParticipe implements Serializable {

    /** Código PK */
    @Id
    @Basic
    @Column(name = "CNBPCDGO")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /** FK - Entidad (Partícipe) */
    @ManyToOne
    @JoinColumn(name = "ENTDCDGO", referencedColumnName = "ENTDCDGO")
    private Entidad entidad;

    /** FK - Banco Externo (TSR.BEXT) */
    @ManyToOne
    @JoinColumn(name = "BEXTCDGO", referencedColumnName = "BEXTCDGO")
    private BancoExterno bancoExterno;

    /**
     * Tipo de cuenta bancaria.
     * Almacena el codigoAlterno del DetalleRubro del rubro de tipo de cuenta bancaria (codigoAlterno = 23).
     */
    @Basic
    @Column(name = "CNBPTPCN")
    private Long tipoCuenta;

    /** Número de cuenta */
    @Basic
    @Column(name = "CNBPNMRO", length = 100)
    private String numeroCuenta;

    /** ID Estado */
    @Basic
    @Column(name = "CNBPIDST")
    private Long estado;

    // ============================
    // Getters y Setters
    // ============================

    public Long getCodigo() { return codigo; }
    public void setCodigo(Long codigo) { this.codigo = codigo; }

    public Entidad getEntidad() { return entidad; }
    public void setEntidad(Entidad entidad) { this.entidad = entidad; }

    public BancoExterno getBancoExterno() { return bancoExterno; }
    public void setBancoExterno(BancoExterno bancoExterno) { this.bancoExterno = bancoExterno; }

    public Long getTipoCuenta() { return tipoCuenta; }
    public void setTipoCuenta(Long tipoCuenta) { this.tipoCuenta = tipoCuenta; }

    public String getNumeroCuenta() { return numeroCuenta; }
    public void setNumeroCuenta(String numeroCuenta) { this.numeroCuenta = numeroCuenta; }

    public Long getEstado() { return estado; }
    public void setEstado(Long estado) { this.estado = estado; }
}
