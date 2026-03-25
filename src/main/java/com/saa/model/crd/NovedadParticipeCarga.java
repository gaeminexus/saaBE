package com.saa.model.crd;

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
import jakarta.persistence.Table;

/**
 * Representa la tabla NVPC (NovedadParticipeCarga).
 * Almacena las novedades encontradas para cada partícipe durante la carga.
 * Un partícipe puede tener MÚLTIPLES novedades, incluso más de una por producto.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "NVPC", schema = "CRD")
@NamedQueries({
    @NamedQuery(name = "NovedadParticipeCargaAll", query = "select e from NovedadParticipeCarga e"),
    @NamedQuery(name = "NovedadParticipeCargaId", query = "select e from NovedadParticipeCarga e where e.codigo = :id")
})
public class NovedadParticipeCarga implements Serializable {

    // PK
    @Id
    @Basic
    @Column(name = "NVPCCDGO")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /** FK - Partícipe X Carga Archivo */
    @ManyToOne
    @JoinColumn(name = "PXCACDGO", referencedColumnName = "PXCACDGO")
    private ParticipeXCargaArchivo participeXCargaArchivo;

    /** Tipo de novedad (código del rubro ASPNovedadesCargaArchivo) */
    @Basic
    @Column(name = "NVPCTPNV")
    private Long tipoNovedad;

    /** Descripción de la novedad */
    @Basic
    @Column(name = "NVPCDSCR", length = 4000)
    private String descripcion;

    /** Código del producto relacionado (si aplica) */
    @Basic
    @Column(name = "NVPCCDPR")
    private Long codigoProducto;

    /** Código del préstamo relacionado (si aplica) */
    @Basic
    @Column(name = "NVPCCDPS")
    private Long codigoPrestamo;

    /** Código de la cuota relacionada (si aplica) */
    @Basic
    @Column(name = "NVPCCDCT")
    private Long codigoCuota;

    /** Monto esperado del sistema */
    @Basic
    @Column(name = "NVPCMNES")
    private Double montoEsperado;

    /** Monto recibido del archivo */
    @Basic
    @Column(name = "NVPCMNRC")
    private Double montoRecibido;

    /** Diferencia entre montos */
    @Basic
    @Column(name = "NVPCMNDF")
    private Double montoDiferencia;

    /** Código de la carga archivo (para facilitar búsquedas en frontend) */
    @Basic
    @Column(name = "NVPCCDCA")
    private Long codigoCargaArchivo;

    /** ID Asoprep del préstamo (para mostrar en pantalla) */
    @Basic
    @Column(name = "NVPCIASP")
    private Long idAsoprepPrestamo;

    /** Estado del registro */
    @Basic
    @Column(name = "NVPCESTD")
    private Long estado;

    // ============================================================
    // GETTERS Y SETTERS
    // ============================================================

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public ParticipeXCargaArchivo getParticipeXCargaArchivo() {
        return participeXCargaArchivo;
    }

    public void setParticipeXCargaArchivo(ParticipeXCargaArchivo participeXCargaArchivo) {
        this.participeXCargaArchivo = participeXCargaArchivo;
    }

    public Long getTipoNovedad() {
        return tipoNovedad;
    }

    public void setTipoNovedad(Long tipoNovedad) {
        this.tipoNovedad = tipoNovedad;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Long getCodigoProducto() {
        return codigoProducto;
    }

    public void setCodigoProducto(Long codigoProducto) {
        this.codigoProducto = codigoProducto;
    }

    public Long getCodigoPrestamo() {
        return codigoPrestamo;
    }

    public void setCodigoPrestamo(Long codigoPrestamo) {
        this.codigoPrestamo = codigoPrestamo;
    }

    public Long getCodigoCuota() {
        return codigoCuota;
    }

    public void setCodigoCuota(Long codigoCuota) {
        this.codigoCuota = codigoCuota;
    }

    public Double getMontoEsperado() {
        return montoEsperado;
    }

    public void setMontoEsperado(Double montoEsperado) {
        this.montoEsperado = montoEsperado;
    }

    public Double getMontoRecibido() {
        return montoRecibido;
    }

    public void setMontoRecibido(Double montoRecibido) {
        this.montoRecibido = montoRecibido;
    }

    public Double getMontoDiferencia() {
        return montoDiferencia;
    }

    public void setMontoDiferencia(Double montoDiferencia) {
        this.montoDiferencia = montoDiferencia;
    }

    public Long getCodigoCargaArchivo() {
        return codigoCargaArchivo;
    }

    public void setCodigoCargaArchivo(Long codigoCargaArchivo) {
        this.codigoCargaArchivo = codigoCargaArchivo;
    }

    public Long getIdAsoprepPrestamo() {
        return idAsoprepPrestamo;
    }

    public void setIdAsoprepPrestamo(Long idAsoprepPrestamo) {
        this.idAsoprepPrestamo = idAsoprepPrestamo;
    }

    public Long getEstado() {
        return estado;
    }

    public void setEstado(Long estado) {
        this.estado = estado;
    }
}
