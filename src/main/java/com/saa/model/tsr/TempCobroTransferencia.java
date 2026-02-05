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
 * @author GaemiSoft 
 * <p>Pojo mapeo de tabla TSR.TCTR.
 *  Entity Temporal de cobro con transferencia.
 *  Almacena el cobro con transferencia de manera temporal.
 *  Luego pasara a la tabla TSR.CTRN.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "TCTR", schema = "TSR")
@SequenceGenerator(name = "SQ_TCTRCDGO", sequenceName = "TSR.SQ_TCTRCDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "TempCobroTransferenciaAll", query = "select e from TempCobroTransferencia e"),
	@NamedQuery(name = "TempCobroTransferenciaId", query = "select e from TempCobroTransferencia e where e.codigo = :id")
})
public class TempCobroTransferencia implements Serializable {

    /**
     * Id.
     */
    @Basic
    @Id
    @Column(name = "TCTRCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_TCTRCDGO")
    private Long codigo;
    
    /**
     * Cobro temporal al que pertenece
     */
    @ManyToOne
    @JoinColumn(name = "TCBRCDGO", referencedColumnName = "TCBRCDGO")
    private TempCobro tempCobro;    

    /**
     * Banco externo al que pertenece
     */
    @ManyToOne
    @JoinColumn(name = "BEXTCDGO", referencedColumnName = "BEXTCDGO")
    private BancoExterno bancoExterno;    

    /**
     * Cuenta origen de la transferencia.
     */
    @Basic
    @Column(name = "TCTRCTOR", length = 50)
    private String cuentaOrigen;
    
    /**
     * Número de transferencia
     */
    @Basic
    @Column(name = "TCTRNMRO")
    private Long numeroTransferencia;
    
    /**
     * Banco de la empresa a la que se realiza la transferencia
     */
    @ManyToOne
    @JoinColumn(name = "BNCOCDGO", referencedColumnName = "BNCOCDGO")
    private Banco banco;    

    /**
     * Cuenta bancaria a la que se realiza la transferencia
     */
    @ManyToOne
    @JoinColumn(name = "CNBCCDGO", referencedColumnName = "CNBCCDGO")
    private CuentaBancaria cuentaBancaria;    

    /**
     * Cuenta destino de la transferencia.
     */
    @Basic
    @Column(name = "TCTRCTDS", length = 50)
    private String cuentaDestino;
    
    /**
     * Valor de la transferencia.
     */
    @Basic
    @Column(name = "TCTRVLRR")
    private Double valor;
    
    /**
     * Devuelve codigo
     * @return codigo
     */
    public Long getCodigo() {
        return codigo;
    }

    /**
     * Asigna codigo
     * @param codigo Nuevo valor para codigo 
     */
    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }
    
    /**
     * Devuelve tempCobro
     */
    public TempCobro getTempCobro() {
        return this.tempCobro;
    }
    
    /**
     * Asigna tempCobro
     */
    public void setTempCobro(TempCobro tempCobro) {
        this.tempCobro = tempCobro;
    }

    /**
     * Devuelve bancoExterno
     */
    public BancoExterno getBancoExterno() {
        return this.bancoExterno;
    }
    
    /**
     * Asigna bancoExterno
     */
    public void setBancoExterno(BancoExterno bancoExterno) {
        this.bancoExterno = bancoExterno;
    }

    /**
     * Devuelve cuentaOrigen
     * @return cuentaOrigen
     */
    public String getCuentaOrigen() {
        return cuentaOrigen;
    }

    /**
     * Asigna cuentaOrigen
     * @param cuentaOrigen Nuevo valor para cuentaOrigen 
     */
    public void setCuentaOrigen(String cuentaOrigen) {
        this.cuentaOrigen = cuentaOrigen;
    }
    
    /**
     * Devuelve numeroTransferencia
     * @return numeroTransferencia
     */
    public Long getNumeroTransferencia() {
        return numeroTransferencia;
    }

    /**
     * Asigna numeroTransferencia
     * @param numeroTransferencia: Nuevo valor para numeroTransferencia
     */
    public void setNumeroTransferencia(Long numeroTransferencia) {
        this.numeroTransferencia = numeroTransferencia;
    }
    
    /**
     * Devuelve banco
     */
    public Banco getBanco() {
        return this.banco;
    }
    
    /**
     * Asigna banco
     */
    public void setBanco(Banco banco) {
        this.banco = banco;
    }

    /**
     * Devuelve cuentaBancaria
     */
    public CuentaBancaria getCuentaBancaria() {
        return this.cuentaBancaria;
    }
    
    /**
     * Asigna cuentaBancaria
     */
    public void setCuentaBancaria(CuentaBancaria cuentaBancaria) {
        this.cuentaBancaria = cuentaBancaria;
    }

    /**
     * Devuelve cuentaDestino
     * @return cuentaDestino
     */
    public String getCuentaDestino() {
        return cuentaDestino;
    }

    /**
     * Asigna cuentaDestino
     * @param cuentaDestino Nuevo valor para cuentaDestino 
     */
    public void setCuentaDestino(String cuentaDestino) {
        this.cuentaDestino = cuentaDestino;
    }
    
    /**
     * Devuelve valor
     * @return valor
     */
    public Double getValor() {
        return valor;
    }

    /**
     * Asigna valor
     * @param valor Nuevo valor para valor 
     */
    public void setValor(Double valor) {
        this.valor = valor;
    }
}
