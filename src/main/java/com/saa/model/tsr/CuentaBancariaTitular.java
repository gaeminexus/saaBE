/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.model.tsr;

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
 * @author GaemiSoft
 * <p>Pojo mapeo de tabla TSR.CTBN.
 * Entity CuentaBancariaTitular.
 * Almacena las cuentas bancarias asociadas a los titulares del sistema.
 * Se relaciona con la tabla de bancos externos y almacena el tipo de cuenta
 * según el rubro con código alterno 23 (TipoCuentasBancarias).</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "CTBN", schema = "TSR")
@NamedQueries({
    @NamedQuery(name = "CuentaBancariaTitularAll",
        query = "select e from CuentaBancariaTitular e order by e.titular.codigo, e.banco.nombre"),
    @NamedQuery(name = "CuentaBancariaTitularId",
        query = "select e from CuentaBancariaTitular e where e.codigo = :id"),
    @NamedQuery(name = "CuentaBancariaTitularByTitular",
        query = "select e from CuentaBancariaTitular e where e.titular.codigo = :titularId order by e.banco.nombre")
})
public class CuentaBancariaTitular implements Serializable {

    /**
     * Código / PK autoincrementable.
     */
    @Basic
    @Id
    @Column(name = "CTBNCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /**
     * FK al Titular dueño de la cuenta bancaria.
     */
    @ManyToOne
    @JoinColumn(name = "TTLRCDGO", nullable = false)
    private Titular titular;

    /**
     * FK al Banco Externo donde se tiene la cuenta.
     */
    @ManyToOne
    @JoinColumn(name = "BEXTCDGO", nullable = false)
    private BancoExterno banco;

    /**
     * Tipo de cuenta bancaria.
     * Referencia al rubro con código alterno 23 (TipoCuentasBancarias):
     * 1 = Corriente, 2 = Ahorros.
     */
    @Basic
    @Column(name = "CTBNTPCT")
    private Long tipoCuenta;

    /**
     * Número de cuenta bancaria.
     */
    @Basic
    @Column(name = "CTBNNMCT", length = 50)
    private String numeroCuenta;

    /**
     * Observaciones adicionales sobre la cuenta.
     */
    @Basic
    @Column(name = "CTBNOBSR", length = 500)
    private String observaciones;

    /**
     * Estado del registro. 1 = Activo, 0 = Inactivo.
     */
    @Basic
    @Column(name = "CTBNESTD")
    private Long estado;

    /**
     * Fecha y hora de creación del registro (auditoría).
     */
    @Basic
    @Column(name = "CTBNFCRG")
    private LocalDateTime fechaCreacion;

    /**
     * Usuario que creó el registro (auditoría).
     */
    @Basic
    @Column(name = "CTBNUSAR", length = 50)
    private String usuarioCreacion;

    // -------------------------------------------------------------------------
    // GETTERS Y SETTERS
    // -------------------------------------------------------------------------

    /**
     * Devuelve codigo
     * @return codigo
     */
    public Long getCodigo() {
        return codigo;
    }

    /**
     * Asigna codigo
     * @param codigo Nuevo valor de codigo
     */
    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    /**
     * Devuelve titular
     * @return titular
     */
    public Titular getTitular() {
        return titular;
    }

    /**
     * Asigna titular
     * @param titular Nuevo valor de titular
     */
    public void setTitular(Titular titular) {
        this.titular = titular;
    }

    /**
     * Devuelve banco
     * @return banco
     */
    public BancoExterno getBanco() {
        return banco;
    }

    /**
     * Asigna banco
     * @param banco Nuevo valor de banco
     */
    public void setBanco(BancoExterno banco) {
        this.banco = banco;
    }

    /**
     * Devuelve tipoCuenta
     * @return tipoCuenta (rubro 23: 1=Corriente, 2=Ahorros)
     */
    public Long getTipoCuenta() {
        return tipoCuenta;
    }

    /**
     * Asigna tipoCuenta
     * @param tipoCuenta Nuevo valor de tipoCuenta
     */
    public void setTipoCuenta(Long tipoCuenta) {
        this.tipoCuenta = tipoCuenta;
    }

    /**
     * Devuelve numeroCuenta
     * @return numeroCuenta
     */
    public String getNumeroCuenta() {
        return numeroCuenta;
    }

    /**
     * Asigna numeroCuenta
     * @param numeroCuenta Nuevo valor de numeroCuenta
     */
    public void setNumeroCuenta(String numeroCuenta) {
        this.numeroCuenta = numeroCuenta;
    }

    /**
     * Devuelve observaciones
     * @return observaciones
     */
    public String getObservaciones() {
        return observaciones;
    }

    /**
     * Asigna observaciones
     * @param observaciones Nuevo valor de observaciones
     */
    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    /**
     * Devuelve estado
     * @return estado
     */
    public Long getEstado() {
        return estado;
    }

    /**
     * Asigna estado
     * @param estado Nuevo valor de estado
     */
    public void setEstado(Long estado) {
        this.estado = estado;
    }

    /**
     * Devuelve fechaCreacion
     * @return fechaCreacion
     */
    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    /**
     * Asigna fechaCreacion
     * @param fechaCreacion Nuevo valor de fechaCreacion
     */
    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    /**
     * Devuelve usuarioCreacion
     * @return usuarioCreacion
     */
    public String getUsuarioCreacion() {
        return usuarioCreacion;
    }

    /**
     * Asigna usuarioCreacion
     * @param usuarioCreacion Nuevo valor de usuarioCreacion
     */
    public void setUsuarioCreacion(String usuarioCreacion) {
        this.usuarioCreacion = usuarioCreacion;
    }
}
