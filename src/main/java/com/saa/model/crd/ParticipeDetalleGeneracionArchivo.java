package com.saa.model.crd;

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
 * Representa la tabla PDGA (ParticipeDetalleGeneracionArchivo).
 * 
 * Almacena el detalle de cada partícipe-producto enviado en el archivo
 * de descuentos Petrocomercial.
 * Una línea por cada registro del archivo TXT generado.
 * 
 * PROPÓSITO:
 * Esta tabla es el nivel más detallado de la generación del archivo.
 * Cada registro representa UNA LÍNEA del archivo TXT enviado a Petrocomercial.
 * Permite:
 * - Rastrear exactamente qué se envió por cada partícipe
 * - Vincular el descuento con el préstamo específico (si aplica)
 * - Auditar montos enviados vs procesados
 * - Reconstruir el archivo si es necesario
 * - Registrar el monto efectivamente descontado cuando se recibe respuesta
 * 
 * RELACIONES:
 * - Pertenece a un DetalleGeneracionArchivo (DTGA)
 * - Se vincula a una Entidad (partícipe)
 * - Opcionalmente se vincula a un Prestamo (NULL para AH)
 * 
 * ESTRUCTURA DEL ARCHIVO:
 * Cada registro de esta tabla corresponde a una línea de 55 caracteres en el archivo TXT:
 * - Código partícipe (5 chars)
 * - Código fijo "JRNN" (4 chars)
 * - Relleno ceros (8 chars)
 * - Fecha proceso (8 chars)
 * - Monto * 10000 (13 chars)
 * - Código "1" (1 char)
 * - Relleno ceros (14 chars)
 * - Tipo producto (2 chars)
 * 
 * @author Sistema SAA
 * @since 2026-04-15
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "PDGA", schema = "CRD")
@NamedQueries({
    @NamedQuery(name = "ParticipeDetalleGeneracionArchivoAll", query = "select e from ParticipeDetalleGeneracionArchivo e"),
    @NamedQuery(name = "ParticipeDetalleGeneracionArchivoId", query = "select e from ParticipeDetalleGeneracionArchivo e where e.codigo = :id"),
    @NamedQuery(name = "ParticipeDetalleGeneracionArchivoByDetalle", query = "select e from ParticipeDetalleGeneracionArchivo e where e.detalleGeneracionArchivo.codigo = :codigoDetalle order by e.numeroLinea"),
    @NamedQuery(name = "ParticipeDetalleGeneracionArchivoByEntidad", query = "select e from ParticipeDetalleGeneracionArchivo e where e.entidad.codigo = :codigoEntidad order by e.numeroLinea")
})
public class ParticipeDetalleGeneracionArchivo implements Serializable {

    // ============================================================
    // PK
    // ============================================================
    
    /**
     * Código único del registro (IDENTITY).
     */
    @Id
    @Basic
    @Column(name = "PDGACDGO")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    // ============================================================
    // RELACIONES (FKs)
    // ============================================================
    
    /**
     * FK - Detalle Generación Archivo (tipo de producto).
     */
    @ManyToOne
    @JoinColumn(name = "DTGACDGO", referencedColumnName = "DTGACDGO")
    private DetalleGeneracionArchivo detalleGeneracionArchivo;
    
    /**
     * FK - Entidad (partícipe).
     */
    @ManyToOne
    @JoinColumn(name = "ENTDCDGO", referencedColumnName = "ENTDCDGO")
    private Entidad entidad;
    
    /**
     * FK - Préstamo (NULL para AH - Aportes).
     */
    @ManyToOne
    @JoinColumn(name = "PRSTCDGO", referencedColumnName = "PRSTCDGO")
    private Prestamo prestamo;

    // ============================================================
    // DATOS PETROCOMERCIAL
    // ============================================================
    
    /**
     * Rol Petrocomercial del partícipe.
     * Este es el código interno que Petrocomercial usa para identificar al empleado.
     */
    @Basic
    @Column(name = "PDGARLPC")
    private Long rolPetrocomercial;
    
    /**
     * Código producto Petrocomercial (AH, HS, PE, PH, PQ, PP).
     */
    @Basic
    @Column(name = "PDGACDPT", length = 2)
    private String codigoProductoPetro;
    
    /**
     * Monto enviado a descontar.
     * Este es el valor real en dólares (no multiplicado por 10000).
     */
    @Basic
    @Column(name = "PDGAMNEN")
    private Double montoEnviado;

    // ============================================================
    // CONTROL DE ARCHIVO
    // ============================================================
    
