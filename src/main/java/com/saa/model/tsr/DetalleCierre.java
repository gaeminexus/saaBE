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
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

/**
 *  @author GaemiSoft
 * <p>Pojo mapeo de tabla TSR.DTCR.
 *  Entity Detalle de cierre de caja.
 *  Almacena un resumen de todos los cobros incluidos en el cierre.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "DTCR", schema = "TSR")
@SequenceGenerator(name = "SQ_DTCRCDGO", sequenceName = "TSR.SQ_DTCRCDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "DetalleCierreAll", query = "select e from DetalleCierre e"),
	@NamedQuery(name = "DetalleCierreId", query = "select e from DetalleCierre e where e.codigo = :id")
})
public class DetalleCierre implements Serializable {

    /**
     * Id.
     */
    @Basic
    @Id
    @Column(name = "DTCRCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_DTCRCDGO")
    private Long codigo;
    
    /**
     * Cierre de caja al que pertenece
     */
    @ManyToOne
    @JoinColumn(name = "CRCJCDGO", referencedColumnName = "CRCJCDGO")
    private CierreCaja cierreCaja;    

    /**
     * Attribute cobro
     */
    @ManyToOne
    @JoinColumn(name = "CBROCDGO", referencedColumnName = "CBROCDGO")
    private Cobro cobro;    

    /**
     * Nombre del cliente al que se realizó el cobro.
     */
    @Basic
    @Column(name = "CBROCLNT", length = 200)
    private String nombreCliente;
    
    /**
     * Fecha en la que se realizo el cobro.
     */
    @Basic
    @Column(name = "CBROFCHA")
    private LocalDateTime fechaCobro;
    
    /**
     * Valor en efectivo del cobro.
     */
    @Basic
    @Column(name = "DTCREFCT")
    private Double valorEfectivo;
    
    /**
     * Valor en cheque del cobro.
     */
    @Basic
    @Column(name = "DTCRCHQQ")
    private Double valorCheque;
    
    /**
     * Valor en tarjeta del cobro.
     */
    @Basic
    @Column(name = "DTCRTRJC")
    private Double valorTarjeta;
    
    /**
     * Valor en transferencia del cobro.
     */
    @Basic
    @Column(name = "DTCRTRNS")
    private Double valorTransferencia;
    
    /**
     * Valor en retencion del cobro.
     */
    @Basic
    @Column(name = "DTCRRTNC")
    private Double valorRetencion;
    
    /**
     * Valor total del cobro.
     */
    @Basic
    @Column(name = "CBROVLRR")
    private Double valorTotal;    
    
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
     * Devuelve cierreCaja
     */
    public CierreCaja getCierreCaja() {
        return this.cierreCaja;
    }
    
    /**
     * Asigna cierreCaja
     */
    public void setCierreCaja(CierreCaja cierreCaja) {
        this.cierreCaja = cierreCaja;
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
     * Devuelve nombreCliente
     * @return nombreCliente
     */
    public String getNombreCliente() {
        return nombreCliente;
    }

    /**
     * Asigna nombreCliente
     * @param nombreCliente Nuevo valor para nombreCliente 
     */
    public void setNombreCliente(String nombreCliente) {
        this.nombreCliente = nombreCliente;
    }
    
    /**
     * Devuelve fechaCobro
     * @return fechaCobro
     */
    public LocalDateTime getFechaCobro() {
        return fechaCobro;
    }

    /**
     * Asigna fechaCobro
     * @param fechaCobro Nuevo valor para fechaCobro 
     */
    public void setFechaCobro(LocalDateTime fechaCobro) {
        this.fechaCobro = fechaCobro;
    }
    
    /**
     * Devuelve valorEfectivo
     * @return valorEfectivo
     */
    public Double getValorEfectivo() {
        return valorEfectivo;
    }

    /**
     * Asigna valorEfectivo
     * @param valorEfectivo Nuevo valor para valorEfectivo 
     */
    public void setValorEfectivo(Double valorEfectivo) {
        this.valorEfectivo = valorEfectivo;
    }
    
    /**
     * Devuelve valorCheque
     * @return valorCheque
     */
    public Double getValorCheque() {
        return valorCheque;
    }

    /**
     * Asigna valorCheque
     * @param valorCheque Nuevo valor para valorCheque 
     */
    public void setValorCheque(Double valorCheque) {
        this.valorCheque = valorCheque;
    }
    
    /**
     * Devuelve valorTarjeta
     * @return valorTarjeta
     */
    public Double getValorTarjeta() {
        return valorTarjeta;
    }

    /**
     * Asigna valorTarjeta
     * @param valorTarjeta Nuevo valor para valorTarjeta 
     */
    public void setValorTarjeta(Double valorTarjeta) {
        this.valorTarjeta = valorTarjeta;
    }
    
    /**
     * Devuelve valorTransferencia
     * @return valorTransferencia
     */
    public Double getValorTransferencia() {
        return valorTransferencia;
    }

    /**
     * Asigna valorTransferencia
     * @param valorTransferencia Nuevo valor para valorTransferencia 
     */
    public void setValorTransferencia(Double valorTransferencia) {
        this.valorTransferencia = valorTransferencia;
    }
    
    /**
     * Devuelve valorRetencion
     * @return valorRetencion
     */
    public Double getValorRetencion() {
        return valorRetencion;
    }

    /**
     * Asigna valorRetencion
     * @param valorRetencion Nuevo valor para valorRetencion 
     */
    public void setValorRetencion(Double valorRetencion) {
        this.valorRetencion = valorRetencion;
    }
    
    /**
     * Devuelve valorTotal
     * @return valorTotal
     */
    public Double getValorTotal() {
        return valorTotal;
    }

    /**
     * Asigna valorTotal
     * @param valorTotal Nuevo valor para valorTotal 
     */
    public void setValorTotal(Double valorTotal) {
        this.valorTotal = valorTotal;
    }

}
