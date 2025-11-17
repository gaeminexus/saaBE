/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.model.tesoreria;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.saa.model.scp.Empresa;

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
 * <p>Pojo mapeo de tabla TSR.TCBR.
 *  Entity Temporal de cobro.
 *  Almacena el cobro de manera temporal durante el ingreso del cobro.
 *  Luego pasara a la tabla TSR.CBRO</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "TCBR", schema = "TSR")
@SequenceGenerator(name = "SQ_TCBRCDGO", sequenceName = "TSR.SQ_TCBRCDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "TempCobroAll", query = "select e from TempCobro e"),
	@NamedQuery(name = "TempCobroId", query = "select e from TempCobro e where e.codigo = :id")
})
public class TempCobro implements Serializable {

    /**
     * Id de tabla.
     */
    @Basic
    @Id
    @Column(name = "TCBRCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_TCBRCDGO")
    private Long codigo;
    
    /**
     * Tipo de identificacion. 1 = Cedula, 2 = Ruc.
     */
    @Basic
    @Column(name = "TCBRTPID")
    private Long tipoId;
    
    /**
     * Numero de identificacion.
     */
    @Basic
    @Column(name = "TCBRIDNT", length = 20)
    private String numeroId;
    
    /**
     * Nombre del cliente que realiza el cobro.
     */
    @Basic
    @Column(name = "TCBRCLNT", length = 200)
    private String cliente;
    
    /**
     * Descripcion del cobro.
     */
    @Basic
    @Column(name = "TCBRDSCR", length = 300)
    private String descripcion;
    
    /**
     * Fecha en que se realiza el cobro.
     */
    @Basic
    @Column(name = "TCBRFCHA")
    private LocalDateTime fecha;
    
    /**
     * Nombre del usuario que realiza el cobro.
     */
    @Basic
    @Column(name = "TCBRUSRO", length = 50)
    private String nombreUsuario;
    
    /**
     * Valor total del cobro.
     */
    @Basic
    @Column(name = "TCBRVLRR")
    private Double valor;
    
    /**
     * Empresa en la que se realiza el cobro.
     */
    @ManyToOne
    @JoinColumn(name = "PJRQCDGO", referencedColumnName = "PJRQCDGO")
    private Empresa empresa;    

    /**
     * Usuario por caja en la que se realiza el cobro
     */
    @ManyToOne
    @JoinColumn(name = "USXCCDGO", referencedColumnName = "USXCCDGO")
    private UsuarioPorCaja usuarioPorCaja;    

    /**
     * Cierre de caja que contiene los cobros temporales
     */
    @ManyToOne
    @JoinColumn(name = "CRCJCDGO", referencedColumnName = "CRCJCDGO")
    private CierreCaja cierreCaja;
     
    /**
     * Fecha de desactivacion del cobro.
     */
    @Basic
    @Column(name = "TCBRFCDS")
    private LocalDateTime fechaInactivo;
    
    /**
     * Rubro 29. Motivo de anulacion del cobro
     */
    @Basic
    @Column(name = "TCBRRYYB")
    private Long rubroMotivoAnulacionP;
    
    /**
     * Detalle de rubro 29. Motivo de anulacion del cobro.
     */
    @Basic
    @Column(name = "TCBRRZZB")
    private Long rubroMotivoAnulacionH;
    
    /**
     * Rubro 28. Estado de cobros
     */
    @Basic
    @Column(name = "TCBRRYYA")
    private Long rubroEstadoP;
    
    /**
     * Detalle de rubro 28. Estado de cobros.
     */
    @Basic
    @Column(name = "TCBRRZZA")
    private Long rubroEstadoH;
    
    /**
     * Caja logica en que se realiza el cobro
     */
    @ManyToOne
    @JoinColumn(name = "CJCNCDGO", referencedColumnName = "CJCNCDGO")
    private CajaLogica cajaLogica;    

    /**
     * Persona a la que se realiza el cobro
     */
    @ManyToOne
    @JoinColumn(name = "PRSNCDGO", referencedColumnName = "PRSNCDGO")
    private Persona persona;    

    /**
     * Tipo de cobro. 1 = Factura, 2 = Anticipo.
     */
    @Basic
    @Column(name = "TCBRTPCB")
    private Long tipoCobro;
    
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
     * Devuelve tipoId
     * @return tipoId
     */
    public Long getTipoId() {
        return tipoId;
    }

    /**
     * Asigna tipoId
     * @param tipoId Nuevo valor para tipoId 
     */
    public void setTipoId(Long tipoId) {
        this.tipoId = tipoId;
    }
    
    /**
     * Devuelve numeroId
     * @return numeroId
     */
    public String getNumeroId() {
        return numeroId;
    }

    /**
     * Asigna numeroId
     * @param numeroId Nuevo valor para numeroId 
     */
    public void setNumeroId(String numeroId) {
        this.numeroId = numeroId;
    }
    
    /**
     * Devuelve cliente
     * @return cliente
     */
    public String getCliente() {
        return cliente;
    }

