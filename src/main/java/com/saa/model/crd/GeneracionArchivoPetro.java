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
 * Representa la tabla GNAP (GeneracionArchivoPetro).
 * 
 * Almacena la cabecera de cada generación mensual del archivo de descuentos
 * que se envía a Petrocomercial.
 * 
 * PROPÓSITO:
 * Esta tabla registra cada proceso de generación de archivo TXT para Petrocomercial,
 * conteniendo:
 * - Periodo (mes/año) de la generación
 * - Totales generales (registros y montos)
 * - Estado del proceso (GENERADO, ENVIADO, PROCESADO)
 * - Información del archivo físico generado
 * - Fechas de seguimiento (generación, envío, procesamiento)
 * 
 * RELACIONES:
 * - Tiene múltiples DetalleGeneracionArchivo (DTGA) - uno por tipo de producto
 * - Pertenece a una Filial
 * 
 * FLUJO:
 * 1. Se crea registro en estado GENERADO al iniciar el proceso
 * 2. Se genera el archivo TXT con formato Petrocomercial
 * 3. Se guarda ruta y nombre del archivo
 * 4. Al enviar, cambia a ENVIADO y registra fecha de envío
 * 5. Al recibir respuesta, cambia a PROCESADO y registra fecha de procesamiento
 * 
 * @author Sistema SAA
 * @since 2026-04-15
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "GNAP", schema = "CRD")
@NamedQueries({
    @NamedQuery(name = "GeneracionArchivoPetroAll", query = "select e from GeneracionArchivoPetro e order by e.fechaGeneracion desc"),
    @NamedQuery(name = "GeneracionArchivoPetroId", query = "select e from GeneracionArchivoPetro e where e.codigo = :id"),
    @NamedQuery(name = "GeneracionArchivoPetroByPeriodo", query = "select e from GeneracionArchivoPetro e where e.mesPeriodo = :mes and e.anioPeriodo = :anio and e.filial.codigo = :codigoFilial")
})
public class GeneracionArchivoPetro implements Serializable {

    // ============================================================
    // PK
    // ============================================================
    
    /**
     * Código único de la generación (IDENTITY).
     */
    @Id
    @Basic
    @Column(name = "GNAPCDGO")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    // ============================================================
    // PERIODO
    // ============================================================
    
    /**
     * Mes del periodo (1-12).
     */
    @Basic
    @Column(name = "GNAPMSPE")
    private Long mesPeriodo;
    
    /**
     * Año del periodo (ej: 2025).
     */
    @Basic
    @Column(name = "GNAPANPE")
    private Long anioPeriodo;

    // ============================================================
    // GENERACIÓN
    // ============================================================
    
    /**
     * Fecha de generación del archivo.
     */
    @Basic
    @Column(name = "GNAPFCGN")
    private LocalDate fechaGeneracion;
    
    /**
     * Usuario que generó el archivo.
     */
    @Basic
    @Column(name = "GNAPUSGN", length = 50)
    private String usuarioGeneracion;

    // ============================================================
    // TOTALES
    // ============================================================
    
    /**
     * Total de registros en el archivo.
     */
    @Basic
    @Column(name = "GNAPTLRG")
    private Long totalRegistros;
    
    /**
     * Total monto enviado a descontar (suma de todos los productos).
     */
    @Basic
    @Column(name = "GNAPMTEN")
    private Double totalMontoEnviado;

    // ============================================================
    // ESTADO Y ARCHIVO
    // ============================================================
    
    /**
     * Estado (numérico): 1=GENERADO, 2=ENVIADO, 3=PROCESADO.
     */
    @Basic
    @Column(name = "GNAPESTD")
    private Long estado;
    
    /**
     * Ruta física del archivo generado.
     */
    @Basic
    @Column(name = "GNAPRTAA", length = 500)
    private String rutaArchivo;
    
    /**
     * Nombre del archivo generado.
     */
    @Basic
    @Column(name = "GNAPNMAR", length = 200)
    private String nombreArchivo;

