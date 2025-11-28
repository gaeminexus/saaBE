/**
 * Copyright (c) 2010 Compuseg Cía. Ltda.
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados.
 * Este software es la información confidencial y patentada de Compuseg Cía. Ltda. ("Información Confidencial").
 * Usted no puede divulgar dicha Información Confidencial y se utilizará sólo en conformidad con los términos del acuerdo
 * de licencia que ha introducido dentro de Compuseg.
 */
package com.saa.model.cxc;

import java.io.Serializable;
import java.util.Date;

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
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

/**
 * @author GaemiSoft
 * Pojo mapeo de tabla CBR.PRDC.
 * Entity ProductoCobro.
 * Tabla que contiene los productos que la empresa ofrece a los clientes.
 * Hija de la tabla grupo producto (GRPC).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "PRDC", schema = "CBR")
@SequenceGenerator(name = "SQ_PRDCCDGO", sequenceName = "CBR.SQ_PRDCCDGO", allocationSize = 1)
@NamedQueries({
    @NamedQuery(name = "ProductoCobroAll", query = "select e from ProductoCobro e"),
    @NamedQuery(name = "ProductoCobroId", query = "select e from ProductoCobro e where e.codigo = :id")
})
public class ProductoCobro implements Serializable {

    /**
     * Código de la entidad.
     */
    @Id
    @Basic
    @Column(name = "PRDCCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /**
     * Empresa a la que pertenece el grupo de producto.
     */
    @ManyToOne
    @JoinColumn(name = "PJRQCDGO", referencedColumnName = "PJRQCDGO")
    private Empresa empresa;

    /* /**
     * Tipo de producto tomado de la tabla de mapeo de productos
     * del SRI del usuario PRG.
     
    @ManyToOne
    @JoinColumn(name = "STPRCDGO", referencedColumnName = "STPRCDGO")
    private SRITipoProducto sRTipoProducto;*/

    /**
     * Nombre del grupo de productos.
     */
    @Basic
    @Column(name = "PRDCNMBR")
    private String nombre;

    /**
     * Aplica IVA. 1 = sí, 0 = no.
     */
    @Basic
    @Column(name = "PRDCAPIV")
    private Long aplicaIVA;

    /**
     * Aplica retención. 1 = sí, 0 = no.
     */
    @Basic
    @Column(name = "PRDCAPRT")
    private Long aplicaRetencion;

    /**
     * Estado: 1 = activo, 2 = inactivo.
     */
    @Basic
    @Column(name = "PRDCESTD")
    private Long estado;

    /**
     * Fecha de ingreso.
     */
    @Basic
    @Column(name = "PRDCFCIN")
    @Temporal(TemporalType.TIMESTAMP)
    private Date fechaIngreso;

    /**
     * Nivel del producto en el árbol.
     */
    @Basic
    @Column(name = "PRDCNVLL")
    private Long nivel;

    /**
     * Código del padre del producto.
     * Tomado del id de esta misma entidad.
     */
    @Basic
    @Column(name = "PRDCCDPD")
    private Long idPadre;

    /**
     * Grupo de producto de pagos al que pertenece.
     */
    @ManyToOne
    @JoinColumn(name = "GRPCCDGO", referencedColumnName = "GRPCCDGO")
    private GrupoProductoCobro grupoProductoCobro;

    /**
     * Porcentaje de la base para la retención.
     * Usualmente 100%. Para productos de seguros puede ser 10%.
     */
    @Basic
    @Column(name = "PRDCPRBR")
    private Double porcentajeBaseRetencion;

    /**
     * Fecha de anulación.
     */
    @Basic
    @Column(name = "PRDCFCAN")
    @Temporal(TemporalType.TIMESTAMP)
    private Date fechaAnulacion;

    /**
     * Número que identifica al producto en el árbol.
     */
    @Basic
    @Column(name = "PRDCNMRO")
    private String numero;

    /**
     * Tipo de producto dependiendo del nivel que ocupa:
     * acumulación o movimiento.
     */
    @Basic
    @Column(name = "PRDCTPNV")
    private Long tipoNivel;

    // ============================================================
    // Getters y Setters
    // ============================================================

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }

    /*
    public SRITipoProducto getsRTipoProducto() {
        return sRTipoProducto;
    }

    public void setsRTipoProducto(SRITipoProducto sRTipoProducto) {
        this.sRTipoProducto = sRTipoProducto;
    }*/

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Long getAplicaIVA() {
        return aplicaIVA;
    }

    public void setAplicaIVA(Long aplicaIVA) {
        this.aplicaIVA = aplicaIVA;
    }

    public Long getAplicaRetencion() {
        return aplicaRetencion;
    }

    public void setAplicaRetencion(Long aplicaRetencion) {
        this.aplicaRetencion = aplicaRetencion;
    }

    public Long getEstado() {
        return estado;
    }

    public void setEstado(Long estado) {
        this.estado = estado;
    }

    public Date getFechaIngreso() {
        return fechaIngreso;
    }

    public void setFechaIngreso(Date fechaIngreso) {
        this.fechaIngreso = fechaIngreso;
    }

    public Long getNivel() {
        return nivel;
    }

    public void setNivel(Long nivel) {
        this.nivel = nivel;
    }

    public Long getIdPadre() {
        return idPadre;
    }

    public void setIdPadre(Long idPadre) {
        this.idPadre = idPadre;
    }

    public GrupoProductoCobro getGrupoProductoCobro() {
        return grupoProductoCobro;
    }

    public void setGrupoProductoCobro(GrupoProductoCobro grupoProductoCobro) {
        this.grupoProductoCobro = grupoProductoCobro;
    }

    public Double getPorcentajeBaseRetencion() {
        return porcentajeBaseRetencion;
    }

    public void setPorcentajeBaseRetencion(Double porcentajeBaseRetencion) {
        this.porcentajeBaseRetencion = porcentajeBaseRetencion;
    }

    public Date getFechaAnulacion() {
        return fechaAnulacion;
    }

    public void setFechaAnulacion(Date fechaAnulacion) {
        this.fechaAnulacion = fechaAnulacion;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public Long getTipoNivel() {
        return tipoNivel;
    }

    public void setTipoNivel(Long tipoNivel) {
        this.tipoNivel = tipoNivel;
    }
}
