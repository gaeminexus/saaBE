package com.saa.model.crd;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.saa.model.scp.Usuario;

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
 * Representa la tabla CRAR (CargaArchivo).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "CRAR", schema = "CRD")
@NamedQueries({
    @NamedQuery(name = "CargaArchivoAll", query = "select e from CargaArchivo e"),
    @NamedQuery(name = "CargaArchivoId", query = "select e from CargaArchivo e where e.codigo = :id")
})
public class CargaArchivo implements Serializable {

    // PK
    @Id
    @Basic
    @Column(name = "CRARCDGO")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /** Nombre del archivo cargado */
    @Basic
    @Column(name = "CRARNMBR", length = 2000)
    private String nombre;

    /** Fecha de carga */
    @Basic
    @Column(name = "CRARFCCR")
    private LocalDateTime fechaCarga;

    /** Usuario que cargó */
    @ManyToOne
    @JoinColumn(name = "CRARUSCD", referencedColumnName = "PJRQCDGO")
    private Usuario usuarioCarga;

    /** FK - Filial */
    @ManyToOne
    @JoinColumn(name = "FLLLCDGO", referencedColumnName = "FLLLCDGO")
    private Filial filial;

    /** Ruta del archivo en el servidor */
    @Basic
    @Column(name = "CRARRTAS", length = 2000)
    private String rutaArchivo;

    /** Mes de afectación */
    @Basic
    @Column(name = "CRARMSAF")
    private Long mesAfectacion;

    /** Año de afectación */
    @Basic
    @Column(name = "CRARANAF")
    private Long anioAfectacion;

    /** Total Saldo Actual */
    @Basic
    @Column(name = "CRARTTSA")
    private Double totalSaldoActual;

    /** Total Interés Anual */
    @Basic
    @Column(name = "CRARTTIA")
    private Double totalInteresAnual;

    /** Total Valor Seguro */
    @Basic
    @Column(name = "CRARTTVS")
    private Double totalValorSeguro;

    /** Total Descontar */
    @Basic
    @Column(name = "CRARTTDS")
    private Double totalDescontar;

    /** Total Capital Descontado */
    @Basic
    @Column(name = "CRARTTCD")
    private Double totalCapitalDescontado;

    /** Total Interés Descontado */
    @Basic
    @Column(name = "CRARTTID")
    private Double totalInteresDescontado;

    /** Total Seguro Descontado */
    @Basic
    @Column(name = "CRARTTSD")
    private Double totalSeguroDescontado;

    /** Total Descontado */
    @Basic
    @Column(name = "CRARTTDO")
    private Double totalDescontado;

    /** Total Capital No Descontado */
    @Basic
    @Column(name = "CRARTCND")
    private Double totalCapitalNoDescontado;

    /** Total Interés No Descontado */
    @Basic
    @Column(name = "CRARTIND")
    private Double totalInteresNoDescontado;

    /** Total Desgravamen No Descontado */
    @Basic
    @Column(name = "CRARTDND")
    private Double totalDesgravamenNoDescontado;

    /** Estado del registro */
    @Basic
    @Column(name = "CRARESTD")
    private Long estado;

    /** Número de Transferencia */
    @Basic
    @Column(name = "CRARNMTF")
    private Long numeroTransferencia;

    /** Usuario Contabilidad que Confirma */
    @ManyToOne
    @JoinColumn(name = "CRARUSCC", referencedColumnName = "PJRQCDGO")
    private Usuario usuarioContabilidadConfirma;

    /** Fecha de Autorización Contabilidad */
    @Basic
    @Column(name = "CRARFCAC")
    private LocalDateTime fechaAutorizacionContabilidad;

    /** Usuario de Anulación */
    @ManyToOne
    @JoinColumn(name = "CRARUSAN", referencedColumnName = "PJRQCDGO")
    private Usuario usuarioAnulacion;

    /** Motivo de Anulación */
    @Basic
    @Column(name = "CRARMTAN", length = 2000)
    private String motivoAnulacion;

    /** Fecha de Anulación */
    @Basic
    @Column(name = "CRARFCAN")
    private LocalDateTime fechaAnulacion;

    // ============================================================
    // GETTERS Y SETTERS
    // ============================================================

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public LocalDateTime getFechaCarga() {
        return fechaCarga;
    }

    public void setFechaCarga(LocalDateTime fechaCarga) {
        this.fechaCarga = fechaCarga;
    }

    public Usuario getUsuarioCarga() {
        return usuarioCarga;
    }

    public void setUsuarioCarga(Usuario usuarioCarga) {
        this.usuarioCarga = usuarioCarga;
    }

    public Filial getFilial() {
        return filial;
    }

    public void setFilial(Filial filial) {
        this.filial = filial;
    }

