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
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

/**
 * Representa la tabla PRDC (Producto).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "PRDC", schema = "CRD")
@NamedQueries({
    @NamedQuery(name = "ProductoAll", query = "select e from Producto e"),
    @NamedQuery(name = "ProductoId", query = "select e from Producto e where e.codigo = :id")
})
public class Producto implements Serializable {

    // ============================================================
    // CAMPOS – MAPEOS DIRECTOS A LA TABLA
    // ============================================================

    /** Código */
    @Id
    @Basic
    @Column(name = "PRDCCDGO")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /** Código SBS */
    @Basic
    @Column(name = "PRDCCSPB", length = 50, nullable = false)
    private String codigoSBS;

    /** Nombre */
    @Basic
    @Column(name = "PRDCNMBR", length = 2000)
    private String nombre;

    /** FK - Código Filial */
    @ManyToOne
    @JoinColumn(name = "FLLLCDGO", referencedColumnName = "FLLLCDGO")
    private Filial filial;

    /** FK - Código Tipo Préstamo */
    @ManyToOne
    @JoinColumn(name = "TPPRCDGO", referencedColumnName = "TPPRCDGO")
    private TipoPrestamo tipoPrestamo;

    /** Código externo */
    @Basic
    @Column(name = "PRDCCDEX")
    private Long codigoExterno;

    /** Fecha de registro */
    @Basic
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "PRDCFCRG")
    private Date fechaRegistro;

    /** Usuario registro */
    @Basic
    @Column(name = "PRDCUSRG", length = 2000)
    private String usuarioRegistro;

    /** IP registro */
    @Basic
    @Column(name = "PRDCIPRG", length = 50)
    private String ipRegistro;

    /** Fecha modificación */
    @Basic
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "PRDCFCMD")
    private Date fechaModificacion;

    /** Usuario modificación */
    @Basic
    @Column(name = "PRDCUSMD", length = 2000)
    private String usuarioModificacion;

    /** IP modificación */
    @Basic
    @Column(name = "PRDCIPMD", length = 50)
    private String ipModificacion;

    /** Estado */
    @Basic
    @Column(name = "PRDCESTD")
    private Long estado;

    /** codigoPetro */
    @Basic
    @Column(name = "PRDCCDPT")
    private String codigoPetro;

    // ============================================================
    // GETTERS Y SETTERS
    // ============================================================

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public String getCodigoSBS() {
        return codigoSBS;
    }

    public void setCodigoSBS(String codigoSBS) {
        this.codigoSBS = codigoSBS;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Filial getFilial() {
        return filial;
    }

    public void setFilial(Filial filial) {
        this.filial = filial;
    }

    public TipoPrestamo getTipoPrestamo() {
        return tipoPrestamo;
    }

    public void setTipoPrestamo(TipoPrestamo tipoPrestamo) {
        this.tipoPrestamo = tipoPrestamo;
    }

    public Long getCodigoExterno() {
        return codigoExterno;
    }

    public void setCodigoExterno(Long codigoExterno) {
        this.codigoExterno = codigoExterno;
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

    public String getIpRegistro() {
        return ipRegistro;
    }

    public void setIpRegistro(String ipRegistro) {
        this.ipRegistro = ipRegistro;
    }

    public Date getFechaModificacion() {
        return fechaModificacion;
    }

    public void setFechaModificacion(Date fechaModificacion) {
        this.fechaModificacion = fechaModificacion;
    }

    public String getUsuarioModificacion() {
        return usuarioModificacion;
    }

    public void setUsuarioModificacion(String usuarioModificacion) {
        this.usuarioModificacion = usuarioModificacion;
    }

    public String getIpModificacion() {
        return ipModificacion;
    }

    public void setIpModificacion(String ipModificacion) {
        this.ipModificacion = ipModificacion;
    }

    public Long getEstado() {
        return estado;
    }

    public void setEstado(Long estado) {
        this.estado = estado;
    }
    
    public String getCodigoPetro() {
        return codigoPetro;
    }

    public void setCodigoPetro(String codigoPetro) {
        this.codigoPetro = codigoPetro;
    }
}

