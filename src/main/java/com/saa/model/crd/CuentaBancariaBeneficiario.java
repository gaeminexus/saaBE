package com.saa.model.crd;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

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
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

/**
 * Representa la tabla CRD.CBBP (CuentaBancariaBeneficiario).
 *
 * Beneficiario de un partícipe fallecido y la cuenta bancaria a la que cobra su porcentaje del
 * valor de sepelio recibido en {@link RecepcionValorSeguro} (CRD.RVSG). Paralelo directo de
 * {@link CuentaBancariaParticipe} (CRD.CNBP) para el titular, pero con nombre e identificación
 * propios porque el titular ya no puede cobrar.
 *
 * El porcentaje NO se valida a que sume 100 en esta capa: se valida al pagar (fase 2b, sin
 * implementar). Ver {@code docs/logica-negocio/crd/API-BENEFICIARIOS-PARTICIPE.md}.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "CBBP", schema = "CRD")
@SequenceGenerator(name = "SQ_CBBPCDGO", sequenceName = "CRD.SQ_CBBPCDGO", allocationSize = 1)
@NamedQueries({
    @NamedQuery(name = "CuentaBancariaBeneficiarioAll", query = "select e from CuentaBancariaBeneficiario e"),
    @NamedQuery(name = "CuentaBancariaBeneficiarioId",  query = "select e from CuentaBancariaBeneficiario e where e.codigo = :id")
})
public class CuentaBancariaBeneficiario implements Serializable {

    /** Código PK. */
    @Id
    @Basic
    @Column(name = "CBBPCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_CBBPCDGO")
    private Long codigo;

    /** FK - Entidad (partícipe fallecido) del que este beneficiario cobra. */
    @ManyToOne
    @JoinColumn(name = "ENTDCDGO", referencedColumnName = "ENTDCDGO")
    private Entidad entidad;

    /** Nombre completo del beneficiario. */
    @Basic
    @Column(name = "CBBPNMBR", length = 200)
    private String nombre;

    /** Identificación (cédula) del beneficiario. Única por partícipe, no en toda la tabla. */
    @Basic
    @Column(name = "CBBPIDNT", length = 20)
    private String numeroIdentificacion;

    /** FK - Banco externo (TSR.BEXT). */
    @ManyToOne
    @JoinColumn(name = "BEXTCDGO", referencedColumnName = "BEXTCDGO")
    private BancoExterno bancoExterno;

    /**
     * Tipo de cuenta bancaria.
     * Almacena el codigoAlterno del DetalleRubro, mismo catálogo que CNBPTPCN.
     */
    @Basic
    @Column(name = "CBBPTPCN")
    private Long tipoCuenta;

    /** Número de cuenta. */
    @Basic
    @Column(name = "CBBPNMRO", length = 100)
    private String numeroCuenta;

    /** Porcentaje del valor de sepelio que le corresponde a este beneficiario, en (0, 100]. */
    @Basic
    @Column(name = "CBBPPRCN")
    private BigDecimal porcentaje;

    /** Estado: 1 activo, 2 inactivo (com.saa.rubros.EstadoCuentasBancarias). */
    @Basic
    @Column(name = "CBBPIDST")
    private Long estado;

    /** Usuario que registró el beneficiario. */
    @Basic
    @Column(name = "CBBPUSRG", length = 50)
    private String usuarioRegistro;

    /** Fecha de registro. */
    @Basic
    @Column(name = "CBBPFCRG")
    private LocalDate fechaRegistro;

    // ============================================================
    // Getters y Setters
    // ============================================================

    public Long getCodigo() { return codigo; }
    public void setCodigo(Long codigo) { this.codigo = codigo; }

    public Entidad getEntidad() { return entidad; }
    public void setEntidad(Entidad entidad) { this.entidad = entidad; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getNumeroIdentificacion() { return numeroIdentificacion; }
    public void setNumeroIdentificacion(String numeroIdentificacion) { this.numeroIdentificacion = numeroIdentificacion; }

    public BancoExterno getBancoExterno() { return bancoExterno; }
    public void setBancoExterno(BancoExterno bancoExterno) { this.bancoExterno = bancoExterno; }

    public Long getTipoCuenta() { return tipoCuenta; }
    public void setTipoCuenta(Long tipoCuenta) { this.tipoCuenta = tipoCuenta; }

    public String getNumeroCuenta() { return numeroCuenta; }
    public void setNumeroCuenta(String numeroCuenta) { this.numeroCuenta = numeroCuenta; }

    public BigDecimal getPorcentaje() { return porcentaje; }
    public void setPorcentaje(BigDecimal porcentaje) { this.porcentaje = porcentaje; }

    public Long getEstado() { return estado; }
    public void setEstado(Long estado) { this.estado = estado; }

    public String getUsuarioRegistro() { return usuarioRegistro; }
    public void setUsuarioRegistro(String usuarioRegistro) { this.usuarioRegistro = usuarioRegistro; }

    public LocalDate getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDate fechaRegistro) { this.fechaRegistro = fechaRegistro; }
}