    /**
     * Asigna cliente
     * @param cliente Nuevo valor para cliente 
     */
    public void setCliente(String cliente) {
        this.cliente = cliente;
    }
    
    /**
     * Devuelve descripcion
     * @return descripcion
     */
    public String getDescripcion() {
        return descripcion;
    }

    /**
     * Asigna descripcion
     * @param descripcion Nuevo valor para descripcion 
     */
    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
    
    /**
     * Devuelve fecha
     * @return fecha
     */
    public LocalDateTime getFecha() {
        return fecha;
    }

    /**
     * Asigna fecha
     * @param fecha Nuevo valor para fecha 
     */
    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }
    
    /**
     * Devuelve nombreUsuario
     * @return nombreUsuario
     */
    public String getNombreUsuario() {
        return nombreUsuario;
    }

    /**
     * Asigna nombreUsuario
     * @param nombreUsuario Nuevo valor para nombreUsuario 
     */
    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
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
     * Devuelve empresa
     */
    public Empresa getEmpresa() {
        return this.empresa;
    }
    
    /**
     * Asigna empresa
     */
    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }

    /**
     * Devuelve usuarioPorCaja
     */
    public UsuarioPorCaja getUsuarioPorCaja() {
        return this.usuarioPorCaja;
    }
    
    /**
     * Asigna usuarioPorCaja
     */
    public void setUsuarioPorCaja(UsuarioPorCaja usuarioPorCaja) {
        this.usuarioPorCaja = usuarioPorCaja;
    }    

    /**
     * Devuelve cierreCaja
     */
    public CierreCaja getCierreCaja() {
        return cierreCaja;
    }

    /**
     * Asigna cierreCaja
     */
    public void setCierreCaja(CierreCaja cierreCaja) {
        this.cierreCaja = cierreCaja;
    }

    /**
     * Devuelve fechaInactivo
     * @return fechaInactivo
     */
    public LocalDateTime getFechaInactivo() {
        return fechaInactivo;
    }

    /**
     * Asigna fechaInactivo
     * @param fechaInactivo Nuevo valor para fechaInactivo 
     */
    public void setFechaInactivo(LocalDateTime fechaInactivo) {
        this.fechaInactivo = fechaInactivo;
    }
    
    /**
     * Devuelve rubroMotivoAnulacionP
     * @return rubroMotivoAnulacionP
     */
    public Long getRubroMotivoAnulacionP() {
        return rubroMotivoAnulacionP;
    }

    /**
     * Asigna rubroMotivoAnulacionP
     * @param rubroMotivoAnulacionP Nuevo valor para rubroMotivoAnulacionP 
     */
    public void setRubroMotivoAnulacionP(Long rubroMotivoAnulacionP) {
        this.rubroMotivoAnulacionP = rubroMotivoAnulacionP;
    }
    
    /**
     * Devuelve rubroMotivoAnulacionH
     * @return rubroMotivoAnulacionH
     */
    public Long getRubroMotivoAnulacionH() {
        return rubroMotivoAnulacionH;
    }

    /**
     * Asigna rubroMotivoAnulacionH
     * @param rubroMotivoAnulacionH Nuevo valor para rubroMotivoAnulacionH 
     */
    public void setRubroMotivoAnulacionH(Long rubroMotivoAnulacionH) {
        this.rubroMotivoAnulacionH = rubroMotivoAnulacionH;
    }
    
    /**
     * Devuelve rubroEstadoP
     * @return rubroEstadoP
     */
    public Long getRubroEstadoP() {
        return rubroEstadoP;
    }

    /**
     * Asigna rubroEstadoP
     * @param rubroEstadoP Nuevo valor para rubroEstadoP 
     */
    public void setRubroEstadoP(Long rubroEstadoP) {
        this.rubroEstadoP = rubroEstadoP;
    }
    
    /**
     * Devuelve rubroEstadoH
     * @return rubroEstadoH
     */
    public Long getRubroEstadoH() {
        return rubroEstadoH;
    }

    /**
     * Asigna rubroEstadoH
     * @param rubroEstadoH Nuevo valor para rubroEstadoH 
     */
    public void setRubroEstadoH(Long rubroEstadoH) {
        this.rubroEstadoH = rubroEstadoH;
    }
    
    /**
     * Devuelve cajaLogica
     */
    public CajaLogica getCajaLogica() {
        return this.cajaLogica;
    }
    
    /**
     * Asigna cajaLogica
     */
    public void setCajaLogica(CajaLogica cajaLogica) {
        this.cajaLogica = cajaLogica;
    }

    /**
     * Devuelve persona
     */
    public Persona getPersona() {
        return this.persona;
    }
    
    /**
     * Asigna persona
     */
    public void setPersona(Persona persona) {
        this.persona = persona;
    }

    /**
     * Devuelve tipoCobro
     * @return tipoCobro
     */
    public Long getTipoCobro() {
        return tipoCobro;
    }

    /**
     * Asigna tipoCobro
     * @param tipoCobro Nuevo valor para tipoCobro 
     */
    public void setTipoCobro(Long tipoCobro) {
        this.tipoCobro = tipoCobro;
    }
    
}