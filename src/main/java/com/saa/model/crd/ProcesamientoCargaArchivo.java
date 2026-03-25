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
 * Representa la tabla PRCA (ProcesamientoCargaArchivo).
 * Tracking del procesamiento de registros del archivo Petro (FASE 2).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "PRCA", schema = "CRD")
@NamedQueries({
    @NamedQuery(name = "ProcesamientoCargaArchivoAll", query = "select e from ProcesamientoCargaArchivo e"),
    @NamedQuery(name = "ProcesamientoCargaArchivoId", query = "select e from ProcesamientoCargaArchivo e where e.codigo = :id")
})
public class ProcesamientoCargaArchivo implements Serializable {

    /** PK */
    @Id
    @Basic
    @Column(name = "PRCACDGO")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /** FK - Participe X Carga Archivo */
    @ManyToOne
    @JoinColumn(name = "PXCACDGO", referencedColumnName = "PXCACDGO")
    private ParticipeXCargaArchivo participeXCargaArchivo;

    /** Fecha de procesamiento */
    @Basic
    @Column(name = "PRCAFCPR")
    private LocalDateTime fechaProcesamiento;

    /** Estado procesamiento: 0=No procesado, 1=Procesado OK, 2=Error */
    @Basic
    @Column(name = "PRCAPRCS")
    private Integer procesado;

    /** Novedad del procesamiento (códigos ASPNovedadesCargaArchivo) */
    @Basic
    @Column(name = "PRCANVPR")
    private Integer novedadProcesamiento;

    /** ID del PagoPrestamo generado */
    @Basic
    @Column(name = "PRCAIDPG")
    private Long idPagoGenerado;

    /** ID del DetallePrestamo procesada (cuota) */
    @Basic
    @Column(name = "PRCAIDCT")
    private Long idCuotaProcesada;

    /** ID del Aporte generado */
    @Basic
    @Column(name = "PRCAIDAP")
    private Long idAporteGenerado;

    /** ID del Prestamo procesado */
    @Basic
    @Column(name = "PRCAIDPS")
    private Long idPrestamoProcessado;

    /** Saldo de capital pendiente después del pago */
    @Basic
    @Column(name = "PRCASLCP")
    private Double saldoCapitalPendiente;

    /** Saldo de interés pendiente después del pago */
    @Basic
    @Column(name = "PRCASLIN")
    private Double saldoInteresPendiente;

    /** Estado determinado de la cuota (códigos EstadoCuotaPrestamo) */
    @Basic
    @Column(name = "PRCAESDQ")
    private Integer estadoCuotaDeterminado;

    /** Observaciones del procesamiento */
    @Basic
    @Column(name = "PRCAOBSR", length = 4000)
    private String observaciones;

    /** Descripción del error si hubo */
    @Basic
    @Column(name = "PRCAERRO", length = 4000)
    private String error;

    /** Fecha de registro */
    @Basic
    @Column(name = "PRCAFCRG")
    private LocalDateTime fechaRegistro;

    /** Usuario que procesó */
    @Basic
    @Column(name = "PRCAUSRG", length = 200)
    private String usuarioRegistro;

    /** Estado del registro */
    @Basic
    @Column(name = "PRCAESTD")
    private Integer estado;

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

    public LocalDateTime getFechaProcesamiento() {
        return fechaProcesamiento;
    }

    public void setFechaProcesamiento(LocalDateTime fechaProcesamiento) {
        this.fechaProcesamiento = fechaProcesamiento;
    }

    public Integer getProcesado() {
        return procesado;
    }

    public void setProcesado(Integer procesado) {
        this.procesado = procesado;
    }

    public Integer getNovedadProcesamiento() {
        return novedadProcesamiento;
    }

    public void setNovedadProcesamiento(Integer novedadProcesamiento) {
        this.novedadProcesamiento = novedadProcesamiento;
    }

    public Long getIdPagoGenerado() {
        return idPagoGenerado;
    }

    public void setIdPagoGenerado(Long idPagoGenerado) {
        this.idPagoGenerado = idPagoGenerado;
    }

    public Long getIdCuotaProcesada() {
        return idCuotaProcesada;
    }

    public void setIdCuotaProcesada(Long idCuotaProcesada) {
        this.idCuotaProcesada = idCuotaProcesada;
    }

    public Long getIdAporteGenerado() {
        return idAporteGenerado;
    }

    public void setIdAporteGenerado(Long idAporteGenerado) {
        this.idAporteGenerado = idAporteGenerado;
    }

    public Long getIdPrestamoProcessado() {
        return idPrestamoProcessado;
    }

    public void setIdPrestamoProcessado(Long idPrestamoProcessado) {
        this.idPrestamoProcessado = idPrestamoProcessado;
    }

    public Double getSaldoCapitalPendiente() {
        return saldoCapitalPendiente;
    }

    public void setSaldoCapitalPendiente(Double saldoCapitalPendiente) {
        this.saldoCapitalPendiente = saldoCapitalPendiente;
    }

    public Double getSaldoInteresPendiente() {
        return saldoInteresPendiente;
    }

    public void setSaldoInteresPendiente(Double saldoInteresPendiente) {
        this.saldoInteresPendiente = saldoInteresPendiente;
    }

    public Integer getEstadoCuotaDeterminado() {
        return estadoCuotaDeterminado;
    }

    public void setEstadoCuotaDeterminado(Integer estadoCuotaDeterminado) {
        this.estadoCuotaDeterminado = estadoCuotaDeterminado;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public String getUsuarioRegistro() {
        return usuarioRegistro;
    }

    public void setUsuarioRegistro(String usuarioRegistro) {
        this.usuarioRegistro = usuarioRegistro;
    }

    public Integer getEstado() {
        return estado;
    }

    public void setEstado(Integer estado) {
        this.estado = estado;
    }
}
