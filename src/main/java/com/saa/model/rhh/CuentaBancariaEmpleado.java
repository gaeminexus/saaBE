package com.saa.model.rhh;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.saa.basico.util.EntidadAuditableFechaHora;
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
 * Cuenta bancaria de acreditacion del empleado. Admite reparto del neto entre varias cuentas por porcentaje.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "CBEM", schema = "RHH")
@NamedQueries({
    @NamedQuery(name = "CuentaBancariaEmpleadoId", query = "select e from CuentaBancariaEmpleado e where e.codigo=:id"),
    @NamedQuery(name = "CuentaBancariaEmpleadoAll", query = "select e from CuentaBancariaEmpleado e")
})
public class CuentaBancariaEmpleado implements Serializable, EntidadAuditableFechaHora {

    /**
     * Codigo unico de la cuenta.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic
    @Column(name = "CBEMCDGO")
    private Long codigo;

    /**
     * Empleado titular de la relacion laboral.
     */
    @ManyToOne
    @JoinColumn(name = "MPLDCDGO", referencedColumnName = "MPLDCDGO")
    private Empleado empleado;

    /**
     * Banco de la cuenta (TSR.BEXT).
     */
    @ManyToOne
    @JoinColumn(name = "BEXTCDGO", referencedColumnName = "BEXTCDGO")
    private BancoExterno banco;

    /**
     * Tipo de cuenta: detalle del rubro RHH_TIPO_CUENTA_BANCARIA.
     */
    @Basic
    @Column(name = "CBEMTPCT")
    private Long tipoCuenta;

    /**
     * Numero de cuenta.
     */
    @Basic
    @Column(name = "CBEMNMCT", length = 30)
    private String numeroCuenta;

    /**
     * Titular de la cuenta, cuando difiere del empleado.
     */
    @Basic
    @Column(name = "CBEMTTLR", length = 200)
    private String titular;

    /**
     * Identificacion del titular de la cuenta.
     */
    @Basic
    @Column(name = "CBEMIDTT", length = 20)
    private String identificacionTitular;

    /**
     * Es la cuenta principal de acreditacion (S/N).
     */
    @Basic
    @Column(name = "CBEMPRCP", length = 1)
    private String principal;

    /**
     * Porcentaje del neto a acreditar en esta cuenta.
     */
    @Basic
    @Column(name = "CBEMPRCN")
    private Double porcentaje;

    /**
     * Estado del registro.
     */
    @Basic
    @Column(name = "CBEMESTD")
    private Long estado;

    /**
     * Fecha de registro.
     */
    @Basic
    @Column(name = "CBEMFCHR")
    private LocalDateTime fechaRegistro;

    /**
     * Usuario que registro.
     */
    @Basic
    @Column(name = "CBEMUSRR", length = 60)
    private String usuarioRegistro;

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public Empleado getEmpleado() {
        return empleado;
    }

    public void setEmpleado(Empleado empleado) {
        this.empleado = empleado;
    }

    public BancoExterno getBanco() {
        return banco;
    }

    public void setBanco(BancoExterno banco) {
        this.banco = banco;
    }

    public Long getTipoCuenta() {
        return tipoCuenta;
    }

    public void setTipoCuenta(Long tipoCuenta) {
        this.tipoCuenta = tipoCuenta;
    }

    public String getNumeroCuenta() {
        return numeroCuenta;
    }

    public void setNumeroCuenta(String numeroCuenta) {
        this.numeroCuenta = numeroCuenta;
    }

    public String getTitular() {
        return titular;
    }

    public void setTitular(String titular) {
        this.titular = titular;
    }

    public String getIdentificacionTitular() {
        return identificacionTitular;
    }

    public void setIdentificacionTitular(String identificacionTitular) {
        this.identificacionTitular = identificacionTitular;
    }

    public String getPrincipal() {
        return principal;
    }

    public void setPrincipal(String principal) {
        this.principal = principal;
    }

    public Double getPorcentaje() {
        return porcentaje;
    }

    public void setPorcentaje(Double porcentaje) {
        this.porcentaje = porcentaje;
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
}