    // ============================================================
    // FECHAS DE SEGUIMIENTO
    // ============================================================
    
    /**
     * Fecha de envío a Petrocomercial.
     */
    @Basic
    @Column(name = "GNAPFCEN")
    private LocalDate fechaEnvio;
    
    /**
     * Fecha de procesamiento (respuesta recibida).
     */
    @Basic
    @Column(name = "GNAPFCPR")
    private LocalDate fechaProcesamiento;

    // ============================================================
    // OBSERVACIONES
    // ============================================================
    
    /**
     * Observaciones.
     */
    @Basic
    @Column(name = "GNAPOBSR", length = 4000)
    private String observaciones;

    // ============================================================
    // RELACIONES (FKs)
    // ============================================================
    
    /**
     * FK - Filial.
     */
    @ManyToOne
    @JoinColumn(name = "FLLLCDGO", referencedColumnName = "FLLLCDGO")
    private Filial filial;

    // ============================================================
    // AUDITORÍA
    // ============================================================
    
    /**
     * Usuario ingreso.
     */
    @Basic
    @Column(name = "GNAPUSIN", length = 50)
    private String usuarioIngreso;
    
    /**
     * Fecha ingreso.
     */
    @Basic
    @Column(name = "GNAPFCIN")
    private LocalDate fechaIngreso;
    
    /**
     * Usuario modificación.
     */
    @Basic
    @Column(name = "GNAPUSMD", length = 50)
    private String usuarioModificacion;
    
    /**
     * Fecha modificación.
     */
    @Basic
    @Column(name = "GNAPFCMD")
    private LocalDate fechaModificacion;

    // ============================================================
    // CONSTRUCTORES
    // ============================================================
    
    public GeneracionArchivoPetro() {
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

    public Long getMesPeriodo() {
        return mesPeriodo;
    }

    public void setMesPeriodo(Long mesPeriodo) {
        this.mesPeriodo = mesPeriodo;
    }

    public Long getAnioPeriodo() {
        return anioPeriodo;
    }

    public void setAnioPeriodo(Long anioPeriodo) {
        this.anioPeriodo = anioPeriodo;
    }

    public LocalDate getFechaGeneracion() {
        return fechaGeneracion;
    }

    public void setFechaGeneracion(LocalDate fechaGeneracion) {
        this.fechaGeneracion = fechaGeneracion;
    }

    public String getUsuarioGeneracion() {
        return usuarioGeneracion;
    }

    public void setUsuarioGeneracion(String usuarioGeneracion) {
        this.usuarioGeneracion = usuarioGeneracion;
    }

    public Long getTotalRegistros() {
        return totalRegistros;
    }

    public void setTotalRegistros(Long totalRegistros) {
        this.totalRegistros = totalRegistros;
    }

    public Double getTotalMontoEnviado() {
        return totalMontoEnviado;
    }

    public void setTotalMontoEnviado(Double totalMontoEnviado) {
        this.totalMontoEnviado = totalMontoEnviado;
    }

    public Long getEstado() {
        return estado;
    }

    public void setEstado(Long estado) {
        this.estado = estado;
    }

    public String getRutaArchivo() {
        return rutaArchivo;
    }

    public void setRutaArchivo(String rutaArchivo) {
        this.rutaArchivo = rutaArchivo;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public void setNombreArchivo(String nombreArchivo) {
        this.nombreArchivo = nombreArchivo;
    }

    public LocalDate getFechaEnvio() {
        return fechaEnvio;
    }

    public void setFechaEnvio(LocalDate fechaEnvio) {
        this.fechaEnvio = fechaEnvio;
    }

    public LocalDate getFechaProcesamiento() {
        return fechaProcesamiento;
    }

    public void setFechaProcesamiento(LocalDate fechaProcesamiento) {
        this.fechaProcesamiento = fechaProcesamiento;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public Filial getFilial() {
        return filial;
    }

    public void setFilial(Filial filial) {
        this.filial = filial;
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