    public String getRutaArchivo() {
        return rutaArchivo;
    }

    public void setRutaArchivo(String rutaArchivo) {
        this.rutaArchivo = rutaArchivo;
    }

    public Long getMesAfectacion() {
        return mesAfectacion;
    }

    public void setMesAfectacion(Long mesAfectacion) {
        this.mesAfectacion = mesAfectacion;
    }

    public Long getAnioAfectacion() {
        return anioAfectacion;
    }

    public void setAnioAfectacion(Long anioAfectacion) {
        this.anioAfectacion = anioAfectacion;
    }

    public Double getTotalSaldoActual() {
        return totalSaldoActual;
    }

    public void setTotalSaldoActual(Double totalSaldoActual) {
        this.totalSaldoActual = totalSaldoActual;
    }

    public Double getTotalInteresAnual() {
        return totalInteresAnual;
    }

    public void setTotalInteresAnual(Double totalInteresAnual) {
        this.totalInteresAnual = totalInteresAnual;
    }

    public Double getTotalValorSeguro() {
        return totalValorSeguro;
    }

    public void setTotalValorSeguro(Double totalValorSeguro) {
        this.totalValorSeguro = totalValorSeguro;
    }

    public Double getTotalDescontar() {
        return totalDescontar;
    }

    public void setTotalDescontar(Double totalDescontar) {
        this.totalDescontar = totalDescontar;
    }

    public Double getTotalCapitalDescontado() {
        return totalCapitalDescontado;
    }

    public void setTotalCapitalDescontado(Double totalCapitalDescontado) {
        this.totalCapitalDescontado = totalCapitalDescontado;
    }

    public Double getTotalInteresDescontado() {
        return totalInteresDescontado;
    }

    public void setTotalInteresDescontado(Double totalInteresDescontado) {
        this.totalInteresDescontado = totalInteresDescontado;
    }

    public Double getTotalSeguroDescontado() {
        return totalSeguroDescontado;
    }

    public void setTotalSeguroDescontado(Double totalSeguroDescontado) {
        this.totalSeguroDescontado = totalSeguroDescontado;
    }

    public Double getTotalDescontado() {
        return totalDescontado;
    }

    public void setTotalDescontado(Double totalDescontado) {
        this.totalDescontado = totalDescontado;
    }

    public Double getTotalCapitalNoDescontado() {
        return totalCapitalNoDescontado;
    }

    public void setTotalCapitalNoDescontado(Double totalCapitalNoDescontado) {
        this.totalCapitalNoDescontado = totalCapitalNoDescontado;
    }

    public Double getTotalInteresNoDescontado() {
        return totalInteresNoDescontado;
    }

    public void setTotalInteresNoDescontado(Double totalInteresNoDescontado) {
        this.totalInteresNoDescontado = totalInteresNoDescontado;
    }

    public Double getTotalDesgravamenNoDescontado() {
        return totalDesgravamenNoDescontado;
    }

    public void setTotalDesgravamenNoDescontado(Double totalDesgravamenNoDescontado) {
        this.totalDesgravamenNoDescontado = totalDesgravamenNoDescontado;
    }

    public Long getEstado() {
        return estado;
    }

    public void setEstado(Long estado) {
        this.estado = estado;
    }

    public Long getNumeroTransferencia() {
        return numeroTransferencia;
    }

    public void setNumeroTransferencia(Long numeroTransferencia) {
        this.numeroTransferencia = numeroTransferencia;
    }

    public Usuario getUsuarioContabilidadConfirma() {
        return usuarioContabilidadConfirma;
    }

    public void setUsuarioContabilidadConfirma(Usuario usuarioContabilidadConfirma) {
        this.usuarioContabilidadConfirma = usuarioContabilidadConfirma;
    }

    public LocalDateTime getFechaAutorizacionContabilidad() {
        return fechaAutorizacionContabilidad;
    }

    public void setFechaAutorizacionContabilidad(LocalDateTime fechaAutorizacionContabilidad) {
        this.fechaAutorizacionContabilidad = fechaAutorizacionContabilidad;
    }

    public Usuario getUsuarioAnulacion() {
        return usuarioAnulacion;
    }

    public void setUsuarioAnulacion(Usuario usuarioAnulacion) {
        this.usuarioAnulacion = usuarioAnulacion;
    }

    public String getMotivoAnulacion() {
        return motivoAnulacion;
    }

    public void setMotivoAnulacion(String motivoAnulacion) {
        this.motivoAnulacion = motivoAnulacion;
    }

    public LocalDateTime getFechaAnulacion() {
        return fechaAnulacion;
    }

    public void setFechaAnulacion(LocalDateTime fechaAnulacion) {
        this.fechaAnulacion = fechaAnulacion;
    }
}