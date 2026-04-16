package com.saa.model.crd;

import java.io.Serializable;
import java.time.LocalDate;

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
 * Representa la tabla DTGA (DetalleGeneracionArchivo).
 * 
 * Almacena el resumen por tipo de producto de cada generación del archivo
 * de descuentos Petrocomercial.
 * 
 * PROPÓSITO:
 * Esta tabla agrupa y totaliza los datos por tipo de producto dentro de una
 * generación específica. Permite tener un resumen consolidado por producto
 * antes de desglosar en partícipes individuales.
 * 
 * TIPOS DE PRODUCTO:
 * - AH: Aportes Voluntarios / Ahorro
 * - HS: Hipoteca Secundaria
 * - PE: Préstamo Emergente
 * - PH: Préstamo Hipotecario
 * - PQ: Préstamo Quirografario
 * - PP: Préstamo Personal
 * 
 * RELACIONES:
 * - Pertenece a una GeneracionArchivoPetro (GNAP)
 * - Tiene múltiples ParticipeDetalleGeneracionArchivo (PDGA) - uno por partícipe
 * 
 * @author Sistema SAA
 * @since 2026-04-15
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "DTGA", schema = "CRD")
@NamedQueries({
    @NamedQuery(name = "DetalleGeneracionArchivoAll", query = "select e from DetalleGeneracionArchivo e"),
    @NamedQuery(name = "DetalleGeneracionArchivoId", query = "select e from DetalleGeneracionArchivo e where e.codigo = :id"),
    @NamedQuery(name = "DetalleGeneracionArchivoByGeneracion", query = "select e from DetalleGeneracionArchivo e where e.generacionArchivoPetro.codigo = :codigoGeneracion order by e.codigoProductoPetro")
})
public class DetalleGeneracionArchivo implements Serializable {

    // ============================================================
    // PK
    // ============================================================
    
    /**
     * Código único del detalle (IDENTITY).
     */
    @Id
    @Basic
    @Column(name = "DTGACDGO")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    // ============================================================
    // RELACIONES (FKs)
    // ============================================================
    
    /**
     * FK - Generación Archivo Petrocomercial.
     */
    @ManyToOne
    @JoinColumn(name = "GNAPCDGO", referencedColumnName = "GNAPCDGO")
    private GeneracionArchivoPetro generacionArchivoPetro;

    // ============================================================
    // PRODUCTO
    // ============================================================
    
    /**
     * Código del producto Petrocomercial (AH, HS, PE, PH, PQ, PP).
     */
    @Basic
    @Column(name = "DTGACDPT", length = 2)
    private String codigoProductoPetro;
    
    /**
     * Descripción del tipo de producto.
     */
    @Basic
    @Column(name = "DTGADSCR", length = 200)
    private String descripcionProducto;

    // ============================================================
    // TOTALES
    // ============================================================
    
    /**
     * Total de registros de este tipo de producto.
     */
    @Basic
    @Column(name = "DTGATLRG")
    private Long totalRegistros;
    
    /**
     * Total monto de este tipo de producto.
     */
    @Basic
    @Column(name = "DTGAMNTO")
    private Double totalMonto;

    // ============================================================
    // AUDITORÍA
    // ============================================================
    
    /**
     * Usuario ingreso.
     */
    @Basic
    @Column(name = "DTGAUSIN", length = 50)
    private String usuarioIngreso;
    
    /**
     * Fecha ingreso.
     */
    @Basic
    @Column(name = "DTGAFCIN")
    private LocalDate fechaIngreso;
    
    /**
     * Usuario modificación.
     */
    @Basic
    @Column(name = "DTGAUSMD", length = 50)
    private String usuarioModificacion;
    
    /**
     * Fecha modificación.
     */
    @Basic
    @Column(name = "DTGAFCMD")
    private LocalDate fechaModificacion;

    // ============================================================
    // CONSTRUCTORES
    // ============================================================
    
    public DetalleGeneracionArchivo() {
    }

    // ============================================================
    // GETTERS Y SETTERS
    // ============================================================

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public GeneracionArchivoPetro getGeneracionArchivoPetro() {
        return generacionArchivoPetro;
    }

    public void setGeneracionArchivoPetro(GeneracionArchivoPetro generacionArchivoPetro) {
        this.generacionArchivoPetro = generacionArchivoPetro;
    }

    public String getCodigoProductoPetro() {
        return codigoProductoPetro;
    }

    public void setCodigoProductoPetro(String codigoProductoPetro) {
        this.codigoProductoPetro = codigoProductoPetro;
    }

    public String getDescripcionProducto() {
        return descripcionProducto;
    }

    public void setDescripcionProducto(String descripcionProducto) {
        this.descripcionProducto = descripcionProducto;
    }

    public Long getTotalRegistros() {
        return totalRegistros;
    }

    public void setTotalRegistros(Long totalRegistros) {
        this.totalRegistros = totalRegistros;
    }

    public Double getTotalMonto() {
        return totalMonto;
    }

    public void setTotalMonto(Double totalMonto) {
        this.totalMonto = totalMonto;
    }

    public String getUsuarioIngreso() {
        return usuarioIngreso;
    }

    public void setUsuarioIngreso(String usuarioIngreso) {
        this.usuarioIngreso = usuarioIngreso;
    }

    public LocalDate getFechaIngreso() {
        return fechaIngreso;
    }

    public void setFechaIngreso(LocalDate fechaIngreso) {
        this.fechaIngreso = fechaIngreso;
    }

    public String getUsuarioModificacion() {
        return usuarioModificacion;
    }

    public void setUsuarioModificacion(String usuarioModificacion) {
        this.usuarioModificacion = usuarioModificacion;
    }

    public LocalDate getFechaModificacion() {
        return fechaModificacion;
    }

    public void setFechaModificacion(LocalDate fechaModificacion) {
        this.fechaModificacion = fechaModificacion;
    }
}
