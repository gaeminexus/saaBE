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
 * <p>Pojo mapeo de tabla TSR.DSDT.
 *  Entity Desglose de Detalle de deposito.
 *  Almacena el desglose del detalle de deposito.
 *  Contiene los valores que se enviaron a cada banco en un deposito.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "DSDT", schema = "TSR")
@SequenceGenerator(name = "SQ_DSDTCDGO", sequenceName = "TSR.SQ_DSDTCDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "DesgloseDetalleDepositoAll", query = "select e from DesgloseDetalleDeposito e"),
	@NamedQuery(name = "DesgloseDetalleDepositoId", query = "select e from DesgloseDetalleDeposito e where e.codigo = :id")
})
public class DesgloseDetalleDeposito implements Serializable {

    /**
     * Id.
     */
    @Basic
    @Id
    @Column(name = "DSDTCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_DSDTCDGO")
    private Long codigo;
    
    /**
     * Detalle de deposito al que pertenece
     */
    @ManyToOne
    @JoinColumn(name = "DTDPCDGO", referencedColumnName = "DTDPCDGO")
    private DetalleDeposito detalleDeposito;    

    /**
     * Tipo de deposito. 1 = Efectivo, 2 = Cheque 
     */
    @Basic
    @Column(name = "DSDTTPOO")
    private Long tipo;
    
    /**
     * Valor.
     */
    @Basic
    @Column(name = "DSDTVLRR")
    private Double valor;
    
    /**
     * Cobro al que hace referencia el desglose
     */
    @ManyToOne
    @JoinColumn(name = "CBROCDGO", referencedColumnName = "CBROCDGO")
    private Cobro cobro;    

    /**
     * Banco Externo del que se origino el cobro. Para el caso de cobro con cheque
     */
    @ManyToOne
    @JoinColumn(name = "BEXTCDGO", referencedColumnName = "BEXTCDGO")
    private BancoExterno bancoExterno;    

    /**
     * Numero de cheque con que se realizo el cobro.
     */
    @Basic
    @Column(name = "CCHQNMRO")
    private Long numeroCheque;
    
    /**
     * Cobro con cheque al que hace referencia el desglose
     */
    @ManyToOne
    @JoinColumn(name = "CCHQCDGO", referencedColumnName = "CCHQCDGO")
    private CobroCheque cobroCheque;    
    
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
     * Devuelve detalleDeposito
     */
    public DetalleDeposito getDetalleDeposito() {
        return this.detalleDeposito;
    }
    
    /**
     * Asigna detalleDeposito
     */
    public void setDetalleDeposito(DetalleDeposito detalleDeposito) {
        this.detalleDeposito = detalleDeposito;
    }

    /**
     * Devuelve tipo
     * @return tipo
     */
    public Long getTipo() {
        return tipo;
    }

    /**
     * Asigna tipo
     * @param tipo Nuevo valor para tipo 
     */
    public void setTipo(Long tipo) {
        this.tipo = tipo;
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
    
    /**
     * Devuelve cobro
     */
    public Cobro getCobro() {
        return this.cobro;
    }
    
    /**
     * Asigna cobro
     */
    public void setCobro(Cobro cobro) {
        this.cobro = cobro;
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
     * Devuelve numeroCheque
     * @return numeroCheque
     */
    public Long getNumeroCheque() {
        return numeroCheque;
    }

    /**
     * Asigna numeroCheque
     * @param numeroCheque Nuevo valor para numeroCheque 
     */
    public void setNumeroCheque(Long numeroCheque) {
        this.numeroCheque = numeroCheque;
    }
    
    /**
     * Devuelve cobroCheque
     */
    public CobroCheque getCobroCheque() {
        return this.cobroCheque;
    }
    
    /**
     * Asigna cobroCheque
     */
    public void setCobroCheque(CobroCheque cobroCheque) {
        this.cobroCheque = cobroCheque;
    }
	
}