    /**
     * Número de línea en el archivo TXT.
     * Permite rastrear la posición exacta de este registro en el archivo generado.
     */
    @Basic
    @Column(name = "PDGANNLN")
    private Long numeroLinea;
    
    /**
     * Observaciones.
     */
    @Basic
    @Column(name = "PDGAOBSR", length = 500)
    private String observaciones;

    // ============================================================
    // ESTADO Y RESPUESTA
    // ============================================================
    
    /**
     * Estado del registro individual (numérico).
     * 1=ENVIADO, 2=PROCESADO, 3=RECHAZADO
     */
    @Basic
    @Column(name = "PDGAESTD")
    private Integer estado;
    
    /**
     * Monto efectivamente descontado (según respuesta de Petrocomercial).
     */
    @Basic
    @Column(name = "PDGAMNDC")
    private Double montoDescontado;
    
    /**
     * Fecha en que se aplicó el descuento (según archivo de respuesta).
     */
    @Basic
    @Column(name = "PDGAFCDC")
    private LocalDateTime fechaDescuento;

    // ============================================================
    // AUDITORÍA
    // ============================================================
    
    /**
     * Usuario ingreso.
     */
    @Basic
    @Column(name = "PDGAUSIN", length = 50)
    private String usuarioIngreso;
    
    /**
     * Fecha ingreso.
     */
    @Basic
    @Column(name = "PDGAFCIN")
    private LocalDateTime fechaIngreso;
    
    /**
     * Usuario modificación.
     */
    @Basic
    @Column(name = "PDGAUSMD", length = 50)
    private String usuarioModificacion;
    
    /**
     * Fecha modificación.
     */
    @Basic
    @Column(name = "PDGAFCMD")
    private LocalDateTime fechaModificacion;

    // ============================================================
    // CONSTRUCTORES
    // ============================================================
    
    public ParticipeDetalleGeneracionArchivo() {
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

    public DetalleGeneracionArchivo getDetalleGeneracionArchivo() {
        return detalleGeneracionArchivo;
    }

    public void setDetalleGeneracionArchivo(DetalleGeneracionArchivo detalleGeneracionArchivo) {
        this.detalleGeneracionArchivo = detalleGeneracionArchivo;
    }

    public Entidad getEntidad() {
        return entidad;
    }

    public void setEntidad(Entidad entidad) {
        this.entidad = entidad;
    }

    public Prestamo getPrestamo() {
        return prestamo;
    }

    public void setPrestamo(Prestamo prestamo) {
        this.prestamo = prestamo;
    }

    public Long getRolPetrocomercial() {
        return rolPetrocomercial;
    }

    public void setRolPetrocomercial(Long rolPetrocomercial) {
        this.rolPetrocomercial = rolPetrocomercial;
    }

    public String getCodigoProductoPetro() {
        return codigoProductoPetro;
    }

    public void setCodigoProductoPetro(String codigoProductoPetro) {
        this.codigoProductoPetro = codigoProductoPetro;
    }

    public Double getMontoEnviado() {
        return montoEnviado;
    }

    public void setMontoEnviado(Double montoEnviado) {
        this.montoEnviado = montoEnviado;
    }

    public Long getNumeroLinea() {
        return numeroLinea;
    }

    public void setNumeroLinea(Long numeroLinea) {
        this.numeroLinea = numeroLinea;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public Integer getEstado() {
        return estado;
    }

    public void setEstado(Integer estado) {
        this.estado = estado;
    }

    public Double getMontoDescontado() {
        return montoDescontado;
    }

    public void setMontoDescontado(Double montoDescontado) {
        this.montoDescontado = montoDescontado;
    }

    public LocalDateTime getFechaDescuento() {
        return fechaDescuento;
    }

    public void setFechaDescuento(LocalDateTime fechaDescuento) {
        this.fechaDescuento = fechaDescuento;
    }

    public String getUsuarioIngreso() {
        return usuarioIngreso;
    }

    public void setUsuarioIngreso(String usuarioIngreso) {
        this.usuarioIngreso = usuarioIngreso;
    }

    public LocalDateTime getFechaIngreso() {
        return fechaIngreso;
    }

    public void setFechaIngreso(LocalDateTime fechaIngreso) {
        this.fechaIngreso = fechaIngreso;
    }

    public String getUsuarioModificacion() {
        return usuarioModificacion;
    }

    public void setUsuarioModificacion(String usuarioModificacion) {
        this.usuarioModificacion = usuarioModificacion;
    }

    public LocalDateTime getFechaModificacion() {
        return fechaModificacion;
    }

    public void setFechaModificacion(LocalDateTime fechaModificacion) {
        this.fechaModificacion = fechaModificacion;
    }
}
